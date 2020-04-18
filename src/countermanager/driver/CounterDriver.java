/* Copyright (C) 2020 Christoph Theis */

/*
 * CounterDriver.java
 *
 * Created on 15. Dezember 2006, 11:53
 */

package countermanager.driver;

import static countermanager.prefs.Prefs.*;

/**
 *
 * @author Christoph Theis
 */
public class CounterDriver {

    /**
     * @return the debug
     */
    public static boolean isDebug() {
        return driver == null ? false : driver.isDebug();
    }

    /**
     * @param aDebug the debug to set
     */
    public static void setDebug(boolean debug) {
        if (driver != null)
            driver.setDebug(debug);
    }

    /**
     * @return the passive
     */
    public static boolean isPassive() {
        return driver == null ? false : driver.isPassive();
    }

    /**
     * @param aPassive the passive to set
     */
    public static void setPassive(boolean passive) {
        if (driver != null)
            driver.setPassive(passive);
    }
    
    public static boolean isBroadcast() {
        return driver == null ? false : driver.isBroadcast();
    }
    
    public static void setBroadcast(boolean broadcast) {
        if (driver != null)
            driver.setBroadcast(broadcast);
    }
    
    public static long getUpdateTime() {
        return driver == null ? (long) 3000 : driver.getUpdateTime();
    }
    
    
    public static long getMinimumDelay() {
        return driver == null ? (long) 1000 : driver.getMinimumDelay();
    }
    
    
    /** Creates a new instance of CounterDriver */
    private CounterDriver() {
    }
    
    /** Get the error description */
    public static String getErrorMessage(int errCode) {
        return driver == null ? null : driver.getErrorMessage(errCode);
    }
    
    /** Activate trace output */
    public static void activateTrace(int type) {
        if (driver != null)
            driver.activateTrace(type);
    }
    
    /** Get the driver version */
    public static String getDriverVersion() {
        return driver == null ? null : driver.getDriverVersion();
    }


    /** Open a connection */
    public static int openConnection(countermanager.prefs.Properties prefs, ICounterCallback cbObject) throws java.io.IOException {
        String className = (String) prefs.get(CT_CLASS_PREF);
        if (className == null)
            driver = new countermanager.driver.ttm.CounterDriverTTM();
        else {
            try {
                Class clazz = cbObject.getClass().getClassLoader().loadClass(className);
                
                
                driver = (ICounterDriver) clazz.newInstance();
            } catch (Throwable t) {
                return -1;
            }
        }
        driver.setCallbackProcs(cbObject);
        
        return driver.openConnection(prefs);
    }
    
    /** Close the connection */
    public static void closeConnection() {
        if (driver != null)
            driver.closeConnection();
        
        driver = null;
    }
    
    /** Set the baudrate */
    public static void setBaudrate(int baudrate) {
        if (driver != null)
            driver.setBaudrate(baudrate);
    }


    /** Get the counter version */
    public static void getVersion(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getVersion(counter);
    }

    /** Get the serial number */
    public static void getSerialNumber(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getSerialNumber(counter);
    }

    /** Reset the counter */
    public static void resetCounter(int counter) throws java.io.IOException {
        if (driver != null)
            driver.resetCounter(counter);
    }

    /** Set date and time */
    public static void setDateTime(int counter) throws java.io.IOException {
        if (driver != null)
            driver.setDateTime(counter);
    }

    /** Get date and time */
    public static void getDateTime(int counter, String format) throws java.io.IOException {
        if (driver != null)
            driver.getDateTime(counter, format);
    }

    /** Sets the data of the digits */
    public static void setDigits(int counter, DigitsData data) throws java.io.IOException {
        if (driver != null)
            driver.setDigits(counter, data);
    }

    /** Simulate a button push */
    public static void pushCounterButton(int counter, int button) throws java.io.IOException {
        if (driver != null)
            driver.pushCounterButton(counter, button);
    }

    /** Switch the players */
    public static void switchPlayerNumbers(int counter) throws java.io.IOException {
        if (driver != null)
            driver.switchPlayerNumbers(counter);
    }
    
    /** Set the counter configuration */
    public static void setCounterConfig(int counter, CounterConfig counterConfig) throws java.io.IOException {
        if (driver != null)
            driver.setCounterConfig(counter, counterConfig);
    }

    /** Get the counter configuration */
    public static void getCounterConfig(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getCounterConfig(counter);
    }

    /** Set the game number */
    public static void setGameNumber(int counter, int gameNumber) throws java.io.IOException {
        if (driver != null)
            driver.setGameNumber(counter, gameNumber);
    }

    /** Get the game number */
    public static void getGameNumber(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getGameNumber(counter);
    }

    /** Get the counter data */
    public static void getCounterData(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getCounterData(counter);
    }

    /** Get the data from all counters */
    public static void getCounterDataBroadcast(int last) throws java.io.IOException {
        if (driver != null)
            driver.getCounterDataBroadcast(last);
    }
    
    /** Set the player numbers */
    public static void setPlayerNumbers(int counter, int playerLeft, int playerRight) throws java.io.IOException {
        if (driver != null)
            driver.setPlayerNumbers(counter, playerLeft, playerRight);
    }

    /** Get all results from a counter */
    public static void getSetResults(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getSetResults(counter);
    }

    /** Get the player numbers from a counter */
    public static void getPlayerNumbers(int counter) throws java.io.IOException {
        if (driver != null)
            driver.getPlayerNumbers(counter);
    }

    /** Set the game data for a counter*/
    public static void setGameData(int counter, IGameData gameData) throws java.io.IOException {
        if (driver != null)
            driver.setGameData(counter, gameData);
    }
    
    /** Set the result for a counter */
    public static void setResult(int counter, int[][] result) throws java.io.IOException {
        if (driver != null)
            driver.setResult(counter, result);
    }

    /** Set the code nr */
    public static void setCodeNr(int codeNr) {
        if (driver != null)
            driver.setCodeNr(codeNr);
    }
    
    /** Reset the alert bit */
    public static void resetAlert(int counter) throws java.io.IOException {
        if (driver != null)
            driver.resetAlert(counter);
    }
    
    
    public static IGameData createGameData() {
        return driver == null ? null : driver.createGameData();
    }
    
    
    // Get the address of a counter
    public static String getAddress(int counter) {
        return driver == null ? null : driver.getAddress(counter);
    }
    
    
    // Lock / unlock screen
    public static void lockScreen(int counter) {
        if (driver != null)
            driver.lockScreen(counter);
    }
    
    
    public static void unlockScreen(int counter) {
        if (driver != null)
            driver.unlockScreen(counter);
    }

    // private static ICounterDriver driver = new countermanager.driver.serial.CounterDriverSerial();
    // private static ICounterDriver driver = new countermanager.driver.websocket.CounterDriverWebsocket();
    private static ICounterDriver driver = null;
}
