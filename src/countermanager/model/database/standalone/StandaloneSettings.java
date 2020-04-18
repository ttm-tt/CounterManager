/* Copyright (C) 2020 Christoph Theis */

package countermanager.model.database.standalone;

import countermanager.model.database.IDatabaseSettings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author chtheis
 */
public class StandaloneSettings extends javax.swing.JPanel implements IDatabaseSettings {

    Standalone db;
    
    /**
     * Creates new form XmlSettings
     */
    public StandaloneSettings(Standalone database) {
        this.db = database;
        
        initComponents();
        
        jPlayersTable.setDefaultEditor(Nation.class, new DefaultCellEditor(new JComboBox<>(db.database.nations)));
        jMatchTable.setDefaultEditor(Competition.class, new DefaultCellEditor(new JComboBox<>(db.database.competitions)));
        jMatchTable.setDefaultEditor(Player.class, new DefaultCellEditor(new JComboBox<>(db.database.players)));
    }
    
    class NationTableModel extends AbstractTableModel {

        private final String[] columnNames = {"Name", "Description", "Delete"};
        private final Class[] columnClasses = {String.class, String.class, Boolean.class};
        
        @Override
        public int getRowCount() {
            return db.database.nations.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == getRowCount() - 1)
                return null;
            
            switch (columnIndex) {
                case 0 :
                    return db.database.nations.get(rowIndex).naName;
                    
                case 1 :
                    return db.database.nations.get(rowIndex).naDesc;
                    
                case 2 :
                    return Boolean.FALSE;
            }
            
            return null;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {            
            if (rowIndex == getRowCount() - 1) {
                db.database.nations.add(new Nation());
                
                fireTableRowsInserted(rowIndex + 1, rowIndex + 1);
            }
            
            if (columnIndex == columnNames.length - 1) {
                db.database.nations.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            } else {
                switch (columnIndex) {
                    case 0 :
                        db.database.nations.get(rowIndex).naName = aValue.toString();
                        break;
                        
                    case 1 :
                        db.database.nations.get(rowIndex).naDesc = aValue.toString();
                        break;
                }
            }
            
        }        
    }

    class EventsTableModel extends AbstractTableModel {

        private final String[] columnNames = {"Name", "Description", "Best Of", "Delete"};
        private final Class[] columnClasses = {String.class, String.class, Integer.class, Boolean.class};
        
        @Override
        public int getRowCount() {
            return db.database.competitions.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == getRowCount() - 1)
                return null;
            
            switch (columnIndex) {
                case 0 :
                    return db.database.competitions.get(rowIndex).cpName;
                    
                case 1 :
                    return db.database.competitions.get(rowIndex).cpDesc;
                    
                case 2 :
                    return db.database.competitions.get(rowIndex).mtBestOf;
                    
                case 3 :
                    return Boolean.FALSE;
            }
            
            return null;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {            
            if (rowIndex == getRowCount() - 1) {
                db.database.competitions.add(new Competition());
                
                fireTableRowsInserted(rowIndex + 1, rowIndex + 1);
            }
            
            if (columnIndex == columnNames.length - 1) {
                db.database.competitions.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            } else {
                switch (columnIndex) {
                    case 0 :
                        db.database.competitions.get(rowIndex).cpName = aValue.toString();
                        break;
                        
                    case 1 :
                        db.database.competitions.get(rowIndex).cpDesc = aValue.toString();
                        break;
                        
                    case 2 :
                        db.database.competitions.get(rowIndex).mtBestOf = ((Integer) aValue);
                        break;
                }
            }
            
        }        
    }
    
    class PlayersTableModel extends AbstractTableModel {

        private final String[] columnNames = {"Start No.", "Given Name", "Family Name", "Nation", "Delete"};
        private final Class[] columnClasses = {Integer.class, String.class, String.class, Nation.class, Boolean.class};
        
        @Override
        public int getRowCount() {
            return db.database.players.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == getRowCount() - 1)
                return null;
            
