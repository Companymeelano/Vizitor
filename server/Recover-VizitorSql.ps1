<#
═══════════════════════════════════════════════════════════════════════════
  Vizitor — آتیران ویزیتور | بازیابی فوری SQL Server (Recover-VizitorSql.ps1)
  Developed by Milano Technical Team, Milad Yaghoobi
  ─────────────────────────────────────────────────────────────────────────
  برای وقتی که سرویس SQL Server پس از تغییر تنظیمات استارت نمی‌شود.

  این اسکریپت به‌ترتیب:
    ۱) از رجیستری فعلی SQL بکاپ می‌گیرد (فایل .reg کنار اسکریپت)
    ۲) نمونه SQL و سرویس را پیدا می‌کند و مقادیر فعلی را نمایش می‌دهد
    ۳) چند بار سرویس را استارت می‌زند (پوشش حالت «بازیابی کند پس از توقف اجباری»)
    ۴) اگر استارت نشد: انتهای لاگ ERRORLOG و رویدادهای ویندوز را می‌خواند و چاپ می‌کند
       و سپس «بازگردانی امن» انجام می‌دهد: TCP/IP ← غیرفعال، LoginMode ← ۱ (Windows Auth)
       و دوباره سرویس را استارت می‌زند تا حسابداری هرچه زودتر بالا بیاید
    ۵) پورت 1433 و ورود به دیتابیس را تست می‌کند و راهنمای قدم بعدی را می‌دهد

  اجرا (پاورشل مدیر):
      powershell -ExecutionPolicy Bypass -File .\Recover-VizitorSql.ps1
      .\Recover-VizitorSql.ps1 -SafeRestore      # بدون پرسش، بازگردانی امن را نیز اعمال کن
═══════════════════════════════════════════════════════════════════════════
#>

[CmdletBinding()]
param(
    [string]$InstanceName = 'MSSQLSERVER',   # نام نمونه SQL (پیش‌فرض: نمونه پیش‌فرض)
    [int]   $SqlPort      = 1433,
    [string]$DbName       = 'Meelano',
    [string]$DbUser       = '',              # اگر خالی بماند از config.php کنار اسکریپت خوانده می‌شود
    [string]$DbPass       = '',
    [switch]$SafeRestore,                    # اعمال بازگردانی امن رجیستری بدون پرسش
    [switch]$NoSafeRestore                   # فقط تشخیص؛ دست به رجیستری نزن
)

try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding           = [System.Text.Encoding]::UTF8
} catch { }
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

