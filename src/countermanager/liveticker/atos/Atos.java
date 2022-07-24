/* Copyright (C) 2022 Christoph Theis */
package countermanager.liveticker.atos;

import countermanager.driver.CounterData;
import countermanager.liveticker.Liveticker;
import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;
import countermanager.prefs.Preferences;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chtheis
 */
public class Atos extends Liveticker {
    
    private enum WhatToSend {
        REQ_PHASES,         // 1042
        REQ_NEXT_MATCH,     // 1001     
        NTF_START_MATCH,    // 1005
        NTF_END_MATCH,      // 1010 
        NTF_START_GAME,     // 1006
        NTF_END_GAME,       // 1009
        NTF_SCORE,          // 1007
        NTF_CARD,           // 1008
        NTF_SCORE_HISTORY   // 1050
    }
    
    private enum Service {
        NONE,
        A,
        B,
        X,
        Y
    }
    
    // Phases id <-> description
    private final Map<String, String> phases = new java.util.HashMap<>();
    // Match identifiers table <-> String composed of cpName, mtMatch, nmType, mtMS
    private final Map<Integer, String> matchStrings = new java.util.HashMap<>();
    // Atos' match id as table <-> idmatch
    private final Map<Integer, String> idmatches = new java.util.HashMap<>();
    
    private Service calculateService(CounterData cd, boolean  isDouble) {
        if (!isDouble) {
            if (cd.getServiceLeft())
                return Service.A;
            if (cd.getServiceRight())
                return Service.X;

            return Service.NONE;
        }
        
        switch (cd.getServiceDouble()) {
            case 0 :
                return Service.NONE;
            case 1 :  // BX
            case -4 : // BY
                return Service.B;
            case 2 :  // XA
            case -1:  // XB
                return Service.X;
            case 3 :  // AY
            case -2 : // AX
                return Service.A;
            case 4 :  // YB
            case -3 : // YA
                return Service.Y;
        }
        
        return Service.NONE;
    }
    
    private Service calculateReceiver(CounterData cd, boolean isDouble) {
        if (!isDouble) {
            if (cd.getServiceLeft())
                return Service.X;
            if (cd.getServiceRight())
                return Service.A;

            return Service.NONE;
        }
        
        switch (cd.getServiceDouble()) {
            case 0 :
                return Service.NONE;                
            case 1 :  // BX
            case -2 : // AX
                return Service.X;
            case 2 :  // XA
            case -3 : // YA
                return Service.A;
            case 3 :  // AY
            case -4 : // BY
                return Service.Y;
            case 4 :  // YB
            case -1:  // XB
                return Service.B;
        }
        
        return Service.NONE;
    }
    
