/* Copyright (C) 2020 Christoph Theis */

package countermanager.liveticker.ttm;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import countermanager.driver.CounterData;
import countermanager.liveticker.Liveticker;
import countermanager.liveticker.LivetickerAdmininstration;
import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;
import countermanager.prefs.PasswordCrypto;
import countermanager.prefs.Preferences;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


public final class TTM extends Liveticker {

    // A simple tuple
    class Pair {
        public Pair(long t, String s) {
            this.t = t;
            this.s = s;
        }

        public long t;
        public String s;
    };
            
    java.util.Timer timer;

    /**
     * @return the ftpHost
     */
    public String getFtpHost() {
        return ftpHost;
    }

    /**
     * @param ftpHost the ftpHost to set
     */
    public void setFtpHost(String ftpHost) {
        if (!ftpHost.equals(this.ftpHost)) {
            if (client != null) {
                try {
                    client.disconnect(true);
                } catch (FTPException | IOException t) {

                }
            }

            client = null;
        }
                
        this.ftpHost = ftpHost;
    }

    /**
     * @return the ftpUser
     */
    public String getFtpUser() {
        return ftpUser;
    }

    /**
     * @param ftpUser the ftpUser to set
     */
    public void setFtpUser(String ftpUser) {
        this.ftpUser = ftpUser;
    }

    /**
     * @return the ftpPassword
     */
    
    // encrypt password in Xml
    @XmlJavaTypeAdapter(countermanager.liveticker.XmlPasswordAdapter.class)
    public String getFtpPassword() {
        return ftpPassword;
    }

    /**
     * @param ftpPassword the ftpPassword to set
     */
    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    /**
     * @return the ftpDirectory
     */
    public String getFtpDirectory() {
        return ftpDirectory;
    }

    /**
     * @param ftpDirectory the ftpDirectory to set
     */
    public void setFtpDirectory(String ftpDirectory) {
        if (!ftpDirectory.equals(this.ftpDirectory)) {
            if (client != null) {
                try {
                    client.disconnect(true);
                } catch (FTPException | IOException t) {

                }
            }

            client = null;
        }
                
        this.ftpDirectory = ftpDirectory;
    }
    
    
    public boolean isFtpPassiveMode() {
        return ftpPassiveMode;
    }
    
    
    public void setFtpPassiveMode(boolean ftpPassiveMode) {
        this.ftpPassiveMode = ftpPassiveMode;
    }
    
    
    public boolean isFtpSecure() {
        return ftpSecure;
    }
    
    public void setFtpSecure(boolean ftpSecure) {
        this.ftpSecure = ftpSecure;
    }
    
    
    public TTM() {
        // loadProperties();
        startTimer();
    }
    
