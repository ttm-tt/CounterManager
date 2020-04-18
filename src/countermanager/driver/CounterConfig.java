/* Copyright (C) 2020 Christoph Theis */

/*
 * CounterConfig.java
 *
 * Created on 1. Januar 2007, 18:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package countermanager.driver;

/**
 *
 * @author Administrator
 */
public class CounterConfig {
    
    public enum AfterReset {BESTOF, MINUS, BLANK, TABLE}
    
    private String  setTime = "10:00";
    private boolean setTimeButtonPress = false;
    private String  timeoutTime = "01:00";
    private boolean timeoutButtonPress = false;
    private String  warmupTime = "02:00";
    private boolean warmupButtonPress = false;
    private String  setBreakTime = "01:00";
    private boolean setBreakButtonPress = false;
    private String  verletzungsPause = "10:00";
    private boolean verletzungsPauseButtonPress = false;
    private int     bestOf = 5;
    private int     maxPoints = 11;
    private int     setPointsOffset = 2;
    private int     sideSwitchLastSet = 5;
    private int     timeStopAt = 9;
    private int     aufschlagWechsel = 2;
    private int     entprellung = 5;
    private AfterReset  afterReset = AfterReset.TABLE;
    private int     displayOff = 0;
    private boolean lockReset = false;
    private boolean lockKeys = false;
    private boolean afterResetStateOff = false;
    private boolean displayEAModus = false;
    private boolean displayWarmupTime = false;
    private boolean displaySetTime = false;
    private boolean displayTimeoutTime = false;
    private boolean displaySetBreakTime = false;
    private boolean displayVerletzungsPause = false;
    private boolean displayMatchEnd = false;
    private boolean showTimeoutAsOn = false;

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


    /** Creates a new instance of CounterConfig */
    public CounterConfig() {
    }

    public String getSetTime() {
        return setTime;
    }

    public void setSetTime(String setTime) {
        this.setTime = setTime;
    }

    public boolean getSetTimeButtonPress() {
        return isSetTimeButtonPress();
    }

    public void setSetTimeButtonPress(boolean setTimeButtonPress) {
        this.setTimeButtonPress = setTimeButtonPress;
    }

    public String getTimeoutTime() {
        return timeoutTime;
    }

    public void setTimeoutTime(String timeoutTime) {
        this.timeoutTime = timeoutTime;
    }

    public boolean getTimeoutButtonPress() {
        return isTimeoutButtonPress();
    }

    public void setTimeoutButtonPress(boolean timeoutButtonPress) {
        this.timeoutButtonPress = timeoutButtonPress;
    }

    public String getWarmupTime() {
        return warmupTime;
    }

    public void setWarmupTime(String warmupTime) {
        this.warmupTime = warmupTime;
    }

    public boolean getWarmupButtonPress() {
        return isWarmupButtonPress();
    }

    public void setWarmupButtonPress(boolean warmupButtonPress) {
        this.warmupButtonPress = warmupButtonPress;
    }

    public String getSetBreakTime() {
        return setBreakTime;
    }

    public void setSetBreakTime(String setBreakTime) {
        this.setBreakTime = setBreakTime;
    }

    public boolean getSetBreakButtonPress() {
        return isSetBreakButtonPress();
    }

    public void setSetBreakButtonPress(boolean setBreakButtonPress) {
        this.setBreakButtonPress = setBreakButtonPress;
    }

    public String getVerletzungsPause() {
        return verletzungsPause;
    }

    public void setVerletzungsPause(String verletzungsPause) {
        this.verletzungsPause = verletzungsPause;
    }

    public boolean getVerletzungsPauseButtonPress() {
        return isVerletzungsPauseButtonPress();
    }

    public void setVerletzungsPauseButtonPress(boolean verletzungsPauseButtonPress) {
        this.verletzungsPauseButtonPress = verletzungsPauseButtonPress;
    }

    public int getBestOf() {
        return bestOf;
    }

