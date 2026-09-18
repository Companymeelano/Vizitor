# Verified ERP schema — database `Meelano` (live server, 2026-09-18)

Source of truth: the real server output of `sql/00_audit_atiran2.sql` **v5**
(section 00..12). Everything below is observed, not guessed. Nothing here is
invented; where a value is unknown it is marked `?`.

Notation: `name type(len) [NOT NULL|NULL] [PK|IDENTITY]`
(`PK` = part of the primary key, `IDENTITY` = auto increment)

## 0. Server facts

| item | value | consequence for the app |
|---|---|---|
| server_name / machine | `MIGHTY` | |
| instance | **default instance** (no instance name) | confirmed listening on **0.0.0.0:1433** and `[::]:1433` (TSQL, ONLINE) |
| product_version | `12.0.2269.0` (SQL Server **2014**, RTM) | mssql-jdbc must stay on a JDBC-4.0/4.2 compatible build |
| edition | Enterprise Edition (64-bit) | |
| database | `Meelano` | connection string database name |
| compat_level | `120` | |
| db_collation | `SQL_Latin1_General_CP1256_CI_AS` | case-insensitive, Arabic/Persian code page |
| `dbo.CUSTOMERS` probe | found | the database is the ERP database |

## 1. Schemas in use

`dbo` (ERP core), **`Hamrah`** (mobile module: `Visit`, `TabletCustomer`,
`PishDaryaft*`, `Device*`, `backsail_pish`, `subBackSail_pish`),
`warehousing`, `security`, `EMS`, `kg`.

**The mobile/tablet module is `Hamrah`.** Atiran's own tablet app used it; all
those tables are empty (0 rows) on this server.

## 2. Answers to the five open VERIFY items

1. **`new_cust` is a stored procedure**, not a table (`dbo.new_cust`,
   50 parameters, `@id_en` + `@id_en1` OUTPUT). That is why section 07 reported
   it as missing from the table list. There is also `dbo.newcust`,
   `dbo.t_newcust`, and the table `dbo.NCustomers`.
2. **Login**: `dbo.sys_users` = `user_id` (PK, identity), `user_name`,
   `user_password varbinary(50) NOT NULL` → the password is stored as a
   **SQL Server hash** (PWDENCRYPT family), so it must be verified **inside SQL
   Server with `PWDCOMPARE`** — no hash scheme has to be re-implemented in
   Kotlin, and the plaintext never leaves the device (probe script `02`).
3. **No `vwVizitor*` views exist** (section 03/08 empty) — the Vizitor PHP layer
   was never installed on this server, as expected for a first deployment.
4. **Invoice procedures that DO exist**: `dbo.add_sail_pish` (25 params,
   `@id_en OUTPUT`), `dbo.AddInvoice` (39 params, `@id_en OUTPUT`, takes
   `@shpish bigint` = the pre-invoice id), `dbo.FixMojodi (@Shfac, @state)`,
   `dbo.new_cust` (50 params). **Missing**: `add_sailfact`, `subsailfact`,
   `subsailfact_pish`, `sp_add_sail_pish`, `svcAddSailFactPish`.
5. **Password scheme probe**: `sys_users.user_password` is varbinary; the
   `first3` sample is meaningless for binary data (expected). Row counts:
   `sys_users` = 2 rows, `visitors.Password` = 0 non-null values →
   `visitors.Username/Password` is not the login source; **login must be
   `sys_users`** (as the user requires).

## 3. Row counts on the live database (first deployment, confirmed)

| table | rows | | table | rows |
|---|---|---|---|---|
| `dbo.CUSTOMERS` | 7 | | `dbo.sailfact` | 3 |
| `dbo.inventory` | 50 | | `dbo.subsailfact` | 15 |
| `dbo.forosh_price` | 50 | | `dbo.sailfact_pish` | **0** |
| `dbo.kagroup` | 30 | | `dbo.subsailfact_pish` | **0** |
| `dbo.custgroup` | 9 | | `ddo.vis_goals` | 0 |
| `dbo.anbars` | 1 | | `dbo.prizePercent` | 0 |
| `dbo.sys_users` | 2 | | `dbo.visitors` | 1 |
| `dbo.sys_vis` | 1 | | `dbo.masir` / `dbo.MasirDay` | 1 / 0 |
| `dbo.sys_cus` | 7 | | `dbo.getchk`, `dbo.BANK` | 0 / 0 |
| `dbo.sys_kal` | 50 | | **all `Hamrah.*`** | **0** |
| `dbo.sys_anb` | 1 | | | |

