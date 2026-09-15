﻿<#
═══════════════════════════════════════════════════════════════════════════
  Vizitor — آتیران ویزیتور | راه‌اندازی خودکار سرور (Setup-VizitorServer.ps1)
  Developed by Milano Technical Team, Milad Yaghoobi
  ─────────────────────────────────────────────────────────────────────────
  این اسکریپت تمام پیش‌نیازهای اتصال اپ اندروید به وب‌سرویس api.php را روی
  ویندوز ۱۱ به‌صورت مرحله‌به‌مرحله «بررسی» می‌کند؛ اگر بخشی سالم باشد تیک
  می‌خورد و عبور می‌کند، و اگر اشتباه/ناقص باشد همان‌جا «اصلاح» می‌شود:

    ۱) فایل‌های سرور و خواندن تنظیمات از config.php
    ۲) نصب/فعال‌سازی IIS + ماژول FastCGI/CGI
    ۳) نصب/بررسی PHP 8.x (NTS پیش‌فرض 8.3)
    ۴) درایور Microsoft ODBC for SQL Server
    ۵) افزونه‌های php_sqlsrv و php_pdo_sqlsrv
    ۶) سایت IIS روی پورت 8731 + استقرار api.php/config.php + محافظت config.php
    ۷) فایروال ویندوز (باز کردن پورت وب‌سرویس)
    ۸) SQL Server: فعال‌سازی TCP/IP روی پورت 1433
    ۹) SQL Server: فعال‌سازی Mixed Mode Authentication
    ۱۰) بررسی اتصال با کاربر دیتابیس (ساخت/اصلاح لاگین در صورت نیاز و امکان)
    ۱۱) تست سرتاسری API: ping با کلید، ۴۰۱ بدون کلید، مسدود بودن config.php
    ۱۲) بررسی دسترس‌پذیری IP عمومی (اطلاعاتی)

  نحوه اجرا (روی خود سرور 37.143.148.14):
    - راست‌کلیک روی فایل ← Run with PowerShell  (خودش مدیر می‌شود)
    یا از پاورشل مدیر:
      powershell -ExecutionPolicy Bypass -File .\Setup-VizitorServer.ps1

  اسکریپت ایمن و قابل اجرای مجدد است (idempotent).
═══════════════════════════════════════════════════════════════════════════
#>

[CmdletBinding()]
param(
    [string]$ServerSource = '',                    # پوشه حاوی api.php و config.php (پیش‌فرض: پوشه همین اسکریپت)
    [string]$SiteName     = 'VizitorAPI',          # نام سایت IIS
    [string]$SiteRoot     = 'C:\inetpub\VizitorAPI', # ریشه فیزیکی سایت
    [int]   $WebPort      = 8731,                  # پورت وب‌سرویس (همان پورت اپ)
    [string]$PhpRoot      = 'C:\php\php-8.3-nts',  # مسیر نصب PHP در صورت نبودن
    [string]$PhpFamily    = '8.3',                 # خانواده PHP برای نصب تازه
    [string]$PublicHost   = '37.143.148.14',       # IP عمومی سرور برای تست نهایی
    [int]   $SqlPort      = 1433,                  # پورت SQL Server
    [switch]$SkipDownloads,                        # بدون دانلود از اینترنت (آفلاین)
    [switch]$SkipSqlLoginFix,                      # عدم ساخت/اصلاح خودکار لاگین SQL
    [switch]$NoSqlRestart,                         # اعمال تنظیمات SQL بدون ری‌استارت سرویس (برای ساعت کاری حسابداری)
    [switch]$Yes                                   # تأیید خودکار سؤال ری‌استارت سرویس SQL
)

# ─── آماده‌سازی خروجی UTF-8 ────────────────────────────────────────────────
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding           = [System.Text.Encoding]::UTF8
} catch { }

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'

# ─── خود-ارتقا به مدیر ──────────────────────────────────────────────────────
$currentIdentity  = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$currentPrincipal = New-Object System.Security.Principal.WindowsPrincipal($currentIdentity)
if (-not $currentPrincipal.IsInRole([System.Security.Principal.WindowsBuiltInRole]::Administrator)) {
    if (-not $PSCommandPath) {
        Write-Host 'لطفاً پاورشل را به‌صورت Run as Administrator باز کرده و اسکریپت را اجرا کنید.' -ForegroundColor Red
        exit 1
    }
    Write-Host 'در حال اجرای مجدد با دسترسی مدیر…' -ForegroundColor Yellow
    $argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$PSCommandPath`"")
    foreach ($k in $PSBoundParameters.Keys) {
        $v = $PSBoundParameters[$k]
        if ($v -is [switch]) { if ($v.IsPresent) { $argList += "-$k" } }
        else { $argList += "-$k"; $argList += "`"$v`"" }
    }
    Start-Process -FilePath "$PSHOME\powershell.exe" -Verb RunAs -ArgumentList $argList
    exit
}

if ($ServerSource -eq '') { $ServerSource = Split-Path -Parent $PSCommandPath }
if (-not $ServerSource)   { $ServerSource = (Get-Location).Path }

# ─── لاگ هم‌زمان در فایل ─────────────────────────────────────────────────────
try { Start-Transcript -Path (Join-Path $ServerSource 'Setup-VizitorServer.log') -Append | Out-Null } catch { }

# ─── ابزار چاپ مرحله‌ها ─────────────────────────────────────────────────────
$script:Steps         = New-Object System.Collections.Generic.List[object]
$script:RestartNeeded = $false
$script:RestartSqlService = $false
$script:PhpDir = $null
$script:StoppedDepsList = New-Object System.Collections.Generic.List[string]

function Add-Step([string]$Name, [string]$Status, [string]$Note) {
    $script:Steps.Add([pscustomobject]@{ Step = $Name; Status = $Status; Note = $Note })
}
function Write-StepHeader([int]$N, [string]$Title) {
    Write-Host ''
    Write-Host ("[{0,2}/12] {1}" -f $N, $Title) -ForegroundColor Cyan
}
function Step-Ok([string]$Name, [string]$Note)    { Write-Host "    ✔ $Name — سالم" -ForegroundColor Green;  if ($Note) { Write-Host "      $Note" -ForegroundColor DarkGray }; Add-Step $Name 'OK'    $Note }
function Step-Fixed([string]$Name, [string]$Note) { Write-Host "    ✔ $Name — اصلاح شد" -ForegroundColor Green; if ($Note) { Write-Host "      $Note" -ForegroundColor DarkGray }; Add-Step $Name 'FIXED' $Note }
function Step-Fail([string]$Name, [string]$Note)  { Write-Host "    ✖ $Name — ناموفق" -ForegroundColor Red;   if ($Note) { Write-Host "      $Note" -ForegroundColor Yellow   }; Add-Step $Name 'FAIL'  $Note }
function Step-Warn([string]$Name, [string]$Note)  { Write-Host "    ⚠ $Name — هشدار" -ForegroundColor Yellow;  if ($Note) { Write-Host "      $Note" -ForegroundColor DarkGray }; Add-Step $Name 'WARN'  $Note }
function Step-Skip([string]$Name, [string]$Note)  { Write-Host "    – $Name — رد شد" -ForegroundColor DarkGray; if ($Note) { Write-Host "      $Note" -ForegroundColor DarkGray }; Add-Step $Name 'SKIP'  $Note }

