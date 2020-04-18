/* Copyright (C) 2020 Christoph Theis */

package countermanager.model.database.league;

import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import countermanager.model.database.league.MatchDetails.MatchType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.event.TableModelListener;

public class LeagueSettings extends javax.swing.JPanel implements IDatabaseSettings {
    
    private List<MatchDetails> list;
    
    @Override
    public void store() {
        String dateTimeString = dateTextField.getText() + ", " + timeTextField.getText();
        java.util.Date dateTime = null;
        java.text.DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        try {
            dateTime = dateFormat.parse(dateTimeString);
        } catch (ParseException ex) {
            String msg = ex.getLocalizedMessage() + " at: " + ex.getErrorOffset();
            if (dateFormat instanceof SimpleDateFormat)
                msg = msg + " with format \"" + ((SimpleDateFormat) dateFormat).toPattern() + "\"";
            Logger.getLogger(LeagueSettings.class.getName()).log(Level.SEVERE, msg, ex);
            dateTime = new java.util.Date(System.currentTimeMillis());
        }
        
        for (MatchDetails detail : list) {
            detail.mtDateTime = dateTime.getTime();
            detail.tmAtmName = homeTmName.getText();
            detail.tmAtmDesc = homeTmDesc.getText();
            detail.tmAnaName = homeNaName.getText();
            detail.tmXtmName = guestTmName.getText();
            detail.tmXtmDesc = guestTmDesc.getText();
            detail.tmXnaName = guestNaName.getText();
            detail.mtBestOf = Integer.parseInt(bestOfSpinner.getValue().toString());
        }
        
        // Clear all matches if checked so
        if (startNewMatchCheckBox.isSelected()) {
            ((League) database).setMatchList(null);
        }
        
        ((League) database).setMatchList(list);
    }
    
    private class MatchTableModel extends javax.swing.table.AbstractTableModel {

        @Override
        public int getRowCount() {
            int rows = 0;
            for (MatchDetails m : list) {
                if (m.type.equals(MatchType.Double))
                    rows += 2;
                else
                    rows += 1;
            }
            
            return rows;
        }

        @Override
        public int getColumnCount() {
            return 6;
        }
                
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0 :
                    return MatchType.class;
                case 1 : 
                case 2 :
                case 3 :
                case 4 :
                    return String.class;
                case 5 :
                    return Integer.class;
                    
