/* ═══════════════════════════════════════════════════════════════════════════
   Vizitor — Atiran2 audit (READ-ONLY, safe to run anytime)
   ═══════════════════════════════════════════════════════════════════════════
   Purpose: before wiring the Android app directly to SQL Server, this script
   verifies on the REAL server every object the app will touch, and reports:

     1. Existence + full column list (name/type/nullable/PK) of every table
        used by the new data layer — including sys_users, Roles, new_cust,
        ka_act which are NOT in the shipped codebase model.
     2. The real definitions of the vwVizitor* views (if they exist), so the
        app can be pointed at the same real tables they already use.
     3. Existence + parameter signatures of the Atiran stored procedures the
        invoice flow may call (add_sail_pish, subsailfact_pish, ...).
     4. Password column FORMAT (first 3 chars only — never the value) so the
        login implementation matches the real hash scheme.
     5. Visitor-scope sample counts (sys_vis / sys_cus / masir / MasirDay).
     6. SQL Server edition/version + TLS-relevant info.

   HOW TO RUN:  SSMS → new query on the Atiran2 database (or sqlcmd).
   The script never writes, never drops, never alters anything.
   Copy the result grid back to the Vizitor project (docs/audit-output/) so
   the implementation can be finalized against verified names.
   ═══════════════════════════════════════════════════════════════════════════ */
SET NOCOUNT ON;

/* ── 0) Server identity ─────────────────────────────────────────────────── */
SELECT '00_SERVER' AS section,
       CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(64))  AS product_version,
       CAST(SERVERPROPERTY('ProductLevel')   AS NVARCHAR(16))  AS level_,
       CAST(SERVERPROPERTY('Edition')        AS NVARCHAR(128)) AS edition,
       DB_NAME() AS database_name;

/* ── 1) Tables the app will use — existence, columns, PKs ─────────────────
   Every object from the new data layer contract. "exists" = 1 only if the
   object exists in THIS database. */
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
        (N'getchk'), (N'BANK'), (N'BANK_NAME'), (N'MasirDay'), (N'overal_setting')
    ) AS t(name)
)
SELECT '01_TABLES' AS section,
       w.name,
       CASE WHEN OBJECT_ID(N'dbo.' + w.name) IS NOT NULL THEN 1 ELSE 0 END AS exists_in_db
FROM wanted w
ORDER BY CASE WHEN OBJECT_ID(N'dbo.' + w.name) IS NOT NULL THEN 0 ELSE 1 END, w.name;

/* Column detail for every existing table above (one row per column). */
SELECT '02_COLUMNS' AS section,
       t.NAME          AS table_name,
       c.NAME          AS column_name,
       ty.NAME         AS sql_type,
       c.MAX_LENGTH,
       c.PRECISION, c.SCALE,
       c.IS_NULLABLE,
       CASE WHEN ic.index_column_id IS NOT NULL AND ik.[TYPE] = N'CL' THEN 1 ELSE 0 END AS is_pk
FROM sys.tables t
JOIN sys.columns c  ON c.object_id = t.object_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
LEFT JOIN sys.index_columns ic ON ic.object_id = t.object_id AND ic.column_id = c.column_id AND ic.is_primary_key = 1
LEFT JOIN sys.indexes    ik ON ik.object_id = ic.object_id AND ik.index_id = ic.index_id
WHERE t.NAME IN (N'sys_users', N'Roles', N'new_cust', N'ka_act',
                 N'CUSTOMERS', N'custgroup', N'CITYS', N'Province', N'regions',
                 N'inventory', N'kagroup', N'forosh_price', N'prizePercent',
                 N'anbars', N'inventory_anbars',
                 N'sailfact', N'subsailfact', N'sailfact_pish', N'subsailfact_pish',
                 N'PishDaryaft', N'visitors', N'sys_vis', N'sys_cus',
                 N'masir', N'MasirDay', N'Visit', N'vis_goals',
                 N'cust_act', N'TabletCustomer', N'osystems', N'Company', N'getchk')
ORDER BY t.NAME, c.column_id;

/* ── 2) Real definition of the Vizitor views (if present) ───────────────── */
SELECT '03_VIEWS' AS section,
       v.NAME AS view_name,
       m.definition
FROM sys.views v
JOIN sys.sql_modules m ON m.object_id = v.object_id
WHERE v.NAME IN (N'vwVizitorProducts', N'vwVizitorCustomers', N'vwVizitorSalMali')
   OR v.NAME LIKE N'vwVizitor%';

/* ── 3) Atiran stored procedures — existence + parameters ─────────────────
   If a proc exists here, the app MAY use it (final decision after reading
   the body: the bodies are printed below, truncated to 4000 chars). */
SELECT '04_PROCS' AS section,
       p.NAME AS proc_name,
       CASE WHEN p.object_id IS NOT NULL THEN 1 ELSE 0 END AS exists_in_db,
       STUFF((SELECT ',' + PP.PARAMETER_NAME + ' ' + TYP.NAME
                + CASE WHEN PP.MAX_LENGTH IN (0, -1) THEN N'' ELSE N'(' + CAST(PP.MAX_LENGTH AS NVARCHAR(8)) + N')' END
                + CASE WHEN PP.MODE = 20 THEN N' OUTPUT' ELSE N'' END
             FROM sys.parameters PP JOIN sys.types TYP ON TYP.user_type_id = PP.user_type_id
             WHERE PP.object_id = o.object_id
             ORDER BY PP.parameter_id FOR XML PATH('')), 1, 1, N'') AS parameters
