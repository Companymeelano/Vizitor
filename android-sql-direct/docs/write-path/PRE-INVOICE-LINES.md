# The tables that carry a pre-invoice (head, lines, staging)

Sources: live audit of `Meelano` (§4, §5, §13 of `docs/VERIFIED-SCHEMA-Meelano.md`),
the four procedure bodies (`ERP-WRITE-PROCEDURES.md`), and the operator's column-list
paste of 2026-09-18 (`docs/audit-runs/out_10_line_tables.txt`).
Nothing below is guessed: every column name came from one of those three.

## 1. `dbo.sailfact_pish` — the HEAD (already known)

Written by `dbo.add_sail_pish`, whose INSERT names exactly these columns:

```
rdf__, shfacfo, user__, [date], shmo, barbari, shfacthand, vis_rdf, sumlineall,
[all], gainall, tafif, jamtakhgh, done_date, panevis, isret, ismodify, active,
modpar, rdf_sarbarg, rdf_tahbarg, nah_par, mod_darsad_vis, man_gh, sh_f, user_f,
date_f, ted_rooz, taeed, taeedUser, sysid, rejected, tax, avarez
```

`sh_f = 0` means "not invoiced yet"; `AddInvoice` sets `sh_f`, `user_f`, `date_f`.
The confirmation columns (`TaedHesabdari`, `TaedForush`, `UserTaedHesabdari`, …) are
filled by `add_sail_pish` itself when the settings say so.

## 2. `dbo.subsailfact_pish` — the LINES

Keys, from the live audit of `Meelano`: `rdf__` int PK, `shfacfo` bigint PK,
`RDF` int PK → a line is identified by (pre-invoice number, line number).

**Correction after reading `Edit_sail_pish` (2026-09-18):** `subsailfact_pish.rdf__` is
not a free line number, it is the **version** of the head row it belongs to: the ERP's
own view joins `sailfact_pish` and `subsailfact_pish` on `shfacfo` **and** `rdf__`, and
an edit retires the old head (`active='f'`) together with all its lines and inserts a
new head with `rdf__ + 1`. A line is therefore identified by `(shfacfo, rdf__, RDF)`
and must always be written with the `rdf__` of the *current* head row.

| group | columns |
|---|---|
| keys | `rdf__, shfacfo, RDF` |
| what | `SHKA, rdf_anbar` |
| how much | `TEDVAH, TEDJOZ, VAHPRICE, JOZPRICE, LINESUM, LINEGAIN, TEDBASTEBANDI, BASTEBANDI` |
| discount / extra | `PERTAFIF, PERVIS, litakhma, jozgain, Mp` |
| tax & levy | `Tax, Ptax, Avarez, Pavarez` |
| flags | `active char(1), ISRET char(1), amani bit` |
| only in `Atiran14050603` | `PerPromotion` |

Types recorded for `Meelano` (§4): `TEDVAH decimal(18,3)`, `TEDJOZ int NULL`,
`VAHPRICE money`, `JOZPRICE money`, `LINESUM money`, `LINEGAIN money`,
`PERTAFIF decimal(18,2) NULL`, `PERVIS decimal(18,2) NULL`, `litakhma money`,
`Tax money NULL`, `Ptax decimal(18,2) NULL`, `Avarez money NULL`,
`Pavarez decimal(18,2) NULL`, `Mp int NULL`, `jozgain money`, `BASTEBANDI varchar(25)`,
`TEDBASTEBANDI int NULL`, `active char(1)`, `ISRET char(1)`, `amani bit NULL`.

✅ **SOLVED on 2026-09-18 (§7):** the ERP never inserts into this table directly. It
inserts into the staging table `dbo.subsailtemp_pish`, and the enabled INSTEAD OF
trigger `dbo.trig_sst_pish` copies the row here. The column list of that copy is the
authoritative answer to "which columns does a line need".

## 3. `dbo.SubSailSefaresh` — links a line to an order (7 columns)

```
SubSailSefareshRowID, RowIDSefaresh, ExplainValueSefaresh,
subSailRdf_, SubSailShfac, SubSailShka, SubSailRDFKhat
```

