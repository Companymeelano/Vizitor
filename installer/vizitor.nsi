; ===========================================================================
;  سامانهٔ ویزیتور — نصب‌کنندهٔ ویندوز (Vizitor Windows Setup)
;  NSIS 3 / MUI2  •  Unicode  •  RTL (فارسی)  •  Windows 7 / 8 / 10 / 11
;
;  هستهٔ منطقی نصب، همان install.ps1 است (تشخیص خودکار آی‌پی، چک و تعمیر،
;  رفتار کاملاً غیرتلفیقی روی SQL Server). این فایل پوستهٔ گرافیکی،
;  بخش‌بندی تیک‌خور، صفحهٔ فعال‌سازی و صفحهٔ تنظیمات سرور/دیتابیس است و
;  پاسخ‌ها را در یک فایل INI به install.ps1 می‌دهد.
;
;  ساخت:  makensis installer\vizitor.nsi      (ویندوز: installer\build.bat)
; ===========================================================================

Unicode true
ManifestDPIAware true
ManifestSupportedOS all

SetCompressor /SOLID lzma
SetCompressorDictSize 32

!define PRODUCT     "Vizitor"
!define PRODUCT_FA  "سامانهٔ ویزیتور"
!define VERSION     "1.0.0"
!define PUBLISHER   "Atiran"
!define TASK_NAME   "VizitorAPI"
!define APP_HOME    "C:\Vizitor"

RequestExecutionLevel admin

Name "${PRODUCT_FA} ${VERSION}"
OutFile "..\release\Vizitor-Setup-${VERSION}.exe"
InstallDir "${APP_HOME}"
InstallDirRegKey HKLM "Software\Vizitor" "InstallDir"
BrandingText "${PRODUCT_FA} ${VERSION}"
Icon "assets\vizitor.ico"
UninstallIcon "assets\vizitor.ico"

VIProductVersion "1.0.0.0"
VIAddVersionKey /LANG=1033 "ProductName"     "Vizitor Server"
VIAddVersionKey /LANG=1033 "FileDescription" "Vizitor Server Setup"
VIAddVersionKey /LANG=1033 "FileVersion"     "${VERSION}"
VIAddVersionKey /LANG=1033 "ProductVersion"  "${VERSION}"
VIAddVersionKey /LANG=1033 "CompanyName"     "${PUBLISHER}"
VIAddVersionKey /LANG=1033 "LegalCopyright"  "Atiran"

!include "MUI2.nsh"
!include "nsDialogs.nsh"
!include "LogicLib.nsh"
!include "FileFunc.nsh"
!include "WinVer.nsh"
!include "x64.nsh"

; ---------------------------------------------------------------------------
;  متغیرها
; ---------------------------------------------------------------------------
Var /GLOBAL PageDialog
Var /GLOBAL RecheckOnly
Var /GLOBAL PrevExists
Var /GLOBAL FreshClean
Var /GLOBAL PyExe
Var /GLOBAL hHealth
Var /GLOBAL hPublicIp
Var /GLOBAL hExternal
Var /GLOBAL PublicIpField
Var /GLOBAL ExternalOk
Var /GLOBAL hDbList
Var /GLOBAL hBtnList
Var /GLOBAL hListStatus
Var /GLOBAL DbCount
Var /GLOBAL DbIdx
Var /GLOBAL HealthWanted
Var /GLOBAL IpAddr
Var /GLOBAL ApiPort
Var /GLOBAL SqlHost
Var /GLOBAL SqlPort
Var /GLOBAL SqlAuth
Var /GLOBAL SqlUser
Var /GLOBAL SqlPass
Var /GLOBAL DbName
Var /GLOBAL ErpDb
Var /GLOBAL AdminUser
Var /GLOBAL ActCode
Var /GLOBAL ActLater
Var /GLOBAL ApiUrlDisplay
Var /GLOBAL InstallRc
Var /GLOBAL Detected
Var /GLOBAL hIp
Var /GLOBAL hPort
Var /GLOBAL hSqlHost
Var /GLOBAL hSqlPort
Var /GLOBAL hWinAuth
Var /GLOBAL hSqlUser
Var /GLOBAL hSqlPass
Var /GLOBAL hDbName
Var /GLOBAL hStatus
Var /GLOBAL hAct
Var /GLOBAL hActLater
Var /GLOBAL hUrlHint

; ---------------------------------------------------------------------------
;  ظاهر
; ---------------------------------------------------------------------------
!define MUI_ICON                        "assets\vizitor.ico"
!define MUI_UNICON                      "assets\vizitor.ico"
!define MUI_WELCOMEFINISHPAGE_BITMAP    "assets\wizard-welcome.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP  "assets\wizard-welcome.bmp"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP          "assets\wizard-header.bmp"
!define MUI_ABORTWARNING
!define MUI_UNABORTWARNING
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

!define MUI_WELCOMEPAGE_TITLE "به نصب‌کنندهٔ ${PRODUCT_FA} خوش آمدید"
!define MUI_WELCOMEPAGE_TEXT  "این برنامه، سرور ویزیتور را روی این کامپیوتر نصب و آمادهٔ استفاده می‌کند:$\r$\n$\r$\n• سرویس API و اتصال برنامهٔ اندروید$\r$\n• دیتابیس سامانه در SQL Server (فقط ساخت چیزهای ناموجود؛ هیچ دادهٔ موجودی تغییر نمی‌کند)$\r$\n• پنل مدیریت، قاعده‌های فایروال و راهنماهای فارسی$\r$\n$\r$\nدر صفحهٔ بعد، بخش‌های موردنیاز را تیک بزنید."

