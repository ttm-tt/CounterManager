#define Version '25.05.03'

[Setup]
AppName=ScoreBoardManager
AppVersion={#Version}
VersionInfoVersion={#Version}
; AppPublisher=Christoph Theis
DefaultDirName={autopf}\TTM\CounterManager
DefaultGroupName=TTM
OutputDir=.\Output
MinVersion= 0,6.1sp1
OutputBaseFilename=install
ArchitecturesInstallIn64BitMode=x64

; Sign installer
; SignTool=MS /d $qTTM Installer$q $f


[Languages]
Name: en; MessagesFile: compiler:Default.isl
Name: de; MessagesFile: compiler:Languages\German.isl 

[CustomMessages]
en.Language=en

de.Language=de
          
                                                                                                 
[Types]
Name: "client"; Description: "Scoreboard Manager for TTM"


[Components]
Name: "Client"; Description: "Scoreboard Manager for TTM"; Types: client; Flags: fixed


[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:";


[Dirs]
; Brauche ich, um die Berechtigungen zu setzen.
; Installation ist vom Admin, ausfuehren soll es aber ein non-admin
Name: {code:GetIniDir}; Permissions: authusers-modify


[Files]
Source: ".\dist\CounterManager.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\dist\*.dll"; DestDir: "{app}"; Flags: ignoreversion 
Source: ".\dist\lib\*.jar"; DestDir: "{app}\lib"; Excludes: "edtftpj*.jar"; Flags: ignoreversion
; Copy either the OSS edtftpj.jar or the commercial runtime
; Source: ".\lib\edtftpj.jar"; DestDir: "{app}\lib"; Flags:  ignoreversion
Source: "..\FtpClient\dist\FtpClient.jar"; DestDir: "{app}\lib"; Flags:  ignoreversion
Source: "..\FtpClient\lib\edtftpj-pro.jar"; DestDir: "{app}\lib"; Flags:  ignoreversion
Source: ".\dist\http\scripts\*"; DestDir: "{code:GetIniDir}\http\scripts"; Flags: ignoreversion   recursesubdirs
Source: ".\dist\http\display\*"; DestDir: "{code:GetIniDir}\http\display"; Excludes: ".\dist\http\display\scenes\*"; Flags: ignoreversion recursesubdirs
Source: ".\dist\http\monitor\*"; DestDir: "{code:GetIniDir}\http\monitor"; Flags: ignoreversion recursesubdirs
Source: ".\dist\http\counter\*"; DestDir: "{code:GetIniDir}\http\counter"; Flags: ignoreversion recursesubdirs
Source: ".\dist\http\ceremony\*"; DestDir: "{code:GetIniDir}\http\ceremony"; Flags: ignoreversion recursesubdirs
Source: ".\dist\http\flags\*"; DestDir: "{code:GetIniDir}\http\flags"; Flags: ignoreversion
; Source: ".\dist\http\fonts\*"; DestDir: "{code:GetIniDir}\http\fonts"; Flags: ignoreversion
Source: ".\dist\CounterManager.ico"; DestDir: "{app}"; Flags: ignoreversion
; Source: ".\changes.html"; DestDir: "{app}"; Flags: ignoreversion
Source: ".\3rdparty.html"; DestDir: "{app}"; Flags: ignoreversion
; Source: "..\TTM Manuals\src\CounterManagerGeneral\CounterManagerGeneral.pdf"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\TTM Manuals\src\CounterManagerVenue\CounterManagerVenue.pdf"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\TTM Manuals\src\LiveTickerInput\LiveTickerInput.pdf"; DestDir: "{app}"; Flags: ignoreversion


[INI]
Filename: {code:GetIniDir}\countermanager.ini; Section: Settings; Key: Language; String: {cm:Language}

[Registry]
; HKLM\Software\JavaSoft\Prefs should be created by JRE installer, but that does not always happen
Root: HKLM; Subkey: "Software\JavaSoft\Prefs"; Flags: noerror

[Icons]
Name: "{group}\ScoreBoardManager"; Filename: "{app}\lib\CounterManager.jar"; WorkingDir: "{app}";
Name: "{userdesktop}\ScoreBoardManager"; Filename: "{app}\CounterManager.jar"; WorkingDir: "{app}"; Tasks: desktopicon; IconFilename: "{app}\CounterManager.ico";


[Code]
{Looks for the directory to put the ini file in.}
{First we try the current directory.}
{Then we look for a directory named TTM in the following places:}
{ - ALLUSERSPROFILE (\ProgramData)}
{ - APPDATA (\Users\<user>\AppData\Roaming)}
{ - LOCALAPPDATA (\Users\<user>\AppData\Local)}
{Default: if Admin, put it in ALLUSERSPROFILE, else put in in LOCALAPPDATA}
function GetIniDir(Param: String): String;
begin
  if (FileExists(ExpandConstant( '{app}\countermanager.ini' ))) then
  begin
    Result := ExpandConstant('{app}');
  end
  else if ( isAdmin OR FileExists(ExpandConstant( '{commonappdata}\TTM' )) ) then
  begin
    CreateDir(ExpandConstant('{commonappdata}\TTM\CounterManager'));
    Result := ExpandConstant('{commonappdata}\TTM\CounterManager');
  end
  else if ( FileExists(ExpandConstant( '{userappdata}\TTM' )) ) then
  begin
    CreateDir(ExpandConstant('{userappdata}\TTM\CounterManager'));
    Result := ExpandConstant('{userappdata}\TTM\CounterManager');
  end
  else
  begin
    CreateDir(ExpandConstant('{localappdata}\TTM\CounterManager'));
    Result := ExpandConstant('{localappdata}\TTM\CounterManager');
  end
end;


(* looks for JRE version in Registry *)
function getJREVersion(): String;
var
	jreVersion: String;
begin
	jreVersion := '';
  if IsWin64 then begin
	  RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JRE', 'CurrentVersion', jreVersion);
  end
  else begin
  	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JRE', 'CurrentVersion', jreVersion);
  end;
	Result := jreVersion;
end;

(* looks for JDK version, in Registry *)
function getJDKVersion(): String;
var
	jdkVersion: String;
begin
	jdkVersion := '';
  if IsWin64 then begin
	  RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', jdkVersion);
  end
  else begin
  	RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK', 'CurrentVersion', jdkVersion);
  end;
	Result := jdkVersion;
end;

(* Called on setup startup *)
function InitializeSetup(): Boolean;
var
	javaVersion: String;
begin
	javaVersion := getJDKVersion();
  if javaVersion = '' then begin
    javaVersion := getJREVersion();
  end;

	if javaVersion >= '11' then begin
		(* MsgBox('Found java version' + javaVersion, mbInformation, MB_OK); *)
		Result := true;
	end
	else begin
		MsgBox('Setup is unable to find a Java Development Kit or Java Runtime 11, or higher, installed.' + #13 +
			     'You must have installed at least JDK or JRE, 11 or higher to continue setup.' + #13 +
			     'Please install one from https://AdoptOpenJDK.com and then run this setup again.', mbInformation, MB_OK);
		Result := true;
	end;
		
  Result := true;
end;

