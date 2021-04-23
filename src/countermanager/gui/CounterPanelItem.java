/* Copyright (C) 2020 Christoph Theis */

/*
 * CounterPanelItem.java
 *
 * Created on 3. Januar 2007, 21:26
 */

package countermanager.gui;

import countermanager.driver.CounterConfig;
import countermanager.driver.CounterData;
import countermanager.driver.CounterDriver;
import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.DateFormat;
import javax.swing.JOptionPane;

import java.util.List;

/**
 *
 * @author  Christoph Theis
 */
public final class CounterPanelItem extends javax.swing.JPanel {
    
    // Allowed overtime for a match. 
    // If a match does not start after this time this item will go red.
    public final static int ALLOWED_OVERTIME = 5;
    
    // Supress now alert notificatons for this time after the last one
    private final static long ALERT_SUPPRESSION_TIME = 60 * 1000;
    
    private int     counter;
    private long    lastMatchFinished = System.currentTimeMillis();
    private boolean hasAlert = false;
    private long    lastAlertTime = 0;

    /** Creates new form CounterPanelItem */
    public CounterPanelItem(int counter) {
        this.counter = counter;
        
        initComponents();
        
        // Update colors 
        counterChanged();
    }
    

    public int getCounter() {
        return counter;
    }
    
    
   
