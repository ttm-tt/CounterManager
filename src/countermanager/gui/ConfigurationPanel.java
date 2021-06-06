/* Copyright (C) 2020 Christoph Theis */

/*
 * Configuration.java
 *
 * Cre    @Override
    public boolean verify(JComponent input) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
ated on 12. Januar 2007, 12:51
 */

package countermanager.gui;

import countermanager.driver.ICounterProperties;
import countermanager.driver.ICounterDriver;
import countermanager.liveticker.LivetickerAdmininstration;
import countermanager.model.CounterModel;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import java.io.File;
import java.io.IOException;

import static countermanager.prefs.Prefs.*;
import javax.swing.JOptionPane;

/**
 *
 * @author  Christoph Theis
 */
public class ConfigurationPanel extends javax.swing.JPanel {
    
    static class LivetickerTableModel extends javax.swing.table.AbstractTableModel {
        public LivetickerTableModel() {
        }
        
        @Override
        public int getRowCount() {
            return LivetickerAdmininstration.getLiveticker().size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0 :
                    return LivetickerAdmininstration.isLivetickerEnabled(rowIndex);
                    
                case 1 :
                    return LivetickerAdmininstration.getLiveticker(rowIndex).getName();
                    
                case 2 :
                    return "...";
                    
                case 3 :
                    return "x";
                    
                default :
                    return null;
            }
        }
        
        @Override 
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0 :
                    LivetickerAdmininstration.setLivetickerEnabled(rowIndex, ((Boolean) value));
                    break;
                    
