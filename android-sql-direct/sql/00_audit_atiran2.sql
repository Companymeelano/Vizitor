/* ═══════════════════════════════════════════════════════════════════════════
   Vizitor — Atiran2 audit  ·  v2  ·  2026-09-18  ·  READ ONLY
   ═══════════════════════════════════════════════════════════════════════════
   This script NEVER writes, NEVER drops, NEVER alters anything.
   It only reads system catalogs (sys.*) plus a few COUNT/MIN/MAX checks.

   WHY v2:
     v1 was one single batch (no GO). One syntax error therefore killed the
     WHOLE script and produced no output at all. v2 is split into batches with
     GO, so every section runs independently — one failure can never hide the
     rest of the audit. v2 also skips (instead of crashing on) objects that do
     not exist in this database, and additional sections were added
     (network config, object-name discovery).

   HOW TO RUN (SSMS on the server):
     1. Connect to the server and select database [Atiran2] in the dropdown
        (the script prints which database it actually ran in — check it).
     2. Ctrl+T  → Results to Text  (much easier to copy back)
        Optional: Tools ▸ Options ▸ Query Results ▸ SQL Server ▸ Results to Text
        → "Maximum number of characters displayed in each column" = 8000.
        (Long view/procedure definitions are ALSO split into 200-character
         rows, so even the default of 256 characters loses nothing.)
     3. F5 (execute) and send back EVERYTHING that is printed.
        Every result row carries a `section` label ("00_SERVER", "01_TABLES",
        "02_COLUMNS", ...), so partial output is still useful — send what you
        get, including any error messages.

   SECTIONS:
     00_SERVER            version / edition / instance / database context
     01_TABLES            every table the app plans to use: exists? schema?
     02_COLUMNS           full column list (type/length/nullable/identity/PK)
     03_VIEWS             real definition of vwVizitor* / *Vizitor* views
     04_PROCS             stored procedures: exists? + parameter signature
     05_PROC_BODIES       procedure bodies, split into 200-char chunks
     06_PASSWORD_COLUMNS  login-password columns: name/type/length only
     06b_PW_SAMPLE        hash scheme evidence: row counts, min/max length and
                          the first 3 characters only (never a full value)
     07_ROW_COUNTS        row count of every relevant table that exists
     08_VIZITOR_OBJECTS   all objects whose name contains "Vizitor"
     09_VIZITOR_COUNTS    approximate row counts of those tables
     10_NAME_SEARCH       name discovery: all objects matching Atiran keywords
                          (vis / masir / pish / sail / anbar / cust / price /
                           forosh / user / role / goal / sys_)
     11_SQL_NETWORK_CONFIG TCP port + LoginMode from the server registry
                          (needs VIEW SERVER STATE; skip if it errors)
   ═══════════════════════════════════════════════════════════════════════════ */

SET NOCOUNT ON;
GO

/* ── database context guard ─────────────────────────────────────────────── */
IF DB_NAME() <> N'Atiran2'
    PRINT N'*** WARNING: the current database is [' + DB_NAME() + N'] but the expected database is [Atiran2].'
        + N' Switch the database dropdown to Atiran2 and run the script again. ***';
ELSE
    PRINT N'OK: running in [Atiran2].';
GO


/* ═══ 00) Server identity ═════════════════════════════════════════════════ */
SELECT '00_SERVER' AS section,
       CAST(SERVERPROPERTY('ServerName')   AS NVARCHAR(128)) AS server_name,
       CAST(SERVERPROPERTY('InstanceName') AS NVARCHAR(128)) AS instance_name,     -- NULL = default instance
       CAST(SERVERPROPERTY('MachineName')  AS NVARCHAR(128)) AS machine_name,
       CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(64)) AS product_version,   -- 12.x = SQL Server 2014
       CAST(SERVERPROPERTY('ProductLevel')   AS NVARCHAR(32)) AS product_level,
       CAST(SERVERPROPERTY('Edition')        AS NVARCHAR(128)) AS edition,
       DB_NAME() AS database_name,
       (SELECT d.compatibility_level FROM sys.databases d WHERE d.database_id = DB_ID()) AS compat_level,
       (SELECT d.collation_name      FROM sys.databases d WHERE d.database_id = DB_ID()) AS db_collation;
GO


