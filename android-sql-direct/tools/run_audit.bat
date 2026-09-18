@echo off
setlocal
REM  *** THIS IS A WINDOWS BATCH FILE. RUN THE FILE - DO NOT COPY ITS TEXT. ***
REM  *** It is NOT SQL: do not open/run it in SSMS, and do not paste it into ***
REM  *** PowerShell. In File Explorer: right-click -> "Run as administrator". ***
REM ===========================================================================
REM  Vizitor - run the read-only audits on the server and write them to files
REM  ---------------------------------------------------------------------------
REM  WHY THIS FILE EXISTS
REM    The SSMS "Messages" tab drops long output, which is why the earlier
REM    scripts came back cut in half. This file lets SQL Server write the result
REM    straight into text files, and you just attach those files.
REM
REM  HOW TO USE
REM    1. Put this file in the SAME folder as the .sql scripts
REM       (02, 03, 03b, 04, 05) - for example C:\vizitor_audit\
REM    2. In File Explorer right-click this file -> "Run as administrator"
REM       (never copy the text of this file into a console)
REM    3. When it finishes, the folder contains these files:
REM         out_02_gaps.txt      small, the answers we still need
REM         out_03_bodies.txt    full bodies of add_sail_pish / AddInvoice /
REM                              new_cust / FixMojodi
REM         out_05_small.txt     the short gap list (columns, flags, samples)
REM         out_04_port.txt      port + login mode
REM    4. Send those .txt files back.
REM
REM  If sqlcmd is missing, this file tells you what to do instead
REM  (open 03b_bodies_file.sql in SSMS with Ctrl+Shift+F).
REM  Nothing is written to the database: every script is read-only.
REM ===========================================================================

cd /d "%~dp0"

set "DBSERVER=localhost"
set "DBNAME=Meelano"
set "SQLCMD="

where sqlcmd >nul 2>nul
if not errorlevel 1 set "SQLCMD=sqlcmd"
if not defined SQLCMD if exist "%ProgramFiles%\Microsoft SQL Server\Client SDK\ODBC\110\Tools\Binn\sqlcmd.exe" set "SQLCMD=%ProgramFiles%\Microsoft SQL Server\Client SDK\ODBC\110\Tools\Binn\sqlcmd.exe"
if not defined SQLCMD if exist "%ProgramFiles%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe" set "SQLCMD=%ProgramFiles%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe"
if not defined SQLCMD if exist "%ProgramFiles(x86)%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe" set "SQLCMD=%ProgramFiles(x86)%\Microsoft SQL Server\110\Tools\Binn\sqlcmd.exe"
if not defined SQLCMD goto nosqlcmd
goto run

:nosqlcmd
echo.
echo *** sqlcmd was not found on this computer - nothing was run.
echo.
echo *** Two ways to get exactly the same text:
echo ***   A) open 03b_bodies_file.sql in SSMS (on database Meelano),
echo ***      press Ctrl+Shift+F first (Results to File), then F5, and save it
echo ***      as out_03_bodies.txt - that file needs no sqlcmd at all.
echo ***   B) install "SQL Server Command Line Utilities" (or "Client Tools
echo ***      SDK") and run this file again.
echo.
echo *** Checked these places: PATH, Client SDK\ODBC\110, SQL Server\110.
echo.
pause
exit /b 1

:run
echo Running Vizitor audits against %DBNAME% on %DBSERVER% ...
echo.

if exist 02_fill_gaps.sql goto do02
echo   [skip] 02_fill_gaps.sql not found in "%CD%"
goto after02
:do02
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 02_fill_gaps.sql -o out_02_gaps.txt -y 0 -W
echo   -^> out_02_gaps.txt
:after02

if exist 03_dump_proc_bodies.sql goto do03
echo   [skip] 03_dump_proc_bodies.sql not found in "%CD%"
goto after03
:do03
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 03_dump_proc_bodies.sql -o out_03_bodies.txt -y 0 -W
echo   -^> out_03_bodies.txt
:after03

if exist 05_gaps_small.sql goto do05
echo   [skip] 05_gaps_small.sql not found in "%CD%"
goto after05
:do05
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 05_gaps_small.sql -o out_05_small.txt -y 0 -W
echo   -^> out_05_small.txt
:after05

if exist 04_port_check.sql goto do04
echo   [skip] 04_port_check.sql not found in "%CD%"
goto after04
:do04
"%SQLCMD%" -S %DBSERVER% -d %DBNAME% -E -i 04_port_check.sql -o out_04_port.txt -y 0 -W
echo   -^> out_04_port.txt
:after04

echo.
echo Done. Send the out_*.txt files back (they contain no passwords).
echo If a file says "Msg 102" or "Msg 156", send it as it is - do not fix anything.
echo.
pause
