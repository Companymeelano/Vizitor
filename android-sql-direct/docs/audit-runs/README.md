# Audit runs — what the operator ran and what came back

These files are the **raw output the operator pasted back** after running a script on
the real server (SSMS, server `MIGHTY`, database `Meelano` unless stated otherwise).
Chat-only content dies with the session, so the workspace keeps a copy.

| file | what produced it | what it answered | state |
|---|---|---|---|
| `out_10_line_tables.txt` | SSMS *Script Table as › SELECT TOP 1000* for three tables | column lists of `subsailfact_pish`, `SubSailSefaresh`, `subsailtemp` **as they exist in the database `Atiran14050603`** | verbatim, 2026-09-18 |
| `out_06_login.txt` | `sql/06_login_probe.sql` (v2) | L1..L7: login source, `PWDCOMPARE`, ConfirmUser/LoginDetails, sal_mali | **not saved verbatim** — only the distilled facts survive (§13.1) |
| `out_05_gaps.txt` | `sql/05_gaps_small.sql` | G1..G5: flags, custgroup tiers, sys_cus/sys_vis mapping, sal_mali | **not saved verbatim** — summary in §15.1 |
| `out_03_bodies.txt` | `sql/03b_bodies_file.sql` / `run_audit.bat` | the four procedure bodies + helper lengths | **not saved verbatim** — behaviour written out in `docs/write-path/ERP-WRITE-PROCEDURES.md` |
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
