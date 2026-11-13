!include "MUI2.nsh"

!ifndef APPROOT
  !error "APPROOT missing"
!endif
!ifndef OUTDIR
  !error "OUTDIR missing"
!endif
!ifndef ICONROOT
  !error "ICONROOT missing"
!endif
!ifndef BRANDROOT
  !error "BRANDROOT missing"
!endif

Name "DEXRIVE 1.4.0"
OutFile "${OUTDIR}\DEXRIVE-1.4.0-setup.exe"
Unicode true
RequestExecutionLevel user
InstallDir "$LOCALAPPDATA\Programs\DEXRIVE"
InstallDirRegKey HKCU "Software\DEXRIVE" "InstallDir"
SetCompressor /SOLID lzma
SetCompressorDictSize 32
Icon "${ICONROOT}"
UninstallIcon "${ICONROOT}"
ShowInstDetails show
ShowUninstDetails show
BrandingText "DEXRIVE  •  by Brendigo  •  brendigo.com"

; Original DEXRIVE install artwork, generated locally during packaging.
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "${BRANDROOT}\dexrive-header.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "${BRANDROOT}\dexrive-welcome.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "${BRANDROOT}\dexrive-welcome.bmp"
!define MUI_ABORTWARNING
!define MUI_ICON "${ICONROOT}"
!define MUI_UNICON "${ICONROOT}"

!define MUI_WELCOMEPAGE_TITLE "Welcome to DEXRIVE"
!define MUI_WELCOMEPAGE_TEXT "A modern workspace for exploring Android packages, bytecode, and resources.$\r$\n$\r$\nThis wizard installs DEXRIVE for the current Windows user. No administrator permissions are required."
!define MUI_LICENSEPAGE_CHECKBOX
!define MUI_LICENSEPAGE_CHECKBOX_TEXT "I accept the terms of the license agreement"
!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_FINISHPAGE_RUN "$INSTDIR\DEXRIVE.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Launch DEXRIVE"
!define MUI_FINISHPAGE_TITLE "DEXRIVE is ready"
!define MUI_UNCONFIRMPAGE_TEXT_TOP "Remove DEXRIVE program files and shortcuts? Your personal projects will not be deleted."

!insertmacro MUI_PAGE_WELCOME
; Preserve original Apache-2.0 license; display agreement before installation.
!insertmacro MUI_PAGE_LICENSE "${APPROOT}\LICENSE"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH
!insertmacro MUI_LANGUAGE "English"

Section "DEXRIVE application (required)" SecCore
  SectionIn RO
  SetShellVarContext current
  SetOutPath "$INSTDIR"
  File /r "${APPROOT}\*"

  ; Program and uninstall metadata only; no unrelated system settings.
  WriteRegStr HKCU "Software\DEXRIVE" "InstallDir" "$INSTDIR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "DisplayName" "DEXRIVE 1.4.0"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "DisplayVersion" "1.4.0"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "Publisher" "Brendigo"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "DisplayIcon" "$INSTDIR\DEXRIVE.exe"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE" "NoRepair" 1
  WriteUninstaller "$INSTDIR\Uninstall.exe"
SectionEnd

Section "Start menu shortcut" SecMenu
  SetShellVarContext current
  CreateDirectory "$SMPROGRAMS\DEXRIVE"
  CreateShortcut "$SMPROGRAMS\DEXRIVE\DEXRIVE.lnk" "$INSTDIR\DEXRIVE.exe"
SectionEnd

Section /o "Desktop shortcut" SecDesktop
  SetShellVarContext current
  CreateShortcut "$DESKTOP\DEXRIVE.lnk" "$INSTDIR\DEXRIVE.exe"
SectionEnd

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCore} "Install the DEXRIVE desktop application, bundled Java runtime and required legal notices."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecMenu} "Add a DEXRIVE shortcut to your Start menu."
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} "Create an optional DEXRIVE desktop shortcut."
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "Uninstall"
  SetShellVarContext current
  Delete "$DESKTOP\DEXRIVE.lnk"
  Delete "$SMPROGRAMS\DEXRIVE\DEXRIVE.lnk"
  RMDir "$SMPROGRAMS\DEXRIVE"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\DEXRIVE"
  DeleteRegKey HKCU "Software\DEXRIVE"

  ; Never recursively delete the user-selected installation directory:
  ; it may contain unrelated documents.
  Delete "$INSTDIR\DEXRIVE.exe"
  Delete "$INSTDIR\LICENSE"
  Delete "$INSTDIR\NOTICE"
  Delete "$INSTDIR\BRAND_NOTICE.md"
  Delete "$INSTDIR\README.md"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\jre"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir "$INSTDIR"
SectionEnd
