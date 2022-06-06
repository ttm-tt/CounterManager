/* Copyright (C) 2020 Christoph Theis */
package countermanager.liveticker.atos;

import countermanager.driver.CounterData;
import countermanager.liveticker.Liveticker;
import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;
import countermanager.prefs.Preferences;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
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
        REQ_PHASES,
        REQ_NEXT_MATCH,
        NTF_START_MATCH,
        NTF_END_MATCH,
        NTF_START_GAME,
        NTF_END_GAME,
        NTF_SCORE,
        NTF_CARD,
        NTF_SCORE_HISTORY,
    }
    
    private enum Service {
        NONE,
        A,
        B,
        X,
        Y
    }
    
    // Phases id <-> description
    private Map<String, String> phases = new java.util.HashMap<>();
    // Match identifiers table <-> String composed of cpName, mtMatch, nmType, mtMS
    private Map<Integer, String> matchStrings = new java.util.HashMap<>();
    // Atos' match id as table <-> idmatch
    private Map<Integer, String> idmatches = new java.util.HashMap<>();
    
    private Service calculateService(CounterData cd) {
        switch (cd.getServiceDouble()) {
            case 0 :
                if (cd.getServiceLeft())
                    return Service.A;
                if (cd.getServiceRight())
                    return Service.X;
                
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
    
    private Service calculateReturn(CounterData cd) {
        switch (cd.getServiceDouble()) {
            case 0 :
                if (cd.getServiceLeft())
                    return Service.X;
                if (cd.getServiceRight())
                    return Service.A;
                
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

        int[] osh = oldData.getSetHistory() == null ? new int[] {0, 0} : oldData.getSetHistory()[ocs];
        int[] nsh = newData.getSetHistory() == null ? new int[] {0, 0} : newData.getSetHistory()[ocs];
        
        // Start of match
        if (oldData.getGameMode() == CounterData.GameMode.RESET && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_MATCH);
        
        if (oldData.getGameMode() == CounterData.GameMode.WARMUP && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_MATCH);
        
        if (oldData.getGameMode() == CounterData.GameMode.WARMUP && newData.getGameMode() == CounterData.GameMode.RUNNING)
            res.add(WhatToSend.NTF_START_GAME);

        if (oldData.getGameMode() == CounterData.GameMode.RUNNING) {
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

        // Points
        if (osh[0] != nsh[0] || osh[1] != nsh[1])
            res.add(WhatToSend.NTF_SCORE);

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
        java.time.Duration d = java.time.Duration.ofMillis(t);
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

    @Override
    protected void loadProperties() {
        Preferences.loadProperties(this, this.getClass().getName(), true);
    }

    @Override
    protected void saveProperties() {
        Preferences.saveProperties(this, this.getClass().getName(), true);
    }
    
    private void onActivate(boolean isActive) {
        if (!isActive) 
            return;
        
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
    
    
    private void onCounterChanged(int counter) {
        CounterData newCounterData = CounterModel.getDefaultInstance().getCounterData(counter);
        CounterData oldCounterData = currentCounterData.get(counter);
        
        // Make sure we have the initial state
        if (oldCounterData == null) {
            oldCounterData = new CounterData();
        } 
        
        if (oldCounterData.equals(newCounterData))
            return;

        EnumSet<WhatToSend> es = compareCounterData(oldCounterData, newCounterData);
        
        if (es.contains(WhatToSend.NTF_START_MATCH))
            sendStartMatch(counter, oldCounterData, newCounterData);
                
        if (es.contains(WhatToSend.NTF_END_MATCH))
            sendEndMatch(counter, oldCounterData, newCounterData);
                
        if (es.contains(WhatToSend.NTF_START_GAME))
            sendStartGame(counter, oldCounterData, newCounterData);
                
        if (es.contains(WhatToSend.NTF_END_GAME))
            sendEndGame(counter, oldCounterData, newCounterData);
        
        if (es.contains(WhatToSend.NTF_CARD))
            sendCard(counter, oldCounterData, newCounterData);
        
        if (es.contains((WhatToSend.NTF_SCORE))) {
            sendScore(counter, oldCounterData, newCounterData);
            
            if (!es.contains(WhatToSend.NTF_CARD))
                sendHistory(counter, oldCounterData, newCounterData);
        }
                
        currentCounterData.put(counter, newCounterData);
    }
    
    
    private void onTimer() {
        for (int table = fromTable; table <= toTable; ++table) {
            CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(table - tableOffset);
            CounterData counterData = CounterModel.getDefaultInstance().getCounterData(table - tableOffset);
            
            if (counterMatch == null || counterData == null)
                continue;
            
            if (counterData.getGameMode() != CounterData.GameMode.RESET)
                continue;
            
            Message msg = new Message();
            msg.id = table;
            msg.code = 1001;
            msg.data = "ENG";
            msg.wantAnswer = true;
            
            sendMessage(msg);
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
    
    private static DateFormat daytimeFormat = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
    
    private void sendStartMatch(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (cm.startMatchTime == 0)
            cm.startMatchTime = System.currentTimeMillis();
        
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1005;
        msg.data = String.format( "%s%s%s", matchStrings.get(cm.mtTable), daytime, idmatch );
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendEndMatch(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1010;
        msg.data = String.format( "%s%s%s", matchStrings.get(cm.mtTable), daytime, idmatch );
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private void sendStartGame(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        int cs = cm.mtResA + cm.mtResX;
        if (cm.startGameTime[cs] == 0)
            cm.startGameTime[cs] = System.currentTimeMillis();
        
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1006;
        msg.data = String.format( "%s%d%d%d%s%s", matchStrings.get(cm.mtTable), cm.mtResA + cm.mtResX + 1, cm.mtResA, cm.mtResX, daytime, idmatch );
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    private void sendEndGame(int counter, CounterData oldCounterData, CounterData newCounterData) {
        if (commThread == null)
            return;
        
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);  
        
        // 0-base, but we are already in the next game
        int cs = cm.mtResA + cm.mtResX - 1;
        cm.endGameTime[cs] = System.currentTimeMillis();
        
        String daytime = daytimeFormat.format(new Date(System.currentTimeMillis()));
        String idmatch = idmatches.get(cm.mtTable);
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1009;
        msg.data = String.format( "%s%d%d%d%s%s", matchStrings.get(cm.mtTable), cm.mtResA + cm.mtResX + 1, cm.mtResA, cm.mtResX, daytime, idmatch );
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private void sendScore(int counter, CounterData oldCounterData, CounterData newCounterData) {
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
     
        // current set, 0-based
        int cs = newCounterData.getSetsLeft() + newCounterData.getSetsRight();
        // points left / right
        int[] sh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        // service, return
        int s = calculateService(newCounterData).ordinal();
        int r = calculateReturn(newCounterData).ordinal();
        String e = newCounterData.isExpedite() ? "Y" : "N";
        String mt = formatDuration(cm.getRunningMatchTime());
        String gt = formatDuration(cm.getRunningGameTime(cs+1));
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1007;
        msg.data = String.format("%s%d%02d%02d%d%d%s%s%s%s", 
                matchStrings.get(cm.mtTable), cs + 1, sh[0], sh[1], s, r, e, gt, mt, idmatches.get(cm.mtTable));
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private void sendCard(int counter, CounterData oldCounterData, CounterData newCounterData) {
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        // [SOH]<<Id>>[DC3]1008<<AA>><<BBB>><<CC>><<M>><<pp>><<xx>><<yy>><<SERVICE>><<PLAYER>><<CARD>><<f>><<GAMETIME>><<MATCHTIME>><<IDMATCH>>[EOT]
        
        // current set, 0-based
        int cs = oldCounterData.getSetsLeft() + oldCounterData.getSetsRight();
        // points left / right
        int[] osh = oldCounterData.getSetHistory() == null ? new int[] {0, 0} : oldCounterData.getSetHistory()[cs];
        int[] nsh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        // service
        int s = calculateService(oldCounterData).ordinal();
        // player and card
        int pl = 0;
        int card = 0;
        if (!oldCounterData.getTimeoutLeft() && newCounterData.getTimeoutRight()) {
            pl = 1;
            card = 1;
        } else if (!oldCounterData.getTimeoutRight() && newCounterData.getTimeoutRight()) {
            pl = 3;   
            card = 1;
        } else if (oldCounterData.getCardLeft().ordinal() > newCounterData.getCardLeft().ordinal()) {
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
        } else if (oldCounterData.getCardRight().ordinal() > newCounterData.getCardRight().ordinal()) {
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
        }
        
        // Strokes
        int strokes = 0;
        // Do, Undo
        String f = "+";
        String mt = formatDuration(cm.getRunningMatchTime());
        String gt = formatDuration(cm.getRunningGameTime(cs+1));
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1008;

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
        
        msg.data = sb.toString();
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void sendHistory(int counter, CounterData oldCounterData, CounterData newCounterData) {
        CounterModelMatch cm = CounterModel.getDefaultInstance().getCounterMatch(counter);
     
        // current set, 0-based
        int cs = oldCounterData.getSetsLeft() + oldCounterData.getSetsRight();
        // points left / right
        int[] osh = oldCounterData.getSetHistory() == null ? new int[] {0, 0} : oldCounterData.getSetHistory()[cs];
        int[] nsh = newCounterData.getSetHistory() == null ? new int[] {0, 0} : newCounterData.getSetHistory()[cs];
        // service
        int s = calculateService(oldCounterData).ordinal();
        // point_to
        String pt = "";
        if (nsh[0] > osh[0])
            pt = "H";
        else if (nsh[1] > osh[1])
            pt = "A";
        // Strokes
        int strokes = 0;
        // Do, Undo
        String f = "+";
        String mt = formatDuration(cm.getRunningMatchTime());
        String gt = formatDuration(cm.getRunningGameTime(cs+1));
        
        Message msg = new Message();
        msg.id = cm.mtTable;
        msg.code = 1050;
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
                // POINT_TO
                .append(pt)
                // STROKES
                .append(String.format("%03d", strokes))
                // f
                .append(f)
                // GAMETIME
                .append(gt)
                // MATCHTIME
                .append(mt)
                // IDMATCH
                .append(idmatches.get(cm.mtTable))
        ;
        msg.data = sb.toString();
        
        try {
            commThread.sendMessage(msg);
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
        }
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
        
        System.out.println("Event: " + event + ", phase: " + phase + ", pool: " + pool + ", match: " + mtMatch + ", plAnaName: " + plAnaName + ", plXnaName: " + plXnaName + ", idMatch:" + idmatch);
    }
    
    private static class Message {
        public int id;
        public int code;
        public String data;
        public boolean wantAnswer = false;
    };
    
    private int fromTable = 1;
    private int toTable = 1;
    private int tableOffset = 1;
    
    private String name = getClass().getSimpleName();

    private String host;
    private int    port;
    
    // Last state of CounterData
    private Map<Integer, CounterData> currentCounterData = new java.util.HashMap<>();
    
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
            os.write(SOH);
            os.write(String.format("%02d", msg.id).getBytes(StandardCharsets.UTF_8));
            os.write(DC3);
            os.write(String.format("%04d", msg.code).getBytes(StandardCharsets.UTF_8));
            os.write(msg.data.getBytes(StandardCharsets.UTF_8));
            os.write(EOT);
            // Format logfile with line breaks
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        
        private Message nextMatchResponse = null;
        private Message readMessage() throws IOException {
            StringBuilder sb = new StringBuilder();
            int c;
            
            do {
                c = is.read();
                // when we read from file return a good response after EOL
                if (c == -1)
                    return nextMatchResponse;
                
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
                throw new IOException("Illegal end ff data received");                
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
            if (msg.code == 2042)
                nextMatchResponse = msg;
            
            return msg;
        }
        
        @Override
        public void run() {
            while (true) {
/*                
                while (isEnabled() && socket == null) {
                    try {
                        socket = new Socket(host, port);
                    } catch (IOException ex) {
                        Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                        socket = null;
                    }
                }
*/
                while (isEnabled() && is == null) {
                    try {                    
                        is  = new java.io.FileInputStream("..\\2022.03.17.13.26.18.SERVER_TCP_1_CLIENT_1.log");
                        os = new java.io.FileOutputStream("..\\test.log");
                    } catch (IOException ex) {
                        Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                        is = null;
                        os = null;
                    }
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
                            sendMessage(msg);
                            if (msg.wantAnswer) {
                                Message ret;

                                do {
                                    ret = readMessage();                                    
                                } while (ret != null && ret.code < 2000);

                                onMessage(ret);
                            }
                            output.remove(0);
                        } catch (IOException ex) {
                            Logger.getLogger(Atos.class.getName()).log(Level.SEVERE, null, ex);
                            socket = null;                        
                        }
                    }
                }
            }
        }
    };
    
    CommThread commThread = null;
    
    private static long INTERVAL = 1000;
}
