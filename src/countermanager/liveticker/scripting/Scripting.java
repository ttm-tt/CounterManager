/* Copyright (C) 2020 Christoph Theis */
package countermanager.liveticker.scripting;

import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FileTransferOutputStream;
import countermanager.driver.CounterData;
import countermanager.liveticker.Liveticker;
import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;
import countermanager.prefs.PasswordCrypto;
import countermanager.prefs.Preferences;
import countermanager.prefs.Properties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptException;

/**
 *
 * @author chtheis
 */
public class Scripting extends Liveticker {

    @Override
    public void setMessage(int counter, String message) {

    }

    @Override
    public String getMessage(int counter) {
        return null;
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
        if (counter < fromTable - CounterModel.getDefaultInstance().getTableOffset())
            return;
        if (counter > toTable - CounterModel.getDefaultInstance().getTableOffset())
            return;
        
        List<CounterModelMatch> matchList = new java.util.ArrayList<>();
        List<CounterData> dataList = new java.util.ArrayList<>();
        
        for (int table = fromTable; table <= toTable; ++table) {
            matchList.add(CounterModel.getDefaultInstance().getCounterMatch(table - 1));
            dataList.add(CounterModel.getDefaultInstance().getCounterData(table - 1));
        }
        
        Object o = runScript("counterChanged", counter, matchList, dataList);

        if (o == null)
            return;
        
        String file = getOutputName();
        String content = null;
        
        if ((o instanceof Map)){
            if ( ((Map) o).containsKey("file") && ((Map) o).containsKey("content")) {
                file = ((Map) o).get("file").toString();
                content = ((Map) o).get("content").toString();
            } else {
                Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, "Returned map has not the required keys");
                return;
            }
        } else if (o instanceof String) {
            content = o.toString();
        } else {
            Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, "Illegal return type " + o.getClass().getSimpleName());
            return;
        }

        // Check if we have to store the result
        File outputFile = new File(getLocalDirectory(), file);
        boolean doSave = !outputFile.exists();
        doSave |= !content.equals(lastResult.get(file));

        if (!content.isEmpty() && doSave) {     
            outputFile.getParentFile().mkdirs();

            try (PrintWriter pw = new PrintWriter(outputFile.getPath(), StandardCharsets.UTF_8)) {
                // Store with current timestamp
                pw.print(content.replaceAll("<CURRENT_TIMESTAMP>", "" + System.currentTimeMillis()));
                lastResult.put(file, content);  // Unchanged string

                // We ignore errors in upload, for the moment at least
                upload(file, content.replaceAll("<CURRENT_TIMESTAMP>", "" + System.currentTimeMillis()));                    
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, ex);
                lastResult.remove(file);
            } catch (IOException ex) {
                Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, ex);
                lastResult.remove(file);
            }
        }
    }

    @Override
    public void counterError(int counter, Throwable t) {

    }

    public String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
    }

    public String getFtpUser() {
        return ftpUser;
    }

    public void setFtpUser(String ftpUser) {
        this.ftpUser = ftpUser;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public String getFtpDirectory() {
        return ftpDirectory;
    }

    public void setFtpDirectory(String ftpDirectory) {
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

    public boolean isFtpDebug() {
        return ftpDebug;
    }

    public void setFtpDebug(boolean ftpDebug) {
        this.ftpDebug = ftpDebug;
    }

    public int getFromTable() {
        return fromTable;
    }

    public void setFromTable(int fromTable) {
        this.fromTable = fromTable;
    }

    public int getToTable() {
        return toTable;
    }

    public void setToTable(int toTable) {
        this.toTable = toTable;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        if (script == null)
            return;
        
        this.script = script;
        lastModifiedScript = 0;
    }
    
    public Map<String, Object> getScriptOptions() {
        if (scriptOptions == null || scriptOptions.isEmpty()) {
            Object o = runScript("getScriptOptions");
            
            if (o == null || !(o instanceof Map))
                return new java.util.HashMap<>();
            
            // We need top copy the Map, because JS uses their own implementation,
            // which doesn't support put etc.
            scriptOptions = new java.util.HashMap<>();
            scriptOptions.putAll((Map) o);
        }
        
        return scriptOptions;
    }
    
    public void setScriptOptions(Map val) {
        if (val == null || val.isEmpty())
            val = new java.util.HashMap<>();
        
        // We need top copy the Map, because JS uses their own implementation,
        // which doesn't support put etc.
        scriptOptions = new java.util.HashMap<>();
        scriptOptions.putAll((Map) val);
        
        runScript("setScriptOptions", scriptOptions);
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
    
    
    protected Object runScript(String method, Object... args) {
        String s = null;
        
        // Use a script to convert that list to sthg
        try {
            File fs = null;
            for (File path : scriptPathes) {
                fs = new File(path, script);
                if (fs.exists())
                    break;
            }

            if (fs == null || !fs.exists()) {
                Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, "Script file not found"); 
                return null;
            }
            
            if (fs.lastModified() != lastModifiedScript)
                jsEngine = null;

            if (jsEngine == null) {
                jsEngine = new javax.script.ScriptEngineManager().getEngineByExtension("js");

                jsEngine.eval(new java.io.FileReader(fs));
                
                lastModifiedScript = fs.lastModified();
            }
            
            Object o = ((Invocable) jsEngine).invokeFunction(method, args);      

            return o;
        } catch (FileNotFoundException | ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(Scripting.class.getName()).log(Level.SEVERE, null, ex);

            jsEngine = null;
            lastResult.clear();
            
            return null;
        }
    }
    

    private boolean upload(String fileName, String data) {
        if (ftpHost.isEmpty())
            return true;
        
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
                    // Eigentlich geschieht das fuer de.webgen in MainFrame, aber aus
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
            }
            

        } catch (Exception e) {                    
            try {
                String message = getName() + ": " + e.getLocalizedMessage();

                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message, e);

                client.disconnect(true);
            } catch (Throwable t) {
                
            } finally {
                client = null;
            }
        }

        // We reach this location only in case of an error
        return false;
    }

    private void uploadString(String str, String remoteFile) throws FTPException, IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        
        try (FileTransferOutputStream fos = client.uploadStream(remoteFile)) {
            fos.write(bytes);
        }
    }

    
    private String  ftpHost = "";
    private String  ftpUser = "";
    private String  ftpPassword = "";    
    private String  ftpDirectory = "";
    private boolean ftpPassiveMode = true;
    private boolean ftpSecure = false;
    private boolean ftpDebug = false;
    private String  outputName = "tvprod.csv";
    private String  localDirectory = "";
    private Map<String, String>  lastResult = new java.util.HashMap<>();
    
    private int fromTable = 1;
    private int toTable = 1;
    
    private String script = "tvprod.js";
    private long   lastModifiedScript = 0;
    
    private Map scriptOptions = new java.util.HashMap<>();
    
    private String name = getClass().getSimpleName();

    private javax.script.ScriptEngine jsEngine;
    
    // Resolve path for scripts: sources, dist, cwd
    private static File[] scriptPathes = new File[] {
            new File("../src/countermanager/liveticker/scripting/scripts"),
            new File(Properties.getIniFile().getParent(), "liveticker/scripting/scripts"),
            new File(Properties.getIniFile().getParent(), "http/scripts"),
            new File(".")
    };

    private com.enterprisedt.net.ftp.FileTransferClientInterface client = null;                    

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    /**
     * @return the localDirectory
     */
    public String getLocalDirectory() {
        return localDirectory;
    }

    /**
     * @param localDirectory the localDirectory to set
     */
    public void setLocalDirectory(String localDirectory) {
        this.localDirectory = localDirectory;
    }
 
    final private static com.google.gson.Gson json = new com.google.gson.Gson();
}
