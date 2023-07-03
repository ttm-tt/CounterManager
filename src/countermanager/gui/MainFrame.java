/* Copyright (C) 2020 Christoph Theis */

package countermanager.gui;

import countermanager.http.HTTP;
import countermanager.model.CounterModel;
import countermanager.model.ICounterModelListener;
import java.awt.Component;
import java.util.ResourceBundle;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.UnsupportedLookAndFeelException;

import static countermanager.prefs.Prefs.*;
import countermanager.prefs.Properties;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author  Administrator
 */
public class MainFrame extends javax.swing.JFrame {
    
    private static final int ITEM_SIZE = 100;
    
    private static final String VERSION_STRING = "23.06";
    private static final String COPYRIGHT_STRING = "(C) 2022 Christoph Theis";
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle("countermanager/gui/resources/CounterManager");
    
    CounterPanelItem counters[] = new CounterPanelItem[CounterModel.MAX_COUNTERS];
    
    private class ComboBoxLogRecord  {
        LogRecord record;

        public ComboBoxLogRecord(LogRecord record) {
            this.record = record;
        }

        @Override
        public String toString() {
            String date = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(record.getMillis()));
            String message;

            if (record.getMessage() != null)
                message = (new java.util.logging.SimpleFormatter()).formatMessage(record);
            else
                message = record.getThrown().getClass().getName() + ": " + record.getThrown().getLocalizedMessage();

