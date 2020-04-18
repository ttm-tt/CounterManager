/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.model.database;

import countermanager.model.CounterModelMatch;
import java.util.List;

/**
 *
 * @author chtheis
 */
public interface IDatabase {
    public String  getConnectionString();
    public boolean connect();
    public void    disconnect();
    public boolean isConnected();
    public boolean testConnection(String connectString);
    public List<CounterModelMatch> update(int fromTable, int toTable);
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX);
    
    public List<CounterModelMatch> getCurrentTeamMatches(int fromTable, int toTable);
    
    public IDatabaseSettings getSettingsPanel();
    
    public countermanager.prefs.Properties getProperties();
    public void putProperties(countermanager.prefs.Properties props);
}
