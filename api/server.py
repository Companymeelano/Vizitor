#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor API — HTTP server (standard library only).
Endpoints (JSON, UTF-8):
  GET  /api/ping      -> health ping (no auth, no db needed)
  GET  /api/config    -> public config for the Android app
  GET  /api/health    -> full health report (db, activation, ...)
  GET  /api/activate  -> current activation state
  POST /api/activate  -> activate with a code  {"code": "..."}
  POST /api/login     -> admin login            {"username": "...", "password": "..."}
  GET  /api/visitors  -> list visitors (bearer token, activated)
  POST /api/visitors  -> create visitor (bearer token, activated)
"""
import argparse
import hmac
import json
import os
import secrets
import signal
import sys
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from db import DB, default_config_path, hash_password, load_config, verify_password  # noqa: E402

VERSION = "1.0.0"
START_TIME = time.time()
STATE = {"cfg": None, "db": None, "db_error": None}


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


def public_config(cfg):
    db = get_db()
    return {
        "name": (cfg.get("app") or {}).get("name", "Vizitor"),
        "version": VERSION,
        "api_url": api_url(cfg),
        "activated": is_activated(db),
        "db_engine": (cfg.get("db") or {}).get("engine", "sqlite"),
        "server_time": time.strftime("%Y-%m-%d %H:%M:%S"),
    }


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
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), fmt % args))
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
            elif path == "/api/visitors":
                self._list_visitors()
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
            self._send(200, {"ok": True, "activated": True, "message": "first activation saved"})
        elif hmac.compare_digest(stored, code):
            db.upsert_setting("activated", "1")
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
            rows = db.q(
                "SELECT id, name, phone, purpose, host_name, created_at "
                "FROM visitors ORDER BY id DESC LIMIT 100"
            )
        except Exception as exc:
            self._send(500, {"error": "db_error", "detail": str(exc)})
            return
        self._send(200, {"ok": True, "count": len(rows), "visitors": rows})

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
        self._send(201, {"ok": True, "id": visitor_id})


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