Shape: a row ties an order row (`RowIDSefaresh`) to a factor line
(`SubSailShfac` = factor, `SubSailShka` = product, `SubSailRDFKhat` = line serial),
with a free-text explanation. Order integration is not part of v1; this table is
recorded so nothing is invented later.

## 4. `dbo.subsailtemp` — the ERP's staging table (52 columns)

Head fields (`shfacfo, date, done_date, modpar, mod, rdf__, sysid, vis_rdf, UserID`)
plus line fields (`shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice, mohvah,
ted_kol, vah_nam, linesum, pertafif, tafifAghlam, litakhma, naka, amani, invepgh,
vah_w, sood_gh, vis_sahm, sahmtaf, sahm_mod_par, tax, ptax, avarez, PAvarez, Gift,
TafifLine, PerPromotion, PromotionValue, ProductionSeriesID, TEDVAHMain, TEDJOZMain,
MultiPishFactor, TafifPos, TafifNaghd, VarietyID`) and the flags
(`active, isret, bastebandi, tedbastebandi`).

`UserID` + `shfacfo` + head fields is the signature of a per-user staging area: the
ERP screen builds lines here and a trigger moves them into the real factor tables.

**Resolved 2026-09-18:** `dbo.subsailtemp` is the staging table for the lines of a
**real invoice** (`sailfact`), not of a pre-invoice. The enabled INSTEAD OF trigger
`dbo.InvoiceTrigger` on `dbo.subsailtemp` writes *two* tables from every staged row:
`dbo.subsailfact` (Step 1) and `dbo.ka_act` (Step 2, the inventory ledger, `act_id = 20`).
`dbo.subsailtemp_pish` is the sibling staging table for **pre-invoice** lines and is
handled by `dbo.trig_sst_pish` (§7).
## 5. Which database does the app write to? (operator: `Meelano`)

| evidence | `Meelano` | `Atiran14050603` |
|---|---|---|
| every previous audit ran here | yes | no |
| login probe `V1|MATCH` (`sys_users`) | yes | not checked |
| `sal_mali` contents | 1 row, `nam_db = Meelano` | not checked |
| operator statement (2nd time, 2026-09-18) | "the database name is `Meelano`" | — |
| `subsailfact_pish` column count | the audit printed 25 | the operator's paste printed 26 (with `PerPromotion`) |
| row counts elsewhere | customers 7, inventory 50, sailfact 3 | not checked |

`trig_sst_pish` (just read, on the server the operator is using) inserts
`PerPromotion` into `subsailfact_pish`. That is only possible if the line table has the
column, so the "25 columns" line is the odd one out - one query settles it (§8 item 1).

The ERP itself knows about several fiscal-year databases (`ChangeUserPassInSalMali`
walks `select nam_db from dbo.sal_mali`), so "the database" is not necessarily a
constant. Design consequence, independent of the answer: the connection layer resolves
the fiscal-year database the same way the ERP does (`sal_mali.nam_db`) instead of
hard-coding a name - then a year change cannot silently point the app at a stale DB.

## 6. The "still needed" list - state after 2026-09-18

1. `dbo.Edit_sail_pish` (3134) - ✅ read: it retires the old version and never writes lines.
2. `dbo.AddFromAtiranDetailsForVisitors` (955) - ✅ read.
3. `dbo.SelectPriceAndTedvahForushVisitorhaByDate` (1066) - ✅ read: price + available qty.
4. Columns of `dbo.VW_InventoryAnbars` - ✅ read: the app's `stock()` now uses the view.
5. Which database - ✅ operator: `Meelano` (with the one loose end in §5).
6. **The line writer - ✅ solved, §7.**
7. Still open: the exact column list + types of `dbo.subsailtemp_pish`, and one real
   example of the ERP's own numbers - §8.

## 7. THE LINE WRITER — `dbo.trig_sst_pish` on `dbo.subsailtemp_pish` (found 2026-09-18)

`sys.triggers` on the server:

