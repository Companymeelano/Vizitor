@echo off
rem ---------------------------------------------------------------
rem  Vizitor - check & repair the local installation
rem  (Persian output comes from install.ps1 itself)
rem ---------------------------------------------------------------
setlocal
set "PS=powershell.exe"
set "HERE=%~dp0"
title Vizitor - recheck
"%PS%" -NoProfile -ExecutionPolicy Bypass -File "%HERE%..\setup\install.ps1" -Recheck
echo.
echo [ done ] press any key to close
pause >nul