!define MUI_LICENSEPAGE_TEXT_TOP    "لطفاً شرایط استفاده را بخوانید."
!define MUI_LICENSEPAGE_TEXT_BOTTOM "برای ادامه، شرایط بالا را بپذیرید."
!define MUI_COMPONENTSPAGE_TEXT_TOP "هر بخشی که لازم دارید تیک بزنید؛ توضیح هر بخش پایین همین صفحه نمایش داده می‌شود."
!define MUI_DIRECTORYPAGE_TEXT_TOP  "پوشهٔ نصب سامانه. سرویس API، ابزارها و راهنماها اینجا قرار می‌گیرند."

!define MUI_FINISHPAGE_TITLE          "نصب ${PRODUCT_FA} کامل شد"
!define MUI_FINISHPAGE_TEXT           "آدرس API برای برنامهٔ اندروید:$\r$\n$ApiUrlDisplay$\r$\n$\r$\nاتصال مستقیم اندروید به SQL Server (پورت ۱۴۳۳) آماده شد؛ جزئیات و کد QR:$\r$\n$INSTDIR\setup\android-connect.png$\r$\n$\r$\nتنظیمات و لاگ‌ها:  C:\ProgramData\Vizitor$\r$\n$\r$\nاگر بازرسی خودکار به مشکل خورد، از میان‌بر «بازرسی و تعمیر» استفاده کنید."
!define MUI_FINISHPAGE_RUN          "$INSTDIR\tools\open-panel.bat"
!define MUI_FINISHPAGE_RUN_FUNCTION   OpenPanel
!define MUI_FINISHPAGE_RUN_TEXT       "بازکردن پنل مدیریت در مرورگر"
!define MUI_FINISHPAGE_SHOWREADME     "$INSTDIR\setup\android-connect.txt"
!define MUI_FINISHPAGE_SHOWREADME_TEXT "نمایش کارت اتصال اندروید (سرور، دیتابیس، کاربر، کد راه‌اندازی)"
!define MUI_FINISHPAGE_LINK           "راهنمای فارسی و بستهٔ مهاجرت (پوشهٔ docs)"
!define MUI_FINISHPAGE_LINK_LOCATION  "$INSTDIR\docs"

!define MUI_PAGE_CUSTOMFUNCTION_PRE PagePreSkip
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "assets\license-fa.txt"
!define MUI_PAGE_CUSTOMFUNCTION_PRE PagePreSkip
!insertmacro MUI_PAGE_COMPONENTS
!define MUI_PAGE_CUSTOMFUNCTION_PRE PagePreSkip
!insertmacro MUI_PAGE_DIRECTORY
Page custom PageServerCreate PageServerLeave
Page custom PageDbCreate PageDbLeave
Page custom PageActivationCreate PageActivationLeave
!insertmacro MUI_PAGE_INSTFILES
!define MUI_PAGE_CUSTOMFUNCTION_PRE PagePreFinish
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "Farsi"

; ---------------------------------------------------------------------------
;  میان‌برها (ماکرو باید قبل از استفاده تعریف شود)
; ---------------------------------------------------------------------------
!macro CreateShortcuts
  SetShellVarContext all
  CreateDirectory "$SMPROGRAMS\${PRODUCT_FA}"
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\پنل مدیریت.lnk"      "$INSTDIR\tools\open-panel.bat" "" "$INSTDIR\tools\open-panel.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\راهنمای فارسی.lnk"    "$INSTDIR\tools\open-guide.bat" "" "$INSTDIR\tools\open-guide.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\بازرسی و تعمیر.lnk"   "$INSTDIR\tools\recheck.bat" "" "$INSTDIR\tools\recheck.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\شروع سرویس API.lnk"   "$INSTDIR\tools\service-start.bat" "" "$INSTDIR\tools\service-start.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\توقف سرویس API.lnk"   "$INSTDIR\tools\service-stop.bat" "" "$INSTDIR\tools\service-stop.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\اتصال مستقیم SQL.lnk" "$INSTDIR\tools\android-prep.bat" "" "$INSTDIR\tools\android-prep.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\کارت اتصال اندروید.lnk" "$INSTDIR\setup\android-connect.txt" "" "notepad.exe" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\حذف نصب.lnk"         "$INSTDIR\uninstall.exe"
  CreateShortCut "$DESKTOP\${PRODUCT_FA} — پنل مدیریت.lnk"        "$INSTDIR\tools\open-panel.bat" "" "$INSTDIR\tools\open-panel.bat" 0
!macroend

