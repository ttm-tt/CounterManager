/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.prefs;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.Profile;

/**
 *
 * @author chtheis
 */
public class Properties extends java.util.Properties {
    
    private static File findPath(String what) {
        File file = null;

        // 1) cwd
        try {
            file = new File(System.getProperty("user.dir") + File.separator + what);

            if (!file.exists())
                file = null;
        } catch (Exception ex) {
            // Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, null, ex);   
            file = null;
        }
        
        // 2) C:\ProgramData (unter Win 7) oder entsprechend unter Win XP
        if (file == null) {
            try {
                file = new File(System.getenv("ALLUSERSPROFILE") + File.separator + "TTM" + File.separator + what);
                
                if (!file.exists())
                    file = null;
            } catch (Exception ex) {
                // Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, null, ex);   
                file = null;
            }
        }

        // 3) Roaming profile vom Benutzer: C:\Users\<user>\AppData\Roaming
        //    Eigentlich nur ein Versehen vom TTM Installer
        if (file == null) {
            try {
                file = new File(System.getenv("APPDATA") + File.separator + "TTM" + File.separator + what);
                
                if (!file.exists())
                    file = null;
            } catch (Exception ex) {
                // Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, null, ex);   
                file = null;
            }
        }

        // 4) Local profile vom Benutzer: C:\Users\<user>\AppData\Local
        if (file == null) {
            try {
                file = new File(System.getenv("LOCALAPPDATA") + File.separator + "TTM" + File.separator + what);
                
                if (!file.exists())
                    file = null;
            } catch (Exception ex) {
                // Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, null, ex);   
                file = null;
            }
        }

    return file;
    }

    
    public static File getIniFile() {
        return getIniFile("countermanager.ini");
    }
    
    
    public static File getIniFile(String fileName) {
        File ini = null;
        
        // 1) Im aktuellen Verzeichnis nach countermanger.ini suchen
        ini = new File(System.getProperty("user.dir"), fileName);
        if (ini.exists())
            return ini;
        
        // 2) Nach CounterManager/countermanager.ini suchen
        //    Das File wird von einer Installation angelegt
        ini = findPath("CounterManager" + File.separator + fileName);
        if (ini != null)
            return ini;
        
        // 3) Wenn alles nichts hilft kommen wir nicht von einer Installation
        //    In dem Fall ini file im aktuellen Verzeichnis anlegen
        ini = new File(System.getProperty("user.dir"), fileName);
        try {
            ini.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Properties.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ini;
    }
    
    public void load(String sectionName) throws IOException {
        clear();
        
        org.ini4j.Ini ini = new org.ini4j.Ini();
        ini.setFile(getIniFile());
        
        try {
            ini.load();
        } catch (Exception ex) {
            
        }
        
        Profile.Section section = ini.get(sectionName);
        
        if (section != null){
            for (String s : section.keySet()) {
                setProperty(s, section.get(s));                    
            }
        }
    }
    
    public void store(String sectionName) throws IOException {
        
        org.ini4j.Ini ini = new org.ini4j.Ini();
        ini.setFile(getIniFile());
        
        try {
            ini.load();
        } catch (Exception ex) {
            
        }
        
        ini.remove(sectionName);
        
        for (Object s : keySet()) {
            ini.add(sectionName, s.toString(), getProperty(s.toString()));
        }
        
        ini.store();
    }
    
    public int getInt(String key, int defaultValue) {
        String val = getProperty(key);
        if (val == null || val.isEmpty())
            return defaultValue;
        
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
        
    public void putInt(String key, int val) {
        setProperty(key, Integer.toString(val));
    }
        
    public String getString(String key, String defaultValue) {
        String val = getProperty(key);
        return (val == null ? defaultValue : val);
    }
        
    public void putString(String key, String val) {
        setProperty(key, val);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getProperty(key);
        if (val == null || val.isEmpty())
            return defaultValue;
        
        return Boolean.parseBoolean(val);
    }
    
    public void putBoolean(String key, boolean val) {
        setProperty(key, Boolean.toString(val));
    }
    
    
    // Read specific value form ini file
    public static long readLong(String section, String key, long defaultValue) {
        org.ini4j.Ini ini = new org.ini4j.Ini();
        ini.setFile(getIniFile());
        
        try {
            ini.load();
        } catch (Exception ex) {
            
        }
        
        String val = ini.get(section, key);

        if (val == null || val.isEmpty())
            return defaultValue;
        
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
        
    // Write specific value to ini file
    public static void writeLong(String section, String key, long val) {
        org.ini4j.Ini ini = new org.ini4j.Ini();
        ini.setFile(getIniFile());
        
        try {
            ini.load();
        } catch (Exception ex) {
            
        }
     
        ini.add(section, key, val);
    }
}