/* ═══ 01) Tables the app will use — existence + real schema ═══════════════ */
;WITH wanted AS (
    SELECT name FROM (VALUES
        (N'sys_users'), (N'Roles'), (N'new_cust'), (N'ka_act'),
        (N'CUSTOMERS'), (N'custgroup'), (N'CITYS'), (N'Province'), (N'regions'),
        (N'inventory'), (N'kagroup'), (N'forosh_price'), (N'prizePercent'),
        (N'anbars'), (N'inventory_anbars'),
        (N'sailfact'), (N'subsailfact'), (N'sailfact_pish'), (N'subsailfact_pish'),
        (N'PishDaryaft'), (N'PishDaryaftGetCheck'), (N'PishDaryaftMultiFactor'), (N'PishDaryaftPos'),
        (N'visitors'), (N'sys_vis'), (N'sys_cus'), (N'sys_kal'), (N'sys_anb'),
        (N'masir'), (N'MasirDay'), (N'Visit'), (N'vis_goals'),
        (N'cust_act'), (N'TabletCustomer'), (N'Device'), (N'DeviceLocation'),
        (N'DeviceMessages'), (N'DeviceSettings'), (N'osystems'), (N'Company'),
        (N'getchk'), (N'BANK'), (N'BANK_NAME'), (N'overal_setting')
    ) AS t(name)
)
SELECT '01_TABLES' AS section,
       w.name AS object_name,
       CASE WHEN o.object_id IS NULL THEN 0 ELSE 1 END AS exists_in_db,
       o.type_desc AS object_type,                       -- NULL = not found
       SCHEMA_NAME(o.schema_id) AS object_schema          -- e.g. dbo
FROM wanted w
LEFT JOIN sys.objects o
       ON o.name = w.name
      AND o.type IN (N'U', N'V')
ORDER BY CASE WHEN o.object_id IS NULL THEN 1 ELSE 0 END, w.name;
GO


/* ═══ 02) Full column detail of every existing table above ════════════════ */
SELECT '02_COLUMNS' AS section,
       SCHEMA_NAME(t.schema_id) AS schema_name,
       t.name                   AS table_name,
       c.column_id,
       c.name                   AS column_name,
       ty.name                  AS sql_type,
       c.max_length,
       c.[precision],
       c.[scale],
       c.is_nullable,
       CASE WHEN c.is_nullable = 1 THEN N'NULL' ELSE N'NOT NULL' END AS nullable_txt,
       c.is_identity,
       c.is_computed,
       CASE WHEN EXISTS (SELECT 1
                           FROM sys.index_columns ic
                           JOIN sys.indexes i ON i.object_id = ic.object_id
                                             AND i.index_id  = ic.index_id
                          WHERE ic.object_id = c.object_id
                            AND ic.column_id = c.column_id
                            AND i.is_primary_key = 1) THEN 1 ELSE 0 END AS is_pk
FROM sys.tables t
JOIN sys.columns c  ON c.object_id = t.object_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
WHERE t.name IN (N'sys_users', N'Roles', N'new_cust', N'ka_act',
                 N'CUSTOMERS', N'custgroup', N'CITYS', N'Province', N'regions',
                 N'inventory', N'kagroup', N'forosh_price', N'prizePercent',
                 N'anbars', N'inventory_anbars',
                 N'sailfact', N'subsailfact', N'sailfact_pish', N'subsailfact_pish',
                 N'PishDaryaft', N'visitors', N'sys_vis', N'sys_cus',
                 N'masir', N'MasirDay', N'Visit', N'vis_goals',
                 N'cust_act', N'TabletCustomer', N'osystems', N'Company', N'getchk')
ORDER BY t.name, c.column_id;
GO


/* ═══ 03) Real definition of the Vizitor views ════════════════════════════
   Definitions are split into 200-character rows so nothing is truncated by
   the SSMS output settings. */
SELECT '03_VIEWS' AS section,
       SCHEMA_NAME(v.schema_id) AS schema_name,
       v.name AS view_name,
       num.n  AS chunk_no,
       SUBSTRING(m.definition, (num.n - 1) * 200 + 1, 200) AS def_chunk
FROM sys.views v
JOIN sys.sql_modules m ON m.object_id = v.object_id
CROSS JOIN (SELECT TOP (60) ROW_NUMBER() OVER (ORDER BY object_id) AS n FROM sys.all_objects) num
WHERE (v.name LIKE N'vwVizitor%' OR v.name LIKE N'%Vizitor%')
  AND num.n <= CEILING(LEN(ISNULL(m.definition, N'')) / 200.0)
ORDER BY v.name, num.n;
GO


