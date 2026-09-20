@echo off
rem Vizitor - open the admin panel / API page in the default browser
rem (reads the non-secret connection file written by install.ps1)
setlocal
set "HERE=%~dp0"
set "URL="
for /f "tokens=2 delims==" %%a in ('findstr /b /i "apiurl=" "%HERE%..\setup\connect.txt" 2^>nul') do set "URL=%%a"
rem آدرس پنل = ریشهٔ سرویس (بدون /api) — پنل مدیریت همان‌جا سرو می‌شود
set "URL=%URL:/api=%"
if "%URL%"=="" set "URL=http://127.0.0.1:9595"
if "%URL:~-1%"=="/" set "URL=%URL:~0,-1%"
start "" "%URL%/"