    private EnumSet<WhatToSend> compareCounterData(CounterData oldData, CounterData newData) {
        EnumSet<WhatToSend> res = EnumSet.noneOf(WhatToSend.class);
        
        // Paranoia
        if (oldData == null || newData == null)
            return res;
        
        int ocs = oldData.getSetsLeft() + oldData.getSetsRight();
        int ncs = newData.getSetsLeft() + newData.getSetsRight();

        int[] osh = oldData.getSetHistory() == null || oldData.getSetHistory().length <= ocs ? new int[] {0, 0} : oldData.getSetHistory()[ocs];
        int[] nsh = newData.getSetHistory() == null || newData.getSetHistory().length <= ncs ? new int[] {0, 0} : newData.getSetHistory()[ncs];
        
        // Start of match
        if (oldData.getGameMode() == CounterData.GameMode.RESET && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_MATCH);
        
        if (oldData.getGameMode() == CounterData.GameMode.WARMUP && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_MATCH);
        
        // A match also starts a game
        if (oldData.getGameMode() == CounterData.GameMode.RESET && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_GAME);
        
        if (oldData.getGameMode() == CounterData.GameMode.WARMUP && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_GAME);

        if (oldData.getGameMode() == CounterData.GameMode.RUNNING && newData.getGameMode() != CounterData.GameMode.RESET) {
            // Switch game status
            // Start game
            if (oldData.getTimeMode() == CounterData.TimeMode.PREPARE && newData.getTimeMode() == CounterData.TimeMode.MATCH)
                res.add(WhatToSend.NTF_START_GAME);
            
            if (oldData.getTimeMode() == CounterData.TimeMode.BREAK && newData.getTimeMode() == CounterData.TimeMode.MATCH)
                res.add(WhatToSend.NTF_START_GAME);
                        
            if (oldData.getTimeMode() == CounterData.TimeMode.BREAK && newData.getTimeMode() == CounterData.TimeMode.NONE)
                res.add(WhatToSend.NTF_START_GAME);
            
            // End game
            if (newData.getGameMode() == CounterData.GameMode.END)
                res.add(WhatToSend.NTF_END_GAME);
            
            if (oldData.getTimeMode() == CounterData.TimeMode.MATCH && newData.getTimeMode() == CounterData.TimeMode.BREAK)
                res.add(WhatToSend.NTF_END_GAME);
                        
            if (oldData.getTimeMode() == CounterData.TimeMode.NONE && newData.getTimeMode() == CounterData.TimeMode.BREAK)
                res.add(WhatToSend.NTF_END_GAME);            
        }
        
        // Service
        if (oldData.getServiceLeft() != newData.getServiceLeft())
            res.add(WhatToSend.NTF_SCORE);
        if (oldData.getServiceRight() != newData.getServiceRight())
            res.add(WhatToSend.NTF_SCORE);
        if (oldData.getServiceDouble()!= newData.getServiceDouble())
            res.add(WhatToSend.NTF_SCORE);

        // Points
        if (newData.getGameMode() != CounterData.GameMode.END) {
            if (osh[0] != nsh[0] || osh[1] != nsh[1]) {
                res.add(WhatToSend.NTF_SCORE);
                res.add(WhatToSend.NTF_SCORE_HISTORY);
            }
        }
        
        // Timeout
        if (oldData.isTimeoutLeft() != newData.isTimeoutLeft())
            res.add(WhatToSend.NTF_CARD);
        if (oldData.isTimeoutRight() != newData.isTimeoutRight())
            res.add(WhatToSend.NTF_CARD);

        // Cards
        if (oldData.getCardLeft().ordinal() != newData.getCardLeft().ordinal())
            res.add(WhatToSend.NTF_CARD);

        if (oldData.getCardRight().ordinal() != newData.getCardRight().ordinal())
            res.add(WhatToSend.NTF_CARD);

        // So it comes after any score including END_GAME
        if (oldData.getGameMode() != CounterData.GameMode.END && newData.getGameMode() == CounterData.GameMode.END)
            res.add(WhatToSend.NTF_END_MATCH);
        
        return res;
    }
    
    
    private String formatDuration(long t) {
        if (t < 0) {
            System.out.println(String.format("Duration < 0"));
        }
        
        java.time.Duration d = java.time.Duration.ofNanos(t);
        if (d.toSecondsPart() < 0)
            System.out.println("Seconds < 0");
        
        return String.format("%02d:%02d:%02d.%03d", 
                d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart());
    }
    
    @Override
    public void setMessage(int counter, String message) {

    }

