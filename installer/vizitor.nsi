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
!define PUBLISHER   "${STUDIO}"
!define TASK_NAME   "VizitorAPI"
!define APP_HOME    "C:\Vizitor"

; --- سازنده و گروه نرم‌افزاری (در همهٔ صفحه‌ها، مشخصات فایل و رجیستری دیده می‌شود) ---
!define AUTHOR_FA   "میلاد یقوبی"
!define AUTHOR_EN   "Milad Yaghoobi"
!define STUDIO      "Meelano Studio Design"

; --- رنگ‌های رابط (قالب 0xRRGGBB) ---
!define COL_BAND      0x123A6B
!define COL_BAND_SUB  0xBCD3EE
!define COL_CARD      0xF3F7FC
!define COL_CARD2     0xE8EFF8
!define COL_WHITE     0xFFFFFF
!define COL_TEXT      0x22303F
!define COL_MUTED     0x5B6B7C
!define COL_BRAND     0x1B5FA8
!define COL_OK        0x1E7B45
!define COL_OKBG      0xE6F5EC
!define COL_WARN      0x8A6100
!define COL_WARNBG    0xFDF3DF
!define COL_ERR       0xA93125
!define COL_ERRBG     0xFCE9E7

RequestExecutionLevel admin

Name "${PRODUCT_FA} ${VERSION}"
OutFile "..\release\Vizitor-Setup-${VERSION}.exe"
InstallDir "${APP_HOME}"
InstallDirRegKey HKLM "Software\Vizitor" "InstallDir"
BrandingText "${PRODUCT_FA} ${VERSION}  •  ${STUDIO}"
Icon "assets\vizitor.ico"
UninstallIcon "assets\vizitor.ico"

VIProductVersion "1.0.0.0"
VIAddVersionKey /LANG=1033 "ProductName"     "Vizitor Server"
VIAddVersionKey /LANG=1033 "FileDescription" "Vizitor Server Setup"
VIAddVersionKey /LANG=1033 "FileVersion"     "${VERSION}"
VIAddVersionKey /LANG=1033 "ProductVersion"  "${VERSION}"
VIAddVersionKey /LANG=1033 "CompanyName"     "${STUDIO}"
VIAddVersionKey /LANG=1033 "LegalCopyright"  "© ${AUTHOR_FA} (${AUTHOR_EN}) — ${STUDIO}"
VIAddVersionKey /LANG=1033 "Comments"        "طراحی و برنامه‌نویسی: ${AUTHOR_FA} (${AUTHOR_EN}) | گروه نرم‌افزاری: ${STUDIO}"
VIAddVersionKey /LANG=1035 "FileVersion"     "${VERSION}"
VIAddVersionKey /LANG=1035 "ProductVersion"  "${VERSION}"
VIAddVersionKey /LANG=1035 "ProductName"     "سامانهٔ ویزیتور"
VIAddVersionKey /LANG=1035 "FileDescription" "نصب‌کنندهٔ سامانهٔ ویزیتور — اتصال مستقیم اندروید به SQL Server"
VIAddVersionKey /LANG=1035 "CompanyName"     "${STUDIO}"
VIAddVersionKey /LANG=1035 "LegalCopyright"  "© ${AUTHOR_FA} (${AUTHOR_EN}) — ${STUDIO}"
VIAddVersionKey /LANG=1035 "Comments"        "طراحی و برنامه‌نویسی: ${AUTHOR_FA} | گروه نرم‌افزاری: ${STUDIO}"

!define MUI_CUSTOMFUNCTION_GUIINIT StyleWizardButtons

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
Var /GLOBAL hAuthWin
Var /GLOBAL hAuthSql
Var /GLOBAL hSqlStatus
Var /GLOBAL hSqlTest
Var /GLOBAL hErpDbEdit
Var /GLOBAL hDbConfirm
Var /GLOBAL hListBtn
Var /GLOBAL hConfirmBtn
Var /GLOBAL DbListOk
Var /GLOBAL DbListCount
Var /GLOBAL FontTitle
Var /GLOBAL FontHead
Var /GLOBAL FontBody
Var /GLOBAL FontSmall
Var /GLOBAL FontBtn
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
!define MUI_WELCOMEPAGE_TEXT  "این برنامه، سرور ویزیتور را روی این کامپیوتر نصب و آمادهٔ استفاده می‌کند:$\r$\n$\r$\n• سرویس API و اتصال برنامهٔ اندروید$\r$\n• دیتابیس سامانه در SQL Server (فقط ساخت چیزهای ناموجود؛ هیچ دادهٔ موجودی تغییر نمی‌کند)$\r$\n• پنل مدیریت، قاعده‌های فایروال و راهنماهای فارسی$\r$\n$\r$\nدر صفحهٔ بعد، بخش‌های موردنیاز را تیک بزنید.$\r$\n$\r$\nطراحی و برنامه‌نویسی: ${AUTHOR_FA} (${AUTHOR_EN})  •  گروه: ${STUDIO}"

