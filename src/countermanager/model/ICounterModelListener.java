/* Copyright (C) 2020 Christoph Theis */

/*
 * ICounterModelListener.java
 *
 * Created on 2. Januar 2007, 11:59
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package countermanager.model;

/**
 *
 * @author Christoph Theis
 */
public interface ICounterModelListener extends java.util.EventListener {
    /// A counter has been added
    public void counterAdded(int counter);
    
    // A counter has been removed
    public void counterRemoved(int counter);

    // Data for a counter have been changed
    void counterChanged(int counter);
    
    // Error from counter
    void counterError(int counter, Throwable t);
}
