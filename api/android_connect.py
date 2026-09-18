#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Vizitor — connection card for the Android app  (direct SQL Server mode)

Writes everything the Android app needs so the visitor only types his own
user name and password:

    C:\\ProgramData\\Vizitor\\android_config.json     full card  (has the SQL password, ACL protected)
    <package>\\android-connect.json                   safe card  (no password, has the setup token)
    <package>\\android-connect.txt                    Persian instructions (printable)
    <package>\\android-connect.png / .svg             QR code  (scan -> app configures itself)

It also (with --update-config) records the block in config.json so that the
running API can hand the settings to the app over the LAN:

    GET /api/config                 -> host, port, database, login (no password)
    GET /api/direct-sql/setup?token=<setup_token>  -> the same + password

The SQL password is NEVER printed to the console or written to the safe card.
The QR generator (segno, BSD-3) is vendored under api/vendor/segno.

Usage
    python android_connect.py --config <config.json> [--package-dir <dir>]
        [--erp-db Meelano] [--login vizitor_android] [--password-file <path>]
        [--token <8 chars>|auto|none] [--update-config] [--no-qr]
"""
import argparse
import datetime
import json
import os
import secrets
import string
import sys
import urllib.parse

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "vendor"))

TOKEN_ALPHABET = string.ascii_uppercase + string.digits
SQL_PORT = 1433


def say(msg):
    print(msg, flush=True)


def read_json(path, default=None):
    try:
        with open(path, encoding="utf-8-sig") as fh:
            return json.load(fh)
    except Exception:
        return default


def write_json(path, obj, indent=2):
    d = os.path.dirname(path)
    if d:
        os.makedirs(d, exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(obj, fh, ensure_ascii=False, indent=indent)
        fh.write("\n")


def new_token(n=8):
    return "".join(secrets.choice(TOKEN_ALPHABET) for _ in range(n))


def lan_ip(cfg):
    """The address the Android app must dial: LAN IP first, then public, then host."""
    meta = cfg.get("meta") or {}
    dsql = cfg.get("direct_sql") or {}
    for cand in (dsql.get("host"), meta.get("local_ip"), meta.get("public_ip"),
                 (cfg.get("db") or {}).get("host")):
        cand = (cand or "").strip()
        if cand and cand not in ("127.0.0.1", "localhost", "0.0.0.0"):
            return cand
    return (meta.get("local_ip") or "").strip() or "SERVER-IP"


def api_base(cfg, host):
    url = ((cfg.get("api") or {}).get("url") or "").strip().rstrip("/")
    if url:
        # replace whatever host the installer guessed with the LAN IP the phone can reach
        try:
            parts = urllib.parse.urlsplit(url if "//" in url else "http://" + url)
            port = parts.port
            netloc = host + ((":%d" % port) if port and port != 80 else "")
            return "http://%s%s" % (netloc, parts.path.rstrip("/"))
        except Exception:
            pass
    return "http://%s/api" % host


def build_qr_uri(payload, path_png, path_svg):
    try:
        import segno
    except Exception as exc:
        return None, "segno not available (%s)" % exc
    try:
        qr = segno.make(payload, error="m")
        qr.save(path_png, scale=6, border=3, dark="#12183a", light="#ffffff")
        qr.save(path_svg, scale=6, border=3, dark="#12183a", light="#ffffff")
        return qr, None
    except Exception as exc:
        return None, str(exc)


def main():
    ap = argparse.ArgumentParser(description="Vizitor - Android direct SQL connection card")
    ap.add_argument("--config", required=True, help="config.json written by the installer")
    ap.add_argument("--package-dir", default="", help="folder for the safe card / QR (e.g. C:\\Vizitor\\setup)")
    ap.add_argument("--erp-db", default="", help="ERP database (default: taken from config / Meelano)")
    ap.add_argument("--login", default="", help="restricted SQL login (default: vizitor_android)")
    ap.add_argument("--password-file", default="", help="file that holds the SQL password for that login")
    ap.add_argument("--token", default="auto", help="setup token: 8 chars, 'auto' or 'none'")
    ap.add_argument("--update-config", action="store_true", help="record the direct_sql block in config.json")
    ap.add_argument("--no-qr", action="store_true", help="skip the QR image")
    args = ap.parse_args()

    cfg = read_json(args.config)
    if not isinstance(cfg, dict):
        say("01|FAILED|cannot read config.json: %s" % args.config)
        return 1

    data_dir = os.path.dirname(os.path.abspath(args.config))
    prev = cfg.get("direct_sql") or {}

    host = lan_ip(cfg)
    erp = (args.erp_db or prev.get("database") or "Meelano").strip()
    login = (args.login or prev.get("login") or "vizitor_android").strip()
    pw_file = (args.password_file or prev.get("password_file")
               or os.path.join(data_dir, "android_app_password.txt")).strip()

    password = ""
    if os.path.exists(pw_file):
        try:
            with open(pw_file, encoding="utf-8-sig") as fh:
                password = fh.read().strip()
        except Exception:
            password = ""

    token = args.token
    if token == "auto":
        token = (prev.get("setup_token") or "").strip() or new_token(8)
    elif token in ("none", "-", "off"):
        token = ""

    base = api_base(cfg, host)
    sql_server = "%s,%d" % (host, SQL_PORT)

    block = {
        "enabled": True,
        "mode": "direct_sql",
        "host": host,
        "port": SQL_PORT,
        "database": erp,
        "login": login,
        "password_file": pw_file,
        "password_ready": bool(password),
        "setup_token": token,
        "allow_anonymous": False,
        "encrypt": "no",
        "trust_server_certificate": True,
        "application_intent": "ReadOnly",
        "api_base": base,
        "config_url": base + "/config",
        "setup_url": base + "/direct-sql/setup",
        "updated_at": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "updated_by": "vizitor installer",
    }

    # ---- 1) config.json (the API reads this) -------------------------------
    if args.update_config:
        cfg["direct_sql"] = block
        write_json(args.config, cfg)
        say("02|OK|config.json updated: direct_sql -> %s,%d / %s / %s" % (host, SQL_PORT, erp, login))

    # ---- 2) full card, stays in the protected data folder ------------------
    full = dict(block)
    full["password"] = password
    full["connection_string"] = (
        "jdbc:jtds:sqlserver://%s/%s;user=%s;password=%s;useUnicode=true;characterEncoding=UTF-8"
        % (sql_server, erp, login, "***" if password else "")
    )
    full_path = os.path.join(data_dir, "android_config.json")
    write_json(full_path, full)
    say("03|OK|full card (with SQL password, protected): %s" % full_path)

    # ---- 3) safe card + QR + text, next to the installer -------------------
    safe = dict(block)
    safe["password"] = None
    safe["password_required"] = True
    safe["how_to_get_password"] = (
        "GET %s?token=%s   (from the server LAN)  -  or read the file on the server: %s"
        % (block["setup_url"], token or "<no-token>", pw_file)
    )
    pkg = args.package_dir or os.path.dirname(os.path.abspath(__file__)) + os.sep + ".."
    pkg = os.path.abspath(pkg)

    safe_path = os.path.join(pkg, "android-connect.json")
    write_json(safe_path, safe)
    say("04|OK|safe card (no password): %s" % safe_path)

    # کوتاه و استاندارد: همان پنج مقدار، با کلیدهای یک‌حرفی تا کد QR کم‌چگالی
    # و راحت‌اسکن بماند. معنی کلیدها در کارت متنی آمده است.
    uri = "vizitor://c?" + urllib.parse.urlencode({
        "h": host,                                   # host
        "p": str(SQL_PORT),                          # port
        "d": erp,                                    # database
        "u": login,                                  # user
        "t": token,                                  # setup token
        "a": base,                                   # api base
    })
    pretty = ("vizitor://connect?api=%s&sql=%s&db=%s&user=%s&token=%s"
              % (base, sql_server, erp, login, token))
    text_path = os.path.join(pkg, "android-connect.txt")
    with open(text_path, "w", encoding="utf-8") as fh:
        fh.write("کارت اتصال برنامهٔ اندروید ویزیتور (حالت اتصال مستقیم به SQL Server)\r\n")
        fh.write("=====================================================================\r\n\r\n")
        fh.write("سرور SQL      : %s\r\n" % sql_server)
        fh.write("دیتابیس       : %s\r\n" % erp)
        fh.write("نام کاربری    : %s\r\n" % login)
        fh.write("رمز           : %s\r\n" % (
            "در فایل «%s» روی همین سرور (فقط مدیر)" % pw_file if not password
            else "در فایل «%s» روی همین سرور (به‌صورت خودکار ساخته شد)" % pw_file))
        fh.write("آدرس API      : %s\r\n" % base)
        fh.write("کد راه‌اندازی  : %s\r\n" % (token or "(غیرفعال)"))
        fh.write("\r\nراه ساده (بدون تایپ کردن):\r\n")
        fh.write("  ۱) تصویر QR کنار همین فایل (android-connect.png) را با برنامهٔ اندروید اسکن کنید\r\n")
        fh.write("  ۲) یا در برنامهٔ اندروید فقط نشانی «%s» را وارد کنید؛ بقیهٔ تنظیمات خودکار گرفته می‌شود\r\n" % base)
        fh.write("  ۳) بعد از آن، ویزیتور فقط نام کاربری و کلمهٔ عبور خودش را وارد می‌کند\r\n")
        fh.write("\r\nمتن QR (در صورت نیاز به تایپ دستی):\r\n  %s\r\n" % uri)
        fh.write("  معنی کلیدها: h=سرور، p=پورت، d=دیتابیس، u=کاربر، t=کد راه‌اندازی، a=آدرس API\r\n")
        fh.write("\r\nشکل خوانا (برای مستندسازی برنامهٔ اندروید):\r\n  %s\r\n" % pretty)
        fh.write("\r\nنکته: این اتصال فقط در شبکهٔ محلی باز است. رمز SQL را در اختیار کسی نگذارید.\r\n")
    say("05|OK|Persian connection card: %s" % text_path)

    if not args.no_qr:
        png = os.path.join(pkg, "android-connect.png")
        svg = os.path.join(pkg, "android-connect.svg")
        qr, err = build_qr_uri(uri, png, svg)
        if qr is None:
            say("06|WARN|QR was not generated: %s" % err)
        else:
            say("07|OK|QR code (version %s): %s" % (qr.version, png))
            say("08|OK|QR code vector: %s" % svg)

    say("09|OK|Android app: server %s | database %s | login %s | password %s"
        % (sql_server, erp, login, "ready" if password else "MISSING (run the installer again)"))
    say("10|DONE|connection card ready")
    return 0


if __name__ == "__main__":
    sys.exit(main())