# ─── خود-ارتقا به مدیر ───
$id = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$pp = New-Object System.Security.Principal.WindowsPrincipal($id)
if (-not $pp.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)) {
    if (-not $PSCommandPath) {
        Write-Host 'پاورشل را Run as Administrator باز کرده و دوباره اجرا کنید.' -ForegroundColor Red
        exit 1
    }
    $argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"")
    foreach ($k in $PSBoundParameters.Keys) {
        $v = $PSBoundParameters[$k]
        if ($v -is [switch]) { if ($v.IsPresent) { $argList += "-$k" } }
        else { $argList += "-$k"; $argList += "`"$v`"" }
    }
    Start-Process -FilePath "$PSHOME\powershell.exe" -Verb RunAs -ArgumentList $argList
    exit
}

$WorkDir = if ($PSCommandPath) { Split-Path -Parent $PSCommandPath } else { (Get-Location).Path }

function Ok([string]$m)   { Write-Host "  ✔ $m" -ForegroundColor Green }
function Bad([string]$m)  { Write-Host "  ✖ $m" -ForegroundColor Red }
function Warn([string]$m) { Write-Host "  ⚠ $m" -ForegroundColor Yellow }
function Inf([string]$m)  { Write-Host "  · $m" -ForegroundColor DarkGray }
function H([string]$m)    { Write-Host ''; Write-Host "── $m ──" -ForegroundColor Cyan }

Write-Host ''
Write-Host '╔══════════════════════════════════════════════════════════════╗' -ForegroundColor Magenta
Write-Host '║        Vizitor SQL Recovery — بازیابی فوری SQL Server          ║' -ForegroundColor Magenta
Write-Host '╚══════════════════════════════════════════════════════════════╝' -ForegroundColor Magenta

# ─── ۱) بکاپ رجیستری ───
H '۱) بکاپ از رجیستری SQL Server'
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupFile = Join-Path $WorkDir "sql-registry-backup-$ts.reg"
try {
    & reg.exe export 'HKLM\SOFTWARE\Microsoft\Microsoft SQL Server' $backupFile /y | Out-Null
    Ok "بکاپ رجیستری ← $backupFile"
} catch {
    Warn "بکاپ رجیستری ناموفق: $($_.Exception.Message)"
}

# ─── ۲) یافتن نمونه و نمایش تنظیمات فعلی ───
H '۲) نمونه SQL و تنظیمات فعلی'
$instReg = 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL'
$suffix = $null
$svcName = $null
if (Test-Path $instReg) {
    $props = (Get-ItemProperty $instReg).PSObject.Properties | Where-Object { $_.Name -notlike 'PS*' }
    $chosen = $props | Where-Object { $_.Name -eq $InstanceName } | Select-Object -First 1
    if (-not $chosen) { $chosen = $props | Select-Object -First 1 }
    if ($chosen) {
        $suffix = "$($chosen.Value)"
        $InstanceName = "$($chosen.Name)"
        $svcName = if ($InstanceName -eq 'MSSQLSERVER') { 'MSSQLSERVER' } else { "MSSQL`$$InstanceName" }
    }
}
if (-not $suffix) {
    Bad 'هیچ نمونه SQL Server روی این ماشین یافت نشد! (رجیستری Instance Names خالی است)'
    Inf 'اگر SQL نصب است اما سرویس ندارد، از SQL Server Installation Center وضعیت را بررسی کنید.'
    exit 1
}
Ok "نمونه: $InstanceName ($suffix) — سرویس: $svcName"
$base   = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$suffix\MSSQLServer"
$tcpKey = "$base\SuperSocketNetLib\Tcp"
$ipAll  = "$tcpKey\IPAll"
$tcpEnabled = (Get-ItemProperty $tcpKey -ErrorAction SilentlyContinue).Enabled
$tcpPort    = (Get-ItemProperty $ipAll  -ErrorAction SilentlyContinue).TcpPort
$tcpDyn     = (Get-ItemProperty $ipAll  -ErrorAction SilentlyContinue).TcpDynamicPorts
$loginMode  = (Get-ItemProperty $base   -ErrorAction SilentlyContinue).LoginMode
Inf "TCP Enabled=$tcpEnabled | TcpPort=$tcpPort | TcpDynamicPorts='$tcpDyn' | LoginMode=$loginMode"

# ─── ۳) تلاش برای استارت سرویس (چند نوبت — برای بازیابی کند) ───
H '۳) تلاش برای استارت سرویس'
function Service-Running { $s = Get-Service -Name $script:SvcNameLocal -ErrorAction SilentlyContinue; return ($s -and $s.Status -eq 'Running') }
$script:SvcNameLocal = $svcName
function Port-Open { return Test-NetConnection -ComputerName 127.0.0.1 -Port $SqlPort -InformationLevel Quiet -WarningAction SilentlyContinue }

$started = $false
for ($try = 1; $try -le 4 -and -not $started; $try++) {
    try {
        Inf "تلاش $try/4 برای استارت $svcName…"
        Start-Service -Name $svcName -ErrorAction Stop
    } catch {
        Inf "  استارت ناموفق: $($_.Exception.Message)"
    }
    $w = 0
    while (-not (Service-Running) -and $w -lt 45) { Start-Sleep -Seconds 3; $w += 3 }
    $started = Service-Running
    if ($started) { Ok "سرویس $svcName در تلاش $try بالا آمد" }
}
if (-not $started) { Warn 'سرویس پس از ۴ تلاش هنوز Running نیست — خواندن لاگ‌ها…' }

