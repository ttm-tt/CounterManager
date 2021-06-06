/* Copyright (C) 2020 Christoph Theis */

/*
 * CounterModel.java
 *
 * Created on 1. Januar 2007, 15:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package countermanager.model;

import countermanager.model.database.IDatabase;
import countermanager.driver.CounterConfig;
import countermanager.driver.CounterData;
import countermanager.driver.CounterDriver;
import countermanager.prefs.Prefs;
import countermanager.prefs.Properties;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Administrator
 */
public class CounterModel {

    public final static int MAX_COUNTERS = 255;
    
    // Number of retries for periodical reads from a counter
    private final static int RETRIES = 3;
    
    // Delay between 2 counters
    private final static long COUNTER_DELAY = (long) (0.050 * 1000);
    
    // Delay if an counter had an error
    private final static long ERROR_DELAY = 0 * 1000;
    
    // Reset counter RESET_TIMEOUT milliseconds after a match finished.
    // Setting to 0 disables automatic resets
    private static int resetTimeout = 30 * 1000;
    
    private final static CounterModel instance = new CounterModel();
    
    // List of all known counters (and tables)
    private final CounterModelItem counterList[] = new CounterModelItem[MAX_COUNTERS];
    
    // Database interface
    private IDatabase database;
    
    // SMS Settings
    private String smsPhone = "";
    private String smsUser = "";
    private String smsPwd = "";
    
    public static CounterModel getDefaultInstance() {
        return instance;
    }
    
    public static void setBroadcast(boolean broadcast) {
        CounterDriver.setBroadcast(broadcast);
    }
    
    public static boolean isBroadcast() {
        return CounterDriver.isBroadcast();
    }
    
    public static void setResetTimeout(int timeout) {
        resetTimeout = timeout;
    }
    
    public static int getResetTimeout() {
        return resetTimeout;
    }
    
    /** Private constructor since there is only one instance */
    private CounterModel() {
        
        countermanager.liveticker.LivetickerAdmininstration.loadLiveticker();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    
                    try {
                        updateFromCounters();
                        // updateFromDatabase();
                    } catch (Throwable t) {
                        System.out.println(t.getLocalizedMessage());
                    }
                    
                    try {
                        long next = System.currentTimeMillis() + CounterDriver.getUpdateTime() / 2;
                        Thread.sleep(Math.max(next - System.currentTimeMillis(), CounterDriver.getMinimumDelay()));
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    
                    try {
                        // updateFromCounters();
                        updateFromDatabase();
                    } catch (Throwable t) {
                        System.out.println(t.getLocalizedMessage());
                    }
                    
                    try {
                        long next = System.currentTimeMillis() + CounterDriver.getUpdateTime() / 2;
                        Thread.sleep(Math.max(next - System.currentTimeMillis(), CounterDriver.getMinimumDelay()));
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }).start();
    }
    
    
    public void addCounter(final int counter) {
        if (counterList[counter] != null)
            return;
        
        counterList[counter] = new CounterModelItem(counter);
        
        fireCounterAddedEvent(counter);
    }
    
    
    public void removeCounter(final int counter) {
        if (counterList[counter] == null)
            return;
        
        counterList[counter] = null;
        
        fireCounterRemovedEvent(counter);
    }


