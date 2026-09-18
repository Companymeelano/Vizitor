@echo off
rem ===========================================================================
rem  ساخت فایل نصب ویزیتور روی ویندوز  (Vizitor-Setup-x.y.z.exe)
rem  نیاز: NSIS 3  (از https://nsis.sourceforge.io/Download نصب کنید)
rem  اجرا:  installer\build.bat
rem  خروجی: release\Vizitor-Setup-1.0.0.exe
rem ===========================================================================
setlocal enabledelayedexpansion
set "HERE=%~dp0"
set "ROOT=%HERE%.."
set "NSI=%HERE%vizitor.nsi"

set "MAKENSIS="
for %%P in (makensis.exe) do if not "%%~$PATH:P"=="" set "MAKENSIS=%%~$PATH:P"
if not defined MAKENSIS if exist "%ProgramFiles(x86)%\NSIS\makensis.exe" set "MAKENSIS=%ProgramFiles(x86)%\NSIS\makensis.exe"
if not defined MAKENSIS if exist "%ProgramFiles%\NSIS\makensis.exe"  set "MAKENSIS=%ProgramFiles%\NSIS\makensis.exe"
if not defined NSISDIR if exist "%ProgramFiles(x86)%\NSIS\Include"   set "NSISDIR=%ProgramFiles(x86)%\NSIS"
if not defined NSISDIR if exist "%ProgramFiles%\NSIS\Include"        set "NSISDIR=%ProgramFiles%\NSIS"

if not defined MAKENSIS (
  echo [!!] makensis.exe not found. Install NSIS 3 and run again.
  echo      Download: https://nsis.sourceforge.io/Download
  pause
  exit /b 1
)

if not exist "%ROOT%\release" mkdir "%ROOT%\release"

echo [ok] makensis : %MAKENSIS%
if defined NSISDIR echo [ok] NSISDIR  : %NSISDIR%
echo [..] building installer ...

pushd "%ROOT%"
if defined NSISDIR set "NSISDIR=%NSISDIR%"
"%MAKENSIS%" "%NSI%"
set "RC=%ERRORLEVEL%"
popd

if not "%RC%"=="0" (
  echo [!!] build failed with code %RC%
  pause
  exit /b %RC%
)
dir /b "%ROOT%\release"
echo [ok] done.
pause
