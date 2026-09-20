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
!define DATA_HOME   "C:\ProgramData\Vizitor"

; --- سازنده و گروه نرم‌افزاری (در همهٔ صفحه‌ها، مشخصات فایل و رجیستری دیده می‌شود) ---
!define AUTHOR_FA   "میلاد یقوبی"
!define AUTHOR_EN   "Milad Yaghoobi"
!define STUDIO      "Meelano Studio Design"

; --- رنگ‌های رابط (قالب 0xRRGGBB — به‌ترتیب بایت‌های قرمز، سبز، آبی) ---
!define COL_PAGE      0x0B2138
!define COL_BAND      0x102A45
!define COL_NAVY1     0x0B2138
!define COL_NAVY2     0x102A45
!define COL_NAVY3     0x143254
!define COL_NAVY      0x0E2B4A
!define COL_ACCENT    0xD9A23C
!define COL_CARD      0xF4F8FC
!define COL_EDGE      0xBFD0E0
!define COL_WHITE     0xFFFFFF
!define COL_TEXT      0x1D2A38
!define COL_MUTED     0x5C6C7C
!define COL_BRAND     0x1B5FA8
!define COL_BTN_TOP   0x7FB4E0
!define COL_BTN_DARK  0x123F6B
!define COL_BAND_TEXT 0xEAF3FB
!define COL_OK        0x1E7B45
!define COL_WARN      0x9A6A00
!define COL_ERR       0xA93125

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
Var /GLOBAL Detected
Var /GLOBAL InstallRc
Var /GLOBAL IpAddr
Var /GLOBAL ApiPort
Var /GLOBAL ApiUrlDisplay
Var /GLOBAL SqlHost
Var /GLOBAL SqlPort
Var /GLOBAL SqlAuth
Var /GLOBAL SqlUser
Var /GLOBAL SqlPass
Var /GLOBAL DbName
Var /GLOBAL ErpDb
Var /GLOBAL PublicIpField
Var /GLOBAL ExternalOk
Var /GLOBAL HealthWanted
Var /GLOBAL AdminUser
Var /GLOBAL ActCode
Var /GLOBAL ActLater
Var /GLOBAL DbCount
Var /GLOBAL DbIdx
Var /GLOBAL DbListOk
Var /GLOBAL DbListCount
Var /GLOBAL hIp
Var /GLOBAL hPort
Var /GLOBAL hUrlHint
Var /GLOBAL hStatus
Var /GLOBAL hDbName
Var /GLOBAL hHealth
Var /GLOBAL hSqlHost
Var /GLOBAL hSqlPort
Var /GLOBAL hAuthSql
Var /GLOBAL hAuthWin
Var /GLOBAL hSqlUser
Var /GLOBAL hSqlPass
Var /GLOBAL hPublicIp
Var /GLOBAL hExternal
Var /GLOBAL hSqlTest
Var /GLOBAL hSqlStatus
Var /GLOBAL hDbList
Var /GLOBAL hListBtn
Var /GLOBAL hListStatus
Var /GLOBAL hErpDbEdit
Var /GLOBAL hConfirmBtn
Var /GLOBAL hDbConfirm
Var /GLOBAL hAct
Var /GLOBAL hActLater
Var /GLOBAL FontTitle
Var /GLOBAL FontHead
Var /GLOBAL FontBody
Var /GLOBAL FontSmall
Var /GLOBAL FontBtn
Var /GLOBAL FontLoaded

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
!define MUI_WELCOMEPAGE_TEXT  "این برنامه، سرور ویزیتور را روی این کامپیوتر نصب و آمادهٔ استفاده می‌کند:$\r$\n$\r$\n• سرویس سامانه و پنل مدیریت روی پورت ۹۵۹۵ (فقط شبکهٔ محلی)$\r$\n• اتصال مستقیم برنامهٔ اندروید به SQL Server روی پورت ۱۴۳۳ — بدون IIS و بدون API میانی$\r$\n• دیتابیس سامانه در SQL Server (فقط ساخت چیزهای ناموجود؛ هیچ دادهٔ موجودی تغییر نمی‌کند)$\r$\n• کاربر محدود دیتابیس، قاعده‌های فایروال، کارت اتصال و راهنماهای فارسی$\r$\n$\r$\nدر صفحهٔ بعد، بخش‌های موردنیاز را تیک بزنید.$\r$\n$\r$\nطراحی و برنامه‌نویسی: ${AUTHOR_FA} (${AUTHOR_EN})  •  گروه: ${STUDIO}"

