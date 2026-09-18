#!/usr/bin/env bash
# ===========================================================================
#  ساخت فایل نصب ویندوز ویزیتور (Vizitor-Setup-x.y.z.exe) با NSIS
#
#  استفاده (لینوکس/CI):
#      ./installer/build.sh                     # اگر makensis در PATH باشد
#      NSISDIR=/opt/nsis ./installer/build.sh   # یا با پوشهٔ NSIS مشخص
#
#  خروجی: release/Vizitor-Setup-<version>.exe
# ===========================================================================
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
NSI="$HERE/vizitor.nsi"

# --- پیدا کردن makensis ----------------------------------------------------
MAKENSIS="${MAKENSIS:-}"
if [ -z "$MAKENSIS" ]; then
  for cand in makensis /usr/bin/makensis /usr/local/bin/makensis; do
    if command -v "$cand" >/dev/null 2>&1; then MAKENSIS="$(command -v "$cand")"; break; fi
  done
fi
if [ -z "$MAKENSIS" ] && [ -n "${NSISDIR:-}" ] && [ -x "$NSISDIR/makensis" ]; then
  MAKENSIS="$NSISDIR/makensis"
fi
if [ -z "$MAKENSIS" ]; then
  echo "[!!] makensis پیدا نشد."
  echo "     ۱) NSIS را نصب کنید (nsis / nsis-data) یا"
  echo "     ۲) پوشهٔ NSIS را با NSISDIR بدهید:  NSISDIR=/opt/nsis $0"
  exit 1
fi

# --- NSISDIR (برای پیدا کردن Include/Stubs/Plugins/Contrib) ----------------
if [ -z "${NSISDIR:-}" ]; then
  for d in /usr/share/nsis /usr/local/share/nsis /opt/nsis; do
    if [ -d "$d/Include" ] && [ -d "$d/Stubs" ]; then export NSISDIR="$d"; break; fi
  done
fi
if [ -z "${NSISDIR:-}" ]; then
  echo "[!!] NSISDIR مشخص نیست (Include/Stubs/Plugins لازم است)."
  echo "     مثال: NSISDIR=/usr/share/nsis $0"
  exit 1
fi
for need in Include Stubs; do
  [ -d "$NSISDIR/$need" ] || { echo "[!!] $NSISDIR/$need نیست — نسخهٔ کامل NSIS لازم است"; exit 1; }
done

mkdir -p "$ROOT/release"
echo "[ok] makensis : $MAKENSIS"
echo "[ok] NSISDIR  : $NSISDIR"
echo "[..] ساخت نصب‌کننده ..."

# اسکریپت‌های PowerShell داخل exe با «Windows PowerShell 5.1» اجرا می‌شوند؛
# آن‌ها فایل بدون BOM را با ANSI می‌خوانند و متن فارسی به‌هم می‌ریزد. پس BOM لازم است.
python3 - <<'PSBOM' || true
for p in ('install.ps1', 'installer/nsi/preflight.ps1'):
    try:
        d = open(p, 'rb').read()
    except OSError:
        print('[!!] پیدا نشد: ' + p)
        continue
    if not d.startswith(b'\xef\xbb\xbf'):
        open(p, 'wb').write(b'\xef\xbb\xbf' + d)
        print('[ok] BOM به ' + p + ' اضافه شد')
    else:
        print('[ok] BOM موجود است: ' + p)
PSBOM

# NSIS فایل .nsi را باید UTF-8 با BOM بخواند (متن فارسی)
python3 - "$NSI" <<'PY' || true
import sys
p = sys.argv[1]
d = open(p, 'rb').read()
if not d.startswith(b'\xef\xbb\xbf'):
    open(p, 'wb').write(b'\xef\xbb\xbf' + d)
    print('[ok] BOM به ' + p + ' اضافه شد')
else:
    print('[ok] BOM موجود است')
PY

( cd "$ROOT" && NSISDIR="$NSISDIR" "$MAKENSIS" "$NSI" )

# --- ثبت sha256 فایل ساخته‌شده (این فایل داخل exe بسته‌بندی نمی‌شود) ---------
EXE="$ROOT/release/Vizitor-Setup-1.0.0.exe"
[ -f "$EXE" ] || EXE="$(ls -1 "$ROOT"/release/Vizitor-Setup-*.exe | head -1)"
if [ -f "$EXE" ]; then
  ( cd "$(dirname "$EXE")" && sha256sum "$(basename "$EXE")" > "$(basename "$EXE").sha256" \
      && echo "[ok] sha256: $(cat "$(basename "$EXE").sha256")" )
fi

ls -la "$ROOT/release"
echo "[ok] فایل نصب ساخته شد."
