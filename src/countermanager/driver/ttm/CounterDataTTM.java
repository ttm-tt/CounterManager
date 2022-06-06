/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.driver.ttm;

/**
 *
 * @author chtheis
 */
public class CounterDataTTM extends countermanager.driver.CounterData {
    private String  alertText;
    private int     gameTime;
    private boolean locked;
    
    @Override
    public String getAlertText() {
        return alertText;
    }
    
    @Override
    public boolean getTimegameMode() {
        return getGameMode() == GameMode.RUNNING && getTimeMode() == TimeMode.NONE;
    }
    
    @Override
    public boolean getTimegameBlock() {
        return getGameMode() == GameMode.RUNNING && getTimeMode() == TimeMode.NONE;
    }
    
    @Override
    public void setLocked(boolean b) {
        locked = b;
    }
    
    @Override
    public boolean isLocked() {
        return locked;
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof CounterDataTTM) )
            return false;
        
        if (!super.equals(obj))
            return false;
        
        CounterDataTTM cd = (CounterDataTTM) obj;
        
        // Vergleiche Felder
        return 
            (alertText == null ? cd.alertText == null : alertText.equals(cd.alertText)) &&
            gameTime == cd.gameTime &&
            locked == cd.locked
        ;
    }
}
