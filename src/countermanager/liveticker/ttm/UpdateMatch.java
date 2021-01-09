/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.liveticker.ttm;

import countermanager.model.CounterModelMatch;
import java.util.Date;

/**
 *
 * @author chtheis
 */
class UpdateMatch {

    static class Player {

        int plNr;
        String psLast;
        String psFirst;
        String naName;
        String naRegion;
    }

    static class Team {
        String naName;
        String naRegion;
        String tmName;
        String tmDesc;
        int mtRes;
    }

    public UpdateMatch(CounterModelMatch counterMatch) {
        if (counterMatch.plA.plNr > 0) {
            this.plA = new UpdateMatch.Player();
            this.plA.plNr = counterMatch.plA.plNr;
            this.plA.psLast = counterMatch.plA.psLast;
            this.plA.psFirst = counterMatch.plA.psFirst;
            this.plA.naName = counterMatch.plA.naName;
            this.plA.naRegion = counterMatch.plA.naRegion;
        }
        if (counterMatch.plB.plNr > 0) {
            this.plB = new UpdateMatch.Player();
            this.plB.plNr = counterMatch.plB.plNr;
            this.plB.psLast = counterMatch.plB.psLast;
            this.plB.psFirst = counterMatch.plB.psFirst;
            this.plB.naName = counterMatch.plB.naName;
            this.plB.naRegion = counterMatch.plB.naRegion;
        }
        if (counterMatch.plX.plNr > 0) {
            this.plX = new UpdateMatch.Player();
            this.plX.plNr = counterMatch.plX.plNr;
            this.plX.psLast = counterMatch.plX.psLast;
            this.plX.psFirst = counterMatch.plX.psFirst;
            this.plX.naName = counterMatch.plX.naName;
            this.plX.naRegion = counterMatch.plX.naRegion;
        }
        if (counterMatch.plY.plNr > 0) {
            this.plY = new UpdateMatch.Player();
            this.plY.plNr = counterMatch.plY.plNr;
            this.plY.psLast = counterMatch.plY.psLast;
            this.plY.psFirst = counterMatch.plY.psFirst;
            this.plY.naName = counterMatch.plY.naName;
            this.plY.naRegion = counterMatch.plY.naRegion;
        }
        if (counterMatch.tmA != null && counterMatch.tmA.tmName != null && !counterMatch.tmA.tmName.isEmpty()) {
            this.tmA = new UpdateMatch.Team();
            this.tmA.naName = counterMatch.tmA.naName;
            this.tmA.naRegion = counterMatch.tmA.naRegion;
            this.tmA.tmName = counterMatch.tmA.tmName;
            this.tmA.tmDesc = counterMatch.tmA.tmDesc;
            this.tmA.mtRes = counterMatch.mttmResA;
        }
        if (counterMatch.tmX != null && counterMatch.tmX.tmName != null && !counterMatch.tmX.tmName.isEmpty()) {
            this.tmX = new UpdateMatch.Team();
            this.tmX.naName = counterMatch.tmX.naName;
            this.tmX.naRegion = counterMatch.tmX.naRegion;
            this.tmX.tmName = counterMatch.tmX.tmName;
            this.tmX.tmDesc = counterMatch.tmX.tmDesc;
            this.tmX.mtRes = counterMatch.mttmResX;
        }
        this.mtNr = counterMatch.mtNr;
        this.mtMS = counterMatch.mtMS;
        this.mtDateTime = new Date((long) counterMatch.mtDateTime);
        this.mtTable = counterMatch.mtTable;
        this.cpType = counterMatch.cpType;
        this.cpName = counterMatch.cpName;
        this.cpDesc = counterMatch.cpDesc;
        this.grName = counterMatch.grName;
        this.grDesc = counterMatch.grDesc;
        this.grModus = counterMatch.grModus;
        this.mtRound = counterMatch.mtRound;
        this.mtRoundString = roundToString(counterMatch);
        this.mtMatch = counterMatch.mtMatch;
        this.mtBestOf = counterMatch.mtBestOf;
        this.mtReverse = counterMatch.mtReverse;
    }
    
    public void copyGames(int[][] mtSets) {
        if (mtSets == null) {
            this.mtSets = null;
            return;
        }
        
        this.mtSets = new int[mtSets.length][];
        for (int i = 0; i < mtSets.length; i++) {
            this.mtSets[i] = new int[2];
            this.mtSets[i][0] = mtSets[i][0];
            this.mtSets[i][1] = mtSets[i][1];
        }
    }
    