!define MUI_LICENSEPAGE_TEXT_TOP    "لطفاً شرایط استفاده را بخوانید."
!define MUI_LICENSEPAGE_TEXT_BOTTOM "برای ادامه، شرایط بالا را بپذیرید."
!define MUI_COMPONENTSPAGE_TEXT_TOP "هر بخشی که لازم دارید تیک بزنید؛ توضیح هر بخش پایین همین صفحه نمایش داده می‌شود."
!define MUI_DIRECTORYPAGE_TEXT_TOP  "پوشهٔ نصب سامانه. سرویس سامانه، ابزارها و راهنماها این‌جا قرار می‌گیرند."

!define MUI_FINISHPAGE_TITLE          "نصب ${PRODUCT_FA} کامل شد"
!define MUI_FINISHPAGE_TEXT           "آدرس API برای برنامهٔ اندروید:$\r$\n$ApiUrlDisplay$\r$\n$\r$\nاتصال مستقیم اندروید به SQL Server (پورت ۱۴۳۳) آماده شد؛ جزئیات و کد QR:$\r$\n$INSTDIR\setup\android-connect.png$\r$\n$\r$\nتنظیمات و لاگ‌ها:  C:\ProgramData\Vizitor$\r$\n$\r$\nاگر بازرسی خودکار به مشکل خورد، از میان‌بر «بازرسی و تعمیر» استفاده کنید.$\r$\n$\r$\nسامانهٔ ویزیتور — طراحی و برنامه‌نویسی: ${AUTHOR_FA} ($\r$\n${AUTHOR_EN})  •  گروه نرم‌افزاری: ${STUDIO}"
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
Page custom PageSqlCreate PageSqlLeave
Page custom PageDbCreate PageDbLeave
Page custom PageActivationCreate PageActivationLeave
!insertmacro MUI_PAGE_INSTFILES
!define MUI_PAGE_CUSTOMFUNCTION_PRE PagePreFinish
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "Farsi"

; ---------------------------------------------------------------------------
;  متن دکمه‌های ویزارد (فارسی، بدون حرف میانبر و با امضای سازنده در نوار عنوان)
; ---------------------------------------------------------------------------
LangString ^SetupCaption ${LANG_FARSI} "نصب سامانهٔ ویزیتور  •  ${STUDIO}"
LangString ^NextBtn    ${LANG_FARSI} "بعدی"
LangString ^BackBtn    ${LANG_FARSI} "قبلی"
LangString ^InstallBtn ${LANG_FARSI} "نصب سامانه"
LangString ^CancelBtn  ${LANG_FARSI} "انصراف"
LangString ^CloseBtn   ${LANG_FARSI} "بستن"

; ---------------------------------------------------------------------------
;  میان‌برها (ماکرو باید قبل از استفاده تعریف شود)
; ---------------------------------------------------------------------------
!macro CreateShortcuts
  SetShellVarContext all
  CreateDirectory "$SMPROGRAMS\${PRODUCT_FA}"
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\پنل مدیریت.lnk"      "$INSTDIR\tools\open-panel.bat" "" "$INSTDIR\tools\open-panel.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\راهنمای فارسی.lnk"    "$INSTDIR\tools\open-guide.bat" "" "$INSTDIR\tools\open-guide.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\بازرسی و تعمیر.lnk"   "$INSTDIR\tools\recheck.bat" "" "$INSTDIR\tools\recheck.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\شروع سرویس سامانه.lnk" "$INSTDIR\tools\service-start.bat" "" "$INSTDIR\tools\service-start.bat" 0
  CreateShortCut "$SMPROGRAMS\${PRODUCT_FA}\توقف سرویس سامانه.lnk" "$INSTDIR\tools\service-stop.bat" "" "$INSTDIR\tools\service-stop.bat" 0
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
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCore}       "سرویس سامانه (${TASK_NAME}) و پنل مدیریت روی پورت ۹۵۹۵، فایل‌های پیکربندی و ابزارها — بدون IIS و بدون API میانی. (اتصال برنامهٔ اندروید مستقل از این بخش و مستقیم به SQL Server است.)"
  !insertmacro MUI_DESCRIPTION_TEXT ${SecPrereq}     "pyodbc و درایور ODBC مایکروسافت — همان چیزهایی که برای گفت‌وگو با SQL Server لازم است؛ فقط در صورت کمبود نصب می‌شوند."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDb}         "ساخت دیتابیس و جداول داخلی خود سامانه (پنل و فعال‌سازی) — کاملاً غیرتلفیقی؛ دیتابیس حسابداری شما دست‌نخورده می‌ماند."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecFirewall}   "باز کردن پورت ۱۴۳۳ برای اتصال مستقیم برنامهٔ اندروید به دیتابیس (شبکهٔ محلی، و در صورت تیک «اتصال از بیرون شبکه» برای هر آدرس) و پورت ۹۵۹۵ فقط برای شبکهٔ محلی."
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
;  قالب ظاهری صفحه‌ها: نوار عنوان، کارت‌های رنگی، دکمه‌های تخت، امضای سازنده
; ---------------------------------------------------------------------------
!macro CardPanel x y w h col
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}" 0 ${x} ${y} ${w} ${h} ""
  Pop $0
  SetCtlColors $0 "" ${col}
