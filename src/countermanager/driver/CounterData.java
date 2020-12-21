/* Copyright (C) 2020 Christoph Theis */

package countermanager.driver;
/*
 * CounterData.java
 *
 * Created on 1. Januar 2007, 17:50
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Christoph Theis
 */
public class CounterData {
    
    // Get the alert text or null
    public String getAlertText() {return null;}
    
    // Get info if we are in expedite system
    public boolean getTimegameMode() {return false;}

    // Get info if we cannot enter expedite system
    public boolean getTimegameBlock() {return false;}
    
    public enum GameMode  {RESET, RUNNING, WARMUP, END, INACTIVE}
    
    public enum TimeMode  {NONE, PREPARE, MATCH, BREAK, TIMEOUT, INJURY}
    
    private boolean   alert;
    private GameMode  gameMode;
    private boolean   serviceLeft;
    private boolean   serviceRight;
    private boolean   abandonOrAbort;
    private int       gameNr;
    private int       playerNrLeft;
    private int       playerNrRight;
    private int       bestOf;
    private TimeMode  timeMode;
    private boolean   timeoutLeft;
    private boolean   timeoutRight;
    private boolean   timeoutLeftRunning;
    private boolean   timeoutRightRunning;
    private boolean   injuredLeft;
    private boolean   injuredRight;
    private boolean   injuredLeftRunning;
    private boolean   injuredRightRunning;
    private int       time;
    private int       setsLeft;
    private int       setsRight;
    private int[][]   setHistory;
    private boolean   swapped = false;
    private long      updateTime;
    
    // Bei Erweiterungen equals beachten

    @Override
    public String toString() {
        java.lang.reflect.Field[] fields = this.getClass().getDeclaredFields();
        StringBuilder sb = new StringBuilder();

            for (int i = 0; i < fields.length; i++) {
                try {
                    if (sb.length() > 0)
                        sb.append(", ");
                    sb.append("" + fields[i].getName() + "=" + fields[i].get(this));
                } catch (Throwable t) {
                    
                }
            }

        return sb.toString();
    }


    /** Creates a new instance of CounterData */
    public CounterData() {
    }

    public boolean getAlert() {
        return alert;
    }

    public void setAlert(boolean alert) {
        this.alert = alert;
    }
    
    
    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }
    
    public boolean getAbandonOrAbort() {
        return abandonOrAbort;
    }

    public void setAbandonOrAbort(boolean abandonOrAbort) {
        this.abandonOrAbort = abandonOrAbort;
    }

    public int getGameNr() {
        return gameNr;
    }

    public void setGameNr(int gameNr) {
        this.gameNr = gameNr;
    }

    public int getPlayerNrLeft() {
        return playerNrLeft;
    }

    public void setPlayerNrLeft(int playerNrLeft) {
        this.playerNrLeft = playerNrLeft;
    }

    public int getPlayerNrRight() {
        return playerNrRight;
    }

    public void setPlayerNrRight(int playerNrRight) {
        this.playerNrRight = playerNrRight;
    }
    
    public int getBestOf() {
        return bestOf;
    }
    
    public void setBestOf(int bestOf) {
        this.bestOf = bestOf;
    }

    public int getSetsLeft() {
        return setsLeft;
    }

    public void setSetsLeft(int setsLeft) {
        this.setsLeft = setsLeft;
    }

    public int getSetsRight() {
        return setsRight;
    }

    public void setSetsRight(int setsRight) {
        this.setsRight = setsRight;
    }

    public int[][] getSetHistory() {
        return setHistory;
    }

    public void setSetHistory(int[][] setHistory) {
        this.setHistory = setHistory;
    }

    public boolean getServiceLeft() {
        return serviceLeft;
    }
    
    public boolean getServiceRight() {
        return serviceRight;
    }

    public void setServiceLeft(boolean serviceLeft) {
        this.serviceLeft = serviceLeft;
    }

    public void setServiceRight(boolean serviceRight) {
        this.serviceRight = serviceRight;
    }

    public TimeMode getTimeMode() {
        return timeMode;
    }

    public void setTimeMode(TimeMode timeMode) {
        this.timeMode = timeMode;
    }

    public boolean getTimeoutLeft() {
        return timeoutLeft;
    }
    
    public void setTimeoutLeft(boolean timeout) {
        timeoutLeft = timeout;
    }
    
    
    public boolean getTimeoutRight() {
        return timeoutRight;
    }
    
    public void setTimeoutRight(boolean timeout) {
        timeoutRight = timeout;
    }
    
    public void setTimeoutLeftRunning() {
        this.timeoutLeftRunning = true;
    }
    
    public void setTimeoutRightRunning() {
        this.timeoutRightRunning = true;
    }
    
    public boolean isTimeoutLeftRunning() {
        return timeMode == TimeMode.TIMEOUT && timeoutLeftRunning;
    }
    
    
    public boolean isTimeoutRightRunning() {
        return timeMode == TimeMode.TIMEOUT && timeoutRightRunning;
    }
        
    public boolean getInjuredLeft() {
        return injuredLeft;
    }
    
    public void setInjuredLeft(boolean injured) {
        injuredLeft = injured;
    }
    
    
    public boolean getInjuredRight() {
        return injuredRight;
    }
    
    public void setInjuredRight(boolean injured) {
        injuredRight = injured;
    }
    
    
    public void setInjuredLeftRunning() {
        this.injuredLeftRunning = true;
    }
    
    public void setInjuredRightRunning() {
        this.injuredRightRunning = true;
    }
    
    public boolean isInjuredLeftRunning() {
        return timeMode == TimeMode.INJURY && injuredLeftRunning;
    }    
    
    public boolean isInjuredRightRunning() {
        return timeMode == TimeMode.INJURY && injuredRightRunning;
    }
    
    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
    
    public long getUpdateTime() {
        return updateTime;
    }
       
    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
    
    
    // Returns if the counter is locked
    public boolean isLocked() {
        return false;
    }
    
    // Returns the swapped flag, i.e. if data is swapped w.r.t. match
    public boolean isSwapped() {
        return swapped;
    }
    
    public static boolean equalSetHistory(int[][] s1, int[][] s2) {
        if (s1 == s2)
            return true;
        
        if (s1 == null || s2 == null)
            return false;
        
        if (s1.length != s2.length)
            return false;
        
        for (int i = 0; i < s1.length; i++) {
            if (s1[i][0] != s2[i][0])
                return false;
            
            if (s1[i][1] != s2[i][1])
                return false;
        }
        
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof CounterData) )
            return false;
        
        CounterData cd = (CounterData) obj;
        
        // Vergleiche Felder, aber nicht updateTime (internal use)
        return 
                abandonOrAbort == cd.abandonOrAbort &&
                alert == cd.alert &&
                bestOf == cd.bestOf &&
                gameMode.equals(cd.gameMode) &&
                gameNr == cd.gameNr &&
                injuredLeft == cd.injuredLeft &&
                injuredLeftRunning == cd.injuredLeftRunning &&
                injuredRight == cd.injuredRight &&
                playerNrLeft == cd.playerNrLeft &&
                playerNrRight == cd.playerNrRight &&
                serviceLeft == cd.serviceLeft &&
                serviceRight == cd.serviceRight &&
                equalSetHistory(setHistory, cd.setHistory) &&
                setsLeft == cd.setsLeft &&
                setsRight == cd.setsRight &&
                timeMode.equals(cd.timeMode) &&
                timeoutLeft == cd.timeoutLeft &&
                timeoutRight == cd.timeoutRight &&
                time == cd.time &&
                swapped == cd.swapped
            ;                
    }
}