                default :
                    return Object.class;
            }
        }
        
        @Override
        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0 :
                    return "Type";
                case 1 :
                    return "Given Name (Home)";
                case 2 :
                    return "Family Name (Home)";
                case 3 :
                    return "Given Name (Guest)";
                case 4 :
                    return "Family Name (Guest)";
                case 5 :
                    return "Table";
                default :
                    return null;
                    
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex != 0 && columnIndex != 5)
                return true;
            
            int rows = 0, idx = 0;
            for (; idx < list.size(); idx++) {
                if (rows == rowIndex)
                    return true;
                
                if (list.get(idx).type.equals(MatchType.Double))
                    rows += 2;
                else
                    rows += 1;
                
                if (rows > rowIndex)
                    return false;
            }
            
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            int rows = 0, idx = 0;
            boolean first = true;
            
            for (; idx < list.size(); idx++) {
                if (rows == rowIndex) {
                    first = true;
                    break;
                }
                
                if (list.get(idx).type.equals(MatchType.Double))
                    rows += 2;
                else
                    rows += 1;
                
                if (rows > rowIndex) {
                    first = false;
                    break;
                }
            }
            
            if (idx >= list.size())
                return null;
            
            MatchDetails m = list.get(idx);
            
            switch (columnIndex) {
                case 0 :
                    return first ? m.type : null;
                case 1 :
                    return first ? m.plApsFirst : m.plBpsFirst;
                    
                case 2 :
                    return first ? m.plApsLast : m.plBpsLast;
                    
                case 3 :
                    return first ? m.plXpsFirst : m.plYpsFirst;
                    
                case 4 :
                    return first ? m.plXpsLast : m.plYpsLast;
                    
                case 5 :
                    return first && m.mtTable > 0 ? m.mtTable : null;
            }
            
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            int rows = 0, idx = 0;
            boolean first = true;
            
            for (; idx < list.size(); idx++) {
                if (rows == rowIndex) {
                    first = true;
                    break;
                }
                
                if (list.get(idx).type.equals(MatchType.Double))
                    rows += 2;
                else
                    rows += 1;
                
                if (rows > rowIndex) {
                    first = false;
                    break;
                }
            }
            
            if (idx >= list.size())
                return;
            
            MatchDetails m = list.get(idx);
            
            switch (columnIndex) {
                case 0 :
                    if(first) {
                        if (!m.type.equals(aValue)) {                        
                            m.type = (MatchType) aValue;
                            
                            if (aValue.equals(MatchType.Double)) {
                                // Add row
                                fireTableRowsInserted(rowIndex + 1, rowIndex + 1);
                            } else {
                                // Remove row
                                fireTableRowsDeleted(rowIndex + 1, rowIndex + 1);
                            }
                        }
                    }
                    break;
                    
                case 1 :
                    if (first) 
                        m.plApsFirst = (String) aValue;
                    else 
                        m.plBpsFirst = (String) aValue;
                    break;
                    
                case 2 :
                    if (first) 
                        m.plApsLast = (String) aValue;
                    else 
                        m.plBpsLast = (String) aValue;
                    break;
                    
                case 3 :
                    if (first) 
                        m.plXpsFirst = (String) aValue;
                    else 
                        m.plYpsFirst = (String) aValue;
                    break;
                    
                case 4 :
                    if (first) 
                        m.plXpsLast = (String) aValue;
                    else 
                        m.plYpsLast = (String) aValue;
                    break;
                    
                case 5 :
                    if(first) 
                        m.mtTable = (Integer) aValue;
                    break;
                    
            }
            
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
            super.addTableModelListener(l);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
            super.removeTableModelListener(l);
        }        
    }
    
    /**
     * Creates new form StandaloneSettings
     */
    public LeagueSettings(IDatabase database) {
        this.database = database;
        list = ((League) database).getMatchList();
        initComponents();
        nofMatchesSpinner.setValue(list.size());
        nofMatchesSpinnerStateChanged(null);
        
        if (list.size() > 0) {
            homeTmName.setText(list.get(0).tmAtmName);
            homeTmDesc.setText(list.get(0).tmAtmDesc);
            homeNaName.setText(list.get(0).tmAnaName);
            guestTmName.setText(list.get(0).tmXtmName);
            guestTmDesc.setText(list.get(0).tmXtmDesc);
            guestNaName.setText(list.get(0).tmXnaName);
            
            if (list.get(0).mtDateTime != 0) {
                dateTextField.setText(SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(new java.util.Date(list.get(0).mtDateTime)));
                timeTextField.setText(SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(new java.util.Date(list.get(0).mtDateTime)));
            }
            
            bestOfSpinner.setValue(list.get(0).mtBestOf);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane3 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        dateTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        timeTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        homeTmName = new javax.swing.JTextField();
        homeTmDesc = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        guestTmName = new javax.swing.JTextField();
        guestTmDesc = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        matchTable = new javax.swing.JTable();
        jLabel7 = new javax.swing.JLabel();
        nofMatchesSpinner = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        bestOfSpinner = new javax.swing.JSpinner();
        startNewMatchCheckBox = new javax.swing.JCheckBox();
        homeNaName = new javax.swing.JTextField();
        guestNaName = new javax.swing.JTextField();

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jTable3);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager"); // NOI18N
        jLabel1.setText(bundle.getString("dateLabel")); // NOI18N

        jLabel2.setText(bundle.getString("timeLabel")); // NOI18N

        jLabel3.setText(bundle.getString("homeLabel")); // NOI18N

        homeTmName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeTmNameActionPerformed(evt);
            }
        });

        jLabel4.setText(bundle.getString("guestLabel")); // NOI18N

        matchTable.setModel(new MatchTableModel()
        );
        matchTable.setRowHeight((int) (getFont().getSize() * 1.5));
        matchTable.getTableHeader().setReorderingAllowed(false);
        matchTable.getColumnModel().getColumn(0).setCellEditor(
            new DefaultCellEditor(
                new javax.swing.JComboBox(MatchType.values())
            )
        );
        jScrollPane2.setViewportView(matchTable);

        jLabel7.setText(bundle.getString("matchesLabel")); // NOI18N

        nofMatchesSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                nofMatchesSpinnerStateChanged(evt);
            }
        });

        jLabel5.setText(bundle.getString("bestofLabel")); // NOI18N

        bestOfSpinner.setModel(new javax.swing.SpinnerNumberModel(5, 3, 9, 2));

        startNewMatchCheckBox.setText(bundle.getString("Start new match")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel3))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nofMatchesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(dateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(homeTmName, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(homeTmDesc, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(homeNaName, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bestOfSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(timeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(guestTmName, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(guestTmDesc, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(guestNaName, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 740, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(startNewMatchCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addGap(19, 19, 19))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {guestTmName, homeTmName});

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {guestTmDesc, homeTmDesc});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(dateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(timeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(nofMatchesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(bestOfSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(homeTmName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(homeTmDesc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(guestTmName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(guestTmDesc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(homeNaName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(guestNaName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(startNewMatchCheckBox)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void nofMatchesSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_nofMatchesSpinnerStateChanged
        int newCount = Integer.parseInt(nofMatchesSpinner.getValue().toString());
        int oldRowCount = matchTable.getModel().getRowCount();
        
        while (list.size() < newCount)
            list.add(new MatchDetails());
        while (list.size() > newCount)
            list.remove(list.size() - 1);
        
        int newRowCount = matchTable.getModel().getRowCount();
        
        if (oldRowCount < newRowCount)
            ((MatchTableModel) matchTable.getModel()).fireTableRowsInserted(oldRowCount, newRowCount - 1);
        if (oldRowCount > newRowCount)
            ((MatchTableModel) matchTable.getModel()).fireTableRowsDeleted(oldRowCount - 1, newRowCount);
    }//GEN-LAST:event_nofMatchesSpinnerStateChanged

    private void homeTmNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeTmNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_homeTmNameActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner bestOfSpinner;
    private javax.swing.JTextField dateTextField;
    private javax.swing.JTextField guestNaName;
    private javax.swing.JTextField guestTmDesc;
    private javax.swing.JTextField guestTmName;
    private javax.swing.JTextField homeNaName;
    private javax.swing.JTextField homeTmDesc;
    private javax.swing.JTextField homeTmName;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable3;
    private javax.swing.JTable matchTable;
    private javax.swing.JSpinner nofMatchesSpinner;
    private javax.swing.JCheckBox startNewMatchCheckBox;
    private javax.swing.JTextField timeTextField;
    // End of variables declaration//GEN-END:variables

    private IDatabase database;
}
