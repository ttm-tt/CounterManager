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
    public enum Cards {NONE, YELLOW, YR1P, YR2P }
    
    private String  alertText;
    private boolean expedite;
    private int     gameTime;
    private Cards   cardLeft = Cards.NONE;
    private Cards   cardRight = Cards.NONE;
    private int     serviceDouble;
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
    
    public void setCardLeft(Cards card) {
        cardLeft = card;
    }
    
    public Cards getCardLeft() {
        return cardLeft;
    }
    
    public void setCardRight(Cards card) {
        cardRight = card;
    }
    
    public Cards getCardRight() {
        return cardRight;
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
            expedite == cd.expedite &&
            gameTime == cd.gameTime &&
            cardLeft.equals(cd.cardLeft) &&
            cardRight.equals(cd.cardRight) &&
            serviceDouble == cd.serviceDouble &&
            locked == cd.locked
        ;
    }
}