    public void setBestOf(int bestOf) {
        this.bestOf = bestOf;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public int getSetPointsOffset() {
        return setPointsOffset;
    }

    public void setSetPointsOffset(int setPointsOffset) {
        this.setPointsOffset = setPointsOffset;
    }

    public int getSideSwitchLastSet() {
        return sideSwitchLastSet;
    }

    public void setSideSwitchLastSet(int sideSwitchLastSet) {
        this.sideSwitchLastSet = sideSwitchLastSet;
    }

    public int getTimeStopAt() {
        return timeStopAt;
    }

    public void setTimeStopAt(int timeStopAt) {
        this.timeStopAt = timeStopAt;
    }

    public int getAufschlagWechsel() {
        return aufschlagWechsel;
    }

    public void setAufschlagWechsel(int aufschlagWechsel) {
        this.aufschlagWechsel = aufschlagWechsel;
    }

    public int getEntprellung() {
        return entprellung;
    }

    public void setEntprellung(int entprellung) {
        this.entprellung = entprellung;
    }

    public AfterReset getAfterReset() {
        return afterReset;
    }

    public void setAfterReset(AfterReset afterReset) {
        this.afterReset = afterReset;
    }

    public boolean isShowTimeoutAsOn() {
        return showTimeoutAsOn;
    }

    public void setShowTimeoutAsOn(boolean showTimeoutAsOn) {
        this.showTimeoutAsOn = showTimeoutAsOn;
    }

    public boolean isLockReset() {
        return lockReset;
    }

    public void setLockReset(boolean lockReset) {
        this.lockReset = lockReset;
    }

    public int getDisplayOff() {
        return displayOff;
    }

    public void setDisplayOff(int displayOff) {
        this.displayOff = displayOff;
    }

    public boolean isLockKeys() {
        return lockKeys;
    }

    public void setLockKeys(boolean lockKeys) {
        this.lockKeys = lockKeys;
    }

    public boolean isAfterResetStateOff() {
        return afterResetStateOff;
    }

    public void setAfterResetStateOff(boolean afterResetStateOff) {
        this.afterResetStateOff = afterResetStateOff;
    }

    public boolean isDisplayEAModus() {
        return displayEAModus;
    }

    public boolean isDisplayWarmupTime() {
        return displayWarmupTime;
    }

    public boolean isDisplaySetTime() {
        return displaySetTime;
    }

    public boolean isDisplaySetBreakTime() {
        return displaySetBreakTime;
    }

    public boolean isDisplayVerletzungsPause() {
        return displayVerletzungsPause;
    }

    public boolean isDisplayMatchEnd() {
        return displayMatchEnd;
    }

    public boolean isSetTimeButtonPress() {
        return setTimeButtonPress;
    }

    public boolean isTimeoutButtonPress() {
        return timeoutButtonPress;
    }

    public boolean isWarmupButtonPress() {
        return warmupButtonPress;
    }

    public boolean isSetBreakButtonPress() {
        return setBreakButtonPress;
    }

    public boolean isVerletzungsPauseButtonPress() {
        return verletzungsPauseButtonPress;
    }

    public void setDisplayEAModus(boolean displayEAModus) {
        this.displayEAModus = displayEAModus;
    }

    public void setDisplayWarmupTime(boolean displayWarmupTime) {
        this.displayWarmupTime = displayWarmupTime;
    }

    public void setDisplaySetTime(boolean displaySetTime) {
        this.displaySetTime = displaySetTime;
    }

    public void setDisplaySetBreakTime(boolean displaySetBreakTime) {
        this.displaySetBreakTime = displaySetBreakTime;
    }

    public void setDisplayVerletzungsPause(boolean displayVerletzungsPause) {
        this.displayVerletzungsPause = displayVerletzungsPause;
    }

    public void setDisplayMatchEnd(boolean displayMatchEnd) {
        this.displayMatchEnd = displayMatchEnd;
    }

    public boolean isDisplayTimeoutTime() {
        return displayTimeoutTime;
    }

    public void setDisplayTimeoutTime(boolean displayTimeoutTime) {
        this.displayTimeoutTime = displayTimeoutTime;
    }
    
}
