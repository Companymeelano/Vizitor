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
Var /GLOBAL IpAddr
Var /GLOBAL ApiPort
Var /GLOBAL IisProxy
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
Var /GLOBAL hIis
Var /GLOBAL hSqlHost
Var /GLOBAL hSqlPort
Var /GLOBAL hWinAuth
Var /GLOBAL hSqlUser
Var /GLOBAL hSqlPass
Var /GLOBAL hDbName
Var /GLOBAL hErpDb
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
Section "هستهٔ سامانه — API و سرویس (اجباری)" SecCore
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
    RMDir /r "$INSTDIR\iis"
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

Section "پروکسی IIS روی پورت ۸۰" SecIis
SectionEnd

Section "قاعدهٔ فایروال ویندوز برای پورت API" SecFirewall
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
  SectionGetFlags ${SecIis} $0
  IntOp $0 $0 & ${SF_SELECTED}
  ${If} $0 == 0
    StrCpy $1 "$1 -SkipIis"
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
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCore}       "موتور API، سرویس ویندوز (${TASK_NAME})، فایل‌های پیکربندی و ابزارهای مدیریتی. این بخش اجباری است."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPrereq}     "نصب خودکار pyodbc و بررسی درایور ODBC مایکروسافت؛ فقط چیزی که روی این کامپیوتر کم باشد نصب می‌شود."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDb}         "ساخت دیتابیس و جداول داخلی سامانه در SQL Server. کاملاً غیرتلفیقی: هیچ جدول، ستون یا دادهٔ موجودی حذف/تغییر نمی‌شود."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecIis}        "اجرای API روی پورت ۸۰ با پروکسی معکوس IIS، بدون دست‌زدن به سایت‌های موجود. اگر IIS نبود، همان پورت مستقیم استفاده می‌شود."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecFirewall}   "ساخت قاعدهٔ فایروال ویندوز برای پورت API تا برنامهٔ اندروید بتواند وصل شود."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecAndroidSql} "اتصال مستقیم اندروید به SQL Server: ساخت کاربر محدود vizitor_android، دادن گرنت فقط برای اشیای تأییدشدهٔ پروژه و باز کردن پورت ۱۴۳۳ تنها برای شبکهٔ محلی."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDocs}       "کپی راهنماهای فارسی، گزارش نهایی مهاجرت، اسکریپت‌های بازرسی SQL و ابزارهای بررسی."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecShortcuts}  "ساخت میان‌بر دسکتاپ و منوی استارت (پنل مدیریت، بازرسی و تعمیر، شروع/توقف سرویس، حذف نصب)."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecSelfCheck}  "پس از نصب، API و دیتابیس و فعال‌سازی و لاگین بازرسی می‌شوند و در صورت خطا خودکار تعمیر می‌شود (تا ۴ دور)."
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
    StrCpy $ApiUrlDisplay "http://127.0.0.1:8080/api"
  ${EndIf}
  ExecShell "open" "$ApiUrlDisplay"
FunctionEnd

Function WriteAnswers
  Delete "$PLUGINSDIR\answers.ini"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "addr"         "$IpAddr"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "port"         "$ApiPort"
  WriteINIStr "$PLUGINSDIR\answers.ini" "server"     "iis"          "$IisProxy"
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
  WriteINIStr "$PLUGINSDIR\answers.ini" "ui"         "source"       "vizitor-setup"
FunctionEnd