/* ═══ 04) Atiran stored procedures — existence + parameter signature ══════ */
SELECT '04_PROCS' AS section,
       o.name AS proc_name,
       CASE WHEN p.object_id IS NULL THEN 0 ELSE 1 END AS exists_in_db,
       SCHEMA_NAME(p.schema_id) AS proc_schema,
       (SELECT STUFF((SELECT N', ' + PP.name + N' ' + TYP.name
                            + CASE WHEN TYP.is_user_defined <> 0 THEN N''
                                   WHEN PP.max_length = -1 THEN N'(MAX)'
                                   WHEN TYP.name IN (N'nvarchar', N'nchar')
                                        THEN N'(' + CAST(PP.max_length / 2 AS NVARCHAR(8)) + N')'
                                   WHEN TYP.name IN (N'varchar', N'char', N'varbinary', N'binary')
                                        THEN CASE WHEN PP.max_length IN (0, -1) THEN N''
                                                  ELSE N'(' + CAST(PP.max_length AS NVARCHAR(8)) + N')' END
                                   WHEN TYP.name IN (N'decimal', N'numeric')
                                        THEN N'(' + CAST(PP.precision AS NVARCHAR(3)) + N','
                                                 + CAST(PP.scale     AS NVARCHAR(3)) + N')'
                                   ELSE N'' END
                            + CASE WHEN PP.is_output = 1 THEN N' OUTPUT' ELSE N'' END
                        FROM sys.parameters PP
                        JOIN sys.types TYP ON TYP.user_type_id = PP.user_type_id
                       WHERE PP.object_id = p.object_id
                         AND PP.parameter_id > 0
                       ORDER BY PP.parameter_id
                         FOR XML PATH('')), 1, 2, N'')) AS parameters
FROM (VALUES (N'add_sail_pish'), (N'subsailfact_pish'), (N'AddInvoice'),
             (N'FixMojodi'), (N'sp_add_sail_pish'), (N'svcAddSailFactPish'),
             (N'add_sailfact'), (N'subsailfact'), (N'new_cust'), (N'add_new_cust'),
             (N'VizitorLogin'), (N'sp_VizitorLogin')) o(name)
LEFT JOIN sys.procedures p ON p.name = o.name;
GO


/* ═══ 05) Procedure bodies (200-char chunks, up to 8000 characters) ═══════
   Read-only listing so the real transaction/validation logic can be mirrored
   instead of guessed. */
SELECT '05a_PROC_LIST' AS section,
       SCHEMA_NAME(p.schema_id) AS proc_schema,
       p.name AS proc_name,
       CASE WHEN m.definition IS NULL
            THEN N'<no definition available (encrypted or native)>'
            ELSE CAST(LEN(m.definition) AS NVARCHAR(16)) + N' characters' END AS definition_status
FROM sys.procedures p
LEFT JOIN sys.sql_modules m ON m.object_id = p.object_id
WHERE p.name IN (N'add_sail_pish', N'subsailfact_pish', N'AddInvoice', N'FixMojodi',
                 N'sp_add_sail_pish', N'svcAddSailFactPish', N'add_sailfact',
                 N'subsailfact', N'new_cust', N'add_new_cust')
ORDER BY p.name;

SELECT '05_PROC_BODIES' AS section,
       SCHEMA_NAME(p.schema_id) AS proc_schema,
       p.name AS proc_name,
       num.n  AS chunk_no,
       SUBSTRING(m.definition, (num.n - 1) * 200 + 1, 200) AS body_chunk
FROM sys.procedures p
JOIN sys.sql_modules m ON m.object_id = p.object_id
CROSS JOIN (SELECT TOP (40) ROW_NUMBER() OVER (ORDER BY object_id) AS n FROM sys.all_objects) num
WHERE p.name IN (N'add_sail_pish', N'subsailfact_pish', N'AddInvoice', N'FixMojodi',
                 N'sp_add_sail_pish', N'svcAddSailFactPish', N'add_sailfact',
                 N'subsailfact', N'new_cust', N'add_new_cust')
  AND num.n <= CEILING(LEN(ISNULL(m.definition, N'')) / 200.0)
ORDER BY p.name, num.n;
GO


/* ═══ 06) Login password columns — names/types only, no values ════════════ */
SELECT '06_PASSWORD_COLUMNS' AS section,
       SCHEMA_NAME(t.schema_id) AS schema_name,
       t.name AS table_name,
       c.name AS column_name,
       ty.name AS sql_type,
       c.max_length,
       c.is_nullable
