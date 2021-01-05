/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package countermanager.liveticker;

import countermanager.model.ICounterModelListener;


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
