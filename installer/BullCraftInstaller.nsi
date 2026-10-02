Unicode true

!include "MUI2.nsh"
!include "LogicLib.nsh"

!define APP_NAME "BullMC Client"
!define APP_EXE "BullMCClient.exe"
!define APP_VERSION "0.1.1"
!define APP_SOURCE "..\build\compose\binaries\main\app\BullMCClient"
!define COMPANY_NAME "BullCraft"
!define REG_KEY "Software\BullCraft\BullMCClient"

Name "${APP_NAME}"
Caption "Установка ${APP_NAME}"
OutFile "..\build\release\BullCraft-Installer-${APP_VERSION}.exe"
InstallDir "$LOCALAPPDATA\BullMCClient"
InstallDirRegKey HKCU "${REG_KEY}" "InstallDir"
RequestExecutionLevel user
Icon "..\src\main\resources\bull.ico"
UninstallIcon "..\src\main\resources\bull.ico"
BrandingText "BullCraft Installer"
XPStyle on
ShowInstDetails show
ShowUninstDetails show

!define MUI_ABORTWARNING
!define MUI_ICON "..\src\main\resources\bull.ico"
!define MUI_UNICON "..\src\main\resources\bull.ico"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "assets\header.bmp"
!define MUI_HEADERIMAGE_UNBITMAP "assets\header.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "assets\welcome.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "assets\welcome.bmp"
!define MUI_FINISHPAGE_RUN "$INSTDIR\${APP_EXE}"
!define MUI_FINISHPAGE_RUN_TEXT "Запустить BullMC Client после установки"
!define MUI_FINISHPAGE_LINK "Открыть страницу проекта на GitHub"
!define MUI_FINISHPAGE_LINK_LOCATION "https://github.com/idnyakrytoi-afk/bulauncher"

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

!insertmacro MUI_LANGUAGE "Russian"

LangString DESC_SecMain ${LANG_RUSSIAN} "Основные файлы BullMC Client. Этот компонент обязателен."
LangString DESC_SecDesktopShortcut ${LANG_RUSSIAN} "Создать ярлык BullMC Client на рабочем столе."
LangString DESC_SecStartMenu ${LANG_RUSSIAN} "Добавить ярлыки BullMC Client и удаления в меню Пуск."

Section "BullMC Client" SecMain
    SectionIn RO

    SetOutPath "$INSTDIR"
    File /r "${APP_SOURCE}\*"

    WriteRegStr HKCU "${REG_KEY}" "InstallDir" "$INSTDIR"
    WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

Section "Создать ярлык на рабочем столе" SecDesktopShortcut
    CreateShortcut "$DESKTOP\BullMC Client.lnk" "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_EXE}" 0
SectionEnd

Section "Добавить в меню Пуск" SecStartMenu
    CreateDirectory "$SMPROGRAMS\${COMPANY_NAME}"
    CreateShortcut "$SMPROGRAMS\${COMPANY_NAME}\BullMC Client.lnk" "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_EXE}" 0
    CreateShortcut "$SMPROGRAMS\${COMPANY_NAME}\Удалить BullMC Client.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
    !insertmacro MUI_DESCRIPTION_TEXT ${SecMain} $(DESC_SecMain)
    !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktopShortcut} $(DESC_SecDesktopShortcut)
    !insertmacro MUI_DESCRIPTION_TEXT ${SecStartMenu} $(DESC_SecStartMenu)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "Uninstall"
    Delete "$DESKTOP\BullMC Client.lnk"
    Delete "$SMPROGRAMS\${COMPANY_NAME}\BullMC Client.lnk"
    Delete "$SMPROGRAMS\${COMPANY_NAME}\Удалить BullMC Client.lnk"
    RMDir "$SMPROGRAMS\${COMPANY_NAME}"

    Delete "$INSTDIR\Uninstall.exe"
    RMDir /r "$INSTDIR"
    DeleteRegKey HKCU "${REG_KEY}"
SectionEnd