    @Override
    public String getMessage(int counter) {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void counterAdded(int counter) {

    }

    @Override
    public void counterRemoved(int counter) {

    }

    @Override
    public void counterChanged(int counter) {
        if (counter < fromTable - CounterModel.getDefaultInstance().getTableOffset())
            return;
        if (counter > toTable - CounterModel.getDefaultInstance().getTableOffset())
            return;
        
        onCounterChanged(counter);
    }

    @Override
    public void counterError(int counter, Throwable t) {
    }
    
    @Override
    public void onGlobalEnable(boolean e) {
        if (e) {
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        onTimer();
                    }}, INTERVAL, INTERVAL);
            }
            
            if (isInstanceEnabled())
                onActivate(true);            
        } else {
            onActivate(false);
        }
    }
    
    
    @Override
    public void setInstanceEnabled(boolean e) {
        boolean wasActive = isEnabled();
        super.setInstanceEnabled(e);
        boolean nowActive = isEnabled();
        
        if (!wasActive && nowActive) {
            // 
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        onTimer();
                    }}, INTERVAL, INTERVAL);
            }
            
            onActivate(true);
        } else if (wasActive && !nowActive) {
            onActivate(false);            
        }
    }    

    public int getFromTable() {
        return fromTable;
    }

    public void setFromTable(int fromTable) {
        this.fromTable = fromTable;
    }

    public int getToTable() {
        return toTable;
    }

    public void setToTable(int toTable) {
        this.toTable = toTable;
    }

    public int getTableOffset() {
        return tableOffset;
    }

    public void setTableOffset(int tableOffset) {
        this.tableOffset = tableOffset;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public void setOutputFile(String file) {
        this.outputFile = file;
    }

    public boolean isDebug() {
        return debug;
    }


    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    protected void loadProperties() {
        Preferences.loadProperties(this, this.getClass().getName(), true);
    }

    @Override
    protected void saveProperties() {
        Preferences.saveProperties(this, this.getClass().getName(), true);
    }
    
    private void onActivate(boolean isActive) {
        if (!isActive) {
            return;
        }
        
        if (commThread == null) {
            commThread = new CommThread();
            commThread.start();
        }
        
        // We go active, get current settings
        Message msg = new Message();
        msg.id = 0;
        msg.code = 1042;
        msg.data = "ENG";
        msg.wantAnswer = true;
        
        sendMessage(msg);
    }
    
    
    private synchronized void onCounterChanged(int counter) {
        CounterData newCounterData = CounterModel.getDefaultInstance().getCounterData(counter);
        CounterData oldCounterData = currentCounterData.get(counter);

        if (newCounterData == null)
            return;

        // Make sure we have the initial state
        if (oldCounterData == null) {
            oldCounterData = new CounterData();
        } 

        // Align with match, i.e. swap if isSwapped
        try {
            if (oldCounterData.isSwapped()) {
                oldCounterData = oldCounterData.clone();
                oldCounterData.swap();
            }

            if (newCounterData.isSwapped()) {
                newCounterData = newCounterData.clone();
                newCounterData.swap();
            }
        } catch (CloneNotSupportedException ex) {
            return;
        }


        if (oldCounterData.equals(newCounterData))
            return;

        if (debug && debugStream == null) {
            try {
                debugStream = new java.io.PrintStream("..\\dbg.log");
            } catch (IOException ex) {
                debugStream = System.out;
            }
        } else {
            debugStream = new java.io.PrintStream(new java.io.OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    // Ignore all 
                }
            });
        }

        EnumSet<WhatToSend> es = compareCounterData(oldCounterData, newCounterData);

        if (es.contains(WhatToSend.NTF_START_MATCH))
            sendStartMatch(counter, oldCounterData, newCounterData);

        if (es.contains(WhatToSend.NTF_START_GAME))
            sendStartGame(counter, oldCounterData, newCounterData);

        if (es.contains(WhatToSend.NTF_CARD))
            sendCard(counter, oldCounterData, newCounterData);

        if (es.contains((WhatToSend.NTF_SCORE)))
            sendScore(counter, oldCounterData, newCounterData);

        if (es.contains(WhatToSend.NTF_SCORE_HISTORY))
            sendHistory(counter, oldCounterData, newCounterData);

        if (es.contains(WhatToSend.NTF_END_GAME))
            sendEndGame(counter, oldCounterData, newCounterData);

        if (es.contains(WhatToSend.NTF_END_MATCH))
            sendEndMatch(counter, oldCounterData, newCounterData);

        currentCounterData.put(counter, newCounterData);
    }
    
    
    private void onTimer() {
        for (int table = fromTable; table <= toTable; ++table) {
            CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(table - tableOffset);
            CounterData counterData = CounterModel.getDefaultInstance().getCounterData(table - tableOffset);
            
            if (counterMatch == null || counterData == null)
                continue;
            
            if ( (counterData.getGameMode() != CounterData.GameMode.RESET) && 
                    idmatches.containsKey(table) && !idmatches.get(table).startsWith("<") &&
                    matchStrings.containsKey(table) && !matchStrings.get(table).startsWith("<") )
                continue;
            
            Message msg = new Message();
            msg.id = table;
            msg.code = 1001;
            msg.data = "ENG";
            msg.wantAnswer = true;
            
            sendMessage(msg);
            
            // Delay after sending to give some time to write on socket
            // We need a better way to do it.
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    void onMessage(Message msg) {
        if (msg == null)
            return;
        
        // int table = msg.id;
        switch (msg.code) {
            case 2042 : // Phases
                handlePhases(msg);
                break;
                
            case 2001 : // Next math
                handleNextMatch(msg);
                break;
        }
    }
    
    private static final DateFormat daytimeFormat = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
    private java.io.PrintStream debugStream = null;
    
    private void sendStartMatch(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }
        
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        StringBuilder sb = new StringBuilder();
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // DAYTIME
                .append(daytime)
                // IDMATCH
                .append(idmatch)
        ;
        
        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("Start Match: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("DAy Time = ").append(daytime).append(", ")
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1005;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    
    private void sendEndMatch(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }
        
        String mt = formatDuration(cm.getRunningMatchTime());
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        StringBuilder sb = new StringBuilder();
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // MATCHTIME
                .append(mt)
                // DAYTIME
                .append(daytime)
                // IDMATCH
                .append(idmatch)
        ;
        
        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("End Match: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("Match Time = ").append(mt).append(", ")
                .append("Day Time = ").append(daytime)
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1010;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    
    
    private void sendStartGame(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }
        
        // In case timer was not started
        int cs = cm.mtResA + cm.mtResX;
        cm.startGameTime(cs);
        
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        StringBuilder sb = new StringBuilder();
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // pp
                .append(String.format("%d", cm.mtResA + cm.mtResX + 1))
                // x y
                .append(String.format("%d%d", cm.mtResA, cm.mtResX))
                // DAYTIME
                .append(daytime)
                // IDMATCH
                .append(idmatch)
        ;
        
        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("Start Game: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("Game = ").append(String.format("%d", cm.mtResA + cm.mtResX + 1)).append(", ")
                .append("Res A = ").append(String.format("%d", cm.mtResA)).append(", ")
                .append("Res X = ").append(String.format("%d", cm.mtResX)).append(", ")
                .append("Day Time = ").append(daytime).append(", ")
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1006;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    

    private void sendEndGame(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);  
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }
        
        int cs = cm.mtResA + cm.mtResX;
        
        String gt = formatDuration(cm.getRunningGameTime(cs));
        String mt = formatDuration(cm.getRunningMatchTime());
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        StringBuilder sb = new StringBuilder();
        
        int resA = cm.mtResA;
        int resX = cm.mtResX;
        
        // points left / right
        int[] sh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        if (sh[0] > sh[1])
            ++resA;
        else if (sh[1] > sh[0])
            ++resX;
        
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // pp
                .append(String.format("%d", cs + 1))
                // x y
                .append(String.format("%d%d", resA, resX))
                // GAMETIME
                .append(gt)
                // MATCHTIME
                .append(mt)
                // DAYTIME
                .append(daytime)
                // IDMATCH
                .append(idmatch)
        ;
        
        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("End Game: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("Game = ").append(String.format("%d", cs)).append(", ")
                .append("Res A = ").append(String.format("%d", cm.mtResA)).append(", ")
                .append("Res X = ").append(String.format("%d", cm.mtResX)).append(", ")
                .append("Game Time = ").append(gt).append(", ")
                .append("Match Time = ").append(mt).append(", ")
                .append("Day Time = ").append(daytime).append(", ")
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1009;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    
    
    private void sendScore(int counter, CounterData oldCounterData, CounterData newCounterData) {
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }
     
        boolean isDouble = (cm.cpType == 2 || cm.cpType == 3 || cm.cpType == 4 && cm.nmType == 2);
                
        // current set, 0-based
        int cs = newCounterData.getSetsLeft() + newCounterData.getSetsRight();
        // points left / right
        int[] sh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        // In case the match has ended the result is already updated
        if (newCounterData.getGameMode() == CounterData.GameMode.END)
            --cs;
        
        // service, return
        int s = calculateService(newCounterData, isDouble).ordinal();
        int r = calculateReceiver(newCounterData, isDouble).ordinal();
        String e = newCounterData.isExpedite() ? "Y" : "N";
        String gt = formatDuration(cm.getRunningGameTime(cs));
        String mt = formatDuration(cm.getRunningMatchTime());
        
        StringBuilder sb = new StringBuilder();
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // pp
                .append(String.format("%d", cs + 1))
                // xx yy
                .append(String.format("%02d%02d", sh[0], sh[1]))
                // Service, Receiver
                .append(String.format("%d%d", s, r))
                // Expedite
                .append(e)
                // GAMETIME
                .append(gt)
                // MATCHTIME
                .append(mt)
                // IDMATCH
                .append(idmatches.get(cm.mtTable))
        ;
        
        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("Score: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("Game = ").append(String.format("%d", cs + 1)).append(", ")
                .append("Res A = ").append(String.format("%02d", sh[0])).append(", ")
                .append("Res X = ").append(String.format("%02d", sh[1])).append(", ")
                .append("Service = ").append(String.format("%d", s)).append(", ")
                .append("Receiver = ").append(String.format("%d", r)).append(", ")
                .append("Expedite = ").append(e).append(", ")
                .append("Game Time = ").append(gt).append(", ")
                .append("Match Time = ").append(mt).append(", ")
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1007;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    
    
    private void sendCard(int counter, CounterData oldCounterData, CounterData newCounterData) {
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }

        // [SOH]<<Id>>[DC3]1008<<AA>><<BBB>><<CC>><<M>><<pp>><<xx>><<yy>><<SERVICE>><<PLAYER>><<CARD>><<f>><<GAMETIME>><<MATCHTIME>><<IDMATCH>>[EOT]
        
        boolean isDouble = (cm.cpType == 2 || cm.cpType == 3);
                
        // current set, 0-based
        int cs = oldCounterData.getSetsLeft() + oldCounterData.getSetsRight();

        // Match time
        String mt = formatDuration(cm.getRunningMatchTime());
        // Game time
        String gt = formatDuration(cm.getRunningGameTime(cs));
        
        // points left / right
        int[] osh = oldCounterData.getSetHistory() == null ? new int[] {0, 0} : oldCounterData.getSetHistory()[cs];
        int[] nsh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        // service
        int s = calculateService(oldCounterData, isDouble).ordinal();
        // Do, Undo
        String f = "+";
        // player and card
        int pl = 0;
        int card = 0;
        if (!oldCounterData.hasTimeoutLeft() && newCounterData.hasTimeoutLeft()) {
            pl = 1;
            card = 1;
        } else if (!oldCounterData.hasTimeoutRight() && newCounterData.hasTimeoutRight()) {
            pl = 3;   
            card = 1;            
        } else if (oldCounterData.getCardLeft().ordinal() < newCounterData.getCardLeft().ordinal()) {
            pl = 1;
            switch (newCounterData.getCardLeft()) {
                case YELLOW :
                    card = 2;
                    break;
                case YR1P :
                    card = 4;
                    break;
                case YR2P :
                    card = 5;
                    break;
            }
        } else if (oldCounterData.getCardRight().ordinal() < newCounterData.getCardRight().ordinal()) {
            pl = 3;
            switch (newCounterData.getCardLeft()) {
                case YELLOW :
                    card = 2;
                    break;
                case YR1P :
                    card = 4;
                    break;
                case YR2P :
                    card = 5;
                    break;
            }
        } else if (oldCounterData.hasTimeoutLeft() && !newCounterData.hasTimeoutLeft()) {
            f = "-";
            pl = 1;
            card = 1;
        } else if (oldCounterData.hasTimeoutRight() && !newCounterData.hasTimeoutRight()) {
            f = "-";
            pl = 3;   
            card = 1;            
        } else if (oldCounterData.getCardLeft().ordinal() > newCounterData.getCardLeft().ordinal()) {
            f = "-";
            pl = 1;
            switch (oldCounterData.getCardLeft()) {
                case YR2P :
                    card = 5;
                    break;
                case YR1P :
                    card = 4;
                    break;
                case YELLOW :
                    card = 2;
                    break;
            }
        } else if (oldCounterData.getCardRight().ordinal() > newCounterData.getCardRight().ordinal()) {
            f = "-";
            pl = 3;
            switch (oldCounterData.getCardLeft()) {
                case YR2P :
                    card = 5;
                    break;
                case YR1P :
                    card = 4;
                    break;
                case YELLOW :
                    card = 2;
                    break;
            }
        } else {
            System.out.println("Nothing has changed at table " + cm.mtTable);
        }

        StringBuilder sb = new StringBuilder();
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // pp
                .append(String.format("%d", cs + 1))
                // xx yy
                .append(String.format("%02d%02d", osh[0], osh[1]))
                // SERVICE
                .append(String.format("%d", s))
                // PLAYER
                .append(String.format("%d", pl))
                // CARD
                .append(String.format("%d", card))
                // f
                .append(f)
                // GAMETIME
                .append(gt)
                // MATCHTIME
                .append(mt)
                // IDMATCH
                .append(idmatches.get(cm.mtTable))                
        ;
        
        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("Card: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("Game = ").append(String.format("%d", cs + 1)).append(", ")
                .append("Res A = ").append(String.format("%02d", osh[0])).append(", ")
                .append("Res X = ").append(String.format("%02d", osh[1])).append(", ")
                .append("Service = ").append(String.format("%d", s)).append(", ")
                .append("Player = ").append(String.format("%d", pl)).append(", ")
                .append("Card = ").append(String.format("%d", card)).append(", ")
                .append("Game Time = ").append(gt).append(", ")
                .append("Match Time = ").append(mt).append(", ")
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1008;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    
    private void sendHistory(int counter, CounterData oldCounterData, CounterData newCounterData) {
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm == null) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, String.format("No match for table %d", counter + tableOffset));
            return;
        }
     
        boolean isDouble = (cm.cpType == 2 || cm.cpType == 3 || (cm.cpType == 4 && cm.nmType == 2));
                
        // current set, 0-based
        int cs = oldCounterData.getSetsLeft() + oldCounterData.getSetsRight();
        // points left / right
        int[] osh = oldCounterData.getSetHistory() == null ? new int[] {0, 0} : oldCounterData.getSetHistory()[cs];
        int[] nsh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        
        boolean isAdd = nsh[0] > osh[0] || nsh[1] > osh[1];
        
        // service
        int s = calculateService(isAdd ? oldCounterData : newCounterData, isDouble).ordinal();
        // point_to
        String pt = "";
        if (nsh[0] != osh[0])
            pt = "H";
        else if (nsh[1] != osh[1])
            pt = "A";
        // Strokes
        int strokes = 0;
        
        if (isAdd) {
            strokes = 2 * oldCounterData.getStrokes(); // 1 + (new java.util.Random()).nextInt(10);
            // + 1, if server makes the point. But who is the server?
            // If receiver (A) scores => +0 strokes
            // If server scores => +1 strokes
            if (oldCounterData.getServiceLeft() && pt.equals("H"))
                ++strokes;
            else if (oldCounterData.getServiceRight() && pt.equals("A"))
                ++strokes;
        }
        // Do, Undo
        String f = isAdd ? "+" : "-";
        String mt = formatDuration(cm.getRunningMatchTime());
        String gt = formatDuration(cm.getRunningGameTime(cs));
        
        StringBuilder sb = new StringBuilder();
        sb
                // AA BBB CC M
                .append(matchStrings.get(cm.mtTable))
                // pp
                .append(String.format("%d", cs + 1))
                // xx yy
                .append(String.format("%02d%02d", 
                        isAdd ? nsh[0] : osh[0], isAdd ? nsh[1] : osh[1]))
                // SERVICE
                .append(String.format("%d", s))
                // POINT_TO
                .append(pt)
                // STROKES
                .append(String.format("%03d", isAdd ? strokes : 0))
                // f
                .append(f)
                // GAMETIME
                .append(gt)
                // MATCHTIME
                .append(mt)
                // IDMATCH
                .append(idmatches.get(cm.mtTable))
        ;

        StringBuilder dbg = new StringBuilder();
        
        dbg
                .append("History: ")
                .append("Match String = ").append(matchStrings.get(cm.mtTable)).append(", ")
                .append("Game = ").append(String.format("%d", cs + 1)).append(", ")
                .append("Res A = ").append(String.format("%02d", nsh[0])).append(", ")
                .append("Res X = ").append(String.format("%02d", nsh[1])).append(", ")
                .append("Service = ").append(String.format("%d", s)).append(", ")
                .append("Point To = ").append(pt).append(", ")
                .append("Strokes = " ).append(String.format("%03d", strokes)).append(", ")
                .append("Flag = ").append(f).append(", ")
                .append("Game Time = ").append(gt).append(", ")
                .append("Match Time = ").append(mt).append(", ")
                .append("ID Match = ").append(idmatches.get(cm.mtTable)).append(", ")
        ;
        
        debugStream.println(dbg.toString());
        debugStream.flush();

        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1050;
        msg.data = sb.toString();
        
        sendMessage(msg);
    }
    
    
    private void sendMessage(Message msg) {
        synchronized (output) {
            output.add(msg);
            output.notifyAll();
        }
    }
    
    
    private void handlePhases(Message msg) {
        // [SOH]<<Id>>[DC3]2042|<<LAN>>|<<PHASE1>>#<<DESCRIPCIONPHASE1>>|…|<<PHASEn>>#<<DESCRIPCIONPHASEn>>|[EOT]
        // data starts with '|', so the first item is empty, we actually start with idx 1        
        // But set idx to 0, so we access it with ++idx
        int idx = 0;
        String[] parts = msg.data.split("\\|");
        String lang = parts[++idx];
        while (idx < parts.length - 1) {
            String[] part = parts[++idx].split("#");
            String phase = part[0];
            String description = part[1];
            
            // Put it into map
            phases.put(phase, description);
            
            System.out.println("" + idx + ": " + "Phase: " + phase + ", description: " + description);
        }
    }
    
    
    private void handleNextMatch(Message msg) {
        // [SOH]<<Id>>[DC3]
        // 2001|<<LAN>>|
        // <<AA>>|<<BBB>>|<<CC>>|<<M>>|
        // <<NOC1>>|<<NOC2>>|<<NOC3>>|<<NOC4>>|
        // <<NAME1>>|<<NAME2>>|<<NAME3>>|<<NAME4>>|
        // <<NAMETEAM1>>|<<NAMETEAM2>>|
        // <<PHASE>>|<<POOL>>|
        // <<JU>>|
        // <<V1>>|<<V2>>|
        // <<IDMATCH>>|[EOT]
        int idx = 0;
        String[] parts = msg.data.split("\\|");
        String lang = parts[++idx];
        String event = parts[++idx];
        int mtMatch = Integer.valueOf(parts[++idx]);
        // CC: code (MS, WD) of individual match
        ++idx;
        // M: match no of individual match
        ++idx;
        String plAnaName = parts[++idx];
        String plBnaName = parts[++idx];
        String plXnaName = parts[++idx];
        String plYnaName = parts[++idx];
        String plApsName = parts[++idx];
        String plBpsName = parts[++idx];
        String plXpsName = parts[++idx];
        String plYpsName = parts[++idx];
        // NAMETEAM1, NAMETEAM2
        ++idx;
        ++idx;
        String phase = parts[++idx];
        String pool = parts[++idx];
        int mtBestOf = Integer.valueOf(parts[++idx]);
        // V1, V2: team result
        ++idx;
        ++idx;
        String idmatch = parts[++idx];
        
        idmatches.put(msg.id, idmatch);
        matchStrings.put(msg.id, String.format("%s%03d  0", event, mtMatch));
        
        System.out.println("Table: " + msg.id + ", Event: " + event + ", phase: " + phase + ", pool: " + pool + ", match: " + mtMatch + ", plAnaName: " + plAnaName + ", plXnaName: " + plXnaName + ", idMatch: " + idmatch);
    }
    
    private static class Message {
        public int id;
        public int code;
        public String data;
        public boolean wantAnswer = false;
    };
    
    private String name = getClass().getSimpleName();

    private int fromTable = 1;
    private int toTable = 1;
    private int tableOffset = 1;
    
    public enum Type {
        FILE,
        NET
    };
    private Type type = Type.FILE;
    private String host;
    private int    port;
    private String inputFile;
    private String outputFile;
    private boolean debug = false;
    
    // Last state of CounterData
    private final Map<Integer, CounterData> currentCounterData = new java.util.HashMap<>();
    
    // Data to write
    private final List<Message> output = new java.util.ArrayList<>();
    
    private Timer timer = null;
    
    static byte SOH = 0x01;
    static byte DC3 = 0x13;
    static byte EOT = 0x04;

    private class CommThread extends Thread {
        
        private Socket socket = null;
        private java.io.InputStream is = null;
        private java.io.OutputStream os = null;
        
        private void sendMessage(Message msg) throws IOException {
            if (os == null)
                return;
            
            // Buffer bytes
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bos.write(SOH);
            bos.write(String.format("%02d", msg.id).getBytes(StandardCharsets.UTF_8));
            bos.write(DC3);
            bos.write(String.format("%04d", msg.code).getBytes(StandardCharsets.UTF_8));
            bos.write(msg.data.getBytes(StandardCharsets.UTF_8));
            bos.write(EOT);
            // Format logfile with line breaks
            if (type == Type.FILE)
                bos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            
            bos.writeTo(os);
            os.flush();
        }
        
        private Message nextMatchResponse = null;
        private Message readMessage() throws IOException {
            if (is == null)
                return null;
            
            StringBuilder sb = new StringBuilder();
            int c;
            
            do {
                c = is.read();
                // when we read from file return a good response after EOT
                if (c == -1) {
                    return type == Type.FILE ? nextMatchResponse : null;
                }
                
                if (Character.isWhitespace(c))
                    continue;
                sb.append(Character.toString(c));
            } while (c != EOT);
            
            String s = sb.toString();
            if ((c = s.charAt(0)) != SOH) {
                os.close();
                throw new IOException("Illegal start if header received");
            }

            if ((c = s.charAt(3)) != DC3) {
                os.close();
                throw new IOException("Illegal start of data received");                
            }
            
            if ((c = s.charAt(s.length() - 1)) != EOT) {
                os.close();
                throw new IOException("Illegal end of data received");                
            }
            
            // Read 2 bytes ID
            int id = Integer.valueOf(s.substring(1, 3));
            int code = Integer.valueOf(s.substring(4, 8));
            String data = s.substring(8, s.length() - 1);
            
            Message msg = new Message();
            msg.id = id;
            msg.code = code;
            msg.data = data;
            
            // when we read from file  a good response to return after EOL
            if (msg.code == 2001)
                nextMatchResponse = msg;
            
            return msg;
        }
        
        @Override
        public void run() {
            if (debug) {
                for (int i = fromTable; i <= toTable; ++i) {
                    idmatches.put(i, "<IDMATCH>");
                    matchStrings.put(i, "<AABBBCCM>");
                }
            }
            
            while (isEnabled()) {
                if (type == Type.NET) {
                    while (socket == null) {
                        try {
                            socket = new Socket();
                            socket.connect(new java.net.InetSocketAddress(host, port), 2000);
                            
                            Logger.getLogger(Atos.class.getName()).log(Level.INFO, "Connected to server " + host + " at " + port + " with local port " + socket.getLocalPort());
                            socket.setSoTimeout(2000);
                            is = socket.getInputStream();
                            os = socket.getOutputStream();
                        } catch (IOException ex) {
                            Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, "Conection to " + host + " at " + port + "failed", ex);
                            socket = null;
                            is = null;
                            os = null;
                            
                            break;
                        }
                    }
                } else {
                    if (is == null) {
                        try {
                            if (getInputFile() != null && !getInputFile().isEmpty())
                                is  = new java.io.FileInputStream(getInputFile());
                            else
                                is = new java.io.InputStream() {
                                    @Override
                                    public int read() throws IOException {
                                        return -1; // EOF
                                    }
                                };
                        } catch (IOException ex) {
                            Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                            is = null;
                        }
                    } 
                    
                    if (os == null) {
                        try {
                            if (getOutputFile() != null && !getOutputFile().isEmpty()) {
                                os = new java.io.FileOutputStream(getOutputFile().replace(
                                        "<DT>", new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date()))
                                );
                            } else {
                                os = new java.io.OutputStream() {
                                    @Override
                                    public void write(int b) throws IOException {
                                       // Nothing;
                                    }
                                };
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                            os = null;
                        }
                    }
                }
                
                if (is == null || os == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        
                    }
                    
                    continue;
                }
                
                synchronized (output) {
                    if (output.isEmpty()) {
                        try {
                            output.wait();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                    if (!output.isEmpty()) {
                        try {
                            Message msg = output.get(0);
                            // Remove msg before sending, or in case of IO errors it would be sent again
                            output.remove(0);
                            sendMessage(msg);
                            if (msg.wantAnswer) {
                                Message ret;

                                do {
                                    ret = readMessage();                                    
                                } while (ret != null && ret.code < 2000);

                                onMessage(ret);
                            }
                        } catch (SocketTimeoutException ex) {
                            onMessage(null);
                        } catch (IOException ex) {
                            Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                            socket = null;                        
                        }
                    }
                }
            }
            
            if (!isEnabled()) {
                try {
                    if (socket != null) {
                        socket.close();
                    } else {
                        if (is != null)
                            is.close();
                        if (os != null)
                             os.close();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);                        
                }
                
                socket = null;
                is = null;
                os = null;
            }
        }
    };
    
    CommThread commThread = null;
    
    private static final long INTERVAL = 10000; // 10s
}