    public void reverse() {
        Player tmpPl;
        Team   tmpTm;
        int    tmpInt;
        boolean tmpBool;
        
        tmpPl = plA;
        plA = plX;
        plX = tmpPl;
        
        tmpPl = plB;
        plB = plY;
        plY = tmpPl;
        
        tmpTm = tmA;
        tmA = tmX;
        tmX = tmpTm;
        
        tmpInt = mtResA;
        mtResA = mtResX;
        mtResX = tmpInt;
        
        if (mtSets != null) {
            for (int i = 0; i < mtSets.length; i++) {
                tmpInt = mtSets[i][0];
                mtSets[i][0] = mtSets[i][1];
                mtSets[i][1] = tmpInt;
            }
        }
        
        tmpBool = timeoutLeft;
        timeoutLeft = timeoutRight;
        timeoutRight = tmpBool;
        
        tmpBool = timeoutLeftRunning;
        timeoutLeftRunning = timeoutRightRunning;
        timeoutRightRunning = tmpBool;

        tmpBool = serviceLeft;
        serviceLeft = serviceRight;
        serviceRight = tmpBool;
    }

    static String roundToString(countermanager.model.database.Match counterMatch) {
        switch (counterMatch.grModus) {
            case 1:
                // RR
                return "Rd.&nbsp;" + counterMatch.mtRound;
            case 2:
                // SKO
                // SKO
                {
                    int nof = counterMatch.grSize >> counterMatch.mtRound;
                    if (counterMatch.grWinner > 1) {
                        return "Rd.&nbsp;" + counterMatch.mtRound;
                    }
                    if (counterMatch.grNofRounds > 0 || counterMatch.grNofMatches > 0) {
                        return "Rd.&nbsp;" + counterMatch.mtRound;
                    }
                    if (nof == 1) {
                        return "F";
                    } else if (nof == 2) {
                        return "SF";
                    } else if (nof == 4) {
                        return "QF";
                    } else {
                        return "Rd.&nbsp;of&nbsp;" + (2 * nof);
                    }
                }
            case 3:
                // DKO
                // DKO
                {
                    return "Rd.&nbsp;" + counterMatch.mtRound;
                }
            case 4:
                // PLO
                // PLO
                {
                    int nof = 
                        counterMatch.grNofRounds > 0 ?
                        1 << (counterMatch.grNofRounds - counterMatch.mtRound) :
                        counterMatch.grSize >> counterMatch.mtRound;
                    
                    if (counterMatch.grWinner == 1) {
                        if (nof == 1 && counterMatch.mtMatch == 1) {
                            return "F";
                        } else if (nof == 2 && counterMatch.mtMatch <= 2) {
                            return "SF";
                        } else if (nof == 4 && counterMatch.mtMatch <= 4) {
                            return "QF";
                        }
                    }
                    int m = counterMatch.mtMatch - 1;
                    int from = (m / nof) * nof;
                    int to = from + nof;
                    // 1 hier ist grWinner
                    return "Pos&nbsp;" + (counterMatch.grWinner - 1 + (1 + 2 * from)) + "&mdash;" + (counterMatch.grWinner - 1 + (1 + 2 * to - 1));
                }
        }
        return "Rd.&nbsp;" + counterMatch.mtRound;
    }
    
    Team tmA;
    Team tmX;
    Player plA;
    Player plB;
    Player plX;
    Player plY;
    int mtNr;
    int mtMS;
    int mtTable;
    Date mtDateTime;
    int    cpType;
    String cpName;
    String cpDesc;
    String grName;
    String grDesc;
    int    grModus;
    int    mtRound;
    int    mtMatch;
    String mtRoundString;
    int mtResA;
    int mtResX;
    int mtBestOf;
    int[][] mtSets;
    boolean mtReverse;
    int time;
    boolean timeoutLeft;
    boolean timeoutRight;
    boolean serviceLeft;
    boolean serviceRight;
    boolean walkOver;
    boolean matchRunning;
    boolean gameRunning;
    boolean timeoutLeftRunning;
    boolean timeoutRightRunning;
    String resultLocation;
    
}