!macroend

!macro SolidLabel x y w h txt colFg colBg align
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${align}|${SS_NOTIFY}" 0 ${x} ${y} ${w} ${h} "${txt}"
  Pop $0
  SetCtlColors $0 ${colFg} ${colBg}
!macroend

!macro FlatButton x y w h txt hVar colFg colBg
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_NOTIFY}|${SS_CENTER}|${SS_CENTERIMAGE}|${WS_TABSTOP}" 0 ${x} ${y} ${w} ${h} "${txt}"
  Pop ${hVar}
  SetCtlColors ${hVar} ${colFg} ${colBg}
  SendMessage ${hVar} ${WM_SETFONT} $FontBtn 1
!macroend

!macro PageBand title sub
  !insertmacro CardPanel 0 0 100% 26u ${COL_BAND}
  !insertmacro SolidLabel 2% 4u 96% 11u "${title}" ${COL_WHITE} ${COL_BAND} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontTitle 1
  !insertmacro SolidLabel 2% 16u 96% 8u "${sub}" ${COL_BAND_SUB} ${COL_BAND} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
!macroend

!macro PageCredit y
  !insertmacro SolidLabel 0 ${y}u 100% 10u "طراحی و برنامه‌نویسی: ${AUTHOR_FA} (${AUTHOR_EN})   •   گروه نرم‌افزاری: ${STUDIO}" ${COL_MUTED} ${COL_WHITE} ${SS_CENTER}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
!macroend

!macro PageSep y
  ${NSD_CreateHLine} 0 ${y}u 100% 1u ""
  Pop $0
!macroend

; ---------------------------------------------------------------------------
;  صفحهٔ ۱ — سرور سامانه (پنل مدیریت)
; ---------------------------------------------------------------------------
Function PageServerCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "سرور سامانه" "آدرس و پورت پنل مدیریت — اتصال اندروید مستقل و مستقیم به دیتابیس است"

  nsDialogs::Create 1044
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_WHITE}

  !insertmacro PageBand "سرور سامانهٔ ویزیتور" "آدرس و پورت پنل مدیریت — اتصال برنامهٔ اندروید مستقل و مستقیم به SQL Server است (بدون IIS و بدون API)"

  !insertmacro CardPanel 0 32u 100% 54u ${COL_CARD}
  !insertmacro SolidLabel 2% 36u 46% 10u "آدرس سرور (IP یا دامنه در شبکهٔ داخلی):" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateText} 2% 47u 58% 13u "$IpAddr"
  Pop $hIp
  SendMessage $hIp ${WM_SETFONT} $FontBody 1
  !insertmacro SolidLabel 64% 36u 34% 10u "پورت پنل مدیریت:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateText} 64% 47u 24% 13u "$ApiPort"
  Pop $hPort
  SendMessage $hPort ${WM_SETFONT} $FontBody 1
  !insertmacro SolidLabel 2% 64u 96% 9u "• این پورت فقط برای شبکهٔ محلی باز می‌شود و به اینترنت باز نیست." ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  !insertmacro SolidLabel 2% 73u 96% 10u "• آدرس نهایی پنل و سرویس:  $ApiUrlDisplay" ${COL_BRAND} ${COL_CARD} ${SS_LEFT}
  StrCpy $hUrlHint $0
  SendMessage $hUrlHint ${WM_SETFONT} $FontHead 1

  !insertmacro CardPanel 0 90u 100% 52u ${COL_CARD}
  !insertmacro SolidLabel 2% 94u 96% 10u "وضعیت شناسایی‌شدهٔ همین سیستم:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro SolidLabel 2% 105u 96% 34u "$Detected" ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  StrCpy $hStatus $0
  SendMessage $hStatus ${WM_SETFONT} $FontSmall 1

  !insertmacro PageSep 150u
  !insertmacro CardPanel 0 154u 100% 24u ${COL_CARD2}
  !insertmacro SolidLabel 2% 158u 96% 18u "در قدم‌های بعد: مشخصات SQL Server، انتخاب دیتابیس برنامه از فهرست همین سرور (هیچ دیتابیسی اجباری نیست) و کد فعال‌سازی." ${COL_TEXT} ${COL_CARD2} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro PageCredit 182

  Call RefreshUrlHint
  Call muiPageLoadFullWindow
  nsDialogs::Show
  Call muiPageUnloadFullWindow
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
  ${NSD_SetText} $hUrlHint "• آدرس نهایی پنل و سرویس:  $ApiUrlDisplay"
FunctionEnd