FROM (VALUES (N'add_sail_pish'), (N'subsailfact_pish'), (N'AddInvoice'),
             (N'FixMojodi'), (N'sp_add_sail_pish'), (N'svcAddSailFactPish'),
             (N'add_sailfact'), (N'subsailfact'), (N'new_cust'), (N'add_new_cust')) o(name)
LEFT JOIN sys.procedures p ON p.NAME = o.name;

/* Proc bodies (truncated) — review before direct invocation. */
SELECT '05_PROC_BODIES' AS section,
       p.NAME AS proc_name,
       LEFT(m.definition, 4000) AS body_head
FROM sys.procedures p
JOIN sys.sql_modules m ON m.object_id = p.object_id
WHERE p.NAME IN (N'add_sail_pish', N'subsailfact_pish', N'AddInvoice', N'FixMojodi',
                 N'sp_add_sail_pish', N'svcAddSailFactPish', N'add_sailfact', N'new_cust', N'add_new_cust');

/* ── 4) Login-related: password column FORMAT (no values!) ────────────────
   Reports the column name/type/length plus the pattern of the first stored
   hash (first 3 chars only) so we know the scheme (bcrypt $2y$, md5 32hex,
   sha1 40hex, sha256 64hex, plain...). Built only from system catalog data. */
;WITH pwcols AS (
    SELECT t.NAME AS tbl, c.NAME AS col, ty.NAME AS typ, c.MAX_LENGTH
    FROM sys.tables t
    JOIN sys.columns c ON c.object_id = t.object_id
    JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
    WHERE t.NAME IN (N'sys_users', N'visitors', N'VizitorUsers')
      AND c.NAME IN (N'Password', N'password', N'PasswordHash', N'PassWord', N'pass', N'PWord')
)
SELECT '06_PASSWORD_FORMAT' AS section, tbl, col, typ, MAX_LENGTH
FROM pwcols;

DECLARE @pwSql NVARCHAR(MAX) = N'';
SELECT @pwSql = @pwSql + N'
SELECT N''06b_PW_SAMPLE'' AS section, N''' + tbl + N''' AS tbl, N''' + col + N''' AS col,
       (SELECT TOP 1 LEFT(CAST([' + col + N'] AS NVARCHAR(300)), 3) + N''...''
         FROM dbo.' + QUOTENAME(tbl) + N'
        WHERE [' + col + N'] IS NOT NULL
          AND LEN(CAST([' + col + N'] AS NVARCHAR(300))) > 0) AS sample_first3
UNION ALL
'
FROM pwcols;
IF @pwSql <> N''
    EXEC sp_executesql STUFF(@pwSql, LEN(@pwSql) - 9, 10, N'');

/* ── 5) Visitor scope sample counts ─────────────────────────────────────── */
SELECT '07_SCOPE_COUNTS' AS section,
       (SELECT COUNT(*) FROM dbo.visitors)  AS visitors,
       (SELECT COUNT(*) FROM dbo.sys_vis)   AS sys_vis_rows,
       (SELECT COUNT(*) FROM dbo.sys_cus)   AS sys_cus_rows,
       (SELECT COUNT(*) FROM dbo.masir)     AS masir,
       (SELECT COUNT(*) FROM dbo.MasirDay)  AS masir_day,
       (SELECT COUNT(*) FROM dbo.vis_goals) AS vis_goals,
       (SELECT COUNT(*) FROM dbo.CUSTOMERS) AS customers,
       (SELECT COUNT(*) FROM dbo.inventory) AS inventory,
       (SELECT COUNT(*) FROM dbo.forosh_price) AS forosh_price,
       (SELECT COUNT(*) FROM dbo.inventory_anbars) AS inventory_anbars;

/* ── 6) Existing Vizitor-owned objects (from the PHP install) ───────────── */
SELECT '08_VIZITOR_TABLES' AS section, t.NAME,
       (SELECT COUNT(*) FROM sys.objects o WHERE o.object_id = t.object_id) AS rows_check,
       (SELECT COUNT(*) FROM (SELECT 1) x) AS dummy
FROM sys.tables t
WHERE t.NAME LIKE N'Vizitor%'
ORDER BY t.NAME;

/* Row counts of Vizitor tables */
SELECT '09_VIZITOR_COUNTS' AS section, t.NAME AS table_name,
       CAST(SUM(p.rows) AS BIGINT) AS approx_rows
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0,1)
WHERE t.NAME LIKE N'Vizitor%'
GROUP BY t.NAME
ORDER BY t.NAME;

/* ═══════════════════════════════════════════════════════════════════════════
   END OF AUDIT — save the output (all sections) to:
       viz/docs/audit-output/atiran2-audit-<date>.txt
   Then continue with the implementation phase (connection manager + repos).
   ═══════════════════════════════════════════════════════════════════════════ */