!define MUI_LICENSEPAGE_TEXT_TOP    "لطفاً شرایط استفاده را بخوانید."
!define MUI_LICENSEPAGE_TEXT_BOTTOM "برای ادامه، شرایط بالا را بپذیرید."
!define MUI_COMPONENTSPAGE_TEXT_TOP "هر بخشی که لازم دارید تیک بزنید؛ توضیح هر بخش پایین همین صفحه نمایش داده می‌شود."
!define MUI_DIRECTORYPAGE_TEXT_TOP  "پوشهٔ نصب سامانه. سرویس سامانه، ابزارها و راهنماها این‌جا قرار می‌گیرند."

!define MUI_FINISHPAGE_TITLE          "نصب ${PRODUCT_FA} کامل شد"
!define MUI_FINISHPAGE_TEXT           "آدرس پنل مدیریت سامانه:$\r$\n$ApiUrlDisplay$\r$\n$\r$\nاتصال مستقیم اندروید به SQL Server (پورت ۱۴۳۳) آماده شد؛ جزئیات و کد QR:$\r$\n$INSTDIR\setup\android-connect.png$\r$\n$\r$\nتنظیمات و لاگ‌ها:  C:\ProgramData\Vizitor$\r$\n$\r$\nاگر بازرسی خودکار به مشکل خورد، از میان‌بر «بازرسی و تعمیر» استفاده کنید.$\r$\n$\r$\nسامانهٔ ویزیتور — طراحی و برنامه‌نویسی: ${AUTHOR_FA} (${AUTHOR_EN})  •  گروه نرم‌افزاری: ${STUDIO}"
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
  SetOutPath "$INSTDIR\panel"
  File /r /x __pycache__ "..\panel\*.*"
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
  File /oname=Vazirmatn-OFL.txt "assets\fonts\OFL.txt"
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

  ; ذخیرهٔ پاسخ‌های ویزارد برای نصب/تعمیر بعدی — رمز SQL هرگز نوشته نمی‌شود
  CreateDirectory "${DATA_HOME}"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "ip"        "$IpAddr"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "port"      "$ApiPort"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "sqlhost"   "$SqlHost"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "sqlport"   "$SqlPort"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "sqlauth"   "$SqlAuth"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "sqluser"   "$SqlUser"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "erpdb"     "$ErpDb"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "dbname"    "$DbName"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "publicip"  "$PublicIpField"
  WriteINIStr "${DATA_HOME}\wizard_last.ini" "wizard" "external"  "$ExternalOk"

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
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCore}       "سرویس سامانه (${TASK_NAME}) و پنل مدیریت روی پورت ۹۵۹۵ (صفحهٔ پنل + فونت وزیرمتن + پیکربندی و ابزارها) — بدون IIS و بدون API میانی. (اتصال برنامهٔ اندروید مستقل از این بخش و مستقیم به SQL Server است.)"
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
;  قالب ظاهری: نوار گرادیانی، کارت‌های سه‌بعدی با لبه و سایه، دکمه‌های برجسته
;  قاعدهٔ اندازه‌ها: x و عرض به‌صورت رشتهٔ آماده (۲٪ یا ۹۶٪ یا 100%) و y و ارتفاع
;  به‌صورت عدد ساده پاس داده می‌شوند؛ داخل ماکرو به «u» تبدیل می‌شوند.
;  همهٔ کنترل‌ها داخل کادر استاندارد صفحه‌های ویزارد (۳۰۰×۱۴۰ واحد) جا می‌شوند.
; ---------------------------------------------------------------------------
!macro Rect x y w h col
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}" 0 ${x} ${y}u ${w} ${h}u ""
  Pop $0
  SetCtlColors $0 "" ${col}
!macroend

