/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.model.database.simulation;

import countermanager.model.CounterModel;
import countermanager.model.CounterModelMatch;
import countermanager.model.database.Entry;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import countermanager.model.database.Player;
import countermanager.model.database.Match;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 *
 * @author chtheis
 */
public final class Simulation implements IDatabase {

    final static String DOUBLES_EACH_PREF = "DoublesEach";
    final static String TEAMS_EACH_PREF = "TeamsEach";
    final static long   MATCH_TIMEOUT = 60 * 60 * 1000;
    
    public Simulation() {
        countermanager.prefs.Properties prefs = getProperties();
        
        doublesEach = prefs.getInt(DOUBLES_EACH_PREF, 0);
        teamsEach = prefs.getInt(TEAMS_EACH_PREF, 0);
    }
    
    public Simulation(int doublesEach, int teamsEach) {
        this.doublesEach = doublesEach;
        this.teamsEach = teamsEach;
    }
    
    @Override
    public String getConnectionString() {
        return null;
    }

    @Override
    public boolean connect() {
        connected = true;
        return true;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean testConnection(String connectString) {
        return true;
    }

    private final Object syncUpdateMatch = new Object();
    
    @Override
    public List<CounterModelMatch> update(int fromTable, int toTable, java.time.LocalDate when, boolean all) {
        synchronized (syncUpdateMatch) {
            List<CounterModelMatch> list = new java.util.ArrayList<>();
            for (int table = fromTable; table <= toTable; table++) {
                if (table < 1)
                    continue;
                if (table > CounterModel.getDefaultInstance().getToTable())
                    break;

                if (matches.get(table) == null)
                    matches.put(table, new java.util.ArrayList<>());
                if (matches.get(table).isEmpty())
                    matches.get(table).add(createMatch(table));

                // Housekeeping: remove abandoned matches
                if (matches.get(table).get(0).mtResult != null && CounterModel.getDefaultInstance().getCounterData(table) == null) {
                    if (matches.get(table).get(0).mtResult[0][0] > 0 || matches.get(table).get(0).mtResult[0][1] > 0) {
                        if (matches.get(table).get(0).mtTimestamp < System.currentTimeMillis() - MATCH_TIMEOUT) {
                            matches.get(table).remove(0);
                            matches.get(table).add(createMatch(table));
                        }
                    }
                }

                if (all)
                    list.addAll(matches.get(table));
                else {
                    for (CounterModelMatch mt : matches.get(table)) {
                        if (mt.mtWalkOverA || mt.mtWalkOverX)
                            continue;

                        if (2 * mt.mtResA > mt.mtBestOf || 2 * mt.mtResX > mt.mtBestOf)
                            continue;

                        list.add(mt);
                    }
                }
            }

            return list;
        }
    }

    @Override
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX) {
        synchronized (syncUpdateMatch) {
            for (List<CounterModelMatch> list : matches.values()) {
                for (CounterModelMatch mt : list) {
                    if (mt.mtNr != mtNr || mt.mtMS != mtMS)
                        continue;

                    // Remove all earlier and thus finished matches
                    while (mt != list.get(0))
                        list.remove(0);

                    // And add one more
                    if (list.size() < 2)
                        list.add(createMatch(mt.mtTable));

                    mt.mtResult = mtSets;
                    mt.mtWalkOverA = mtWalkOverA > 0;
                    mt.mtWalkOverX = mtWalkOverX > 0;
                    mt.mtTimestamp = System.currentTimeMillis();

                    // Calculate result in 
                    mt.mtResA = 0;
                    mt.mtResX = 0;
                    for (var game = 0; game < mtSets.length; ++game) {
                        if (mtSets[game][0] >= 11 && mtSets[game][0] >= mtSets[game][1] + 2)
                            ++mt.mtResA;
                        if (mtSets[game][1] >= 11 && mtSets[game][1] >= mtSets[game][0] + 2)
                            ++mt.mtResX;
                    }

                    return true;
                }
            }

            return false;
        }
    }
    
    @Override
    public List<CounterModelMatch> getCurrentTeamMatches(int fromTable, int toTable) {
        return new java.util.ArrayList<>();
    }

    @Override
    public IDatabaseSettings getSettingsPanel() {
        return new countermanager.model.database.simulation.SimulationSettings(this);
    }
    
    @Override
    public countermanager.prefs.Properties getProperties() {
        countermanager.prefs.Properties props = new countermanager.prefs.Properties();
        
        try {
            props.load(getClass().getName());
        } catch (Exception ex) {
            
        }

        return props;
    }
    
    
    @Override
    public void putProperties(countermanager.prefs.Properties props) {
        try {
            props.store(getClass().getName());

            doublesEach = props.getInt(DOUBLES_EACH_PREF, 4);
            teamsEach = props.getInt(TEAMS_EACH_PREF, 4);
        } catch (Exception ex) {

        }
    }
    
    
    private int doublesEach = 0;
    private int teamsEach = 0;
    private boolean connected = false;
    private int lastMtNr = 0;
    private Map<Integer, List<CounterModelMatch>> matches = new java.util.HashMap<>();
    