; ---------------------------------------------------------------------------
;  صفحهٔ ۲ — اتصال به SQL Server (کاربر/رمز، اتصال از بیرون شبکه، تست اتصال)
; ---------------------------------------------------------------------------
Function PageSqlCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "اتصال به SQL Server" "پورت پیش‌فرض ۱۴۳۳ — رمز فقط برای همین نصب استفاده می‌شود"

  nsDialogs::Create 1044
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_WHITE}

  !insertmacro PageBand "اتصال به SQL Server" "پورت پیش‌فرض ۱۴۳۳ — رمز فقط برای همین نصب استفاده می‌شود؛ نه نمایش داده می‌شود و نه در گزارشی نوشته می‌شود"

  !insertmacro CardPanel 0 32u 100% 66u ${COL_CARD}
  !insertmacro SolidLabel 2% 36u 20% 10u "سرور SQL Server:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateText} 23% 35u 41% 13u "$SqlHost"
  Pop $hSqlHost
  SendMessage $hSqlHost ${WM_SETFONT} $FontBody 1
  !insertmacro SolidLabel 66% 36u 10% 10u "پورت:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateText} 77% 35u 21% 13u "$SqlPort"
  Pop $hSqlPort
  SendMessage $hSqlPort ${WM_SETFONT} $FontBody 1

  !insertmacro SolidLabel 2% 53u 46% 10u "روش ورود:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateRadioButton} 2% 63u 46% 10u "کاربر SQL Server (نام کاربری و کلمهٔ عبور)"
  Pop $hAuthSql
  SendMessage $hAuthSql ${WM_SETFONT} $FontSmall 1
  ${NSD_CreateRadioButton} 50% 63u 48% 10u "حساب ویندوز همین سرور (بدون رمز SQL)"
  Pop $hAuthWin
  SendMessage $hAuthWin ${WM_SETFONT} $FontSmall 1
  ${NSD_OnClick} $hAuthSql OnAuthClick
  ${NSD_OnClick} $hAuthWin OnAuthClick
  ${If} $SqlAuth == "windows"
    ${NSD_SetState} $hAuthWin ${BST_CHECKED}
  ${Else}
    ${NSD_SetState} $hAuthSql ${BST_CHECKED}
  ${EndIf}

  !insertmacro SolidLabel 2% 76u 20% 10u "نام کاربری:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  ${NSD_CreateText} 23% 75u 41% 13u "$SqlUser"
  Pop $hSqlUser
  SendMessage $hSqlUser ${WM_SETFONT} $FontBody 1
  !insertmacro SolidLabel 66% 76u 10% 10u "کلمهٔ عبور:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  ${NSD_CreatePassword} 77% 75u 21% 13u ""
  Pop $hSqlPass
  SendMessage $hSqlPass ${WM_SETFONT} $FontBody 1

  !insertmacro SolidLabel 2% 90u 96% 8u "• کاربر SQL Server باید به دیتابیس‌های همین سرور دسترسی داشته باشد (مثلاً sa یا کاربر مدیر)." ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro CardPanel 0 102u 100% 44u ${COL_CARD}
  !insertmacro SolidLabel 2% 106u 96% 10u "اتصال از بیرون شبکه (اختیاری):" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro SolidLabel 2% 117u 34% 8u "آی‌پی اختصاصی/اینترنتی سرور:" ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  ${NSD_CreateText} 37% 115u 61% 13u "$PublicIpField"
  Pop $hPublicIp
  SendMessage $hPublicIp ${WM_SETFONT} $FontBody 1
  ${NSD_CreateCheckBox} 2% 131u 96% 12u "اجازهٔ اتصال به پورت ۱۴۳۳ از بیرون شبکه (فوروارد پورت در روتر لازم است)"
  Pop $hExternal
  SendMessage $hExternal ${WM_SETFONT} $FontSmall 1
  ${If} $ExternalOk == "1"
    ${NSD_SetState} $hExternal ${BST_CHECKED}
  ${EndIf}

  !insertmacro FlatButton 0 150u 46% 15u "تست اتصال و شمردن دیتابیس‌ها" $hSqlTest ${COL_WHITE} ${COL_BRAND}
  ${NSD_OnClick} $hSqlTest OnTestSqlClick
  !insertmacro SolidLabel 48% 150u 52% 15u "با دکمهٔ کناری، اتصال را قبل از ادامه امتحان کنید." ${COL_MUTED} ${COL_CARD2} ${SS_CENTER}
  StrCpy $hSqlStatus $0
  SendMessage $hSqlStatus ${WM_SETFONT} $FontSmall 1

  !insertmacro SolidLabel 0 168u 100% 9u "• در SQL Server باید TCP/IP فعال و پورت ۱۴۳۳ باز باشد (SQL Server Configuration Manager)." ${COL_MUTED} ${COL_WHITE} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro PageCredit 182

  Call ToggleSqlFields
  Call muiPageLoadFullWindow
  nsDialogs::Show
  Call muiPageUnloadFullWindow
FunctionEnd

Function OnAuthClick
  Call ToggleSqlFields
FunctionEnd

Function ToggleSqlFields
  ${NSD_GetState} $hAuthWin $0
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