; کارت سه‌بعدی: بدنهٔ روشن + لبهٔ سفید بالا + سایهٔ ۲ واحدی پایین
!macro CardBox x y w h
  !define /math _cb1 ${y} + ${h}
  !define /math _cb ${_cb1} - 1
  !insertmacro Rect ${x} ${y} ${w} ${h} ${COL_CARD}
  !insertmacro Rect ${x} ${_cb} ${w} 1u ${COL_EDGE}
  !insertmacro Rect ${x} ${y} ${w} 1u ${COL_WHITE}
  !undef _cb
  !undef _cb1
!macroend

!macro CardTitle x y w text
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 ${x} ${y}u ${w} 11u "${text}"
  Pop $0
  SetCtlColors $0 ${COL_NAVY} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro Rect ${x} ${y} ${w} 1u ${COL_ACCENT}
!macroend

!macro Txt x y w h text colFg colBg
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 ${x} ${y}u ${w} ${h}u "${text}"
  Pop $0
  SetCtlColors $0 ${colFg} ${colBg}
!macroend

; دکمهٔ برجسته: دکمهٔ واقعی ویندوز + پایهٔ تیرهٔ دو واحدی + درخشش یک‌واحدی بالا
!macro Btn x y w h text hOut
  !define /math _bh ${h} - 2
  !define /math _bb1 ${y} + ${h}
  !define /math _bb ${_bb1} - 2
  ${NSD_CreateButton} ${x} ${y}u ${w} ${_bh}u "${text}"
  Pop ${hOut}
  SetCtlColors ${hOut} ${COL_WHITE} ${COL_BRAND}
  SendMessage ${hOut} ${WM_SETFONT} $FontBtn 1
  !insertmacro Rect ${x} ${_bb} ${w} 2u ${COL_BTN_DARK}
  !insertmacro Rect ${x} ${y} ${w} 1u ${COL_BTN_TOP}
  !undef _bb
  !undef _bb1
  !undef _bh
!macroend

; نوار بالای صفحه: گرادیان سه‌رنگ + خط طلایی + شمارهٔ گام
!macro PageBg
  !insertmacro Rect 0 0 300 140 ${COL_PAGE}
!macroend

!macro PageBand step
  !insertmacro Rect 0 0 100% 8 ${COL_BAND}
  !insertmacro Rect 0 8 100% 1 ${COL_ACCENT}
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_RIGHT}|${SS_NOTIFY}" 0 2% 2u 96% 6u "گام ${step} از ۴   •   سامانهٔ ویزیتور"
  Pop $0
  SetCtlColors $0 ${COL_BAND_TEXT} ${COL_BAND}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
!macroend

; نوار پایین: امضای سازنده و گروه نرم‌افزاری
!macro PageCreditBar
  !insertmacro Rect 0 130 100% 10 ${COL_BAND}
  !insertmacro Rect 0 129 100% 1 ${COL_ACCENT}
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_CENTER}|${SS_NOTIFY}" 0 0 130u 100% 10u "طراحی و برنامه‌نویسی: ${AUTHOR_FA} (${AUTHOR_EN})   •   گروه نرم‌افزاری: ${STUDIO}"
  Pop $0
  SetCtlColors $0 ${COL_BAND_TEXT} ${COL_BAND}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
!macroend

