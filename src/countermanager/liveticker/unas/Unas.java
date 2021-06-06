/* Copyright (C) 2020 Christoph Theis */

package countermanager.liveticker.unas;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferOutputStream;
import countermanager.liveticker.Liveticker;
import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;
import countermanager.model.database.IDatabase;
import countermanager.prefs.PasswordCrypto;
import countermanager.prefs.Preferences;
import countermanager.prefs.Properties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptException;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


public class Unas extends Liveticker {
    private long lastTimestamp = 0;
    private Map<String, String> lastUploads = new java.util.HashMap<>();

    private java.util.Timer timer;
    private long lastErrorTime = 0;
    
    public Unas() {
        startTimer();
    }
    
    private void startTimer() {
        timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                update();
            }
            
        }, getUpdateTime() * 1000, getUpdateTime() * 1000);
    }
    
    private void update() {
        // In case of a previous error wait some time
        if (lastErrorTime + 60000 > System.currentTimeMillis()) {
            // System.out.println("Liveticker upload wait: " + new java.util.Date().toString());
            return;
        }

        // Stop updates if LT is not enabled or host is empty
        if (!isEnabled() || ftpHost.isEmpty()) {
            if (client != null) {
                try {
                    client.disconnect(true);
                } catch (Throwable t) {

                } finally {
                    client = null;
                }
            }

            return;
        }
                
        lastTimestamp = readLastTimestamp();
        
        IDatabase database = CounterModel.getDefaultInstance().getDatabase();
        if (database == null)
            return;
        
        long currentLastTimestamp = database.getMaxMtTimestamp();
        
        // If current mtTimestamp == last mtTimestamp, then there is nothing to do
        if (currentLastTimestamp == lastTimestamp)
            return;
        
        // Paranoia: if current mtTimestamp < lastTimestamp. then there is fire on the roof
        if (currentLastTimestamp < lastTimestamp)
            lastTimestamp = 0;
        
        List<java.time.LocalDate> dates = database.getChangedDates(lastTimestamp);
        if (dates == null || dates.isEmpty())
            return;
        
        for (java.time.LocalDate date : dates) {
            List<CounterModelMatch> matchList = readMatches(database, date);
            
            String s = null;
            
            // Use a script to convert that list to json: that way it is easier
            // to have different formats depending if the match is a team match
            // or not, and it is easier to modify the format
            try {
                if (jsEngine == null) {
                    jsEngine = new javax.script.ScriptEngineManager().getEngineByExtension("js");

                    File fs = null;
                    for (File path : scriptPathes) {
                        fs = new File(path, script);
                        if (fs.exists())
                            break;
                    }

                    jsEngine.eval(new java.io.FileReader(fs));
                }

                Object o = ((Invocable) jsEngine).invokeFunction("fixtures", matchList);      
                
                if (o instanceof String)
                    s = o.toString();
                    
            } catch (FileNotFoundException | ScriptException | NoSuchMethodException ex) {
                Logger.getLogger(Unas.class.getName()).log(Level.SEVERE, null, ex);

                jsEngine = null;
                return;
            }
            
            String fileName = date.format(DateTimeFormatter.ofPattern("'d'yyyy-MM-dd'.json'"));
            if (s == null || lastUploads.containsKey(fileName) && lastUploads.get(fileName).equals(s))
                continue;
            
            if (!upload(fileName, s))
                return;
            
            lastUploads.put(fileName, s);
        }
        
        writeLastTimestamp(currentLastTimestamp);
    }
    
    private List<CounterModelMatch> readMatches(IDatabase database, java.time.LocalDate when) {
        List<CounterModelMatch> matchList = database.update(fromTable, toTable, when, true);
        
        // Sort by time and table (we would get the list sorted by table and time)
        matchList.sort(new java.util.Comparator<>() {
            @Override
            public int compare(CounterModelMatch o1, CounterModelMatch o2) {
                if (o1.mtDateTime < o2.mtDateTime)
                    return -1;
                if (o1.mtDateTime > o2.mtDateTime)
                    return +1;
                if (o1.mtTable < o2.mtTable)
                    return -1;
                if (o1.mtTable > o2.mtTable)
                    return +1;
                if (o1.mtMS < o2.mtMS)
                    return -1;
                if (o1.mtMS > o2.mtMS)
                    return +1;
                
                return 0;
            }
        });
        
        return matchList;
    }
    
    private boolean upload(String fileName, String data) {
        try {
            if (client == null) {  
                try {
                    client = (com.enterprisedt.net.ftp.FileTransferClientInterface) Class.forName("at.co.ttm.ftp.FtpClient").getConstructor(Boolean.TYPE).newInstance(ftpSecure);
                } catch (ClassNotFoundException ex) {
                    // We expect this when we can't use the commercial lib
                    Logger.getLogger(getClass().getName()).log(Level.FINE, null, ex);                
                } catch (Exception ex) {
                    // Anything unexpected like the Spanish Inquistion
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, null, ex);                
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

                if (ftpDebug) {
                    // Wenn Debugging eingeschalten ist, auch den Log-Level setzen.
                    // Eigentljch geschieht das fuer de.webgen in MainFrame, aber aus
                    // seltsamen Gruenden kommt es vor, dass er zurueckgesetzt wird.
                    // Oder es wird ueberhaupt ein anderer Logger verwendet ...
                    Logger.getLogger(getClass().getName()).setLevel(Level.FINE);

                    client.setEventListener(new com.enterprisedt.net.ftp.EventAdapter() {
                        @Override
                        public void commandSent(String connId, String cmd) {
                            Logger.getLogger(getClass().getName()).log(Level.FINE, cmd);
                        }

                        @Override
                        public void replyReceived(String connId, String reply) {
                            Logger.getLogger(getClass().getName()).log(Level.FINE, reply);
                        }
                    });
                }

                client.setRemoteHost(ftpHost);
                client.setUserName(ftpUser);
                client.setPassword(ftpPassword);
                if (ftpPassiveMode)
                    client.getAdvancedFTPSettings().setConnectMode(com.enterprisedt.net.ftp.FTPConnectMode.PASV);
                client.connect();
                if (!ftpDirectory.isEmpty())
                    client.changeDirectory(ftpDirectory);

                // always in the unas subdirectory
                try {                            
                    client.changeDirectory("unas");
                } catch (com.enterprisedt.net.ftp.FTPException e) {
                    client.createDirectory("unas");
                    client.changeDirectory("unas");
                }
            }
            
            uploadString(data, fileName);
                
            // The true return point
            return true;
        } catch (com.enterprisedt.net.ftp.FTPException e) {
            try {
                String message = getName() + ": " + e.getLocalizedMessage();

                Logger.getLogger(getClass().getName()).log(Level.FINE, message, e);
                System.err.println(getName() + ": [" + new java.util.Date().toString() + "] ftp error - " + e.getLocalizedMessage());

                client.disconnect(true);
            } catch (Throwable t) {

            } finally {
                client = null;

                lastErrorTime = System.currentTimeMillis();
            }
            

        } catch (Exception e) {                    
            try {
                String message = getName() + ": " + e.getLocalizedMessage();

                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message, e);

                client.disconnect(true);
            } catch (Throwable t) {
                
            } finally {
                client = null;
                lastErrorTime = System.currentTimeMillis();
            }
        }

        // We reach this location only in case of an error
        return false;
    }

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
            
            // Reset last timesetamp
            writeLastTimestamp(0);
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
            
            // Reset last timesetamp
            writeLastTimestamp(0);
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
    
    public int getFromTable() {
        return fromTable;
    }

    public void setFromTable(int fromTable) {
        if (fromTable != this.fromTable) {
            this.fromTable = fromTable;
            
            // Reset last timesetamp
            writeLastTimestamp(0);
        }
    }

    public int getToTable() {
        return toTable;
    }

    public void setToTable(int toTable) {
        if (toTable != this.toTable) {
            this.toTable = toTable;
            
            // Reset last timesetamp
            writeLastTimestamp(0);
        }
    }
    
    public boolean isFtpDebug() {
        return ftpDebug;
    }
    
    public void setFtpDebug(boolean ftpDebug) {
        this.ftpDebug = ftpDebug;
    }

    public int getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(int updateTime) {
        this.updateTime = updateTime;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        if (!script.equals(this.script))
            jsEngine = null;
        
        this.script = script;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        if (!method.equals(this.method))
            jsEngine = null;
        
        this.method = method;
    }
    

    @Override
    public void setMessage(int counter, String message) {
        
    }

    @Override
    public String getMessage(int counter) {
        return null;
    }

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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void counterAdded(int counter) {
        
    }

    @Override
    public void counterRemoved(int counter) {
        
    }

    @Override
    public void counterChanged(int counter) {
        
    }

    @Override
    public void counterError(int counter, Throwable t) {
        
    }

    // If the instance is enabled again, reset script engine,
    // so that we will reparse the script
    @Override
    public void setInstanceEnabled(boolean e) {
        if (e && !super.isInstanceEnabled())
            jsEngine = null;
        
        super.setInstanceEnabled(e);
    }
    
    private void uploadString(String str, String remoteFile) throws FTPException, IOException {
        byte[] bytes = str.getBytes(charsetUTF);
        
        try (FileTransferOutputStream fos = client.uploadStream(remoteFile)) {
            fos.write(bytes);
        }
    }

    // Read and write lastTimestamp in INI file
    // deliberately named readXxx / writeXxx so they don't appear in the settings
    private long readLastTimestamp() {
        return Properties.readLong("Unas", "lastTimestamp", lastTimestamp);
    }
    
    private void writeLastTimestamp(long currentMaxTimestamp) {
        Properties.writeLong("Unas", "lastTimestamp", currentMaxTimestamp);
        lastTimestamp = currentMaxTimestamp;
    }


    private String  ftpHost = "";
    private String  ftpUser = "";
    private String  ftpPassword = "";    
    private String  ftpDirectory = "";
    private boolean ftpPassiveMode = true;
    private boolean ftpSecure = false;
    private boolean ftpDebug = false;
    
    private int fromTable = 1;
    private int toTable = 999;
    
    private String script = "unas.js";
    private String method = "fixtures";

    private int updateTime = 30;
        
    private String name = getClass().getSimpleName();

    private com.enterprisedt.net.ftp.FileTransferClientInterface client = null;        
            
    final static private java.nio.charset.Charset charsetUTF = java.nio.charset.Charset.forName("UTF-8");
    
    private javax.script.ScriptEngine jsEngine;
    
    // Resolve path for scripts: sources, dist, cwd
    private static File[] scriptPathes = new File[] {
            new File("../src/countermanager/http/scripts"),
            new File(Properties.getIniFile().getParent() + File.separator + "http/scripts"),
            new File(".")
    };
}
