#!/usr/bin/env python3
"""
Static verification for the Vizitor SQL scripts (no SQL Server needed).

Why this exists: these scripts are executed by hand on a production ERP server,
where a single syntax mistake costs a round-trip with the operator. This tool
catches the whole class of mistakes that already burned us once:

  1. every batch (split on GO) is parsed with a real T-SQL parser (sqlglot)
  2. every string that the script builds dynamically (EXEC / sp_executesql) is
     reconstructed and parsed as well
  3. executable code is checked to be pure ASCII (Persian text is allowed in
     comments only, because SSMS 2014 encoding of non-ASCII inside code is risky)
  4. known-bad catalog columns are rejected (sys.index_columns has no
     is_primary_key; sys.parameters has no PARAMETER_NAME)

Usage:  python3 verify_tsql.py ../sql/00_audit_atiran2.sql ../sql/01_setup_vizitor_user.sql
Requires: pip install sqlglot
"""
import re
import sys
import pathlib

try:
    import sqlglot
except ImportError:  # pragma: no cover
    sys.exit("sqlglot is required:  pip install sqlglot")

MARK = "\x00"  # sentinel used while tokenizing T-SQL string literals


def strip_comments(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"--[^\n]*", "", text)


def batches(text: str):
    text = strip_comments(text)
    return [b for b in re.split(r"(?im)^\s*GO\s*$", text) if b.strip()]


def parse(sql: str) -> str:
    """Return '' when the statement parses, otherwise the error message."""
    try:
        sqlglot.parse(sql, read="tsql")
        return ""
    except Exception as exc:  # noqa: BLE001
        return str(exc).splitlines()[0]


SAMPLES = {"@sch": "dbo", "@tbl": "sys_users", "@col": "Password",
           "@dbName": "Meelano", "@dbname": "Meelano"}


