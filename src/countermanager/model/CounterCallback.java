/* Copyright (C) 2020 Christoph Theis */

/*
 * CallBack.java
 *
 * Created on 1. Januar 2007, 11:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package countermanager.model;

import countermanager.driver.CounterConfig;
import countermanager.driver.CounterData;
import countermanager.driver.CounterDriver;
import countermanager.driver.ICounterCallback;

/**
 *
 * @author Christoph Theis
 */
public class CounterCallback implements ICounterCallback {
    
    /** Creates a new instance of CallBack */
    public CounterCallback() {
    }
    
    
    @Override
    public void setBaudrateCallback(boolean ret) {
    }


    @Override
    public void getVersionCallback(int counter, String version) {
        CounterModel.getDefaultInstance().setVersion(counter, version);
    }
    
    
    @Override
    public void getSerialNumberCallback(int counter, String serialNumber) {
        CounterModel.getDefaultInstance().setSerialNumber(counter, serialNumber);        
    }
    
    
    @Override
    public void getCounterConfigCallback(int counter, CounterConfig counterConfig) {
        CounterModel.getDefaultInstance().setCounterConfig(counter, counterConfig);
    }
    
    
    @Override
    public void getCounterDataCallback(int counter, CounterData counterData) {
        CounterModel.getDefaultInstance().addCounter(counter);
        CounterModel.getDefaultInstance().setCounterData(counter, counterData);
    }

    
    @Override
    public void getSetResultsCallback(int counter, String results) {
    }

    @Override
    public void pushCounterButtonCallback(int counter, boolean ret) {
    }

    @Override
    public void setDigitsCallback(int counter, boolean ret) {
    }

    @Override
    public void switchPlayerNumbersCallback(int counter, boolean ret) {
    }

    @Override
    public void setCounterConfigCallback(int counter, boolean ret) {
    }

    @Override
    public void setGameNumberCallback(int counter, boolean result) {
    }

    @Override
    public void getGameNumberCallback(int counter, int gameNr) {
    }

    @Override
    public void setPlayerNumbersCallback(int counter, boolean result) {
    }

    @Override
    public void setDateTimeCallback(int counter, boolean ret) {
    }

    @Override
    public void getDateTimeCallback(int counter, String dateTime) {
    }

    @Override
    public void onErrorCallback(int counter, int errCode) {
        System.err.println(
                "Error from counter " + (counter + 1) +": " + 
                CounterDriver.getErrorMessage(errCode) + " (" + errCode + ")");
    }

    @Override
    public void resetCallback(int counter, boolean result) {
    }

    @Override
    public void resetAlertCallback(int counter, boolean result) {
        
    }
    

    @Override
    public void getPlayerNumbersCallback(int counter, int playerNrLeft, int playerNrRight) {
        
    }
}