; ---------------------------------------------------------------------------
;  صفحهٔ ۱ — سرور سامانه
; ---------------------------------------------------------------------------
Function PageServerCreate
  !insertmacro MUI_HEADER_TEXT "سرور سامانه" "آدرس و پورت پنل مدیریت — اتصال برنامهٔ اندروید مستقل و مستقیم به SQL Server است"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_PAGE}

  !insertmacro PageBg
  !insertmacro PageBand ۱

  !insertmacro CardBox 0 9 100 61
  !insertmacro CardTitle 2% 14 96% "آدرس سرور و پورت پنل مدیریت"
  !insertmacro Txt 2% 26 44% 9 "آدرس سرور (IP یا دامنهٔ داخلی):" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 48% 24u 50% 13u "$IpAddr"
  Pop $hIp
  SendMessage $hIp ${WM_SETFONT} $FontBody 1
  !insertmacro Txt 2% 41 44% 9 "پورت پنل مدیریت:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 48% 39u 22% 13u "$ApiPort"
  Pop $hPort
  SendMessage $hPort ${WM_SETFONT} $FontBody 1
  !insertmacro Txt 72% 41 26% 9 "(پیش‌فرض ۹۵۹۵)" ${COL_MUTED} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  !insertmacro Txt 2% 54 96% 8 "• این پورت فقط برای شبکهٔ محلی باز می‌شود؛ IIS و API میانی وجود ندارد." ${COL_MUTED} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 2% 63u 96% 8u "آدرس نهایی پنل:  $ApiUrlDisplay"
  Pop $hUrlHint
  SetCtlColors $hUrlHint ${COL_BRAND} ${COL_CARD}
  SendMessage $hUrlHint ${WM_SETFONT} $FontSmall 1

  !insertmacro CardBox 0 73 100 53
  !insertmacro CardTitle 2% 78 96% "دیتابیس خود سامانهٔ ویزیتور و وضعیت همین سیستم"
  !insertmacro Txt 2% 90 34% 9 "دیتابیس خود ویزیتور:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 38% 88u 30% 13u "$DbName"
  Pop $hDbName
  SendMessage $hDbName ${WM_SETFONT} $FontBody 1
  ${NSD_CreateCheckBox} 70% 88u 28% 14u "بررسی سلامت در پایان نصب"
  Pop $hHealth
  SetCtlColors $hHealth ${COL_TEXT} ${COL_CARD}
  SendMessage $hHealth ${WM_SETFONT} $FontSmall 1
  ${NSD_SetState} $hHealth ${BST_CHECKED}
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 2% 104u 96% 19u "$Detected"
  Pop $hStatus
  SetCtlColors $hStatus ${COL_MUTED} ${COL_CARD}
  SendMessage $hStatus ${WM_SETFONT} $FontSmall 1

  !insertmacro PageCreditBar
  Call RefreshUrlHint
  nsDialogs::Show
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
  ${NSD_SetText} $hUrlHint "آدرس نهایی پنل:  $ApiUrlDisplay"
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

