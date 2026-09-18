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

## 2. `dbo.subsailfact_pish` — the LINES (26 columns)

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

⚠️ **The ERP's own INSERT into this table is not known yet.** We know the column
list, not which of them the ERP fills and with what. That is exactly what the body of
`dbo.Edit_sail_pish` (3091 chars) is expected to show — until it arrives, the
line-writing code is written but not enabled.

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
ERP screen builds lines here and a procedure moves them into the real factor tables.
This is a second reason `Edit_sail_pish` (and any sibling that copies from
`subsailtemp`) has to be read before the app writes lines on its own.

## 5. Open question — WHICH database does the app write to?

| evidence | `Meelano` | `Atiran14050603` |
|---|---|---|
| every previous audit ran here | yes | no |
| login probe `V1\|MATCH` (`sys_users`) | yes | not checked |
| `sal_mali` contents | 1 row, `nam_db = Meelano` | not checked |
| `subsailfact_pish` | exists, **0 rows**, 25 columns | exists, `PerPromotion` extra → 26 columns |
| row counts elsewhere | customers 7, inventory 50, sailfact 3 | not checked |

The ERP itself knows about several fiscal-year databases (`ChangeUserPassInSalMali`
walks `select nam_db from dbo.sal_mali`), so "the database" is not necessarily a
constant. Deciding which one the app connects to is an **evidence question**: run
`sql/08_which_db.sql` in SSMS (once with `Meelano` selected in the toolbar, once with
`Atiran14050603`) and compare `last_write` and the row counts.

Design consequence, independent of the answer: the connection layer resolves the
fiscal-year database the same way the ERP does (`sal_mali.nam_db`) instead of
hard-coding a name — then a year change cannot silently point the app at a stale DB.

## 6. Still needed before the line-writing code can be enabled

1. `dbo.Edit_sail_pish` (3091) — the ERP's own line-writing path.
2. `dbo.AddFromAtiranDetailsForVisitors` (843) — how tablet-originated lines are added.
3. `dbo.SelectPriceAndTedvahForushVisitorhaByDate` (1066) — price + available qty.
4. The column names of `dbo.VW_InventoryAnbars`.
5. The answer to §5.