→ No visitor has ever produced a pre-invoice: the tables the app will write
(`sailfact_pish`, `subsailfact_pish`) are completely empty.

## 4. Login & identity tables

```
sys_users:  user_id int IDENTITY PK | user_password varbinary(50) NOT NULL |
            user_name varchar(80) NULL | user_lname varchar(30) NOT NULL |
            user_fname varchar(30) NOT NULL | role_id int NULL | skin_id int NULL |
            user_pic image NULL | IsLocked bit NULL | IsLoggedIn bit NULL |
            TafsilCow nchar(10) NULL | TafsilID bigint NULL | phone nvarchar(11) NULL |
            email nvarchar(50) NULL | nationalCode nvarchar(11) NULL |
            address nvarchar(0) NULL | active bit NULL | shmo int NOT NULL |
            BackGroundAddress nvarchar(2000) NULL | IsNotePade bit NULL |
            SysuserTransferCode nvarchar(50) NULL
Roles:      id int IDENTITY PK | name varchar(30) NOT NULL | SubSystemId int NULL
visitors:   vis_rdf int IDENTITY PK | vis_name varchar(30) | vis_addre varchar(80) |
            vis_tell1/tell2/vis_cell varchar(25) | vis_man money | VIs_region int |
            viss_date char(10) | active char(1) | vis_city int NULL | h_sabet money NULL |
            image image NULL | is_supervisor char(1) NULL | supervisor_rdf int NULL |
            supervisor_per decimal(18,3) NULL | dar_z decimal(18,3) NULL | eteb money NULL |
            per_p_d_naghd / per_p_d_check / per_jar_bch decimal(18,3) NULL | kind int NULL |
            tedad_fmmt int NULL | mab_fmmt money NULL | rdf_device int NULL |
            TedadFactorMojazMande int | MablaghMojazMandeJahatFactorha decimal(18,0) |
            Type1 / Type2 bit NULL | Username nvarchar(100) NULL |
            Password nvarchar(100) NULL | UserID int NULL | rdf_device_distribution int NULL
```

**Scope / permission tables** (they define what a visitor may see):

```
sys_vis:  rdf int IDENTITY | SysID int PK | shvis int PK | UserID int
sys_cus:  rdf int IDENTITY | SysID int PK | Shmo int PK | UserID int
sys_kal:  (columns still to capture — script 02)
sys_anb:  (columns still to capture — script 02)
```

`SysID` groups rows per company/OSystem (`osystems`), `UserID` joins to
`sys_users.user_id`; `visitors.UserID` joins a visitor to his `sys_users` row.
So the chain is: login → `sys_users.user_id` → `visitors` (UserID) → `vis_rdf`
→ `sys_vis/sys_cus/sys_kal/sys_anb` (filtered by `SysID`).

## 5. Customers, groups and the Atiran 5-price-tier logic

```
CUSTOMERS:  SHMO int IDENTITY PK | MONAME nvarchar(500) | code nvarchar(250) |
            SHHES nvarchar(200) | BANKNAME/BANKSHOBE nvarchar(300) | addre nvarchar(2000) |
            tell1/tell2/cell nvarchar(200) | active char(1) | cred money | man money |
            peygham1 nvarchar(100) | special char(1) | rdf_city int NULL |
            rdf_region int | group_rdf int | date char(10) NULL | sh_i_m int NULL |
            sharh nvarchar(2550) NULL | vis_rdf int | user_d nvarchar(300) NULL |
            shomare_masir int NULL | defi_vis int NULL | hesab_status int |
            maxopen_time char(10) NULL | check_eteb int NULL | just_naghdi int NULL |
            black_list int NULL | result_m nvarchar(1000) | c_egh/c_mel/c_pos nvarchar(100) NULL |
            kind int NULL | IsEmp int NULL | MaxManFactor int NULL | RDF_masir int NULL |
            Lat/Lng float NULL | TafsilCode nchar(13) NULL | Ecode_Vis int NULL |
            PersonalityType int NULL | EtehadieID int NULL | Shenaseh_Egh nvarchar(100) NULL |
            TafsilID bigint NULL | Username nvarchar(100) NULL | Password nvarchar(500) NULL |
            PriceCheck decimal(18,3) NULL | CheckDateDay int NULL | ShmoMoaref int NULL |
            RoleCode nvarchar(20) NULL | TransferCode nvarchar(50) NULL
custgroup:  group_rdf int IDENTITY PK | group_name varchar(30) | stdate char(10) NULL |
            price int NULL            <-- THE PRICE TIER (1..5) ->
            ted_rooz int NULL | AccType int NULL | MoeinID bigint NULL |
            Active bit NULL | PerGain decimal(18,2) NULL
forosh_price: shka bigint PK | forosh1..forosh5 money NOT NULL | mp1..mp5 int NOT NULL |
            pv1..pv5 decimal(18,3) NOT NULL | naka nvarchar(500) NULL |
            group_rdf int NULL | active char(1) NULL | MinPrice money | MaxPrice money
```