; ---------------------------------------------------------------------------
;  صفحهٔ ۲ — اتصال به SQL Server
; ---------------------------------------------------------------------------
Function PageSqlCreate
  !insertmacro MUI_HEADER_TEXT "اتصال به SQL Server" "پورت پیش‌فرض ۱۴۳۳ — رمز فقط برای همین نصب استفاده می‌شود"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_PAGE}

  !insertmacro PageBg
  !insertmacro PageBand ۲

  !insertmacro CardBox 0 9 100 55
  !insertmacro CardTitle 2% 14 96% "مشخصات ورود به SQL Server"
  !insertmacro Txt 2% 25 26% 9 "سرور SQL Server:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 30% 23u 34% 13u "$SqlHost"
  Pop $hSqlHost
  SendMessage $hSqlHost ${WM_SETFONT} $FontBody 1
  !insertmacro Txt 66% 25 8% 9 "پورت:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 76% 23u 22% 13u "$SqlPort"
  Pop $hSqlPort
  SendMessage $hSqlPort ${WM_SETFONT} $FontBody 1
  ${NSD_CreateRadioButton} 2% 38u 48% 11u "کاربر SQL Server (نام کاربری و رمز)"
  Pop $hAuthSql
  SetCtlColors $hAuthSql ${COL_TEXT} ${COL_CARD}
  SendMessage $hAuthSql ${WM_SETFONT} $FontSmall 1
  ${NSD_CreateRadioButton} 52% 38u 46% 11u "حساب ویندوز همین سرور (بدون رمز)"
  Pop $hAuthWin
  SetCtlColors $hAuthWin ${COL_TEXT} ${COL_CARD}
  SendMessage $hAuthWin ${WM_SETFONT} $FontSmall 1
  ${NSD_OnClick} $hAuthSql OnAuthClick
  ${NSD_OnClick} $hAuthWin OnAuthClick
  ${If} $SqlAuth == "windows"
    ${NSD_SetState} $hAuthWin ${BST_CHECKED}
  ${Else}
    ${NSD_SetState} $hAuthSql ${BST_CHECKED}
  ${EndIf}
  !insertmacro Txt 2% 51 26% 9 "نام کاربری:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 30% 49u 34% 13u "$SqlUser"
  Pop $hSqlUser
  SendMessage $hSqlUser ${WM_SETFONT} $FontBody 1
  !insertmacro Txt 66% 51 8% 9 "کلمهٔ عبور:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreatePassword} 76% 49u 22% 13u ""
  Pop $hSqlPass
  SendMessage $hSqlPass ${WM_SETFONT} $FontBody 1

  !insertmacro CardBox 0 67 100 59
  !insertmacro CardTitle 2% 72 96% "اتصال از بیرون شبکه و آزمایش اتصال"
  !insertmacro Txt 2% 84 44% 9 "آی‌پی اختصاصی/اینترنتی سرور (اختیاری):" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  ${NSD_CreateText} 48% 82u 50% 13u "$PublicIpField"
  Pop $hPublicIp
  SendMessage $hPublicIp ${WM_SETFONT} $FontBody 1
  ${NSD_CreateCheckBox} 2% 98u 96% 11u "اجازهٔ اتصال به پورت ۱۴۳۳ از بیرون شبکه (فوروارد پورت در روتر)"
  Pop $hExternal
  SetCtlColors $hExternal ${COL_TEXT} ${COL_CARD}
  SendMessage $hExternal ${WM_SETFONT} $FontSmall 1
  ${If} $ExternalOk == "1"
    ${NSD_SetState} $hExternal ${BST_CHECKED}
  ${EndIf}
  !insertmacro Btn 2% 111 32% 13 "تست اتصال" $hSqlTest
  ${NSD_OnClick} $hSqlTest OnTestSqlClick
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 36% 111u 62% 13u "با این دکمه، اتصال را قبل از ادامه امتحان کنید."
  Pop $hSqlStatus
  SetCtlColors $hSqlStatus ${COL_MUTED} ${COL_CARD}
  SendMessage $hSqlStatus ${WM_SETFONT} $FontSmall 1

  !insertmacro PageCreditBar
  Call ToggleSqlFields
  nsDialogs::Show
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
    SetCtlColors $hSqlStatus ${COL_WARN} ${COL_CARD}
    ${NSD_SetText} $hSqlStatus "پایتون روی این سیستم پیدا نشد؛ تست اتصال ممکن نیست ولی نصب پیش‌نیازها آن را می‌آورد."
    Return
  ${EndIf}

  Delete "$PLUGINSDIR\dbcreds.ini"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "server" "$1"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "port" "$2"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "user" "$3"
  WriteINIStr "$PLUGINSDIR\dbcreds.ini" "sql" "pass" "$4"

  SetCtlColors $hSqlStatus ${COL_MUTED} ${COL_CARD}
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
      SetCtlColors $hSqlStatus ${COL_OK} ${COL_CARD}
      ${NSD_SetText} $hSqlStatus "اتصال برقرار شد — $DbListCount دیتابیس روی این سرور پیدا شد."
      Return
    ${EndIf}
    ReadINIStr $6 "$PLUGINSDIR\dbs.ini" "result" "error"
    ${If} $6 == ""
      StrCpy $6 "خطای نامشخص"
    ${EndIf}
    SetCtlColors $hSqlStatus ${COL_ERR} ${COL_CARD}
    ${NSD_SetText} $hSqlStatus "اتصال برقرار نشد: $6"
    Return
  test_failed:
    SetCtlColors $hSqlStatus ${COL_ERR} ${COL_CARD}
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
    StrCpy $SqlUser ""
    StrCpy $SqlPass ""
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
  !insertmacro MUI_HEADER_TEXT "انتخاب دیتابیس برنامه" "از فهرست سرور انتخاب کنید یا نام آن را دستی بنویسید — هیچ دیتابیسی اجباری نیست"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_PAGE}

  !insertmacro PageBg
  !insertmacro PageBand ۳

  !insertmacro CardBox 0 9 100 78
  !insertmacro CardTitle 2% 14 46% "۱) فهرست دیتابیس‌های روی سرور"
  !insertmacro Btn 50% 12 48% 13 "دریافت فهرست دیتابیس‌ها" $hListBtn
  ${NSD_OnClick} $hListBtn OnListDbClick
  ${NSD_CreateListBox} 2% 27u 96% 40u ""
  Pop $hDbList
  SendMessage $hDbList ${WM_SETFONT} $FontBody 1
  SetCtlColors $hDbList ${COL_TEXT} ${COL_WHITE}
  ${NSD_OnChange} $hDbList OnDbSelChange
  ${If} $ErpDb != ""
    ${NSD_LB_AddString} $hDbList "$ErpDb"
    ${NSD_LB_SelectString} $hDbList "$ErpDb"
  ${EndIf}
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 2% 69u 96% 8u "هنوز فهرستی گرفته نشده — دکمهٔ آبی بالا را بزنید."
  Pop $hListStatus
  SetCtlColors $hListStatus ${COL_MUTED} ${COL_CARD}
  SendMessage $hListStatus ${WM_SETFONT} $FontSmall 1
  !insertmacro Txt 2% 78 96% 8 "• با کلیک روی هر نام در فهرست، همان نام خودکار در کادر پایین می‌آید." ${COL_MUTED} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1

  !insertmacro CardBox 0 90 100 37
  !insertmacro CardTitle 2% 94 96% "۲) نام دیتابیس برنامه (حسابداری) — انتخاب از فهرست یا تایپ دستی"
  ${NSD_CreateText} 2% 105u 46% 14u "$ErpDb"
  Pop $hErpDbEdit
  SendMessage $hErpDbEdit ${WM_SETFONT} $FontBtn 1
  !insertmacro Btn 50% 105 48% 14 "تأیید نام دیتابیس" $hConfirmBtn
  ${NSD_OnClick} $hConfirmBtn OnConfirmDbClick
  nsDialogs::CreateControl STATIC "${DEFAULT_STYLES}|${SS_LEFT}|${SS_NOTIFY}" 0 2% 121u 96% 8u "نام را بنویسید یا از فهرست انتخاب کنید، سپس دکمهٔ آبی را بزنید."
  Pop $hDbConfirm
  SetCtlColors $hDbConfirm ${COL_MUTED} ${COL_CARD}
  SendMessage $hDbConfirm ${WM_SETFONT} $FontSmall 1

  !insertmacro PageCreditBar
  nsDialogs::Show
