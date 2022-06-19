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
    public int     cpSex;
    public String  grStage;
    public String  grName;
    public String  grDesc;
    public int     grModus;
    public int     grSize;
    public int     grWinner;
    public int     grNofRounds;
    public int     grNofMatches;
    public boolean syComplete = false;
    public int     mtMatches;
    public int     mtBestOf;
    public int     mtRound;
    public int     mtMatch;
    public int     mtNr;
    public int     mtMS;
    public boolean mtReverse;
    public int     nmType;      // Type of individual team match (1 or 2)
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
    
    // For statistics
    public long    startMatchTime = 0;
    public long    endMatchTime = 0;
    public long[]  startGameTime = new long[] {0, 0, 0, 0, 0, 0, 0};
    public long[]  elapsedGameTime = new long[] {0, 0, 0, 0, 0, 0, 0};
    public long[]  endGameTime = new long[] {0, 0, 0, 0, 0, 0, 0};
    
    
    // Start and stop and get running match time
    public void startMatchTime() {
        if (startMatchTime == 0)
            startMatchTime = System.nanoTime();
    }
    
    public void endMatchTime() {
        long ct = System.nanoTime();
        
        if (startMatchTime == 0)
            startMatchTime = ct;
        
        endMatchTime = ct;
    }
    
    public long getRunningMatchTime() {
        if (endMatchTime > 0)
            return endMatchTime - startMatchTime;
        else
            return System.nanoTime()- startMatchTime;
    }
    
    
    // Start, stop, break, and get game time
    public void startGameTime(int cs) {
        if (cs < 0 || cs >= startGameTime.length)
            return;
            
        if (startGameTime[cs] == 0)
            startGameTime[cs] = System.nanoTime();
    }
    
    public void breakGameTime(int cs) {
        if (cs < 0 || cs >= startGameTime.length)
            return;
            
        if (startGameTime[cs] == 0)
            return;
        
        elapsedGameTime[cs] += System.nanoTime() - startGameTime[cs];
        startGameTime[cs] = 0;
    }
    
    public void endGameTime(int cs) {
        if (cs < 0 || cs >= endGameTime.length)
            return;
            
        if (endGameTime[cs] != 0)
            return;
        
        long ct = System.nanoTime();
        
        if (startGameTime[cs] == 0)
            startGameTime[cs] = ct;
        
        endGameTime[cs] = ct;
    }
        
    // Get current game time
    public long getRunningGameTime(int cs) {
        if (cs < 0 || cs >= startGameTime.length)
            return 0;
        
        if (startGameTime[cs] == 0)
            return elapsedGameTime[cs];
        
        long ct = System.nanoTime();
        long ret = 0;
        
        if (endGameTime[cs] == 0 && startGameTime[cs] > ct) {
            System.out.println(String.format("Start game time %d less than current time %d for game %d", startGameTime[cs], ct, cs));
        }
        
        if (endGameTime[cs] == 0)
            ret = ct - startGameTime[cs] + elapsedGameTime[cs];
        else
            ret = endGameTime[cs] - startGameTime[cs]  + elapsedGameTime[cs];
        
        if (ret < 0) {
            System.out.println("Running game time < 0");
        }
        
        return ret;
    }
    
    
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