; ---------------------------------------------------------------------------
;  صفحهٔ تنظیمات سرور و دیتابیس
; ---------------------------------------------------------------------------
Function PageServerCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "تنظیمات سرور و دیتابیس" "آدرس API، مشخصات SQL Server و دیتابیس ERP"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 0 100% 9u "این مقادیر را بعداً هم می‌توانید تغییر دهید. رمز SQL Server هرگز نمایش داده نمی‌شود و در گزارش‌ها نوشته نمی‌گردد."
  Pop $0

  ${NSD_CreateGroupBox} 0 10u 100% 58u "دسترسی برنامهٔ اندروید به API"
  Pop $0
  ${NSD_CreateLabel} 2% 20u 38% 9u "آدرس سرور (IP یا دامنه):"
  Pop $0
  ${NSD_CreateText} 41% 19u 57% 12u "$IpAddr"
  Pop $hIp
  ${NSD_CreateLabel} 2% 33u 38% 9u "پورت API:"
  Pop $0
  ${NSD_CreateText} 41% 32u 20% 12u "$ApiPort"
  Pop $hPort
  ${NSD_CreateCheckBox} 2% 46u 96% 10u "اجرای API از طریق IIS روی پورت ۸۰ (پیشنهادی اگر IIS نصب است)"
  Pop $hIis
  ${If} $IisProxy == "1"
    ${NSD_SetState} $hIis ${BST_CHECKED}
  ${EndIf}
  ${NSD_CreateLabel} 2% 57u 96% 9u "آدرس نهایی برای برنامهٔ اندروید:  $ApiUrlDisplay"
  Pop $hUrlHint

  ${NSD_CreateGroupBox} 0 70u 100% 76u "دیتابیس سامانهٔ ویزیتور (SQL Server)"
  Pop $0
  ${NSD_CreateLabel} 2% 80u 38% 9u "سرور SQL Server:"
  Pop $0
  ${NSD_CreateText} 41% 79u 57% 12u "$SqlHost"
  Pop $hSqlHost
  ${NSD_CreateLabel} 2% 93u 38% 9u "پورت SQL Server:"
  Pop $0
  ${NSD_CreateText} 41% 92u 20% 12u "$SqlPort"
  Pop $hSqlPort
  ${NSD_CreateCheckBox} 2% 105u 96% 10u "اتصال با احراز هویت ویندوز (بدون نام کاربری و رمز)"
  Pop $hWinAuth
  ${If} $SqlAuth == "windows"
    ${NSD_SetState} $hWinAuth ${BST_CHECKED}
  ${EndIf}
  ${NSD_OnClick} $hWinAuth OnWinAuthClick
  ${NSD_CreateLabel} 2% 117u 38% 9u "نام کاربری SQL:"
  Pop $0
  ${NSD_CreateText} 41% 116u 57% 12u "$SqlUser"
  Pop $hSqlUser
  ${NSD_CreateLabel} 2% 129u 38% 9u "رمز SQL Server:"
  Pop $0
  ${NSD_CreatePassword} 41% 128u 57% 12u ""
  Pop $hSqlPass
  ${NSD_CreateLabel} 2% 141u 38% 9u "نام دیتابیس سامانه:"
  Pop $0
  ${NSD_CreateText} 41% 140u 57% 12u "$DbName"
  Pop $hDbName

  ${NSD_CreateGroupBox} 0 148u 100% 28u "اتصال مستقیم اندروید به SQL Server"
  Pop $0
  ${NSD_CreateLabel} 2% 158u 38% 9u "نام دیتابیس ERP:"
  Pop $0
  ${NSD_CreateText} 41% 157u 57% 12u "$ErpDb"
  Pop $hErpDb
  ${NSD_CreateLabel} 2% 168u 96% 8u "کاربر محدود vizitor_android و رمزش خودکار ساخته و در فایلی با دسترسی محدود ذخیره می‌شود (چاپ نمی‌شود)."
  Pop $0

  ${NSD_CreateLabel} 0 178u 100% 11u "$Detected"
  Pop $hStatus

  Call ToggleSqlFields
  Call RefreshUrlHint
  nsDialogs::Show
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
    StrCpy $1 "8080"
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
  ${NSD_GetState} $hIis $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $IisProxy "1"
  ${Else}
    StrCpy $IisProxy "0"
  ${EndIf}
  Call ToggleSqlFields
  ${NSD_GetText} $hSqlHost $SqlHost
  ${NSD_GetText} $hSqlPort $SqlPort
  ${NSD_GetText} $hSqlUser $SqlUser
  ${NSD_GetText} $hSqlPass $SqlPass
  ${NSD_GetText} $hDbName $DbName
  ${NSD_GetText} $hErpDb $ErpDb

  ${If} $IpAddr == ""
    MessageBox MB_ICONEXCLAMATION "آدرس سرور (IP یا دامنه) را وارد کنید."
    Abort
  ${EndIf}
  ${If} $ApiPort == ""
    StrCpy $ApiPort "8080"
  ${EndIf}
  ${If} $DbName == ""
    StrCpy $DbName "vizitor"
  ${EndIf}
  ${If} $ErpDb == ""
    StrCpy $ErpDb "Meelano"
  ${EndIf}
  ${If} $SqlAuth == "sql"
    ${If} $SqlUser == ""
      MessageBox MB_ICONEXCLAMATION "نام کاربری SQL Server را وارد کنید یا گزینهٔ احراز هویت ویندوز را تیک بزنید."
      Abort
    ${EndIf}
    ${If} $SqlPass == ""
      MessageBox MB_ICONEXCLAMATION "رمز SQL Server را وارد کنید. (رمز فقط برای نصب استفاده می‌شود و هرگز چاپ نمی‌شود.)"
      Abort
    ${EndIf}
  ${EndIf}
  Call RefreshUrlHint