Function OnTestSqlClick
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
  ${NSD_GetState} $hAuthWin $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $3 ""
    StrCpy $4 ""
  ${EndIf}
  ${If} $PyExe == ""
    SetCtlColors $hSqlStatus ${COL_WARN} ${COL_WARNBG}
    ${NSD_SetText} $hSqlStatus "پایتون روی این سیستم پیدا نشد؛ تست اتصال ممکن نیست ولی نصب پیش‌نیازها آن را می‌آورد."
    Return
  ${EndIf}

  Delete "$PLUGINSDIR\dbcreds.ini"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "server" "$1"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "port" "$2"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "user" "$3"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "pass" "$4"

  SetCtlColors $hSqlStatus ${COL_MUTED} ${COL_CARD2}
  ${NSD_SetText} $hSqlStatus "در حال تست اتصال ... چند ثانیه صبر کنید"
  Delete "$PLUGINSDIR\dbs.ini"
  nsExec::ExecToStack '"$PyExe" "$PLUGINSDIR\sql_admin_tools.py" databases --creds "$PLUGINSDIR\dbcreds.ini" --out-ini "$PLUGINSDIR\dbs.ini"'
  Pop $9
  Pop $8

  StrCpy $DbListCount "0"
  StrCpy $DbListOk "0"
  IfFileExists "$PLUGINSDIR\dbs.ini" 0 test_failed
    ReadINIStr $5 "$PLUGINSDIR\dbs.ini" "result" "ok"
    ReadINIStr $DbListCount "$PLUGINSDIR\dbs.ini" "result" "count"
    ${If} $DbListCount == ""
      StrCpy $DbListCount "0"
    ${EndIf}
    ${If} $5 == "1"
      StrCpy $DbListOk "1"
      SetCtlColors $hSqlStatus ${COL_OK} ${COL_OKBG}
      ${NSD_SetText} $hSqlStatus "اتصال برقرار شد — $DbListCount دیتابیس روی این سرور پیدا شد. در صفحهٔ بعد از همین فهرست انتخاب می‌کنید."
      Return
    ${EndIf}
    ReadINIStr $6 "$PLUGINSDIR\dbs.ini" "result" "error"
    ${If} $6 == ""
      StrCpy $6 "خطای نامشخص"
    ${EndIf}
    SetCtlColors $hSqlStatus ${COL_ERR} ${COL_ERRBG}
    ${NSD_SetText} $hSqlStatus "اتصال برقرار نشد: $6 — سرور، پورت، کاربر و رمز را بررسی کنید."
    Return
  test_failed:
    SetCtlColors $hSqlStatus ${COL_ERR} ${COL_ERRBG}
    ${NSD_SetText} $hSqlStatus "اجرای تست ممکن نشد (کد $9) — سرور/پورت/کاربر/رمز را بررسی کنید."
FunctionEnd

Function PageSqlLeave
  ${NSD_GetText} $hSqlHost $SqlHost
  ${NSD_GetText} $hSqlPort $SqlPort
  ${NSD_GetText} $hSqlUser $SqlUser
  ${NSD_GetText} $hSqlPass $SqlPass
  ${NSD_GetText} $hPublicIp $PublicIpField
  ${NSD_GetState} $hExternal $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $ExternalOk "1"
  ${Else}
    StrCpy $ExternalOk "0"
  ${EndIf}
  ${If} $SqlHost == ""
    StrCpy $SqlHost "localhost"
  ${EndIf}
  ${If} $SqlPort == ""
    StrCpy $SqlPort "1433"
  ${EndIf}
  ${NSD_GetState} $hAuthWin $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $SqlAuth "windows"
    Goto auth_checked
  ${EndIf}
  StrCpy $SqlAuth "sql"
  ${If} $SqlUser == ""
    MessageBox MB_ICONEXCLAMATION "نام کاربری SQL Server را وارد کنید یا «حساب ویندوز همین سرور» را انتخاب کنید."
    Abort
  ${EndIf}
  ${If} $SqlPass == ""
    MessageBox MB_ICONEXCLAMATION "کلمهٔ عبور SQL Server را وارد کنید. (فقط برای همین نصب استفاده می‌شود.)"
    Abort
  ${EndIf}
  auth_checked:
  ${If} $ExternalOk == "1"
    ${If} $PublicIpField == ""
      MessageBox MB_ICONEXCLAMATION "برای اتصال از بیرون شبکه، آی‌پی اختصاصی/اینترنتی سرور را وارد کنید یا تیک مربوطه را بردارید."
      Abort
    ${EndIf}
  ${EndIf}
FunctionEnd