Price resolution (to be implemented exactly, no fallback invented):
`CUSTOMERS.group_rdf → custgroup.price (tier n) → forosh_price.forosh<n>`
with `mp<n>`/`pv<n>` as the same tier's quantity/percent modifiers.
`prizePercent` holds range/date-based promotional percentages
(`Shka`, `FromNum`, `ToNum`, `FromPrice`, `ToPrice`, `FromDate`, `ToDate`,
`Percent`, `RdfCustGroup`, `RdfProvinc/City/Region/Masir`, `SysID`) — **empty
(0 rows)** right now.

## 6. Catalogue, stock and warehouses

```
inventory:  shka bigint IDENTITY PK | naka nvarchar(500) | coka nvarchar(500) |
            group_rdf int | vahsanj nvarchar(300) | mohvah bigint | mojkavah decimal(18,3) |
            mojkajoz int | reopoint int | bastebandi nvarchar(250) | tedbastebandi decimal(18,3) |
            vahwe/vahsp decimal(18,3) | visper decimal(18,3) NULL | active char(1) |
            pure_buy_price/buy_price/inventory_price money | MODPAR int NULL |
            buyjoz money NULL | sharh nvarchar(2000) NULL | min_sef int NULL |
            ted_f_ja/ted_ja int NULL | shka_ja bigint NULL | barbari_vahed money NULL |
            ptax decimal(18,3) NULL | inventory_price_tax money NULL |
            maxtafnaghd decimal(18,3) NULL | black_list int NULL | backfine decimal(18,2) NULL |
            ExpirationDate nchar(10) NULL | FinalSalePrice money NULL |
            PAvarez decimal(18,2) NULL | ImPureBuyPrice money | MaxJozForosh decimal(18,3) |
            PerPos decimal(18,3) NULL | GoodsKindID int NULL | InventoryTypeID int NULL |
            WithProductionSerial bit | ActiveOnlineSales bit | ActiveCapillarySales bit |
            reducePrizeFromSales bit | siteId bigint NULL | nakaEN nvarchar(500) NULL |
            isCustomerClub bit NULL | MandatoryRoleCode bit NULL | TransferCode nvarchar(50) NULL
inventory_anbars: rdf_anbars int PK | shka bigint PK | mojkavah decimal(18,3) |
            mojkajoz int | name nvarchar(250) | tedbastebandi decimal(18,3)   <- per-warehouse stock
anbars:     rdf_anbar int IDENTITY PK | name nvarchar(300) | addre nvarchar(2000) |
            start_date nvarchar(50) | tell1/tell2 nvarchar(50) | anbardar nvarchar(500) NULL |
            Active bit NULL | Base bit NULL | UserID int NULL
kagroup:    group_rdf int IDENTITY PK | group_name nvarchar(500) | stdate datetime |
            CanNegative int NULL | ParentGroupRdf int NULL | GroupLevel int NULL |
            hasPic bit NULL | Active bit | siteId bigint NULL
```

Views available for stock: **`dbo.VW_InventoryAnbars`** (plus
`warehousing.InvantoryAnbar`, `dbo.Inventory_Anbars_PS`).

## 7. Pre-invoice (پیش‌فاکتور), invoice and the mobile module

