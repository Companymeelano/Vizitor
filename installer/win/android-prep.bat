@echo off
rem ---------------------------------------------------------------
rem  Vizitor - prepare direct Android -> SQL Server access
rem  (limited login vizitor_android + grants + LAN firewall rule)
rem ---------------------------------------------------------------
setlocal
set "HERE=%~dp0"
title Vizitor - android SQL preparation
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%HERE%..\setup\install.ps1" -AndroidPrepOnly
echo.
echo [ done ] press any key to close
pause >nul