; ---------------------------------------------------------------------------
;  صفحهٔ ۳ — انتخاب دیتابیس برنامه (فهرست + تایپ دستی)
; ---------------------------------------------------------------------------
Function PageDbCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "انتخاب دیتابیس برنامه" "از فهرست سرور انتخاب کنید یا نام آن را دستی بنویسید"

  nsDialogs::Create 1044
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_WHITE}

  !insertmacro PageBand "انتخاب دیتابیس برنامه" "هیچ دیتابیسی اجباری نیست — دیتابیس حسابداری خودتان را از فهرست سرور انتخاب کنید یا نامش را بنویسید"

  !insertmacro SolidLabel 0 30u 100% 9u "سرور جاری:  $SqlHost , پورت $SqlPort    •    روش ورود: $SqlAuth" ${COL_BRAND} ${COL_WHITE} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro CardPanel 0 40u 100% 78u ${COL_CARD}
  !insertmacro SolidLabel 2% 43u 44% 11u "۱) فهرست دیتابیس‌های سرور" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro FlatButton 52% 42u 46% 14u "دریافت فهرست دیتابیس‌ها" $hListBtn ${COL_WHITE} ${COL_BRAND}
  ${NSD_OnClick} $hListBtn OnListDbClick
  ${NSD_CreateListBox} 2% 58u 96% 40u ""
  Pop $hDbList
  SendMessage $hDbList ${WM_SETFONT} $FontBody 1
  SetCtlColors $hDbList ${COL_TEXT} ${COL_WHITE}
  ${NSD_OnChange} $hDbList OnDbSelChange
  ${If} $ErpDb != ""
    ${NSD_LB_AddString} $hDbList "$ErpDb"
    ${NSD_LB_SelectString} $hDbList "$ErpDb"
  ${EndIf}
  !insertmacro SolidLabel 2% 100u 96% 8u "هنوز فهرستی گرفته نشده — دکمهٔ آبی «دریافت فهرست دیتابیس‌ها» را بزنید." ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  StrCpy $hListStatus $0
  SendMessage $hListStatus ${WM_SETFONT} $FontSmall 1
  !insertmacro SolidLabel 2% 109u 96% 8u "• با کلیک روی هر نام در فهرست، همان نام خودکار در کادر پایین می‌آید." ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro CardPanel 0 121u 100% 38u ${COL_CARD}
  !insertmacro SolidLabel 2% 124u 96% 10u "۲) نام دیتابیس برنامه (حسابداری) — انتخاب از فهرست یا تایپ دستی:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateText} 2% 135u 54% 14u "$ErpDb"
  Pop $hErpDbEdit
  SendMessage $hErpDbEdit ${WM_SETFONT} $FontBtn 1
  SetCtlColors $hErpDbEdit ${COL_TEXT} ${COL_WHITE}
  !insertmacro FlatButton 58% 134u 40% 16u "تأیید نام دیتابیس" $hConfirmBtn ${COL_WHITE} ${COL_OK}
  ${NSD_OnClick} $hConfirmBtn OnConfirmDbClick
  !insertmacro SolidLabel 2% 150u 96% 8u "نام را بنویسید یا از فهرست انتخاب کنید، بعد دکمهٔ سبز «تأیید نام دیتابیس» را بزنید." ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  StrCpy $hDbConfirm $0
  SendMessage $hDbConfirm ${WM_SETFONT} $FontSmall 1

  !insertmacro PageSep 162u
  !insertmacro SolidLabel 2% 166u 30% 8u "دیتابیس خود ویزیتور:" ${COL_MUTED} ${COL_WHITE} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  ${NSD_CreateText} 33% 165u 30% 13u "$DbName"
  Pop $hDbName
  SendMessage $hDbName ${WM_SETFONT} $FontBody 1
  ${NSD_CreateCheckBox} 65% 166u 34% 16u "بررسی سلامت اتصال و جدول‌ها در پایان نصب (پیشنهادی)"
  Pop $hHealth
  SendMessage $hHealth ${WM_SETFONT} $FontSmall 1
  ${NSD_SetState} $hHealth ${BST_CHECKED}

  !insertmacro PageCredit 183

  Call muiPageLoadFullWindow
  nsDialogs::Show
  Call muiPageUnloadFullWindow
FunctionEnd