```
sailfact_pish: rdf__ int PK | shfacfo bigint PK | USER__ varchar(300) | date char(10) |
            shmo int | barbari money NULL | shfacthand nvarchar(500) NULL | vis_rdf int |
            sumlineall money | all money | gainall money | tafif money | jamtakhgh money |
            done_date char(10) | panevis varchar(80) | isret char(1) | ismodify char(1) |
            active char(1) | modpar int | rdf_sarbarg int | rdf_tahbarg int | nah_par int |
            mod_darsad_vis int | nah_d_text text | man_gh money NULL | sh_f bigint NULL |
            user_f varchar(300) NULL | date_f char(10) NULL | ted_rooz int NULL |
            taeed int NULL | taeedUser varchar(300) NULL | sysid int NULL |
            TaedHesabdari bit | TaedForush bit | UserTaedHesabdari nvarchar(500) NULL |
            DateTaedHesabdari nvarchar(10) NULL | UserTaedForush nvarchar(50) NULL |
            DateTaedForush nvarchar(10) NULL | VisitID bigint NULL | Stamp nvarchar(500) NULL |
            Rejected bit NULL | RejectedUser nvarchar(500) NULL | RejectedDate nvarchar(10) NULL |
            RejectedComment nvarchar(1000) NULL | taraz_kh_pish int NULL |
            DateRecive varchar(10) NULL | TimeRecive varchar(20) NULL | tax money NULL |
            avarez money NULL | MpKol int NULL | MpIsAuto bit NULL
subsailfact_pish: rdf__ int PK | shfacfo bigint PK | SHKA bigint | rdf_anbar int |
            TEDVAH decimal(18,3) | TEDJOZ int NULL | VAHPRICE money | JOZPRICE money |
            BASTEBANDI varchar(25) NULL | TEDBASTEBANDI int NULL | LINESUM money |
            LINEGAIN money | ISRET char(1) | PERTAFIF decimal(18,2) NULL | RDF int PK |
            jozgain money | PERVIS decimal(18,2) NULL | litakhma money | active char(1) |
            amani bit NULL | Pavarez decimal(18,2) NULL | Avarez money NULL |
            Ptax decimal(18,2) NULL | Tax money NULL | Mp int NULL
```

`Hamrah` module (all empty): `Visit` (VisitID, VisRdf, Shmo, Duration, Created,
Sent, Description, SentLat/SentLng/SaveLat/SaveLng, SignatureImage varbinary(max)),
`TabletCustomer` (id, vis_rdf, shmo, create_date, birth_date, name, melli_code,
tell1/tell2/cell, address, sharh, estijari, owners_count, metraj_shop,
metraj_yakhchal, yakhchal_count, tablo, sabeghe, Lat, Lng, Active),
`PishDaryaft` (cash receipts, 19 cols incl. VisitorID/SysID/Taeed*/Rejected*),
`PishDaryaftGetCheck/MultiFactor/Pos`, `Device*`, `vishfactor`, `backsail_pish`.

## 8. Procedures available for the write path (verified signatures)

```
dbo.add_sail_pish(@date char(10), @shmo bigint, @barbari money, @tozih varchar(500),
    @vis_rdf int, @sumlineall money, @all money, @gainall money, @tafif money,
    @jamtakhgh money, @done_date char(10), @user varchar(300), @panevis varchar(80),
    @rdf_sarbarg int, @rdf_tahbarg int, @modpar int, @ph_kh int, @mod int,
    @mod_darsad_vis int, @nah_par int, @sh_fac bigint, @id_en bigint OUTPUT,
    @ted_rooz int, @sysid int, @tax money, @avarez money)          -- 3118 chars
dbo.AddInvoice(@username nvarchar(100), @date char(10), @shmo bigint, @barbari money,
    @description nvarchar(500), @vis_rdf int, @sumlineall money, @all money, @tafif money,
    @SumTafifAghlam money, @done_date char(10), @panevis nvarchar(1000), @modpar int,
    @rdf_tahbarg int, @nah_par int, @nah_d_text nvarchar(200), @driver_name varchar(70),
    @rdf_driver int, @mamorp_name varchar(70), @rdf_mamorp int, @bamandeh int,
    @shpish bigint, @batarikh int, @chap_f bit, @chap_h bit, @tax money,
    @moname nvarchar(500), @nahve_namayesh_daryaft int, @vazn decimal(18,2), @avarez money,
    @sysid int, @userid int, @id_en bigint OUTPUT, @VisitorPoorsant money,
    @ShSanadFerestande nvarchar(500), @ExternalCosts decimal(18,0),
    @HajmiOverall decimal(18,0), @chapWithTasvie bit, @chapWithMande bit)   -- 5850 chars
dbo.FixMojodi(@Shfac bigint, @state int)                                    -- 7957 chars
dbo.new_cust(... 50 params ... @id_en OUTPUT, @id_en1 OUTPUT,
    @username nvarchar(100), @password nvarchar(500), @DateCheck int,
    @PriceCheck decimal(18,3), @vis_rdf int, @shomare_masir int, ...)        -- 3868 chars
```