    @Override
    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);
        
        CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(counter);
        CounterData counterData = CounterModel.getDefaultInstance().getCounterData(counter);     
        
        java.awt.Font oldFont = g.getFont();
        
        g.setFont(oldFont.deriveFont(java.awt.Font.BOLD, oldFont.getSize() * 1.2f));
        
        String str = 
                java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                    .getString("tableString") + " " + (counter + CounterModel.getDefaultInstance().getTableOffset());
        
        int offset = getHeight() / 10, x, y;
        
        x = ((getWidth() - g.getFontMetrics().stringWidth(str)) / 2);
        y = offset + ((g.getFontMetrics().getAscent()) / 2);
        
        ((Graphics2D) g).drawString(str, x, y);
        
        g.setFont(oldFont);
        
        offset += getHeight() / 5;
        
        if (counterMatch != null) {
            if (counterData == null || counterData.getGameMode() == CounterData.GameMode.RESET)
                str = String.format("%1$TH:%1$TM", new java.util.Date((long) counterMatch.mtDateTime));
            else if (counterData.getGameMode() == CounterData.GameMode.RUNNING)
                str = counterData.getTimeMode().name();
            else 
                str = counterData.getGameMode().name();
            
            x = ((getWidth() - g.getFontMetrics().stringWidth(str)) / 2);
            y = offset + ((g.getFontMetrics().getAscent()) / 2);

            ((Graphics2D) g).drawString(str, x, y);
            
            offset += getHeight() / 5;

            if (counterData == null)
                str = counterMatch.cpName;
            else if (counterData.getGameMode() == CounterData.GameMode.RESET)
                str = counterMatch.cpName;
            else if (counterData.getGameMode() == CounterData.GameMode.RUNNING)
                str = "" + counterData.getTime();
            else if (counterData.getGameMode() == CounterData.GameMode.WARMUP)
                str = "" + counterData.getTime();
            else 
                str = "";

            if (str == null)
                str = "";
            
            x = ((getWidth() - g.getFontMetrics().stringWidth(str)) / 2);
            y = offset + ((g.getFontMetrics().getAscent()) / 2);

            ((Graphics2D) g).drawString(str, x, y);
                   
            offset += getHeight() / 5;
            
            boolean isSwapped = counterData != null && counterData.isSwapped();
            
            if (counterData == null || counterMatch.plA.naName == null || counterMatch.plX.naName == null)
                str = counterMatch.grName;
            else if (isSwapped)
                str = "" + counterMatch.plX.naName + 
                      " - " + counterMatch.plA.naName; 
            else
                str = "" + counterMatch.plA.naName + 
                      " - " + counterMatch.plX.naName;
            
            x = ((getWidth() - g.getFontMetrics().stringWidth(str)) / 2);
            y = offset + ((g.getFontMetrics().getAscent()) / 2);

            ((Graphics2D) g).drawString(str, x, y);
                   
            offset += getHeight() / 5;
            
            str = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                    .getString("shortRoundString") + ": ";
            
            if (counterData != null && counterMatch.plA.plNr > 0 && counterMatch.plX.plNr > 0) {
                if (isSwapped)
                    str = "" + counterMatch.plX.plNr + 
                          " - " + counterMatch.plA.plNr; 
                else
                    str = "" + counterMatch.plA.plNr + 
                          " - " + counterMatch.plX.plNr;
                
            } else if (counterMatch.grModus == 1) {
                str += counterMatch.mtRound;
            } else {
                int maxRounds = 1;
                while ( (1 << maxRounds) < counterMatch.grSize )
                    maxRounds++;
                
                if (counterMatch.mtRound == maxRounds)
                    str += java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("shortFinalString");
                else if (counterMatch.mtRound == maxRounds - 1)
                    str += java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("shortSemiFinalString");
                // else if (match.mtRound == maxRounds - 2)
                //     str += java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("shortQuarterFinalString");
                else
                    str += "1/" + (1 << (maxRounds - counterMatch.mtRound));                    
            }
            
            x = ((getWidth() - g.getFontMetrics().stringWidth(str)) / 2);
            y = offset + ((g.getFontMetrics().getAscent()) / 2);

            ((Graphics2D) g).drawString(str, x, y);                   
        }
    }
    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        counterPopupMenu = new javax.swing.JPopupMenu();
        versionMenuItem = new javax.swing.JMenuItem();
        refreshMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        configMenuItem = new javax.swing.JMenuItem();
        dataMenuItem = new javax.swing.JMenuItem();
        resultsMenuItem = new javax.swing.JMenuItem();
        matchMenuItem = new javax.swing.JMenuItem();
        addressMenuItem = new javax.swing.JMenuItem();
        vncMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        setMessageMenuItem = new javax.swing.JMenuItem();
        enableLivetickerMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        activateMenuItem = new javax.swing.JCheckBoxMenuItem();
        resetMenuItem = new javax.swing.JMenuItem();
        lockscreenMenuItem = new javax.swing.JCheckBoxMenuItem();
        selectMatchMenuItem = new javax.swing.JCheckBoxMenuItem() {
            public boolean isSelected() {return CounterPanelItem.this.isCounterMatchForced();}
        };
        editResultMenuItem = new javax.swing.JMenuItem();
        resetAlertMenuItem = new javax.swing.JMenuItem();
        checkedMenuItem = new javax.swing.JCheckBoxMenuItem();
        swapPlayerMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        removeMenuItem = new javax.swing.JMenuItem();
        selectMatchPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        selectMatchTable = new javax.swing.JTable();
        editResultPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        editResultTable = new javax.swing.JTable();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager"); // NOI18N
        versionMenuItem.setText(bundle.getString("versionMenuItem")); // NOI18N
        versionMenuItem.setActionCommand("version");
        versionMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                versionMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(versionMenuItem);

        refreshMenuItem.setText(bundle.getString("refreshMenuItem")); // NOI18N
        refreshMenuItem.setActionCommand("refresh");
        refreshMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(refreshMenuItem);
        counterPopupMenu.add(jSeparator1);

        configMenuItem.setText(bundle.getString("configMenuItem")); // NOI18N
        configMenuItem.setActionCommand("config");
        configMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(configMenuItem);

        dataMenuItem.setText(bundle.getString("dataMenuItem")); // NOI18N
        dataMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(dataMenuItem);

        resultsMenuItem.setText(bundle.getString("resultsMenuItem")); // NOI18N
        resultsMenuItem.setActionCommand("results");
        resultsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resultsMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(resultsMenuItem);

        matchMenuItem.setText(bundle.getString("matchMenuItem")); // NOI18N
        matchMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                matchMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(matchMenuItem);

        addressMenuItem.setText(bundle.getString("addressMenuItem")); // NOI18N
        addressMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addressMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(addressMenuItem);

        vncMenuItem.setText(bundle.getString("vncMenuItem")); // NOI18N
        vncMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                vncMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(vncMenuItem);
        counterPopupMenu.add(jSeparator4);

        setMessageMenuItem.setText(bundle.getString("setMessage")); // NOI18N
        setMessageMenuItem.setEnabled(countermanager.liveticker.LivetickerAdmininstration.isLivetickerEnabled());
        setMessageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setMessageMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(setMessageMenuItem);

        enableLivetickerMenuItem.setSelected(true);
        enableLivetickerMenuItem.setText(bundle.getString("EnableLiveticker")); // NOI18N
        enableLivetickerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableLivetickerMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(enableLivetickerMenuItem);
        counterPopupMenu.add(jSeparator2);

        activateMenuItem.setSelected(CounterModel.getDefaultInstance().isCounterActive(counter));
        activateMenuItem.setText(bundle.getString("activateMenuItem")); // NOI18N
        activateMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                activateMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(activateMenuItem);

        resetMenuItem.setText(bundle.getString("resetMenuItem")); // NOI18N
        resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(resetMenuItem);

        lockscreenMenuItem.setText(bundle.getString("lockscreenMenuItem")); // NOI18N
        lockscreenMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockscreenMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(lockscreenMenuItem);

        selectMatchMenuItem.setText(bundle.getString("Select Match")); // NOI18N
        selectMatchMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectMatchMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(selectMatchMenuItem);

        editResultMenuItem.setText(bundle.getString("Edit Result")); // NOI18N
        editResultMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editResultMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(editResultMenuItem);

        resetAlertMenuItem.setText(bundle.getString("resetAlert")); // NOI18N
        resetAlertMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetAlertMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(resetAlertMenuItem);

        checkedMenuItem.setSelected(true);
        checkedMenuItem.setText(bundle.getString("checkedMenuItem")); // NOI18N
        checkedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkedMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(checkedMenuItem);

        swapPlayerMenuItem.setText(bundle.getString("swapPlayer")); // NOI18N
        swapPlayerMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                swapPlayerMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(swapPlayerMenuItem);
        counterPopupMenu.add(jSeparator3);

        removeMenuItem.setText(bundle.getString("removeMenuItem")); // NOI18N
        removeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeMenuItemActionPerformed(evt);
            }
        });
        counterPopupMenu.add(removeMenuItem);

        selectMatchTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Time", "Match", "MS", "Player A", "Player X"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        selectMatchTable.setFillsViewportHeight(true);
        selectMatchTable.setRowHeight(32);
        selectMatchTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        selectMatchTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(selectMatchTable);
        if (selectMatchTable.getColumnModel().getColumnCount() > 0) {
            selectMatchTable.getColumnModel().getColumn(0).setPreferredWidth(5);
            selectMatchTable.getColumnModel().getColumn(1).setPreferredWidth(5);
            selectMatchTable.getColumnModel().getColumn(2).setPreferredWidth(5);
        }

        javax.swing.GroupLayout selectMatchPanelLayout = new javax.swing.GroupLayout(selectMatchPanel);
        selectMatchPanel.setLayout(selectMatchPanelLayout);
        selectMatchPanelLayout.setHorizontalGroup(
            selectMatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectMatchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                .addContainerGap())
        );
        selectMatchPanelLayout.setVerticalGroup(
            selectMatchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectMatchPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 242, Short.MAX_VALUE)
                .addGap(14, 14, 14))
        );

        editResultTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null}
            },
            new String [] {
                "Game", "Player A", "Player X"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        editResultTable.setFillsViewportHeight(true);
        editResultTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane2.setViewportView(editResultTable);
        if (editResultTable.getColumnModel().getColumnCount() > 0) {
            editResultTable.getColumnModel().getColumn(0).setPreferredWidth(5);
        }

        javax.swing.GroupLayout editResultPanelLayout = new javax.swing.GroupLayout(editResultPanel);
        editResultPanel.setLayout(editResultPanelLayout);
        editResultPanelLayout.setHorizontalGroup(
            editResultPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, editResultPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                .addContainerGap())
        );
        editResultPanelLayout.setVerticalGroup(
            editResultPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editResultPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                .addContainerGap())
        );

        setBackground(new java.awt.Color(0, 0, 0));
        setForeground(new java.awt.Color(255, 255, 255));
        setComponentPopupMenu(counterPopupMenu);
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                formMouseDragged(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                formMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 569, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 267, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void resetAlertMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetAlertMenuItemActionPerformed
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CounterDriver.resetAlert(counter);
                } catch (java.io.IOException e) {
                    System.err.println(e.getLocalizedMessage());
                    e.printStackTrace();
                }
                repaint();            
            }            
        }).start();

    }//GEN-LAST:event_resetAlertMenuItemActionPerformed

    private void removeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeMenuItemActionPerformed
        CounterModel.getDefaultInstance().removeCounter(counter);
    }//GEN-LAST:event_removeMenuItemActionPerformed

    private void activateMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_activateMenuItemActionPerformed
        boolean active = activateMenuItem.isSelected();
        CounterModel.getDefaultInstance().setCounterActive(counter, active);
    }//GEN-LAST:event_activateMenuItemActionPerformed

    private void refreshMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshMenuItemActionPerformed
        new Thread(new Runnable() {
            @Override
            public void run() {
                java.awt.Cursor oldCursor = getCursor();
                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                CounterModel.getDefaultInstance().refreshConfiguration(counter);                

                setCursor(oldCursor);
            }
        }).start();
    }//GEN-LAST:event_refreshMenuItemActionPerformed

    java.awt.Point mousePt = null;
    
    private void formMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseDragged
        if (mousePt == null)
            return;
        
        setLocation(getX() + evt.getX() - mousePt.x, getY() + evt.getY() - mousePt.y);
    }//GEN-LAST:event_formMouseDragged

    private void formMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseReleased
        if (mousePt == null)
            return;
        
        mousePt = null;
        
        setLocation(
                Math.round(getX() / getWidth()) * getWidth() + 10, 
                Math.round(getY() / getHeight()) * getHeight() + 10 
        );
    }//GEN-LAST:event_formMouseReleased

    private void formMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMousePressed
        if (evt.getButton() == java.awt.event.MouseEvent.BUTTON1)
            mousePt = new java.awt.Point(evt.getX(), evt.getY());
    }//GEN-LAST:event_formMousePressed

    private void resultsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resultsMenuItemActionPerformed
        String result = getToolTipText();
        if (result == null)
            result = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("noResultsAvailableString");
        
        int ret = javax.swing.JOptionPane.showConfirmDialog(
                this, new javax.swing.JLabel(result), 
                java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("resultString"),
                javax.swing.JOptionPane.OK_CANCEL_OPTION);
        
        if (ret == javax.swing.JOptionPane.OK_OPTION)
            CounterModel.getDefaultInstance().setCounterChecked(counter, true);
    }//GEN-LAST:event_resultsMenuItemActionPerformed

    private void configMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configMenuItemActionPerformed
        final CounterConfig counterConfig = CounterModel.getDefaultInstance().getCounterConfig(counter);
        if (counterConfig != null) {
            // new PropertyFrame(counterConfig).setVisible(true);
            PropertyPanel panel = new PropertyPanel();
            panel.setObject(counterConfig, true, true);
            
            int ret = javax.swing.JOptionPane.showConfirmDialog(
                    this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("configurationString"), javax.swing.JOptionPane.OK_CANCEL_OPTION, 
                    javax.swing.JOptionPane.PLAIN_MESSAGE);

            if (ret == javax.swing.JOptionPane.OK_OPTION) {
                panel.updateObject(counterConfig);
                
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CounterModel.getDefaultInstance().updateCounterConfig(counter, counterConfig);                
                    }
                }).start();
            }
        }
    }//GEN-LAST:event_configMenuItemActionPerformed

    private void dataMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dataMenuItemActionPerformed
        CounterData counterData = CounterModel.getDefaultInstance().getCounterData(counter);
        if (counterData != null) {
            PropertyPanel panel = new PropertyPanel();
            panel.setObject(counterData, false, true);
            
            javax.swing.JOptionPane.showMessageDialog(
                    this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                            .getString("dataString"), javax.swing.JOptionPane.PLAIN_MESSAGE);
        }
    }//GEN-LAST:event_dataMenuItemActionPerformed
    
    private void resetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetMenuItemActionPerformed
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("resetCounterString");
            
                String title = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("resetCounterTitle");
            
                if (JOptionPane.showConfirmDialog(CounterPanelItem.this, msg, title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
                java.awt.Cursor oldCursor = getCursor();
                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                
                CounterModel.getDefaultInstance().reset(counter);                
                
                setCursor(oldCursor);
            }
        }).start();
    }//GEN-LAST:event_resetMenuItemActionPerformed

    private void versionMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_versionMenuItemActionPerformed
        String   version = CounterModel.getDefaultInstance().getVersion(counter);
        String   serialNumber = CounterModel.getDefaultInstance().getSerialNumber(counter);
        
        javax.swing.JOptionPane.showMessageDialog(
                this, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("counterVersionString") + ": " + version + ", S/N: " + serialNumber);
    }//GEN-LAST:event_versionMenuItemActionPerformed

    private void setMessageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setMessageMenuItemActionPerformed

    }//GEN-LAST:event_setMessageMenuItemActionPerformed

    private void enableLivetickerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableLivetickerMenuItemActionPerformed
        boolean active = enableLivetickerMenuItem.isSelected();
        CounterModel.getDefaultInstance().setLivetickerActive(counter, active);
    }//GEN-LAST:event_enableLivetickerMenuItemActionPerformed

    private void matchMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_matchMenuItemActionPerformed
        final countermanager.driver.IGameData match = CounterDriver.createGameData();

        PropertyPanel panel = new PropertyPanel();
        panel.setObject(match, true, true);

        int ret = javax.swing.JOptionPane.showConfirmDialog(
                this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                    .getString("configurationString"), javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);

        if (ret == javax.swing.JOptionPane.OK_OPTION) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CounterDriver.setGameData(counter, match);
                    } catch (java.io.IOException e) {
                        // Ignore
                    }
                }
            }).start();
        }
    }//GEN-LAST:event_matchMenuItemActionPerformed

    private void swapPlayerMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_swapPlayerMenuItemActionPerformed
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("swapPlayerString");
            
                String title = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("swapPlayerTitle");
            
                if (JOptionPane.showConfirmDialog(CounterPanelItem.this, msg, title, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    return;
                java.awt.Cursor oldCursor = getCursor();
                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                
                CounterModel.getDefaultInstance().swapPlayer(counter);                
                
                setCursor(oldCursor);
            }
        }).start();
    }//GEN-LAST:event_swapPlayerMenuItemActionPerformed

    private void checkedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkedMenuItemActionPerformed
        boolean active = checkedMenuItem.isSelected();
        CounterModel.getDefaultInstance().setCounterChecked(counter, active);
    }//GEN-LAST:event_checkedMenuItemActionPerformed

    private void selectMatchMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectMatchMenuItemActionPerformed

        int table = counter + CounterModel.getDefaultInstance().getTableOffset();
        final List<CounterModelMatch> matches = CounterModel.getDefaultInstance().getDatabase().update(table, table, java.time.LocalDate.now(), false);
        selectMatchTable.setModel(new javax.swing.table.AbstractTableModel() {
            
            private final String[] columnNames =  {
                "Time", "Event", "Match", "MS", "Player A", "Player X"
            };

            @Override
            public int getRowCount() {
                return matches.size();
            }

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }
            
            @Override 
            public String getColumnName(int columnIndex) {
                return columnNames[columnIndex];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm");
                CounterModelMatch match = matches.get(rowIndex);
                switch (columnIndex) {
                    case 0 : // Time
                        return tf.format(match.mtDateTime);
                        
                    case 1 : // Event
                        return match.cpName;
                        
                    case 2 : // mtNr
                        return match.mtNr;
                        
                    case 3 : // mtMS
                        return match.mtMS > 0 ? "" + match.mtMS : "";
                        
                    case 4 : // plA
                    {
                        String pl = "<html>";
                        if (match.plA != null && match.plA.psLast != null) 
                            pl += match.plA.plNr + " " + match.plA.psLast;
                        if (match.plB != null && match.plB.psLast != null)
                            pl += "<br>" + match.plB.plNr + " " + match.plB.psLast;
                        
                        pl += "</html>";

                        return pl;
                    }
                        
                    case 5 : // plX
                    {
                        String pl = "<html>";
                        if (match.plX != null && match.plX.psLast != null)
                            pl += match.plX.plNr + " " + match.plX.psLast;
                        if (match.plY != null && match.plY.psLast != null)
                            pl += "<br>" + match.plY.plNr + " " + match.plY.psLast;
                        
                        pl += "</html>";
                        
                        return pl;
                    }
                }
                
                return null;
            }
        });
        
        selectMatchTable.clearSelection();
        
        CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(counter);
        if (counterMatch != null) {
            for (int i = 0; i < matches.size(); i++) {
                if (counterMatch.mtNr == matches.get(i).mtNr && counterMatch.mtMS == matches.get(i).mtMS) {
                    selectMatchTable.getSelectionModel().setSelectionInterval(i, i);
                    break;
                }
            }
        }
        
        int oldIdx = selectMatchTable.getSelectedRow();
        
        String title = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("Select Match");
            
        int ret = javax.swing.JOptionPane.showConfirmDialog(
                CounterPanelItem.this, selectMatchPanel, title, javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (ret != javax.swing.JOptionPane.OK_OPTION)
            return;
        
        // Force match to be loaded
        int idx = selectMatchTable.getSelectedRow();
        if (idx < 0 || idx == oldIdx)
            return;
        
        CounterModel.getDefaultInstance().forceMatch(counter, matches.get(idx));
    }//GEN-LAST:event_selectMatchMenuItemActionPerformed

    private void editResultMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editResultMenuItemActionPerformed
        CounterModelMatch match = CounterModel.getDefaultInstance().getCounterMatch(counter);
        
        if (match == null)
            return;

        int[][] result = CounterModel.getDefaultInstance().getResults(counter);
        final String plA = match.cpType == 4 ? 
                match.tmA.tmDesc : match.plA.psLast + ", " + match.plA.psFirst;
        final String plX = match.cpType == 4 ? 
                match.tmX.tmDesc : match.plX.psLast + ", " + match.plX.psFirst;

        editResultTable.setModel(new javax.swing.table.AbstractTableModel() {
            
            private final String[] columnNames =  {
                "Game", plA, plX
            };

            @Override
            public int getRowCount() {
                return match.mtBestOf;
            }

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }
            
            @Override 
            public String getColumnName(int columnIndex) {
                return columnNames[columnIndex];
            }
            
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex > 0;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (rowIndex >= match.mtBestOf)
                    return null;
                
                switch (columnIndex) {
                    case 0 : // Time
                        return rowIndex + 1;
                        
                    case 1 : // Result Player A
                        return result != null && rowIndex < result.length ? result[rowIndex][0] : 0;
                        
                    case 2 : // Result Player X
                        return result != null && rowIndex < result.length ? result[rowIndex][1] : 0;                        
                }
                
                return null;
            }
            
            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                if (columnIndex == 0)
                    return;
                
                if (rowIndex >= result.length)
                    return;
                
                if (value == null)
                    return;
                
                result[rowIndex][columnIndex - 1] = Integer.parseInt(value.toString());
            }
        });
        
        String title = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("Edit Result");

        int ret = javax.swing.JOptionPane.showConfirmDialog(
                CounterPanelItem.this, editResultPanel, title, javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (ret != javax.swing.JOptionPane.OK_OPTION)
            return;

        CounterModel.getDefaultInstance().forceResult(counter, result);
    }//GEN-LAST:event_editResultMenuItemActionPerformed

    private void addressMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addressMenuItemActionPerformed
        String address = CounterDriver.getAddress(counter);
        if (address == null)
            address = "";
        
        javax.swing.JOptionPane.showMessageDialog(CounterPanelItem.this, "IP: " + address);
    }//GEN-LAST:event_addressMenuItemActionPerformed

    private void vncMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_vncMenuItemActionPerformed
        final String address = CounterDriver.getAddress(counter);
        if (address == null)
            return;
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                VNCViewer vncViewer = new VNCViewer();
                vncViewer.setVisible(true);
                vncViewer.connect(address, 5900);
            }            
        }).start();        
    }//GEN-LAST:event_vncMenuItemActionPerformed

    private void lockscreenMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockscreenMenuItemActionPerformed
        if (lockscreenMenuItem.isSelected())
            CounterDriver.lockScreen(counter);
        else
            CounterDriver.unlockScreen(counter);
    }//GEN-LAST:event_lockscreenMenuItemActionPerformed

    private boolean isCounterMatchForced() {
        return CounterModel.getDefaultInstance().isCounterMatchForced(counter);
    }
    
    
    private boolean isScreenLocked() {
        return CounterModel.getDefaultInstance().isScreenLocked(counter);
    }
    
    private String getResultString(boolean reverse) {        
        int[][] result = CounterModel.getDefaultInstance().getResults(counter);
        
        if (result != null) {
            String tmp = new String();
            
            for (int i = 0; i < result.length; i++) {
                if (result[i][0] == 0 && result[i][1] == 0)
                    break;

                if (i > 0)
                    tmp += ";";

                tmp += result[i][reverse ? 1 : 0] + ":" + result[i][reverse ? 0 : 1];
            }
            
            return tmp;
        } else {
            return null;
        }        
    }
    
    
    private String getToolTipString() {
        CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(counter);
        CounterData counterData = CounterModel.getDefaultInstance().getCounterData(counter);     
        
        // Alarmtext hat Prio
        if (counterData != null && counterData.getAlertText() != null && !counterData.getAlertText().isEmpty())
            return counterData.getAlertText();
                
        if (counterMatch == null) {
            return null;
        }
        
        boolean isSingle = (counterMatch.plB.plNr == 0 && counterMatch.plY.plNr == 0);

        boolean reverse = counterData == null ? false : counterData.isSwapped();
        
        countermanager.model.database.Player plA = reverse ? counterMatch.plX : counterMatch.plA;
        countermanager.model.database.Player plB = reverse ? counterMatch.plY : counterMatch.plB;
        countermanager.model.database.Player plX = reverse ? counterMatch.plA : counterMatch.plX;
        countermanager.model.database.Player plY = reverse ? counterMatch.plB : counterMatch.plY;
            
        // Results are as seen from umpire, so the parameter is not necessary
        String result = getResultString(false);

        if (result == null) 
            result = "";

        result = result.replaceAll(";", "&nbsp;&nbsp;");

        String tt;

        tt = 
        "<html>" +
           "<table border=\"0\">" +      
                  "<tr>";
        
        // Player(s) A,B
        if (plA.plNr == 0) {
            if (counterMatch.cpType == 4)
                tt +=  "<td colspan=\"3\">" + (counterMatch.tmA == null ? "" : counterMatch.tmA.tmDesc) + "</td>";
            else
                tt +=  "<td></td><td></td><td></td>";
        } else if (isSingle) {
            tt +=      "<td>" + plA.plNr + "</td>" +
                       "<td>" + plA.psLast + ",&nbsp;" + plA.psFirst + "</td>" +
                       "<td>" + plA.naName + "</td>";
        } else {
            tt +=      "<td>" + plA.plNr + "<br>" + plB.plNr + "</td>" +
                       "<td>" + plA.psLast + ",&nbsp;" + plA.psFirst + "<br>" +
                                plB.psLast + ",&nbsp;" + plB.psFirst + "</td>" +
                       "<td>" + plA.naName + "<br>" + plB.naName + "</td>";
        }
        
        // Seperator
        tt +=          "<td>" + " - " + "</td>";
        
        // Player(s) X, Y
        if (plX.plNr == 0) {
            if (counterMatch.cpType == 4)
                tt +=  "<td colspan=\"3\">" + (counterMatch.tmX == null ? "" : counterMatch.tmX.tmDesc) + "</td>";
            else
                tt +=  "<td></td><td></td><td></td>";
        } else if (isSingle) {
            tt +=      "<td>" + plX.plNr + "</td>" +
                       "<td>" + plX.psLast + ",&nbsp;" + plX.psFirst + "</td>" +
                       "<td>" + plX.naName + "</td>";
        } else {
            tt +=      "<td>" + plX.plNr + "<br>" + plY.plNr + "</td>" +
                       "<td>" + plX.psLast + ",&nbsp;" + plX.psFirst + "<br>" +
                                plY.psLast + ",&nbsp;" + plY.psFirst + "</td>" +
                       "<td>" + plX.naName + "<br>" + plY.naName + "</td>";
        }
        
        // Result
        tt +=          "<td>" + result + "</td>";
        
        boolean swap = counterData != null && 
                       (counterData.getPlayerNrLeft() == 0xFFFE || 
                       counterData.getPlayerNrLeft() == counterMatch.plX.plNr);

        // List games        
        if (counterData == null)
            tt +=      "<td></td>";
        else if (result == null || result.length() == 0)
            tt +=      "<td></td>";
        else if (reverse ^ swap)
            tt +=      "<td><b>" + counterData.getSetsRight() + "&nbsp;:&nbsp;" + counterData.getSetsLeft() + "</b></td>";
        else
            tt +=      "<td><b>" + counterData.getSetsLeft() + "&nbsp;:&nbsp;" + counterData.getSetsRight() + "</b></td>";
        
        tt += 
                   "</tr>" +
               "</table>" +
            "</html>";
                
        
        return tt;
    }
    
    public void counterChanged() {        
        CounterData counterData = CounterModel.getDefaultInstance().getCounterData(counter);  
        CounterModelMatch counterMatch = CounterModel.getDefaultInstance().getCounterMatch(counter);
                
        boolean active = CounterModel.getDefaultInstance().isCounterActive(counter);
        boolean checked = CounterModel.getDefaultInstance().isCounterChecked(counter);
        boolean locked = CounterModel.getDefaultInstance().isScreenLocked(counter);

        refreshMenuItem.setEnabled(active);
        // resetMenuItem.setEnabled(active);
        selectMatchMenuItem.setEnabled(active);
        editResultMenuItem.setEnabled(active);
        
        lockscreenMenuItem.setSelected(locked);

        if (counterData != null) {
        /*
            resetMenuItem.setEnabled(
                    active && 
                    counterData.getGameMode() != CounterData.GameMode.WARMUP && 
                    counterData.getGameMode() != CounterData.GameMode.RUNNING);
         */   
         
            resetAlertMenuItem.setEnabled(counterData.getAlert());

            versionMenuItem.setEnabled(active);
            configMenuItem.setEnabled(active);
            dataMenuItem.setEnabled(active);
            resultsMenuItem.setEnabled(active);
            matchMenuItem.setEnabled(active);
            addressMenuItem.setEnabled(active);
            vncMenuItem.setEnabled(active);
            swapPlayerMenuItem.setEnabled(active);
            checkedMenuItem.setEnabled(
                    active && 
                    counterData.getGameMode() != CounterData.GameMode.RESET);
            lockscreenMenuItem.setEnabled(
                    counterData.getGameMode() == CounterData.GameMode.RESET);
        } else {
            resetAlertMenuItem.setEnabled(false);

            versionMenuItem.setEnabled(false);
            configMenuItem.setEnabled(false);
            dataMenuItem.setEnabled(false);
            resultsMenuItem.setEnabled(false);
            matchMenuItem.setEnabled(false);
            addressMenuItem.setEnabled(false);
            vncMenuItem.setEnabled(false);
            swapPlayerMenuItem.setEnabled(false);
            checkedMenuItem.setEnabled(false);
            editResultMenuItem.setEnabled(false);
            lockscreenMenuItem.setEnabled(false);
        }
        
        activateMenuItem.setSelected(CounterModel.getDefaultInstance().isCounterActive(counter));
        checkedMenuItem.setSelected(CounterModel.getDefaultInstance().isCounterChecked(counter));
        
        setToolTipText(getToolTipString());
        
        if (!CounterModel.getDefaultInstance().isCounterActive(counter)) {
            setForeground(java.awt.Color.BLACK);
            setBackground(java.awt.Color.LIGHT_GRAY);
        } else if (counterData == null) {
            // Communication problem? counterData shall never be null.
            setBackground(java.awt.Color.BLACK);
            setForeground(java.awt.Color.WHITE);
        } else if (counterData.getAlert()) {
            setBackground(java.awt.Color.RED);
            setForeground(java.awt.Color.WHITE);
        } else {            
            switch (counterData.getGameMode()) {
                case RESET :                                        
                    if (counterMatch == null) {
                        // RESET without a match is OK
                        setBackground(java.awt.Color.WHITE);
                        setForeground(java.awt.Color.BLACK);
                    } else if (2 * counterData.getSetsLeft() > counterMatch.mtBestOf ||
                               2 * counterData.getSetsRight() > counterMatch.mtBestOf) {
                        // RESET but match is finished (not yet cleared)
                        setBackground(java.awt.Color.WHITE);
                        setForeground(java.awt.Color.BLACK);
                    } else {
                        // The individual matches following the first one are always on time
                        // May be better to make them green, too, so we know team matches are
                        // still running.
                        setBackground(java.awt.Color.YELLOW);
                        setForeground(java.awt.Color.BLACK);
                    }
                    break;
                    
                case END :
                    
                    // But remember, when. We consider this time for the state of 
                    // the next match being overdue
                    lastMatchFinished = System.currentTimeMillis();
                    
                    // Fall through
                case WARMUP :                
                case RUNNING :
                    if (checked)
                        setBackground(java.awt.Color.GREEN);
                    else
                        setBackground(java.awt.Color.RED);
                    
                    setForeground(java.awt.Color.BLACK);
                    break;                
            }
        }
        
        repaint();
        
        if (counterData == null)
            hasAlert = false;
        else if (hasAlert != counterData.getAlert()) {
            hasAlert = counterData.getAlert();
            if (hasAlert) {
                // Don't trigger again if last alert was only recently
                // I.e. have some kind of flutter suppression, if e.g. 2 inputs
                // are counting the same table and one has an alert
                if (lastAlertTime + ALERT_SUPPRESSION_TIME < System.currentTimeMillis()) {
                    sendAlertSMS(counterData.getAlertText());
                    showAlertText(counterData.getAlertText());
                }
                
                lastAlertTime = System.currentTimeMillis();
            }
        }
    }
    
    // -------------------------------------------------------------------
    private void showAlertText(String msg) {
        final String text = (msg == null || msg.isEmpty() ? "<Alert>" : msg);
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String msg = "<html><b>" + DateFormat.getTimeInstance().format(new java.util.Date()) + "</b><br>" + text+"</html>";
                javax.swing.JOptionPane.showMessageDialog(
                        null, msg, "CounterManager Alert T." + (counter + CounterModel.getDefaultInstance().getTableOffset()), 
                        javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }

        });
    }
    
    private void sendAlertSMS(String msg) {
        String text = 
            "CounterManager Alert from T. " + (counter + CounterModel.getDefaultInstance().getTableOffset()) + ":\n" + 
            (msg == null || msg.isEmpty() ? "<Alert>" : msg)
        ;
        
        CounterModel.getDefaultInstance().sendSMS(text);
    }
    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBoxMenuItem activateMenuItem;
    private javax.swing.JMenuItem addressMenuItem;
    private javax.swing.JCheckBoxMenuItem checkedMenuItem;
    private javax.swing.JMenuItem configMenuItem;
    private javax.swing.JPopupMenu counterPopupMenu;
    private javax.swing.JMenuItem dataMenuItem;
    private javax.swing.JMenuItem editResultMenuItem;
    private javax.swing.JPanel editResultPanel;
    private javax.swing.JTable editResultTable;
    private javax.swing.JCheckBoxMenuItem enableLivetickerMenuItem;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JCheckBoxMenuItem lockscreenMenuItem;
    private javax.swing.JMenuItem matchMenuItem;
    private javax.swing.JMenuItem refreshMenuItem;
    private javax.swing.JMenuItem removeMenuItem;
    private javax.swing.JMenuItem resetAlertMenuItem;
    private javax.swing.JMenuItem resetMenuItem;
    private javax.swing.JMenuItem resultsMenuItem;
    private javax.swing.JCheckBoxMenuItem selectMatchMenuItem;
    private javax.swing.JPanel selectMatchPanel;
    private javax.swing.JTable selectMatchTable;
    private javax.swing.JMenuItem setMessageMenuItem;
    private javax.swing.JMenuItem swapPlayerMenuItem;
    private javax.swing.JMenuItem versionMenuItem;
    private javax.swing.JMenuItem vncMenuItem;
    // End of variables declaration//GEN-END:variables
    
}