Function OnListDbClick
  ${If} $PyExe == ""
    SetCtlColors $hListStatus ${COL_WARN} ${COL_WARNBG}
    ${NSD_SetText} $hListStatus "پایتون روی این سیستم پیدا نشد؛ نام دیتابیس را دستی در کادر پایین بنویسید."
    Return
  ${EndIf}

  Delete "$PLUGINSDIR\dbcreds.ini"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "server" "$SqlHost"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "port" "$SqlPort"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "user" "$SqlUser"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "pass" "$SqlPass"

  SetCtlColors $hListStatus ${COL_MUTED} ${COL_CARD}
  ${NSD_SetText} $hListStatus "در حال گرفتن فهرست دیتابیس‌ها ... چند ثانیه صبر کنید"
  Delete "$PLUGINSDIR\dbs.ini"
  nsExec::ExecToStack '"$PyExe" "$PLUGINSDIR\sql_admin_tools.py" databases --creds "$PLUGINSDIR\dbcreds.ini" --out-ini "$PLUGINSDIR\dbs.ini"'
  Pop $R0
  Pop $R1

  ${NSD_LB_Clear} $hDbList
  StrCpy $DbCount "0"
  StrCpy $DbListOk "0"
  IfFileExists "$PLUGINSDIR\dbs.ini" 0 list_failed
    ReadINIStr $DbCount "$PLUGINSDIR\dbs.ini" "result" "count"
    ${If} $DbCount == ""
      StrCpy $DbCount "0"
    ${EndIf}
    StrCmp $DbCount "0" list_empty
    StrCpy $DbIdx "1"
  ${If} $DbCount > 0
    StrCpy $DbIdx "1"
  loop_db:
    ${If} $DbIdx > $DbCount
      Goto list_done
    ${EndIf}
    ReadINIStr $5 "$PLUGINSDIR\dbs.ini" "db$DbIdx" "name"
    ${If} $5 != ""
      ${NSD_LB_AddString} $hDbList "$5"
    ${EndIf}
    IntOp $DbIdx $DbIdx + 1
    Goto loop_db
  ${EndIf}
  list_done:
    StrCpy $DbListOk "1"
    ReadINIStr $6 "$PLUGINSDIR\dbs.ini" "db1" "name"
    ${If} $6 != ""
      ${NSD_LB_SelectString} $hDbList "$6"
      ${NSD_SetText} $hErpDbEdit "$6"
    ${EndIf}
    SetCtlColors $hListStatus ${COL_OK} ${COL_CARD}
    ${NSD_SetText} $hListStatus "$DbCount دیتابیس پیدا شد. روی نام موردنظر کلیک کنید یا نام را در کادر پایین بنویسید."
    Goto list_end
  list_empty:
    SetCtlColors $hListStatus ${COL_WARN} ${COL_CARD}
    ${NSD_SetText} $hListStatus "هیچ دیتابیس کاربری روی این سرور پیدا نشد — سرور، کاربر و رمز را بررسی کنید یا نام را دستی بنویسید."
    Goto list_end
  list_failed:
    SetCtlColors $hListStatus ${COL_ERR} ${COL_CARD}
    ${NSD_SetText} $hListStatus "گرفتن فهرست ممکن نشد (کد $R0) — سرور/پورت/کاربر/رمز را بررسی کنید یا نام را دستی بنویسید."
  list_end:
FunctionEnd

Function OnDbSelChange
  ${NSD_LB_GetSelection} $hDbList $0
  ${If} $0 == ""
    Return
  ${EndIf}
  ${NSD_SetText} $hErpDbEdit "$0"
  SetCtlColors $hDbConfirm ${COL_BRAND} ${COL_CARD}
  ${NSD_SetText} $hDbConfirm "از فهرست انتخاب شد: $0 — برای ثبت نهایی، دکمهٔ سبز «تأیید نام دیتابیس» را بزنید."
FunctionEnd

Function OnConfirmDbClick
  ${NSD_GetText} $hErpDbEdit $0
  ${If} $0 == ""
    SetCtlColors $hDbConfirm ${COL_ERR} ${COL_CARD}
    ${NSD_SetText} $hDbConfirm "نام دیتابیس خالی است — از فهرست انتخاب کنید یا نام آن را بنویسید."
    Return
  ${EndIf}
  StrCpy $ErpDb "$0"
  ${If} $DbListOk == "1"
    ${NSD_LB_GetCount} $hDbList $1
    StrCpy $2 "0"
    StrCpy $3 "0"
    ${While} $3 < $1
      ${NSD_LB_GetItemText} $hDbList $3 $4
      ${If} $4 == "$0"
        StrCpy $2 "1"
        ${Break}
      ${EndIf}
      IntOp $3 $3 + 1
    ${EndWhile}
    ${If} $2 == "1"
      SetCtlColors $hDbConfirm ${COL_OK} ${COL_CARD}
      ${NSD_SetText} $hDbConfirm "تأیید شد: دیتابیس «$0» در فهرست همین سرور وجود دارد."
    ${Else}
      SetCtlColors $hDbConfirm ${COL_WARN} ${COL_CARD}
      ${NSD_SetText} $hDbConfirm "توجه: «$0» در فهرست گرفته‌شده نبود؛ اگر مطمئنید همین نام درست است، ادامه بدهید (بررسی سلامت نصب هم آن را امتحان می‌کند)."
    ${EndIf}
    Return
  ${EndIf}
  SetCtlColors $hDbConfirm ${COL_OK} ${COL_CARD}
  ${NSD_SetText} $hDbConfirm "تأیید شد: دیتابیس «$0» برای اتصال برنامه استفاده می‌شود."
FunctionEnd

Function PageDbLeave
  ${NSD_GetText} $hErpDbEdit $0
  ${If} $0 == ""
    MessageBox MB_ICONEXCLAMATION "دیتابیس برنامه را از فهرست انتخاب کنید یا نامش را دستی بنویسید. هیچ دیتابیسی اجباری نیست."
    Abort
  ${EndIf}
  StrCpy $ErpDb "$0"
  ${NSD_GetText} $hDbName $DbName
  ${NSD_GetState} $hHealth $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $HealthWanted "1"
  ${Else}
    StrCpy $HealthWanted "0"
  ${EndIf}
  ${If} $DbName == ""
    StrCpy $DbName "vizitor"
  ${EndIf}