    private void startTimer() {        
        timer = new java.util.Timer();
        
        timer.schedule(new java.util.TimerTask() {
            long lastErrorTime = 0;

            @Override
            @SuppressWarnings("UseSpecificCatch")
            public void run() {
                if (lastErrorTime + 60000 > System.currentTimeMillis()) {
                    // System.out.println("Liveticker upload wait: " + new java.util.Date().toString());
                    return;
                }
                
                if (!isEnabled() || ftpHost.isEmpty()) {
                    if (client != null) {
                        try {
                            client.disconnect(true);
                        } catch (Throwable t) {
                            
                        }
                        client = null;
                        
                        // Clear cache
                        lastUpdateString = "";
                        updates.clear();
                        
                        // Also when a counter expires
                        expires.clear();
                        
                        // And the delay queue
                        msgList.clear();
                    }
                    
                    // If host is empty store locally for debugging purposes
                    if (isEnabled()) { 
                        String updateString = getUpdateString(System.currentTimeMillis());
                        if (updateString != null && !updateString.isEmpty()) {
                            try (PrintWriter pw = new PrintWriter(venue + ".js")) {
                                pw.print(updateString);
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(TTM.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    
                    return;
                } 
                
                if (client == null) {  
                    try {
                        client = (com.enterprisedt.net.ftp.FileTransferClientInterface) Class.forName("at.co.ttm.ftp.FtpClient").getConstructor(Boolean.TYPE).newInstance(ftpSecure);
                    } catch (ClassNotFoundException ex) {
                        // We expect this when we can't use the commercial lib
                        Logger.getLogger(TTM.class.getName()).log(Level.FINE, null, ex);
                    } catch (Exception ex) {
                        // Anything unexpected like the Spanish Inquistion
                        Logger.getLogger(TTM.class.getName()).log(Level.WARNING, null, ex);
                    }

                    if (client == null) {
                        client = new com.enterprisedt.net.ftp.FileTransferClient() {
                            @Override
                            public synchronized void connect() throws FTPException, IOException {
                                // UTF-8 Filenamen
                                masterContext.setControlEncoding("UTF-8");

                                super.connect();
                            }
                        };
                    }
                        
                    try {  
                        client.setRemoteHost(ftpHost);
                        client.setUserName(ftpUser);
                        client.setPassword(ftpPassword);
                        client.getAdvancedFTPSettings().setConnectMode(com.enterprisedt.net.ftp.FTPConnectMode.PASV);
                        client.connect();
                        if (!ftpDirectory.isEmpty())
                            client.changeDirectory(ftpDirectory);
                        
                        // always in the js subdirectory
                        client.changeDirectory("js");
                    } catch (FTPException | IOException ex) {
                        Logger.getLogger(TTM.class.getName()).log(Level.SEVERE, null, ex); 
                        client = null;
                        
                        return;
                    }
                }  

                String updateString = getUpdateString(System.currentTimeMillis());
                
                try {
                    if (!isUploadWithRename()) {
                        // Direktes upload
                        uploadString(updateString, venue + ".js");
                    } else {
                        // Upload mit rename
                        try {
                            if (client.exists(venue + ".js"))
                                client.deleteFile(venue + ".js");
                        } catch (com.enterprisedt.net.ftp.FTPException e) {
                            String message = getName() + ": " + e.getLocalizedMessage();
                        
                            Logger.getLogger(getClass().getName()).log(Level.INFO, message, e);

                            return;
                        }

                        uploadString(updateString, "_" + venue + ".js");
                        client.rename("_" + venue + ".js", venue + ".js");
                    }
                } catch (com.enterprisedt.net.ftp.FTPException e) {
                    try {
                        String message = getName() + ": " + e.getLocalizedMessage();
                        
                        Logger.getLogger(getClass().getName()).log(Level.FINE, message, e);
                        System.err.println(getName() + ": [" + new java.util.Date().toString() + "] ftp error - " + e.getLocalizedMessage());
                        
                        client.disconnect(true);
                        client = null;
                    
                        lastErrorTime = System.currentTimeMillis();
                    } catch (Throwable t) {
                        
                    }
                        
                } catch (Exception e) {                    
                    try {
                        String message = getName() + ": " + e.getLocalizedMessage();
                        
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, message, e);
                        
                        client.disconnect(true);
                        client = null;
                    } catch (Throwable t) {
                        
                    }
                }
            }
            
        }, updateTime * 1000, updateTime * 1000);
    }
    
    
    // Calculate what to upload, if there is anything to do so
    // Called periodically when ftp client is connected
    String getUpdateString(long ct) {
        String updateString;
        
        // First calculate Strings from update
        synchronized(updates) {
            updateString = json.toJson(updates.values());
        }

        if (!updateString.equals(lastUpdateString)) {                    
            msgList.add(new Pair(ct, updateString));
            lastUpdateString = updateString;
        }
                
        // Now check the first entry if it can be dequeued
        if (msgList.isEmpty() || msgList.get(0).t > ct - uploadDelay * 1000)
            return null;

        // And then use the first entry
        updateString = msgList.get(0).s;
        msgList.remove(0);

        return updateString;
    }
    
    @Override
    public void setMessage(int counter, String message) {
        
    }

    @Override
    public String getMessage(int counter) {
        return null;
    }

    @Override
    public void counterAdded(int counter) {
        
    }

    @Override
    public void counterRemoved(int counter) {
        
    }

    @Override
    public void counterError(int counter, Throwable t) {
        
    }
    
    @Override
    public void counterChanged(int counter) {
        if ( !CounterModel.getDefaultInstance().isCounterActive(counter) ||
             LivetickerAdmininstration.isLivetickerDisabled(counter) ) {
            synchronized(expires) {
                if (expires.get(counter) != null)
                    expires.remove(counter);
            }
            
            synchronized(updates) {
                if (updates.get(counter) != null)
                    updates.remove(counter);
            }
            
            return;
        }
        
        CounterData counterData = CounterModel.getDefaultInstance().getCounterData(counter);
        CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        doCounterChanged(counter, counterData, counterMatch, System.currentTimeMillis());
    }
    
    // Implementation; package private so we can test it
    void doCounterChanged(int counter, CounterData counterData, CounterModelMatch counterMatch, long ct) {        
        if (counterData == null || counterData.getGameMode() == CounterData.GameMode.RESET) {

            synchronized(expires) {
                if (expires.get(counter) != null && expires.get(counter) + expireTimeout * 1000 > ct) {
                    // No next match: Leave it alone
                    if (counterMatch == null)
                        return;

                    // Minimum displayed
                    if (expires.get(counter) + minExpireTimeout * 1000 > ct)
                        return;

                    // But before the next match
                    if (counterMatch.mtDateTime > ct + prestartTime * 1000)
                        return;
                }
            }
        }

        if (counterMatch == null) {
            // No new match
            if (true) return; // keep the old match in the ticker
            synchronized(expires) {
                if (expires.get(counter) != null)
                    expires.remove(counter);
            }
            
            synchronized(updates) {
                if (updates.get(counter) != null)
                    updates.remove(counter);
            }
            
            return;
        }
        
        if (counterData != null && counterData.getGameNr() != 0xFFFF) {
            if (counterMatch.mtMS > 1 && counterData.getGameNr() != counterMatch.mtMS ||
                counterMatch.mtMS <= 1 && counterData.getGameNr() != counterMatch.mtNr) {
                // System.out.println(new java.util.Date().toString() + ": Skip match " + counterMatch.mtNr + " / " + counterMatch.mtMS + " at table " + counterMatch.mtTable + " because counter data has match " + counterData.getGameNr());
                return;
            }
        }
        
        UpdateMatch updateMatch = new UpdateMatch(counterMatch);
        
        // Im Status WARMUP liefert das elZG "7:2" als Ergebnis
        if (counterData != null && counterData.getGameMode() != CounterData.GameMode.WARMUP) {
            updateMatch.mtResA = counterData.getSetsLeft();
            updateMatch.mtResX = counterData.getSetsRight();
            
            updateMatch.copyGames(counterData.getSetHistory());

            updateMatch.walkOver = counterData.getAbandonOrAbort();

            updateMatch.time = counterData.getTime();

            updateMatch.timeoutLeft = counterData.getTimeoutLeft();
            updateMatch.timeoutRight = counterData.getTimeoutRight();

            updateMatch.serviceLeft = counterData.getServiceLeft();
            updateMatch.serviceRight = counterData.getServiceRight();
        } else {
            updateMatch.mtResA = counterMatch.mtResA;
            updateMatch.mtResX = counterMatch.mtResX;
            
            updateMatch.copyGames(counterMatch.getMtResult());
        }
        
        if (counterMatch.cpType == 4) {
            if (counterData != null) {
                // Sobald das Spiel in die DB eingetragen wurde sehe ich das
                // Spiel nicht mehr. Damit sehe ich auch nicht das aktualisierte
                // Mannschaftsergebnis.
                if (counterData.getGameMode() != CounterData.GameMode.END)
                    ; // Nix. Nur der folgende Code soll ausgeschalten werden
                else if (2 * counterData.getSetsLeft() > counterData.getBestOf()) {
                    if (counterData.isSwapped() != (counterData.getPlayerNrLeft() == updateMatch.plX.plNr))
                      System.out.println(json.toJson(new Object[] {"Left", counterData, counterMatch, updateMatch}));
                    // If counterData is swapped, it is the other way round
                    if (counterData.isSwapped()) 
                        ++updateMatch.tmX.mtRes;
                    else
                        ++updateMatch.tmA.mtRes;
                } else if (2 * counterData.getSetsRight() > counterData.getBestOf()) {
                    if (counterData.isSwapped() != (counterData.getPlayerNrRight() == updateMatch.plA.plNr))
                      System.out.println(json.toJson(new Object[] {"Left", counterData, counterMatch, updateMatch}));
                    if (counterData.isSwapped())
                        ++updateMatch.tmA.mtRes;
                    else
                        ++updateMatch.tmX.mtRes;
                }
            }
        }

        if (counterData != null && counterData.isSwapped())
            updateMatch.swapResult();
        
        updateMatch.resultLocation = this.resultLocation;
        
        if (counterData != null) {
            updateMatch.matchRunning = 
                    counterData.getGameMode() == CounterData.GameMode.RUNNING || 
                    counterData.getGameMode() == CounterData.GameMode.END;
            
            updateMatch.gameRunning = 
                    counterData.getGameMode() == CounterData.GameMode.RUNNING &&
                    counterData.getTimeMode() != CounterData.TimeMode.BREAK;
            
            updateMatch.timeoutLeftRunning = counterData.isTimeoutLeftRunning();
            updateMatch.timeoutRightRunning = counterData.isTimeoutRightRunning();
        } else {
            updateMatch.gameRunning = counterMatch.getMtResult()[0][0] > 0 || counterMatch.getMtResult()[0][1] > 0;
            updateMatch.matchRunning = updateMatch.gameRunning || counterMatch.mtResA > 0 || counterMatch.mtResX > 0;
            updateMatch.timeoutLeftRunning = false;
            updateMatch.timeoutRightRunning = false;
        }
        
        if (counterMatch.mtReverse)
            updateMatch.reverse();

        String data = json.toJson(updateMatch);

        synchronized(updates) {
            if (!data.equals(updates.get(counter))) {
                updates.put(counter, data);  
                expires.put(counter, ct);
            }
        }
    }
    
    
    public String getResultLocation() {
        return resultLocation;
    }
    
    public void setResultLocation(String resultLocation) {
        if (!resultLocation.endsWith("/"))
            resultLocation += "/";
        
        this.resultLocation = resultLocation;
    }
    
    public int getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(int updateTime) {
        if (updateTime != this.updateTime) {
            if (timer != null)
                timer.cancel();
            
            timer = null;
            
            this.updateTime = updateTime;
            
            startTimer();
        }
    }
    

    public void setExpireTimeout(int expireTimeout) {
        this.expireTimeout = expireTimeout;
    }
    
    public int getExpireTimeout() {
        return expireTimeout;
    }
    

    /**
     * @return the minExpireTimeout
     */
    public int getMinExpireTimeout() {
        return minExpireTimeout;
    }

    /**
     * @param minExpireTimeout the minExpireTimeout to set
     */
    public void setMinExpireTimeout(int minExpireTimeout) {
        this.minExpireTimeout = minExpireTimeout;
    }

    /**
     * @return the prestartTime
     */
    public int getPrestartTime() {
        return prestartTime;
    }

    /**
     * @param prestartTime the prestartTime to set
     */
    public void setPrestartTime(int prestartTime) {
        this.prestartTime = prestartTime;
    }
    
    
    public int getUploadDelay() {
        return uploadDelay;
    }
    
    
    public void setUploadDelay(int delay) {
        this.uploadDelay = delay;
    }
    
    public String getVenue() {
        return venue;
    }
    
    
    public void setVenue(String fileName) {
        this.venue = fileName;
    }
    
    
    public boolean isUploadWithRename() {
        return uploadWithRename;
    }
    
    
    public void setUploadWithRename(boolean uploadWithRename) {
        this.uploadWithRename = uploadWithRename;
    }
    
        
    // load and save properties to encode ftp password
    @Override
    protected void loadProperties() {
        Preferences.loadProperties(this, this.getClass().getName(), true);
        
        if (!ftpPassword.isEmpty())
            ftpPassword = PasswordCrypto.decryptPassword(ftpPassword);
    }
    
    @Override
    protected void saveProperties() {
        String savePwd = ftpPassword;
        if (!ftpPassword.isEmpty())
            ftpPassword = PasswordCrypto.encryptPassword(ftpPassword);
        
        Preferences.saveProperties(this, this.getClass().getName(), true);
        
        ftpPassword = savePwd;
    }   
    
    private void uploadString(String str, String remoteFile) throws FTPException, IOException {
        if (str == null)
            return;
        
        byte[] bytes = str.getBytes(charsetUTF);
        
        if (bytes == null)
            return;
        
        try (FileTransferOutputStream fos = client.uploadStream(remoteFile)) {
            fos.write(bytes);
        }
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    private String  ftpHost = "";
    private String  ftpUser = "";
    private String  ftpPassword = "";
    private String  ftpDirectory = "";
    private boolean ftpPassiveMode = true;
    private boolean ftpSecure = false;
    
    private com.enterprisedt.net.ftp.FileTransferClientInterface client = null;        
            
    private String resultLocation = "";
    
    private int    updateTime = 5;
        
    private int    expireTimeout = 15 * 60;   // Time in seconds the result will stay in the LT
    private int    minExpireTimeout = 1 * 60; // Minimum timeout the result will stay in the LT
    private int    prestartTime = 1 * 60;     // Time before scheduled start the match will appear in the LT
    private int    uploadDelay = 0;
    
    private String venue = "update";
    
    private boolean uploadWithRename = true;  // Upload with different name, then rename file
    
    private String name = getClass().getSimpleName();

    final private static Gson json = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
    // final private static Gson json = new GsonBuilder().create();
    
    String lastUpdateString = "";
            
    final Map<Integer, String> updates = new java.util.HashMap<>();
    final Map<Integer, Long>   expires  = new java.util.HashMap<>();
    final List<Pair> msgList = new java.util.ArrayList<>();
            

    final static private java.nio.charset.Charset charsetUTF = java.nio.charset.Charset.forName("UTF-8");
}
