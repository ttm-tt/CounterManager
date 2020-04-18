/* Copyright (C) 2020 Christoph Theis */

/*
 * ICounterCallback.java
 *
 * Created on 7. Januar 2007, 19:32
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package countermanager.driver;

/**
 *
 * @author Christoph Theis
 */
public interface ICounterCallback {
    
    public void setBaudrateCallback(boolean ret);
    
    public void getVersionCallback(int counter, String version);    
    
    public void getSerialNumberCallback(int counter, String serialNumber);
    
    public void pushCounterButtonCallback(int counter, boolean ret);
    
    public void setDigitsCallback(int counter, boolean ret);
    
    public void switchPlayerNumbersCallback(int counter, boolean ret);
    
    public void setCounterConfigCallback(int counter, boolean ret);
    
    public void getCounterConfigCallback(int counter, CounterConfig counterConfig);    
    
    public void getCounterDataCallback(int counter, CounterData counterData);
    
    public void setGameNumberCallback(int counter, boolean result);
    
    public void getGameNumberCallback(int counter, int gameNr);
    
    public void setPlayerNumbersCallback(int counter, boolean result);
    
    public void getSetResultsCallback(int counter, String results);
    
    public void setDateTimeCallback(int counter, boolean ret);
    
    public void getDateTimeCallback(int counter, String dateTime);
    
    public void onErrorCallback(int counter, int errCode);
    
    public void getPlayerNumbersCallback(int counter, int playerNrLeft, int playerNrRight);
       
    /// Never called ...
    public void resetCallback(int counter, boolean result);

    public void resetAlertCallback(int counter, boolean result);
}
