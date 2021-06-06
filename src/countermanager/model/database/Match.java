/* Copyright (C) 2020 Christoph Theis */
package countermanager.model.database;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Match {
    public double  mtTimestamp;
    
    public String  cpName;
    public String  cpDesc;
    public int     cpType;
    public String  grStage;
    public String  grName;
    public String  grDesc;
    public int     grModus;
    public int     grSize;
    public int     grWinner;
    public int     grNofRounds;
    public int     grNofMatches;
    public int     mtMatches;
    public int     mtBestOf;
    public int     mtRound;
    public int     mtMatch;
    public int     mtNr;
    public int     mtMS;
    public boolean mtReverse;
    public int     mttmResA;
    public int     mttmResX;
    public int     mtResA;
    public int     mtResX;
    public boolean mtWalkOverA;
    public boolean mtWalkOverX;
    public int[][] mtResult;
    
    public double  mtDateTime;
    public int     mtTable;
    
    public Team    tmA;
    public Team    tmX;

    public Player  plA;    
    public Player  plB;
    public Player  plX;
    public Player  plY;
    
    public Umpire  up1;  // Main umpire
    public Umpire  up2;  // Assistent umpire
    
    public Map<String, Object> convertToMap(String prefix) {
        Map map = new java.util.HashMap<>();
        
        Class clazz = getClass();
        while (clazz != null && !clazz.equals(Object.class)) {
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                try {
                    String name = f.getName();
                    Object val  = f.get(this);

                    if (val == null)
                        ;
                    else if (val instanceof Team)
                        map.putAll( ((Team) val).convertToMap(prefix + name) );
                    else if (val instanceof Player)
                        map.putAll( ((Player) val).convertToMap(prefix + name) );
                    else
                        map.put(prefix + name, val);
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    Logger.getLogger(Match.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            clazz = clazz.getSuperclass();
        }
        
        return map;
    }
}
