/* Copyright (C) 2020 Christoph Theis */

package countermanager.model.database.league;

import countermanager.model.CounterModelMatch;
import countermanager.model.database.Entry;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import countermanager.model.database.Player;
import countermanager.model.database.Match;
import countermanager.model.database.league.MatchDetails.MatchType;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;


public class League implements IDatabase {
    
    private final static String dbFileName = "league.xml";
    
    JAXBContext context;

    @Override
    public List<LocalDate> getChangedDates(long timestamp) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public long getMaxMtTimestamp() {
        double max = 0;
        for (CounterModelMatch match : database.list) {
            if (match.mtTimestamp > max)
                max = match.mtTimestamp;
        }
        
        return (long) max;
    }

    @Override
    public Player[] listPlayers(String naName) {
        java.util.Set<Player> set = new java.util.HashSet<>();
        
        for (CounterModelMatch match : database.list) {
            if (match.plA != null)
                set.add(match.plA);
            if (match.plB != null)
                set.add(match.plB);
            if (match.plX != null)
                set.add(match.plX);
            if (match.plY != null)
                set.add(match.plY);
        }
        
        return set.toArray(new Player[0]);        
    }

    @Override
    public Match[] listMatches(long mtTimestamp, java.time.LocalDateTime from, java.time.LocalDateTime to, int fromTable, int toTable, boolean individual, boolean notStarted, boolean notFinished) {
        List<Match> list = new java.util.ArrayList<>();
        
        for (CounterModelMatch match : database.list) {
            if (mtTimestamp > 0 && mtTimestamp <= match.mtTimestamp)
                continue;
            if (from.compareTo(new java.sql.Timestamp((long) match.mtDateTime).toLocalDateTime()) > 0)
                continue;
            if (to.compareTo(new java.sql.Timestamp((long) match.mtDateTime).toLocalDateTime()) < 0)
                continue;
            if (notStarted && (match.mtResult[0][0] > 0 || match.mtResult[0][1] > 0))
                continue;
            if (notFinished && (2 * match.mtResA > match.mtBestOf || 2 * match.mtResX > match.mtBestOf))
                continue;
            
            list.add(match);
        }
        
        return list.toArray(new Match[0]);
    }

    @Override
    public Entry[] listEntries(List<String> cpNames, String grStage, List<String> grNames) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public List<Long> getTimes(int day) {
       List<Long> list = new java.util.ArrayList<>();
       if (database.list.size() > 0)
           list.add((long) database.list.get(0).mtDateTime);
       
       return list;
    }

    @XmlRootElement
    private static class Database {
        @XmlElementWrapper(name="matches")
        @XmlElement(name="match")
        public List<CounterModelMatch> list = new java.util.ArrayList<>();
    }
    
    private Database database;
    
    public League() {
        try {
            context = JAXBContext.newInstance(Database.class);
        } catch (JAXBException ex) {
            Logger.getLogger(League.class.getName()).log(Level.SEVERE, null, ex);
        }
        load();
    }
    
    @Override
    public String getConnectionString() {
        return "";
    }
    
    @Override
    public boolean connect() {
        return true;
    }

    @Override
    public void disconnect() {
        
    }

    @Override
    public boolean isConnected() {
        return true;
    }
    
    @Override
    public boolean testConnection(String connectString) {
        return true;
    }