# ─── ابزار دانلود ───────────────────────────────────────────────────────────
function Save-File([string]$Url, [string]$Dest) {
    $old = $ProgressPreference
    $ProgressPreference = 'SilentlyContinue'
    try {
        Write-Host "      دانلود: $Url" -ForegroundColor DarkGray
        Invoke-WebRequest -Uri $Url -OutFile $Dest -UseBasicParsing -TimeoutSec 600 -ErrorAction Stop
    } finally { $ProgressPreference = $old }
}

# ─── ابزار php.ini بدون BOM ─────────────────────────────────────────────────
function Add-IniOnce([string]$Path, [string[]]$Lines) {
    $txt = [System.IO.File]::ReadAllText($Path)
    $add = @()
    foreach ($l in $Lines) { if ($txt -notmatch [regex]::Escape($l)) { $add += $l } }
    if ($add.Count -gt 0) {
        $txt = $txt.TrimEnd() + "`r`n`r`n; --- Vizitor Setup ---" + "`r`n" + ($add -join "`r`n") + "`r`n"
        [System.IO.File]::WriteAllText($Path, $txt, (New-Object System.Text.UTF8Encoding($false)))
    }
}

# ─── ابزار فراخوانی API (سازگار با PS 5.1) ──────────────────────────────────
function Invoke-Api([string]$Url, [hashtable]$Headers) {
    try {
        $r = Invoke-RestMethod -Uri $Url -Headers $Headers -TimeoutSec 15 -ErrorAction Stop
        return @{ Code = 200; Json = $r; Body = ($r | ConvertTo-Json -Depth 8 -Compress) }
    } catch [System.Net.WebException] {
        $resp = $_.Exception.Response
        if ($null -ne $resp) {
            $code = [int]$resp.StatusCode
            $body = ''
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
                $body = $sr.ReadToEnd()
            } catch { }
            $json = $null
            try { $json = $body | ConvertFrom-Json } catch { }
            return @{ Code = $code; Json = $json; Body = $body }
        }
        return @{ Code = 0; Json = $null; Body = $_.Exception.Message }
    } catch {
        return @{ Code = 0; Json = $null; Body = $_.Exception.Message }
    }
}

# ═══════════════════════════ شروع مراحل ═══════════════════════════════════
Write-Host ''
Write-Host '╔══════════════════════════════════════════════════════════════╗' -ForegroundColor Magenta
Write-Host '║   Vizitor Server Setup — راه‌اندازی خودکار سرور آتیران میلانو   ║' -ForegroundColor Magenta
Write-Host '╚══════════════════════════════════════════════════════════════╝' -ForegroundColor Magenta
Write-Host "  سرور: $PublicHost   پورت وب: $WebPort   پورت SQL: $SqlPort" -ForegroundColor DarkGray
Write-Host ''
Write-Host '  ⚠ توجه مهم: اگر تنظیمات SQL (مرحله ۸/۹) نیاز به تغییر داشته باشد، سرویس' -ForegroundColor Yellow
Write-Host '    SQL Server باید یک‌بار ری‌استارت شود و اتصال‌های جاری حسابداری لحظه‌ای قطع می‌شود.' -ForegroundColor Yellow
Write-Host '    در ساعت اوج کاری از پارامتر -NoSqlRestart استفاده کنید و بعداً خودتان سرویس را ری‌استارت کنید.' -ForegroundColor DarkGray

# ─────────── ۱) فایل‌ها + خواندن config.php ───────────
Write-StepHeader 1 'فایل‌های سرور و پیکربندی config.php'
$stepName = 'فایل‌های سرور (api.php / config.php)'
$apiPhp    = Join-Path $ServerSource 'api.php'
$configPhp = Join-Path $ServerSource 'config.php'
if ((Test-Path $apiPhp) -and (Test-Path $configPhp)) {
    Step-Ok $stepName "پوشه: $ServerSource"
} else {
    Step-Fail $stepName "api.php یا config.php در «$ServerSource» یافت نشد — اسکریپت باید کنار این دو فایل باشد یا -ServerSource را بدهید."
}

# خواندن ثابت‌ها از config.php (منبع واحد حقیقت — بدون تکرار رمز در اسکریپت)
$script:ConfigText = ''
$DbHost = $PublicHost; $DbPort = $SqlPort; $DbName = 'Meelano'; $DbUser = ''; $DbPass = ''; $ApiKey = ''
if (Test-Path $configPhp) {
    $script:ConfigText = Get-Content $configPhp -Raw -Encoding UTF8
    function Read-CfgConst([string]$Name, $Default) {
        $pat1 = "const\s+$Name\s*=\s*'([^']*)'\s*;"
        $pat2 = "const\s+$Name\s*=\s*(\d+)\s*;"
        if ($script:ConfigText -match $pat1) { return $Matches[1] }
        if ($script:ConfigText -match $pat2) { return [int]$Matches[1] }
        return $Default
    }
    $DbHost = [string](Read-CfgConst 'DB_HOST'     $PublicHost)
    $DbPort = [int]   (Read-CfgConst 'DB_PORT'     $SqlPort)
    $DbName = [string](Read-CfgConst 'DB_NAME'     'Meelano')
    $DbUser = [string](Read-CfgConst 'DB_USER'     '')
    $DbPass = [string](Read-CfgConst 'DB_PASSWORD' '')
    $ApiKey = [string](Read-CfgConst 'API_KEY'     '')
    Write-Host "      config.php → DB=$DbName @ $DbHost`:$DbPort | کاربر=$DbUser | کلیدAPI=$ApiKey" -ForegroundColor DarkGray
    if ([string]::IsNullOrWhiteSpace($ApiKey)) {
        Step-Warn 'خواندن تنظیمات config.php' 'API_KEY در config.php تعریف نشده است'
    } else {
        Step-Ok 'خواندن تنظیمات config.php' "DB=$DbName | میزبان=$DbHost"
    }
} else {
    Step-Fail 'خواندن تنظیمات config.php' 'config.php در دسترس نیست — مراحل دیتابیس با مقادیر پیش‌فرض ادامه می‌یابد'
}