            switch (columnIndex) {
                case 0 :
                    return db.database.players.get(rowIndex).plNr;
                    
                case 1 :
                    return db.database.players.get(rowIndex).psFirst;
                    
                case 2 :
                    return db.database.players.get(rowIndex).psLast;
                    
                case 3 :
                    return db.database.players.get(rowIndex).nation;
                    
                case 4 :
                    return Boolean.FALSE;
            }
            
            return null;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {            
            if (rowIndex == getRowCount() - 1) {
                db.database.players.add(new Player());
                
                fireTableRowsInserted(rowIndex + 1, rowIndex + 1);
            }
            
            if (columnIndex == columnNames.length - 1) {
                db.database.players.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            } else {
                switch (columnIndex) {
                    case 0 :
                        db.database.players.get(rowIndex).plNr = (Integer) aValue;
                        break;
                        
                    case 1 :
                        db.database.players.get(rowIndex).psFirst = aValue.toString();
                        break;
                        
                    case 2 :
                        db.database.players.get(rowIndex).psLast = aValue.toString();
                        break;
                        
                    case 3 :
                        db.database.players.get(rowIndex).nation = (Nation) aValue;
                        break;
                }
            }
            
        }        
    }
    
    class MatchesTableModel extends AbstractTableModel {

        private final String[] columnNames = {"Competition", "Round", "Player A", "Player X", "Date", "Time", "Table", "Delete"};
        private final Class[] columnClasses = {Competition.class, Integer.class, Player.class, Player.class, String.class, String.class, Integer.class, Boolean.class};
        
        @Override
        public int getRowCount() {
            return db.database.matches.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        String formatDate(long mtDateTime) {
            if (mtDateTime == 0)
                return "";
            
            return new SimpleDateFormat("yyyy-MM-dd").format(mtDateTime);
        }
        
        String formatTime(long mtDateTime) {
            if (mtDateTime == 0)
                return "";
            
            return new SimpleDateFormat("HH:mm").format(mtDateTime);
        }
        
        long parseDateTime(String date, String time) {
            if (date.isEmpty() && time.isEmpty())
                return 0;
            
            Date ct = new Date();
            if (date.isEmpty())
                date = new SimpleDateFormat("yyyy-MM-dd").format(ct);
            
            if (time.isEmpty())
                time = "00:00";
            
            switch (date.split("-").length) {
                case 1 :
                    date = "" + ct.getYear() + "-" + (ct.getMonth() + 1) + "-" + date;
                    break;
                    
                case 2 :
                    date = "" + ct.getYear() + "-" + date;
                    break;
            }
            
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date + " " + time).getTime();
            } catch (ParseException ex) {
                Logger.getLogger(StandaloneSettings.class.getName()).log(Level.SEVERE, null, ex);
                return 0;
            }
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == getRowCount() - 1)
                return null;
            
            Match mt = db.database.matches.get(rowIndex);
            
            switch (columnIndex) {
                case 0 :
                    return mt.cp;
                    
                case 1 :
                    return mt.mtRound;
                    
                case 2 :
                    return mt.plA;
                    
                case 3 :
                    return mt.plX;
                    
                case 4 :
                    return formatDate(mt.mtDateTime);
                    
                case 5 :
                    return formatTime(mt.mtDateTime);
                    
                case 6 :
                    return mt.mtTable;
                    
                case 7 :
                    return Boolean.FALSE;
            }
            
