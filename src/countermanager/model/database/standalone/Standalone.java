/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import countermanager.model.CounterModelMatch;
import countermanager.model.database.Entry;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import countermanager.model.database.Player;
import countermanager.model.database.Match;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

public class Standalone implements IDatabase {

    private static final String dbFileName = "standalone.xml";
    
    Database database;
    JAXBContext context;
    
    public Standalone() {
        try {
            context = JAXBContext.newInstance(Database.class);
            Unmarshaller um = context.createUnmarshaller();
            database = (Database) um.unmarshal(new java.io.FileReader(dbFileName));
        } catch (JAXBException ex) {
            Logger.getLogger(Standalone.class.getName()).log(Level.SEVERE, null, ex);
            database = new Database();
        } catch (FileNotFoundException ex) {
            // Logger.getLogger(XmlDatabase.class.getName()).log(Level.SEVERE, null, ex);
            database = new Database();
        }
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
        List<CounterModelMatch> list = new java.util.ArrayList<>();
        
        java.util.Collections.sort(database.matches);

        Set<Integer> set = new java.util.HashSet<>();

        for (countermanager.model.database.standalone.Match match : database.matches) {
            if (match.mtTable < fromTable || match.mtTable > toTable)
                continue;
            
            // Only not finished matches
            if (2 * match.mtResA > match.gr.cp.mtBestOf) {
                continue;
            }
            
            if (2 * match.mtResX > match.gr.cp.mtBestOf) {
                continue;
            }
            
            // Not finished team matches
            if (match.gr.cp.mtMatches > 1 && 2 * match.mttmResA > match.gr.cp.mtMatches)
                continue;
            
            if (match.gr.cp.mtMatches > 1 && 2 * match.mttmResX > match.gr.cp.mtMatches)
                continue;
            
            if (set.contains(match.mtTable))
                continue;

            set.add(match.mtTable);   

            list.add(match.toMatch());
        }
        
        return list;
    }

    @Override
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX) {
        int mttmResA = 0, mttmResX = 0;
        
        for (countermanager.model.database.standalone.Match match : database.matches) {
            if (match.mtNr == mtNr && match.mtMS == mtMS) {
                match.setResult(mtSets);

                mttmResA = match.mttmResA;
                mttmResX = match.mttmResX;
                
                break;
            }
        }
        
        for (countermanager.model.database.standalone.Match match : database.matches) {
            if (match.mtNr == mtNr) {
                match.mttmResA = mttmResA;
                match.mttmResX = mttmResX;
            }
        }
        
        return store();
    }

    @Override
    public List<CounterModelMatch> getCurrentTeamMatches(int fromTable, int toTable) {
        return new java.util.ArrayList<>();
    }
    
    @Override
    public IDatabaseSettings getSettingsPanel() {
        return new StandaloneSettings(this);
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
    

    boolean store() {
        // Sort tables
        java.util.Collections.sort(database.nations);
        java.util.Collections.sort(database.players);
        java.util.Collections.sort(database.competitions);
        java.util.Collections.sort(database.matches);

        try {
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(database, new java.io.FileWriter(dbFileName));    
            
            return true;
        } catch (JAXBException ex) {
            Logger.getLogger(Standalone.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Standalone.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Standalone.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
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
        throw new UnsupportedOperationException("Not supported yet."); 
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