FunctionEnd

; ---------------------------------------------------------------------------
;  صفحهٔ فعال‌سازی
; ---------------------------------------------------------------------------
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
  StrCpy $ApiPort "8080"
  StrCpy $IisProxy "0"
  StrCpy $SqlHost "localhost"
  StrCpy $SqlPort "1433"
  StrCpy $SqlAuth "sql"
  StrCpy $SqlUser "sa"
  StrCpy $SqlPass ""
  StrCpy $DbName "vizitor"
  StrCpy $ErpDb "Meelano"
  StrCpy $AdminUser "admin"
  StrCpy $ActCode ""
  StrCpy $ActLater "0"
  StrCpy $RecheckOnly "0"
  StrCpy $ApiUrlDisplay ""
  StrCpy $InstallRc ""
  StrCpy $Detected "در حال تشخیص وضعیت سیستم ..."
  StrCpy $PrevExists "0"
  StrCpy $FreshClean "0"

  InitPluginsDir
  File /oname=$PLUGINSDIR\preflight.ps1 "nsi\preflight.ps1"
  nsExec::ExecToStack '"$SYSDIR\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "$PLUGINSDIR\preflight.ps1" -Out "$PLUGINSDIR\pre.ini"'
  Pop $0
  Pop $1

  IfFileExists "$PLUGINSDIR\pre.ini" 0 preflight_done
    ReadINIStr $0 "$PLUGINSDIR\pre.ini" "pre" "ip"
    ${If} $0 != ""
      StrCpy $IpAddr $0
    ${EndIf}
    ReadINIStr $PrevExists "$PLUGINSDIR\pre.ini" "pre" "existing"
    ReadINIStr $0 "$PLUGINSDIR\pre.ini" "pre" "iis"
    ${If} $0 == "1"
      StrCpy $IisProxy "1"
    ${EndIf}
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

  DetailPrint "حذف سایت/اپلیکیشن IIS ساختهٔ ویزیتور (فقط اگر همین نصب‌کننده ساخته باشد) ..."
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
  RMDir /r "$INSTDIR\iis"
  Delete "$INSTDIR\run.bat"
  Delete "$INSTDIR\connect.txt"
  Delete "$INSTDIR\vizitor.ico"
  Delete "$INSTDIR\uninstall.exe"
  RMDir "$INSTDIR"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\Vizitor"
  DeleteRegKey HKLM "Software\Vizitor"
SectionEnd