            return null;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {            
            if (rowIndex == getRowCount() - 1) {
                Match mt = new Match();
                mt.mtNr = ++db.database.lastMtNr;
                db.database.matches.add(mt);
                
                if (rowIndex > 0) {
                    Match prevMt = db.database.matches.get(rowIndex - 1);
                    
                    mt.mtDateTime = prevMt.mtDateTime;
                    mt.mtTable = prevMt.mtTable + 1;
                    mt.mtRound = prevMt.mtRound;
                }
                                
                fireTableRowsInserted(rowIndex + 1, rowIndex + 1);
            }
            
            if (columnIndex == columnNames.length - 1) {
                db.database.matches.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            } else {
                Match mt = db.database.matches.get(rowIndex);
                
                switch (columnIndex) {
                    case 0 :
                        mt.cp = (Competition) aValue;
                        break;
                        
                    case 1 :
                        mt.mtRound = (Integer) aValue;
                        break;
                        
                    case 2 :
                        mt.plA = (Player) aValue;
                        break;
                        
                    case 3 :
                        mt.plX = (Player) aValue;
                        break;
                        
                    case 4 :
                        mt.mtDateTime = parseDateTime(aValue.toString(), formatTime(mt.mtDateTime));
                        break;
                    
                    case 5 :
                        mt.mtDateTime = parseDateTime(formatDate(mt.mtDateTime), aValue.toString());
                        break;
                    
                    case 6 :
                        mt.mtTable = (Integer) aValue;
                        break;
                }
            }
            
        }        
    }
    
    @Override
    public void store() {
        db.store();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        Associations = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jNationTable = new javax.swing.JTable();
        Players = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jPlayersTable = new javax.swing.JTable();
        Competitions = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jCompetitionTable = new javax.swing.JTable();
        Matches = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jMatchTable = new javax.swing.JTable();

        jNationTable.setModel(new NationTableModel());
        jNationTable.setCellSelectionEnabled(true);
        jNationTable.setFillsViewportHeight(true);
        jScrollPane2.setViewportView(jNationTable);

        javax.swing.GroupLayout AssociationsLayout = new javax.swing.GroupLayout(Associations);
        Associations.setLayout(AssociationsLayout);
        AssociationsLayout.setHorizontalGroup(
            AssociationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AssociationsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 694, Short.MAX_VALUE)
                .addContainerGap())
        );
        AssociationsLayout.setVerticalGroup(
            AssociationsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(AssociationsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Nations", Associations);

        jPlayersTable.setModel(new PlayersTableModel()
        );
        jPlayersTable.setFillsViewportHeight(true);
        jScrollPane3.setViewportView(jPlayersTable);

        javax.swing.GroupLayout PlayersLayout = new javax.swing.GroupLayout(Players);
        Players.setLayout(PlayersLayout);
        PlayersLayout.setHorizontalGroup(
            PlayersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PlayersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
                .addContainerGap())
        );
        PlayersLayout.setVerticalGroup(
            PlayersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PlayersLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 352, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Players", Players);

        jCompetitionTable.setModel(new EventsTableModel());
        jCompetitionTable.setFillsViewportHeight(true);
        jScrollPane4.setViewportView(jCompetitionTable);

        javax.swing.GroupLayout CompetitionsLayout = new javax.swing.GroupLayout(Competitions);
        Competitions.setLayout(CompetitionsLayout);
        CompetitionsLayout.setHorizontalGroup(
            CompetitionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CompetitionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 929, Short.MAX_VALUE)
                .addContainerGap())
        );
        CompetitionsLayout.setVerticalGroup(
            CompetitionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CompetitionsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Events", Competitions);

        jMatchTable.setModel(new MatchesTableModel());
        jMatchTable.setFillsViewportHeight(true);
        jScrollPane1.setViewportView(jMatchTable);

        javax.swing.GroupLayout MatchesLayout = new javax.swing.GroupLayout(Matches);
        Matches.setLayout(MatchesLayout);
        MatchesLayout.setHorizontalGroup(
            MatchesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MatchesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 837, Short.MAX_VALUE)
                .addContainerGap())
        );
        MatchesLayout.setVerticalGroup(
            MatchesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MatchesLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Matches", Matches);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 875, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Associations;
    private javax.swing.JPanel Competitions;
    private javax.swing.JPanel Matches;
    private javax.swing.JPanel Players;
    private javax.swing.JTable jCompetitionTable;
    private javax.swing.JTable jMatchTable;
    private javax.swing.JTable jNationTable;
    private javax.swing.JTable jPlayersTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables

}
