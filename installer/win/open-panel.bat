@echo off
rem Vizitor - open the admin panel / API page in the default browser
rem (reads the non-secret connection file written by install.ps1)
setlocal
set "HERE=%~dp0"
set "URL="
for /f "tokens=2 delims==" %%a in ('findstr /b /i "apiurl=" "%HERE%..\setup\connect.txt" 2^>nul') do set "URL=%%a"
if "%URL%"=="" set "URL=http://127.0.0.1:9595/api/health"
start "" "%URL%"