                case 1 :
                    LivetickerAdmininstration.getLiveticker(rowIndex).setName(value.toString());
                    break;
            }
        }
        
        @Override
        public Class getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0 :
                    return Boolean.class;
                    
                case 1 :
                    return String.class;
                    
                case 2 :
                    return String.class;
                    
                case 3 :
                    return String.class;
                    
                default :
                    return Object.class;
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {            
            switch (columnIndex) {
                case 0 :
                    return true;
                    
                case 1 :
                    return true;
                    
                default :
                    return false;
            }
        }
    }
    
    /** Creates new form Configuration */
    public ConfigurationPanel() {
        initComponents();
        
        livetickerTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        livetickerTable.getColumnModel().getColumn(0).setMinWidth(45);
        livetickerTable.getColumnModel().getColumn(0).setMaxWidth(45);
        livetickerTable.getColumnModel().getColumn(0).setResizable(false);
        
        livetickerTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        livetickerTable.getColumnModel().getColumn(2).setMinWidth(60);
        livetickerTable.getColumnModel().getColumn(2).setMaxWidth(60);
        livetickerTable.getColumnModel().getColumn(2).setResizable(false);
        
        livetickerTable.getColumnModel().getColumn(3).setPreferredWidth(45);
        livetickerTable.getColumnModel().getColumn(3).setMinWidth(45);
        livetickerTable.getColumnModel().getColumn(3).setMaxWidth(45);        
        livetickerTable.getColumnModel().getColumn(3).setResizable(false);
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager");
        
        jTabbedPane1.add(bundle.getString("Display"), configDisplayPanel);
        jTabbedPane1.add(bundle.getString("Input"), configCounterPanel);
        jTabbedPane1.add(bundle.getString("Database"), configDatabasePanel);
        jTabbedPane1.add(bundle.getString("HTTP"), configHTTPPanel);
        jTabbedPane1.add(bundle.getString("Liveticker"), configLiveticker);
        jTabbedPane1.add(bundle.getString("SMS"), configSmsPanel);
    }
    
    public void readProperties(countermanager.prefs.Properties prefs) {
        int row;

        firstCounterSpinner.setValue(prefs.getInt(FIRST_COUNTER_PREF, 0));
        offsetTableSpinner.setValue(prefs.getInt(OFFSET_TABLE_PREF, 1) - 1);
        
        resetTimeoutTextField.setText( (new Integer(prefs.getInt(RESET_TIMEOUT_PREF, 30))).toString() );

        fromTableSpinner.setValue(prefs.getInt(FROM_TABLE_PREF, 1));
        toTableSpinner.setValue(prefs.getInt(TO_TABLE_PREF, CounterModel.MAX_COUNTERS));
        
        httpPortTextField.setText("" + prefs.getInt(HTTP_PORT_PREF, 80));
        
        String scripts = prefs.getString(SCRIPT_PREF, "");
        row = 0;
        for (String script : scripts.split(File.pathSeparator)) {
            if (script.isEmpty())
                continue;
            
            String[] tmp = script.split("=");
            if (tmp.length != 2)
                continue;
            
            ((javax.swing.table.DefaultTableModel) scriptsTable.getModel()).addRow(new String[] {tmp[0], tmp[1], "..."});
            
            ++row;                        
        }
        
        ((javax.swing.table.DefaultTableModel) scriptsTable.getModel()).addRow(new String[] {"", "", "..."});

        String addDirs = prefs.getString(ADD_DIRS_PREF, "");
        row = 0;
        for (String dir : addDirs.split(File.pathSeparator)) {
            if (dir.isEmpty())
                continue;
            
            String[] tmp = dir.split("=");
            if (tmp.length != 2)
                continue;
            
            ((javax.swing.table.DefaultTableModel) addDirsTable.getModel()).addRow(new String[] {tmp[0], tmp[1], "..."});
            
            ++row;            
        }
        
        ((javax.swing.table.DefaultTableModel) addDirsTable.getModel()).addRow(new String[] {"", "", "..."});

        try {
            String className = prefs.getString(CT_CLASS_PREF, countermanager.driver.ttm.CounterDriverTTM.class.getName());

            Class clazz = getClass().getClassLoader().loadClass(className);
            if (clazz != null)
                inputTypeComboBox.setSelectedItem(clazz);
        } catch (ClassNotFoundException e) {
            
        }

        try {
            String className = prefs.getString(DATABASE_CLASS_PREF, countermanager.model.database.ttm.TTM.class.getName());

            Class clazz = getClass().getClassLoader().loadClass(className);
            if (clazz != null)
                databaseTypeComboBox.setSelectedItem(clazz);
        } catch (ClassNotFoundException e) {
            
        }

        smsPhoneTextField.setText(prefs.getString(SMS_PHONE, ""));
        smsUserTextField.setText(prefs.getString(SMS_USER, ""));
        smsPasswordTextField.setText(prefs.getString(SMS_PWD, ""));        
    }

    
    
    public void writeProperties(countermanager.prefs.Properties prefs) {
        prefs.putInt(FIRST_COUNTER_PREF, ((Number) firstCounterSpinner.getValue()).intValue());
        prefs.putInt(OFFSET_TABLE_PREF, ((Number) offsetTableSpinner.getValue()).intValue() + 1);
        
        prefs.putInt(RESET_TIMEOUT_PREF, Integer.parseInt(resetTimeoutTextField.getText()));
        
        CounterModel.setResetTimeout(prefs.getInt(RESET_TIMEOUT_PREF, 30) * 1000);
        
        prefs.putInt(FROM_TABLE_PREF, ((Number) fromTableSpinner.getValue()).intValue());
        prefs.putInt(TO_TABLE_PREF, ((Number) toTableSpinner.getValue()).intValue());        
        
        prefs.putInt(HTTP_PORT_PREF, Integer.parseInt(httpPortTextField.getText()));
        
        String scripts = "";
        for (int row = 0; row < scriptsTable.getRowCount(); row++) {
            if (scriptsTable.getValueAt(row, 0) == null)
                break;
            
            if (scriptsTable.getValueAt(row, 1) == null)
                break;
            
            String alias = scriptsTable.getValueAt(row, 0).toString();
            String localPath = scriptsTable.getValueAt(row, 1).toString();
            if (alias.isEmpty() || localPath.isEmpty())
                break;
            
            scripts += alias + "=" + localPath + File.pathSeparator;
        }
        prefs.putString(SCRIPT_PREF, scripts);
        
        prefs.putString(SMS_PHONE, smsPhoneTextField.getText());
        prefs.putString(SMS_USER, smsUserTextField.getText());
        prefs.putString(SMS_PWD, new String(smsPasswordTextField.getPassword()));
        
        String addDirs = "";
        for (int row = 0; row < addDirsTable.getRowCount(); row++) {
            if (addDirsTable.getValueAt(row, 0) == null)
                break;
            
            if (addDirsTable.getValueAt(row, 1) == null)
                break;
            
            String alias = addDirsTable.getValueAt(row, 0).toString();
            String localPath = addDirsTable.getValueAt(row, 1).toString();
            if (alias.isEmpty() || localPath.isEmpty())
                break;
            
            addDirs += alias + "=" + localPath + File.pathSeparator;
        }
        
        prefs.putString(ADD_DIRS_PREF, addDirs);

        if (inputTypeComboBox.getSelectedItem() != null)
            prefs.put(CT_CLASS_PREF, ((Class) inputTypeComboBox.getSelectedItem()).getName());
        else
            prefs.remove(CT_CLASS_PREF);
        
        if (databaseTypeComboBox.getSelectedItem() != null)
            prefs.put(DATABASE_CLASS_PREF, ((Class) databaseTypeComboBox.getSelectedItem()).getName());
        else
            prefs.remove(DATABASE_CLASS_PREF);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        configCounterPanel = new javax.swing.JPanel();
        firstCounterLabel = new javax.swing.JLabel();
        firstCounterSpinner = new javax.swing.JSpinner();
        firstCounterSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, CounterModel.MAX_COUNTERS, 1));
        offsetTableLabel = new javax.swing.JLabel();
        offsetTableSpinner = new javax.swing.JSpinner();
        offsetTableSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, CounterModel.MAX_COUNTERS, 1));
        jLabel3 = new javax.swing.JLabel();
        inputTypeComboBox = new javax.swing.JComboBox();
        configureInputButton = new javax.swing.JButton();
        resetTimeoutLabel = new javax.swing.JLabel();
        resetTimeoutTextField = new javax.swing.JTextField();
        configDatabasePanel = new javax.swing.JPanel();
        databaseTypeComboBox = new javax.swing.JComboBox();
        configureDatabaseButton = new javax.swing.JButton();
        configDisplayPanel = new javax.swing.JPanel();
        fromTableLabel = new javax.swing.JLabel();
        fromTableSpinner = new javax.swing.JSpinner();
        toTableLabel = new javax.swing.JLabel();
        toTableSpinner = new javax.swing.JSpinner();
        configHTTPPanel = new javax.swing.JPanel();
        httpPortLabel = new javax.swing.JLabel();
        httpPortTextField = new javax.swing.JFormattedTextField();
        httpScriptLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        addDirsTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        scriptsTable = new javax.swing.JTable();
        configLiveticker = new javax.swing.JPanel();
        livetickerTable = new javax.swing.JTable();
        addLivetickerButton = new javax.swing.JButton();
        configSmsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        smsUserTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        smsPasswordTextField = new javax.swing.JPasswordField();
        smsPhoneTextField = new javax.swing.JTextField();
        jTabbedPane1 = new javax.swing.JTabbedPane();

        configCounterPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager"); // NOI18N
        firstCounterLabel.setText(bundle.getString("firstCounterLabel")); // NOI18N

        firstCounterSpinner.setMinimumSize(new java.awt.Dimension(26, 20));
        firstCounterSpinner.setPreferredSize(new java.awt.Dimension(26, 20));

        offsetTableLabel.setText(bundle.getString("offsetTableLabel")); // NOI18N

        offsetTableSpinner.setMinimumSize(new java.awt.Dimension(26, 20));
        offsetTableSpinner.setPreferredSize(new java.awt.Dimension(26, 20));

        jLabel3.setText(bundle.getString("Input:")); // NOI18N

        inputTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(
            new Class[] {
                // countermanager.driver.gts.CounterDriverGTS.class,
                countermanager.driver.ttm.CounterDriverTTM.class
            }
        )

    );
    inputTypeComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
        public java.awt.Component getListCellRendererComponent(
            javax.swing.JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(
                list, value == null ? "none" : ((Class) value).getSimpleName(), index, isSelected, cellHasFocus);
        }
    });

    configureInputButton.setText(bundle.getString("Configure")); // NOI18N
    configureInputButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            configureInputButtonActionPerformed(evt);
        }
    });

    resetTimeoutLabel.setText(bundle.getString("resetTimeoutLabel")); // NOI18N

    resetTimeoutTextField.setText("30");

    javax.swing.GroupLayout configCounterPanelLayout = new javax.swing.GroupLayout(configCounterPanel);
    configCounterPanel.setLayout(configCounterPanelLayout);
    configCounterPanelLayout.setHorizontalGroup(
        configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configCounterPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel3)
                .addComponent(firstCounterLabel)
                .addComponent(offsetTableLabel))
            .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addGroup(configCounterPanelLayout.createSequentialGroup()
                    .addGap(10, 10, 10)
                    .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(configCounterPanelLayout.createSequentialGroup()
                            .addComponent(firstCounterSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(18, 18, 18)
                            .addComponent(resetTimeoutLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(resetTimeoutTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(inputTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGap(18, 18, 18)
                    .addComponent(configureInputButton, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(configCounterPanelLayout.createSequentialGroup()
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(offsetTableSpinner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(233, 233, 233)))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    configCounterPanelLayout.setVerticalGroup(
        configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configCounterPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(configCounterPanelLayout.createSequentialGroup()
                    .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(inputTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(configureInputButton))
                    .addGap(18, 18, 18)
                    .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(firstCounterLabel)
                        .addComponent(firstCounterSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resetTimeoutLabel)
                    .addComponent(resetTimeoutTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGap(18, 18, 18)
            .addGroup(configCounterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(offsetTableLabel)
                .addComponent(offsetTableSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    configDatabasePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

    databaseTypeComboBox.setModel(new javax.swing.DefaultComboBoxModel(
        new Class[] {
            countermanager.model.database.ttm.TTM.class,
            countermanager.model.database.league.League.class,
            countermanager.model.database.standalone.Standalone.class,
            countermanager.model.database.simulation.Simulation.class
        }
    )

    );
    databaseTypeComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
        public java.awt.Component getListCellRendererComponent(
            javax.swing.JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(
                list, value == null ? "none" : ((Class) value).getSimpleName(), index, isSelected, cellHasFocus);
        }
    });

    configureDatabaseButton.setText(bundle.getString("Configure")); // NOI18N
    configureDatabaseButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            configureDatabaseButtonActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout configDatabasePanelLayout = new javax.swing.GroupLayout(configDatabasePanel);
    configDatabasePanel.setLayout(configDatabasePanelLayout);
    configDatabasePanelLayout.setHorizontalGroup(
        configDatabasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configDatabasePanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(databaseTypeComboBox, 0, 249, Short.MAX_VALUE)
            .addGap(18, 18, 18)
            .addComponent(configureDatabaseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );
    configDatabasePanelLayout.setVerticalGroup(
        configDatabasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configDatabasePanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configDatabasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(databaseTypeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(configureDatabaseButton))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    configDisplayPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

    fromTableLabel.setText(bundle.getString("fromTableLabel")); // NOI18N

    fromTableSpinner.setMinimumSize(new java.awt.Dimension(26, 20));
    fromTableSpinner.setPreferredSize(new java.awt.Dimension(26, 20));

    toTableLabel.setText(bundle.getString("toTableLabel")); // NOI18N

    toTableSpinner.setMinimumSize(new java.awt.Dimension(26, 20));
    toTableSpinner.setPreferredSize(new java.awt.Dimension(26, 20));

    javax.swing.GroupLayout configDisplayPanelLayout = new javax.swing.GroupLayout(configDisplayPanel);
    configDisplayPanel.setLayout(configDisplayPanelLayout);
    configDisplayPanelLayout.setHorizontalGroup(
        configDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configDisplayPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(fromTableLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(fromTableSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(32, 32, 32)
            .addComponent(toTableLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(toTableSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(11, Short.MAX_VALUE))
    );
    configDisplayPanelLayout.setVerticalGroup(
        configDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configDisplayPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(fromTableLabel)
                .addComponent(fromTableSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(toTableLabel)
                .addComponent(toTableSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );

    configHTTPPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

    httpPortLabel.setText(bundle.getString("Port:")); // NOI18N

    httpPortTextField.setText("80");

    httpScriptLabel.setText(bundle.getString("Scripts:")); // NOI18N

    jLabel4.setText(bundle.getString("Add. Dirs:")); // NOI18N

    addDirsTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Alias", "Path", "..."
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.Object.class
        };
        boolean[] canEdit = new boolean [] {
            true, true, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    addDirsTable.setFillsViewportHeight(true);
    addDirsTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            addDirsTableMouseClicked(evt);
        }
    });
    jScrollPane1.setViewportView(addDirsTable);
    if (addDirsTable.getColumnModel().getColumnCount() > 0) {
        addDirsTable.getColumnModel().getColumn(2).setResizable(false);
        addDirsTable.getColumnModel().getColumn(2).setPreferredWidth(3);
    }

    scriptsTable.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Alias", "Script", "..."
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            true, true, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    scriptsTable.setFillsViewportHeight(true);
    scriptsTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            scriptsTableMouseClicked(evt);
        }
    });
    jScrollPane2.setViewportView(scriptsTable);
    if (scriptsTable.getColumnModel().getColumnCount() > 0) {
        scriptsTable.getColumnModel().getColumn(2).setResizable(false);
        scriptsTable.getColumnModel().getColumn(2).setPreferredWidth(3);
    }

    javax.swing.GroupLayout configHTTPPanelLayout = new javax.swing.GroupLayout(configHTTPPanel);
    configHTTPPanel.setLayout(configHTTPPanelLayout);
    configHTTPPanelLayout.setHorizontalGroup(
        configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configHTTPPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(configHTTPPanelLayout.createSequentialGroup()
                    .addGroup(configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(httpScriptLabel)
                        .addComponent(httpPortLabel))
                    .addGap(15, 15, 15)
                    .addGroup(configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(configHTTPPanelLayout.createSequentialGroup()
                            .addComponent(httpPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(265, 265, 265))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, configHTTPPanelLayout.createSequentialGroup()
                            .addGap(2, 2, 2)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addContainerGap())))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, configHTTPPanelLayout.createSequentialGroup()
                    .addComponent(jLabel4)
                    .addGap(6, 6, 6)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addContainerGap())))
    );
    configHTTPPanelLayout.setVerticalGroup(
        configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configHTTPPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(httpPortLabel)
                .addComponent(httpPortTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(15, 15, 15)
            .addGroup(configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(httpScriptLabel)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE))
            .addGap(18, 18, 18)
            .addGroup(configHTTPPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE)
                .addComponent(jLabel4))
            .addContainerGap())
    );

    configLiveticker.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

    livetickerTable.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
    livetickerTable.setModel(new LivetickerTableModel());
    livetickerTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
    livetickerTable.setDefaultRenderer(Class.class, new javax.swing.table.DefaultTableCellRenderer() {
        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            if (value instanceof Class)
            value = ((Class) value).getSimpleName();

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

        }
    });
    livetickerTable.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            livetickerTableMouseClicked(evt);
        }
    });

    addLivetickerButton.setText(bundle.getString("Add Liveticker")); // NOI18N
    addLivetickerButton.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            addLivetickerButtonActionPerformed(evt);
        }
    });

    javax.swing.GroupLayout configLivetickerLayout = new javax.swing.GroupLayout(configLiveticker);
    configLiveticker.setLayout(configLivetickerLayout);
    configLivetickerLayout.setHorizontalGroup(
        configLivetickerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configLivetickerLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configLivetickerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(livetickerTable, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                .addGroup(configLivetickerLayout.createSequentialGroup()
                    .addComponent(addLivetickerButton)
                    .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
    );
    configLivetickerLayout.setVerticalGroup(
        configLivetickerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configLivetickerLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(livetickerTable, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(addLivetickerButton)
            .addContainerGap())
    );

    jLabel1.setText(bundle.getString("Phone:")); // NOI18N

    jLabel2.setText(bundle.getString("User:")); // NOI18N

    smsUserTextField.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            smsUserTextFieldActionPerformed(evt);
        }
    });

    jLabel5.setText(bundle.getString("Password:")); // NOI18N

    smsPhoneTextField.setText("+");

    javax.swing.GroupLayout configSmsPanelLayout = new javax.swing.GroupLayout(configSmsPanel);
    configSmsPanel.setLayout(configSmsPanelLayout);
    configSmsPanelLayout.setHorizontalGroup(
        configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configSmsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jLabel5)
                .addComponent(jLabel1)
                .addComponent(jLabel2))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                .addComponent(smsUserTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
                .addComponent(smsPasswordTextField)
                .addComponent(smsPhoneTextField))
            .addContainerGap(163, Short.MAX_VALUE))
    );
    configSmsPanelLayout.setVerticalGroup(
        configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(configSmsPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel1)
                .addComponent(smsPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(14, 14, 14)
            .addGroup(configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel2)
                .addComponent(smsUserTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(9, 9, 9)
            .addGroup(configSmsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel5)
                .addComponent(smsPasswordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap())
    );

    setBorder(javax.swing.BorderFactory.createTitledBorder(""));
    setMinimumSize(new java.awt.Dimension(26, 20));

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
    this.setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 584, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
    );
    }// </editor-fold>//GEN-END:initComponents

    private void configureInputButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureInputButtonActionPerformed
       
        Class clazz = (Class) inputTypeComboBox.getSelectedItem();         
        if (clazz == null) {             
            return;
        }         
    
        try {             
            ICounterDriver input = (ICounterDriver) clazz.newInstance();
            ICounterProperties props = input.getCounterProperties();              
            
            PropertyPanel panel = new PropertyPanel();             
            panel.setObject(props, true, true);              
            int ret = javax.swing.JOptionPane.showConfirmDialog(                     
                    this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("configurationString"), 
                    javax.swing.JOptionPane.OK_CANCEL_OPTION,                     
                    javax.swing.JOptionPane.PLAIN_MESSAGE);              
            
            if (ret == javax.swing.JOptionPane.OK_OPTION) {                 
                panel.updateObject(props);
                
                // Save the properties                 
                input.setCounterProperties(props);             
            }         
        } catch (InstantiationException i) {  
            
        } catch (IllegalAccessException i) {         
            
        }             
    }//GEN-LAST:event_configureInputButtonActionPerformed

    private void configureDatabaseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureDatabaseButtonActionPerformed
        Class clazz = (Class) databaseTypeComboBox.getSelectedItem();         
        if (clazz == null) {             
            return;
        }         
    
        try {             
            IDatabase database = CounterModel.getDefaultInstance().getDatabase();
            if ( database == null || !clazz.equals(database.getClass()) )
                database = (IDatabase) clazz.newInstance();
            IDatabaseSettings panel = database.getSettingsPanel();
            int ret = javax.swing.JOptionPane.showConfirmDialog(                     
                    this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager").getString("configurationString"), 
                    javax.swing.JOptionPane.OK_CANCEL_OPTION,                     
                    javax.swing.JOptionPane.PLAIN_MESSAGE);              
            
            if (ret == javax.swing.JOptionPane.OK_OPTION) {                 
                panel.store();
            }         
        } catch (InstantiationException i) {  
            
        } catch (IllegalAccessException i) {         
            
        }     
    }//GEN-LAST:event_configureDatabaseButtonActionPerformed

    private void livetickerTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_livetickerTableMouseClicked
        int rowIndex = livetickerTable.rowAtPoint(evt.getPoint());

        switch (livetickerTable.columnAtPoint(evt.getPoint())) {
            case 2 :
                countermanager.liveticker.Liveticker lt = LivetickerAdmininstration.getLiveticker(rowIndex);
                if (lt == null)
                    return;

                PropertyPanel panel = new PropertyPanel();
                panel.setObject(lt, true, false);

                int ret = javax.swing.JOptionPane.showConfirmDialog(
                        this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                            .getString("configurationString"), javax.swing.JOptionPane.OK_CANCEL_OPTION,
                        javax.swing.JOptionPane.PLAIN_MESSAGE);

                if (ret == javax.swing.JOptionPane.OK_OPTION) {
                    panel.updateObject(lt);
                }       
                
                ((LivetickerTableModel) livetickerTable.getModel()).fireTableRowsUpdated(rowIndex, rowIndex);
            
                break;
                
            case 3 :
                LivetickerAdmininstration.removeLiveticker(rowIndex);
                ((LivetickerTableModel) livetickerTable.getModel()).fireTableRowsDeleted(rowIndex, rowIndex);
                break;
        }    
    }//GEN-LAST:event_livetickerTableMouseClicked

    private void addDirsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_addDirsTableMouseClicked
        switch (addDirsTable.columnAtPoint(evt.getPoint())) {
            case 2 :
                int row = addDirsTable.rowAtPoint(evt.getPoint());

                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                fc.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
                fc.setCurrentDirectory(new File(System.getProperty("user.dir")));

                if (addDirsTable.getValueAt(row, 1) != null) {
                    String dir = addDirsTable.getValueAt(row, 1).toString();

                    if (!dir.isEmpty())
                        fc.setSelectedFile(new File(dir));
                }

                int ret = fc.showOpenDialog(this);
                if (ret == javax.swing.JFileChooser.APPROVE_OPTION) {
                    try {
                        addDirsTable.setValueAt(fc.getSelectedFile().getCanonicalPath(), row, 1);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }    

                    if (row == addDirsTable.getRowCount() - 1) {
                        ((javax.swing.table.DefaultTableModel) addDirsTable.getModel()).addRow(new String[] {"", "", "..."});
                    }
                }
            
                break;
        }
    }//GEN-LAST:event_addDirsTableMouseClicked

    private void addLivetickerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addLivetickerButtonActionPerformed
        Object ret = javax.swing.JOptionPane.showInputDialog(
                this, null, null, JOptionPane.PLAIN_MESSAGE, null, new String[] {"TTM", "Unas", "Scripting"}, "TTM"
        );
        
        if (ret == null)
            return;
        
        switch (ret.toString()) {
            case "TTM" :
                LivetickerAdmininstration.addLiveticker(new countermanager.liveticker.ttm.TTM());
                break;
                
            case "Unas" :
                LivetickerAdmininstration.addLiveticker(new countermanager.liveticker.unas.Unas());
                break;
                
            case "Scripting" :
                LivetickerAdmininstration.addLiveticker(new countermanager.liveticker.scripting.Scripting());
                break;
                
            default :
                return;
        }
        
        ((LivetickerTableModel) livetickerTable.getModel()).fireTableRowsInserted(livetickerTable.getRowCount() - 1, livetickerTable.getRowCount() - 1);
    }//GEN-LAST:event_addLivetickerButtonActionPerformed

    private void smsUserTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_smsUserTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_smsUserTextFieldActionPerformed

    private void scriptsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scriptsTableMouseClicked
        switch (scriptsTable.columnAtPoint(evt.getPoint())) {
            case 2 :
                int row = scriptsTable.rowAtPoint(evt.getPoint());

                javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
                fc.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);

                if (scriptsTable.getValueAt(row, 1) != null) {
                    String script = scriptsTable.getValueAt(row, 1).toString();

                    if (!script.isEmpty())
                        fc.setSelectedFile(new File(script));
                }

                int ret = fc.showOpenDialog(this);
                if (ret == javax.swing.JFileChooser.APPROVE_OPTION) {
                    try {
                        scriptsTable.setValueAt(fc.getSelectedFile().getCanonicalPath(), row, 1);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }    

                    if (row == scriptsTable.getRowCount() - 1) {
                        ((javax.swing.table.DefaultTableModel) scriptsTable.getModel()).addRow(new String[] {"", "..."});
                    }
                }
            
                break;
        }    
    }//GEN-LAST:event_scriptsTableMouseClicked
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable addDirsTable;
    private javax.swing.JButton addLivetickerButton;
    private javax.swing.JPanel configCounterPanel;
    private javax.swing.JPanel configDatabasePanel;
    private javax.swing.JPanel configDisplayPanel;
    private javax.swing.JPanel configHTTPPanel;
    private javax.swing.JPanel configLiveticker;
    private javax.swing.JPanel configSmsPanel;
    private javax.swing.JButton configureDatabaseButton;
    private javax.swing.JButton configureInputButton;
    private javax.swing.JComboBox databaseTypeComboBox;
    private javax.swing.JLabel firstCounterLabel;
    private javax.swing.JSpinner firstCounterSpinner;
    private javax.swing.JLabel fromTableLabel;
    private javax.swing.JSpinner fromTableSpinner;
    private javax.swing.JLabel httpPortLabel;
    private javax.swing.JFormattedTextField httpPortTextField;
    private javax.swing.JLabel httpScriptLabel;
    private javax.swing.JComboBox inputTypeComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable livetickerTable;
    private javax.swing.JLabel offsetTableLabel;
    private javax.swing.JSpinner offsetTableSpinner;
    private javax.swing.JLabel resetTimeoutLabel;
    private javax.swing.JTextField resetTimeoutTextField;
    private javax.swing.JTable scriptsTable;
    private javax.swing.JPasswordField smsPasswordTextField;
    private javax.swing.JTextField smsPhoneTextField;
    private javax.swing.JTextField smsUserTextField;
    private javax.swing.JLabel toTableLabel;
    private javax.swing.JSpinner toTableSpinner;
    // End of variables declaration//GEN-END:variables
    
}
