/* Copyright (C) 2020 Christoph Theis */

/*
 * CounterModelItem.java
 *
 * Created on 1. Januar 2007, 15:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package countermanager.model;

import countermanager.driver.CounterConfig;
import countermanager.driver.CounterData;
import countermanager.driver.CounterDriver;
import countermanager.liveticker.LivetickerAdmininstration;
import java.io.IOException;

/**
 *
 * @author Christoph Theis
 */
 // Package private
 class CounterModelItem {
     
    private int counter;
        
    private String version;
    private String serialNumber;
    private CounterConfig counterConfig;
    private CounterData counterData;
    private CounterModelMatch match;
    private boolean active = true;
    private int errorCount = 0;
    private long errorTime = 0;
    private long endTime = 0;
    private boolean checked = false;
    private boolean forced = false;
    
    private static final int UNKNOWN_MTNR = 0xFFFF;
    private static final int UNKNOWN_PLNR_LEFT = 0xFFFF;
    private static final int UNKNOWN_PLNR_RIGHT = 0xFFFE;
    
    private CounterModel counterModel = CounterModel.getDefaultInstance();

    /** Creates a new instance of CounterModelItem */
    public CounterModelItem(int counter) {
        this.counter = counter;
    }
    
    public void refreshConfiguration() throws IOException {
        CounterDriver.getVersion(counter);
        CounterDriver.getSerialNumber(counter);
        // Not necessary. And we had to wait 2s, if there is no callbak function.
        // CounterDriver.setDateTime(counter);
        CounterDriver.getCounterConfig(counter);
    }
    
    
    public void refreshData() throws IOException {
        CounterDriver.getCounterData(counter);
    }
    
    
    public void forceMatch(CounterModelMatch counterMatch) throws IOException {
        reset();

        setMatch(counterMatch);        
        
        forced = true;        
    }
    
    
    public void forceResult(int[][] result) throws IOException {
        if (active)
            CounterDriver.setResult(counter, result);
    }
    
    
    public void reset() throws IOException {
        match = null;
        counterData = null;
        errorCount = 0;
        forced = false;
        
        if (true || active) {            
            CounterDriver.resetCounter(counter);                   
        }        

        endTime = 0;        
    }
    
    
    public void swapPlayer() throws IOException {
        CounterDriver.switchPlayerNumbers(counter);
    }
          
    
    public String getVersion() {
        try {
            CounterDriver.getVersion(counter);
            
            if (version == null)
                Thread.sleep(100);
        } catch (Throwable t) {
            
        }
        
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSerialNumber() {
        try {            
            CounterDriver.getSerialNumber(counter);
            
            if (serialNumber == null)
                Thread.sleep(100);
        } catch(Throwable t) {
            
        }
        
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public CounterConfig getCounterConfig() {
        try {
            CounterDriver.getCounterConfig(counter);
            
            if (counterConfig == null)
                Thread.sleep(100);
        } catch (Throwable t) {
            
        }
        
        return counterConfig;
    }

    public void setCounterConfig(CounterConfig counterConfig) {
        this.counterConfig = counterConfig;
    }

    public CounterData getCounterData() {
        return counterData;
    }
    
    
    synchronized public int[][] getResult() {
        if (counterData == null || match == null)
            return null;
        
        if (counterData.getGameMode() == CounterData.GameMode.RESET ||
            counterData.getGameMode() == CounterData.GameMode.WARMUP)
            return null;
        
        return counterData.getSetHistory();
    }

    // Set new match data from a counter.
    synchronized public void setCounterData(CounterData counterData) { 
        // Zuerst das swapped flag setzen
        if (counterData != null && match != null) {            
            if ( counterData.getPlayerNrLeft() == UNKNOWN_PLNR_RIGHT ||
                 counterData.getPlayerNrLeft() == match.plX.plNr )
                counterData.swap();
        }
        
        // Wenn sich die Daten nicht geaendert haben, die Recivetime vom letzten Mal uebernehmen.
        // Ansonsten ist es die aktuelle Zeit.
        // Objekt trotzdem uebernehmen, weil hier die Zeit runtergezaehlt wird.
        if (counterData != null && counterData.getUpdateTime() == 0) {        
            if (this.counterData != null && this.counterData.equals(counterData)) 
                counterData.setUpdateTime(this.counterData.getUpdateTime());
            else 
                counterData.setUpdateTime(System.currentTimeMillis());
        }

        // Ignore new data but accept null if we are inactive.
        if (!active && counterData != null)
            return;
        
        CounterData oldCounterData = this.counterData;        
        this.counterData = counterData;
        
        if (oldCounterData == null || counterData == null ||
            oldCounterData.getGameNr() != counterData.getGameNr())
            checked = false;
        
        // Nothing to do if the old data were cleared, e.g. for a refresh
        if (counterData == null)
            return;
        
        // Check if this is the right match
        if (counterData != null && match != null) {
            int plLeftPlNr = counterData.getPlayerNrLeft();
            int plRightPlNr = counterData.getPlayerNrRight();
            int mtNr = counterData.getGameNr();

            if (true) {
                if (plLeftPlNr == UNKNOWN_PLNR_LEFT)
                    plLeftPlNr = match.plA.plNr;
                else if (plLeftPlNr == UNKNOWN_PLNR_RIGHT)
                    plLeftPlNr = match.plX.plNr;

                if (plRightPlNr == UNKNOWN_PLNR_LEFT)
                    plRightPlNr = match.plA.plNr;
                else if (plRightPlNr == UNKNOWN_PLNR_RIGHT)
                    plRightPlNr = match.plX.plNr;

                if (mtNr == UNKNOWN_MTNR)
                    mtNr = match.mtMS > 1 ? match.mtMS : match.mtNr;
            }

            if (match.mtMS > 1 && mtNr != match.mtMS ||
                match.mtMS <= 1 && mtNr != match.mtNr)
                return;
            
            if (plLeftPlNr > 0 && plLeftPlNr != match.plA.plNr &&
                plRightPlNr > 0 && plRightPlNr != match.plA.plNr ||
                plLeftPlNr > 0 && plLeftPlNr != match.plX.plNr &&
                plRightPlNr > 0 && plRightPlNr != match.plX.plNr) {

                return;
            }
        }

        // Transition from != RESET to RESET clears the match
        if (!forced && oldCounterData != null && 
            oldCounterData.getGameMode() != counterData.getGameMode() && 
            counterData.getGameMode() == CounterData.GameMode.RESET) {
            
            match = null;
            
            // And nothing else to do
            return;
        }
        
        // Running / just finished match changes (different results or w/o-flag):
        // Update the result
        if ( (counterData.getGameMode() == CounterData.GameMode.RUNNING ||
              counterData.getGameMode() == CounterData.GameMode.END) &&
             (oldCounterData == null || oldCounterData.getAbandonOrAbort() != counterData.getAbandonOrAbort() ||
              !CounterData.equalSetHistory(oldCounterData.getSetHistory(), counterData.getSetHistory())
             )
           ) {
            
            counterModel.updateResult(counter);
            
            return;
        }
        
        // Finished match: mark end time
        if (counterData.getGameMode() == CounterData.GameMode.END) {
            if (endTime == 0 || oldCounterData == null || oldCounterData.getGameMode() != CounterData.GameMode.END) {
                if (CounterDriver.isDebug())
                    System.err.println("Set endTime from " + endTime + " to ct");
                endTime = System.currentTimeMillis();
            }
        }
        else if (endTime != 0) {
            if (CounterDriver.isDebug())
                System.err.println("Reset endTime from ct to 0");
            endTime = 0;        
        }
    }
    
    public void setMatch(CounterModelMatch match) throws IOException {

        // If forced ignore
        if (forced && this.match != null && (match == null || this.match.mtNr != match.mtNr || this.match.mtMS != match.mtMS))
            return;
        
        // If active and no data from counter, accept match.
        // If this ia not the right match, we will take care abouzt it later.
        // But with the match we can display it on the liveticker
        if (active && counterData == null) {            
            this.match = match;
            return;
        }
        
        // If we are inactive, accept and return
        if (!active) {
            this.match = match;
            return;
        }
        
        // Starting here counterData must be != null

        // Wait until reset
        if (counterData.getGameMode() == CounterData.GameMode.END) {
            if (CounterDriver.isDebug())
                System.err.println("Ignore: wrong state (gameMode=" + counterData.getGameMode().toString() + ")");
            return;
        }
        
        CounterModelMatch oldMatch = this.match;

        // After a match started we accept changes only if:
        // - the current match is null (else we must wait for the e-results or score sheet)
        // - the new match is not null (else the match has changed for sure)
        // - match does not change compared to the counter (we cannot touch a counting system)
        
        // XXX CounterData.playerNrXXX ebenfalls ein int machen? Oder muss CounterData.gameNr ein short sein
        int plAplNr = match != null && match.plA.plNr > 0 ? match.plA.plNr : UNKNOWN_PLNR_LEFT;
        int plXplNr = match != null && match.plX.plNr > 0 ? match.plX.plNr : UNKNOWN_PLNR_RIGHT;
        int matchNr =  match == null ? UNKNOWN_MTNR : (match.mtMS > 1 ? match.mtMS : match.mtNr);
        
        // Don't do anything if this is the wrong match
        if (counterData.getGameMode() != CounterData.GameMode.RESET) {
            if (match == null || counterData.getGameNr() != matchNr)
                return;
        }
        
        boolean needPlayersUpdate = 
                oldMatch == null && match != null;
        
        needPlayersUpdate |=
                 match != null && (
                    counterData.getPlayerNrLeft() != plAplNr  && 
                    counterData.getPlayerNrRight() != plAplNr ||
                    counterData.getPlayerNrLeft() != plXplNr  &&
                    counterData.getPlayerNrRight() != plXplNr ||
                    counterData.getBestOf() != match.mtBestOf ||
                    counterData.getGameNr() != matchNr
                );
        
        // Or Match number etc has changed
        needPlayersUpdate |=
            match != null && oldMatch != null && (
                oldMatch.plA.plNr != match.plA.plNr ||
                oldMatch.plB.plNr != match.plB.plNr ||                    
                oldMatch.plX.plNr != match.plX.plNr ||
                oldMatch.plY.plNr != match.plY.plNr ||
                oldMatch.mtNr != match.mtNr || 
                oldMatch.mtMS != match.mtMS ||
                oldMatch.mtBestOf != match.mtBestOf
            );
        
        // Or the timestamp has changed
        needPlayersUpdate |=
                match != null && oldMatch != null &&
                   oldMatch.getMtTimestamp() != match.getMtTimestamp();
        
        // Accept new match here after all the checks
        this.match = match;
        
        // But only update counter if not running.
        if (needPlayersUpdate && counterData.getGameMode() == CounterData.GameMode.RESET) {
            // this.match must be != null
            // CounterDriver.setPlayerNumbers(counter, match.plA.plNr, match.plX.plNr);

            if (CounterDriver.isDebug())
                System.err.println("Update counter data");
            CounterDriver.setGameData(counter, match);
        }
    }
    
    public CounterModelMatch getMatch() {
        return match;
    }
    
    
    public void setActive(boolean active) {
        this.active = active;
        
        // Remove counterData if counter is set to inactive
        if (!active) {
            counterData = null;
        }
    }
    
    public boolean isActive() {
        return active;
    }
    
    
    public void setLivetickerActive(boolean active) {
        LivetickerAdmininstration.setLivetickerEnabled(counter, active);
    }
    
    public boolean isLivetickerActive() {
        return !LivetickerAdmininstration.isLivetickerDisabled(counter);
    }
    
    public boolean isMatchRunning(int mtnr, int mtms) {
        // If this is the correct match?
        if (match == null || match.mtNr != mtnr || match.mtMS != mtms)
            return false;
        
        // And do we have data?
        if (counterData == null)
            return false;
        
        // And is the state RUNNING?
        return counterData.getGameMode() == CounterData.GameMode.RUNNING;
    }
    
    // Returns who has the service: -1 for A, +1 for X, 0 for none
    public int getServiceAX(int mtnr, int mtms) {
        // Validate match
        if (match == null || counterData == null || counterData.getGameMode() != CounterData.GameMode.RUNNING)
            return 0;
        
        // If this is the correct match?
        if (match.mtNr != mtnr || match.mtMS != mtms)
            return 0;
        
        int ax = 0;
        if (counterData.getServiceLeft())
            ax = -1;
        else if (counterData.getServiceRight())
            ax = +1;
        
        if (counterData.getPlayerNrLeft() == UNKNOWN_PLNR_RIGHT)
            ax = -ax;
        else if (counterData.getPlayerNrRight() == UNKNOWN_PLNR_LEFT)
            ax = -ax;
        else if (match.plA != null && counterData.getPlayerNrRight()== (match.plA.plNr % 10000))
            ax = -ax;
        else if (match.plX != null && counterData.getPlayerNrLeft() == (match.plX.plNr % 10000))
            ax = -ax;
        
        return ax;
    }

    /**
     * @return the errorCount
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * @param errorCount the errorCount to set
     */
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    /**
     * @return the errorTime
     */
    public long getErrorTime() {
        return errorTime;
    }

    /**
     * @param errorTime the errorTime to set
     */
    public void setErrorTime(long errorTime) {
        this.errorTime = errorTime;
    }
    
    public long getEndTime() {
        return endTime;
    }        

    /**
     * @return the checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * @param checked the checked to set
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }
    
    public boolean isSwapped() {
        if (counterData != null)
            return counterData.isSwapped();
        else
            return false;
    }
        
    public boolean isForced() {
        return forced;
    }
    
    public void setLocked(boolean locked) {
        if (locked)
            CounterDriver.lockScreen(counter);
        else
            CounterDriver.unlockScreen(counter);
    }
    
    public boolean isLocked() {
        return counterData != null && counterData.isLocked();
    }
}