    @Override
    public List<CounterModelMatch> update(int fromTable, int toTable, java.time.LocalDate when, boolean all) {
        Map<Integer, CounterModelMatch> map = new java.util.HashMap<>();

        for (CounterModelMatch match : database.list) {
            if (match.mtTable < fromTable || match.mtTable > toTable)
                continue;
            
            // Only not finished matches
            if (match.mttmResA > match.mtMatches / 2)
                continue;
            if (match.mttmResX > match.mtMatches / 2)
                continue;
            
            if (match.mtResA > match.mtBestOf / 2)
                continue;
            if (match.mtResX > match.mtBestOf / 2)
                continue;
            
            if (map.containsKey(match.mtTable))
                continue;
            
            map.put(match.mtTable, match);            
        }

        List<CounterModelMatch> matches = new java.util.ArrayList<>(map.values());
        
        java.util.Collections.sort(matches, new java.util.Comparator<CounterModelMatch>() {

            @Override
            public int compare(CounterModelMatch o1, CounterModelMatch o2) {
                if (o1.mtTable < o2.mtTable)
                    return -1;
                if (o1.mtTable > o2.mtTable)
                    return +1;
                if (o1.mtNr < o2.mtNr)
                    return -1;
                if (o2.mtNr > o2.mtNr)
                    return +1;
                if (o1.mtMS < o2.mtMS)
                    return -1;
                if (o2.mtMS > o2.mtMS)
                    return +1;
                return 0;
            }
        });

        return matches;
    }