# ─────────── ۲) IIS + CGI ───────────
Write-StepHeader 2 'سرویس IIS + ماژول FastCGI/CGI'
$stepName = 'سرویس IIS و FastCGI'
$iisFeatures = @(
    'IIS-WebServerRole', 'IIS-WebServer', 'IIS-CommonHttpFeatures',
    'IIS-StaticContent', 'IIS-DefaultDocument', 'IIS-HttpErrors',
    'IIS-ApplicationDevelopment', 'IIS-CGI', 'IIS-Security',
    'IIS-RequestFiltering', 'IIS-WebServerManagementTools'
)
$appcmd = Join-Path $env:SystemRoot 'system32\inetsrv\appcmd.exe'
function Test-IisReady { return (Test-Path $appcmd) }
$isClient = ((Get-CimInstance Win32_OperatingSystem).ProductType -eq 1)
if (Test-IisReady) {
    # حتی اگر IIS نصب است، CGI ممکن است نباشد
    $cgiOk = $true
    try {
        $st = Get-WindowsOptionalFeature -Online -FeatureName 'IIS-CGI' -ErrorAction Stop
        $cgiOk = ($st.State -eq 'Enabled')
    } catch { }
    if ($cgiOk) {
        Step-Ok $stepName 'IIS و ماژول CGI/FastCGI از قبل فعال است'
    } else {
        try {
            if ($isClient) {
                $r = Enable-WindowsOptionalFeature -Online -FeatureName 'IIS-CGI' -All -NoRestart -ErrorAction Stop
                if ($r.RestartNeeded -and "$($r.RestartNeeded)" -ne 'No') { $script:RestartNeeded = $true }
            } else {
                Install-WindowsFeature -Name Web-Cgi -ErrorAction Stop | Out-Null
            }
            Step-Fixed $stepName 'ماژول CGI/FastCGI فعال شد'
        } catch { Step-Fail $stepName "فعال‌سازی CGI ناموفق: $($_.Exception.Message)" }
    }
} else {
    # نصب کامل IIS
    $allOk = $true
    foreach ($f in $iisFeatures) {
        try {
            if ($isClient) {
                $stt = Get-WindowsOptionalFeature -Online -FeatureName $f -ErrorAction Stop
                if ($stt.State -ne 'Enabled') {
                    $r = Enable-WindowsOptionalFeature -Online -FeatureName $f -All -NoRestart -ErrorAction Stop
                    if ($r.RestartNeeded -and "$($r.RestartNeeded)" -ne 'No') { $script:RestartNeeded = $true }
                }
            } else {
                Install-WindowsFeature -Name $f -ErrorAction Stop | Out-Null
            }
        } catch { $allOk = $false }
    }
    if ((Test-IisReady) -and $allOk) {
        Step-Fixed $stepName 'IIS به‌همراه CGI نصب و فعال شد'
    } elseif (Test-IisReady) {
        Step-Fixed $stepName 'IIS نصب شد (برخی ویژگی‌های فرعی ممکن است نیاز به ری‌استارت داشته باشند)'
    } else {
        Step-Fail $stepName 'نصب IIS ناموفق بود — از Settings → Optional Features نصب دستی کنید'
    }
}

# ─────────── ۳) PHP ───────────
Write-StepHeader 3 'PHP 8.x (موتور اجرای api.php)'
$stepName = 'PHP 8.x'
function Find-Php {
    $cmd = Get-Command php.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $cands = @(
        (Join-Path $env:SystemDrive 'php'),
        (Join-Path $env:ProgramFiles 'PHP'),
        "$env:SystemDrive\tools\php"
    )
    foreach ($c in $cands) {
        if ($c -and (Test-Path $c)) {
            $cgi = Get-ChildItem -Path $c -Filter 'php-cgi.exe' -Recurse -Depth 1 -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($cgi) { return (Join-Path $cgi.DirectoryName 'php.exe') }
        }
    }
    return $null
}

$PhpExe = Find-Php
$PhpDir = $null; $PhpVer = $null; $PhpTs = $null
if ($PhpExe) {
    $PhpDir = Split-Path -Parent $PhpExe
    $vOut = (& $PhpExe -v 2>$null | Select-Object -First 1)
    if ($vOut -match 'PHP (\d+\.\d+)') { $PhpVer = $Matches[1] }
    $PhpTs = (& $PhpExe -r "echo (PHP_ZTS ? 'ts' : 'nts');" 2>$null)
    $PhpTs = "$PhpTs".Trim(); if ($PhpTs -notin @('ts', 'nts')) { $PhpTs = 'nts' }
    $cgiPath = Join-Path $PhpDir 'php-cgi.exe'
    $okVersion = $PhpVer -and ([version]$PhpVer -ge [version]'8.1')
    if ($okVersion -and (Test-Path $cgiPath)) {
        Step-Ok $stepName "PHP $PhpVer ($PhpTs) در $PhpDir"
    } else {
        Write-Host "      PHP موجود از نظر نسخه/php-cgi مناسب نیست (یافت‌شده: $PhpVer) — نصب تازه $PhpFamily" -ForegroundColor Yellow
        $PhpExe = $null
    }
}
if (-not $PhpExe) {
    if ($SkipDownloads) {
        Step-Fail $stepName "PHP یافت نشد و -SkipDownloads فعال است — PHP $PhpFamily NTS x64 را دستی در $PhpRoot نصب کنید"
    } else {
        try {
            $tsSuffix = 'nts'
            $latest = "https://windows.php.net/downloads/releases/latest/php-$PhpFamily-$tsSuffix-Win32-vs16-x64-latest.zip"
            $url = $null
            try {
                $head = Invoke-WebRequest -Uri $latest -Method Head -TimeoutSec 25 -UseBasicParsing -ErrorAction Stop
                if ($head.StatusCode -eq 200) { $url = $latest }
            } catch { }
            if (-not $url) {
                $html = (Invoke-WebRequest -Uri 'https://windows.php.net/download/' -TimeoutSec 45 -UseBasicParsing -ErrorAction Stop).Content
                $ms = [regex]::Matches($html, 'php-(' + [regex]::Escape($PhpFamily) + '\.\d+)-' + $tsSuffix + '-Win32-vs16-x64\.zip')
                $best = $null
                foreach ($m in $ms) {
                    $v = [version]$m.Groups[1].Value
                    if (-not $best -or ($v.CompareTo($best) -gt 0)) { $best = $v }
                }
                if ($best) { $url = "https://windows.php.net/downloads/releases/php-$best-$tsSuffix-Win32-vs16-x64.zip" }
            }
            if (-not $url) { throw 'لینک دانلود PHP از windows.php.net یافت نشد' }
            $zip = Join-Path $env:TEMP 'php-vizitor.zip'
            Save-File $url $zip
            New-Item -ItemType Directory -Force -Path $PhpRoot | Out-Null
            Expand-Archive -Path $zip -DestinationPath $PhpRoot -Force
            $PhpExe = Join-Path $PhpRoot 'php.exe'
            if (-not (Test-Path $PhpExe)) { throw "php.exe پس از استخراج در $PhpRoot یافت نشد" }
            # php.ini تولید
            $iniProd = Join-Path $PhpRoot 'php.ini-production'
            $iniPath = Join-Path $PhpRoot 'php.ini'
            if (-not (Test-Path $iniPath)) {
                if (Test-Path $iniProd) { Copy-Item $iniProd $iniPath -Force } else { New-Item -ItemType File -Path $iniPath -Force | Out-Null }
            }
            Add-IniOnce $iniPath @(
                'extension_dir = "ext"',
                'cgi.force_redirect = 0',
                'fastcgi.impersonate = 1',
                'date.timezone = Asia/Tehran'
            )
            # افزودن به PATH سیستم
            $curPath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
            if (($curPath -split ';') -notcontains $PhpRoot) {
                [Environment]::SetEnvironmentVariable('Path', ($curPath.TrimEnd(';') + ';' + $PhpRoot), 'Machine')
            }
            $PhpDir = $PhpRoot
            $PhpVer = $PhpFamily
            $PhpTs  = 'nts'
            Step-Fixed $stepName "PHP $PhpFamily NTS در $PhpRoot نصب و پیکربندی شد"
        } catch {
            Step-Fail $stepName "نصب خودکار PHP ناموفق: $($_.Exception.Message)"
        }
    }
}
if ($PhpExe -and -not $PhpDir) { $PhpDir = Split-Path -Parent $PhpExe }

