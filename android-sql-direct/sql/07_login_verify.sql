/* ===========================================================================
   Vizitor - audit part 7: prove the app's login condition  -  v1  -  2026-09-18
   READ ONLY, pure ASCII, no BOM, output about 30 lines.
   ---------------------------------------------------------------------------
   WHAT WE KNOW (from the ERP's own code on this server):
     * dbo.SetUserpass reads the password as:  convert(varchar(50), user_password)
     * dbo.ChangeUserPassInSalMali writes it as: user_password = CONVERT(varbinary, @PassWord)
     * the stored value is 1 byte with hex 31, i.e. the plain character "1"
   => this ERP stores the password as plain text inside a varbinary column, so
      SQL Server hashing (PWDCOMPARE) is NOT the mechanism, and the Android app
      compares with CONVERT(varchar(50), user_password) exactly like the ERP does.

   THIS SCRIPT TESTS THAT CONDITION FOR REAL, WITHOUT PRINTING ANY PASSWORD:
     V1 runs the app's exact WHERE clause with a username+password you type once
        at the top of the file, and reports only MATCH / NO MATCH.
     V2 runs the same clause with a deliberately wrong password: it must report
        NO MATCH, otherwise the comparison is not really working.
     V3 checks the second candidate credential store (security.ConfirmUser) and
        security.LoginDetails, so nothing is left unexamined.
     V4 records the fiscal-year database list (dbo.sal_mali) and the accounting
        flag used by dbo.IsAccountingSystemStarted.

   HOW TO RUN: SSMS on [Meelano] (or sqlcmd). Type the password you actually use
   for the ERP user in @testPassword below, press F5, send the Messages tab.
   After running, do NOT save this file.
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor login verification - v1 (2026-09-18)';

/* ---------------------------------------------------------------------------
   FILL IN the username and password you log into the ERP with.
   (Both current users exist with password "1" on this server.)
   --------------------------------------------------------------------------- */
DECLARE @testUser     NVARCHAR(80)  = N'Admin';
DECLARE @testPassword NVARCHAR(128) = N'1';
DECLARE @wrongPassword NVARCHAR(128) = N'definitely-not-the-password-12345';
GO

IF OBJECT_ID(N'tempdb..#o') IS NOT NULL DROP TABLE #o;
CREATE TABLE #o (seq INT IDENTITY(1,1) PRIMARY KEY, line NVARCHAR(MAX));
GO


/* ---- V1) the app's exact login predicate, with the REAL password -------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'V1|MATCH|user_id=' + CAST(u.user_id AS NVARCHAR(6))
            + N'|user=' + ISNULL(u.user_name, N'<null>')
            + N'|fullname=' + ISNULL(u.user_fname, N'') + N' ' + ISNULL(u.user_lname, N'')
            + N'|role_id=' + ISNULL(CAST(u.role_id AS NVARCHAR(6)), N'<null>')
            + N'|active=' + CAST(u.active AS NVARCHAR(2))
            + N'|IsLocked=' + ISNULL(CAST(u.IsLocked AS NVARCHAR(2)), N'null')
            + N'|shmo=' + CAST(u.shmo AS NVARCHAR(6)) + CHAR(10)
FROM dbo.sys_users u
WHERE u.user_name = @testUser
  AND CONVERT(varchar(50), u.user_password) = @testPassword
  AND u.active = 1;

IF LEN(@out) = 0
    SET @out = N'V1|NO MATCH|the predicate returned no row for user ['
             + @testUser + N']. Reasons: wrong password typed at the top of the file,'
             + N' user_name differs, or the comparison differs from the app.' + CHAR(10);

/* ---- V1b) the same query WITHOUT the password condition, as a control --- */
SELECT @out = @out + N'V1b|control|found ' + CAST(COUNT(*) AS NVARCHAR(6))
            + N' row(s) with that user_name (proves the name is right)' + CHAR(10)
FROM dbo.sys_users u WHERE u.user_name = @testUser;

/* ---- V1c) hash-style comparison, for the record ------------------------- */
SELECT @out = @out + N'V1c|PWDCOMPARE_result=' + CAST(PWDCOMPARE(@testPassword, u.user_password) AS NVARCHAR(4))
            + N' (expected 0 - this ERP does not use SQL Server password hashing)' + CHAR(10)
FROM dbo.sys_users u WHERE u.user_name = @testUser;

/* ---- V2) the predicate must REJECT a wrong password -------------------- */
SELECT @out = @out + N'V2|wrong_password_rows=' + CAST(COUNT(*) AS NVARCHAR(6))
            + N' (must be 0, otherwise the comparison is broken)' + CHAR(10)
FROM dbo.sys_users u
WHERE u.user_name = @testUser
  AND CONVERT(varchar(50), u.user_password) = @wrongPassword
  AND u.active = 1;

/* ---- V3) the other candidate credential stores ------------------------- */
SELECT @out = @out + N'V3|security.ConfirmUser|COLS|'
            + STUFF((SELECT N',' + c.name
                       FROM sys.columns c
                      WHERE c.object_id = OBJECT_ID(N'security.ConfirmUser')
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'') + CHAR(10);
SELECT @out = @out + N'V3|security.ConfirmUser|rows=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
FROM security.ConfirmUser;

SELECT @out = @out + N'V3|security.LoginDetails|COLS|'
            + STUFF((SELECT N',' + c.name
                       FROM sys.columns c
                      WHERE c.object_id = OBJECT_ID(N'security.LoginDetails')
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'') + CHAR(10);
SELECT @out = @out + N'V3|security.LoginDetails|rows=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
FROM security.LoginDetails;

/* ---- V4) fiscal-year databases + accounting flag ----------------------- */
SELECT @out = @out + N'V4|sal_mali|COLS|'
            + STUFF((SELECT N',' + c.name
                       FROM sys.columns c
                      WHERE c.object_id = OBJECT_ID(N'dbo.sal_mali')
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'') + CHAR(10);

SELECT @out = @out + N'V4|sal_mali|' + ISNULL(nam_db, N'<null>') + CHAR(10)
FROM dbo.sal_mali;

SELECT @out = @out + N'V4|overal_setting_count=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
FROM dbo.overal_setting;

SELECT @out = @out + N'V4|IsAccountingSystemStarted=' + ISNULL(CAST(dbo.IsAccountingSystemStarted() AS NVARCHAR(10)), N'null')
            + N' (dbo.overal_setting id=67)' + CHAR(10);

INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- print ------------------------------------------------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + line + CHAR(10) FROM #o ORDER BY seq;
IF LEN(@out) = 0 SET @out = N'*** no output - please report this ***' + CHAR(10);

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
DROP TABLE #o;
GO

/* ===========================================================================
   END OF PART 7.
   If V1 says MATCH and V2 says 0, the app's login is proven correct against
   the real database. Send the Messages tab.
   =========================================================================== */