FunctionEnd

Function OnDbSelChange
  ${NSD_LB_GetSelection} $hDbList $0
  ${If} $0 == ""
    Return
  ${EndIf}
  ${NSD_SetText} $hErpDbEdit "$0"
  SetCtlColors $hDbConfirm ${COL_BRAND} ${COL_CARD}
  ${NSD_SetText} $hDbConfirm "از فهرست انتخاب شد: $0 — برای ثبت نهایی دکمهٔ آبی «تأیید نام دیتابیس» را بزنید."
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
      ${NSD_SetText} $hDbConfirm "توجه: «$0» در فهرست نبود؛ اگر مطمئنید درست است ادامه بدهید (بررسی سلامت هم امتحان می‌کند)."
    ${EndIf}
    Return
  ${EndIf}
  SetCtlColors $hDbConfirm ${COL_OK} ${COL_CARD}
  ${NSD_SetText} $hDbConfirm "تأیید شد: دیتابیس «$0» برای اتصال برنامه استفاده می‌شود."
FunctionEnd

Function OnListDbClick
  ${If} $PyExe == ""
    SetCtlColors $hListStatus ${COL_WARN} ${COL_CARD}
    ${NSD_SetText} $hListStatus "پایتون پیدا نشد؛ نام دیتابیس را دستی در کادر پایین بنویسید."
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
  loop_db:
    IntCmp $DbIdx $DbCount list_done
    ReadINIStr $5 "$PLUGINSDIR\dbs.ini" "db$DbIdx" "name"
    ${If} $5 != ""
      ${NSD_LB_AddString} $hDbList "$5"
    ${EndIf}
    IntOp $DbIdx $DbIdx + 1
    Goto loop_db
  list_done:
    StrCpy $DbListOk "1"
    ReadINIStr $6 "$PLUGINSDIR\dbs.ini" "db1" "name"
    ${If} $6 != ""
      ${NSD_LB_SelectString} $hDbList "$6"
      ${NSD_SetText} $hErpDbEdit "$6"
    ${EndIf}
    SetCtlColors $hListStatus ${COL_OK} ${COL_CARD}
    ${NSD_SetText} $hListStatus "$DbCount دیتابیس پیدا شد — روی نام موردنظر کلیک کنید."
    Goto list_end
  list_empty:
    SetCtlColors $hListStatus ${COL_WARN} ${COL_CARD}
    ${NSD_SetText} $hListStatus "هیچ دیتابیس کاربری پیدا نشد — سرور/کاربر/رمز را بررسی کنید یا نام را دستی بنویسید."
    Goto list_end
  list_failed:
    SetCtlColors $hListStatus ${COL_ERR} ${COL_CARD}
    ${NSD_SetText} $hListStatus "گرفتن فهرست ممکن نشد (کد $R0) — سرور/پورت/کاربر/رمز را بررسی کنید یا نام را دستی بنویسید."
  list_end:
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
  !insertmacro MUI_HEADER_TEXT "کد فعال‌سازی" "کد را از فروشندهٔ سامانه گرفته‌اید — اگر در دسترس نیست، تیک «بعداً» را بزنید"

  nsDialogs::Create 1018
  Pop $PageDialog
  ${If} $PageDialog == error
    Abort
  ${EndIf}
  nsDialogs::SetRTL $(^RTL)
  SetCtlColors $PageDialog "" ${COL_PAGE}

  !insertmacro PageBg
  !insertmacro PageBand ۴

  !insertmacro CardBox 0 9 100 65
  !insertmacro CardTitle 2% 14 96% "فعال‌سازی سامانه"
  !insertmacro Txt 2% 26 96% 18 "سامانه بدون کد هم نصب می‌شود، اما تا ورود کد فعال نمی‌شود. کد فقط در فایل تنظیمات سرور ذخیره می‌شود و هرگز در گزارش نصب یا لاگ‌ها چاپ نمی‌شود." ${COL_MUTED} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontSmall 1
  !insertmacro Txt 2% 47 22% 9 "کد فعال‌سازی:" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  ${NSD_CreateText} 26% 45u 72% 14u "$ActCode"
  Pop $hAct
  SendMessage $hAct ${WM_SETFONT} $FontBtn 1
  ${NSD_CreateCheckBox} 2% 62u 96% 12u "فعلاً کد ندارم؛ بعداً وارد می‌کنم."
  Pop $hActLater
  SetCtlColors $hActLater ${COL_TEXT} ${COL_CARD}
  SendMessage $hActLater ${WM_SETFONT} $FontSmall 1
  ${If} $ActLater == "1"
    ${NSD_SetState} $hActLater ${BST_CHECKED}
  ${EndIf}
  ${NSD_OnClick} $hActLater OnActLaterClick

  !insertmacro CardBox 0 77 100 50
  !insertmacro CardTitle 2% 82 96% "شناسنامهٔ سامانه"
  !insertmacro Txt 2% 94 96% 11 "سامانهٔ ویزیتور — نسخهٔ ${VERSION}" ${COL_NAVY} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontHead 1
  !insertmacro Txt 2% 105 96% 10 "طراحی و برنامه‌نویسی:  ${AUTHOR_FA}  (${AUTHOR_EN})" ${COL_TEXT} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1
  !insertmacro Txt 2% 114 96% 10 "گروه نرم‌افزاری:  ${STUDIO}" ${COL_BRAND} ${COL_CARD}
  SendMessage $0 ${WM_SETFONT} $FontBody 1

  !insertmacro PageCreditBar
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

  InitPluginsDir

  ; فونت وزیرمتن فقط برای همین نصب‌کننده بارگذاری می‌شود (روی سیستم نصب نمی‌شود)
  File /oname=$PLUGINSDIR\Vazirmatn-Regular.ttf "assets\fonts\Vazirmatn-Regular.ttf"
  File /oname=$PLUGINSDIR\Vazirmatn-Bold.ttf    "assets\fonts\Vazirmatn-Bold.ttf"
  System::Call 'gdi32::AddFontResource(t "$PLUGINSDIR\Vazirmatn-Regular.ttf") i .r0'
  System::Call 'gdi32::AddFontResource(t "$PLUGINSDIR\Vazirmatn-Bold.ttf") i .r1'
  StrCpy $FontLoaded "$r0"

  ; فونت‌های رابط (یک‌بار برای همهٔ صفحه‌ها) — وزیرمتن با سه اندازه
  CreateFont $FontTitle "Vazirmatn" "13" "700"
  CreateFont $FontHead  "Vazirmatn" "11" "700"
  CreateFont $FontBody  "Vazirmatn" "10" "400"
  CreateFont $FontSmall "Vazirmatn" "9"  "400"
  CreateFont $FontBtn   "Vazirmatn" "11" "700"
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

  ; اگر نصب قبلی هست، کادرها با مقادیر همان نصب پر می‌شوند (رمز SQL هرگز ذخیره نمی‌شود)
  ${If} $PrevExists == "1"
    Call LoadLastWizard
  ${EndIf}

  IfSilent skip_prev_check
  ${If} $PrevExists == "1"
    MessageBox MB_YESNO|MB_ICONQUESTION "نسخهٔ قبلی ویزیتور روی این کامپیوتر پیدا شد.$\r$\n$\r$\nبله = به‌روزرسانی و تعمیر: فایل‌های برنامهٔ قبلی با نسخهٔ نو جایگزین می‌شوند (تنظیمات و دیتابیس دست‌نخورده می‌مانند).$\r$\nخیر = نصب تازه: کادرها با مقادیر نصب قبلی پر شده‌اند و می‌توانید تغییرشان دهید.$\r$\n$\r$\nدر هر دو حالت، صفحه‌های تنظیمات نمایش داده می‌شوند." IDYES do_update
  ${EndIf}
  Goto skip_prev_check
  do_update:
    StrCpy $FreshClean "1"
  skip_prev_check:
FunctionEnd

; خواندن مقادیر آخرین نصب (بدون رمز) برای پر بودن کادرها
Function LoadLastWizard
  IfFileExists "${DATA_HOME}\wizard_last.ini" 0 lw_end
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "ip"
  ${If} $0 != ""
    StrCpy $IpAddr "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "port"
  ${If} $0 != ""
    StrCpy $ApiPort "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "sqlhost"
  ${If} $0 != ""
    StrCpy $SqlHost "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "sqlport"
  ${If} $0 != ""
    StrCpy $SqlPort "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "sqlauth"
  ${If} $0 != ""
    StrCpy $SqlAuth "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "sqluser"
  ${If} $0 != ""
    StrCpy $SqlUser "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "erpdb"
  ${If} $0 != ""
    StrCpy $ErpDb "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "dbname"
  ${If} $0 != ""
    StrCpy $DbName "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "publicip"
  ${If} $0 != ""
    StrCpy $PublicIpField "$0"
  ${EndIf}
  ReadINIStr $0 "${DATA_HOME}\wizard_last.ini" "wizard" "external"
  ${If} $0 != ""
    StrCpy $ExternalOk "$0"
  ${EndIf}
  lw_end:
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

Function .onGUIEnd
  ; فونت‌هایی که فقط برای این نصب‌کننده بارگذاری شده بودند آزاد می‌شوند
  ${If} $FontLoaded != "0"
    System::Call 'gdi32::RemoveFontResource(t "$PLUGINSDIR\Vazirmatn-Regular.ttf") i .r0'
    System::Call 'gdi32::RemoveFontResource(t "$PLUGINSDIR\Vazirmatn-Bold.ttf") i .r0'
  ${EndIf}
FunctionEnd

Function un.onInit
  SetRegView 64
  SetShellVarContext all
FunctionEnd

Section "Uninstall"
  SetRegView 64
  SetShellVarContext all

  DetailPrint "توقف و حذف سرویس سامانه ..."
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