    @Override
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX) {
        for (CounterModelMatch match : database.list) {
            if (match.mtNr == mtNr && match.mtMS == mtMS) {
                match.mtResult = mtSets;
                match.mtResA = match.mtResX = 0;
                for (int[] res : mtSets) {
                    if (res[0] >= 11 && res[0] >= res[1] + 2)
                        match.mtResA++;
                    if (res[1] >= 11 && res[1] >= res[0] + 2)
                        match.mtResX++;
                }
                
                match.mtTimestamp = System.currentTimeMillis();
                
                break;
            }
        }
        
        int resA = 0, resX = 0;
        for (CounterModelMatch match : database.list){
            if (match.mtResA > match.mtBestOf / 2)
                resA++;
            if (match.mtResX > match.mtBestOf / 2)
                resX++;
        }
        
        for (CounterModelMatch match : database.list) {
            match.mttmResA = resA;
            match.mttmResX = resX;
        }
        
        store();
        return true;
    }
    
    @Override
    public List<CounterModelMatch> getCurrentTeamMatches(int fromTable, int toTable) {
        return database.list;
    }
    
    
    private void load() {
        try {
            Unmarshaller um = context.createUnmarshaller();
            java.io.File dir = countermanager.prefs.Properties.getIniFile().getParentFile();
            database = (Database) um.unmarshal(new java.io.FileInputStream(new java.io.File(dir, dbFileName)));
        } catch (JAXBException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            database = new Database();
        } catch (FileNotFoundException ex) {
            // Logger.getLogger(League.class.getName()).log(Level.SEVERE, null, ex);
            database = new Database();
        }
    }
    
    private void store() {
        long ct = System.currentTimeMillis();
        
        for (CounterModelMatch match : database.list) {
            match.mtTimestamp = ct;
        }        
        
        try {
            java.io.File dir = countermanager.prefs.Properties.getIniFile().getParentFile();
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(database, new java.io.FileOutputStream(new java.io.File(dir, dbFileName)));    
        } catch (JAXBException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public IDatabaseSettings getSettingsPanel() {
        return new LeagueSettings(this);
    }
        
    @Override
    public countermanager.prefs.Properties getProperties() {
        countermanager.prefs.Properties properties = new countermanager.prefs.Properties();
        
        try {
            properties.load(getClass().getName());
        } catch (Exception ex) {
            
        }

        return properties;
    }
    
    @Override
    public void putProperties(countermanager.prefs.Properties props) {
        try {
            props.store(getClass().getName());
        } catch (Exception ex) {

        }        
    }

    List<MatchDetails> getMatchList() {
        List<MatchDetails> detailList = new java.util.ArrayList<>();
        
        for (CounterModelMatch match : database.list) {
            MatchDetails detail = new MatchDetails();
            detail.type = match.plB == null || match.plB.psLast == null || match.plB.psLast.isEmpty() ? MatchType.Single : MatchType.Double;
            detail.plApsFirst = match.plA.psFirst;
            detail.plApsLast = match.plA.psLast;
            detail.plBpsFirst = match.plB.psFirst;
            detail.plBpsLast = match.plB.psLast;
            detail.plXpsFirst = match.plX.psFirst;
            detail.plXpsLast = match.plX.psLast;
            detail.plYpsFirst = match.plY.psFirst;
            detail.plYpsLast = match.plY.psLast;
            detail.mtTable = match.mtTable;
            detail.tmAtmName = match.tmA.tmName;
            detail.tmAtmDesc = match.tmA.tmDesc;
            detail.tmAnaName = match.tmA.naName;
            detail.tmXtmName = match.tmX.tmName;
            detail.tmXtmDesc = match.tmX.tmDesc;
            detail.tmXnaName = match.tmX.naName;
            detail.mtTable = match.mtTable;
            detail.mtDateTime = (long) match.mtDateTime;
            detail.mtBestOf = match.mtBestOf > 0 ? match.mtBestOf : 5;
                        
            detailList.add(detail);
        }
        
        return detailList;
    }
    
    void setMatchList(List<MatchDetails> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            database.list.clear();
            store();
            return;
        }
        
        long ct = System.currentTimeMillis();
        
        while (database.list.size() < detailList.size())
            database.list.add(new CounterModelMatch());
        while (database.list.size() > detailList.size())
            database.list.remove(database.list.size() - 1);
        
        for (int idx = 0; idx < detailList.size(); idx++) {
            MatchDetails detail = detailList.get(idx);
            CounterModelMatch match = database.list.get(idx);
            
            match.tmA.tmName = detail.tmAtmName;
            match.tmA.tmDesc = detail.tmAtmDesc;
            match.tmA.naName = detail.tmAnaName;
            match.tmX.tmName = detail.tmXtmName;
            match.tmX.tmDesc = detail.tmXtmDesc;
            match.tmX.naName = detail.tmXnaName;
            match.plA.plNr = 101 + idx;
            match.plA.psFirst = detail.plApsFirst;
            match.plA.psLast = detail.plApsLast;
            match.plA.naName = detail.tmAnaName;
            match.plX.plNr = 301 + idx;
            match.plX.psFirst = detail.plXpsFirst;
            match.plX.psLast = detail.plXpsLast;
            match.plX.naName = detail.tmXnaName;
            
            if (detail.type == MatchType.Double) {
                match.plB.psFirst = detail.plBpsFirst;
                match.plB.psLast = detail.plBpsLast;
                match.plY.psFirst = detail.plYpsFirst;
                match.plY.psLast = detail.plYpsLast;
                if (detail.plBpsLast != null && !detail.plBpsLast.isEmpty()) {
                    match.plB.naName = detail.tmAnaName;
                    match.plB.plNr = 201 + idx;
                } else {
                    match.plB.naName = "";
                    match.plB.plNr = 0;
                }
                if (detail.plYpsLast != null && !detail.plYpsLast.isEmpty()) {
                    match.plY.naName = detail.tmXnaName;
                    match.plY.plNr = 401 + idx;
                } else {
                    match.plY.naName = "";
                    match.plY.plNr = 0;
                }
            } else {
                match.plB.plNr = 0;
                match.plB.psFirst = "";
                match.plB.psLast = "";
                match.plB.naName = "";
                
                match.plY.plNr = 0;
                match.plY.psFirst = "";
                match.plY.psLast = "";
                match.plY.naName = "";
            }
            match.mtTable = (detail.mtTable > 0 ? detail.mtTable : 1);
            match.mtDateTime = detail.mtDateTime == 0 ? System.currentTimeMillis() : detail.mtDateTime;
            match.mtBestOf = detail.mtBestOf;
            
            match.mtNr = 1;
            match.mtMS = idx + 1;
            match.cpName = "";
            match.cpDesc = "";
            match.cpType = 4;
            match.grName = "";
            match.grDesc = "";
            match.grStage = "";
            match.grModus = 2;
            match.grNofMatches = 1;
            match.grNofRounds = 1;
            match.grSize = 2;
            match.mtRound = 1;
            match.mtMatch = 1;
            match.mtMatches = detailList.size();   
            match.mtTimestamp = 0;
            
            match.mtResult = new int[match.mtBestOf][2];
            match.mtTimestamp = ct;
        }
        
        store();
    }
}
