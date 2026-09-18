#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor API — HTTP server (standard library only).
Endpoints (JSON, UTF-8):
  GET  /api/ping      -> health ping (no auth, no db needed)
  GET  /api/config    -> public config for the Android app (self-configuration)
  GET  /api/health    -> full health report (db, activation, ...)
  GET  /api/activate  -> current activation state
  POST /api/activate  -> activate with a code  {"code": "..."}
  POST /api/login     -> admin login            {"username": "...", "password": "..."}
  GET  /api/visitors  -> list visitors (bearer token, activated)
  POST /api/visitors  -> create visitor (bearer token, activated)
  GET  /api/visitors/since?after_id=N  -> incremental fetch (bearer token, activated)
  GET  /api/events    -> Server-Sent Events realtime stream (bearer token, activated)
"""
import argparse
import hmac
import json
import os
import queue
import secrets
import signal
import sys
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from db import DB, default_config_path, hash_password, load_config, verify_password  # noqa: E402

VERSION = "1.1.0"
START_TIME = time.time()
STATE = {"cfg": None, "db": None, "db_error": None}


# ---------------------------------------------------------------- realtime
class EventBus:
    """In-process pub/sub for SSE clients. One queue per subscriber."""

    def __init__(self):
        self._lock = threading.Lock()
        self._clients = set()
        self._seq = 0
        self._seq_lock = threading.Lock()

    def _next_seq(self):
        with self._seq_lock:
            self._seq += 1
            return self._seq

    def subscribe(self):
        q = queue.Queue(maxsize=256)
        with self._lock:
            self._clients.add(q)
        return q

    def unsubscribe(self, q):
        with self._lock:
            self._clients.discard(q)

    def client_count(self):
        with self._lock:
            return len(self._clients)

    def publish(self, event_type, data):
        """Publish to all live SSE clients; slow clients are dropped."""
        ev = {
            "seq": self._next_seq(),
            "type": event_type,
            "time": time.strftime("%Y-%m-%d %H:%M:%S"),
            "data": data,
        }
        with self._lock:
            clients = list(self._clients)
            for q in clients:
                try:
                    q.put_nowait(ev)
                except queue.Full:
                    self._clients.discard(q)
        return ev


EVENT_BUS = EventBus()


def get_db():
    if STATE["db"] is None:
        try:
            STATE["db"] = DB(STATE["cfg"])
            STATE["db_error"] = None
        except Exception as exc:  # keep the HTTP layer alive for diagnostics
            STATE["db"] = None
            STATE["db_error"] = str(exc)
    return STATE["db"]


def api_url(cfg):
    return (cfg.get("api") or {}).get("url", "")


def is_activated(db):
    if not db:
        return False
    try:
        return db.get_setting("activated") == "1"
    except Exception:
        return False


SETUP_FAILURES = {"count": 0, "until": 0.0}


def direct_sql_block(cfg, with_password=False):
    """تنظیمات «اتصال مستقیم اندروید به SQL Server».

    بدون رمز برمی‌گردد مگر with_password=True (که فقط پس از بررسی کد
    راه‌اندازی صدا زده می‌شود). رمز هرگز لاگ/چاپ نمی‌شود.
    """
    ds = (cfg.get("direct_sql") or {})
    if not ds or not ds.get("enabled"):
        return None
    out = {
        "enabled": True,
        "mode": "direct_sql",
        "host": ds.get("host", ""),
        "port": int(ds.get("port") or 1433),
        "database": ds.get("database", ""),
        "login": ds.get("login", ""),
        "encrypt": ds.get("encrypt", "no"),
        "trust_server_certificate": bool(ds.get("trust_server_certificate", True)),
        "application_intent": ds.get("application_intent", "ReadOnly"),
        "password_required": True,
        "setup_path": "/api/direct-sql/setup",
        "token_required": bool(str(ds.get("setup_token") or "").strip()),
    }
    if with_password:
        pw = str(ds.get("password") or "")
        if not pw:
            path = str(ds.get("password_file") or "")
            try:
                with open(path, encoding="utf-8-sig") as fh:
                    pw = fh.read().strip()
            except Exception:
                pw = ""
        out["password"] = pw
        out["password_ready"] = bool(pw)
        out["connection_string"] = (
            "jdbc:jtds:sqlserver://%s,%d/%s;user=%s;password=%s"
            "?useUnicode=true&characterEncoding=UTF-8"
            % (out["host"], out["port"], out["database"], out["login"], pw)
            if pw else ""
        )
    return out


def public_config(cfg):
    db = get_db()
    body = {
        "name": (cfg.get("app") or {}).get("name", "Vizitor"),
        "version": VERSION,
        "api_url": api_url(cfg),
        "activated": is_activated(db),
        "db_engine": (cfg.get("db") or {}).get("engine", "sqlite"),
        "server_time": time.strftime("%Y-%m-%d %H:%M:%S"),
        # real-time capability — the Android app reads this for self-configuration
        "realtime": True,
        "sse_path": "/api/events",
        "features": ["self_config", "sse", "incremental_sync"],
    }
    dsq = direct_sql_block(cfg, with_password=False)
    if dsq:
        body["direct_sql"] = dsq
    return body


def health_report(cfg):
    db = get_db()
    db_status, db_error = "ok", None
    if db:
        try:
            db.ping()
        except Exception as exc:
            db_status, db_error = "error", str(exc)
    else:
        db_status, db_error = "error", STATE["db_error"]
    return {
        "status": "ok",
        "version": VERSION,
        "api_url": api_url(cfg),
        "db": {
            "engine": (cfg.get("db") or {}).get("engine", "sqlite"),
            "status": db_status,
            "error": db_error,
        },
        "activated": is_activated(db),
        "uptime_sec": int(time.time() - START_TIME),
    }


class Handler(BaseHTTPRequestHandler):
    server_version = "VizitorAPI/" + VERSION
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        try:
            line = fmt % args
        except Exception:
            line = str(fmt)
        if "token=" in line:                      # کد راه‌اندازی هرگز لاگ نمی‌شود
            head, _, tail = line.partition("token=")
            rest = tail.split(" ", 1)
            line = head + "token=***" + ((" " + rest[1]) if len(rest) > 1 else "")
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), line))
        sys.stderr.flush()

    # ------------------------------------------------------------- helpers
    def _send(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _body(self):
        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            length = 0
        if length <= 0:
            return {}
        try:
            return json.loads(self.rfile.read(length).decode("utf-8"))
        except Exception:
            return {}

    def _authorized_user(self):
        auth = self.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            return None
        token = auth[7:].strip()
        if not token:
            return None
        db = get_db()
        if not db:
            return None
        try:
            rows = db.q(
                "SELECT u.id, u.username, u.is_admin FROM sessions s "
                "JOIN users u ON u.id = s.user_id WHERE s.token = ?",
                (token,),
            )
            return rows[0] if rows else None
        except Exception:
            return None

    # -------------------------------------------------------------- routes
    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Content-Length", "0")
        self.end_headers()

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path != "/":
            path = path.rstrip("/")
        if path == "/api/events":
            # SSE streams handle their own errors; never wrap in the 500 handler
            self._events_stream()
            return
        try:
            if path in ("", "/"):
                self._send(
                    200,
                    {
                        "name": "Vizitor API",
                        "version": VERSION,
                        "endpoints": [
                            "GET  /api/ping",
                            "GET  /api/config",
                            "GET  /api/health",
                            "GET  /api/activate",
                            "POST /api/activate",
                            "POST /api/login",
                            "GET  /api/visitors",
                            "POST /api/visitors",
                            "GET  /api/visitors/since?after_id=N",
                            "GET  /api/events   (SSE realtime stream)",
                        ],
                    },
                )
            elif path == "/api/ping":
                self._send(200, {"status": "ok", "time": time.time()})
            elif path == "/api/config":
                self._send(200, public_config(STATE["cfg"]))
            elif path == "/api/health":
                self._send(200, health_report(STATE["cfg"]))
            elif path == "/api/activate":
                db = get_db()
                self._send(200, {"activated": is_activated(db)})
            elif path == "/api/direct-sql/setup":
                self._direct_sql_setup()
            elif path == "/api/visitors":
                self._list_visitors()
            elif path == "/api/visitors/since":
                self._list_visitors_since()
            else:
                self._send(404, {"error": "not_found", "path": self.path})
        except BrokenPipeError:
            pass
        except Exception as exc:
            self._send(500, {"error": "internal", "detail": str(exc)})

    def do_POST(self):
        path = self.path.split("?", 1)[0]
        if path != "/":
            path = path.rstrip("/")
        try:
            if path == "/api/activate":
                self._activate()
            elif path == "/api/login":
                self._login()
            elif path == "/api/visitors":
                self._create_visitor()
            else:
                self._send(404, {"error": "not_found", "path": self.path})
        except BrokenPipeError:
            pass
        except Exception as exc:
            self._send(500, {"error": "internal", "detail": str(exc)})

    # ----------------------------------------------------------- endpoints
    def _direct_sql_setup(self):
        """تنظیمات کامل اتصال مستقیم (به‌همراه رمز) — فقط با کد راه‌اندازی.

        کد راه‌اندازی در نصب‌کننده ساخته می‌شود و روی کارت اتصال/QR است.
        اگر در config.json مقدار setup_token خالی باشد و allow_anonymous=true
        باشد، بدون کد هم پاسخ می‌دهد (فقط برای شبکهٔ محلی و به‌خواست مدیر).
        """
        cfg = STATE["cfg"] or {}
        ds = (cfg.get("direct_sql") or {})

        now = time.time()
        if SETUP_FAILURES["count"] >= 10 and now < SETUP_FAILURES["until"]:
            self._send(429, {"ok": False, "error": "too_many_attempts",
                             "detail": "too many wrong setup codes; wait a minute"})
            return

        if not ds or not ds.get("enabled"):
            self._send(404, {"ok": False, "error": "direct_sql_not_enabled"})
            return

        qs = urllib.parse.parse_qs(self.path.split("?", 1)[1] if "?" in self.path else "")
        given = (qs.get("token", [""])[0] or self.headers.get("X-Vizitor-Token", "")).strip()
        expected = str(ds.get("setup_token") or "").strip()
        allow_anon = bool(ds.get("allow_anonymous"))

        if expected:
            if not given or not hmac.compare_digest(expected, given):
                SETUP_FAILURES["count"] += 1
                SETUP_FAILURES["until"] = now + 60
                time.sleep(0.5)
                self._send(403, {"ok": False, "error": "bad_setup_code",
                                 "detail": "setup code is wrong; read it from the connection card"})
                return
        elif not allow_anon:
            self._send(403, {"ok": False, "error": "setup_locked",
                             "detail": "no setup code is configured; ask the administrator"})
            return

        block = direct_sql_block(cfg, with_password=True)
        block["ok"] = True
        self._send(200, block)

    def _activate(self):
        db = get_db()
        if not db:
            self._send(500, {"ok": False, "error": "db_unavailable", "detail": STATE["db_error"]})
            return
        code = str(self._body().get("code", "")).strip()
        if not code:
            self._send(400, {"ok": False, "error": "code_required"})
            return
        stored = db.get_setting("activation_code")
        if not stored:
            db.upsert_setting("activation_code", code)
            db.upsert_setting("activated", "1")
            db.upsert_setting("activated_at", time.strftime("%Y-%m-%d %H:%M:%S"))
            EVENT_BUS.publish("activation.changed", {"activated": True})
            self._send(200, {"ok": True, "activated": True, "message": "first activation saved"})
        elif hmac.compare_digest(stored, code):
            db.upsert_setting("activated", "1")
            EVENT_BUS.publish("activation.changed", {"activated": True})
            self._send(200, {"ok": True, "activated": True})
        else:
            self._send(403, {"ok": False, "activated": False, "error": "invalid_code"})

    def _login(self):
        db = get_db()
        if not db:
            self._send(500, {"ok": False, "error": "db_unavailable", "detail": STATE["db_error"]})
            return
        body = self._body()
        username = str(body.get("username", "")).strip()
        password = str(body.get("password", ""))
        try:
            rows = db.q("SELECT id, username, password_hash, is_admin FROM users WHERE username = ?", (username,))
        except Exception as exc:
            self._send(500, {"ok": False, "error": "db_error", "detail": str(exc)})
            return
        if rows and verify_password(password, rows[0]["password_hash"]):
            token = secrets.token_hex(32)
            db.execute("INSERT INTO sessions (token, user_id) VALUES (?, ?)", (token, rows[0]["id"]))
            self._send(
                200,
                {
                    "ok": True,
                    "token": token,
                    "user": {
                        "id": rows[0]["id"],
                        "username": rows[0]["username"],
                        "is_admin": bool(rows[0]["is_admin"]),
                    },
                },
            )
        else:
            self._send(401, {"ok": False, "error": "bad_credentials"})

    def _gate(self):
        """Common checks for data endpoints; sends an error and returns False on failure."""
        db = get_db()
        if not db:
            self._send(500, {"error": "db_unavailable", "detail": STATE["db_error"]})
            return db, False
        if not is_activated(db):
            self._send(402, {"error": "not_activated", "detail": "send your activation code to POST /api/activate"})
            return db, False
        user = self._authorized_user()
        if not user:
            self._send(401, {"error": "unauthorized", "detail": "login first with POST /api/login"})
            return db, False
        return db, True

    def _list_visitors(self):
        db, ok = self._gate()
        if not ok:
            return
        try:
            if db.engine == "sqlserver":
                rows = db.q(
                    "SELECT TOP (100) id, name, phone, purpose, host_name, created_at "
                    "FROM visitors ORDER BY id DESC"
                )
            else:
                rows = db.q(
                    "SELECT id, name, phone, purpose, host_name, created_at "
                    "FROM visitors ORDER BY id DESC LIMIT 100"
                )
        except Exception as exc:
            self._send(500, {"error": "db_error", "detail": str(exc)})
            return
        self._send(200, {"ok": True, "count": len(rows), "visitors": rows})

    def _list_visitors_since(self):
        """Incremental fetch: visitors with id > after_id (ascending)."""
        db, ok = self._gate()
        if not ok:
            return
        query = self.path.split("?", 1)[1] if "?" in self.path else ""
        params = urllib.parse.parse_qs(query)
        try:
            after_id = int((params.get("after_id") or ["0"])[0])
        except ValueError:
            after_id = 0
        try:
            limit = max(1, min(int((params.get("limit") or ["100"])[0]), 500))
        except ValueError:
            limit = 100
        cols = "id, name, phone, purpose, host_name, created_at"
        try:
            if db.engine == "sqlserver":
                rows = db.q(
                    "SELECT TOP (?) %s FROM visitors WHERE id > ? ORDER BY id ASC" % cols,
                    (limit, after_id),
                )
            else:
                rows = db.q(
                    "SELECT %s FROM visitors WHERE id > ? ORDER BY id ASC LIMIT ?" % cols,
                    (after_id, limit),
                )
        except Exception as exc:
            self._send(500, {"error": "db_error", "detail": str(exc)})
            return
        max_id = rows[-1]["id"] if rows else after_id
        self._send(
            200,
            {
                "ok": True,
                "count": len(rows),
                "visitors": rows,
                "max_id": max_id,
                "has_more": len(rows) == limit,
            },
        )

    def _events_stream(self):
        """Server-Sent Events: pushes visitor.created / activation.changed live."""
        db, ok = self._gate()
        if not ok:
            return
        last_id = 0
        try:
            rows = db.q("SELECT COALESCE(MAX(id), 0) AS m FROM visitors")
            last_id = int(rows[0].get("m", 0)) if rows else 0
        except Exception:
            pass
        q = EVENT_BUS.subscribe()

        def write_event(obj):
            self.wfile.write(("data: %s\n\n" % json.dumps(obj, ensure_ascii=False)).encode("utf-8"))
            self.wfile.flush()

        try:
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream; charset=utf-8")
            self.send_header("Cache-Control", "no-cache, no-transform")
            self.send_header("Connection", "close")
            self.send_header("X-Accel-Buffering", "no")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            # hello: lets the client sync state (then GET /api/visitors/since)
            write_event(
                {
                    "type": "hello",
                    "version": VERSION,
                    "activated": is_activated(db),
                    "last_visitor_id": last_id,
                    "retry_ms": 5000,
                }
            )
            while True:
                try:
                    write_event(q.get(timeout=15))
                except queue.Empty:
                    # periodic keepalive comment (15s)
                    try:
                        self.wfile.write(b": keepalive\n\n")
                        self.wfile.flush()
                    except Exception:
                        break
        except (BrokenPipeError, ConnectionResetError, TimeoutError):
            pass
        except Exception:
            pass
        finally:
            EVENT_BUS.unsubscribe(q)
            self.close_connection = True

    def _create_visitor(self):
        db, ok = self._gate()
        if not ok:
            return
        body = self._body()
        name = str(body.get("name", "")).strip()
        if not name:
            self._send(400, {"ok": False, "error": "name_required"})
            return
        visitor_id = db.execute(
            "INSERT INTO visitors (name, phone, purpose, host_name) VALUES (?, ?, ?, ?)",
            (
                name,
                str(body.get("phone", "")).strip(),
                str(body.get("purpose", "")).strip(),
                str(body.get("host_name", "")).strip(),
            ),
        )
        # push to realtime subscribers (best effort)
        try:
            rows = db.q(
                "SELECT id, name, phone, purpose, host_name, created_at FROM visitors WHERE id = ?",
                (visitor_id,),
            )
            ev = EVENT_BUS.publish("visitor.created", rows[0] if rows else {"id": visitor_id})
        except Exception:
            ev = EVENT_BUS.publish("visitor.created", {"id": visitor_id})
        self._send(201, {"ok": True, "id": visitor_id, "event_seq": ev.get("seq") if ev else None})


def main():
    parser = argparse.ArgumentParser(description="Vizitor API server")
    parser.add_argument("--config", default=None, help="path to config.json")
    args = parser.parse_args()

    cfg_path = args.config or default_config_path()
    try:
        cfg = load_config(cfg_path)
    except Exception as exc:
        sys.stderr.write("FATAL: cannot load config %s: %s\n" % (cfg_path, exc))
        sys.exit(1)
    STATE["cfg"] = cfg

    api = cfg.get("api") or {}
    host = api.get("bind_ip", "0.0.0.0")
    try:
        port = int(api.get("port", 8080))
    except (TypeError, ValueError):
        port = 8080

    try:
        httpd = ThreadingHTTPServer((host, port), Handler)
        httpd.daemon_threads = True  # SSE listener threads die with the process
    except OSError as exc:
        sys.stderr.write("FATAL: cannot bind %s:%s — %s\n" % (host, port, exc))
        sys.exit(1)

    def shutdown(_signum, _frame):
        threading.Thread(target=httpd.shutdown, daemon=True).start()

    signal.signal(signal.SIGTERM, shutdown)
    signal.signal(signal.SIGINT, shutdown)

    sys.stdout.write(
        "Vizitor API v%s starting on %s:%s (config: %s, api_url: %s)\n"
        % (VERSION, host, port, cfg_path, api_url(cfg))
    )
    sys.stdout.flush()
    httpd.serve_forever()
    httpd.server_close()


if __name__ == "__main__":
    main()
