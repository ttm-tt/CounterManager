/* Copyright (C) 2020 Christoph Theis */

/*
 * PropertyPanel.java
 *
 * Created on 3. Januar 2007, 21:04
 */

package countermanager.gui;

import java.awt.Component;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author  Administrator
 */
public class PropertyPanel extends javax.swing.JPanel {
    
    // fields which contain the phrase "Password" (case sensitive) are rendered uneradable
    private class PasswordTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                              boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value.toString().isEmpty())
                ((DefaultTableCellRenderer) c).setText("");
            else
                ((DefaultTableCellRenderer) c).setText(String.format("%-" + value.toString().length() + "s", "").replace(' ', '*'));
            return c;
        }
    }
    
    // Render a Map as "..."
    private class MapTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                              boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ((DefaultTableCellRenderer) c).setText("...");
            return c;
        }
    }
    
    /** Creates new form PropertyPanel */
    public PropertyPanel() {
        initComponents();
    }
    
    public void setObject(Object obj, final boolean editable, final boolean inherit) {
        final Object[] columnNames = new String[] {
            java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                .getString("propertyNameLabel"),
            java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                .getString("propertyValueLabel")
        };
        
        final List<Object[]> rows = new java.util.ArrayList<>();
        
        fillRows(rows, obj, obj.getClass(), inherit);
        
        table.setModel(
            new javax.swing.table.AbstractTableModel() {
                @Override
                public String getColumnName(int column) { return columnNames[column].toString(); }

                @Override
                public int getRowCount() { return rows.size(); }
                
                @Override
                public int getColumnCount() { return columnNames.length; }

                @Override
                public Object getValueAt(int row, int col) { return rows.get(row)[col]; }
                
                @Override
                public boolean isCellEditable(int row, int column) {
                    return editable && column > 0;
                }
                
                @Override
                public void setValueAt(Object value, int row, int col) {
                    if (rows.get(row)[col] instanceof Integer)
                        rows.get(row)[col] = Integer.valueOf(value.toString());
                    else if (rows.get(row)[col] instanceof Long)
                        rows.get(row)[col] = Long.valueOf(value.toString());
                    else if (rows.get(row)[col] instanceof Boolean)
                        rows.get(row)[col] = Boolean.valueOf(value.toString());
                    else
                        rows.get(row)[col] = value;
                    
                    fireTableCellUpdated(row, col);
                }
        });
    }
    
    
    private void fillRows(List<Object[]> rows, Object obj, Class clazz, boolean inherit) {
        
        if (obj == null)
            return;
        
        if (obj instanceof Map) {
            fillRowsFromMap(rows, (Map) obj);
            return;
        }
        
        if (clazz.getName().equals("java.lang.Object"))
            return;
        
        if (inherit && clazz.getSuperclass() != null)
            fillRows(rows, obj, clazz.getSuperclass(), inherit);
        
        java.lang.reflect.Field fields[] = clazz.getDeclaredFields();
        
        for (int idx = 0; idx < fields.length; idx++) {
            Object[] row = new Object[2];
                        
            char[] c = fields[idx].getName().toCharArray();
            c[0] = Character.toUpperCase(c[0]);
            
            String name = new String(c);

            java.lang.reflect.Method method = null;

            // First try: getXxx pattern
            try {
                method = obj.getClass().getMethod("get" + name);
            } catch (NoSuchMethodException e) {
                // Boolean methods might have a isXxx pattern
                try {
                    method = obj.getClass().getMethod("is" + name);
                } catch (NoSuchMethodException e2) {
                    // Keine Property
                    Logger.getLogger(getClass().getName()).log(Level.FINE, "Not a property: " + name);
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            try {
                if (method != null) {
                    row[0] = name;
                    row[1] = method.invoke(obj);
                    
                    if (row[1].getClass().isArray()) {
                        row[1] = json.toJson(row[1]);
                    }
                    
                    rows.add(row);
                }
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
            }
        }
    }
    
    
    private void fillRowsFromMap(List<Object[]> rows, Map map) {
        for (Iterator it = map.keySet().iterator(); it.hasNext();) {
            Object key = it.next();
            Object[] row = new Object[2];
            row[0] = key.toString();
            row[1] = map.get(key);
            rows.add(row);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        table = new javax.swing.JTable() {
            public javax.swing.table.TableCellRenderer getCellRenderer(int row, int col) {
                Object value = getModel().getValueAt(row, col);
                if (value == null)
                return super.getCellRenderer(row, col);
                else if (value instanceof Boolean)
                return getDefaultRenderer(Boolean.class);
                else if (value instanceof java.util.Map)
                return new MapTableCellRenderer();
                else if (col == 1 && getValueAt(row, 0).toString().indexOf("Password") >= 0)
                return new PasswordTableCellRenderer();
                else
                return super.getCellRenderer(row, col);
            }

            public javax.swing.table.TableCellEditor getCellEditor(int row, int col) {
                Object value = getModel().getValueAt(row, col);
                if (value == null)
                return super.getCellEditor(row, col);
                else if (value instanceof Boolean)
                return getDefaultEditor(Boolean.class);
                else if (value instanceof Enum) {
                    try {
                        Object[] values = (Object[]) (value.getClass().getMethod("values").invoke(null));
                        return new javax.swing.DefaultCellEditor(new javax.swing.JComboBox(values));
                    } catch (Exception e) {
                        return super.getCellEditor(row, col);
                    }
                }
                else if (col == 1 && getValueAt(row, 0).toString().indexOf("Password") >= 0)
                return new javax.swing.DefaultCellEditor(new javax.swing.JPasswordField(value.toString()));
                else
                return super.getCellEditor(row, col);
            }
        };

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
        });
        scrollPane.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 355, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
        int row = table.rowAtPoint(evt.getPoint());
        Object value = table.getModel().getValueAt(row, 1);

        if (value instanceof Map) {
            PropertyPanel panel = new PropertyPanel();
            panel.setObject(value, true, false);

            int ret = javax.swing.JOptionPane.showConfirmDialog(
                    this, panel, java.util.ResourceBundle.getBundle("countermanager/gui/resources/CounterManager")
                        .getString("configurationString"), javax.swing.JOptionPane.OK_CANCEL_OPTION,
                    javax.swing.JOptionPane.PLAIN_MESSAGE);

            if (ret == javax.swing.JOptionPane.OK_OPTION) {
                panel.updateObject(value);
                table.getModel().setValueAt(value, row, 1);
            }       
        }
    }//GEN-LAST:event_tableMouseClicked

    void updateObject(Object obj) {
        if (obj == null)
            return;
        
        for (int row = 0; row < table.getModel().getRowCount(); row++) {
            String name = table.getModel().getValueAt(row, 0).toString();
            Object value = table.getModel().getValueAt(row, 1);

            if (value == null)
                continue;
            
            if (obj instanceof Map) {
                ((Map) obj).put(name, value);
                continue;
            }
            
            try {
                Class clazz = value.getClass();
                if (clazz == Boolean.class)
                    clazz = Boolean.TYPE;
                else if (clazz == Byte.class)
                    clazz = Byte.TYPE;
                else if (clazz == Character.class)
                    clazz = Character.TYPE;
                else if (clazz == Short.class)
                    clazz = Short.TYPE;
                else if (clazz == Integer.class)
                    clazz = Integer.TYPE;
                else if (clazz == Long.class)
                    clazz = Long.TYPE;                    
                else if (clazz == Float.class)
                    clazz = Float.TYPE;
                else if (clazz == Double.class)
                    clazz = Double.class;
                else if (value instanceof Map) {
                    // In case of Map (ScriptOptions) the class is org.mozilla.javascript.NativeObject
                    clazz = Map.class;
                } else if (value instanceof List) {
                    // In case of Map (ScriptOptions) the class is org.mozilla.javascript.NativeArray
                    clazz = List.class;
                }                

                java.lang.reflect.Method method = obj.getClass().getMethod("set" + name, clazz);
                method.invoke(obj, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
 
    final private static com.google.gson.Gson json = new com.google.gson.Gson();
}
