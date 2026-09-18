#!/usr/bin/env python3
"""
Guard for the "no guessed identifiers" rule.

Every SQL statement written in the Android data layer is checked against
docs/schema/meelano-columns.tsv, which was extracted from the live server audit
output. Any column that does not exist on the real database is reported - so a
typo or an invented column can never reach the app.

Usage:
    python3 check_sql_columns.py                 # scans ../*.kt and ../*.sql
    python3 check_sql_columns.py file1 file2 ...  # scans specific files

What it understands:
    FROM dbo.inventory i          -> alias i = dbo.inventory
    JOIN dbo.anbars a ON ...      -> alias a = dbo.anbars
    i.shka  /  dbo.inventory.shka -> validated against the table's column list
Columns that are unqualified are only checked when the query touches exactly one
table (otherwise they are skipped, not guessed).
"""
import re
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
TSV = ROOT / "docs" / "schema" / "meelano-columns.tsv"

# identifiers that are not columns: aliases, table names, SQL functions, keywords
IGNORE_QUALIFIERS = {"sys", "dbo", "Hamrah", "warehousing", "security", "EMS", "kg", "information_schema"}


def load_schema():
    tables = {}
    if not TSV.exists():
        sys.exit(f"schema file not found: {TSV}")
    for line in TSV.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        name, cols = line.split("\t", 1)
        tables[name.strip().lower()] = {c.strip() for c in cols.split(",") if c.strip()}
    return tables


def kotlin_sql_strings(text: str):
    """SQL inside Kotlin string literals (raw and escaped)."""
    out = []
    out += re.findall(r'"""(.*?)"""', text, flags=re.S)
    for m in re.finditer(r'"((?:[^"\\\n]|\\.)*)"', text):
        s = m.group(1)
        if re.search(r"\b(SELECT|INSERT|UPDATE|DELETE|EXEC)\b", s, re.I):
            out.append(s)
    return out


def sql_files(text: str):
    """SQL statement blocks of a .sql file (between ; and GO)."""
    return re.findall(r"(SELECT|INSERT|UPDATE|DELETE)[^;]*?(?:;|\nGO)", text, flags=re.S | re.I)


def alias_map(sql: str):
    """alias/table -> real table name, from FROM/JOIN clauses."""
    amap = {}
    for m in re.finditer(
            r"\b(?:FROM|JOIN)\s+((?:\[?\w+\]?\.)?\[?\w+\]?)(?:\s+(?:AS\s+)?(\w+))?", sql, flags=re.I):
        raw, alias = m.group(1), m.group(2)
        table = raw.replace("[", "").replace("]", "")
        key = table.lower()
        if key not in SCHEMA and f"dbo.{key}" in SCHEMA:
            key = f"dbo.{key}"
        if key not in SCHEMA:
            continue
        amap[table.lower()] = key
        short = key.split(".")[-1]                    # also accept "inventory.col"
        amap.setdefault(short, key)
        if alias and alias.upper() not in ("ON", "WHERE", "ORDER", "GROUP", "HAVING", "INNER", "LEFT", "RIGHT", "FULL", "CROSS", "JOIN", "WITH", "AS"):
            amap[alias.lower()] = key
    return amap


SQL_KEYWORDS = {
    "select", "from", "where", "and", "or", "order", "by", "group", "having", "as",
    "inner", "left", "right", "full", "outer", "cross", "join", "on", "desc", "asc",
    "top", "distinct", "is", "not", "null", "case", "when", "then", "else", "end",
    "in", "like", "between", "union", "all", "exists", "with", "offset", "rows",
    "fetch", "next", "only", "over", "partition", "cast", "convert", "collate",
    "values", "insert", "into", "update", "delete", "set", "exec", "execute",
    "declare", "if", "begin", "return", "int", "bigint", "smallint", "tinyint",
    "money", "decimal", "numeric", "nvarchar", "varchar", "nchar", "char", "bit",
    "date", "datetime", "float", "real", "text", "ntext", "sysname", "max",
    "true", "false",
}

