#define MyAppName "Arma Render"
#define MyAppVersion "1.0"
#define MyAppPublisher "Arma Automotive Inc."
#define MyAppURL "https://www.armaautomotive.com"
#define MyAppExeName "ArmaRender_Native.exe"
#define MyAppAssocName MyAppName + " File"
#define MyAppAssocExt ".ads"
#define MyAppAssocKey StringChange(MyAppAssocName, " ", "") + MyAppAssocExt

[Setup]
AppId={{9A01E4C1-B612-4FA8-B2AA-3776AD636A06}}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
ChangesAssociations=yes
DisableProgramGroupPage=yes
; Remove the following line to run in administrative install mode (install for all users.)
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
OutputBaseFilename=ArmaRender_win_x64_Setup
SetupIconFile=favicon.ico
Compression=lzma
SolidCompression=yes
WizardStyle=modern


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkablealone

[Files]
Source: "..\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion

; If the other DLLs and executables are also in the root or a specific subfolder, adjust the paths accordingly
Source: "..\awt.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\fontmanager.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\freetype.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\java.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\javaaccessbridge.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\javajpeg.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\jawt.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\jsound.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\jvm.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\lcms.dll"; DestDir: "{app}"; Flags: ignoreversion   
Source: "favicon.ico"; DestDir: "{app}"; Flags: ignoreversion 


; NOTE: Don't use "Flags: ignoreversion" on any shared system files
[Registry]
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocExt}\OpenWithProgids"; ValueType: string; ValueName: "{#MyAppAssocKey}"; ValueData: ""; Flags: uninsdeletevalue
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}"; ValueType: string; ValueName: ""; ValueData: "{#MyAppAssocName}"; Flags: uninsdeletekey
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\{#MyAppExeName},0"
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocKey}\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""
Root: HKA; Subkey: "Software\Classes\Applications\{#MyAppExeName}\SupportedTypes"; ValueType: string; ValueName: ".myp"; ValueData: ""

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}" ; IconFilename: {app}\favicon.ico
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: {app}\favicon.ico

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