# ─────────── ۴) ODBC Driver ───────────
Write-StepHeader 4 'درایور Microsoft ODBC Driver for SQL Server'
$stepName = 'Microsoft ODBC Driver'
function Test-Odbc {
    return ((Test-Path 'HKLM:\SOFTWARE\ODBC\ODBCINST.INI\ODBC Driver 18 for SQL Server') -or
            (Test-Path 'HKLM:\SOFTWARE\ODBC\ODBCINST.INI\ODBC Driver 17 for SQL Server'))
}
if (Test-Odbc) {
    Step-Ok $stepName 'ODBC Driver 17 یا 18 نصب است'
} elseif ($SkipDownloads) {
    Step-Fail $stepName 'ODBC Driver یافت نشد و دانلود غیرفعال است — از سایت مایکروسافت نصب کنید'
} else {
    $done = $false
    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if ($winget) {
        foreach ($id in @('Microsoft.msodbcsql.18', 'Microsoft.msodbcsql.17')) {
            try {
                & $winget.Source install --id $id --exact --silent `
                    --accept-package-agreements --accept-source-agreements --disable-interactivity | Out-Null
                if (Test-Odbc) { $done = $true; break }
            } catch { }
        }
    }
    if ($done -or (Test-Odbc)) {
        Step-Fixed $stepName 'ODBC Driver با winget نصب شد'
    } else {
        Step-Fail $stepName 'نصب خودکار ODBC ناموفق — نصب دستی: https://learn.microsoft.com/sql/connect/odbc/download-odbc-driver-for-sql-server'
    }
}

# ─────────── ۵) افزونه sqlsrv ───────────
Write-StepHeader 5 'افزونه‌های PHP: sqlsrv و pdo_sqlsrv'
$stepName = 'افزونه‌های PHP SQLSRV'
$mods = @()
if ($PhpExe) { $mods = @(& $PhpExe -m 2>$null | ForEach-Object { "$($_)".Trim() }) }
$haveSqlsrv = ($mods -contains 'sqlsrv') -and ($mods -contains 'pdo_sqlsrv')
if ($haveSqlsrv) {
    Step-Ok $stepName "sqlsrv و pdo_sqlsrv برای PHP $PhpVer بارگذاری شده‌اند"
} elseif (-not $PhpExe) {
    Step-Skip $stepName 'چون PHP در دسترس نیست، بررسی افزونه انجام نشد'
} elseif ($SkipDownloads) {
    Step-Fail $stepName 'افزونه sqlsrv بارگذاری نشده و دانلود غیرفعال است — نسخه متناسب را از گیت‌هاب microsoft/msphpsql نصب کنید'
} else {
    try {
        $digits = ($PhpVer -replace '\.', '')
        $suffix = $PhpTs
        $rels = Invoke-RestMethod -Uri 'https://api.github.com/repos/microsoft/msphpsql/releases?per_page=5' `
                    -Headers @{ 'User-Agent' = 'VizitorSetup'; 'Accept' = 'application/vnd.github+json' } -TimeoutSec 60 -ErrorAction Stop
        $asset = $null
        foreach ($rel in $rels) {
            $asset = @($rel.assets) | Where-Object { $_.name -eq "Windows-$PhpVer.zip" } | Select-Object -First 1
            if ($asset) { break }
        }
        if (-not $asset) { throw "بسته Windows-$PhpVer.zip در ریلیزهای microsoft/msphpsql یافت نشد" }
        $zip = Join-Path $env:TEMP 'sqlsrv-vizitor.zip'
        Save-File $asset.browser_download_url $zip
        $tmp = Join-Path $env:TEMP 'sqlsrv-vizitor'
        if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
        Expand-Archive -Path $zip -DestinationPath $tmp -Force
        $dll1 = Get-ChildItem $tmp -Filter "php_sqlsrv_${digits}_${suffix}_x64.dll" -Recurse | Select-Object -First 1
        $dll2 = Get-ChildItem $tmp -Filter "php_pdo_sqlsrv_${digits}_${suffix}_x64.dll" -Recurse | Select-Object -First 1
        if (-not $dll1 -or -not $dll2) { throw "DLLهای متناسب با PHP $PhpVer $suffix در بسته یافت نشد" }
        $extDir = Join-Path $PhpDir 'ext'
        New-Item -ItemType Directory -Force -Path $extDir | Out-Null
        Copy-Item $dll1.FullName $extDir -Force
        Copy-Item $dll2.FullName $extDir -Force
        $iniPath = Join-Path $PhpDir 'php.ini'
        if (-not (Test-Path $iniPath)) {
            $iniProd = Join-Path $PhpDir 'php.ini-production'
            if (Test-Path $iniProd) { Copy-Item $iniProd $iniPath -Force } else { New-Item -ItemType File -Path $iniPath -Force | Out-Null }
            Add-IniOnce $iniPath @('extension_dir = "ext"', 'cgi.force_redirect = 0', 'fastcgi.impersonate = 1')
        }
        Add-IniOnce $iniPath @(
            "extension=$($dll1.Name)",
            "extension=$($dll2.Name)"
        )
        $mods = @(& $PhpExe -m 2>$null | ForEach-Object { "$($_)".Trim() })
        $haveSqlsrv = ($mods -contains 'sqlsrv') -and ($mods -contains 'pdo_sqlsrv')
        if ($haveSqlsrv) {
            Step-Fixed $stepName "افزونه‌های $($dll1.Name) / $($dll2.Name) نصب و بارگذاری شدند"
        } else {
            $errTail = (& $PhpExe --ri sqlsrv 2>&1 | Select-Object -First 3) -join ' | '
            Step-Fail $stepName "DLL کپی شد اما بارگذاری نشد (نسخه/معماری ناسازگار یا ODBC غایب) — $errTail"
        }
    } catch {
        Step-Fail $stepName "نصب افزونه sqlsrv ناموفق: $($_.Exception.Message)"
    }
}

# ─────────── ۶) سایت IIS + استقرار فایل‌ها ───────────
Write-StepHeader 6 "سایت IIS روی پورت $WebPort + استقرار api.php + محافظت config.php"
$stepName = "سایت وب (پورت $WebPort)"
if (-not (Test-Path $appcmd)) {
    Step-Fail $stepName 'IIS در دسترس نیست (appcmd یافت نشد) — ابتدا مرحله ۲ باید موفق شود'
} else {
    try {
        # اگر سایت دیگری همین پورت را گرفته، همان را اتخاذ کن
        $conflict = & $appcmd list site "/bindings:http/*:$($WebPort):" 2>$null
        if ($conflict) {
            $line = $conflict | Select-Object -First 1
            if ($line -match 'SITE "([^"]+)"') {
                $existing = $Matches[1]
                if ($existing -ne $SiteName) {
                    Write-Host "      پورت $WebPort متعلق به سایت «$existing» است — همان سایت اتخاذ می‌شود" -ForegroundColor Yellow
                    $SiteName = $existing
                    $pp = & $appcmd list vdir "$SiteName/" /text:physicalPath 2>$null
                    if ($pp) { $SiteRoot = "$pp".Trim() }
                }
            }
        }
        New-Item -ItemType Directory -Force -Path $SiteRoot | Out-Null

        # AppPool بدون کد مدیریت‌شده (PHP نیازی به .NET ندارد)
        $poolList = & $appcmd list apppool "$SiteName" 2>$null
        if (-not $poolList) { & $appcmd add apppool "/name:$SiteName" | Out-Null }
        & $appcmd set apppool "$SiteName" '/managedRuntimeVersion:""' '/processModel.identityType:ApplicationPoolIdentity' | Out-Null

        # سایت
        $siteList = & $appcmd list site "$SiteName" 2>$null
        if (-not $siteList) {
            $addArgs = @(
                "/name:$SiteName",
                "/bindings:http/*:$($WebPort):",
                "/physicalPath:$SiteRoot",
                "/applicationDefaults.applicationPool:$SiteName"
            )
            & $appcmd add site @addArgs | Out-Null
        } else {
            & $appcmd set app "$SiteName/" "/applicationPool:$SiteName" | Out-Null
            & $appcmd set vdir "$SiteName/" "/physicalPath:$SiteRoot" | Out-Null
        }

        # ثبت FastCGI و هندلر PHP (در سطح سرور، idempotent)
        $PhpCgi = if ($PhpDir) { Join-Path $PhpDir 'php-cgi.exe' } else { $null }
        if ($PhpCgi -and (Test-Path $PhpCgi)) {
            $fcgi = & $appcmd list config /section:fastCGI /text:fullPath 2>$null
            if (-not (($fcgi -join ' ') -match 'php-cgi\.exe')) {
                & $appcmd set config /section:system.webServer/fastCGI "/+[fullPath='$PhpCgi',activityTimeout='600',requestTimeout='600',instanceMaxRequests='10000']" /commit:apphost 2>&1 | Out-Null
            }
            $hdl = & $appcmd list config /section:handlers /text:path 2>$null
            if (-not (($hdl -join ' ') -match '\*\.php')) {
                & $appcmd set config /section:system.webServer/handlers "/+[name='PHP_via_FastCGI',path='*.php',verb='*',modules='FastCgiModule',scriptProcessor='$PhpCgi',resourceType='Either']" /commit:apphost 2>&1 | Out-Null
            }
        } else {
            Write-Host '      php-cgi.exe در دسترس نیست — هندلر PHP ثبت نشد؛ ابتدا مرحله ۳ را رفع کنید' -ForegroundColor Yellow
        }

        # استقرار فایل‌ها در زیرپوشه server (هم‌راستا با apiPath=server در اپ)
        $deployDir = Join-Path $SiteRoot 'server'
        New-Item -ItemType Directory -Force -Path $deployDir | Out-Null
        if (Test-Path $apiPhp)    { Copy-Item $apiPhp    $deployDir -Force }
        if (Test-Path $configPhp) { Copy-Item $configPhp $deployDir -Force }

        # web.config: بستن directory browsing + مسدود کردن دانلود config.php
        $webConfigPath = Join-Path $deployDir 'web.config'
        $webConfigXml = @'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <system.webServer>
    <directoryBrowse enabled="false" />
    <security>
      <requestFiltering removeServerHeader="true">
        <filteringRules>
          <filteringRule name="DenyConfigPhp" scanUrl="true" scanQueryString="true">
            <denyStrings>
              <add string="config.php" />
            </denyStrings>
          </filteringRule>
        </filteringRules>
      </requestFiltering>
    </security>
  </system.webServer>
</configuration>
'@
        [System.IO.File]::WriteAllText($webConfigPath, $webConfigXml, (New-Object System.Text.UTF8Encoding($true)))

        # دسترسی خواندن برای کاربر IIS
        & icacls $SiteRoot /grant 'IIS_IUSRS:(OI)(CI)RX' /T /Q 2>&1 | Out-Null

        # استارت سایت و اپلیکیشن‌پول
        & $appcmd start apppool "$SiteName" 2>&1 | Out-Null
        & $appcmd start site "$SiteName" 2>&1 | Out-Null

        # بررسی نهایی: پورت در حال گوش دادن است؟
        Start-Sleep -Milliseconds 700
        $listen = Get-NetTCPConnection -State Listen -LocalPort $WebPort -ErrorAction SilentlyContinue
        $hadSite = [bool]$siteList
        if ($listen) {
            if ($hadSite) { Step-Ok $stepName "سایت «$SiteName» روی http://0.0.0.0`:$WebPort فعال است (فایل‌ها تازه‌سازی شد: $deployDir)" }
            else          { Step-Fixed $stepName "سایت «$SiteName» ایجاد و فایل‌ها در $deployDir مستقر شدند" }
        } else {
            Step-Fail $stepName "پورت $WebPort در حالت Listen نیست — وضعیت سایت را با inetmgr بررسی کنید"
        }
    } catch {
        Step-Fail $stepName "پیکربندی سایت ناموفق: $($_.Exception.Message)"
    }
}

# ─────────── ۷) فایروال ───────────
Write-StepHeader 7 "فایروال ویندوز — باز بودن پورت $WebPort"
$stepName = "فایروال (TCP $WebPort)"
$ruleName = "Vizitor API (TCP $WebPort)"
try {
    $rule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
    $portOk = $false
    if ($rule) {
        $pf = $rule | Get-NetFirewallPortFilter
        $portOk = (($pf.LocalPort -contains "$WebPort") -or ($pf.LocalPort -contains 'Any'))
        $ruleOk = ($rule.Enabled -eq 'True') -and ($rule.Action -eq 'Allow') -and $portOk
    } else { $ruleOk = $false }

    if ($ruleOk) {
        Step-Ok $stepName "قانون «$ruleName» فعال است"
    } else {
        if ($rule -and -not $portOk) {
            Remove-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
            $rule = $null
        }
        if ($rule) {
            Enable-NetFirewallRule -DisplayName $ruleName -ErrorAction Stop
            Set-NetFirewallRule -DisplayName $ruleName -Action Allow -ErrorAction Stop
        } else {
            New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP `
                -LocalPort $WebPort -Action Allow -Profile Any -ErrorAction Stop | Out-Null
        }
        $check = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
        if ($check -and $check.Enabled -eq 'True' -and $check.Action -eq 'Allow') {
            Step-Fixed $stepName "قانون بازکردن پورت $WebPort ایجاد/فعال شد"
        } else {
            Step-Fail $stepName 'قانون فایروال پس از تلاش نیز تأیید نشد'
        }
    }
} catch {
    Step-Fail $stepName "خطای فایروال: $($_.Exception.Message)"
}

# تشخیص محلی بودن SQL
$localIps = @('127.0.0.1', 'localhost', '.')
try { $localIps += @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | ForEach-Object { $_.IPAddress }) } catch { }
$sqlIsLocal = ($DbHost -in $localIps) -or ($DbHost -eq $PublicHost) -or ($DbHost -eq "$env:COMPUTERNAME")

# ─────────── ۸) SQL Server TCP/IP ───────────
Write-StepHeader 8 "SQL Server — فعال بودن TCP/IP روی پورت $DbPort"
$stepName = 'SQL Server TCP/IP'
$script:SqlInstanceSuffix = $null
$script:SqlServiceName    = $null
$instReg = 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL'
if (Test-Path $instReg) {
    $props = (Get-ItemProperty $instReg).PSObject.Properties | Where-Object { $_.Name -notlike 'PS*' }
    $def = $props | Where-Object { $_.Name -eq 'MSSQLSERVER' } | Select-Object -First 1
    $chosen = if ($def) { $def } else { $props | Select-Object -First 1 }
    if ($chosen) {
        $script:SqlInstanceSuffix = "$($chosen.Value)"
        $instName = "$($chosen.Name)"
        $script:SqlServiceName = if ($instName -eq 'MSSQLSERVER') { 'MSSQLSERVER' } else { "MSSQL`$$instName" }
    }
}
if (-not $sqlIsLocal) {
    Step-Skip $stepName "SQL روی ماشین دیگری است ($DbHost) — فعال‌سازی TCP/IP را روی همان ماشین انجام دهید"
} elseif (-not $script:SqlInstanceSuffix) {
    Step-Fail $stepName 'هیچ نمونه SQL Server روی این ماشین یافت نشد — SQL Server را نصب/بررسی کنید'
} else {
    $tcpKey = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$($script:SqlInstanceSuffix)\MSSQLServer\SuperSocketNetLib\Tcp"
    $ipAll  = Join-Path $tcpKey 'IPAll'
    $needFix = $true
    if (Test-Path $tcpKey) {
        $enabled = (Get-ItemProperty $tcpKey -ErrorAction SilentlyContinue).Enabled
        $port    = (Get-ItemProperty $ipAll  -ErrorAction SilentlyContinue).TcpPort
        $needFix = -not (($enabled -eq 1) -and ("$port" -eq "$DbPort"))
    }
    if (-not $needFix) {
        Step-Ok $stepName "TCP/IP فعال و پورت $DbPort تنظیم است"
    } else {
        try {
            New-Item -Path $tcpKey -Force | Out-Null
            Set-ItemProperty -Path $tcpKey -Name 'Enabled' -Value 1 -Type DWord
            New-Item -Path $ipAll -Force | Out-Null
            Set-ItemProperty -Path $ipAll -Name 'TcpPort' -Value "$DbPort"
            Set-ItemProperty -Path $ipAll -Name 'TcpDynamicPorts' -Value ''
            $script:RestartSqlService = $true
            Step-Fixed $stepName "TCP/IP فعال و پورت $DbPort تنظیم شد"
        } catch {
            Step-Fail $stepName "تغییر رجیستری SQL ناموفق: $($_.Exception.Message)"
        }
    }
}

# ─────────── ۹) Mixed Mode ───────────
Write-StepHeader 9 'SQL Server — Mixed Mode Authentication (ورود با کاربر SQL)'
$stepName = 'SQL Mixed Mode'
if (-not $sqlIsLocal) {
    Step-Skip $stepName 'SQL روی ماشین دیگری است — Mixed Mode را روی همان ماشین فعال کنید (SQL Server Properties → Security)'
} elseif (-not $script:SqlInstanceSuffix) {
    Step-Fail $stepName 'نمونه SQL یافت نشد'
} else {
    $srvKey = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$($script:SqlInstanceSuffix)\MSSQLServer"
    $mode = (Get-ItemProperty $srvKey -ErrorAction SilentlyContinue).LoginMode
    if ($mode -eq 2) {
        Step-Ok $stepName 'Mixed Mode از قبل فعال است'
    } else {
        try {
            Set-ItemProperty -Path $srvKey -Name 'LoginMode' -Value 2 -Type DWord -ErrorAction Stop
            $script:RestartSqlService = $true
            Step-Fixed $stepName 'Mixed Mode فعال شد'
        } catch {
            Step-Fail $stepName "فعال‌سازی Mixed Mode ناموفق: $($_.Exception.Message)"
        }
    }
}

# ─────────── ری‌استارت امن سرویس SQL (با تأیید + حفظ سرویس‌های وابسته) ───────────
if ($script:RestartSqlService -and $script:SqlServiceName -and $sqlIsLocal) {
    $svc = $script:SqlServiceName
    if ($NoSqlRestart) {
        Step-Warn 'ری‌استارت SQL Server' "تغییرات اعمال شد ولی طبق -NoSqlRestart ری‌استارت نشد — در فرصت مناسب: services.msc ← SQL Server ($svc) ← Restart"
    } else {
        $doRestart = $Yes.IsPresent
        if (-not $doRestart) {
            try {
                $ans = Read-Host "      برای اعمال تنظیمات، سرویس $svc باید ری‌استارت شود (اتصال‌های جاری لحظه‌ای قطع می‌شود). ری‌استارت شود؟ [y/N]"
                $doRestart = ($ans -match '^[yY]')
            } catch { $doRestart = $false }
        }
        if (-not $doRestart) {
            Step-Warn 'ری‌استارت SQL Server' "طبق انتخاب شما ری‌استارت نشد — تغییرات پس از ری‌استارت دستی سرویس فعال می‌شوند (services.msc ← SQL Server ($svc) ← Restart)"
        } else {
            Write-Host "      ری‌استارت امن سرویس $svc (به‌همراه بالا آوردن دوباره سرویس‌های وابسته)…" -ForegroundColor Yellow
            $stoppedDeps = @()
            try {
                # سرویس‌های وابسته در حال اجرا (SQL Agent، سرویس‌های نرم‌افزار حسابداری و…)
                $deps = @(Get-Service -Name $svc -DependentServices -ErrorAction SilentlyContinue | Where-Object { $_.Status -eq 'Running' })
                foreach ($d in $deps) {
                    Write-Host "      توقف موقت سرویس وابسته: $($d.DisplayName)" -ForegroundColor DarkGray
                    try { Stop-Service -InputObject $d -Force -ErrorAction Stop; $script:StoppedDepsList += $d.Name } catch { }
                }
                Restart-Service -Name $svc -ErrorAction Stop
                $waited = 0
                $open = $false
                do {
                    Start-Sleep -Seconds 2; $waited += 2
                    $open = Test-NetConnection -ComputerName 127.0.0.1 -Port $DbPort -InformationLevel Quiet -WarningAction SilentlyContinue
                } while (-not $open -and $waited -lt 60)
                if ($open) {
                    Write-Host "      ✔ سرویس $svc بالا آمد و پورت $DbPort پاسخ می‌دهد" -ForegroundColor Green
                } else {
                    Write-Host "      ✖ سرویس پس از ۶۰ ثانیه پاسخ نداد! فوری این را بزنید:  Start-Service $svc" -ForegroundColor Red
                    Write-Host '        و در services.msc وضعیت SQL Server را ببینید (جزئیات خطا: Event Viewer ← Application ← MSSQLSERVER)' -ForegroundColor Yellow
                }
            } catch {
                Write-Host "      ✖ خطا در ری‌استارت: $($_.Exception.Message)" -ForegroundColor Red
                Write-Host "        راه‌حل فوری: Start-Service $svc" -ForegroundColor Yellow
            }
            # بازگرداندن سرویس‌های وابسته (مهم: بدون این، حسابداری/Agent استپ می‌ماند)
            foreach ($dn in $script:StoppedDepsList) {
                try {
                    Start-Service -Name $dn -ErrorAction Stop
                    Write-Host "      ✔ سرویس وابسته $dn دوباره استارت شد" -ForegroundColor Green
                } catch {
                    Write-Host "      ⚠ سرویس وابسته $dn استارت نشد — دستی بالا بیارید: Start-Service $dn" -ForegroundColor Yellow
                }
            }
            if ($script:StoppedDepsList.Count -eq 0) {
                $postCheck = Test-NetConnection -ComputerName 127.0.0.1 -Port $DbPort -InformationLevel Quiet -WarningAction SilentlyContinue
                if ($postCheck) { Step-Fixed 'ری‌استارت SQL Server' "سرویس $svc ری‌استارت شد و پورت $DbPort سالم است" }
                else            { Step-Fail  'ری‌استارت SQL Server' "سرویس پس از ری‌استارت پاسخ نداد — Start-Service $svc را دستی بزنید" }
            } else {
                Step-Fixed 'ری‌استارت SQL Server' "سرویس $svc و $($script:StoppedDepsList.Count) سرویس وابسته مدیریت شد"
            }
        }
    }
}

# ─────────── ۱۰) اتصال به SQL با کاربر config ───────────
Write-StepHeader 10 "اتصال به دیتابیس $DbName با کاربر $DbUser"
$stepName = 'اتصال SQL با کاربر config'
$sqlTarget = if ($sqlIsLocal) { "tcp:127.0.0.1,$DbPort" } else { "tcp:$DbHost,$DbPort" }
function Test-SqlLogin([string]$User, [string]$Pass) {
    $b = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
    $b.DataSource = $script:SqlTargetLocal
    $b.InitialCatalog = $script:DbNameLocal
    $b.UserID = $User
    $b.Password = $Pass
    $b.Encrypt = $false
    $b.TrustServerCertificate = $true
    $b.ConnectTimeout = 6
    $cn = New-Object System.Data.SqlClient.SqlConnection($b.ConnectionString)
    try { $cn.Open(); $cn.Close(); return $true } catch { return $false }
}
$script:SqlTargetLocal = $sqlTarget
$script:DbNameLocal    = $DbName
if ([string]::IsNullOrWhiteSpace($DbUser)) {
    Step-Fail $stepName 'DB_USER در config.php خوانده نشد — تست اتصال ممکن نیست'
} elseif (Test-SqlLogin $DbUser $DbPass) {
    Step-Ok $stepName "ورود موفق به $DbName"
} elseif ($SkipSqlLoginFix) {
    Step-Fail $stepName 'ورود ناموفق و اصلاح خودکار غیرفعال است — لاگین SQL را دستی بررسی کنید'
} elseif (-not $sqlIsLocal) {
    Step-Fail $stepName "ورود به $DbHost ناموفق — کمی شبکه/فایروال SQL (TCP 1433) و لاگین را روی سرور SQL بررسی کنید"
} else {
    # تلاش برای ساخت/اصلاح لاگین با اتصال Windows-Auth
    $fixed = $false
    try {
        $tb = New-Object System.Data.SqlClient.SqlConnectionStringBuilder
        $tb.DataSource = "tcp:127.0.0.1,$DbPort"
        $tb.InitialCatalog = 'master'
        $tb.IntegratedSecurity = $true
        $tb.Encrypt = $false
        $tb.TrustServerCertificate = $true
        $tb.ConnectTimeout = 6
        $uEsc = $DbUser -replace "'", "''"
        $pEsc = $DbPass -replace "'", "''"
        $dEsc = $DbName -replace "\]", "]]"
        $tsql = @"
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'$uEsc')
    CREATE LOGIN [$DbUser] WITH PASSWORD = N'$pEsc', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
ELSE
    ALTER LOGIN [$DbUser] WITH PASSWORD = N'$pEsc', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
ALTER LOGIN [$DbUser] ENABLE;
USE [$dEsc];
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'$uEsc')
    CREATE USER [$DbUser] FOR LOGIN [$DbUser];
EXEC sp_addrolemember N'db_owner', N'$uEsc';
"@
        $cn = New-Object System.Data.SqlClient.SqlConnection($tb.ConnectionString)
        $cn.Open()
        $cmd = $cn.CreateCommand()
        $cmd.CommandText = $tsql
        $cmd.CommandTimeout = 20
        $null = $cmd.ExecuteNonQuery()
        $cn.Close()
        Start-Sleep -Milliseconds 400
        $fixed = Test-SqlLogin $DbUser $DbPass
    } catch {
        $fixed = $false
        Write-Host "      اصلاح خودکار لاگین ناموفق: $($_.Exception.Message)" -ForegroundColor Yellow
    }
    if ($fixed) {
        Step-Fixed $stepName "لاگین $DbUser ساخته/فعال و رمز آن با config هماهنگ شد"
    } else {
        Step-Fail $stepName "ورود به SQL ناموفق — در SSMS لاگین $DbUser را بررسی کنید (رمز با config.php یکسان باشد و LOGIN فعال)"
    }
}

# ─────────── ۱۱) تست سرتاسری API ───────────
Write-StepHeader 11 'تست سرتاسری وب‌سرویس (ping / امنیت / محافظت config.php)'
$stepName = 'تست سرتاسری API'
$headers = if ($ApiKey) { @{ 'X-Api-Key' = $ApiKey } } else { @{} }
$baseApi = "http://127.0.0.1:$WebPort/server/api.php"
$ping    = Invoke-Api "$baseApi`?action=ping" $headers
$apiAllOk = $true
if ($ping.Code -eq 200 -and $ping.Json -and $ping.Json.success -eq $true) {
    $d = $ping.Json.data
    Write-Host "      ✔ ping موفق — DB=$($d.db) | PHP=$($d.php) | API=v$($d.api_version)" -ForegroundColor Green
    # نتیجهٔ جداول
    if ($d.tables) {
        foreach ($tKey in @('products', 'customers', 'invoices', 'sal_mali')) {
            $c = $d.tables.$tKey
            if ($null -eq $c) {
                Write-Host "      ⚠ جدول $tKey یافت نشد — نام TBL_* را در config.php با جداول واقعی آتیران تطبیق دهید" -ForegroundColor Yellow
                $apiAllOk = $false
            } else {
                Write-Host "      ✔ جدول $tKey : $c رکورد" -ForegroundColor Green
            }
        }
    }
} else {
    $apiAllOk = $false
    $msg = if ($ping.Json -and $ping.Json.message) { $ping.Json.message } else { $ping.Body }
    Write-Host "      ✖ ping ناموفق (HTTP $($ping.Code)): $msg" -ForegroundColor Red
}

# تست امنیت کلید: بدون کلید باید 401 بگیریم
$noKey = Invoke-Api "$baseApi`?action=ping" @{}
if (($noKey.Code -eq 401) -or ($noKey.Json -and $noKey.Json.success -eq $false)) {
    Write-Host '      ✔ احراز هویت کلید API فعال است (درخواست بدون کلید رد می‌شود)' -ForegroundColor Green
} else {
    $apiAllOk = $false
    Write-Host "      ✖ درخواست بدون کلید رد نشد (HTTP $($noKey.Code)) — api.php احراز هویت ندارد!" -ForegroundColor Red
}

# تست محافظت config.php
$cfgTest = Invoke-Api "http://127.0.0.1:$WebPort/server/config.php" @{}
if ($cfgTest.Code -in @(403, 404)) {
    Write-Host '      ✔ دانلود مستقیم config.php مسدود است' -ForegroundColor Green
} elseif ($cfgTest.Code -eq 200) {
    # تلاش برای اصلاح مجدد web.config
    $deployDir2 = Join-Path $SiteRoot 'server'
    $webConfigPath2 = Join-Path $deployDir2 'web.config'
    if (Test-Path $webConfigPath2) {
        & $appcmd start site "$SiteName" 2>&1 | Out-Null
        Start-Sleep -Milliseconds 600
        $cfgTest2 = Invoke-Api "http://127.0.0.1:$WebPort/server/config.php" @{}
        if ($cfgTest2.Code -in @(403, 404)) {
            Write-Host '      ✔ محافظت config.php پس از تازه‌سازی فعال شد' -ForegroundColor Green
        } else {
            $apiAllOk = $false
            Write-Host '      ✖ config.php همچنان قابل دریافت است — requestFiltering را دستی بررسی کنید' -ForegroundColor Red
        }
    } else {
        $apiAllOk = $false
        Write-Host '      ✖ web.config یافت نشد و config.php آزاد است' -ForegroundColor Red
    }
} else {
    Write-Host "      – وضعیت config.php: HTTP $($cfgTest.Code) (قابل قبول در صورت عدم نمایش محتوا)" -ForegroundColor DarkGray
}
if ($apiAllOk) { Step-Ok $stepName "http://127.0.0.1`:$WebPort/server/api.php سالم است" }
else           { Step-Fail $stepName 'یک یا چند زیرتست API ناموفق بود — جزئیات بالا' }

# ─────────── ۱۲) دسترس‌پذیری بیرونی ───────────
Write-StepHeader 12 "دسترس‌پذیری از بیرون: http://$PublicHost`:$WebPort"
$stepName = 'دسترس‌پذیری IP عمومی'
try {
    $ext = Test-NetConnection -ComputerName $PublicHost -Port $WebPort -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($ext) {
        Step-Ok $stepName "پورت $WebPort روی $PublicHost پاسخ می‌دهد — اپ اندروید می‌تواند متصل شود"
    } else {
        Step-Warn $stepName "از این ماشین به $PublicHost`:$WebPort دسترسی نیست؛ اگر سرور پشت NAT است «بازگشت NAT» ممکن است تست داخلی را خراب کند — با موبایل روی اینترنت تست کنید"
    }
} catch {
    Step-Warn $stepName 'تست بیرونی انجام نشد — بعداً با «تست سلامت اتصال» در اپ بررسی کنید'
}

# ═══════════════════════════ خلاصه نهایی ═══════════════════════════════════
Write-Host ''
Write-Host '════════════════ خلاصه نهایی ════════════════' -ForegroundColor Cyan
$script:Steps | Format-Table -AutoSize | Out-String | Write-Host
$failCount = @($script:Steps | Where-Object { $_.Status -eq 'FAIL' }).Count
$okCount   = @($script:Steps | Where-Object { $_.Status -in @('OK', 'FIXED') }).Count
$warnCount = @($script:Steps | Where-Object { $_.Status -eq 'WARN' }).Count
Write-Host "نتیجه: $okCount سالم/اصلاح‌شده | $warnCount هشدار | $failCount ناموفق" -ForegroundColor $(if ($failCount -gt 0) { 'Red' } else { 'Green' })

if ($script:RestartNeeded) {
    Write-Host ''
    Write-Host '⚠ برخی ویژگی‌های ویندوز نیاز به ری‌استارت دارند — پس از پایان، سرور را یک‌بار ری‌استارت کنید.' -ForegroundColor Yellow
}

Write-Host ''
Write-Host '── تنظیمات اپ اندروید (منوی گزارشات ← پیکربندی سرور) ──' -ForegroundColor Cyan
Write-Host "   IP سرور       : $PublicHost"
Write-Host "   پورت وب‌سرویس  : $WebPort"
Write-Host '   پورت SQL      : 1433'
Write-Host '   مسیر API      : server'
Write-Host "   کلید API      : $ApiKey"
Write-Host "   آدرس کامل     : http://$PublicHost`:$WebPort/server/api.php"
Write-Host '   در اپ «بازگشت به پیش‌فرض سرور میلانو» هم همین مقادیر را می‌نویسد.' -ForegroundColor DarkGray
Write-Host ''

try { Stop-Transcript | Out-Null } catch { }

if ($Host.Name -eq 'ConsoleHost') {
    try { Read-Host 'برای خروج Enter بزنید' | Out-Null } catch { }
}
if ($failCount -gt 0) { exit 1 } else { exit 0 }