FunctionEnd

; ---------------------------------------------------------------------------
;  صفحهٔ ۴ — کد فعال‌سازی و شناسنامهٔ سازنده
; ---------------------------------------------------------------------------
Function PageActivationCreate
  ${If} $RecheckOnly == 1
    Abort
  ${EndIf}

  !insertmacro MUI_HEADER_TEXT "کد فعال‌سازی" "برای فعال بودن سامانه، کد فعال‌سازی را وارد کنید"

  nsDialogs::Create 1044
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_WHITE}

  !insertmacro PageBand "کد فعال‌سازی سامانه" "کد را از فروشندهٔ سامانه گرفته‌اید؛ اگر الان در دسترس نیست، تیک «بعداً» را بزنید"

  !insertmacro CardPanel 0 32u 100% 58u ${COL_CARD}
  !insertmacro SolidLabel 2% 36u 96% 20u "سامانه بدون کد هم نصب می‌شود، اما تا ورود کد فعال نمی‌شود. کد در فایل تنظیمات سرور ذخیره می‌شود و هرگز در گزارش نصب یا لاگ‌ها چاپ نمی‌شود." ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  !insertmacro SolidLabel 2% 58u 20% 10u "کد فعال‌سازی:" ${COL_TEXT} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  ${NSD_CreateText} 23% 57u 73% 14u "$ActCode"
  Pop $hAct
  SendMessage $hAct ${WM_SETFONT} $FontBtn 1
  SetCtlColors $hAct ${COL_TEXT} ${COL_WHITE}
  ${NSD_CreateCheckBox} 2% 74u 96% 12u "فعلاً کد ندارم؛ بعداً وارد می‌کنم."
  Pop $hActLater
  SendMessage $hActLater ${WM_SETFONT} $FontSmall 1
  ${If} $ActLater == "1"
    ${NSD_SetState} $hActLater ${BST_CHECKED}
  ${EndIf}
  ${NSD_OnClick} $hActLater OnActLaterClick
  !insertmacro SolidLabel 2% 86u 96% 8u "• قالب رایج کد: VIZ-XXXX-XXXX-XXXX" ${COL_MUTED} ${COL_CARD} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro CardPanel 0 94u 100% 76u ${COL_CARD2}
  !insertmacro SolidLabel 2% 100u 96% 14u "سامانهٔ ویزیتور — نسخهٔ ${VERSION}" ${COL_BRAND} ${COL_CARD2} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontTitle 1
  !insertmacro SolidLabel 2% 121u 96% 12u "طراحی و برنامه‌نویسی:  ${AUTHOR_FA}  (${AUTHOR_EN})" ${COL_TEXT} ${COL_CARD2} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro SolidLabel 2% 136u 96% 12u "گروه نرم‌افزاری:  ${STUDIO}" ${COL_TEXT} ${COL_CARD2} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro SolidLabel 2% 152u 96% 14u "اتصال برنامهٔ اندروید، مستقیم و امن به SQL Server (پورت ۱۴۳۳) — بدون IIS و بدون API میانی." ${COL_MUTED} ${COL_CARD2} ${SS_LEFT}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro PageCredit 176

  Call ToggleActField
  Call muiPageLoadFullWindow
  nsDialogs::Show
  Call muiPageUnloadFullWindow
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
  StrCpy $SqlUser ""
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
  StrCpy $DbListOk "0"
  StrCpy $DbListCount "0"

  ; فونت‌های رابط (یک‌بار برای همهٔ صفحه‌ها)
  CreateFont $FontTitle "Tahoma" "12" "700"
  CreateFont $FontHead  "Tahoma" "10" "700"
  CreateFont $FontBody  "Tahoma" "9"  "400"
  CreateFont $FontSmall "Tahoma" "8"  "400"
  CreateFont $FontBtn   "Tahoma" "10" "700"

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
Function StyleWizardButtons
  ; دکمه‌های پایین ویزارد: متن درشت‌تر و خواناتر (بعدی / قبلی / انصراف)
  GetDlgItem $0 $HWNDPARENT 1
  SendMessage $0 ${WM_SETFONT} $FontBtn 1
  GetDlgItem $0 $HWNDPARENT 2
  SendMessage $0 ${WM_SETFONT} $FontBtn 1
  GetDlgItem $0 $HWNDPARENT 3
  SendMessage $0 ${WM_SETFONT} $FontBtn 1
FunctionEnd

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
  Delete "$SMPROGRAMS\${PRODUCT_FA}\شروع سرویس سامانه.lnk"
  Delete "$SMPROGRAMS\${PRODUCT_FA}\توقف سرویس سامانه.lnk"
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