```
name             on_table              is_disabled  is_instead_of_trigger
InvoiceTrigger   subsailtemp           0            1
trig_sst_pish    subsailtemp_pish      0            1
```

The body (verbatim: `docs/audit-runs/out_13_trigger_bodies.txt`) is an
**INSTEAD OF INSERT** trigger, so nothing is ever stored in the staging table itself:
it reads the row(s) from `inserted` and writes `dbo.subsailfact_pish` inside
`begin transaction forosh … commit transaction forosh`.

### 7.1 The route the app must use

```sql
-- one row per line, parameterised - the trigger does the rest
INSERT INTO dbo.subsailtemp_pish
    (mod, rdf__, shfacfo, shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice,
     bastebandi, tedbastebandi, linesum, isret, pertafif, rdf, pervis, litakhma,
     active, amani, Pavarez, Avarez, Ptax, Tax, PerPromotion, modpar)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

### 7.2 `mod` decides where `rdf__` comes from (this is the whole trick)

| `mod` | `rdf__` of the new line | `active` of the new line |
|---|---|---|
| `0` | **taken from the row you insert** (`rdf__` column) | taken from the row you insert |
| `1` | `select max(rdf__) from sailfact_pish where shfacfo = @shfacfo` — i.e. the **live head** | forced `'t'` |

`mod = 1` is the safe choice for the app: the trigger looks the version up itself, so a
line can never be attached to a retired head. `mod = 0` must only be used when the
caller has just read the head's `rdf__` in the same transaction.

### 7.3 What the trigger adds / forces on the line

* `linegain = 0`, `jozgain = 0` (the app must not send them),
* `Mp = modpar` (the staging column `modpar` lands in `subsailfact_pish.Mp`),
* `active = 't'` when `mod = 1`,
* every other value is copied straight from the staging row.

### 7.4 Consequences for the app's write path

* `add_sail_pish` (head) and `trig_sst_pish` (lines) each own a transaction
  (`transaction forosh`) → **three sequential units of work**, never wrapped in an
  outer transaction. Order: head → lines (the head number is needed first).
* On a line failure the head already exists. Compensation options are to be decided by
  the ERP owner; until then the app must retry and report, never leave a silent
  half-invoice. (Recorded as an open item in §8.)
* `Client_invoice_id` idempotency stays client-side (the ERP has no such column).

### 7.5 Reading a pre-invoice back (which view to trust)

| object | join on | usable? |
|---|---|---|
| `dbo.pishfactor_body` | `subsailfact_pish.rdf__ = sailfact_pish.rdf__ AND shfacfo` + `anbars` | ✅ the ERP's own shape; `LineSum_Takhfif_Tax = (LINESUM - litakhma) + (Avarez + Tax)` |
| `dbo.subsailFactPish` | `inventory` only (no head) | ⚠️ shows lines of **all** versions (`kol = LINESUM - litakhma`) |
| `dbo.VW_Taraz_pish` | `shfacfo` only | ❌ mixes versions of an edited pre-invoice |
| `dbo.CalcDetailsPishfactor(@shfacfo)` | — | line **count** of `active='t'` lines |

## 8. Open items after §7 - each one is a question for the server

1. `INFORMATION_SCHEMA.COLUMNS` of `dbo.subsailtemp_pish` (and `dbo.subsailfact_pish`):
   does `Meelano` really carry `PerPromotion` on the line table?
2. Which columns of `dbo.subsailtemp_pish` are `NOT NULL` without a default - the app
   must send exactly those.
3. One real row of the ERP's own numbers (`sailfact` + its `subsailfact` lines), to learn
   the `tedvah` / `tedjoz` / `vahprice` / `jozprice` / `linesum` convention from data
   instead of guessing it.
4. The head parameter values the ERP passes to `add_sail_pish` (`barbari`, `sumlineall`,
   `all`, `gainall`, `jamtakhgh`, `modpar`, `nah_par`, `ted_rooz`, `ph_kh`,
   `mod_darsad_vis`, `rdf_sarbarg`, `rdf_tahbarg`) - no module calls it, so this must
   come from a real head row.
