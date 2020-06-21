/* Copyright (C) 2020 Christoph Theis */
package countermanager.model.database;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Team {
    public String tmName;
    public String tmDesc;
    public String naName;
    public String naDesc;
    public String naRegion;
    
    public Map<String, Object> convertToMap(String prefix) {
        Map map = new java.util.HashMap<>();
        
        for (java.lang.reflect.Field f : getClass().getDeclaredFields()) {
            try {
                String name = f.getName();
                Object val  = f.get(this);

                if (val != null)
                    map.put(prefix + name, val);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(Match.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return map;
    }    
}