; ---------------------------------------------------------------------------
;  بخش‌ها
; ---------------------------------------------------------------------------
Section "هستهٔ سامانه — سرویس و پنل مدیریت (اجباری)" SecCore
  SectionIn RO

  ; اگر کاربر «پیکربندی دوباره» را انتخاب کرده باشد، فایل‌های برنامهٔ قبلی
  ; پاک می‌شوند تا نسخهٔ نو تمیز بنشیند. تنظیمات/لاگ/داده‌های
  ; C:\ProgramData\Vizitor و دیتابیس هیچ‌وقت دست نمی‌خورند.
  ${If} $FreshClean == "1"
    DetailPrint "توقف سرویس قبلی برای جایگزینی فایل‌ها ..."
    nsExec::ExecToLog 'schtasks /End /TN "${TASK_NAME}"'
    Pop $0
    Sleep 1200
    ${If} ${FileExists} "$INSTDIR\setup\*.*"
      DetailPrint "حذف فایل‌های نسخهٔ قبلی از $INSTDIR ..."
      RMDir /r "$INSTDIR\setup"
    ${EndIf}
    RMDir /r "$INSTDIR\api"
      RMDir /r "$INSTDIR\docs"
    RMDir /r "$INSTDIR\tools"
    Delete "$INSTDIR\run.bat"
    DetailPrint "فایل‌های برنامهٔ قبلی جایگزین شدند (تنظیمات و داده‌ها دست‌نخورده)"
  ${EndIf}
  SetShellVarContext all
  SetOutPath "$INSTDIR\setup"
  File "..\install.ps1"
  File "..\install.sh"
  File "..\README.md"
  File "..\INSTALL.md"
  File "..\ANDROID_INTEGRATION.md"
  File "nsi\preflight.ps1"
  SetOutPath "$INSTDIR"
  File "assets\vizitor.ico"
  SetOutPath "$INSTDIR\setup\api"
  File /r /x __pycache__ /x *.pyc "..\api\*.*"
  SetOutPath "$INSTDIR\setup\database"
  File /r "..\database\*.*"
  SetOutPath "$INSTDIR\tools"
  File "win\recheck.bat"
  File "win\service-start.bat"
  File "win\service-stop.bat"
  File "win\open-panel.bat"
  File "win\open-guide.bat"
  File "win\android-prep.bat"
  SetOutPath "$INSTDIR"
SectionEnd

Section "پیش‌نیازهای پایتون (pyodbc و درایور ODBC)" SecPrereq
SectionEnd

Section "دیتابیس سامانه در SQL Server" SecDb
SectionEnd

Section "قاعدهٔ فایروال ویندوز (۱۴۳۳ برای اندروید + پورت پنل)" SecFirewall
SectionEnd

Section "اتصال مستقیم اندروید به SQL Server (کاربر محدود + پورت ۱۴۳۳)" SecAndroidSql
SectionEnd

Section "راهنماها و اسکریپت‌های SQL (بستهٔ فارسی)" SecDocs
  SetShellVarContext all
  SetOutPath "$INSTDIR\docs"
  File "..\README.md"
  File "..\INSTALL.md"
  File "..\ANDROID_INTEGRATION.md"
  SetOutPath "$INSTDIR\docs\migration"
  File /r "..\android-sql-direct\*.*"
  SetOutPath "$INSTDIR"
SectionEnd

Section "میان‌بر دسکتاپ و منوی استارت" SecShortcuts
  !insertmacro CreateShortcuts
SectionEnd

Section "بازرسی و تعمیر خودکار پس از نصب" SecSelfCheck
SectionEnd

; ---------------------------------------------------------------------------
;  کار اصلی: فراخوانی install.ps1 با پاسخ‌ها و پرچم بخش‌های تیک‌نخورده
; ---------------------------------------------------------------------------
Section "-Run"
  SetShellVarContext all
  Call WriteAnswers

  StrCpy $1 ""
  SectionGetFlags ${SecPrereq} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 == 0
    StrCpy $1 "$1 -SkipPrerequisites"
  ${EndIf}
  SectionGetFlags ${SecDb} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 == 0
    StrCpy $1 "$1 -SkipDatabase"
  ${EndIf}
  SectionGetFlags ${SecFirewall} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 == 0
    StrCpy $1 "$1 -SkipFirewall"
  ${EndIf}
  SectionGetFlags ${SecAndroidSql} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 == 0
    StrCpy $1 "$1 -SkipAndroidPrep"
  ${EndIf}
  SectionGetFlags ${SecSelfCheck} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 == 0
    StrCpy $1 "$1 -SkipSelfCheck"
  ${EndIf}

  DetailPrint "اجرای موتور نصب (install.ps1) با بخش‌های انتخاب‌شده ..."
  nsExec::ExecToLog '"$SYSDIR\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "$INSTDIR\setup\install.ps1" -Answers "$PLUGINSDIR\answers.ini" -AppHome "$INSTDIR" $1'
  Pop $InstallRc
  DetailPrint "نتیجهٔ اجرای موتور نصب: $InstallRc"

  ; پاسخ‌ها (که رمزها داخلشان است) روی دیسک نمی‌مانند
  Delete "$PLUGINSDIR\answers.ini"
  Delete "$INSTDIR\setup\answers.ini"

  WriteUninstaller "$INSTDIR\uninstall.exe"
  WriteRegStr HKLM "Software\Vizitor" "InstallDir" "$INSTDIR"
  WriteRegStr HKLM "Software\Vizitor" "ApiUrl"     "$ApiUrlDisplay"
  WriteRegStr HKLM "Software\Vizitor" "Version"    "${VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "DisplayName"     "${PRODUCT_FA} ${VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "DisplayVersion"  "${VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "Publisher"       "${PUBLISHER}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "DisplayIcon"     "$INSTDIR\vizitor.ico"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "QuietUninstallString" '"$INSTDIR\uninstall.exe" /S'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "NoRepair" 1
  ${GetSize} "$INSTDIR" "/S=0K" $0 $1 $2
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor" "EstimatedSize" $0

  SectionGetFlags ${SecShortcuts} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 != 0
    Call ReadFinalUrl
    !insertmacro CreateShortcuts
  ${EndIf}
SectionEnd

; ---------------------------------------------------------------------------
;  توضیح بخش‌ها (بعد از تعریف بخش‌ها)
; ---------------------------------------------------------------------------
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCore}       "سرویس ویندوز (${TASK_NAME})، پنل مدیریت روی پورت ۹۵۹۵، فایل‌های پیکربندی و ابزارها. (اتصال برنامهٔ اندروید مستقل از این بخش و مستقیم به SQL Server است.)"
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPrereq}     "pyodbc و درایور ODBC مایکروسافت — همان چیزهایی که برای گفت‌وگو با SQL Server لازم است؛ فقط در صورت کمبود نصب می‌شوند."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDb}         "ساخت دیتابیس و جداول داخلی خود سامانه (پنل و فعال‌سازی) — کاملاً غیرتلفیقی؛ دیتابیس حسابداری شما دست‌نخورده می‌ماند."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecFirewall}   "باز کردن پورت ۱۴۳۳ فقط برای شبکهٔ محلی (اتصال مستقیم برنامهٔ اندروید به دیتابیس) و پورت ۹۵۹۵ برای پنل مدیریت."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecAndroidSql} "هستهٔ کار: کاربر محدود vizitor_android (اگر نباشد)، گرنت فقط برای اشیای تأییدشده، کارت اتصال و کد QR، و بررسی سلامت اتصال به دیتابیس حسابداری."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDocs}       "کپی راهنماهای فارسی، گزارش نهایی مهاجرت، اسکریپت‌های بازرسی SQL و ابزارهای بررسی."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecShortcuts}  "ساخت میان‌بر دسکتاپ و منوی استارت (پنل مدیریت، بازرسی و تعمیر، شروع/توقف سرویس، حذف نصب)."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecSelfCheck}  "بازرسی و تعمیر خودکار: دیتابیس سامانه، پنل، فعال‌سازی و در پایان «بررسی سلامت اتصال» دیتابیس حسابداری (تا ۴ دور)."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