    public int getNumberOfCounters() {
        for (int idx = counterList.length; idx-- > 0; )
            if (counterList[idx] != null && counterList[idx].isActive())
                return idx + 1;

        return 0;
    }
    
    
    public void setVersion(int counter, String version) {
        if (counterList[counter] != null)
            counterList[counter].setVersion(version);
    }
    
    
    public String getVersion(int counter) {
        if (counterList[counter] != null)
            return counterList[counter].getVersion();
        else
            return null;
    }
    
    
    public void setSerialNumber(int counter, String serialNumber) {
        if (counterList[counter] != null)
            counterList[counter].setSerialNumber(serialNumber);
    }
    
    
    public String getSerialNumber(int counter) {
        if (counterList[counter] != null)
            return counterList[counter].getSerialNumber();
        else
            return null;
    }
    
    
    public void setCounterConfig(int counter, CounterConfig counterConfig) {
        if (counterList[counter] != null) {
            counterList[counter].setCounterConfig(counterConfig);
            
            fireCounterChangedEvent(counter);
        }
    }
    
    
    public CounterConfig getCounterConfig(int counter) {
        if (counterList[counter] != null)
            return counterList[counter].getCounterConfig();
        else
            return null;
    }
    
    
    public void updateCounterConfig(int counter, CounterConfig counterConfig) {
        try {
            CounterDriver.setCounterConfig(counter, counterConfig);
        } catch (java.io.IOException e) {
            fireCounterErrorEvent(counter, e);
        }
    }
    
    
    public void setCounterData(int counter, CounterData counterData) {
        if (counterList[counter] != null) {
            counterList[counter].setCounterData(counterData);
            fireCounterChangedEvent(counter);

            if (counterData != null) {
                counterList[counter].setErrorTime(0);
                counterList[counter].setErrorCount(0);
            }
        }
    }
    
    
    public CounterData getCounterData(int counter) {
        if (counter >= counterList.length)
            return null;
        
        if (counterList[counter] != null)
            return counterList[counter].getCounterData();
        else
            return null;
    }
    
    
    public int[][] getResults(int counter) {
        if (counter >= counterList.length)
            return null;
        
        if (counterList[counter] != null)
            return counterList[counter].getResult();
        else
            return null;
    }
    
 
    public void setCounterMatch(int counter, CounterModelMatch match) {
        if (counterList[counter] != null) {
            try {
                counterList[counter].setMatch(match);
            } catch (IOException ex) {
                // Reset counterData to signal failure
                counterList[counter].setCounterData(null);
            }            

            fireCounterChangedEvent(counter);
        }
    }

    
    public CounterModelMatch getCounterMatch(int counter) {
        if (counter >= counterList.length)
            return null;
        
        if (counterList[counter] == null)
            return null;
        else
            return counterList[counter].getMatch();
    }
    
    
    public int getErrorCount(int counter) {
        if (counterList[counter] == null)
            return 0;
        else
            return counterList[counter].getErrorCount();
    }
    
    public long getErrorTime(int counter) {
        if (counterList[counter] == null)
            return 0;
        
        return counterList[counter].getErrorTime();
    }
    
    // -------------------------------------------------------------------
    // CounterModel events
    javax.swing.event.EventListenerList  listenerList = new javax.swing.event.EventListenerList();
    
    public void addCounterModelListener(ICounterModelListener l) {
        listenerList.add(ICounterModelListener.class, l);
    }
    
    
    public void removeCounterModelListener(ICounterModelListener l) {
        listenerList.remove(ICounterModelListener.class, l);
    }
    
    // fireCounterXXXEvent are package private. Thus CounterModelItem could fire events
    void fireCounterAddedEvent(int counter) {
        ICounterModelListener listeners[] = listenerList.getListeners(ICounterModelListener.class);
        for (ICounterModelListener listener : listeners) {
            listener.counterAdded(counter);
        }
    }
    
    
    void fireCounterRemovedEvent(int counter) {
        ICounterModelListener listeners[] = listenerList.getListeners(ICounterModelListener.class);
        for (ICounterModelListener listener : listeners) {
            listener.counterRemoved(counter);
        }
    }
    
    
    void fireCounterChangedEvent(int counter) {
        ICounterModelListener listeners[] = listenerList.getListeners(ICounterModelListener.class);
        for (ICounterModelListener listener : listeners) {
            listener.counterChanged(counter);
        }
    }
    
    
    void fireCounterErrorEvent(int counter, Throwable e) {
        ICounterModelListener listeners[] = listenerList.getListeners(ICounterModelListener.class);
        for (ICounterModelListener listener : listeners) {
            listener.counterError(counter, e);
        }
    }
    
    
    // -------------------------------------------------------------------    
    public void refreshConfiguration(int counter) {
        if (counterList[counter] != null)            
            try {
                counterList[counter].refreshConfiguration();
            } catch (IOException ex) {
               setCounterData(counter, null);
            }
    }
    