FROM sys.tables t
JOIN sys.columns c  ON c.object_id = t.object_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
WHERE t.name IN (N'sys_users', N'visitors', N'VizitorUsers')
  AND (c.name LIKE N'%pass%' OR c.name LIKE N'%pwd%' OR c.name LIKE N'%hash%')
ORDER BY t.name, c.column_id;
GO


/* ═══ 06b) Hash-scheme evidence: lengths + first 3 characters only ════════
   Needed to make the Android login verify credentials exactly like Atiran
   does (bcrypt $2y$ = 60 chars, md5 = 32 hex, sha1 = 40, sha256 = 64, ...).
   Only row counts, MIN/MAX lengths and the FIRST 3 CHARACTERS are read —
   never a complete value. The statement is built from sys catalog data only,
   so tables that do not exist here are simply skipped. */
DECLARE @pw TABLE (rn INT IDENTITY(1,1) PRIMARY KEY, sch SYSNAME, tbl SYSNAME, col SYSNAME);

INSERT INTO @pw (sch, tbl, col)
SELECT s.name, t.name, c.name
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN sys.columns c ON c.object_id = t.object_id
WHERE t.name IN (N'sys_users', N'visitors', N'VizitorUsers')
  AND (c.name LIKE N'%pass%' OR c.name LIKE N'%pwd%' OR c.name LIKE N'%hash%');

DECLARE @i INT = 1, @n INT, @sch SYSNAME, @tbl SYSNAME, @col SYSNAME;
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @n = COUNT(*) FROM @pw;

WHILE @i <= @n
BEGIN
    SELECT @sch = sch, @tbl = tbl, @col = col FROM @pw WHERE rn = @i;

    IF @i > 1 SET @sql = @sql + N' UNION ALL ';

    SET @sql = @sql
        + N'SELECT N''06b_PW_SAMPLE'' AS section, N'''
        + REPLACE(@sch, N'''', N'''''') + N''' AS table_schema, N'''
        + REPLACE(@tbl, N'''', N'''''') + N''' AS table_name, N'''
        + REPLACE(@col, N'''', N'''''') + N''' AS column_name, '
        + N'(SELECT COUNT(*) FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL) AS non_null_rows, '
        + N'(SELECT MIN(LEN(CAST([' + @col + N'] AS NVARCHAR(300)))) FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL) AS min_len, '
        + N'(SELECT MAX(LEN(CAST([' + @col + N'] AS NVARCHAR(300)))) FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL) AS max_len, '
        + N'(SELECT TOP 1 LEFT(CAST([' + @col + N'] AS NVARCHAR(300)), 3) + N''...'''
        + N' FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL'
        + N' AND LEN(CAST([' + @col + N'] AS NVARCHAR(300))) > 0) AS first3';

    SET @i = @i + 1;
END

IF LEN(@sql) > 0
BEGIN
    BEGIN TRY
        EXEC (@sql);
    END TRY
    BEGIN CATCH
        PRINT N'06b_PW_SAMPLE could not be read: ' + ERROR_MESSAGE();
    END CATCH;
END
ELSE
    PRINT N'06b_PW_SAMPLE: no password-like column found in sys_users / visitors / VizitorUsers.';
GO


/* ═══ 07) Row counts of every relevant table that exists ═════════════════
   Built dynamically so a table that does not exist is skipped with a note
   instead of aborting the section. */
DECLARE @missing NVARCHAR(MAX) = N'';
DECLARE @sql NVARCHAR(MAX) = N'SELECT ''07_ROW_COUNTS'' AS section';

;WITH wanted AS (
    SELECT name FROM (VALUES
        (N'visitors'), (N'sys_vis'), (N'sys_cus'), (N'sys_kal'), (N'sys_anb'),
        (N'masir'), (N'MasirDay'), (N'Visit'), (N'vis_goals'), (N'osystems'),
        (N'CUSTOMERS'), (N'custgroup'), (N'ka_act'), (N'kagroup'),
        (N'inventory'), (N'anbars'), (N'inventory_anbars'), (N'forosh_price'), (N'prizePercent'),
        (N'sailfact'), (N'subsailfact'), (N'sailfact_pish'), (N'subsailfact_pish'),
        (N'PishDaryaft'), (N'PishDaryaftGetCheck'), (N'getchk'), (N'BANK'),
        (N'sys_users'), (N'Roles'), (N'new_cust'), (N'cust_act'),
        (N'TabletCustomer'), (N'Device'), (N'DeviceLocation'), (N'DeviceMessages'), (N'DeviceSettings')
    ) AS t(name)
)
SELECT @sql = @sql
            + N', (SELECT COUNT(*) FROM ' + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name) + N') AS '
            + QUOTENAME(s.name + N'.' + t.name)
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN wanted w      ON w.name = t.name;

