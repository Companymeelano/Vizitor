/* ===========================================================================
   Vizitor - audit part 6: how does the ERP really verify a login?  - v1
   2026-09-18  -  READ ONLY, pure ASCII, no BOM
   ---------------------------------------------------------------------------
   WHY THIS EXISTS - two hard facts from the live database:

     1) dbo.sys_users.user_password is varbinary(50) but its
        DATALENGTH is only **1 byte** (seen in audit part 5, section G4/G6).
        A real SQL Server password hash is 20..44 bytes, so this column does
        NOT hold a PWDCOMPARE-compatible hash on this server. PWDCOMPARE
        therefore returned 0 for the passwords we tried.
     2) visitors.UserID is NULL, so the user -> visitor link is not there.

   So the app must not guess how the ERP authenticates. This script collects the
   evidence: what the byte contains, which other credential tables exist
   (security.ConfirmUser, EMS.user, ...), and the real code of the ERP's own
   helpers (SetUserpass / SetUsername / GetUser / getEmsUsername).

   Output is small (about 40 lines). Run it in SSMS on [Meelano] and send the
   Messages tab.

   PRIVACY NOTE (read this): the stored password value is 1 byte long. Line
   marked [optional] prints that single byte in hex so we can see whether it is
   a placeholder ('1', 't'), a flag, or real data. If you prefer not to send it,
   delete that one statement - the rest still answers the question.
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor login probe - v1 (2026-09-18)';
GO

DECLARE @out NVARCHAR(MAX) = N'';

/* ---- L1) what is inside user_password? (classification, no value) ------- */
SELECT @out = @out + N'L1|' + CAST(user_id AS NVARCHAR(6))
            + N'|user=' + ISNULL(user_name, N'<null>')
            + N'|bytes=' + ISNULL(CAST(DATALENGTH(user_password) AS NVARCHAR(6)), N'null')
            + N'|shape=' + CASE
                 WHEN user_password IS NULL THEN N'null'
                 WHEN DATALENGTH(user_password) IN (20, 44, 60) THEN N'looks like a password hash'
                 WHEN DATALENGTH(user_password) = 0 THEN N'empty'
                 WHEN DATALENGTH(user_password) = 1 THEN N'single byte - not a hash'
                 ELSE N'other length - not a standard hash' END
            + N'|active=' + ISNULL(CAST(active AS NVARCHAR(2)), N'<null>')
            + N'|locked=' + ISNULL(CAST(IsLocked AS NVARCHAR(2)), N'<null>')
            + N'|role_id=' + ISNULL(CAST(role_id AS NVARCHAR(6)), N'<null>')
            + CHAR(10)
FROM dbo.sys_users
ORDER BY user_id;

/* ---- L2) [optional] the single stored byte, in hex ---------------------- */
SELECT @out = @out + N'L2|' + CAST(user_id AS NVARCHAR(6)) + N'|user=' + ISNULL(user_name, N'<null>')
            + N'|pw_hex=' + ISNULL(CONVERT(NVARCHAR(20), user_password, 2), N'null')
            + N'|as_char=' + ISNULL(CONVERT(NVARCHAR(2), CONVERT(VARCHAR(1), user_password)), N'<null>')
            + CHAR(10)
FROM dbo.sys_users
ORDER BY user_id;

/* ---- L3) other credential stores the ERP may use ----------------------- */
BEGIN TRY
    SELECT @out = @out + N'L3|security.ConfirmUser|cols|'
                + STUFF((SELECT N',' + c.name
                           FROM sys.columns c
                          WHERE c.object_id = OBJECT_ID(N'security.ConfirmUser')
                          ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'') + CHAR(10)
    FROM (SELECT 1) x;
    SELECT @out = @out + N'L3|security.ConfirmUser|rows=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
    FROM security.ConfirmUser;
END TRY
BEGIN CATCH
    SET @out = @out + N'L3|security.ConfirmUser|SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH

BEGIN TRY
    SELECT @out = @out + N'L3|EMS.user|cols|'
                + STUFF((SELECT N',' + c.name
                           FROM sys.columns c
                          WHERE c.object_id = OBJECT_ID(N'EMS.user')
                          ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'') + CHAR(10)
    FROM (SELECT 1) x;
    SELECT @out = @out + N'L3|EMS.user|rows=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
    FROM EMS.user;
END TRY
BEGIN CATCH
    SET @out = @out + N'L3|EMS.user|SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH

/* ---- L4) permission-scope rows of the real users ----------------------- */
SELECT @out = @out + N'L4|sys_vis|' + CAST(SysID AS NVARCHAR(6)) + N'|shvis=' + CAST(shvis AS NVARCHAR(6))
            + N'|UserID=' + CAST(UserID AS NVARCHAR(6)) + CHAR(10)
FROM dbo.sys_vis;

SELECT @out = @out + N'L4|sys_use|' + CAST(SysID AS NVARCHAR(6)) + N'|shuse=' + CAST(shuse AS NVARCHAR(6))
            + N'|UserID=' + CAST(UserID AS NVARCHAR(6)) + CHAR(10)
FROM dbo.sys_use;

/* ---- L5) the ERP's own login helpers (short bodies) -------------------- */
SELECT @out = @out + N'L5|' + o.name + N'|' + ISNULL(LEFT(m.definition, 1500), N'<no definition>') + CHAR(10)
FROM sys.objects o
JOIN sys.sql_modules m ON m.object_id = o.object_id
WHERE o.name COLLATE Latin1_General_CI_AS IN (N'SetUserpass', N'SetUsername', N'GetUser',
        N'getEmsUsername', N'SetSystemName', N'AddUser', N'ChangeUserPassInSalMali',
        N'get_role_id', N'IsAccountingSystemStarted')
ORDER BY o.name;

/* ---- L6) any object whose name hints at login/password ----------------- */
SELECT @out = @out + N'L6|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'.' + o.name
            + N'|' + o.type_desc + CHAR(10)
FROM sys.objects o
WHERE o.type IN (N'U', N'V', N'P', N'FN', N'IF', N'TF')
  AND (o.name COLLATE Latin1_General_CI_AS LIKE N'%pass%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%login%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%confirm%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%auth%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%session%')
ORDER BY o.type_desc, o.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO

/* ===========================================================================
   END OF PART 6. Send the Messages tab (about 40 lines).
   =========================================================================== */