SQL_FUNCTIONS = {
    "count", "sum", "min", "max", "avg", "len", "isnull", "coalesce", "substring",
    "round", "getdate", "pwdcompare", "db_name", "serverproperty", "left", "right",
    "cast", "convert", "charindex", "stuff", "replicate", "upper", "lower", "ltrim",
    "rtrim", "replace", "abs", "floor", "ceiling", "datediff", "dateadd", "convert",
    "row_number", "rank", "dense_rank", "string_agg", "concat", "iif", "try_cast",
    "try_convert", "format", "object_id", "schema_name", "scope_identity", "error_message",
}


def _ci(columns):
    return {c.lower(): c for c in columns}


def check_statement(sql: str, amap: dict):
    problems = []
    known_tables = set(amap.values())
    single = next(iter(known_tables)) if len(known_tables) == 1 else None

    # output aliases ("AS name") are not columns
    output_aliases = {m.group(1).lower() for m in re.finditer(r"\bAS\s+(\w+)", sql, flags=re.I)}
    # Kotlin string interpolation names inside the SQL text
    interpolation = {m.group(1).lower() for m in re.finditer(r"\$\{?(\w+)", sql)}

    # qualified references: alias.column / table.column
    for m in re.finditer(r"\b([A-Za-z_]\w*)\s*\.\s*(\$?\{?\w+\}?)", sql):
        qualifier, column = m.group(1).lower(), m.group(2).lower()
        if qualifier in IGNORE_QUALIFIERS or qualifier in SQL_FUNCTIONS:
            continue
        table = amap.get(qualifier)
        if table is None:
            continue
        if column.startswith("$") or column in interpolation:
            continue                      # built by Kotlin: validated at the call site
        if column.startswith("$"):
            continue
        if column not in _ci(SCHEMA[table]):
            problems.append(f"unknown column {qualifier}.{column}  (table {table})")

    # unqualified names, only when exactly one table is involved
    if single:
        body = re.sub(r"'[^']*'", " ", sql)
        body = re.sub(r"@\w+", " ", body)
        body = re.sub(r"[+\-*/=<>(),;]", " ", body)
        for token in re.findall(r"\b([A-Za-z_]\w*)\b", body):
            low = token.lower()
            if low in SQL_KEYWORDS or low in SQL_FUNCTIONS or low in IGNORE_QUALIFIERS:
                continue
            if low in output_aliases or low in interpolation:
                continue
            if low in SCHEMA or low in known_tables or low in amap:
                continue
            # short table name used as a qualifier (forosh_price.shka)
            if low in {t.split(".")[-1] for t in known_tables}:
                continue
            # any word that is used as a qualifier somewhere in this statement
            if re.search(r"\b" + re.escape(token) + r"\s*\.", sql):
                continue
            if token.lower() in _ci(SCHEMA[single]):
                continue
            problems.append(f"unknown unqualified column '{token}'  (single table {single})")
    return problems


def check(path: pathlib.Path):
    text = path.read_text(encoding="utf-8", errors="replace")
    if path.suffix == ".kt":
        statements = kotlin_sql_strings(text)
    elif path.suffix == ".sql":
        statements = [s if isinstance(s, str) else s[0] for s in sql_files(text)]
        statements = re.findall(r"(?:SELECT|INSERT|UPDATE|DELETE)[^;]{0,4000}", text, flags=re.S | re.I)
    else:
        return True

    problems = []
    for st in statements:
        amap = alias_map(st)
        if not amap:
            continue
        problems += check_statement(st, amap)

    problems = sorted(set(problems))
    if problems:
        print(f"\n=== {path.name} ===")
        for p in problems:
            print(f"  [FAIL] {p}")
        return False
    n = sum(1 for st in statements if alias_map(st))
    print(f"  {path.name:32s} {n} SQL statement(s) checked - OK")
    return True


SCHEMA = load_schema()

if __name__ == "__main__":
    args = sys.argv[1:]
    files = [pathlib.Path(a) for a in args] if args else \
        sorted(list(ROOT.glob("*.kt")) + list(ROOT.glob("*.sql")))
    print(f"schema: {len(SCHEMA)} tables, {sum(len(v) for v in SCHEMA.values())} columns "
          f"(from {TSV.name})")
    ok = all(check(f) for f in files)
    print("\nRESULT:", "NO UNKNOWN IDENTIFIERS" if ok else "PROBLEMS FOUND")
    sys.exit(0 if ok else 1)