; ---------------------------------------------------------------------------
;  توابع کمکی
; ---------------------------------------------------------------------------
Function PagePreSkip
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}
FunctionEnd

Function PagePreFinish
  Call ReadFinalUrl
FunctionEnd

Function ReadFinalUrl
  IfFileExists "$INSTDIR\setup\connect.txt" 0 done
    ReadINIStr $0 "$INSTDIR\setup\connect.txt" "connection" "apiurl"
    ${If} $0 != ""
      StrCpy $ApiUrlDisplay $0
    ${EndIf}
  done:
FunctionEnd

Function OpenPanel
  ${If} $ApiUrlDisplay == ""
    StrCpy $ApiUrlDisplay "http://127.0.0.1:9595/api"
  ${EndIf}
  ExecShell "open" "$ApiUrlDisplay"
FunctionEnd

Function WriteAnswers
  Delete "$PLUGINSDIR\answers.ini"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "addr"         "$IpAddr"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "port"         "$ApiPort"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "lanip"        "$IpAddr"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "apphome"      "$INSTDIR"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "engine"       "sqlserver"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "host"         "$SqlHost"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "port"         "$SqlPort"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "auth"         "$SqlAuth"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "user"         "$SqlUser"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "password"     "$SqlPass"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "name"         "$DbName"
  WriteINIStr "$PLUGINSDIR\answers.ini" "admin"      "username"     "$AdminUser"
  WriteINIStr "$PLUGINSDIR\answers.ini" "activation" "code"         "$ActCode"
  WriteINIStr "$PLUGINSDIR\answers.ini" "android"    "erpdb"        "$ErpDb"
  WriteINIStr "$PLUGINSDIR\answers.ini" "android"    "login"        "vizitor_android"
  WriteINIStr "$PLUGINSDIR\answers.ini" "android"    "openfirewall" "1"
  WriteINIStr "$PLUGINSDIR\answers.ini" "db"         "health"       "$HealthWanted"
  WriteINIStr "$PLUGINSDIR\answers.ini" "android"    "external"     "$ExternalOk"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "publicip"     "$PublicIpField"
  WriteINIStr "$PLUGINSDIR\answers.ini" "ui"         "source"       "vizitor-setup"
FunctionEnd

; ---------------------------------------------------------------------------
;  صفحهٔ تنظیمات سرور و دیتابیس
; ---------------------------------------------------------------------------
Function PageServerCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "سرور سامانه" "آدرس سرور و پورت — اتصال اندروید مستقل و مستقیم به دیتابیس است"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 0 100% 9u "سامانه ویزیتور روی پورت پیش‌فرض ۹۵۹۵ اجرا می‌شود. رمزها هرگز نمایش داده نمی‌شوند."
  Pop $0

  ${NSD_CreateGroupBox} 0 10u 100% 72u "سرور سامانه (پنل مدیریت روی همین پورت بالا می‌آید)"
  Pop $0
  ${NSD_CreateLabel} 2% 20u 34% 9u "آدرس سرور (IP یا دامنه):"
  Pop $0
  ${NSD_CreateText} 38% 19u 60% 12u "$IpAddr"
  Pop $hIp
  ${NSD_CreateLabel} 2% 32u 34% 9u "پورت سامانه:"
  Pop $0
  ${NSD_CreateText} 38% 31u 20% 12u "$ApiPort"
  Pop $hPort
  ${NSD_CreateLabel} 2% 45u 96% 9u "اتصال برنامهٔ اندروید مستقیم به SQL Server روی پورت ۱۴۳۳ است؛ IIS و API در این مسیر نقشی ندارند."
  Pop $0
  ${NSD_CreateLabel} 2% 55u 96% 9u "پنل مدیریت (اختیاری) روی همین پورت با آدرس  .../api/health  باز می‌شود."
  Pop $0
  ${NSD_CreateLabel} 2% 66u 96% 9u "قاعدهٔ فایروال این پورت فقط برای شبکهٔ محلی ساخته می‌شود."
  Pop $0
  ${NSD_CreateLabel} 2% 78u 96% 9u "آدرس نهایی سامانه:  $ApiUrlDisplay"
  Pop $hUrlHint

  ${NSD_CreateLabel} 0 92u 100% 22u "$Detected"
  Pop $hStatus

  Call RefreshUrlHint
  nsDialogs::Show
