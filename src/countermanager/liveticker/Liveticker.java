/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package countermanager.liveticker;

import countermanager.model.ICounterModelListener;
import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 *
 * @author Administrator
 */
public abstract class  Liveticker implements ICounterModelListener {

    public static String encryptPassword(String pwd) {
        if (pwd == null)
            return null;
        else if (pwd.isEmpty())
            return "";
        
        try {
            return Class.forName("countermanager.liveticker.ttm.TTMPrivate").getMethod("encryptPassword", String.class).invoke(null, pwd).toString();
        } catch (Exception ex) {
            
        }

        // In case of an error return the password to encrypt itself
        return pwd;        
    }
    
    public static String decryptPassword(String pwd) {
        if (pwd == null)
            return null;
        else if (pwd.isEmpty())
            return "";

        try {
            return Class.forName("countermanager.liveticker.ttm.TTMPrivate").getMethod("decryptPassword", String.class).invoke(null, pwd).toString();
        } catch (Exception ex) {
            
        }
        
        // In case of an error return the password to decrypt itself
        return pwd;        
    }
    
    public abstract void setMessage(int counter, String message);
    public abstract String getMessage(int counter);
    protected abstract void loadProperties();
    protected abstract void saveProperties();    
    public abstract String getName();
    public abstract void setName(String name);

    public void setInstanceEnabled(boolean e) {
        instanceEnabled = e;
    }
    
    public boolean isInstanceEnabled() {
        return instanceEnabled;
    }
        
    public boolean isEnabled() {
        // Combination of global enable and specific enable
        return globalEnabled && instanceEnabled;
    }
    
    protected boolean instanceEnabled = false;
    
    static boolean globalEnabled = false;
}
