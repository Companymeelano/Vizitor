#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Laye' dadegan (database layer) baraye' API Vizitor.
Supported engines: sqlite3 (default, no dependency) | mysql/mariadb (via optional pymysql).
"""
import hashlib
import hmac
import json
import os
import secrets
import sqlite3
import threading


def default_data_dir():
    if os.path.isdir("/var/lib/vizitor") or os.geteuid() == 0:
        return "/var/lib/vizitor"
    return os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "data")


def default_config_path():
    env = os.environ.get("VIZITOR_CONFIG")
    if env:
        return env
    return os.path.join(default_data_dir(), "config.json")


def load_config(path=None):
    path = path or default_config_path()
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_config(cfg, path=None):
    path = path or default_config_path()
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)


def split_statements(sql_text):
    lines = [ln for ln in sql_text.splitlines() if not ln.strip().startswith("--")]
    stmts = []
    for raw in "\n".join(lines).split(";"):
        s = raw.strip()
        if s:
            stmts.append(s)
    return stmts


def hash_password(password, salt=None):
    salt = salt or secrets.token_hex(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 120000)
    return "%s$%s" % (salt, dk.hex())


def verify_password(password, stored):
    if not stored or "$" not in stored:
        return False
    salt, expected = stored.split("$", 1)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 120000)
    return hmac.compare_digest(dk.hex(), expected)


class DB:
    """Thin thread-safe wrapper around sqlite3 or pymysql connections."""

    def __init__(self, cfg):
        self.cfg = cfg or {}
        self.lock = threading.RLock()
        self.conn = None
        self.engine = (self.cfg.get("db") or {}).get("engine", "sqlite")
        if self.engine not in ("sqlite", "mysql"):
            raise ValueError("unsupported db engine: %s" % self.engine)
        self._pymysql = None
        if self.engine == "mysql":
            import pymysql  # noqa: optional dependency

            self._pymysql = pymysql
        self.connect()

    # ------------------------------------------------------------ connection
    def connect(self):
        d = self.cfg.get("db") or {}
        if self.engine == "mysql":
            self.conn = self._pymysql.connect(
                host=d.get("host", "127.0.0.1"),
                port=int(d.get("port", 3306)),
                user=d.get("user", "vizitor"),
                password=d.get("password", ""),
                database=d.get("name", "vizitor"),
                charset="utf8mb4",
                autocommit=True,
                connect_timeout=8,
            )
        else:
            path = d.get("path") or os.path.join(default_data_dir(), "vizitor.db")
            parent = os.path.dirname(path)
            if parent:
                os.makedirs(parent, exist_ok=True)
            self.conn = sqlite3.connect(path, check_same_thread=False)
            self.conn.row_factory = sqlite3.Row
        self.ensure_schema()

    def _schema_file(self):
        base = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        name = "schema_mysql.sql" if self.engine == "mysql" else "schema_sqlite.sql"
        return os.path.join(base, "database", name)

    def ensure_schema(self):
        path = self._schema_file()
        if not os.path.exists(path):
            return
        with open(path, "r", encoding="utf-8") as f:
            text = f.read()
        with self.lock:
            for stmt in split_statements(text):
                self.conn.execute(stmt)
            self.conn.commit()

    # ------------------------------------------------------------- execution
    def _convert(self, sql):
        if self.engine == "mysql":
            return sql.replace("?", "%s")
        return sql

    def q(self, sql, params=()):
        with self.lock:
            cur = self.conn.execute(self._convert(sql), tuple(params))
            self.conn.commit()
            return [dict(r) for r in cur.fetchall()]

    def execute(self, sql, params=()):
        with self.lock:
            cur = self.conn.execute(self._convert(sql), tuple(params))
            self.conn.commit()
            return cur.lastrowid

    def ping(self):
        with self.lock:
            self.conn.execute("SELECT 1")
            return True

    # ------------------------------------------------------------- settings
    def upsert_setting(self, key, value):
        value = str(value)
        if self.engine == "mysql":
            self.execute(
                "INSERT INTO settings (k, v) VALUES (?, ?) "
                "ON DUPLICATE KEY UPDATE v = VALUES(v)",
                (key, value),
            )
        else:
            self.execute(
                "INSERT INTO settings (k, v) VALUES (?, ?) "
                "ON CONFLICT(k) DO UPDATE SET v = excluded.v",
                (key, value),
            )

    def get_setting(self, key):
        rows = self.q("SELECT v FROM settings WHERE k = ?", (key,))
        return rows[0]["v"] if rows else None

    def close(self):
        try:
            if self.conn:
                self.conn.close()
        except Exception:
            pass
