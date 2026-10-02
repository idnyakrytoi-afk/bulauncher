Unicode true
Name "BullCraft Launcher"
Caption "BullCraft Launcher Setup"
OutFile "..\build\release\BullCraft-Installer-0.1.1.exe"
InstallDir "$LOCALAPPDATA\BullMCClient"
InstallDirRegKey HKCU "Software\BullCraft\BullMCClient" "InstallDir"
RequestExecutionLevel user
Icon "..\src\main\resources\bull.ico"
UninstallIcon "..\src\main\resources\bull.ico"
BrandingText "BullCraft Liquid Installer"
XPStyle on

!include "MUI2.nsh"
!include "nsDialogs.nsh"
!include "LogicLib.nsh"
!include "WinMessages.nsh"

!define APP_NAME "BullMC Client"
!define APP_EXE "BullMCClient.exe"
!define APP_VERSION "0.1.1"
!define APP_SOURCE "..\build\compose\binaries\main\app\BullMCClient"
!define ACCENT "00D8FF"
!define DARK_BG "0B0F17"
!define PANEL_BG "151A24"
!define TEXT_MAIN "F4F7FF"
!define TEXT_MUTED "AAB7C8"

Var DesktopShortcut
Var RunAfterInstall
Var WelcomeImageHandle
Var WelcomeImage
Var DesktopCheckbox
Var RunCheckbox

Page custom WelcomePageCreate
!insertmacro MUI_PAGE_DIRECTORY
Page custom OptionsPageCreate OptionsPageLeave
!insertmacro MUI_PAGE_INSTFILES
Page custom FinishPageCreate FinishPageLeave

!insertmacro MUI_LANGUAGE "Russian"

Function .onInit
    StrCpy $DesktopShortcut "1"
    StrCpy $RunAfterInstall "1"
    InitPluginsDir
    File /oname=$PLUGINSDIR\welcome-small.bmp "assets\welcome-small.bmp"
FunctionEnd

Function WelcomePageCreate
    nsDialogs::Create 1018
    Pop $0
    ${If} $0 == error
        Abort
    ${EndIf}

    SetCtlColors $0 0x${TEXT_MAIN} 0x${DARK_BG}

    ${NSD_CreateBitmap} 0 0 100% 68u ""
    Pop $WelcomeImage
    ${NSD_SetImage} $WelcomeImage "$PLUGINSDIR\welcome-small.bmp" $WelcomeImageHandle

    ${NSD_CreateLabel} 0 78u 100% 18u "Добро пожаловать в BullCraft Launcher"
    Pop $1
    SetCtlColors $1 0x${TEXT_MAIN} 0x${DARK_BG}
    CreateFont $2 "Segoe UI" 13 700
    SendMessage $1 ${WM_SETFONT} $2 1

    ${NSD_CreateLabel} 0 102u 100% 30u "Темный installer для BullMC Client: логотип, выбор папки, ярлыки и аккуратная установка portable runtime."
    Pop $1
    SetCtlColors $1 0x${TEXT_MUTED} 0x${DARK_BG}
    CreateFont $2 "Segoe UI" 9 400
    SendMessage $1 ${WM_SETFONT} $2 1

    ${NSD_CreateLabel} 0 143u 100% 22u "Нажми Далее, чтобы выбрать папку установки."
    Pop $1
    SetCtlColors $1 0x${ACCENT} 0x${DARK_BG}
    CreateFont $2 "Segoe UI" 10 600
    SendMessage $1 ${WM_SETFONT} $2 1

    nsDialogs::Show
FunctionEnd