SELECT @missing = STUFF((SELECT N', ' + w2.name
                           FROM wanted w2
                          WHERE NOT EXISTS (SELECT 1 FROM sys.tables t2 WHERE t2.name = w2.name)
                          ORDER BY w2.name
                            FOR XML PATH('')), 1, 2, N'');

BEGIN TRY
    EXEC (@sql);
END TRY
BEGIN CATCH
    PRINT N'07_ROW_COUNTS failed: ' + ERROR_MESSAGE();
END CATCH;

IF @missing IS NOT NULL
    PRINT N'07_NOTE - tables not present in this database: ' + @missing;
ELSE
    PRINT N'07_NOTE - every table in the wanted list exists in this database.';
GO


/* ═══ 08) Everything whose name contains "Vizitor" ════════════════════════ */
SELECT '08_VIZITOR_OBJECTS' AS section,
       SCHEMA_NAME(o.schema_id) AS object_schema,
       o.name AS object_name,
       o.type_desc AS object_type
FROM sys.objects o
WHERE o.name LIKE N'%Vizitor%'
  AND o.type IN (N'U', N'V', N'P', N'FN', N'IF', N'TF')
ORDER BY o.type_desc, o.name;
GO


/* ═══ 09) Approximate row counts of the Vizitor tables ════════════════════ */
SELECT '09_VIZITOR_COUNTS' AS section,
       SCHEMA_NAME(t.schema_id) AS object_schema,
       t.name AS table_name,
       CAST(SUM(p.rows) AS BIGINT) AS approx_rows
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
WHERE t.name LIKE N'%Vizitor%'
GROUP BY SCHEMA_NAME(t.schema_id), t.name
ORDER BY t.name;
GO


/* ═══ 10) Name discovery — ground truth for the "no guessing" rule ════════ */
SELECT '10_NAME_SEARCH' AS section,
       SCHEMA_NAME(o.schema_id) AS object_schema,
       o.name AS object_name,
       o.type_desc AS object_type
FROM sys.objects o
WHERE o.type IN (N'U', N'V', N'P', N'FN', N'IF', N'TF')
  AND (o.name LIKE N'%vis%'    OR o.name LIKE N'%masir%'  OR o.name LIKE N'%pish%'
    OR o.name LIKE N'%sail%'   OR o.name LIKE N'%anbar%'  OR o.name LIKE N'%cust%'
    OR o.name LIKE N'%price%'  OR o.name LIKE N'%forosh%' OR o.name LIKE N'%user%'
    OR o.name LIKE N'%role%'   OR o.name LIKE N'%goal%'   OR o.name LIKE N'%sys_%')
ORDER BY o.type_desc, o.name;
GO


/* ═══ 11) SQL Server network / authentication config (registry) ═══════════
   Shows the real TCP port of this instance (answers the named-instance
   question) and LoginMode (2 = mixed SQL + Windows authentication, which is
   required for SQL logins such as vizitor_android).
   Needs VIEW SERVER STATE — if it fails with a permission error, ignore it. */
BEGIN TRY
    SELECT '11_SQL_NETWORK_CONFIG' AS section,
           registry_key,
           value_name,
           CAST(value_data AS NVARCHAR(256)) AS value_data
    FROM sys.dm_server_registry
    WHERE registry_key LIKE N'%SuperSocketNetLib%'
       OR (registry_key LIKE N'%MSSQLServer%' AND value_name IN (N'LoginMode', N'LoginAuditLevel'))
    ORDER BY registry_key, value_name;
END TRY
BEGIN CATCH
    PRINT N'11_SQL_NETWORK_CONFIG skipped: ' + ERROR_MESSAGE();
END CATCH;
GO


/* ═══════════════════════════════════════════════════════════════════════════
   END OF AUDIT.

   Next steps for the implementation (after the output is sent back):
     · confirm/adjust every table and column name against 01_TABLES/02_COLUMNS
     · decide the login verification against 06_PASSWORD_COLUMNS/06b_PW_SAMPLE
     · decide which stored procedures to call (or not) from 04_PROCS/05 bodies
     · then wire SettingsScreen → SecureDbStore + SqlConnectionManager, and
       build the SQL data sources (products / prices / stock / customers).

   Nothing in this script writes, so it is safe to run as often as needed.
   ═══════════════════════════════════════════════════════════════════════════ */