FunctionEnd

Function OnListDbClick
  ${NSD_GetText} $hSqlHost $1
  ${NSD_GetText} $hSqlPort $2
  ${NSD_GetText} $hSqlUser $3
  ${NSD_GetText} $hSqlPass $4
  ${If} $1 == ""
    StrCpy $1 "localhost"
  ${EndIf}
  ${If} $2 == ""
    StrCpy $2 "1433"
  ${EndIf}

  Delete "$PLUGINSDIR\dbcreds.ini"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "server" "$1"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "port" "$2"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "user" "$3"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "pass" "$4"

  ${NSD_SetText} $hListStatus "در حال گرفتن لیست دیتابیس‌ها ... چند ثانیه صبر کنید"
  Delete "$PLUGINSDIR\dbs.ini"
  nsExec::ExecToStack '"$PyExe" "$PLUGINSDIR\sql_admin_tools.py" databases --creds "$PLUGINSDIR\dbcreds.ini" --out-ini "$PLUGINSDIR\dbs.ini"'
  Pop $R0
  Pop $R1

  ${NSD_LB_Clear} $hDbList
  StrCpy $DbCount "0"
  IfFileExists "$PLUGINSDIR\dbs.ini" 0 list_failed
    ReadINIStr $DbCount "$PLUGINSDIR\dbs.ini" "result" "count"
    ${If} $DbCount == ""
      StrCpy $DbCount "0"
    ${EndIf}
    StrCmp $DbCount "0" list_empty
    StrCpy $DbIdx "1"
  loop_db:
    IntCmp $DbIdx $DbCount list_done
    ReadINIStr $5 "$PLUGINSDIR\dbs.ini" "db$DbIdx" "name"
    ${If} $5 != ""
      ${NSD_LB_AddString} $hDbList "$5"
    ${EndIf}
    IntOp $DbIdx $DbIdx + 1
    Goto loop_db
  list_done:
    ReadINIStr $6 "$PLUGINSDIR\dbs.ini" "db1" "name"
    ${If} $6 != ""
      ${NSD_LB_SelectString} $hDbList "$6"
    ${EndIf}
    ${NSD_SetText} $hListStatus "لیست گرفته شد ($DbCount دیتابیس). دیتابیس حسابداری را انتخاب کنید."
    Goto list_end
  list_empty:
    ${NSD_SetText} $hListStatus "هیچ دیتابیس کاربری پیدا نشد — سرور، کاربر و رمز را بررسی کنید."
    Goto list_end
  list_failed:
    ${NSD_SetText} $hListStatus "گرفتن لیست ممکن نشد (کد $R0): سرور/پورت/کاربر/رمز را بررسی کنید یا نام را دستی بنویسید."
  list_end:
FunctionEnd

Function OnWinAuthClick
  Call ToggleSqlFields
FunctionEnd

Function ToggleSqlFields
  ${NSD_GetState} $hWinAuth $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $SqlAuth "windows"
    EnableWindow $hSqlUser 0
    EnableWindow $hSqlPass 0
  ${Else}
    StrCpy $SqlAuth "sql"
    EnableWindow $hSqlUser 1
    EnableWindow $hSqlPass 1
  ${EndIf}
FunctionEnd

Function RefreshUrlHint
  ${NSD_GetText} $hIp $0
  ${NSD_GetText} $hPort $1
  ${If} $0 == ""
    StrCpy $0 "127.0.0.1"
  ${EndIf}
  ${If} $1 == ""
    StrCpy $1 "9595"
  ${EndIf}
  StrCpy $ApiUrlDisplay "http://$0:$1/api"
  ${If} $1 == "80"
    StrCpy $ApiUrlDisplay "http://$0/api"
  ${EndIf}
  ${NSD_SetText} $hUrlHint "آدرس نهایی برای برنامهٔ اندروید:  $ApiUrlDisplay"
FunctionEnd

Function PageServerLeave
  ${NSD_GetText} $hIp $IpAddr
  ${NSD_GetText} $hPort $ApiPort
  ${If} $IpAddr == ""
    MessageBox MB_ICONEXCLAMATION "آدرس سرور (IP یا دامنه) را وارد کنید."
    Abort
  ${EndIf}
  ${If} $ApiPort == ""
    StrCpy $ApiPort "9595"
  ${EndIf}
  Call RefreshUrlHint
FunctionEnd