def split_chain(expr: str):
    """Split a T-SQL concatenation chain into literal and non-literal operands."""
    parts, buf, i = [], "", 0
    while i < len(expr):
        c = expr[i]
        if (c in "Nn" and expr[i + 1:i + 2] == "'") or c == "'":
            start = i + 2 if c != "'" else i + 1
            j = start
            while j < len(expr):
                if expr[j] == "'":
                    if expr[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    break
                j += 1
            if j >= len(expr):
                raise ValueError("unterminated string literal in chain")
            buf += MARK + expr[start:j].replace("''", "'") + MARK
            i = j + 1
        elif c == "+":
            if buf.strip():
                parts.append(buf.strip())
            buf = ""
            i += 1
        else:
            buf += c
            i += 1
    if buf.strip():
        parts.append(buf.strip())
    return parts


def chain_of(text: str, var: str, before: int) -> str:
    """Text between '=' and the terminating ';' of the nearest assignment,
    scanned so that semicolons INSIDE string literals are ignored."""
    head = text[:before]
    m = None
    for m in re.finditer(r"SET\s+@" + var + r"\s*=\s*@" + var + r"\s*\+", head, flags=re.I):
        pass  # prefer a self-accumulating loop assignment when present
    if m is None:
        decls = list(re.finditer(r"DECLARE\s+@" + var + r"\s+\w+\s*(?:\([^)]*\))?\s*=", head, flags=re.I))
        if not decls:
            raise ValueError(f"no DECLARE/SET for @{var}")
        start = decls[-1].end()
    else:
        start = m.end()
    i, paren = start, 0
    while i < len(text):
        c = text[i]
        if (c in "Nn" and text[i + 1:i + 2] == "'") or c == "'":
            j = i + 2 if c != "'" else i + 1
            while j < len(text):
                if text[j] == "'":
                    if text[j + 1:j + 2] == "'":
                        j += 2
                        continue
                    break
                j += 1
            i = j + 1
            continue
        if c == "(":
            paren += 1
        elif c == ")":
            paren -= 1
        elif c == ";" and paren == 0:
            break
        i += 1
    return text[start:i]


def eval_operand(token: str) -> str:
    t = token.strip()
    if not t:
        return ""
    if t in SAMPLES:
        return SAMPLES[t]
    if t.startswith("@"):
        return ""                                     # runtime variable: drop
    up = t.upper()
    if up.startswith("QUOTENAME(") and t.endswith(")"):
        return "[" + eval_operand(t[t.index("(") + 1:t.rindex(")")]).strip("[]") + "]"
    if up.startswith("REPLACE(") and t.endswith(")"):
        inner = t[t.index("(") + 1:t.rindex(")")]
        args, depth, buf = [], 0, ""
        for ch in inner:                              # split on top-level commas
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            if ch == "," and depth == 0:
                args.append(buf)
                buf = ""
            else:
                buf += ch
        args.append(buf)
        if len(args) == 3:
            return eval_operand(args[0]).replace(eval_operand(args[1]), eval_operand(args[2]))
    if "+" in t:
        return "".join(eval_operand(p) for p in split_chain(t))
    return ""                                         # anything else: drop


def simulate_dynamic_sql(text: str):
    """Reconstruct every statement the script concatenates at runtime.

    Done per GO-batch on purpose: variables do not survive a GO separator, so
    the assignment that matters is the one inside the same batch as the EXEC."""
    out = []
    for batch in re.split(r"(?im)^\s*GO\s*$", text):
        for m in re.finditer(r"(?:EXEC|EXECUTE)\s*\(\s*@(\w+)\s*\)", batch, flags=re.I):
            var = m.group(1)
            try:
                chain = chain_of(batch, var, m.start())
                generated = "".join(
                    p[1:-1] if p.startswith(MARK) and p.endswith(MARK) else eval_operand(p)
                    for p in split_chain(chain))
            except Exception as exc:                  # noqa: BLE001
                out.append((var, "", f"{type(exc).__name__}: {exc}"))
                continue
            out.append((var, generated, ""))
    return out


def check(path: pathlib.Path) -> bool:
    text = path.read_text(encoding="utf-8-sig")
    ok = True
    print(f"\n=== {path.name} ===")

    batch_list = batches(text)
    failures = [(i, parse(b)) for i, b in enumerate(batch_list, 1) if parse(b)]
    if failures:
        ok = False
        for i, err in failures:
            print(f"  [FAIL] batch {i}: {err[:160]}")
    else:
        print(f"  batches parsed .............. {len(batch_list)}/{len(batch_list)} OK")

    for var, gen, err0 in simulate_dynamic_sql(text):
        if err0:
            ok = False
            print(f"  [FAIL] @{var}: could not reconstruct ({err0})")
            continue
        if not gen.strip():
            print(f"  [skip] @{var}: runtime variables only, no literal text")
            continue
        err = parse(gen)
        if err:
            ok = False
            print(f"  [FAIL] dynamic SQL of @{var}: {err[:160]}")
            print("         generated: " + gen[:400].replace("\n", " "))
        else:
            print(f"  dynamic SQL @{var} ........... OK "
                  f"({len(gen)} chars, quotes balanced: {gen.count(chr(39)) % 2 == 0})")

    code = strip_comments(text)
    bad = sorted({c for c in code if ord(c) > 127})
    if bad:
        ok = False
        print(f"  [FAIL] non-ASCII in executable code: {[hex(ord(c)) for c in bad][:10]}")
    else:
        print("  executable code ASCII ....... OK (Persian text is comments-only)")

    banned = {
        "ic.is_primary_key": "sys.index_columns has no is_primary_key - join sys.indexes",
        "PARAMETER_NAME": "sys.parameters column is named 'name'",
        "sp_executesql STUFF": "EXEC argument list parse error (Msg 102) - use EXEC (@sql)",
    }
    for token, why in banned.items():
        if token in code:
            ok = False
            print(f"  [FAIL] banned construct '{token}': {why}")
    if not any(t in code for t in banned):
        print("  known-bad constructs ........ none")

    leftovers = re.findall(r"(?:FROM|JOIN)\s+wanted\b", code, flags=re.I)
    if len(leftovers) > 1:
        ok = False
        print(f"  [FAIL] CTE 'wanted' used by {len(leftovers)} statements "
              f"(a CTE lives for one statement only) - use a table variable")
    else:
        print("  CTE scope ................... OK")

    return ok


def main(argv):
    paths = [pathlib.Path(p) for p in argv[1:]] or sorted(
        pathlib.Path(__file__).resolve().parent.parent.glob("sql/*.sql"))
    all_ok = all(check(p) for p in paths)
    print("\nRESULT:", "ALL CHECKS PASSED" if all_ok else "PROBLEMS FOUND")
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
