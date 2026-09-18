# ============================================================================
#  Vizitor setup — preflight detection                 (called by vizitor.nsi)
#  Writes a small INI file that the installer reads with ReadINIStr.
#  Written for PowerShell 2.0 compatibility so it also runs on a plain
#  Windows 7 machine (a machine that only has PS 2 is reported as ps=2 and the
#  installer then shows the "upgrade PowerShell" hint).
# ============================================================================
param(
    [Parameter(Mandatory = $true)][string]$Out
)

$ErrorActionPreference = "SilentlyContinue"
$lines = New-Object System.Collections.ArrayList
function Add-Line([string]$k, [string]$v) {
    [void]$lines.Add("$k=$v")
}
function One([object]$v) { if ($null -eq $v) { return "" } else { return [string]$v } }

# ---- OS ------------------------------------------------------------------ #
$osName = ""; $osVer = ""
try {
    $os = Get-WmiObject Win32_OperatingSystem
    $osName = One $os.Caption
    $osVer = One $os.Version
} catch { }
if (-not $osVer) { try { $osVer = [Environment]::OSVersion.Version.ToString() } catch { } }

# ---- PowerShell version -------------------------------------------------- #
$psv = ""
try { $psv = $PSVersionTable.PSVersion.ToString() } catch { $psv = "2.0" }
$psMajor = 2
try { $psMajor = [int]($PSVersionTable.PSVersion.Major) } catch { }

# ---- Administrator? ------------------------------------------------------ #
$admin = 0
try {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $pr = New-Object Security.Principal.WindowsPrincipal($id)
    if ($pr.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { $admin = 1 }
} catch { }

# ---- LAN IPv4 (adapter that has a gateway wins) -------------------------- #
$ip = ""
try {
    $cfg = Get-WmiObject Win32_NetworkAdapterConfiguration |
        Where-Object { $_.IPEnabled -eq $true -and $_.DefaultIPGateway -ne $null } |
        Select-Object -First 1
    if (-not $cfg) {
        $cfg = Get-WmiObject Win32_NetworkAdapterConfiguration |
            Where-Object { $_.IPEnabled -eq $true } | Select-Object -First 1
    }
    if ($cfg) {
        foreach ($a in $cfg.IPAddress) {
            if ($a -notlike "169.254.*" -and $a -ne "127.0.0.1" -and $a -notlike "*:*") { $ip = $a; break }
        }
    }
} catch { }

# ---- Python -------------------------------------------------------------- #
$py = ""
$pyExe = ""
foreach ($cand in @("python", "python3", "py")) {
    try {
        $cmd = Get-Command $cand -ErrorAction SilentlyContinue
        if ($cmd) {
            $out = ""
            if ($cand -eq "py") { $out = (& $cand -3 --version 2>&1 | Out-String) } else { $out = (& $cand --version 2>&1 | Out-String) }
            if ($out -match "Python\s+([0-9]+\.[0-9]+\.[0-9]+)") {
                $py = $Matches[1]; $pyExe = $cmd.Source; break
            }
        }
    } catch { }
}
if (-not $py) {
    foreach ($d in @("$env:ProgramFiles\Python*", "$env:LOCALAPPDATA\Programs\Python\Python*")) {
        $hit = Get-Item $d | Select-Object -First 1
        if ($hit) {
            $exe = Join-Path $hit.FullName "python.exe"
            if (Test-Path $exe) {
                $out = (& $exe --version 2>&1 | Out-String)
                if ($out -match "Python\s+([0-9]+\.[0-9]+\.[0-9]+)") { $py = $Matches[1]; $pyExe = $exe; break }
            }
        }
    }
}
$pyOdbc = 0
if ($pyExe) {
    try { & $pyExe -c "import pyodbc" 2>$null; if ($LASTEXITCODE -eq 0) { $pyOdbc = 1 } } catch { }
}
# the launcher 'py' may not be the same interpreter used above; re-check by name too
if ($pyOdbc -eq 0 -and $py) {
    try { python -m pip show pyodbc >$null 2>&1; if ($LASTEXITCODE -eq 0) { $pyOdbc = 1 } } catch { }
}

# ---- ODBC drivers -------------------------------------------------------- #
$odbc = ""
try {
    $k = Get-ItemProperty "HKLM:\SOFTWARE\ODBC\ODBCINST.INI\ODBC Drivers" -ErrorAction SilentlyContinue
    if ($k) {
        foreach ($p in $k.PSObject.Properties) {
            if ($p.Name -like "ODBC Driver*for SQL Server*" -or $p.Name -like "SQL Server Native Client*" -or $p.Name -eq "SQL Server") {
                $odbc = $p.Name; break
            }
        }
    }
} catch { }
if (-not $odbc -and $pyOdbc -eq 1 -and $pyExe) {
    try { $odbc = (& $pyExe -c "import pyodbc; print('|'.join(pyodbc.drivers()))" 2>$null | Out-String).Trim() } catch { }
}

# ---- SQL Server service / firewall ---------------------------------------- #
$sql = 0
try {
    $svc = Get-Service -Name "MSSQLSERVER", "MSSQL`$*" -ErrorAction SilentlyContinue
    foreach ($s in $svc) { if ($s.Status -eq "Running") { $sql = 1; break } }
    if ($sql -eq 0 -and $svc -ne $null) { $sql = 2 }   # installed but not running
} catch { }

$fw = 0
try {
    if (Get-Command netsh -ErrorAction SilentlyContinue) { $fw = 1 }
} catch { }

# ---- existing Vizitor installation? -------------------------------------- #
$existing = 0
try { if (Test-Path "C:\ProgramData\Vizitor\config.json") { $existing = 1 } } catch { }

# ---- write the INI (UTF-8 without BOM) ----------------------------------- #
Add-Line "osname"      $osName
Add-Line "osver"       $osVer
Add-Line "ps"          $psv
Add-Line "psmajor"     "$psMajor"
Add-Line "admin"       "$admin"
Add-Line "ip"          $ip
Add-Line "python"      $py
Add-Line "pythonexe"   $pyExe
Add-Line "pyodbc"      "$pyOdbc"
Add-Line "odbc"        $odbc
Add-Line "sql"         "$sql"
Add-Line "firewall"    "$fw"
Add-Line "existing"    "$existing"

$text = "[pre]" + [Environment]::NewLine + ($lines -join [Environment]::NewLine) + [Environment]::NewLine
try {
    $enc = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Out, $text, $enc)
    Write-Output "PREFLIGHT_OK"
} catch {
    Write-Output "PREFLIGHT_FAILED: $($_.Exception.Message)"
}
