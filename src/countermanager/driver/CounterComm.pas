/*
(************************************************************)
(*                                                          *)
(*                       CounterComm                        *)
(*                       Version 1.0                        *)
(*                                                          *)
(*                    Exported functions                    *)
(*             Part of counter communication DLL            *)
(*                  (C) 2006 by Patrik Graf                 *)
(*                                                          *)
(************************************************************)

unit UnitExports;

interface

uses Forms, Classes, CPDrv, SysUtils, StrUtils, StrCon, Dialogs, UnitDiagnostics;

type
  /// <summary>Defines a counter button.</summary>
  TCounterButton = (cbNone, cbReset, cbUndo, cbGamestart, cbScoreLeftUp,
                    cbSoreLeftDown, cbScoreRightUp, cbScoreRightDown,
                    cbClockStartStop, cbSetLeftUp, cbSetLeftDown,
                    cbSetRightUp, cbSetRightDown);

  // ChT BEGIN
  // TGameMode = (gmAfterReset, gmWarmup, gmRunning, gmEnd);
  TGameMode = (gmAfterReset, gmRunning, gmWarmup, gmEnd);
  TTimeMode = (NONE, PREPARE, BREAK, MATCH, TIMEOUT, INJURY);

  // ChT END
  /// <summary>Pointer to a counter data structure.</summary>
  PCounterData = ^TCounterData;
  /// <summary>Defines the data received from a counter.</summary>
  TCounterData = record
    PagingInLastSet: Boolean;
    // TimegamePrepareMode: Boolean;
    Alert: Boolean;
    TimegameMode: Boolean;
    Service: Byte;
    GameMode: TGameMode;
    AbandonOrAbort: Boolean;
    TimegameBlock: Boolean;
    GameNr: Cardinal;
    // ChT BEGIN
    // PlayerNrLeft: Byte;
    PlayerNrLeft: Cardinal;
    PlayerNrRight: Cardinal;
    // ChT END
    // ChT BEGIN
    //   Version 1.2
    MaxSets: Cardinal;
    TimeMode: TTimeMode;
    TimeoutOrInjured: Byte;
    TimeStopped: Boolean;
    Time: Cardinal;
    // ChT END
    SetsRight: Byte;
    SetsLeft: Byte;
    SetHistory: PChar;
  end;

  /// <summary>Defines the state after the reset button was pressed.</summary>
  TAfterReset = (arWinSets, arMinus, arBlank, arCounterNr);

  /// <summary>Pointer to a counter configuration structure.</summary>
  PCounterConfig = ^TCounterConfig;
  /// <summary>Defines the configuration of a counter.</summary>
  TCounterConfig = record
    SetTime: PChar;
    SetTimeButtonPress: Boolean;
    TimeoutTime: PChar;
    TimeoutButtonPress: Boolean;
    WarmupTime: PChar;
    WarmupButtonPress: Boolean;
    SetBreakTime: PChar;
    SetBreakButtonPress: Boolean;
    VERLETZUNGSPAUSE: PChar;
    VERLETZUNGSPAUSEButtonPress: Boolean;
    MaxSets: Cardinal;
    MaxPoints: Cardinal;
    SetPointsOffset: Cardinal;
    SideSwitchLastSet: Cardinal;
    TimeStopAt: Cardinal;
    AUFSCHLAGWECHSEL: Cardinal;
    ENTPRELLUNG: Cardinal;
    AfterReset: TAfterReset;
    LockReset: Boolean;
    LockKeys: Boolean;
    AfterResetStateOff: Boolean;
    DisplayEAModus: Boolean;
    DisplayWarmupTime: Boolean;
    DisplaySetTime: Boolean;
    DisplayTimeoutTime: Boolean;
    DisplaySetBreakTime: Boolean;
    DisplayVERLETZUNGSPAUSE: Boolean;
    DisplayMatchEnd: Boolean;
    ShowTimeoutAsOn: Boolean;
  end;

  /// <summary>Pointer to a digit data structure.</summary>
  PDigitsData = ^TDigitsData;
  /// <summary>Defines digit data from a counter.</summary>
  TDigitsData = record
    TimeLeftHigh: Byte;
    TimeLeftLow: Byte;
    TimeRightHigh: Byte;
    TimeRightLow: Byte;
    SetLeft: Byte;
    SetRight: Byte;
    ScoreLeftHigh: Byte;
    ScoreLeftLow: Byte;
    ScoreRightHigh: Byte;
    ScoreRightLow: Byte;
  end;

  /// <summary>Pointer to a callback procedures structure.</summary>
  PCallbackProcs = ^TCallbackProcs;
  /// <summary>Defines a callback procedures structure.</summary>
  TCallbackProcs = record
    VersionCallback: procedure(Handle: THandle; Counter: Byte; Version: PChar); stdcall;
    GetDateTimeCallback: procedure(Handle: THandle; Counter: Byte; DateTime: PChar); stdcall;
    SetDateTimeCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    GetDataCallback: procedure(Handle: THandle; Counter: Byte; Data: PCounterData); stdcall;
    GetConfigCallback: procedure(Handle: THandle; Counter: Byte; Config: PCounterConfig); stdcall;
    SetConfigCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    ResetCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    PushButtonCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    SwitchCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    SetDigitsCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    SetPlayerNumbersCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    BaudrateCallback: procedure(Handle: THandle; Result: Boolean); stdcall;
    SetResultsCallback: procedure(Handle: THandle; Counter: Byte; Data: PChar); stdcall;
    SetGameNumberCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
    GetGameNumberCallback: procedure(Handle: THandle; Counter: Byte; Result: Cardinal); stdcall;
    SerialNumberCallback: procedure(Handle: THandle; Counter: Byte; Result: PChar); stdcall;
    OnErrorCallback: procedure(Handle: THandle; Counter: Byte; ErrorCode: Cardinal); stdcall;
// ChT BEGIN
    GetPlayerNumbersCallback: procedure(Handle: THandle; Counter: Byte; PlayerNrLeft, PlayerNrRight: Cardinal); stdcall;
    ResetAlertCallback: procedure(Handle: THandle; Counter: Byte; Result: Boolean); stdcall;
// ChT END
  end;

  /// <summary>Counter command enumeration.</summary>
  TCommand = (tcVersion, tcGetDateTime, tcSetDateTime, tcGetData, tcGetConfig,
              tcSetConfig, tcReset, tcPushButton, tcSwitchPlayers, tcSetDigits,
              tcSetPlayers, tcGetDataBroadcast, tcBaudrate, tcGetSetResults, tcSetGameNr,
              tcGetGameNr, tcGetSerial,
// ChT BEGIN
              tcGetPlayers, tcSetGameData, tcSetCodeNr, tcResetAlert
// ChT END
              );

  /// <summary>Dummy class for serial driver receive event.</summary>
  TDummy = class(TObject)
  public
    /// <summary>Event handling routine for serial driver.</summary>
    /// <param name="Sender">Points to an object which raised this event.</param>
    /// <param name="DataPtr">Pointer to the received data.</param>
    /// <param name="DataSize">Size in bytes of the received data.</param>
    procedure SerialDriverReceiveData(Sender: TObject; DataPtr: Pointer; DataSize: Cardinal);
  end;

  /// <summary>Gets the driver version.</summary>
  /// <returns>The driver version of this driver.</returns>
  function GetDriverVersion(): PChar; stdcall;

  /// <summary>Prepares and opens a connection to communicate with counters.</summary>
  /// <param name="ComPort">Portnumber where is mounted the radio module for communication.</param>
  /// <param name="Baud">Baudrate which is used by the counters.</param>
  /// <returns>The handle for this new connection.</returns>
  function OpenConnection(ComPort: Byte; Baud: TBaudrate): THandle; stdcall;

  /// <summary>Gets the version information from a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure GetVersion(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Gets the current date and time from a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  /// <param name="FormatStr">The format string for the date and the time.</param>
  procedure GetDateTime(Handle: THandle; Counter: Byte; FormatStr: PChar); stdcall;

  /// <summary>Sets the date and the time of a counter to PC date and time.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter which should receive this information.</param>
  procedure SetDateTime(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Gets all data from a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure GetData(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Gets the configuration of a counter</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure GetCounterConfig(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Sets the configuration of a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  /// <param name="Config">A pointer to the new configuration structure.</param>
  procedure SetCounterConfig(Handle: THandle; Counter: Byte; Config: PCounterConfig); stdcall;

  /// <summary>Resets a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure ResetCounter(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>This will simulate that a button has pushed on the counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  /// <param name="Button">The button(s) you want to push (combine with bitwise or).</param>
  procedure PushCounterButton(Handle: THandle; Counter: Byte; Button: Byte); stdcall;

  /// <summary>Switches the player numbers on a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure SwitchPlayerNumbers(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Sets the data of the digits on a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  /// <param name="DigitsData">A pointer to a digits data structure.</param>
  procedure SetDigits(Handle: THandle; Counter: Byte; DigitsData: PDigitsData); stdcall;

  /// <summary>Sets the player numbers on a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  /// <param name="Player1">The number of the left player.</param>
  /// <param name="Player2">The number of the right player.</param>
  procedure SetPlayerNumbers(Handle: THandle; Counter: Byte; Player1, Player2: Cardinal); stdcall;

  /// <summary>Gets the data from all counters</summary>
  /// <param name="Handle">Connection handle.</param>
  procedure GetDataBroadcast(Handle: THandle); stdcall;

  /// <summary>Sets the baudrate of the connection and all counters.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Baudrate">The new baudrate.</param>
  procedure SetBaudrate(Handle: THandle; Baudrate: TBaudrate); stdcall;

  /// <summary>Gets the results from all sets of a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure GetSetResults(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Sets the game number of a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  /// <param name="GameNr">The game number.</param>
  procedure SetGameNumber(Handle: THandle; Counter: Byte; GameNr: Word); stdcall;

  /// <summary>Gets the game number from a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure GetGameNumber(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Gets the serial number from a counter.</summary>
  /// <param name="Handle">Connection handle.</param>
  /// <param name="Counter">The counter from which you want this information.</param>
  procedure GetSerialNumber(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Closes a connection and frees the allocated memory.</summary>
  /// <param name="Handle">Connection handle.</param>
  procedure CloseConnection(Handle: THandle); stdcall;

  /// <summary>Sets the callback procedures.</summary>
  /// <param name="CallbackProcs">A pointer to a structure with callback procedures.</param>
  procedure SetCallbackProcs(CallbackProcs: PCallbackProcs); stdcall;

  /// <summary>Gets the error message by an error code.</summary>
  /// <param name="ErrorCode">A error code.</param>
  /// <returns>The message for an error code.</returns>
  function GetErrorMessage(ErrorCode: Cardinal): PChar; stdcall;

  /// <summary>Gets the error message by an error code.</summary>
  /// <param name="TraceType">A tracetype.</param>
  procedure ActivateTrace(TraceType: TTraceType); stdcall;

  // ChT BEGIN
  /// <summary>Get the player numbers.</summary>
  procedure GetPlayerNumbers(Handle: THandle; Counter: Byte); stdcall;

  /// <summary>Sets the game data.</summary>
  procedure SetGameData(Handle: THandle; Counter: Byte; MaxSets, GameNr,
                        PlayerNrLeft, PlayerNrRight : Cardinal); stdcall;

  /// <sumary>Sets the code.</summary>
  procedure SetCodeNr(CodeNr: Cardinal); stdcall;

  /// <summary>Reset the alert bit.</summary>
  procedure ResetAlert(Handle: THandle; Counter: Byte); stdcall;
  // ChT END

  exports
    GetDriverVersion,
    OpenConnection,
    CloseConnection,
    GetVersion,
    GetDateTime,
    SetDateTime,
    GetData,
    GetCounterConfig,
    SetCounterConfig,
    ResetCounter,
    PushCounterButton,
    SwitchPlayerNumbers,
    SetDigits,
    SetPlayerNumbers,
    GetDataBroadcast,
    SetBaudrate,
    GetSetResults,
    SetGameNumber,
    GetGameNumber,
    GetSerialNumber,
    SetCallbackProcs,
    GetErrorMessage,
    ActivateTrace,
    SetGameData,
    SetCodeNr,
    ResetAlert;

implementation

type
  TPlayerNumbers = record
    Player1: String;
    Player2: String;
  end;

  TGameData = record
    MaxSets: Byte;
    GameNr : Cardinal;
    PlayerNrLeft : Cardinal;
    PlayerNrRight : Cardinal;
  end;

  PGameData = ^TGameData;

const
  /// <summary>The version of this driver DLL.</summary>
  GTS_DRIVER_VERSION: PChar = 'V 1.0.2' + #0;

  /// <summary>Defines a PC to counter connection.</summary>
  PC_TO_COUNTER: Byte = $21;

  /// <summary>Defines a PC to counter connection with broadcast sending.</summary>
  PC_TO_COUNTER_BROADCAST: Byte = $41;

  {$REGION 'Data length constants'}

  /// <summary>The default data lengt for counter data.</summary>
  DEFAULT_DATA_LENGTH: Byte = 6;

  /// <summary>The length for data when sending the date and time information to a counter.</summary>
  SET_DATETIME_DATA_LENGTH: Byte = 13;

  /// <summary>The length for data when sending a configuration to a counter.</summary>
  SET_CONFIG_DATA_LENGTH: Byte = 26;

  /// <summary>The length for data when sending digits data.</summary>
  SET_DIGITS_DATA_LENGTH: Byte = 16;

  /// <summary>The length for data when setting the baudrate.</summary>
  SET_BAUDRATE_DATA_LENGTH: Byte = 7;

  /// <summary>The length for data when setting the game number.</summary>
  SET_GAME_NUMBER_DATA_LENGTH: Byte = 8;

  /// <summary>The length for data when setting the player numbers.</summary>
  SET_PLAYERS_DATA_LENGTH: Byte = 10;

  /// <summary>The length for data when setting the game data.</summary>
  SET_GAME_DATA_LENGTH: Byte = 13;

  {$ENDREGION}

  {$REGION 'Error constants'}

  /// <summary>Error code for no error.</summary>
  ERROR_NO_ERROR: Cardinal = 0;

  /// <summary>Error code for invalid data packages.</summary>
  ERROR_INVALID_DATA_PACKAGE: Cardinal = 1;

  /// <summary>Error code for not supported baudrates.</summary>
  ERROR_BAUDRATE_NOT_SUPPORTED: Cardinal = 2;

  /// <summary>Error code for not supported tracetypes.</summary>
  ERROR_TRACE_TYPE_NOT_SUPPORTED: Cardinal = 5;

  {$ENDREGION}

var
  DriverList: TList = nil;

  DriverBuffer: TStrings = nil;

  gCallbackProcs: PCallbackProcs;

  NilPointer: Pointer = nil;

  CurrentFormatStr: String = '';

  Dummy: TDummy = nil;

  Trace: TTrace = nil;


/// <summary>Sends data to a counter.</summary>
/// <param name="Handle">Connection handle.</param>
/// <param name="Counter">The counter to send to.</param>
/// <param name="Command">The command to send.</param>
/// <param name="Data">The data for the command.</param>
procedure SendMsgData(Handle: THandle; Counter: Byte; Command: TCommand; var Data); forward;

/// <summary>Prepares the header for sending.</summary>
function PrepareHeader(Codierung, Codestring: ShortString): ShortString; stdcall; external 'endecrypt.dll';

/// <summary>Decodes the received header.</summary>
function DecodeHeader(Codierung, Codestring: ShortString): ShortString; stdcall; external 'endecrypt.dll';

// <summary>Inserts the XOR byte before sendig.</summary>
// function InsertXORByte(XORSendeString: ShortString): ShortString; stdcall; external 'endecrypt.dll';

/// <summary>Checks the XOR byte.</summary>
function CheckXORByte(XOREmpfangsString: ShortString): Boolean; stdcall; external 'endecrypt.dll';

/// <summary>Writes a trace message.</summary>
procedure WriteTrace(Msg: String); forward;

/// <summary>Writes a trace message for entering and leaving functions</summary>
procedure WriteTraceInOut(aType: String; FuncName: String; Params: array of Variant); forward;

/// <summary>Writes counter config to the trace.</summary>
procedure WriteTraceConfig(Config: PCounterConfig); forward;

/// <summary>Writes digits data to the trace.</summary>
procedure WriteTraceDigitsData(DigitsData: PDigitsData); forward;

/// <summary>Writes counter data to the trace.</summary>
procedure WriteTraceCounterData(CounterData: PCounterData); forward;

//******************************************************************************
function GetDriverVersion(): PChar; stdcall;
begin
 WriteTraceInOut('CALL', 'GetDriverVersion', []);
 Result := GTS_DRIVER_VERSION;
 WriteTraceInOut('RET', 'GetDriverVersion', []);
end;

function OpenConnection(ComPort: Byte; Baud: TBaudrate): THandle; stdcall;
begin
 WriteTraceInOut('CALL', 'OpenConnection', [ComPort, Baud]);
 if DriverList = nil then DriverList := TList.Create;
 if DriverBuffer = nil then DriverBuffer := TStringList.Create;
 DriverList.Add(TCommPortDriver.Create(nil));
 DriverBuffer.Add('');
 with TCommPortDriver(DriverList[DriverList.Count - 1]) do begin
  BaudRate := Baud;
  HwFlow := hfNONE;
  EnableDTROnOpen := false;
  InputTimeout := 200;
  OutputTimeout := 200;
  Port := pnCustom;
  if Dummy = nil then Dummy := TDummy.Create;
  OnReceiveData := Dummy.SerialDriverReceiveData;
  PortName := '\\.\COM' + IntToStr(ComPort);
  Connect;
  if not Connected then begin
   WriteTrace('ERROR: Can''t connect!');
   CloseConnection(THandle(DriverList[DriverList.Count - 1]));
   Result := 0;
  end
  else Result := THandle(DriverList[DriverList.Count - 1]);
 end;
 WriteTraceInOut('RET', 'OpenConnection', []);
end;

procedure GetVersion(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetVersion', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcVersion, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetVersion', []);
end;

procedure GetDateTime(Handle: THandle; Counter: Byte; FormatStr: PChar); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetDateTime', [Handle, Counter, string(FormatStr)]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  CurrentFormatStr := string(FormatStr);
  SendMsgData(Handle, Counter, tcGetDateTime, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetDateTime', []);
end;

procedure SetDateTime(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
    DateTime: TDateTime;
begin
 WriteTraceInOut('CALL', 'SetDateTime', [Handle, Counter]);
 DateTime := Date + Time;
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcSetDateTime, DateTime);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'SetDateTime', []);
end;

procedure GetData(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetData', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcGetData, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetData', []);
end;

procedure GetCounterConfig(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetCounterConfig', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcGetConfig, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetCounterConfig', []);
end;

procedure SetCounterConfig(Handle: THandle; Counter: Byte; Config: PCounterConfig); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'SetCounterConfig', [Handle, Counter, '0x' + IntToHex(Cardinal(Config), 8)]);
 WriteTraceConfig(Config);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcSetConfig, Config);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'SetCounterConfig', []);
end;

procedure ResetCounter(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'ResetCounter', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcReset, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'ResetCounter', []);
end;

procedure PushCounterButton(Handle: THandle; Counter: Byte; Button: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'PushCounterButton', [Handle, Counter, Button]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcPushButton, Button);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'PushCounterButton', []);
end;

procedure SwitchPlayerNumbers(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'SwitchPlayerNumbers', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcSwitchPlayers, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'SwitchPlayerNumbers', []);
end;

procedure SetDigits(Handle: THandle; Counter: Byte; DigitsData: PDigitsData); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'SetDigits', [Handle, Counter, '0x' + IntToHex(Cardinal(DigitsData), 8)]);
 WriteTraceDigitsData(DigitsData);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcSetDigits, DigitsData);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'SetDigits', []);
end;

procedure SetPlayerNumbers(Handle: THandle; Counter: Byte; Player1, Player2: Cardinal); stdcall;
var Index: Integer;
    Players: Cardinal;
begin
 WriteTraceInOut('CALL', 'SetPlayerNumbers', [Handle, Counter, Player1, Player2]);
 if (Player1 <= 9999) and (Player2 <= 9999) then begin
  Index := DriverList.IndexOf(Pointer(Handle));
  if Index <> -1 then begin
   Players := (Player1 shl 16) or Player2;
   SendMsgData(Handle, Counter, tcSetPlayers, Players);
  end
  else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 4);
 WriteTraceInOut('RET', 'SetPlayerNumbers', []);
end;

procedure GetDataBroadcast(Handle: THandle); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetDataBroadcast', [Handle]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, 0, tcGetDataBroadcast, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, 0, 3);
 WriteTraceInOut('RET', 'GetDataBroadcast', []);
end;

procedure SetBaudrate(Handle: THandle; Baudrate: TBaudrate); stdcall;
var Index: Integer;
    Temp: TBaudrate;
begin
 WriteTraceInOut('CALL', 'SetBaudrate', [Handle, Baudrate]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  Temp := Baudrate;
  SendMsgData(Handle, 0, tcBaudrate, Temp);
 end
 else gCallbackProcs.OnErrorCallback(Handle, 0, 3);
 WriteTraceInOut('RET', 'SetBaudrate', []);
end;

procedure GetSetResults(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetSetResults', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcGetSetResults, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetSetResults', []);
end;

procedure SetGameNumber(Handle: THandle; Counter: Byte; GameNr: Word); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'SetGameNumber', [Handle, Counter, GameNr]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcSetGameNr, GameNr);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'SetGameNumber', []);
end;

procedure GetGameNumber(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetGameNumber', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcGetGameNr, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetGameNumber', []);
end;

procedure GetSerialNumber(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'GetSerialNumber', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcGetSerial, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'GetSerialNumber', []);
end;

procedure CloseConnection(Handle: THandle); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'CloseConnection', [Handle]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  with TCommPortDriver(DriverList[Index]) do begin
   Disconnect; Free;
  end;
  DriverList.Delete(Index);
  DriverBuffer.Delete(Index);
  if DriverList.Count = 0 then FreeAndNil(DriverList);
  if DriverBuffer.Count = 0 then FreeAndNil(DriverBuffer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, 0, 3);
 WriteTraceInOut('RET', 'CloseConnection', []);
end;

procedure SetCallbackProcs(CallbackProcs: PCallbackProcs); stdcall;
begin
 WriteTraceInOut('CALL', 'SetCallbackProcs', ['0x' + IntToHex(Cardinal(CallbackProcs), 8)]);
 gCallbackProcs := CallbackProcs;
 WriteTraceInOut('RET', 'SetCallbackProcs', []);
end;

function GetErrorMessage(ErrorCode: Cardinal): PChar; stdcall;
var ErrorStr: String;
begin
 WriteTraceInOut('CALL', 'GetErrorMessage', [ErrorCode]);
 case ErrorCode of
  0: ErrorStr := 'No Error.';
  1: ErrorStr := 'The data package from the counter was invalid.';
  2: ErrorStr := 'This baudrate is not supported. Use 1200bd or 9600bd.';
  3: ErrorStr := 'Invalid driver handle.';
  4: ErrorStr := 'Invalid player numbers.';
 end;
 GetMem(Result, Length(ErrorStr) + 1);
 Result := StrPCopy(Result, ErrorStr);
 WriteTraceInOut('RET', 'GetErrorMessage', []);
end;

procedure ActivateTrace(TraceType: TTraceType); stdcall;
begin
 if Trace = nil then Trace := TTrace.Create;
 Trace.Buffersize := 10240;
 Application.OnException := Trace.ExceptionEvent;
 case Tracetype of
   ttEventLog: gCallbackProcs.OnErrorCallback(0, 0, ERROR_TRACE_TYPE_NOT_SUPPORTED);
   ttConsole: Trace.AddTrace(TraceType);
   ttFile: Trace.AddTrace(TraceType, ExtractFilePath(ParamStr(0)) + 'countercomm.trc');
 end;
 Trace.WriteLn('GTS-Electronic - Digital Scoreboard - Driver trace - © 2005 by Patrik Graf');
 Trace.WriteLn('');
 WriteTrace('Tracelistener created. ');
 WriteTraceInOut('RET', 'ActivateTrace', []);
end;

// ChT BEGIN
procedure GetPlayerNumbers(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
  WriteTraceInOut('CALL', 'GetPlayerNumbers', [Handle, Counter]);
  Index := DriverList.IndexOf(Pointer(Handle));
  if Index <> -1 then begin
    SendMsgData(Handle, Counter, tcGetPlayers, NilPointer);
  end
  else
    gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
end;

procedure SetGameData(Handle: THandle; Counter: Byte;
                      MaxSets, GameNr, PlayerNrLeft, PlayerNrRight : Cardinal); stdcall;
var GameData: TGameData; Index: Integer;
begin
  if ( ((PlayerNrLeft <= 9999) or (PlayerNrLeft >= $FFFE)) and
       ((PlayerNrRight <= 9999) or (PlayerNrRight >= $FFFE)) and
       ((GameNr <= 9999) or (GameNr >= $8000)) ) then
  begin
    GameData.MaxSets := MaxSets;
    GameData.GameNr := GameNr;
    GameData.PlayerNrLeft := PlayerNrLeft;
    GameData.PlayerNrRight := PlayerNrRight;

    WriteTraceInOut('CALL', 'SetGameData', [Handle, Counter]);
    Index := DriverList.IndexOf(Pointer(Handle));
    if Index <> -1 then begin
      SendMsgData(Handle, Counter, tcSetGameData, GameData);
    end
    else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
    WriteTraceInOut('RET', 'SetGameData', []);
  end
  else gCallbackProcs.OnErrorCallback(Handle, Counter, 4);
end;

procedure SetCodeNr(CodeNr: Cardinal); stdcall;
begin
end;


procedure ResetAlert(Handle: THandle; Counter: Byte); stdcall;
var Index: Integer;
begin
 WriteTraceInOut('CALL', 'ResetAlert', [Handle, Counter]);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  SendMsgData(Handle, Counter, tcResetAlert, NilPointer);
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'ResetAlert', []);
end;

// ChT END

procedure SendMsgData(Handle: THandle; Counter: Byte; Command: TCommand; var Data);
var Sender, KeyPointer, StrLength, XORByte: Byte;
    Header, DataStr, PCDate, PCTime: String;
    Index: Integer;
    ConfigFlags: Cardinal;       // Byte 24
    DisplayFlags: Cardinal;      // Byte 26
    CounterConfig: PCounterConfig;
    DigitsData: PDigitsData;
    Baudrate: TBaudrate;
    GameNumber: Word;
    PlayerNumbers: TPlayerNumbers;
    GameData: TGameData;
begin
 WriteTraceInOut('CALL', 'SendMessageData', [Handle, Counter, Command, '0x' + IntToHex(Cardinal(@Data), 8)]);
 Sender := PC_TO_COUNTER; DataStr := '';
 KeyPointer := Random(251); StrLength := DEFAULT_DATA_LENGTH; XORByte := 0;
 case Command of
   tcSetDateTime:
     begin
      StrLength := SET_DATETIME_DATA_LENGTH;
      PCDate := DateToStr(Date);
      PCTime := TimeToStr(Time + StrToTime('00:00:01'));
      DataStr := Chr(StrToInt(Copy(PCTime, 7, 2))) + Chr(StrToInt(Copy(PCTime, 4, 2))) + Chr(StrToInt(Copy(PCTime, 1, 2))) +
                 Chr(StrToInt(Copy(PCDate, 1, 2))) + Chr(StrToInt(Copy(PCDate, 4, 2))) + Chr(StrToInt(Copy(PCDate, 9, 2))) +
                 Chr(DayOfWeek(Date));
     end;
   tcSetConfig:
     begin
       StrLength := SET_CONFIG_DATA_LENGTH;
       ConfigFlags := 0;
       CounterConfig := PCounterConfig(Data);
       if not CounterConfig^.SetTimeButtonPress Then ConfigFlags := ConfigFlags or 1;
       if not CounterConfig^.TimeoutButtonPress then ConfigFlags := ConfigFlags or 2;
       if not CounterConfig^.WarmupButtonPress then ConfigFlags := ConfigFlags or 4;
       if not CounterConfig^.SetBreakButtonPress then ConfigFlags := ConfigFlags or 8;
       if not CounterConfig^.VERLETZUNGSPAUSEButtonPress then ConfigFlags := ConfigFlags or $10;
       if CounterConfig^.LockKeys then ConfigFlags := ConfigFlags or $20;
       if CounterConfig^.LockReset then ConfigFlags := ConfigFlags or $40;
       if CounterConfig^.AfterResetStateOff then ConfigFlags := ConfigFlags or $80;

       DisplayFlags := 0;
       if CounterConfig^.DisplayEAModus then DisplayFlags := DisplayFlags or $1;
       if CounterConfig^.DisplayWarmupTime then DisplayFlags := DisplayFlags or $2;
       if CounterConfig^.DisplaySetTime then DisplayFlags := DisplayFlags or $4;
       if CounterConfig^.DisplayTimeoutTime then DisplayFlags := DisplayFlags or $8;
       if CounterConfig^.DisplaySetBreakTime then DisplayFlags := DisplayFlags or $10;
       if CounterConfig^.DisplayVERLETZUNGSPAUSE then DisplayFlags := DisplayFlags or $20;
       if CounterConfig^.DisplayMatchEnd then DisplayFlags := DisplayFlags or $40;
       if CounterConfig^.ShowTimeoutAsOn then DisplayFlags := DisplayFlags or $80;

       DataStr := Chr(StrToInt(Copy(CounterConfig^.SetTime, 4, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.SetTime, 1, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.TimeoutTime, 4, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.TimeoutTime, 1, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.WarmupTime, 4, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.WarmupTime, 1, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.SetBreakTime, 4, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.SetBreakTime, 1, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.VERLETZUNGSPAUSE, 4, 2))) +
                  Chr(StrToInt(Copy(CounterConfig^.VERLETZUNGSPAUSE, 1, 2))) +
                  Chr(CounterConfig^.MaxSets) +
                  Chr(CounterConfig^.MaxPoints) +
                  Chr(CounterConfig^.SetPointsOffset) +
                  Chr(CounterConfig^.SideSwitchLastSet) +
                  Chr(CounterConfig^.TimeStopAt) +
                  Chr(CounterConfig^.AUFSCHLAGWECHSEL) +
                  Chr(CounterConfig^.ENTPRELLUNG) +
                  Chr(ConfigFlags) +
                  Chr(Ord(CounterConfig^.AfterReset)) +
                  Chr(DisplayFlags);
     end;
   tcPushButton:
     begin
      //Buttons aus Data rausziehen und senden.
     end;
   tcSetDigits:
     begin
      StrLength := SET_DIGITS_DATA_LENGTH;
      DigitsData := PDigitsData(Data);
      DataStr := Chr(DigitsData^.TimeRightLow)+Chr(DigitsData^.TimeRightHigh) +
                 Chr(DigitsData^.TimeLeftLow)+Chr(DigitsData^.TimeLeftHigh) +
                 Chr(DigitsData^.ScoreRightLow)+Chr(DigitsData^.ScoreRightHigh) +
                 Chr(DigitsData^.TimeLeftLow)+Chr(DigitsData^.TimeLeftHigh) +
                 Chr(DigitsData^.SetRight)+Chr(DigitsData^.SetLeft);
     end;
   tcSetPlayers:
     begin
      StrLength := SET_PLAYERS_DATA_LENGTH;
      PlayerNumbers.Player1 := sc.Str(Cardinal(Data) shr 16, 4);
      PlayerNumbers.Player2 := sc.Str(Cardinal(Data) and 65535, 4);
      DataStr := Chr(StrToInt(Copy(PlayerNumbers.Player1, 3, 2))) + Chr(StrToInt(Copy(PlayerNumbers.Player1, 1, 2))) +
                 Chr(StrToInt(Copy(PlayerNumbers.Player2, 3, 2))) + Chr(StrToInt(Copy(PlayerNumbers.Player2, 1, 2)));
     end;
   tcBaudrate:
     begin
      StrLength := SET_BAUDRATE_DATA_LENGTH;
      Baudrate := TBaudrate(Data);
      case Baudrate of
        br1200: DataStr := Chr(1);
        br9600: DataStr := Chr(2);
      else
        gCallbackProcs.OnErrorCallback(Handle, Counter, ERROR_BAUDRATE_NOT_SUPPORTED);
      end;
     end;
   tcSetGameNr:
     begin
      StrLength := SET_GAME_NUMBER_DATA_LENGTH;
      GameNumber := Word(Data);
      if (GameNumber >= $8000) then
        DataStr := Chr(GameNumber and $FF) + Chr(GameNumber shl 8)
      else
        DataStr := Chr(GameNumber mod 100) + Chr(GameNumber div 100);
     end;
   tcGetDataBroadcast:
     begin
      Sender := PC_TO_COUNTER_BROADCAST;
     end;
// ChT BEGIN
   tcSetGameData:
     begin
       StrLength := SET_GAME_DATA_LENGTH;
       GameData := TGameData(Data);

       DataStr := Chr(GameData.MaxSets);

       if (GameData.GameNr >= $8000) then
         DataStr := DataStr + Chr(GameData.GameNr and $FF) + Chr(GameData.GameNr shl 8)
       else
         DataStr := DataStr + Chr(GameData.GameNr mod 100) + Chr(GameData.GameNr div 100);

       if (GameData.PlayerNrLeft >= $FFFE) then
         DataStr := DataStr +
                    Chr(GameData.PlayerNrLeft and $FF) +
                    Chr(GameData.PlayerNrLeft shr 8)
       else
         DataStr := DataStr +
                    Chr(GameData.PlayerNrLeft mod 100) +
                    Chr(GameData.PlayerNrLeft div 100);

       if (GameData.PlayerNrRight >= $FFFE) then
         DataStr := DataStr +
                    Chr(GameData.PlayerNrRight and $FF) +
                    Chr(GameData.PlayerNrRight shr 8)
       else
         DataStr := DataStr +
                    Chr(GameData.PlayerNrRight mod 100) +
                    Chr(GameData.PlayerNrRight div 100);
     end;
// ChT END
 end;
 Header := Chr(Sender) + Chr(Counter) + Chr(KeyPointer) + Chr(StrLength) + Chr(XORByte) + Chr(Ord(Command) + 10);
 Index := DriverList.IndexOf(Pointer(Handle));
 if Index <> -1 then begin
  // DriverBuffer[Index] := '';
  with TCommPortDriver(DriverList[Index]) do begin
   Sendstring(PrepareHeader('0', (Header + DataStr)));
   // Sendstring(PrepareHeader('', InsertXORByte((Header + DataStr))));
   // SendString(Chr($EE) + Chr($81) + Chr($57) + Chr($06) + Chr($3E) + Chr($F9));
  end;
 end
 else gCallbackProcs.OnErrorCallback(Handle, Counter, 3);
 WriteTraceInOut('RET', 'SendMessageData', []);
end;

procedure WriteTrace(Msg: String);
begin
 if Trace <> nil then begin
  Trace.Write('[' + FormatDateTime('dd.mm.yyyy hh.nn.ss', Date + Time) + '] ');
  Trace.WriteLn(Msg);
 end;
end;

procedure WriteTraceInOut(aType: String; FuncName: String; Params: array of Variant);
var Str: String;
    i: Integer;
begin
 Str := aType + ' ' + FuncName;
 if (Trim(aType) = 'CALL') or (Trim(aType) = 'CALLBACK') then begin
  Str := Str + '(';
  for i := 0 to High(Params) do begin
   if i = 0 then Str := Str + string(Params[i])
   else Str := Str + ', ' + string(Params[i])
  end;
  Str := Str + ')';
 end;
 WriteTrace(Str);
end;

procedure WriteTraceConfig(Config: PCounterConfig);
begin
 WriteTrace('  struct Config');
 WriteTrace('  {');
 WriteTrace('    SetTime ................... = ' + string(Config.SetTime));
 WriteTrace('    SetTimeButtonPress ........ = ' + BoolToStr(Config.SetTimeButtonPress));
 WriteTrace('    TimeoutTime ............... = ' + string(Config.TimeoutTime));
 WriteTrace('    TimeoutButtonPress ........ = ' + BoolToStr(Config.TimeoutButtonPress));
 WriteTrace('    WarmupTime ................ = ' + string(Config.WarmupTime));
 WriteTrace('    WarmupButtonPress ......... = ' + BoolToStr(Config.WarmupButtonPress));
 WriteTrace('    SetBreakTime .............. = ' + string(Config.SetBreakTime));
 WriteTrace('    SetBreakButtonPress ....... = ' + BoolToStr(Config.SetBreakButtonPress));
 WriteTrace('    VERLETZUNGSPAUSE .......... = ' + string(Config.VERLETZUNGSPAUSE));
 WriteTrace('    VERLETZUNGSPAUSEButtonPress = ' + BoolToStr(Config.VERLETZUNGSPAUSEButtonPress));
 WriteTrace('    MaxSets ................... = ' + IntToStr(Config.MaxSets));
 WriteTrace('    MaxPoints ................. = ' + IntToStr(Config.MaxPoints));
 WriteTrace('    SetPointsOffset ........... = ' + IntToStr(Config.SetPointsOffset));
 WriteTrace('    SideSwitchLastSet ......... = ' + IntToStr(Config.SideSwitchLastSet));
 WriteTrace('    TimeStopAt ................ = ' + IntToStr(Config.TimeStopAt));
 WriteTrace('    AUFSCHLAGWECHSEL .......... = ' + IntToStr(Config.AUFSCHLAGWECHSEL));
 WriteTrace('    ENTPRELLUNG ............... = ' + IntToStr(Config.ENTPRELLUNG));
 WriteTrace('    AfterReset ................ = ' + IntToStr(Ord(Config.AfterReset)));
 WriteTrace('    LockReset ................. = ' + BoolToStr(Config.LockReset));
 WriteTrace('    LockKeys .................. = ' + BoolToStr(Config.LockKeys));
 WriteTrace('    AfterResetStateOff ........ = ' + BoolToStr(Config.AfterResetStateOff));
 WriteTrace('    DisplayEAModus ............ = ' + BoolToStr(Config.DisplayEAModus));
 WriteTrace('    DisplayWarmupTime ......... = ' + BoolToStr(Config.DisplayWarmupTime));
 WriteTrace('    DisplaySetTime ............ = ' + BoolToStr(Config.DisplaySetTime));
 WriteTrace('    DisplayTimeoutTime ........ = ' + BoolToStr(Config.DisplayTimeoutTime));
 WriteTrace('    DisplaySetBreakTime ....... = ' + BoolToStr(Config.DisplaySetBreakTime));
 WriteTrace('    DisplayVERLETZUNGSPAUSE ... = ' + BoolToStr(Config.DisplayVERLETZUNGSPAUSE));
 WriteTrace('    DisplayMatchEnd ........... = ' + BoolToStr(Config.DisplayMatchEnd));
 WriteTrace('    ShowTimeoutAsOn ........... = ' + BoolToStr(Config.ShowTimeoutAsOn));
 WriteTrace('  }');
end;

procedure WriteTraceDigitsData(DigitsData: PDigitsData);
begin
 WriteTrace('  struct DigitsData');
 WriteTrace('  {');
 WriteTrace('    TimeLeftHigh . = ' + IntToStr(DigitsData.TimeLeftHigh));
 WriteTrace('    TimeLeftLow .. = ' + IntToStr(DigitsData.TimeLeftLow));
 WriteTrace('    TimeRightHigh  = ' + IntToStr(DigitsData.TimeRightHigh));
 WriteTrace('    TimeRightLow . = ' + IntToStr(DigitsData.TimeRightLow));
 WriteTrace('    SetLeft ...... = ' + IntToStr(DigitsData.SetLeft));
 WriteTrace('    SetRight ..... = ' + IntToStr(DigitsData.SetRight));
 WriteTrace('    ScoreLeftHigh  = ' + IntToStr(DigitsData.ScoreLeftHigh));
 WriteTrace('    ScoreLeftLow . = ' + IntToStr(DigitsData.ScoreLeftLow));
 WriteTrace('    ScoreRightHigh = ' + IntToStr(DigitsData.ScoreRightHigh));
 WriteTrace('    ScoreRightLow  = ' + IntToStr(DigitsData.ScoreRightLow));
 WriteTrace('  }');
end;

procedure WriteTraceCounterData(CounterData: PCounterData);
begin
 WriteTrace('  struct CounterData');
 WriteTrace('  {');
 WriteTrace('    PagingInLastSet ... = ' + BoolToStr(CounterData.PagingInLastSet));
 WriteTrace('    Alert ............. = ' + BoolToStr(CounterData.Alert));
 WriteTrace('    TimegameMode ...... = ' + BoolToStr(CounterData.TimegameMode));
 WriteTrace('    GameMode .......... = ' + IntToStr(Ord(CounterData.GameMode)));
 Writetrace('    Service ........... = ' + IntToStr(ORD(CounterData.Service)));
 WriteTrace('    AbandonOrAbort .... = ' + BoolToStr(CounterData.AbandonOrAbort));
 WriteTrace('    TimegameBlock ..... = ' + BoolToStr(CounterData.TimegameBlock));
 WriteTrace('    GameNr ............ = ' + IntToStr(CounterData.GameNr));
 WriteTrace('    PlayerNrLeft ...... = ' + IntToStr(CounterData.PlayerNrLeft));
 // ChT BEGIN
 WriteTrace('    PlayerNrRight ..... = ' + IntToStr(CounterData.PlayerNrRight));
 WriteTrace('    MaxSets ........... = ' + IntToStr(CounterData.MaxSets));
 WriteTrace('    TimeMode .......... = ' + IntToStr(Ord(CounterData.TimeMode)));
 WriteTrace('    TimeoutOrInjured .. = ' + IntToStr(CounterData.TimeoutOrInjured));
 WriteTrace('    TimeStopped ....... = ' + BoolToStr(CounterData.TimeStopped));
 WriteTrace('    Time .............. = ' + IntToStr(CounterData.Time));
 // ChT END
 WriteTrace('    SetsRight ......... = ' + IntToStr(CounterData.SetsRight));
 WriteTrace('    SetsLeft .......... = ' + IntToStr(CounterData.SetsLeft));
 WriteTrace('    SetHistory ........ = ' + string(CounterData.SetHistory));
 WriteTrace('  }');
end;

procedure TDummy.SerialDriverReceiveData;
var AntwortString, SerialNrString, TempStr, CDate, CTime: String;
    Schleife, Index : Integer;
    CounterNr, GameMode: Byte;
    Temp: PChar;
    CounterData: PCounterData;
    CounterConfig: PCounterConfig;
    Command: TCommand;
// ChT BEGIN
    PlayerNrLeft, PlayerNrRight: Cardinal;
// ChT END
// Debugging
    Telegram, Log: String;
begin
 WriteTrace('RECEIVING DATA');
 Index := DriverList.IndexOf(Sender);
 AntwortString := StringOfChar(' ',DataSize);
 Move(DataPtr^,PChar(AntwortString)^,DataSize);
 DriverBuffer[Index] := DriverBuffer[Index] + AntwortString; //Es wurden Daten übertragen
 AntwortString := '';
 Telegram := DriverBuffer[Index];
 if (Length(Telegram) >= 4) and (Length(Telegram) >= Ord(Telegram[4])) then begin

  Try
  {
    Log := '87F78809760A75F48B0C73F28DF18E';
    Telegram := '';
    for Schleife := 0 to 14 do begin
      Telegram := Telegram + Chr(StrToIntDef('$'+Copy(Log, 2 * Schleife + 1, 2),0));
    end;
  }
    AntwortString := AnsiRightStr(Telegram, Length(Telegram)-Ord(Telegram[4]));
    Telegram := DecodeHeader('0',(AnsiLeftStr(Telegram, Ord(Telegram[4]))));
  Except
    Telegram := (AnsiLeftStr(DriverBuffer[Index], Length(DriverBuffer[Index])));
    Log := 'Index=' + IntToStr(Index) + ', Length=' + IntToStr(Length(Telegram)) + ', Tg=';
    for Schleife := 0 to Length(Telegram) do begin
      Log := Log + IntToHex(Ord(Telegram[Schleife]), 2);
    end;

    WriteLn(Log);

    gCallbackProcs.OnErrorCallback(THandle(DriverList[Index]), 0, ERROR_INVALID_DATA_PACKAGE);
    DriverBuffer[Index] := AntwortString;

    Exit;
  End;

  if CheckXORByte((Telegram)) = true then begin
   CounterNr := Ord(Telegram[2]);
   WriteTrace('  Counter => ' + IntToStr(CounterNr));
   Command := TCommand(Ord(Telegram[6]) - 10);

   case Command of
     tcVersion:
       begin
        WriteTrace('  Command => GetVersion');
        TempStr := Copy(Telegram, 7, Length(Telegram));
        GetMem(Temp, Length(TempStr) + 1);
        Temp := StrPCopy(Temp, TempStr);
        WriteTraceInOut('  CALLBACK', 'VersionCallback', [THandle(DriverList[Index]), CounterNr, string(Temp)]);
        gCallbackProcs.VersionCallback(THandle(DriverList[Index]), CounterNr, Temp);
       end;
     tcGetDateTime:
       begin
        WriteTrace('  Command => GetDateTime');
        CDate := sc.Str(Ord(Telegram[11]), 2) + '.' +
                 sc.Str(Ord(Telegram[12]), 2) + '.' +
                 sc.Str(Ord(Telegram[13]), 2);
        CTime := sc.Str(Ord(Telegram[10]), 2) + ':' +
                 sc.Str(Ord(Telegram[9]), 2) + ':' +
                 sc.Str(Ord(Telegram[8]), 2);
        TempStr := FormatDateTime(CurrentFormatStr, StrToDate(CDate) + StrToTime(CTime));
        GetMem(Temp, Length(TempStr) + 1);
        Temp := StrPCopy(Temp, TempStr);
        WriteTraceInOut('  CALLBACK', 'GetDateTimeCallback', [THandle(DriverList[Index]), CounterNr, string(Temp)]);
        gCallbackProcs.GetDateTimeCallback(THandle(DriverList[Index]), CounterNr, Temp);
       end;
     tcSetDateTime:
       begin
        WriteTrace('  Command => SetDateTime');
        WriteTraceInOut('  CALLBACK', 'SetDateTimeCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.SetDateTimeCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcGetConfig:
       begin
        WriteTrace('  Command => GetConfig');
        GetMem(CounterConfig, sizeof(TCounterConfig));
        TempStr := sc.Str(Ord(Telegram[8]), 2) + ':' + sc.Str(Ord(Telegram[7]), 2);
        GetMem(CounterConfig^.SetTime, Length(TempStr) + 1);
        // ChT BEGIN
        CounterConfig^.SetTime := StrPCopy(CounterConfig^.SetTime, TempStr);
        CounterConfig^.SetTimeButtonPress := (Ord(Telegram[24]) and 1) = 0;
        TempStr := sc.Str(Ord(Telegram[10]), 2) + ':' + sc.Str(Ord(Telegram[9]), 2);
        GetMem(CounterConfig^.TimeoutTime, Length(TempStr) + 1);
        CounterConfig^.TimeoutTime := StrPCopy(CounterConfig^.TimeoutTime, TempStr);
        CounterConfig^.TimeoutButtonPress := (Ord(Telegram[24]) and 2) = 0;
        TempStr := sc.Str(Ord(Telegram[12]), 2) + ':' + sc.Str(Ord(Telegram[11]), 2);
        GetMem(CounterConfig^.WarmupTime, Length(TempStr) + 1);
        CounterConfig^.WarmupTime := StrPCopy(CounterConfig^.WarmupTime, TempStr);
        CounterConfig^.WarmupButtonPress := (Ord(Telegram[24]) and 4) = 0;
        TempStr := sc.Str(Ord(Telegram[14]), 2) + ':' + sc.Str(Ord(Telegram[13]), 2);
        GetMem(CounterConfig^.SetBreakTime, Length(TempStr) + 1);
        CounterConfig^.SetBreakTime := StrPCopy(CounterConfig^.SetBreakTime, TempStr);
        CounterConfig^.SetBreakButtonPress := (Ord(Telegram[24]) and 8) = 0;
        TempStr := sc.Str(Ord(Telegram[16]), 2) + ':' + sc.Str(Ord(Telegram[15]), 2);
        GetMem(CounterConfig^.VERLETZUNGSPAUSE, Length(TempStr) + 1);
        CounterConfig^.VERLETZUNGSPAUSE := StrPCopy(CounterConfig^.VERLETZUNGSPAUSE, TempStr);
        CounterConfig^.VERLETZUNGSPAUSEButtonPress := (Ord(Telegram[24]) and $10) = 0;
        CounterConfig^.LockKeys := (Ord(Telegram[24]) and $20) = $20;
        CounterConfig^.LockReset := (Ord(Telegram[24]) and $40) = $40;
        CounterConfig^.AfterResetStateOff := (Ord(Telegram[24]) and $80) = $80;
        CounterConfig^.DisplayEAModus := (Ord(Telegram[24]) and $1) = $1;
        CounterConfig^.DisplayWarmupTime := (Ord(Telegram[24]) and $2) = $2;
        CounterConfig^.DisplaySetTime := (Ord(Telegram[24]) and $4) = $4;
        CounterConfig^.DisplayTimeoutTime := (Ord(Telegram[24]) and $8) = $8;
        CounterConfig^.DisplaySetBreakTime := (Ord(Telegram[24]) and $10) = $10;
        CounterConfig^.DisplayVERLETZUNGSPAUSE := (Ord(Telegram[24]) and $20) = $20;
        CounterConfig^.DisplayMatchEnd := (Ord(Telegram[24]) and $40) = $40;
        CounterConfig^.ShowTimeoutAsOn := (Ord(Telegram[26]) and $80) = $80;
        // ChT END
        CounterConfig^.MaxSets := Ord(Telegram[17]);
        CounterConfig^.MaxPoints := Ord(Telegram[18]);
        CounterConfig^.SetPointsOffset := Ord(Telegram[19]);
        CounterConfig^.SideSwitchLastSet := Ord(Telegram[20]);
        CounterConfig^.TimeStopAt := Ord(Telegram[21]);
        CounterConfig^.AUFSCHLAGWECHSEL := Ord(Telegram[22]);
        CounterConfig^.ENTPRELLUNG := Ord(Telegram[23]);
        CounterConfig^.AfterReset := TAfterReset(Ord(Telegram[25]));
        WriteTraceConfig(CounterConfig);
        WriteTraceInOut('  CALLBACK', 'GetConfigCallback', [THandle(DriverList[Index]), CounterNr, '0x' + IntToHex(Cardinal(CounterConfig), 8)]);
        gCallbackProcs.GetConfigCallback(THandle(DriverList[Index]), CounterNr, CounterConfig);
       end;
     tcSetConfig:
       begin
        WriteTrace('  Command => SetConfig');
        WriteTraceInOut('  CALLBACK', 'SetConfigCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.SetConfigCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcReset:
       begin
        WriteTrace('  Command => Reset');
        WriteTraceInOut('  CALLBACK', 'ResetCallback', [THandle(DriverList[Index]), CounterNr,  True]);
        gCallbackProcs.ResetCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcPushButton:
       begin
        WriteTrace('  Command => PushButton');
        WriteTraceInOut('  CALLBACK', 'PushButtonCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.PushButtonCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcSwitchPlayers:
       begin
        WriteTrace('  Command => SwitchPlayers');
        WriteTraceInOut('  CALLBACK', 'SwitchCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.SwitchCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcSetDigits:
       begin
        WriteTrace('  Command => SetDigits');
        WriteTraceInOut('  CALLBACK', 'SetDigitsCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.SetDigitsCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcSetPlayers:
       begin
        WriteTrace('  Command => SetPlayers');
        WriteTraceInOut('  CALLBACK', 'SetPlayerNumbersCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.SetPlayerNumbersCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcGetDataBroadcast, tcGetData:
       begin
        WriteTrace('  Command => GetData');
        GetMem(CounterData, sizeof(TCounterData));
        CounterData^.PagingInLastSet := Ord(Telegram[7]) and 1 = 1;
        CounterData^.Alert := Ord(Telegram[7]) and 2 = 2;
        CounterData^.TimegameMode := Ord(Telegram[7]) and 4 = 4;
        CounterData^.Service := (Ord(Telegram[7]) shr 3) and 1;
        GameMode := (Ord(Telegram[7]) shr 4) and 3;
        CounterData^.GameMode := TGameMode(GameMode);
        CounterData^.AbandonOrAbort := Ord(Telegram[7]) and 64 = 64;
        CounterData^.TimegameBlock := Ord(Telegram[7]) and 128 = 128;
        // ChT BEGIN
        // CounterData^.PlayerNrLeft := Ord(Telegram[11]);
        // CounterData^.PlayerNrLeft := (CounterData^.PlayerNrLeft shl 8) or Ord(Telegram[10]);
        // CounterData^.PlayerNrRight := Ord(Telegram[13]);
        // CounterData^.PlayerNrRight := (CounterData^.PlayerNrRight shl 8) or Ord(Telegram[12]);
        // Spielernummern sind anscheinend Dezimal codiert ...
        CounterData^.GameNr := Ord(Telegram[9]) * 100 + Ord(Telegram[8]);
        if ((Ord(Telegram[11]) = $FF)) then
          CounterData^.PlayerNrLeft := (Ord(Telegram[11]) shl 8) + Ord(Telegram[10])
        else
          CounterData^.PlayerNrLeft := Ord(Telegram[11]) * 100 + Ord(Telegram[10]);
        if ((Ord(Telegram[13]) = $FF)) then
          CounterData^.PlayerNrRight := (Ord(Telegram[13]) shl 8) + Ord(Telegram[12])
        else
          CounterData^.PlayerNrRight := Ord(Telegram[13]) * 100 + Ord(Telegram[12]);

        CounterData^.MaxSets := (Ord(Telegram[14]) and 7);
        CounterData^.TimeMode := TTimeMode((Ord(Telegram[14]) shr 3) and 7);
        CounterData^.TimeoutOrInjured := (Ord(Telegram[14]) shr 6) and 1;
        CounterData^.TimeStopped := ((Ord(Telegram[14]) shr 7) and 1) = 1;
        CounterData^.SetsRight := Ord(Telegram[15]) and 7;
        CounterData^.SetsLeft := Ord(Telegram[16]) and 7;
        CounterData^.Time := (Ord(Telegram[15]) shr 3);
        CounterData^.Time := CounterData^.Time + 32 * ((Ord(Telegram[16]) shr 3) and 1);
        CounterData^.Time := CounterData^.Time + (Ord(Telegram[16]) shr 4) * 60;

        // TempStr := Copy(Telegram, 16, Length(Telegram));
        for Schleife := 17 to Length(Telegram) do
          TempStr := TempStr + IntToHex(Ord(Telegram[Schleife]), 2);

        GetMem(CounterData^.SetHistory, Length(TempStr) + 1);
        CounterData^.SetHistory := StrPCopy(CounterData^.SetHistory, TempStr);
        // ChT END
        WriteTraceCounterData(CounterData);
        WriteTraceInOut('  CALLBACK', 'GetDataCallback', [THandle(DriverList[Index]), CounterNr,  '0x' + IntToHex(Cardinal(CounterData), 8)]);
        gCallbackProcs.GetDataCallback(THandle(DriverList[Index]), CounterNr, CounterData);
       end;
     tcBaudrate:
       begin
        WriteTrace('  Command => Baudrate');
        WriteTraceInOut('  CALLBACK', 'BaudrateCallback', [THandle(DriverList[Index]), True]);
        gCallbackProcs.BaudrateCallback(THandle(DriverList[Index]), True);
       end;
     tcGetSetResults:
       begin
        WriteTrace('  Command => GetSetResults');
        // ChT BEGIN
        // TempStr := Copy(Telegram, 7, Length(Telegram));
        TempStr := '';
        for Schleife := 7 to Length(Telegram) do
          TempStr := TempStr + IntToHex(Ord(Telegram[Schleife]), 2);
        // ChT END
        GetMem(Temp, Length(TempStr) + 1);
        Temp := StrPCopy(Temp, TempStr);
        WriteTraceInOut('  CALLBACK', 'SetResultsCallback', [THandle(DriverList[Index]), CounterNr, string(Temp)]);
        gCallbackProcs.SetResultsCallback(THandle(DriverList[Index]), CounterNr, Temp);
       end;
     tcSetGameNr:
       begin
        WriteTrace('  Command => SetGameNr');
        WriteTraceInOut('  CALLBACK', 'SetGameNumberCallback', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.SetGameNumberCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     tcGetGameNr:
       begin
        WriteTrace('  Command => GetGameNr');
        WriteTraceInOut('  CALLBACK', 'GetGameNumberCallback', [THandle(DriverList[Index]), CounterNr, (Ord(Telegram[8]) shl 8) or Ord(Telegram[7])]);
        gCallbackProcs.GetGameNumberCallback(THandle(DriverList[Index]), CounterNr, (Ord(Telegram[8]) shl 8) or Ord(Telegram[7]));
       end;
     tcGetSerial:
       begin
        WriteTrace('  Command => GetSerial');
        SerialNrString := '';
        for Schleife := 11 to 14 do
         SerialNrString := SerialNrString + IntToHex(Ord(Telegram[Schleife]), 2);
        GetMem(Temp, Length(SerialNrString) + 1);
        Temp := StrPCopy(Temp, SerialNrString);
        WriteTraceInOut('  CALLBACK', 'SerialNumberCallback', [THandle(DriverList[Index]), CounterNr, string(Temp)]);
        gCallbackProcs.SerialNumberCallback(THandle(DriverList[Index]), CounterNr, Temp);
       end;
// ChT BEGIN
     tcGetPlayers:
       begin
         WriteTrace('   Command => GetPlayers');
         if (Ord(Telegram[8]) = $FF) then
           PlayerNrLeft := Ord(Telegram[7]) + (Ord(Telegram[8]) shl 8)
         else
           PlayerNrLeft := Ord(Telegram[7]) + 100 * Ord(Telegram[8]);
         if (Ord(Telegram[10]) = $FF) then
           PlayerNrRight := Ord(Telegram[9]) + (Ord(Telegram[10]) shl 8)
         else
           PlayerNrRight := Ord(Telegram[9]) + 100 * Ord(Telegram[10]);
         WriteTraceInOut('   CALLBACK', 'SetGameDataCallback', [THandle(DriverList[Index]), CounterNr, PlayerNrLeft, PlayerNrRight]);
         gCallbackProcs.GetPlayerNumbersCallback(THandle(DriverList[Index]), CounterNr, PlayerNrLeft, PlayerNrRight);
       end;

     tcResetAlert:
       begin
        WriteTrace('  Command => ResetAlert');
        WriteTraceInOut('  CALLBACK', 'ResetAlert', [THandle(DriverList[Index]), CounterNr, True]);
        gCallbackProcs.ResetAlertCallback(THandle(DriverList[Index]), CounterNr, True);
       end;
     else
        begin
          WriteTrace('  Command => unknown');
        end;
// ChT END

   end;
  end
  else gCallbackProcs.OnErrorCallback(THandle(DriverList[Index]), 0, ERROR_INVALID_DATA_PACKAGE);
  DriverBuffer[Index] := AntwortString;
 end;
 WriteTrace('END OF RECEIVING')
end;

{ Date format strings:

Bezeichner  Anzeige
 c          Zeigt das Datum in dem in der globalen Variablen ShortDateFormat angegebenen Format an. Dahinter wird die Uhrzeit, in dem in der globalen Variablen LongTimeFormat festgelegten Format dargestellt. Die Uhrzeit erscheint nicht, wenn der Datums-/Zeitwert exakt Mitternacht ergibt.

 d          Zeigt den Tag als Zahl ohne führende Null an (1-31).

 dd         Zeigt den Tag als Zahl mit führender Null an (01-31).

 ddd        Zeigt den Wochentag als Abkürzung (Son-Sam) in den in der globalen Variablen ShortDayNames festgelegten Strings an.

 dddd       Zeigt den ausgeschriebenen Wochentag (Sonntag-Samstag) in den in der globalen Variablen LongDayNames festgelegten Strings an.

 ddddd      Zeigt das Datum in dem in der globalen Variablen ShortDateFormat angegebenen Format an.

 dddddd     Zeigt das Datum in dem in der globalen Variablen LongDateFormat angegebenen Format an.

 e          (Nur Windows) Zeigt das Jahr in der aktuellen Datums-/Zeitangabe als Zahl ohne führende Null an (gilt nur für die japanische, koreanische und taiwanesische Ländereinstellung).

 ee         (Nur Windows) Zeigt das Jahr in der aktuellen Datums-/Zeitangabe als Zahl mit führender Null an (gilt nur für die japanische, koreanische und taiwanesische Ländereinstellung).

 g          (Nur Windows) Zeigt die Datums-/Zeitangabe als Abkürzung an (gilt nur für die japanische und taiwanesische Ländereinstellung).

 gg         (Nur Windows) Zeigt die Datums-/Zeitangabe in ausgeschriebener Form an (gilt nur für die japanische und taiwanesische Ländereinstellung).

 m          Zeigt den Monat als Zahl ohne führende Null an (1-12). Wenn auf den Bezeichner m unmittelbar der Bezeichner h oder hh folgt, werden an Stelle des Monats die Minuten angezeigt.

 mm         Zeigt den Monat als Zahl mit führender Null an (01-12). Wenn auf den Bezeichner mm unmittelbar der Bezeichner h oder hh folgt, werden an Stelle des Monats die Minuten angezeigt.

 mmm        Zeigt den Monatsnamen als Abkürzung (Jan-Dez) in den in der globalen Variablen ShortMonthNames festgelegten Strings an.

 mmmm       Zeigt den ausgeschriebenen Monatsnamen (Januar-Dezember) in den in der globalen Variablen LongMonthNames festgelegten Strings an.

 yy         Zeigt das Jahr als zweistellige Zahl an (00-99).

 yyyy       Zeigt das Jahr als vierstellige Zahl an (0000-9999).

 h          Zeigt die Stunde ohne führende Null an (0-23).

 hh         Zeigt die Stunde mit führender Null an (00-23).

 n          Zeigt die Minute ohne führende Null an (0-59).

 nn         Zeigt die Minute mit führender Null an (00-59).

 s          Zeigt die Sekunde ohne führende Null an (0-59).

 ss         Zeigt die Sekunde mit führender Null an (00-59).

 z          Zeigt die Millisekunde ohne führende Null an (0-999).

 zzz        Zeigt die Millisekunde mit führender Null an (000-999).

 t          Zeigt die Uhrzeit in dem in der globalen Variablen ShortDateFormat angegebenen Format an.

 tt\        Zeigt die Uhrzeit in dem in der globalen Variablen LongTimeFormat angegebenen Format an.

 am/pm      Verwendet die 12-Stunden-Zeitanzeige für den vorhergehenden Bezeichner h oder hh und zeigt alle Stunden vor Mittag mit dem String 'am' und alle Stunden nach Mittag mit dem String 'pm' an. Der Bezeichner am/pm kann in Großbuchstaben, in Kleinbuchstaben oder in gemischter Schreibweise eingegeben werden. Die Ausgabe wird entsprechend angepasst.

 a/p        Verwendet die 12-Stunden-Zeitanzeige für den vorhergehenden Bezeichner h oder hh und zeigt alle Stunden vor Mittag mit dem Zeichen 'a' und alle Stunden nach Mittag mit dem Zeichen 'p' an. Der Bezeichner a/p kann in Großbuchstaben, in Kleinbuchstaben oder in gemischter Schreibweise eingegeben werden. Die Ausgabe wird entsprechend angepasst.

 ampm       Verwendet die 12-Stunden-Zeitanzeige für den vorhergehenden Bezeichner h oder hh und zeigt alle Stunden vor Mittag mit dem String aus der globalen Variablen TimeAMString und alle Stunden nach Mittag mit dem String aus der globalen Variable TimePMString an.

 /          Zeigt als Datumstrennzeichen das in der globalen Variablen DateSeparator angegebene Zeichen an.

 :          Zeigt als Uhrzeittrennzeichen das in der globalen Variablen TimeSeparator angegebene Zeichen an.

 'xx'/"xx"  Zeichen, die in einfache oder doppelte Anführungszeichen eingeschlossen sind, werden ohne spezielle Formatierung übernommen.

}

end.
 */

