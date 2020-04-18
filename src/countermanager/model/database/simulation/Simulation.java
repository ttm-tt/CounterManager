/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.model.database.simulation;

import countermanager.model.CounterModelMatch;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import java.util.List;

/**
 *
 * @author chtheis
 */
public final class Simulation implements IDatabase {

    final static String DOUBLES_EACH_PREF = "DoublesEach";
    final static String TEAMS_EACH_PREF = "TeamsEach";
    
    public Simulation() {
        countermanager.prefs.Properties prefs = getProperties();
        
        doublesEach = prefs.getInt(DOUBLES_EACH_PREF, 0);
        teamsEach = prefs.getInt(TEAMS_EACH_PREF, 0);
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

    @Override
    public List<CounterModelMatch> update(int fromTable, int toTable) {
        List<CounterModelMatch> list = new java.util.ArrayList<>();
        for (int table = fromTable; table <= toTable; table++) {
            CounterModelMatch match = new CounterModelMatch();
            match.mtNr = table;
            match.mtTable = table;
            match.cpName = "SIM";
            match.cpDesc = "Simulation";
            // match.mtDateTime = (System.currentTimeMillis() / 1000) + 3600;
            
            if ( teamsEach > 0 && (table % teamsEach) == 0 ) {
                match.cpType = 4;
                match.mtMS = 1;
            } else if ( doublesEach > 0 && (table % doublesEach) == 0 ) {
                match.cpType = 2;
                match.mtMS = 0;
            } else {                
                match.cpType = 1;
                match.mtMS = 0;
            }
            
            match.grName = "GR-" + table;
            match.grModus = 1;
            match.mtBestOf = 5;
            
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

            list.add(match);
        }
        
        return list;
    }

    @Override
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX) {
        return true;
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
    
}