Still to read (script 03 dumps the bodies): `add_sail_pish`, `AddInvoice`,
`new_cust`, `FixMojodi`. Also present and useful:
`dbo.Edit_sail_pish`, `dbo.back_sail`, `dbo.change_price`, `dbo.AddUser`,
`dbo.FixInventoryPrice`, `dbo.UpdateMojodiInventoryAnbars`,
`dbo.ListPishFactor`, `dbo.CustomerListToDate`, `dbo.set_vis_koli`,
`dbo.VisitorSalesCommision`, `dbo.GetVisitorPoints`,
`dbo.AddFromAtiranDetailsForVisitors`.

## 9. Helpful views / functions found (section 10)

Views: `dbo.pishfactors`, `dbo.pishfactor_body`, `dbo.SailFactPish_Details`,
`dbo.subsailFactPish`, `dbo.VW_InventoryAnbars`, `dbo.VW_GoalsVisitors`,
`dbo.VisitorInformation`, `dbo.VisitInfo`, `dbo.Vw_Visit`,
`dbo.VW_CustomerInformation`, `dbo.vw_customer`, `dbo.VW_ListCustomer`,
`dbo.VW_ListPishDaryaft`, `dbo.VwListPishfactorhayeTeadNashodeh`,
`dbo.VW_RowDetailsForosh`, `dbo.VWSailfact`, `dbo.CustactGetchk`,
`dbo.VW_UserAnbar`, `dbo.VW_UserPos`, `dbo.VW_CustomerControl`, `dbo.UsersTasks`.

Functions: `dbo.GetMainMasirID`, `dbo.Func_GetPathByMasirID`,
`dbo.Func_GetVisitorsByVisRdf`, `dbo.vis_name`, `dbo.vis_cell_supervisor`,
`dbo.cust_group_name`, `dbo.cust_vis_name`, `dbo.get_vis_rdf`,
`dbo.get_role_id`, `dbo.CalcDetailsPishfactor`, `dbo.CalcPriceDocument`,
`dbo.CalcPriceFromAtiranDocument`, `dbo.Func_TafifKhalesForosh`,
`dbo.mosh_sp_price`, `dbo.last_price_f_k_mosh`, `dbo.SetUsername`,
`dbo.SetUserpass`, `dbo.SetSystemName`, `dbo.which_panevis`,
`dbo.IsAccountingSystemStarted`, `dbo.GetMainMasirID`.

## 10b. Findings from audit part 5 (2026-09-18) — important corrections

### The char(1) boolean columns hold `'t'`, NOT `'1'`
Section G3 of the live output shows the real values:

```
G3|inventory.active|t|rows=50        G3|CUSTOMERS.active|t|rows=7
G3|forosh_price.active|t|rows=50     G3|visitors.active|t|rows=1
G3|kagroup.Active|0|rows=1           G3|kagroup.Active|1|rows=29
G3|anbars.Active|1|rows=1            G3|custgroup.Active|1|rows=9
```

So `char(1)` flags are `'t'`/`'f'` and `bit` flags are `1`/`0`. **A filter written
as `active = '1'` silently returns 0 rows** — this was a real bug in the first
version of the data source and is now fixed. The guard tool compares the declared
Kotlin constants against `docs/schema/meelano-values.tsv`, so it cannot recur.

### The user → visitor link is `sys_vis`, not `visitors.UserID`
`visitors.UserID` is **NULL** in the live row; the scope table carries the link:

```
G5|visitor|vis_rdf=1|name=ويزيتور سيستم|UserID=<null>|region=1|city=1
G5|sys_vis|SysID=1|shvis=1|UserID=1
G5|sys_cus|SysID=1|Shmo=1..6|UserID=1        (user 1 may serve customers 1..6)
G5|sys_cus|SysID=1|Shmo=7|UserID=2           (user 2 may serve customer 7)
```

Chain: `sys_users.user_id → sys_vis.UserID → sys_vis.shvis → visitors.vis_rdf`.

### Columns of the remaining scope/config tables (G1)