Function PageDbCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "دیتابیس حسابداری" "کاربر و رمز دیتابیس را وارد کنید، لیست را بگیرید و دیتابیس را انتخاب کنید"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 0 100% 9u "رمز فقط برای اتصال همین نصب استفاده می‌شود؛ نه نمایش داده می‌شود و نه در گزارشی نوشته می‌شود."
  Pop $0

  ${NSD_CreateGroupBox} 0 10u 100% 110u "اتصال برنامهٔ اندروید به SQL Server (پورت پیش‌فرض ۱۴۳۳)"
  Pop $0
  ${NSD_CreateLabel} 2% 20u 34% 9u "سرور SQL Server:"
  Pop $0
  ${NSD_CreateText} 38% 19u 60% 12u "$SqlHost"
  Pop $hSqlHost
  ${NSD_CreateLabel} 2% 32u 34% 9u "پورت SQL Server:"
  Pop $0
  ${NSD_CreateText} 38% 31u 20% 12u "$SqlPort"
  Pop $hSqlPort
  ${NSD_CreateCheckBox} 2% 43u 96% 9u "اتصال با احراز هویت ویندوز (بدون نام کاربری و رمز)"
  Pop $hWinAuth
  ${If} $SqlAuth == "windows"
    ${NSD_SetState} $hWinAuth ${BST_CHECKED}
  ${EndIf}
  ${NSD_OnClick} $hWinAuth OnWinAuthClick
  ${NSD_CreateLabel} 2% 54u 34% 9u "نام کاربری (دسترسی کامل):"
  Pop $0
  ${NSD_CreateText} 38% 53u 60% 12u "$SqlUser"
  Pop $hSqlUser
  ${NSD_CreateLabel} 2% 66u 34% 9u "کلمهٔ عبور:"
  Pop $0
  ${NSD_CreatePassword} 38% 65u 60% 12u ""
  Pop $hSqlPass
  ${NSD_CreateButton} 2% 78u 32% 12u "دریافت لیست دیتابیس‌ها"
  Pop $hBtnList
  ${NSD_OnClick} $hBtnList OnListDbClick
  ${NSD_CreateListBox} 36% 78u 62% 22u ""
  Pop $hDbList
  ${If} $ErpDb != ""
    ${NSD_LB_AddString} $hDbList "$ErpDb"
    ${NSD_LB_SelectString} $hDbList "$ErpDb"
  ${EndIf}
  ${NSD_CreateLabel} 2% 89u 96% 8u "دیتابیس برنامه را از لیست انتخاب کنید (هیچ دیتابیسی اجباری نیست)."
  Pop $hListStatus

  ${NSD_CreateLabel} 2% 99u 34% 9u "آی‌پی اختصاصی/اینترنتی سرور SQL (اختیاری):"
  Pop $0
  ${NSD_CreateText} 38% 98u 60% 12u "$PublicIpField"
  Pop $hPublicIp
  ${NSD_CreateCheckBox} 2% 110u 96% 9u "اجازهٔ اتصال به ۱۴۳۳ از بیرون شبکه (آی‌پی اختصاصی / فوروارد پورت در روتر)"
  Pop $hExternal
  ${If} $ExternalOk == "1"
    ${NSD_SetState} $hExternal ${BST_CHECKED}
  ${EndIf}

  ${NSD_CreateGroupBox} 0 122u 100% 30u "نام دیتابیس خود سامانه و بررسی سلامت"
  Pop $0
  ${NSD_CreateLabel} 2% 132u 34% 9u "دیتابیس خود ویزیتور:"
  Pop $0
  ${NSD_CreateText} 38% 131u 60% 12u "$DbName"
  Pop $hDbName
  ${NSD_CreateCheckBox} 2% 144u 96% 9u "بررسی سلامت اتصال و جدول‌های لازم در پایان نصب (پیشنهادی)"
  Pop $hHealth
  ${NSD_SetState} $hHealth ${BST_CHECKED}

  ${NSD_CreateLabel} 0 160u 100% 20u "$Detected"
  Pop $hStatus

  Call ToggleSqlFields
  ${If} $PyExe == ""
    EnableWindow $hBtnList 0
    ${NSD_SetText} $hListStatus "پایتون پیدا نشد؛ نام دیتابیس را دستی در فایل تنظیمات بنویسید."
  ${EndIf}
  nsDialogs::Show
FunctionEnd

Function PageDbLeave
  Call ToggleSqlFields
  ${NSD_GetText} $hSqlHost $SqlHost
  ${NSD_GetText} $hSqlPort $SqlPort
  ${NSD_GetText} $hSqlUser $SqlUser
  ${NSD_GetText} $hSqlPass $SqlPass
  ${NSD_GetText} $hDbName $DbName
  ${NSD_LB_GetSelection} $hDbList $ErpDb
  ${NSD_GetState} $hHealth $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $HealthWanted "1"
  ${Else}
    StrCpy $HealthWanted "0"
  ${EndIf}
  ${If} $SqlHost == ""
    StrCpy $SqlHost "localhost"
  ${EndIf}
  ${If} $SqlPort == ""
    StrCpy $SqlPort "1433"
  ${EndIf}
  ${If} $DbName == ""
    StrCpy $DbName "vizitor"
  ${EndIf}
  ${If} $ErpDb == ""
    MessageBox MB_ICONEXCLAMATION "دیتابیس برنامه را از لیست انتخاب کنید (یا نامش را دستی بنویسید). هیچ دیتابیسی اجباری نیست."
    Abort
  ${EndIf}
  ${NSD_GetText} $hPublicIp $PublicIpField
  ${NSD_GetState} $hExternal $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $ExternalOk "1"
  ${Else}
    StrCpy $ExternalOk "0"
  ${EndIf}
  ${If} $SqlAuth == "sql"
    ${If} $SqlUser == ""
      MessageBox MB_ICONEXCLAMATION "نام کاربری دیتابیس را وارد کنید یا گزینهٔ احراز هویت ویندوز را تیک بزنید."
      Abort
    ${EndIf}
    ${If} $SqlPass == ""
      MessageBox MB_ICONEXCLAMATION "کلمهٔ عبور دیتابیس را وارد کنید. (فقط برای همین نصب استفاده می‌شود.)"
      Abort
    ${EndIf}
  ${EndIf}