    public void refresh() {
        // Clear data from counters
        int last = 0;

        try {
            for (int idx = 0; idx < counterList.length; idx++) {
                // setCounterData(idx, null);
                if (idx < getFromTable() - getTableOffset() || idx > getToTable() - getTableOffset())
                    counterList[idx] = null;

                if (counterList[idx] != null && counterList[idx].isActive()) {
                    last = idx;

                    if (counterList[idx].getErrorCount() < RETRIES)
                        counterList[idx].setErrorCount(counterList[idx].getErrorCount() + 1);

                    if ( resetTimeout != 0 && counterList[idx].getEndTime() != 0 && 
                         counterList[idx].getEndTime() + resetTimeout < System.currentTimeMillis() )
                        counterList[idx].reset();
                }
            }

            CounterDriver.getCounterDataBroadcast(last + 1);

            for (int idx = 0; idx <= last; idx++) {
                if (counterList[idx] == null || !counterList[idx].isActive())
                    continue;

                if (counterList[idx].getErrorCount() == RETRIES) {
                    setCounterData(idx, null);
                }
            }
        } catch (java.io.IOException e) {
            
        }
    }
    
    
    public void reset(int counter) {
        if (counterList[counter] != null) {
            
            try {
                counterList[counter].reset();
            } catch (IOException e) {
                
            }
            
            fireCounterChangedEvent(counter);
        }
    }
    
    
    public void forceMatch(int counter, CounterModelMatch match) {
        if (counterList[counter] != null) {
            try {
                counterList[counter].forceMatch(match);
            } catch (IOException e) {
                
            }
        }
    }
    
    
    public void forceResult(int counter, int[][] result) {
        if (counterList[counter] != null) {
            try {
                counterList[counter].forceResult(result);
            } catch (IOException e) {
                
            }
        }
    }
    
    
    public void swapPlayer(int counter) {
        if (counterList[counter] != null) {
            try {
                counterList[counter].swapPlayer();
            } catch (IOException e) {
                
            }
        }
    }
    
    
    public void setAllCounterActive() {
        for (int i = 0; i < counterList.length; i++)
            setCounterActive(i, true);
    }
    
    
    public void setCounterActive(int counter, boolean active) {
        if (counterList[counter] != null) {
            counterList[counter].setActive(active);
            
            fireCounterChangedEvent(counter);
        }        
    }
    
    public boolean isCounterActive(int counter) {
        if (counterList[counter] != null)
            return counterList[counter].isActive();
        else
            return false;        
    }
    
    public void setCounterChecked(int counter, boolean checked) {
        if (counterList[counter] != null)
            counterList[counter].setChecked(checked);
        
        fireCounterChangedEvent(counter);
    }
        
    
    public boolean isCounterChecked(int counter) {
        if (counterList[counter] != null)
            return counterList[counter].isChecked();
        else
            return false;
    }
    
    public boolean isCounterSwapped(int counter) {
        if (counterList[counter] != null) 
            return counterList[counter].isSwapped();
        else
            return false;
    }
    
    public void setLivetickerActive(int counter, boolean active) {
        if (counterList[counter] != null)
            counterList[counter].setLivetickerActive(active);
    }
    
    public boolean isLivetickerActive(int counter) {
        if (counterList[counter] != null)
            return counterList[counter].isLivetickerActive();
        else
            return false;
    }
    
    public boolean isMatchRunning(int table, int mtnr, int mtms) {
        int counter = table - tableOffset;
        
        if (counter < 0 || counter >= counterList.length || counterList[counter] == null)
            return false;
        return counterList[counter].isMatchRunning(mtnr, mtms);
    }
    
    public int getServiceAX(int table, int mtnr, int mtms) {
        int counter = table - tableOffset;
        
        if (counter < 0 || counter >= counterList.length || counterList[counter] == null)
            return 0;
        
        return counterList[counter].getServiceAX(mtnr, mtms);
    }
    
    // -------------------------------------------------------------------
    // Counter connection functions
    boolean countersConnected = false;
    java.util.Timer updateCounterTimer = null;
    
    CounterCallback cb = null;
    
