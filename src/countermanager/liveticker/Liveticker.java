/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package countermanager.liveticker;

import countermanager.model.ICounterModelListener;
import countermanager.prefs.Properties;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 *
 * @author Administrator
 */
public abstract class  Liveticker implements ICounterModelListener {

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