```
sys_kal: rdf, sysid, shka, UserID          sys_anb: rdf, SysID, shanb, UserID
sys_vis: rdf, SysID, shvis, UserID         sys_cus: rdf, SysID, Shmo, UserID
sys_use: rdf, SysID, shuse, UserID         sys_wor: rdf, SysID, shwor, UserID
systems: rdf, name, active                 AnbarDifferent: RowID, Shka, SanadNo, Kind,
                                           AnbarID, JozPrice, TedVahOld, TedJozOld,
                                           TedVahCounted, TedJozCounted, Active
NCustomers: RowID, NShmo, NMan, NDate, BlackList
```

### Live configuration data (small, first deployment)

* company/OSystem: `rdf=1`, name `مديريت`, warehouse 1, active
* visitor: `vis_rdf=1` «ويزيتور سيستم», active, region 1, city 1, no supervisor,
  commission percentages 0
* customer groups: 9 rows, **all with `price = 1`** → tier 1 for everybody today
  (`مشتريان`, `تامين کنندگان`, `ويزيتورها`, `مامورين پخش`, `مامورين مطالبات`,
  `راننده ها`, `پرسنل دفتري`, `كارگران`, `بنکداران`)
* customers: 7 rows — `ويزيتور سيستم` (group 3), `مشتری آنلاین`, `ایلیا پخش`,
  `بازرگانی موسوی مقدم`, `پخش درخشان`, `پخش بلدی`, `امین لیاقت`; balances (`man`)
  0 / 0 / 5.8M / 0 / 80M / 0 / 25M, `black_list = 0` for all
* route: 1 row (`مسير سيستم`)

### LOGIN IS NOT SOLVED YET — and here is exactly why

```
G4|sys_users|id=1|name=Admin|hash_bytes=1|active=1|pwdcompare_result=0
G4|sys_users|id=2|name=مدير |hash_bytes=1|active=1|pwdcompare_result=0
```

`user_password` is **1 byte long**, so it is not a SQL Server password hash and
`PWDCOMPARE` cannot be the mechanism. (The 0 result is also expected because the
probe placeholder was sent instead of a real password.) The app must therefore
not guess; `sql/06_login_probe.sql` collects the evidence: the classification of
that byte, the other candidate credential stores (`security.ConfirmUser`,
`EMS.user`, `dbo.sys_use`), and the real bodies of the ERP helpers
`SetUserpass` / `SetUsername` / `GetUser` / `getEmsUsername`.

## 10. Still open (filled by scripts 02 and 03)

* ~~real TCP port~~ → **RESOLVED: 1433** (`sys.dm_tcp_listener_states` shows
  `0.0.0.0:1433` + `[::]:1433`, state ONLINE, type TSQL). The `1434` seen in the
  registry belongs to the DAC/admin connection (`AdminConnection\Tcp`), not to the
  application endpoint. The app therefore connects to `192.168.1.150:1433`.
* columns of `sys_kal` / `sys_anb`
* whether `PWDCOMPARE` accepts the stored `sys_users.user_password` hashes
* content of the tiny configuration tables (`osystems`, `Roles`, `visitors`,
  `sys_vis`, `sys_cus`, `sys_kal`, `sys_anb`, `custgroup`, `anbars`)
* full bodies of `add_sail_pish`, `AddInvoice`, `new_cust`, `FixMojodi`
  (they define the exact pre-invoice/invoice transaction flow the app must
  follow), and the short bodies of `SetUserpass` / `GetUser` / `get_vis_rdf`


## 11. Evidence about the write path (from the partial part-3 output)

Only the 4 `LENGTH=` header lines and the last chunk of `dbo.new_cust` came back
(the SSMS Messages tab drops long output), but that last chunk is informative:

```
P|new_cust|20|,@a
   end
   exec FixManCustomer @a
commit transaction t1
end
```

So `dbo.new_cust` **opens its own transaction (`t1`), calls `dbo.FixManCustomer`
inside it and commits** — the app must not wrap it in another transaction, and
must treat the returned `@id_en` as the new customer number. Full bodies are
still needed; `tools/run_audit.bat` writes them to a file without truncation.

## 12. Environments observed while auditing (useful for troubleshooting)

* Messages-tab output is silently dropped past a few thousand characters on this
  SSMS installation → long dumps must go to a file (`sqlcmd -o`, or
  `tools/run_audit.bat`, or SSMS Ctrl+Shift+F "Results to File").