    public void connectCounters(countermanager.prefs.Properties prefs) {   
        
        if (prefs == null)
            return;
        
        // XXX Why does the JNI dll crash if the callback object is set again?
        // The getCounterDataCallback-function fails to lookup "getCounterDataCallback"        
        if (cb == null) {
            cb = new CounterCallback() {
                @Override
                public void onErrorCallback(int counter, int errCode) {
                    if (getCounterData(counter) != null) {
                        if (counterList[counter].getErrorCount() == RETRIES) {
                            counterList[counter].setErrorTime(System.currentTimeMillis() + ERROR_DELAY);
                            setCounterData(counter, null);
                        } else {
                            counterList[counter].setErrorCount(counterList[counter].getErrorCount() + 1);
                        }
                    }
                    
                    fireCounterErrorEvent(counter, new java.io.IOException(
                            CounterDriver.getErrorMessage(errCode) + " (" + errCode + ")"));
                }
            };
        }

        try {
            CounterDriver.openConnection(prefs, cb);
        } catch (IOException e) {

        }
        
        // CounterDriver.setCodeNr(0);
        
        countersConnected = true;
    }
    
    
    public void disconnectCounters() {
        countersConnected = false;
        CounterDriver.closeConnection();
    }
    
    
    // Update thread
    @SuppressWarnings("SleepWhileInLoop")
    private void updateFromCounters() {
        if (!countersConnected)
            return;
        
        // Or getDataBroadcast
        if (isBroadcast()) {
            refresh();
        } else {
            for (int i = 0; i < counterList.length; i++) {
                // Ignore non-existing or inactive counters
                if (counterList[i] == null || !counterList[i].isActive())
                    continue;

                // Ignore counters which had an error recently
                if (counterList[i].getErrorTime() > System.currentTimeMillis())
                    continue;
                                
                if ( resetTimeout != 0 && counterList[i].getEndTime() != 0 && 
                     counterList[i].getEndTime() + resetTimeout < System.currentTimeMillis() ) {
                    try {
                        counterList[i].reset();
                        continue;
                    } catch (IOException e) {
                        
                    }
                }

                try {
                    counterList[i].refreshData(); // throws

                    Thread.sleep(COUNTER_DELAY);
                } catch (IOException e) {
                    cb.onErrorCallback(i, 1);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }
    
    
    // -------------------------------------------------------------------
    // Database connection functions
    private int tableOffset = 0;
    private int fromTable = 1, toTable = MAX_COUNTERS + 1;
        
    public void setTableOffset(int offset) {
        this.tableOffset = offset;
        
        // Update views
        for (int i = 0; i < counterList.length; i++) {
            if (counterList[i] != null)
                fireCounterChangedEvent(i);
        }
    }

    public int getTableOffset() {
        return tableOffset;
    }
    
    public int getFromTable() {
        return fromTable;
    }
    
    public int getToTable() {
        return toTable;
    }
    
    
    public void setTableRange(int fromTable, int toTable) {
        this.fromTable = fromTable;
        this.toTable = toTable;
    }
    
    
    public boolean isDatabaseConnected() {        
        return database != null && database.isConnected();
    }


    @SuppressWarnings("CallToThreadDumpStack")
    public boolean connectDatabase(String databaseType) {
        try {
            database = (IDatabase) getClass().getClassLoader().loadClass(databaseType).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(CounterModel.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        if (database.connect()) {            
            Logger.getLogger(getClass().getName()).log(
                    Level.INFO, "connectedToDatabaseString");
            
            return true;
        }
        
        return false;
    }
    
    
    public void disconnectDatabase() {
        if (database != null)
            database.disconnect();
        
        database = null;
        
        // Set all matches to null
        for (int idx = 0; idx < counterList.length; idx++)
            setCounterMatch(idx, null);
                    
        Logger.getLogger(getClass().getName()).log(Level.INFO, "disconnectedFromDatabaseString");
        
    }
    
    // Update thread
    long  lastUpdateTime = System.currentTimeMillis();
    
    @SuppressWarnings("CallToThreadDumpStack")
    void  updateFromDatabase() {
        if (database == null)
            return;
        
        List<CounterModelMatch> matches = database.update(fromTable, toTable, java.time.LocalDate.now(), false);
        
        int lastTable = -1;
        
        for (CounterModelMatch match : matches) {
            // Sanity check
            if (match.mtTable - tableOffset >= counterList.length)
                continue;
            
            // Only use the first match
            if (lastTable == match.mtTable - tableOffset) {
                if (counterList[lastTable] == null || !counterList[lastTable].isForced())
                    continue;
            }
            
            // There are no matches for the tables ]lastTable, mtTable[, so delete them
            while (lastTable < match.mtTable - tableOffset - 1)
                setCounterMatch(++lastTable, null);
            
            lastTable = match.mtTable - tableOffset;
                        
            // Check if we have a counter for this table.
            // If not, add one inactive
            if (counterList[lastTable] == null) {
                addCounter(lastTable);
                setCounterActive(lastTable, false);
            }    
                        
            setCounterMatch(lastTable, match);
        }
        
                    
        // And now update the last tables. i.e. set match data to null
        for (lastTable++; lastTable < counterList.length; lastTable++) {
            if (counterList[lastTable] != null)
                setCounterMatch(lastTable, null);      
        }
    }
    
    
    // Update a result (package private?)
    @SuppressWarnings("CallToThreadDumpStack")
    void  updateResult(int counter) {
        CounterModelItem counterModelItem = counterList[counter];
        
        if (counterModelItem == null)
            return;
        
        CounterData counterData = counterModelItem.getCounterData();        
        if (counterData == null)
            return;
        
        CounterModelMatch match = counterModelItem.getMatch();
        if (match == null)
            return;
        
        if (counterModelItem.getResult() == null)            
            return;
        
        // "deep" clone
        int[][] mtSets = counterModelItem.getResult().clone();
        for (int i = 0; i < mtSets.length; i++)
            mtSets[i] = mtSets[i].clone();
        
        int mtWalkOverA =                 
                counterData.getAbandonOrAbort() && (2 * counterData.getSetsLeft() < match.mtBestOf) ? 1 : 0;
        int mtWalkOverX = 
                counterData.getAbandonOrAbort() && (2 * counterData.getSetsRight() < match.mtBestOf) ? 1 : 0;        
        
        // And swap if players are swapped
        if (counterData.isSwapped()) {
            int tmp = mtWalkOverX;
            mtWalkOverX = mtWalkOverA;
            mtWalkOverA = tmp;

            for (int i = 0; i < mtSets.length; i++) {
                tmp = mtSets[i][0];
                mtSets[i][0] = mtSets[i][1];
                mtSets[i][1] = tmp;
            }                
        }
                
        updateResult(match.mtNr, match.mtMS, mtSets, mtWalkOverA, mtWalkOverX);
    }
    
           
    private boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX) {
        return database.updateResult(mtNr, mtMS, mtSets, mtWalkOverA, mtWalkOverX);
    }

    // -------------------------------------------------------------------
    // 
    public boolean isCounterMatchForced(int counter) {
        return counterList[counter] != null && counterList[counter].isForced();
    }
    
    
    // Test if all existing screens are locked
    // return false, if no exists or at least one was unlocked
    public boolean allScreensUnlocked() {
        for (CounterModelItem cm : counterList) {
            if (cm != null && cm.isLocked())
                return false;
        }
        
        return true;
    }
    
    
    public void lockAllScreens(boolean lock) {
        for (CounterModelItem cm : counterList) {
            if (cm != null)
                cm.setLocked(lock);
        }
    }
        
    
    public boolean isScreenLocked(int counter) {
        return counterList[counter] != null && counterList[counter].isLocked();
    }

    public IDatabase getDatabase() {
        return database;
    }
    
    public String getConnectString() {
        if (database != null)
            return database.getConnectionString();
        else
            return "";
    }      
          
    public void sendSMS(final String msg) {
        if (smsPhone.isEmpty())
            return;
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection request = null;

                // try ... catch to handle errors nicely                
                try {
                    // This URL is used for sending messages
                    String uri = "https://api.bulksms.com/v1/messages?auto-unicode=true";

                    // the details of the message we want to send                
                    String body = "{from: \"TTM\", to: \"" + smsPhone + "\", body: \"" + msg + "\", deliveryReports: \"NONE\"}";

                    // build the request based on the supplied settings
                    URL url = new URL(uri);
                    request = (HttpURLConnection) url.openConnection();
                    request.setDoOutput(true);

                    // supply the credentials
                    String authStr = smsUser + ":" + smsPwd;
                    String authEncoded = Base64.getEncoder().encodeToString(authStr.getBytes());
                    request.setRequestProperty("Authorization", "Basic " + authEncoded);

                    // we want to use HTTP POST
                    request.setRequestMethod("POST");
                    request.setRequestProperty( "Content-Type", "application/json");

                    // write the data to the request
                    try (OutputStreamWriter out = new OutputStreamWriter(request.getOutputStream())) {
                        out.write(body);
                    }

                    // make the call to the API
                    InputStream response = request.getInputStream();
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(response))) {
                        String replyText;
                        while ((replyText = in.readLine()) != null) {
                            System.out.println(replyText);
                        } 
                    }

                    request.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(CounterModel.class.getName()).log(Level.SEVERE, null, ex);

                    if (request != null) {
                        // print the detail that comes with the error
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(request.getErrorStream()))) {
                            // print the detail that comes with the error
                            String replyText;
                            while ((replyText = in.readLine()) != null) {
                                System.out.println(replyText);
                            } 
                        } catch (Exception ex2) {
                            Logger.getLogger(CounterModel.class.getName()).log(Level.SEVERE, null, ex2);
                        }
                    }
                }
            }
        }).start();        
    }

    public void enableSmsAlerts(boolean state, Properties prefs) {
        if (state) {
            smsPhone = prefs.getString(Prefs.SMS_PHONE, "");
            smsUser = prefs.getString(Prefs.SMS_USER, "");
            smsPwd = prefs.getString(Prefs.SMS_PWD, "");
        } else {
            smsPhone = "";
            smsUser = "";
            smsPwd = "";
        }
    }
}
