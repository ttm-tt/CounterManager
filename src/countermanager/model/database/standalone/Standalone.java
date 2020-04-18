/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import countermanager.model.CounterModelMatch;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    
    public static void main(String[] args) throws JAXBException, FileNotFoundException {
/*        
        int id = 0;
        
        Database db = new Database();
        
        db.nations = new java.util.ArrayList<>();
        Nation na = new Nation();
        na.naName = "GER";
        na.naDesc = "Germany";
        na.naID = "" + ++id;
        db.nations.add(na);
        
        db.competitions = new java.util.ArrayList<>();
        Competition cp = new Competition();
        cp.cpName = "JB";
        cp.cpDesc = "Junior Boys";
        cp.cpID = "" + ++id;
        
        db.players = new java.util.ArrayList<>();
        Player plA = new Player();
        plA.nation = na;
        plA.psFirst = "Timo";
        plA.psLast = "Boll";
        plA.psSex = 1;
        plA.plID = "" + ++id;
        db.players.add(plA);
        
        JAXBContext context = JAXBContext.newInstance(Database.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Write to System.out
        m.marshal(db, System.out);    
        
        JAXBContext context = JAXBContext.newInstance(Database.class);
        Unmarshaller um = context.createUnmarshaller();
        Database db = (Database) um.unmarshal(new java.io.FileReader("F:\\tmp\\db.xml"));    
*/
    }
    
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
    public List<CounterModelMatch> update(int fromTable, int toTable) {
        List<CounterModelMatch> list = new java.util.ArrayList<>();
        
        java.util.Collections.sort(database.matches, new java.util.Comparator<Match>() {

            @Override
            public int compare(Match o1, Match o2) {
                if (o1.mtDateTime < o2.mtDateTime)
                    return -1;
                if (o1.mtDateTime > o2.mtDateTime)
                    return +1;

                if (o1.mtTable < o2.mtTable)
                    return -1;
                if (o1.mtTable > o2.mtTable)
                    return +1;

                return 0;
            }
        });

        Set<Integer> set = new java.util.HashSet<>();

        for (Match match : database.matches) {
            if (match.mtTable < fromTable || match.mtTable > toTable)
                continue;

            // Only not finished matches
            if (match.mtResA > match.cp.mtBestOf / 2)
                continue;
            if (match.mtResX > match.cp.mtBestOf / 2)
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
        for (Match match : database.matches) {
            if (match.mtNr == mtNr) {
                match.setResult(mtSets);

                break;
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
}