            return "[" + date + "] " + message;
        }
    }

    /** Creates new form MainFrame */
    public MainFrame() {
        initComponents();
        
        Logger.getLogger("").addHandler(new Handler() {

            // synchronized, da merere gleichzeitig drauf zugreifen koennten
            @Override
            synchronized public void publish(LogRecord record) {
                if (record.getMessage() == null && record.getThrown() == null)
                    return;

                record.setResourceBundle(bundle);

                javax.swing.DefaultComboBoxModel model = (javax.swing.DefaultComboBoxModel) statusComboBox.getModel();
                model.insertElementAt(new ComboBoxLogRecord(record), 0);

                while (model.getSize() > 100)
                    model.removeElementAt(model.getSize() - 1);

                statusComboBox.setSelectedIndex(0);
                
                if (Level.SEVERE.intValue() <= record.getLevel().intValue()) {
                    statusComboBox.setForeground(java.awt.Color.RED);
                    statusComboBox.setFont(statusComboBox.getFont().deriveFont(0));                            
                } else if (Level.FINE.intValue() >= record.getLevel().intValue()) {
                    statusComboBox.setForeground(java.awt.Color.DARK_GRAY);
                    statusComboBox.setFont(statusComboBox.getFont().deriveFont(java.awt.Font.ITALIC));                                                
                } else {
                    statusComboBox.setForeground(java.awt.Color.BLACK);
                    statusComboBox.setFont(statusComboBox.getFont().deriveFont(0));                                                
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }

        });


        Logger.getLogger("countermanager").setLevel(Level.INFO);

        statusComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value == null)
                    return c;

                LogRecord record = ((ComboBoxLogRecord) value).record;

                if (Level.SEVERE.intValue() <= record.getLevel().intValue()) {
                    c.setForeground(java.awt.Color.RED);
                    c.setFont(c.getFont().deriveFont(0));
                } else if (Level.FINE.intValue() >= record.getLevel().intValue()) {
                    c.setForeground(java.awt.Color.DARK_GRAY);
                    c.setFont(c.getFont().deriveFont(java.awt.Font.ITALIC));
                } else {
                    c.setForeground(java.awt.Color.BLACK);
                    c.setFont(c.getFont().deriveFont(0));
                }

                return c;
            }
        });

        CounterModel.getDefaultInstance().addCounterModelListener(
                new ICounterModelListener() {
            @Override
                    public void counterAdded(int nr) {
                        MainFrame.this.counterAdded(nr);
                    }
                    
            @Override
                    public void counterRemoved(int nr) {
                        MainFrame.this.counterRemoved(nr);
                    }
                    
            @Override
                    public void counterChanged(int nr) {
                        MainFrame.this.counterChanged(nr);
                    }
                    
            @Override
                    public void counterError(int nr, Throwable t) {
                        MainFrame.this.counterError(nr, t);                        
                    }
        });
                
        // CounterModel.getDefaultInstance().addCounter(0);
        
        // Initialize settings
        updateSettings();
        
        // Initialize Tables from Preferences
        String layout = getProperties().getString("Layout", "");
        
        String[] tmp1 = layout.split(";");
        
        for (String tmp11 : tmp1) {
            if (tmp11.length() == 0) {
                continue;
    }
            String[] tmp2 = tmp11.split(",");
            int     nr     = Integer.parseInt(tmp2[0]);
            boolean active = Boolean.parseBoolean(tmp2[1]);
            int     x      = Integer.parseInt(tmp2[2]);
            int     y      = Integer.parseInt(tmp2[3]);
            int counter = nr;
            // Add counter first, so the callback functio will not do it also.
            counters[counter] = new CounterPanelItem(nr);
            counters[counter].setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
            counterPanel.add(counters[counter]);
            counters[counter].setBounds(x * ITEM_SIZE + 10, y * ITEM_SIZE + 10, ITEM_SIZE, ITEM_SIZE);
            // And now update the model
            CounterModel.getDefaultInstance().addCounter(counter);
            CounterModel.getDefaultInstance().setCounterActive(counter, active);
        }
    }
    
    // Add a counter representation
    public void counterAdded(final int nr) {
        if (counters[nr] != null)
            return;

        // Changes must be within the event dispatch thread
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                // Rough calculation of the best position.
                boolean  spots[][] = 
                        new boolean[counterPanel.getHeight() / ITEM_SIZE][counterPanel.getWidth() / ITEM_SIZE];

                int lastCounter = -1;
                for (int i = 0; i < counters.length; i++) {
                    if (counters[i] != null) {
                        lastCounter = i;
                        spots[counters[i].getY() / ITEM_SIZE][counters[i].getX() / ITEM_SIZE] = true;
                    }
                }
                
                int h = 0;
                int w = 0;
                boolean found = false;
                
                for (h = 0; !found && h < spots.length; h++) {
                    for (w = 0; !found && w < spots[h].length; w++) {
                        if (!spots[h][w]) {
                            found = true;
                        }
                    }
                }
                
                // We are 1 off because the increment is done before the check for !found
                h--;
                w--;
                
                // Change height if no space left
                if (h == spots.length) {                    
                    // TODO: Change size
                    // panelHeightSpinner.setValue(new Integer(h));
                }
                        
                counters[nr] = new CounterPanelItem(nr);
                counters[nr].setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
                counterPanel.add(counters[nr]);
                counters[nr].setBounds(w * ITEM_SIZE + 10, h * ITEM_SIZE + 10, ITEM_SIZE, ITEM_SIZE);                                   
                counters[nr].revalidate();
            }
        });                
    }
    
    
    // Remove a counter representation
    public void counterRemoved(final int nr) {
        if (counters[nr] == null)
            return;
        
        // Changes must be within the event dispatch thread
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                counterPanel.remove(counters[nr]);
                counters[nr] = null;          
                
                repaint();
            }
        });
    }
    
    
    // Change the counter data
    public void counterChanged(final int nr) {
        if (counters[nr] == null)            
            return;
        
        // Item will do changes that must be in the event dispatch thread
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                counters[nr].counterChanged();
            }
        });
    }
    
    
    public void counterError(int nr, Throwable t) {
        int tableOffset = getProperties().getInt(OFFSET_TABLE_PREF, 1);
        
        // if (CounterModel.getDefaultInstance().getErrorCount(nr) > 1)
        //     return;
        
        Logger.getLogger(getClass().getName()).log(
                Level.FINE, "tableErrorString",                 
                new Object[] {nr + tableOffset, t.getLocalizedMessage()});
    }
    
    
   /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        counterScrollPane = new javax.swing.JScrollPane();
        counterPanel = new javax.swing.JPanel() {
            public void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);

                java.awt.Color oldColor = g.getColor();
                g.setColor(java.awt.Color.LIGHT_GRAY);

                int startX = 10, endX = 10 + (getWidth() / ITEM_SIZE) * ITEM_SIZE;
                int startY = 10, endY = 10 + (getHeight() / ITEM_SIZE) * ITEM_SIZE;

                ((java.awt.Graphics2D) g).drawRect(
                    startX, startY, endX - startX, endY - startY);

                for (int w = startX; w < endX; w += ITEM_SIZE)
                g.drawLine(w, startY, w, endY);

                for (int h = startY; h < endY; h += ITEM_SIZE)
                g.drawLine(startX, h, endX, h);

                g.setColor(oldColor);
            }
        };
        statusComboBox = new javax.swing.JComboBox();
        menuBar = new javax.swing.JMenuBar();
        connectionsMenu = new javax.swing.JMenu();
        connectDatabaseMenuItem = new javax.swing.JCheckBoxMenuItem();
        connectCounterMenuItem = new javax.swing.JCheckBoxMenuItem();
        httpMenuItem = new javax.swing.JCheckBoxMenuItem();
        livetickerMenuItem = new javax.swing.JCheckBoxMenuItem();
        smsMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        refreshCounterMenuItem = new javax.swing.JMenuItem();
        preferencesMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        setAllActiveMenuItem = new javax.swing.JMenuItem();
        lockscreenMenuItem = new javax.swing.JCheckBoxMenuItem() {
            public boolean isSelected() {return !CounterModel.getDefaultInstance().allScreensUnlocked();}
        };
        debugMenuItem = new javax.swing.JCheckBoxMenuItem();
        largeFontMenuItem = new javax.swing.JCheckBoxMenuItem();
        openIniFileMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager"); // NOI18N
        setTitle(bundle.getString("counterManagerTitle")); // NOI18N
        setName("counterManagerFrame"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        counterScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        counterPanel.setLayout(null);
        counterScrollPane.setViewportView(counterPanel);

        connectionsMenu.setText(bundle.getString("connectionsMenu")); // NOI18N

        connectDatabaseMenuItem.setText(bundle.getString("connectDatabaseMenuItem")); // NOI18N
        connectDatabaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectDatabaseMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(connectDatabaseMenuItem);

        connectCounterMenuItem.setText(bundle.getString("connectCounterMenuItem")); // NOI18N
        connectCounterMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectCounterMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(connectCounterMenuItem);

        httpMenuItem.setText(bundle.getString("httpServerMenuItem")); // NOI18N
        httpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                httpMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(httpMenuItem);

        livetickerMenuItem.setText(bundle.getString("Liveticker")); // NOI18N
        livetickerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                livetickerMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(livetickerMenuItem);

        smsMenuItem.setText(bundle.getString("smsMenuItem")); // NOI18N
        smsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                smsMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(smsMenuItem);
        connectionsMenu.add(jSeparator1);

        refreshCounterMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0));
        refreshCounterMenuItem.setText(bundle.getString("refreshCounterMenuItem")); // NOI18N
        refreshCounterMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshCounterMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(refreshCounterMenuItem);

        preferencesMenuItem.setText(bundle.getString("preferencesMenuItem")); // NOI18N
        preferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferencesMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(preferencesMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        saveMenuItem.setText(bundle.getString("saveMenuItem")); // NOI18N
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(saveMenuItem);
        connectionsMenu.add(jSeparator2);

        setAllActiveMenuItem.setText(bundle.getString("setAllActiveMenuItem")); // NOI18N
        setAllActiveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setAllActiveMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(setAllActiveMenuItem);

        lockscreenMenuItem.setText(bundle.getString("lockscreenMenuItem")); // NOI18N
        lockscreenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockscreenMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(lockscreenMenuItem);

        debugMenuItem.setText(bundle.getString("debugMenuItem")); // NOI18N
        debugMenuItem.setActionCommand("jdebugMenuItem");
        debugMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debugMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(debugMenuItem);

        largeFontMenuItem.setText("Large Font");
        largeFontMenuItem.setActionCommand(bundle.getString("largeFontMenuItem")); // NOI18N
        largeFontMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                largeFontMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(largeFontMenuItem);

        openIniFileMenuItem.setText("Load Configuration");
        openIniFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openIniFileMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(openIniFileMenuItem);

        exitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_DOWN_MASK));
        exitMenuItem.setText(bundle.getString("exitMenuItem")); // NOI18N
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        connectionsMenu.add(exitMenuItem);

        menuBar.add(connectionsMenu);

        helpMenu.setText(bundle.getString("helpMenu")); // NOI18N

        aboutMenuItem.setText(bundle.getString("aboutMenuItem")); // NOI18N
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusComboBox, 0, 834, Short.MAX_VALUE)
                    .addComponent(counterScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 834, Short.MAX_VALUE))
                .addGap(10, 10, 10))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(counterScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 540, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // Check for changed layout
        String layout = getLayoutString();
        
        if ( !layout.equals(getProperties().getString(LAYOUT_PREF, "")) ) {
            String msg = bundle.getString("saveLayoutString");
            
            String title = bundle.getString("counterManagerTitle");
            if (JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                getProperties().putString(LAYOUT_PREF, layout);
        }
        
        java.util.prefs.Preferences prefs = Preferences.userRoot().node("/de/countermanager");
        prefs.putInt("left", getBounds().x);
        prefs.putInt("top", getBounds().y);
        prefs.putInt("width", getBounds().width);
        prefs.putInt("height", getBounds().height);
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            
        }
        setVisible(false);                
        dispose();
        
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void httpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_httpMenuItemActionPerformed
        if (httpMenuItem.isSelected()) {
            String addDirs = getProperties().getString(ADD_DIRS_PREF, "");
            String scriptFiles = getProperties().getString(SCRIPT_PREF, "");
            int port = getProperties().getInt(HTTP_PORT_PREF, 80);

            HTTP.getDefaultInstance().setAliases(addDirs);
            HTTP.getDefaultInstance().setScriptFiles(scriptFiles);
            if (!HTTP.getDefaultInstance().startHttpServer(port)) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not start HTTP Server");
            } else {            
                Logger.getLogger(getClass().getName()).log(Level.INFO, "startHttpString", port);
            }
        } else {
            HTTP.getDefaultInstance().stopHttpServer();
            
            Logger.getLogger(getClass().getName()).log(Level.INFO, "stopHttpServer");
        }
    }//GEN-LAST:event_httpMenuItemActionPerformed

    private String getLayoutString() {
        String layout = "";
        
        for (short i = 0; i < counters.length; i++) {
            if (counters[i] == null)
                continue;
            
            String counterString = "" +
                    i + "," + 
                    CounterModel.getDefaultInstance().isCounterActive(i) + "," +
                    counters[i].getX() / ITEM_SIZE + "," +
                    counters[i].getY() / ITEM_SIZE;
            
            layout += counterString + ";";
        }
        
        return layout;
    }
    
    
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        countermanager.prefs.Properties prefs = getProperties();
        
        String layout = getLayoutString();
        
        prefs.put(LAYOUT_PREF, layout);
        
        try {            
            writeProperties(prefs);
        } catch (java.io.IOException ex) {
            ex.printStackTrace(System.err);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        String aboutMessage =                 
                "<html>" +
                    "<strong>" + bundle.getString("counterManagerTitle") + "</strong><br>" +
                    "Version " + VERSION_STRING + "<br>" +
                    COPYRIGHT_STRING + 
                "</html>";
        
        JOptionPane.showMessageDialog(this, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void connectDatabaseMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectDatabaseMenuItemActionPerformed
        // Selected flag is first updated, then the actions are called
        if (!connectDatabaseMenuItem.isSelected()) {
            CounterModel.getDefaultInstance().disconnectDatabase();
        } else {
            countermanager.prefs.Properties prefs = getProperties();
            String databaseType = prefs.getString(DATABASE_CLASS_PREF, countermanager.model.database.ttm.TTM.class.getName());
        
            CounterModel.getDefaultInstance().connectDatabase(databaseType);
        }
    }//GEN-LAST:event_connectDatabaseMenuItemActionPerformed

    private void refreshCounterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshCounterMenuItemActionPerformed
        
            new Thread(new Runnable() {
            @Override
                public void run() {
                    java.awt.Cursor oldCursor = getCursor();
                    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                    
                    CounterModel.getDefaultInstance().refresh();
                    
                    setCursor(oldCursor);
                }
            }).start();            
        
    }//GEN-LAST:event_refreshCounterMenuItemActionPerformed

    private void connectCounterMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectCounterMenuItemActionPerformed
        // Selected flag is first updated, then the actions are called
        if (!connectCounterMenuItem.isSelected()) {
            CounterModel.getDefaultInstance().disconnectCounters();
                        
            Logger.getLogger(getClass().getName()).log(Level.INFO, "disconnectedFromCounters");
        } else {
            final countermanager.prefs.Properties prefs = getProperties();
            
            final boolean broadcast = prefs.getBoolean(BROADCAST_PREF, false);
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    java.awt.Cursor oldCursor = getCursor();
                    setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                    
                    CounterModel.setBroadcast(broadcast);
                    
                    CounterModel.getDefaultInstance().connectCounters(prefs);
                    
                    setCursor(oldCursor);
                }
            }).start();            
        }
    }//GEN-LAST:event_connectCounterMenuItemActionPerformed

    private void preferencesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferencesMenuItemActionPerformed
        countermanager.prefs.Properties prefs = getProperties();
        ConfigurationPanel panel = new ConfigurationPanel();
        panel.readProperties(prefs);
        int ret = JOptionPane.showConfirmDialog(
                this, panel, 
                bundle.getString("preferencesString"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ret == JOptionPane.OK_OPTION) {
            try {            
                panel.writeProperties(prefs);

                // Write properties (will be read again in updateSettings)
                writeProperties(prefs);

                // Update some settings
                updateSettings();
            } catch (java.io.IOException ex) {
                ex.printStackTrace(System.err);
            }
        } else {
            // Liveticker neu starten
            countermanager.liveticker.LivetickerAdmininstration.loadLiveticker();
        }
    }//GEN-LAST:event_preferencesMenuItemActionPerformed

    private void livetickerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_livetickerMenuItemActionPerformed
        countermanager.liveticker.LivetickerAdmininstration.setLivetickerEnabled(livetickerMenuItem.getState());
    }//GEN-LAST:event_livetickerMenuItemActionPerformed

    private void debugMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debugMenuItemActionPerformed
        countermanager.driver.CounterDriver.setDebug(debugMenuItem.getState());
    }//GEN-LAST:event_debugMenuItemActionPerformed

    private void setAllActiveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setAllActiveMenuItemActionPerformed
                countermanager.model.CounterModel.getDefaultInstance().setAllCounterActive();
    }//GEN-LAST:event_setAllActiveMenuItemActionPerformed

    private void largeFontMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_largeFontMenuItemActionPerformed
        if (largeFontMenuItem.getState())
            changeFont(1.5f);
        else
            changeFont(1.0f);
    }//GEN-LAST:event_largeFontMenuItemActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        java.util.prefs.Preferences prefs = Preferences.userRoot().node("/de/countermanager");
        int x = prefs.getInt("left", 0);
        int y = prefs.getInt("top", 0);
        
        setBounds(x, y, getWidth(), getHeight());
    }//GEN-LAST:event_formWindowOpened

    private void smsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smsMenuItemActionPerformed
        CounterModel.getDefaultInstance().enableSmsAlerts(smsMenuItem.getState(), getProperties());
    }//GEN-LAST:event_smsMenuItemActionPerformed

    private void lockscreenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockscreenMenuItemActionPerformed
        CounterModel.getDefaultInstance().lockAllScreens(!lockscreenMenuItem.isSelected());        
    }//GEN-LAST:event_lockscreenMenuItemActionPerformed

    private void openIniFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openIniFileMenuItemActionPerformed
        File old = countermanager.prefs.Properties.getIniFile();
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
        FileNameExtensionFilter cfgFilter = new FileNameExtensionFilter(bundle.getString("configurationFileFilter"), "ini");
        fc.addChoosableFileFilter(cfgFilter);
        fc.setFileFilter(cfgFilter);
        fc.setSelectedFile(old);
        int ret = fc.showOpenDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION)
            return;
        
        File ini = fc.getSelectedFile();

        Properties.setIniFile(ini);
        
        countermanager.liveticker.LivetickerAdmininstration.loadLiveticker();
        
        setTitle(bundle.getString("counterManagerTitle") + " - " + ini.getName());
    }//GEN-LAST:event_openIniFileMenuItemActionPerformed
        
    private java.awt.Component getInvoker(java.awt.Component c) {        
        while (c.getParent() != null)
            c = c.getParent();
        
        if (c instanceof javax.swing.JPopupMenu)
            return ((javax.swing.JPopupMenu) c).getInvoker();
        else
            return c;
    }

    // Get the preferences, where the configuation is stored.
    private countermanager.prefs.Properties getProperties() {
        countermanager.prefs.Properties properties = new countermanager.prefs.Properties();
        
        try {
            properties.load(getClass().getName());
        } catch (Exception ex) {
            
        }

        return properties;
    }
    
    
    private void writeProperties(countermanager.prefs.Properties prop) throws java.io.IOException {
        prop.store(getClass().getName());
        
        countermanager.liveticker.LivetickerAdmininstration.storeLiveticker();
    }
    
    
    // Update some settings
    private void updateSettings() {
        countermanager.prefs.Properties prefs = getProperties();
        
        CounterModel.setResetTimeout(prefs.getInt(RESET_TIMEOUT_PREF, 30) * 1000);
        
        int tableOffset = prefs.getInt(OFFSET_TABLE_PREF, 1);
        CounterModel.getDefaultInstance().setTableOffset(tableOffset);

        int fromTable = prefs.getInt(FROM_TABLE_PREF, 1);
        int toTable   = prefs.getInt(TO_TABLE_PREF, CounterModel.MAX_COUNTERS);
        CounterModel.getDefaultInstance().setTableRange(fromTable, toTable);        

        if (HTTP.getDefaultInstance().isHttpServerRunning()) {
            HTTP.getDefaultInstance().setScriptFiles(prefs.getString(SCRIPT_PREF, null));
            HTTP.getDefaultInstance().setAliases(prefs.getString(ADD_DIRS_PREF, null));
        }
    }
    
    // Ceck for Settings. We could implement a hierarchy listener. 
    // Or just check for setVisible
    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        
        if (v && getProperties().getInt(FROM_TABLE_PREF, 0) == 0) {
            // The argument "ActionEvent" is ignored.
            preferencesMenuItemActionPerformed(null);
        }
    }
    
    
    @Override
    public List<java.awt.Image> getIconImages() {
        // Icon from http://123rf.com
        java.util.ArrayList<java.awt.Image> imageList = new java.util.ArrayList<>();
        imageList.add(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/countermanager/gui/resources/CounterManager.png")));
        return imageList;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace(System.err);
        }
        
        // Fuer Screenshots brauche ich das GUI in Englisch
        // java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        
        // Init enthaelt u.U, Pfade wir "F: \ cht \ user \ TTM", per default wird aber \ u als Beginn
        // einer Unicode sequence interpretiert und das ergibt einen Parsefehler.
        // Ich muss sogar im Kommentar Leerzeichen nach dem \ einfuegen ...
        System.setProperty(org.ini4j.Config.KEY_PREFIX + org.ini4j.Config.PROP_ESCAPE, Boolean.FALSE.toString());
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame();
                
                java.util.prefs.Preferences prefs = Preferences.userRoot().node("/de/countermanager");
                int x = prefs.getInt("left", 0);
                int y = prefs.getInt("top", 0);
                int w = prefs.getInt("width", frame.getWidth());
                int h = prefs.getInt("height", frame.getHeight());
                frame.setBounds(x, y, w, h);
                
                frame.setVisible(true);
            }
        });
    }
    
    private Map<Object, java.awt.Font> defaultFonts = null;
    
    private void changeFont(float fac) {
        if (defaultFonts == null) {
            defaultFonts = new java.util.HashMap<>();
            
            for (java.util.Enumeration e = javax.swing.UIManager.getDefaults().keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                Object value = javax.swing.UIManager.get(key);

                if (value instanceof java.awt.Font) {
                    defaultFonts.put(key, (java.awt.Font) value);
                }
            }
        }
        
        for (java.util.Enumeration e = javax.swing.UIManager.getDefaults().keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Object value = javax.swing.UIManager.get(key);
            
            if (value instanceof java.awt.Font) {
                java.awt.Font f = defaultFonts.get(key);

                javax.swing.UIManager.put(key, new javax.swing.plaf.FontUIResource(f.getName(), f.getStyle(), (int) (f.getSize() * fac)));
            }
        }
        
        javax.swing.SwingUtilities.updateComponentTreeUI(this);
    }
    
    private boolean allScreensLocked() {
        return false;
    }
    
    private void lockAllScreens(boolean lock) {
        
    }
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JCheckBoxMenuItem connectCounterMenuItem;
    private javax.swing.JCheckBoxMenuItem connectDatabaseMenuItem;
    private javax.swing.JMenu connectionsMenu;
    private javax.swing.JPanel counterPanel;
    private javax.swing.JScrollPane counterScrollPane;
    private javax.swing.JCheckBoxMenuItem debugMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem httpMenuItem;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JCheckBoxMenuItem largeFontMenuItem;
    private javax.swing.JCheckBoxMenuItem livetickerMenuItem;
    private javax.swing.JCheckBoxMenuItem lockscreenMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openIniFileMenuItem;
    private javax.swing.JMenuItem preferencesMenuItem;
    private javax.swing.JMenuItem refreshCounterMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JMenuItem setAllActiveMenuItem;
    private javax.swing.JCheckBoxMenuItem smsMenuItem;
    private javax.swing.JComboBox statusComboBox;
    // End of variables declaration//GEN-END:variables
    
}