Function OptionsPageCreate
    nsDialogs::Create 1018
    Pop $0
    ${If} $0 == error
        Abort
    ${EndIf}

    SetCtlColors $0 0x${TEXT_MAIN} 0x${DARK_BG}

    ${NSD_CreateLabel} 0 0 100% 18u "Пара финальных галочек"
    Pop $1
    SetCtlColors $1 0x${TEXT_MAIN} 0x${DARK_BG}
    CreateFont $2 "Segoe UI" 13 700
    SendMessage $1 ${WM_SETFONT} $2 1

    ${NSD_CreateLabel} 0 26u 100% 24u "Выбери, что нужно добавить после установки BullMC Client."
    Pop $1
    SetCtlColors $1 0x${TEXT_MUTED} 0x${DARK_BG}

    ${NSD_CreateCheckbox} 0 62u 100% 14u "Создать ярлык на рабочем столе"
    Pop $DesktopCheckbox
    SetCtlColors $DesktopCheckbox 0x${TEXT_MAIN} 0x${DARK_BG}
    ${If} $DesktopShortcut == "1"
        ${NSD_Check} $DesktopCheckbox
    ${EndIf}

    ${NSD_CreateCheckbox} 0 84u 100% 14u "Запустить BullMC Client после установки"
    Pop $RunCheckbox
    SetCtlColors $RunCheckbox 0x${TEXT_MAIN} 0x${DARK_BG}
    ${If} $RunAfterInstall == "1"
        ${NSD_Check} $RunCheckbox
    ${EndIf}

    ${NSD_CreateLabel} 0 122u 100% 34u "Ярлык в меню Пуск создается всегда: BullCraft → BullMC Client."
    Pop $1
    SetCtlColors $1 0x${ACCENT} 0x${DARK_BG}

    nsDialogs::Show
FunctionEnd

Function OptionsPageLeave
    ${NSD_GetState} $DesktopCheckbox $DesktopShortcut
    ${NSD_GetState} $RunCheckbox $RunAfterInstall
FunctionEnd

Section "BullMC Client" SecMain
    SetOutPath "$INSTDIR"
    File /r "${APP_SOURCE}\*"

    WriteRegStr HKCU "Software\BullCraft\BullMCClient" "InstallDir" "$INSTDIR"
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    CreateDirectory "$SMPROGRAMS\BullCraft"
    CreateShortcut "$SMPROGRAMS\BullCraft\BullMC Client.lnk" "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_EXE}" 0
    CreateShortcut "$SMPROGRAMS\BullCraft\Uninstall BullMC Client.lnk" "$INSTDIR\Uninstall.exe" "" "$INSTDIR\Uninstall.exe" 0

    ${If} $DesktopShortcut == ${BST_CHECKED}
        CreateShortcut "$DESKTOP\BullMC Client.lnk" "$INSTDIR\${APP_EXE}" "" "$INSTDIR\${APP_EXE}" 0
    ${EndIf}
SectionEnd

Section "Uninstall"
    Delete "$DESKTOP\BullMC Client.lnk"
    Delete "$SMPROGRAMS\BullCraft\BullMC Client.lnk"
    Delete "$SMPROGRAMS\BullCraft\Uninstall BullMC Client.lnk"
    RMDir "$SMPROGRAMS\BullCraft"
    RMDir /r "$INSTDIR"
    DeleteRegKey HKCU "Software\BullCraft\BullMCClient"
SectionEnd

Function FinishPageCreate
    nsDialogs::Create 1018
    Pop $0
    ${If} $0 == error
        Abort
    ${EndIf}

    SetCtlColors $0 0x${TEXT_MAIN} 0x${DARK_BG}

    ${NSD_CreateLabel} 0 10u 100% 20u "BullMC Client установлен"
    Pop $1
    SetCtlColors $1 0x${TEXT_MAIN} 0x${DARK_BG}
    CreateFont $2 "Segoe UI" 14 700
    SendMessage $1 ${WM_SETFONT} $2 1

    ${NSD_CreateLabel} 0 42u 100% 42u "Готово. Лаунчер установлен в выбранную папку, а ярлыки добавлены в меню Пуск."
    Pop $1
    SetCtlColors $1 0x${TEXT_MUTED} 0x${DARK_BG}

    ${NSD_CreateLabel} 0 104u 100% 22u "Нажми Готово, чтобы закрыть installer."
    Pop $1
    SetCtlColors $1 0x${ACCENT} 0x${DARK_BG}

    nsDialogs::Show
FunctionEnd

Function FinishPageLeave
    ${If} $RunAfterInstall == ${BST_CHECKED}
        Exec "$INSTDIR\${APP_EXE}"
    ${EndIf}
FunctionEnd
