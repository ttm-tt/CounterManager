/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.driver;

import countermanager.model.CounterCallback;
import java.io.IOException;

/**
 *
 * @author chtheis
 */
public interface ICounterDriver {

    /**
     * Activate trace output
     */
    void activateTrace(int type);

    /**
     * Close the connection
     */
    void closeConnection();

    /**
     * Get the counter configuration
     */
    void getCounterConfig(int counter) throws IOException;

    /**
     * Get the counter data
     */
    void getCounterData(int counter) throws IOException;

    /**
     * Get the data from all counters
     */
    void getCounterDataBroadcast(int last) throws IOException;

    /**
     * Get date and time
     */
    void getDateTime(int counter, String format) throws IOException;

    /**
     * Get the driver version
     */
    String getDriverVersion();

    /**
     * Get the error description
     */
    String getErrorMessage(int errCode);

    /**
     * Get the game number
     */
    void getGameNumber(int counter) throws IOException;

    /**
     * Get the player numbers from a counter
     */
    void getPlayerNumbers(int counter) throws IOException;

    /**
     * Get the serial number
     */
    void getSerialNumber(int counter) throws IOException;

    /**
     * Get all results from a counter
     */
    void getSetResults(int counter) throws IOException;

    /**
     * Get the counter version
     */
    void getVersion(int counter) throws IOException;

    /**
     * Open a connection
     */
    int openConnection(countermanager.prefs.Properties prefs) throws IOException;

    /**
     * Simulate a button push
     */
    void pushCounterButton(int counter, int button) throws IOException;

    /**
     * Reset the alert bit
     */
    void resetAlert(int counter) throws IOException;

    /**
     * Reset the counter
     */
    void resetCounter(int counter) throws IOException;

    /**
     * Set the baudrate
     */
    void setBaudrate(int baudrate);

    /**
     * Set the callback procs
     */
    void setCallbackProcs(ICounterCallback cbObject);

    /**
     * Set the code nr
     */
    void setCodeNr(int codeNr);

    /**
     * Set the counter configuration
     */
    void setCounterConfig(int counter, CounterConfig counterConfig) throws IOException;

    /**
     * Set date and time
     */
    void setDateTime(int counter) throws IOException;

    /**
     * Sets the data of the digits
     */
    void setDigits(int counter, DigitsData data) throws IOException;

    /**
     * Set the game data for a counter
     */
    void setGameData(int counter, IGameData gameData) throws IOException;

    /**
     * Set the game number
     */
    void setGameNumber(int counter, int gameNumber) throws IOException;

    /**
     * Set the player numbers
     */
    void setPlayerNumbers(int counter, int playerLeft, int playerRight) throws IOException;

    /**
     * Switch the players
     */
    void switchPlayerNumbers(int counter) throws IOException;
    
    /**
     * Force a result
     */
    void setResult(int counter, int[][] result) throws IOException;
    
    /**
     * Get the address of a counter
     */
    public String getAddress(int counter);
    
    /**
     * @return the debug
     */
    public boolean isDebug();

    /**
     * @param aDebug the debug to set
     */
    public void setDebug(boolean aDebug);

    /**
     * @return the passive
     */
    public boolean isPassive();

    /**
     * @param aPassive the passive to set
     */
    public void setPassive(boolean aPassive);
    
    /**
     * @return the broadcast
     */
    public boolean isBroadcast();
    
    /**
     * @param aBroadcaste the broadcast to set
     */
    public void setBroadcast(boolean aBroadcast);    
    
    /**
     * @return the time to query for updates in milliseconds
     */
    public long getUpdateTime();
    
    /**
     * @return the minimum time to wait between updates in milliseconds
     */
    public long getMinimumDelay();
    
    /**
     * Create an object to set the internal game data
     * @return an instance of IGameData
     */
    public IGameData createGameData();
    
    /**
     * Get an property object
     */
    public ICounterProperties getCounterProperties();
    
    /**
     * Set the properties
     */
    public void setCounterProperties(ICounterProperties props);
    
    /**
     * Lock screen
     */
    public void lockScreen(int counter);
    
    /**
     * Unlock screen
     */
    public void unlockScreen(int counter);
}
