@echo off
REM  *** WINDOWS BATCH FILE - NEVER OPEN IT OR RUN IT IN SSMS ***
REM  *** In SSMS you only ever run the .sql files in the sql\ folder (00..07). ***
REM  *** This file is double-clicked (or Run as administrator) on Windows.     ***
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
REM       (00..05) - for example C:\vizitor_audit\
REM    2. Right-click this file -> "Run as administrator"
REM    3. When it finishes, the folder contains these files:
REM         out_02_gaps.txt      small, the answers we still need
REM         out_03_bodies.txt    full bodies of add_sail_pish / AddInvoice /
REM                              new_cust / FixMojodi
REM         out_05_small.txt     the short gap list (columns, flags, samples)
REM    4. Send those .txt files back.
REM
REM  Nothing is written to the database: every script is read-only.
REM ===========================================================================

setlocal
cd /d "%~dp0"

set DBSERVER=localhost
set DBNAME=Meelano

where sqlcmd >nul 2>nul
if errorlevel 1 (
    echo.
    echo *** sqlcmd was not found on this computer.
    echo *** Open "SQL Server Command Line Utilities" or run the .sql files in
    echo *** SSMS with Query -^> Results To -^> Results to File (Ctrl+Shift+F)
    echo *** instead, and save each result as a .txt file.
    echo.
    pause
    exit /b 1
)

echo Running Vizitor audits against %DBNAME% on %DBSERVER% ...
echo.

if exist 02_fill_gaps.sql (
    sqlcmd -S %DBSERVER% -d %DBNAME% -E -i 02_fill_gaps.sql -o out_02_gaps.txt -y 0 -W
    echo   -^> out_02_gaps.txt
) else echo   [skip] 02_fill_gaps.sql not found

if exist 03_dump_proc_bodies.sql (
    sqlcmd -S %DBSERVER% -d %DBNAME% -E -i 03_dump_proc_bodies.sql -o out_03_bodies.txt -y 0 -W
    echo   -^> out_03_bodies.txt
) else echo   [skip] 03_dump_proc_bodies.sql not found

if exist 05_gaps_small.sql (
    sqlcmd -S %DBSERVER% -d %DBNAME% -E -i 05_gaps_small.sql -o out_05_small.txt -y 0 -W
    echo   -^> out_05_small.txt
) else echo   [skip] 05_gaps_small.sql not found

if exist 04_port_check.sql (
    sqlcmd -S %DBSERVER% -d %DBNAME% -E -i 04_port_check.sql -o out_04_port.txt -y 0 -W
    echo   -^> out_04_port.txt
) else echo   [skip] 04_port_check.sql not found

echo.
echo Done. Send the out_*.txt files back (they contain no passwords).
echo.
pause