FunctionEnd

Function PageActivationCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}
  !insertmacro MUI_HEADER_TEXT "کد فعال‌سازی" "برای فعال بودن سامانه، کد فعال‌سازی را وارد کنید"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 0 100% 24u "کد فعال‌سازی را از فروشندهٔ سامانه دریافت کرده‌اید. اگر الان در دسترس نیست، تیک «بعداً» را بزنید: سامانه نصب می‌شود اما تا ورود کد غیرفعال می‌ماند و بعداً از برنامهٔ اندروید یا با یک فرمان ساده در سرور فعال می‌شود."
  Pop $0

  ${NSD_CreateGroupBox} 0 28u 100% 38u "فعال‌سازی"
  Pop $0
  ${NSD_CreateLabel} 2% 40u 30% 9u "کد فعال‌سازی:"
  Pop $0
  ${NSD_CreateText} 33% 39u 65% 12u "$ActCode"
  Pop $hAct
  ${NSD_CreateCheckBox} 2% 55u 96% 10u "فعلاً کد ندارم؛ بعداً وارد می‌کنم."
  Pop $hActLater
  ${If} $ActLater == "1"
    ${NSD_SetState} $hActLater ${BST_CHECKED}
  ${EndIf}
  ${NSD_OnClick} $hActLater OnActLaterClick

  ${NSD_CreateLabel} 0 74u 100% 24u "کد فعال‌سازی در فایل تنظیمات سرور ذخیره می‌شود ولی در گزارش نصب، لاگ‌ها یا خطاها چاپ نمی‌شود. قالب رایج: VIZ-XXXX-XXXX-XXXX"
  Pop $0

  Call ToggleActField
  nsDialogs::Show
FunctionEnd

Function OnActLaterClick
  Call ToggleActField
FunctionEnd

Function ToggleActField
  ${NSD_GetState} $hActLater $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $ActLater "1"
    EnableWindow $hAct 0
  ${Else}
    StrCpy $ActLater "0"
    EnableWindow $hAct 1
  ${EndIf}
FunctionEnd

Function PageActivationLeave
  ${NSD_GetState} $hActLater $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $ActLater "1"
    StrCpy $ActCode ""
    Return
  ${EndIf}
  StrCpy $ActLater "0"
  ${NSD_GetText} $hAct $ActCode
  ${If} $ActCode != ""
    Return
  ${EndIf}
  MessageBox MB_ICONQUESTION|MB_YESNO "کد فعال‌سازی خالی است؛ سامانه نصب می‌شود ولی تا ورود کد غیرفعال می‌ماند. ادامه؟" IDYES act_ok
  Abort
  act_ok:
FunctionEnd

; ---------------------------------------------------------------------------
;  شروع
; ---------------------------------------------------------------------------
Function .onInit
  SetRegView 64
  SetShellVarContext all

  ${IfNot} ${AtLeastWin7}
    MessageBox MB_ICONEXCLAMATION "این نصب‌کننده برای ویندوز ۷ و بالاتر است. روی نسخه‌های قدیمی‌تر ممکن است درست کار نکند."
  ${EndIf}

  StrCpy $IpAddr ""
  StrCpy $ApiPort "9595"
  StrCpy $SqlHost "localhost"
  StrCpy $SqlPort "1433"
  StrCpy $SqlAuth "sql"
  StrCpy $SqlUser "sa"
  StrCpy $SqlPass ""
  StrCpy $DbName "vizitor"
  StrCpy $ErpDb ""
  StrCpy $AdminUser "admin"
  StrCpy $ActCode ""
  StrCpy $ActLater "0"
  StrCpy $RecheckOnly "0"
  StrCpy $ApiUrlDisplay ""
  StrCpy $InstallRc ""
  StrCpy $Detected "در حال تشخیص وضعیت سیستم ..."
  StrCpy $PrevExists "0"
  StrCpy $FreshClean "0"
  StrCpy $PyExe ""
  StrCpy $DbCount "0"
  StrCpy $HealthWanted "1"
  StrCpy $PublicIpField ""
  StrCpy $ExternalOk "0"

  InitPluginsDir
  File /oname=$PLUGINSDIR\preflight.ps1 "nsi\preflight.ps1"
  File /oname=$PLUGINSDIR\sql_admin_tools.py "..\api\sql_admin_tools.py"
  nsExec::ExecToStack '"$SYSDIR\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "$PLUGINSDIR\preflight.ps1" -Out "$PLUGINSDIR\pre.ini"'
  Pop $0
  Pop $1

  IfFileExists "$PLUGINSDIR\pre.ini" 0 preflight_done
    ReadINIStr $PyExe "$PLUGINSDIR\pre.ini" "pre" "pythonexe"
    ReadINIStr $0 "$PLUGINSDIR\pre.ini" "pre" "ip"
    ${If} $0 != ""
      StrCpy $IpAddr $0
    ${EndIf}
    ReadINIStr $PrevExists "$PLUGINSDIR\pre.ini" "pre" "existing"
    ReadINIStr $0 "$PLUGINSDIR\pre.ini" "pre" "python"
    ReadINIStr $1 "$PLUGINSDIR\pre.ini" "pre" "ps"
    ReadINIStr $2 "$PLUGINSDIR\pre.ini" "pre" "sql"
    ReadINIStr $3 "$PLUGINSDIR\pre.ini" "pre" "odbc"
    ReadINIStr $4 "$PLUGINSDIR\pre.ini" "pre" "osver"

    StrCpy $Detected "وضعیت سیستم:  ویندوز $4  •  PowerShell $1"
    ${If} $0 != ""
      StrCpy $Detected "$Detected  •  Python $0"
    ${Else}
      StrCpy $Detected "$Detected  •  Python: یافت نشد (بخش پیش‌نیازها نصبش می‌کند)"
    ${EndIf}
    ${If} $2 == "1"
      StrCpy $Detected "$Detected  •  SQL Server: در حال اجرا"
    ${ElseIf} $2 == "2"
      StrCpy $Detected "$Detected  •  SQL Server: نصب ولی خاموش"
    ${Else}
      StrCpy $Detected "$Detected  •  SQL Server: پیدا نشد"
    ${EndIf}
    ${If} $3 == ""
      StrCpy $Detected "$Detected  •  درایور ODBC: باید نصب شود"
    ${EndIf}
    StrCpy $Detected "$Detected$\r$\nاتصال اندروید: مستقیم به SQL Server روی پورت 1433 — اگر آی‌پی اختصاصی داشته باشید، از بیرون شبکه هم وصل می‌شود"
  preflight_done:

  IfSilent skip_prev_check

  ${If} $PrevExists == "1"
    MessageBox MB_YESNOCANCEL|MB_ICONQUESTION "یک نصب قبلی ویزیتور روی این کامپیوتر پیدا شد.$\r$\n$\r$\nبله = فقط بازرسی و تعمیر (سریع)$\r$\nخیر = پیکربندی دوباره — فایل‌های برنامهٔ قبلی با نسخهٔ نو جایگزین می‌شوند ($\r$\nتنظیمات و داده‌های C:\ProgramData\Vizitor دست‌نخورده می‌مانند)$\r$\nانصراف = خروج" IDYES do_recheck IDNO do_fresh
    Quit
  ${EndIf}
  Goto after_choice

  do_fresh:
    StrCpy $FreshClean "1"
    Goto after_choice

  do_recheck:
    StrCpy $RecheckOnly "1"

  after_choice:
  skip_prev_check:
FunctionEnd

; ---------------------------------------------------------------------------
;  حذف نصب
; ---------------------------------------------------------------------------
Function un.onInit
  SetRegView 64
  SetShellVarContext all
FunctionEnd

Section "Uninstall"
  SetRegView 64
  SetShellVarContext all

  DetailPrint "توقف و حذف سرویس API ..."
  nsExec::ExecToLog 'schtasks /End /TN "${TASK_NAME}"'
  Pop $0
  nsExec::ExecToLog 'schtasks /Delete /TN "${TASK_NAME}" /F'
  Pop $0

  DetailPrint "حذف قاعده‌های فایروال ویزیتور (اگر وجود داشته باشند) ..."
  nsExec::ExecToLog 'netsh advfirewall firewall delete rule name="Vizitor API"'
  Pop $0
  nsExec::ExecToLog 'netsh advfirewall firewall delete rule name="Vizitor SQL 1433"'
  Pop $0

  ; پاک‌سازی باقی‌ماندهٔ نسخه‌های قبلی که سایت IIS می‌ساختند (فقط اگر دقیقاً همین نام باشد)
  IfFileExists "$SYSDIR\inetsrv\appcmd.exe" 0 no_iis
    nsExec::ExecToLog '"$SYSDIR\inetsrv\appcmd.exe" list site "VizitorAPI" /xml'
    Pop $0
    ${If} $0 == 0
      nsExec::ExecToLog '"$SYSDIR\inetsrv\appcmd.exe" delete site "VizitorAPI"'
      Pop $0
    ${EndIf}
  no_iis:

  DetailPrint "حذف میان‌برها ..."
  Delete "$DESKTOP\${PRODUCT_FA} — پنل مدیریت.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\پنل مدیریت.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\راهنمای فارسی.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\بازرسی و تعمیر.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\شروع سرویس API.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\توقف سرویس API.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\اتصال مستقیم SQL.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\کارت اتصال اندروید.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\حذف نصب.lnk"
  RMDir "$SMPROGRAMS\${PRODUCT_FA}"

  MessageBox MB_YESNO|MB_ICONQUESTION "تنظیمات، لاگ‌ها و فایل اتصال (پوشهٔ C:\ProgramData\Vizitor) هم پاک شوند؟$\r$\n$\r$\n«خیر» = باقی می‌مانند تا در نصب بعدی استفاده شوند.$\r$\nداده‌های دیتابیس در هر حالت دست‌نخورده می‌مانند." IDNO keep_data
    RMDir /r "C:\ProgramData\Vizitor"
  keep_data:

  DetailPrint "حذف فایل‌های برنامه ..."
  RMDir /r "$INSTDIR\setup"
  RMDir /r "$INSTDIR\tools"
  RMDir /r "$INSTDIR\docs"
  RMDir /r "$INSTDIR\api"
  Delete "$INSTDIR\run.bat"
  Delete "$INSTDIR\connect.txt"
  Delete "$INSTDIR\vizitor.ico"
  Delete "$INSTDIR\uninstall.exe"
  RMDir "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor"
  DeleteRegKey HKLM "Software\Vizitor"
SectionEnd