    private CounterModelMatch createMatch(int table) {
        CounterModelMatch match = new CounterModelMatch();
        match.mtNr = ++lastMtNr;
        match.mtTable = table;
        match.cpName = "SIM";
        match.cpDesc = "Simulation";
        match.mtDateTime = (System.currentTimeMillis() / (1000 * 60 * 10) * (1000 * 60 * 10)) + (1000 * 60 * 10);
        match.mtTimestamp = System.currentTimeMillis();

        if ( teamsEach > 0 && (table % teamsEach) == 0 ) {
            match.cpType = 4;
            match.mtMS = 1;
            match.mtBestOf = 5;
            if (doublesEach > 0 && (table % doublesEach) == 0)
                match.nmType = 2;
            else
                match.nmType = 1;
        } else if ( doublesEach > 0 && (table % doublesEach) == 0 ) {
            match.cpType = 2;
            match.mtMS = 0;
            match.mtBestOf = 5;
            match.nmType = 2;
        } else {                
            match.cpType = 1;
            match.mtMS = 0;
            match.mtBestOf = 7;
            match.nmType = 1;
        }

        match.grName = "GR-" + table;
        match.grDesc = "Sim";
        match.grModus = 1;
        match.mtRound = 1;
        match.mtMatch = 1;
        
        match.grNofRounds = 0;
        match.grNofMatches = 0;

        match.plA.plNr = 1000 + table;
        match.plA.psFirst = "A";
        match.plA.psLast = "Player";
        match.plA.naName = "ABC";

        match.plX.plNr = 2000 + table;
        match.plX.psFirst = "X";
        match.plX.psLast = "Player";
        match.plX.naName = "XYZ";

        if ( doublesEach > 0 &&  (table % doublesEach) == 0 ) {
            match.plB.plNr = 3000 + table;
            match.plB.psFirst = "B";
            match.plB.psLast = "Player";
            match.plB.naName = "ABC";

            match.plY.plNr = 4000 + table;
            match.plY.psFirst = "Y";
            match.plY.psLast = "Player";
            match.plY.naName = "XYZ";
        }

        if ( teamsEach > 0 && (table % teamsEach) == 0) {
            match.tmA.naName = "ABC";
            match.tmA.tmName = "ABC";
            match.tmA.tmDesc = "Team ABC";

            match.tmX.naName = "XYZ";
            match.tmX.tmName = "XYZ";
            match.tmX.tmDesc = "Team XYZ";
        }

        match.mttmResA = 0;
        match.mttmResX = 0;

        if (false) {
            match.mtResA = 1;
            match.mtResult = new int[][] {{11, 1}, {1, 0}, {0, 0}, {0, 0}, {0, 0}};
        }       
        
        return match;
    }

    @Override
    public List<LocalDate> getChangedDates(long timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public long getMaxMtTimestamp() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Player[] listPlayers(String naName) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public Match[] listMatches(
            long mtTimestamp, java.time.LocalDateTime from, java.time.LocalDateTime to, int fromTable, int toTable, 
            boolean individual, boolean notStarted, boolean notFinished) {
        synchronized (syncUpdateMatch) {
            List<Match> list = new java.util.ArrayList<>();

            for (int table = fromTable; table <= toTable; table++) {
                if (table < 1)
                    continue;
                if (table > CounterModel.getDefaultInstance().getToTable())
                    break;

                if (matches.get(table) == null)
                    continue;

                for (CounterModelMatch mt : matches.get(table)) {
                    if (mt.mtTimestamp <= mtTimestamp)
                        continue;

                    if (java.time.ZonedDateTime.of(from, ZoneId.systemDefault()).toInstant().toEpochMilli() > mt.mtDateTime)
                        continue;
                    if (java.time.ZonedDateTime.of(to, ZoneId.systemDefault()).toInstant().toEpochMilli() < mt.mtDateTime)
                        continue;

                    if (notStarted && (mt.mtResult[0][0] > 0 || mt.mtResult[0][1] > 0))
                        continue;

                    if (notFinished && (2 * mt.mtResA > mt.mtBestOf || 2 * mt.mtResX > mt.mtBestOf))
                        continue;

                    list.add(mt);
                }
            }

            return list.toArray(Match[]::new);
        }
    }

    @Override
    public Entry[] listEntries(List<String> cpNames, String grStage, List<String> grNames) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }


    @Override
    public List<Long> getTimes(int day) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
}
