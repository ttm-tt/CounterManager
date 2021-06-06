/* Copyright (C) 2020 Christoph Theis */
package countermanager.model.database;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chtheis
 */
public class Umpire extends Object {
    public int upNr;
    public String psLast;
    public String psFirst;
    public String naName;
    public String naDesc;
    public String naRegion;
    
    public Map<String, Object> convertToMap(String prefix) {
        Map map = new java.util.HashMap<>();
        
        Class clazz = getClass();
        
        while (clazz != null && !clazz.equals(Object.class)) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                try {
                    String name = f.getName();
                    Object val  = f.get(this);

                    if (val != null)
                        map.put(prefix + name, val);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Match.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            clazz = clazz.getSuperclass();
        }
        
        return map;
    }
    
    public boolean equals(Umpire up) {
        return 
            upNr == up.upNr &&
            psLast.equals(up.psLast) &&
            psFirst.equals(up.psFirst) &&
            naName.equals(up.naName)
        ;
    }
    
}
