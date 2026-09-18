# Audit runs — what the operator ran and what came back

These files are the **raw output the operator pasted back** after running a script on
the real server (SSMS, server `MIGHTY`, database `Meelano` - the operator confirmed the name
`Meelano` again on 2026-09-18).
Chat-only content dies with the session, so the workspace keeps a copy.

| file | what produced it | what it answered | state |
|---|---|---|---|
| `out_10_line_tables.txt` | SSMS *Script Table as › SELECT TOP 1000* for three tables | column lists of `subsailfact_pish`, `SubSailSefaresh`, `subsailtemp` **as they exist in the database `Atiran14050603`** | verbatim, 2026-09-18 |
| `out_06_login.txt` | `sql/06_login_probe.sql` (v2) | L1..L7: login source, `PWDCOMPARE`, ConfirmUser/LoginDetails, sal_mali | **not saved verbatim** — only the distilled facts survive (§13.1) |
| `out_05_gaps.txt` | `sql/05_gaps_small.sql` | G1..G5: flags, custgroup tiers, sys_cus/sys_vis mapping, sal_mali | **not saved verbatim** — summary in §15.1 |
| `out_03_bodies.txt` | `sql/03b_bodies_file.sql` / `run_audit.bat` | the four procedure bodies + helper lengths | **not saved verbatim** — behaviour written out in `docs/write-path/ERP-WRITE-PROCEDURES.md` |
| `out_11_helper_bodies.txt` | `run_audit.bat` / رراه ۳ (sys.sql_modules) | part 7: the four procedure bodies + 8 helper bodies (`Edit_sail_pish`, `FixManCustomer`, `UpdateMojodiInventory*`, `VW_InventoryAnbars`, `AddFromAtiranDetailsForVisitors`, `SelectPriceAndTedvahForushVisitorhaByDate`) | verbatim, 2026-09-18 |
| `out_12_module_search.txt` | رراه ۷ (`sys.sql_modules ... LIKE`) | part 8: the 12 modules that touch the line tables - two INSTEAD OF triggers, two views, one function, one list proc | verbatim, 2026-09-18 |
| `out_13_trigger_bodies.txt` | رراه ۸ (trigger parents + 8 definitions) | part 9: **the line writer found** - `trig_sst_pish` on `subsailtemp_pish` writes `subsailfact_pish`; `InvoiceTrigger` on `subsailtemp` writes `subsailfact` + `ka_act`; `ListPishFactor` gives the ERP's own pre-invoice list shape | verbatim, 2026-09-18 |
| `out_14_columns_and_samples.txt` | راه ۹ (INFORMATION_SCHEMA + سه فاکتور آخر + جست‌وجوی shpish) | part 10: **all of it from `Atiran14050603`** - the two line tables' full column list with types, three real invoices with their lines (the tedvah/vahprice/linesum convention), and the six modules that touch `sailfact` / `shpish` (AddInvoice 6408 here vs 5850 on Meelano) | verbatim, 2026-09-18 |
| `out_02_gaps.txt` | `sql/02_fill_gaps.sql` | audit part 2: roles, visitor limits, view/proc inventory, column lists | **not saved verbatim** — distilled in §14 |

⚠️ For the four "not saved verbatim" rows the raw text existed only in the chat and is
gone; only the facts that were written into the docs can be relied on. Do not
reconstruct those files from memory — re-run the script if the raw text is needed.

## Re-running anything here

* `.sql` scripts run in **SSMS** (open the file, make sure the toolbar database is the
  one you want, `F5`). Never paste a script's text into a shell.
* `tools/run_audit.bat` runs **as a file** on the server (right-click → *Run as
  administrator*), never in SSMS.
* `tools/*.py` run under **python3** on a machine with Python — never in SSMS.