# ─── ۴) خواندن لاگ خطای SQL و رویدادهای ویندوز ───
function Show-SqlDiagnostics {
    H '۴) لاگ خطای SQL (ERRORLOG) و رویدادها'
    $logPath = $null
    try {
        $prm = Get-ItemProperty "$base\Parameters" -ErrorAction Stop
        foreach ($n in @('SQLArg0','SQLArg1','SQLArg2')) {
            $v = $prm.$n
            if ($v -and "$v".StartsWith('-e')) { $logPath = "$v".Substring(2) }
        }
    } catch { }
    if (-not $logPath) {
        $got = Get-ChildItem 'C:\Program Files\Microsoft SQL Server' -Directory -ErrorAction SilentlyContinue |
               ForEach-Object { Join-Path $_.FullName 'MSSQL\Log\ERRORLOG' } |
               Where-Object { (Test-Path $_) -and $_.Contains($suffix) } |
               Select-Object -First 1
        if ($got) { $logPath = $got }
    }
    if ($logPath -and (Test-Path $logPath)) {
        Inf "ERRORLOG ← $logPath"
        $tail = Get-Content $logPath -Tail 60 -ErrorAction SilentlyContinue
        $errLines = $tail | Where-Object { $_ -match '(?i)error|fail|denied|cannot|could not|unable' } | Select-Object -Last 12
        if ($errLines) {
            $errLines | ForEach-Object { Write-Host "    ERR│ $_" -ForegroundColor Yellow }
            return $true
        } else {
            Inf 'خطای آشکاری در انتهای لاگ دیده نشد؛ ۱۲ خط آخر:'
            ($tail | Select-Object -Last 12) | ForEach-Object { Write-Host "    LOG│ $_" -ForegroundColor DarkGray }
            return $true
        }
    } else {
        Warn 'فایل ERRORLOG پیدا نشد'
    }
    try {
        $ev = Get-WinEvent -FilterHashtable @{ LogName = 'System'; ProviderName = 'Service Control Manager'; Level = 2, 3 } -MaxEvents 6 -ErrorAction Stop |
              Where-Object { $_.TimeCreated -gt (Get-Date).AddHours(-3) }
        foreach ($e in $ev) { Write-Host ("    EVT│ {0:HH:mm:ss} #{1} {2}" -f $e.TimeCreated, $e.Id, ($e.Message -split "`n")[0]) -ForegroundColor Yellow }
    } catch { Inf 'رویدادی از Service Control Manager یافت نشد' }
    return $false
}

# ─── ۵) بازگردانی امن رجیستری (در صورت نیاز) ───
if (-not $started) {
    Show-SqlDiagnostics | Out-Null
    H '۵) بازگردانی امن تنظیمات (TCP/IP غیرفعال + Windows Authentication)'
    if ($NoSafeRestore) {
        Warn 'طبق -NoSafeRestore به رجیستری دست نمی‌زنم. سرویس را با SSMS/Configuration Manager بررسی کنید.'
    } else {
        $apply = $SafeRestore.IsPresent
        if (-not $apply) {
            try {
                $ans = Read-Host '  تنظیمات شبکه/احراز به حالت پیش‌فرضِ امن برگردد تا سرویس بالا بیاید؟ [y/N]'
                $apply = ($ans -match '^[yY]')
            } catch { $apply = $false }
        }
        if ($apply) {
            try {
                Set-ItemProperty -Path $tcpKey -Name 'Enabled' -Value 0 -Type DWord -ErrorAction Stop
                Set-ItemProperty -Path $base  -Name 'LoginMode' -Value 1 -Type DWord -ErrorAction Stop
                Ok 'رجیستری به حالت امن برگشت (TCP غیرفعال، LoginMode=1)'
            } catch { Warn "تغییر رجیستری ناموفق: $($_.Exception.Message)" }
            for ($try = 1; $try -le 3 -and -not $started; $try++) {
                try { Start-Service -Name $svcName -ErrorAction Stop } catch { Inf "  استارت: $($_.Exception.Message)" }
                $w = 0
                while (-not (Service-Running) -and $w -lt 45) { Start-Sleep -Seconds 3; $w += 3 }
                $started = Service-Running
            }
            if ($started) { Ok "سرویس پس از بازگردانی امن بالا آمد ✨" } else { Bad 'حتی با تنظیمات امن نیز استارت نشد — خروجی لاگ بالا را برای تیم فنی بفرستید' }
        } else {
            Warn 'بازگردانی امن لغو شد — سرویس را دستی بررسی کنید.'
        }
    }
}

# ─── ۶) راه‌اندازی سرویس‌های وابسته ───
H '۶) سرویس‌های وابسته'
foreach ($dep in @('SQLSERVERAGENT', 'SQLBrowser')) {
    $s = Get-Service -Name $dep -ErrorAction SilentlyContinue
    if ($s -and $s.Status -ne 'Running') {
        try { Start-Service -Name $dep -ErrorAction Stop; Ok "$dep استارت شد" }
        catch { Inf "$dep استارت نشد (اگر لازم نیست مهم نیست): $($_.Exception.Message)" }
    } elseif ($s) { Ok "$dep از قبل در حال اجراست" }
}
$atiranLike = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '(?i)atiran|account|meelano' -and $_.Status -ne 'Running' }
foreach ($s in $atiranLike) {
    try { Start-Service -Name $s.Name -ErrorAction Stop; Ok "سرویس «$($s.DisplayName)» استارت شد" }
    catch { Warn "سرویس $($s.Name) استارت نشد — دستی از services.msc بالا بیارید" }
}

# ─── ۷) تست نهایی ───
H '۷) تست نهایی اتصال'
Inf 'وضعیت سرویس‌ها:'
Get-Service -Name $svcName -ErrorAction SilentlyContinue | Format-Table Name, Status, StartType -AutoSize | Out-String | Write-Host
if (Port-Open) {
    Ok "پورت $SqlPort در حال گوش دادن است"
} else {
    if ($started) { Warn 'سرویس Running است ولی پورت 1433 باز نیست — اگر TCP/IP را در بازگردانی امن غیرفعال کردیم طبیعی است؛ حسابداری محلی با Shared Memory وصل می‌شود' }
    else { Bad 'پورت 1433 بسته است' }
}

# خواندن اعتبار از config.php در صورت پارامتر-نادادن
if (-not $DbUser -and (Test-Path (Join-Path $WorkDir 'config.php'))) {
    $cfgTxt = Get-Content (Join-Path $WorkDir 'config.php') -Raw -Encoding UTF8
    if ($cfgTxt -match "const\s+DB_USER\s*=\s*'([^']*)'")     { $DbUser = $Matches[1] }
    if ($cfgTxt -match "const\s+DB_PASSWORD\s*=\s*'([^']*)'") { $DbPass = $Matches[1] }
    if ($cfgTxt -match "const\s+DB_NAME\s*=\s*'([^']*)'")     { $DbName = $Matches[1] }
    if ($cfgTxt -match "const\s+DB_PORT\s*=\s*(\d+)")         { $SqlPort = [int]$Matches[1] }
}
if ($started) {
    $b = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
    $b.DataSource = "tcp:127.0.0.1,$SqlPort"; $b.InitialCatalog = $DbName
    $b.UserID = $DbUser; $b.Password = $DbPass
    $b.Encrypt = $false; $b.TrustServerCertificate = $true; $b.ConnectTimeout = 6
    try {
        $cn = New-Object System.Data.SqlClient.SqlConnection($b.ConnectionString)
        $cn.Open(); $cn.Close()
        Ok "ورود SQL با کاربر $DbUser به $DbName موفق است"
    } catch {
        Inf "ورود SQL با کاربر $DbUser ناموفق: $($_.Exception.Message)"
        Inf 'اگر LoginMode=1 برگشتیم، ورود SQL موقتاً غیرفعال است — حسابداری با Windows Auth کار می‌کند.'
    }
}

# ─── راهنمای قدم بعدی ───
Write-Host ''
Write-Host '═══════════ راهنمای قدم بعدی ═══════════' -ForegroundColor Cyan
if ($started) {
    Ok 'موتور SQL بالاست — نرم‌افزار حسابداری را باز کنید؛ کلاینت‌ها وصل می‌شوند.'
    if ($loginMode -ne 1) {
        Inf 'نکته: حسابداری با Windows Authentication تازه کارش را از سر گرفت. برای فعال‌سازی دوباره Mixed Mode/TCP'
        Inf '(لازمه اتصال اپ)، وقتی سرویس پایدار بود این را در ساعت خلوت اجرا کنید:'
        Write-Host "      .\\Setup-VizitorServer.ps1 -NoSqlRestart" -ForegroundColor White
        Inf 'سپس خودتان از services.msc فقط سرویس SQL Server را Restart کنید و بلافاصله دوباره تست بگیرید.'
    }
} else {
    Bad 'سرویس هنوز پایین است — لطفاً خروجی لاگ (خطوط ERR بالا) را برای تیم میلانو بفرستید.'
    Inf 'بازیابی دستی کامل: کنار همین اسکریپت فایل .reg بکاپ آمده؛ دابل‌کلیک و Merge کنید سپس:'
    Write-Host "      Start-Service $svcName" -ForegroundColor White
}
Write-Host ''
if ($Host.Name -eq 'ConsoleHost') { try { Read-Host 'برای خروج Enter بزنید' | Out-Null } catch { } }
if ($started) { exit 0 } else { exit 2 }
