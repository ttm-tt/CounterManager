/* Copyright (C) 2020 Christoph Theis */
/**
 * Define interface for different database layers
 */

package countermanager.model.database;

import countermanager.model.CounterModelMatch;
import java.time.LocalDate;
import java.util.List;

public interface IDatabase {
    /**
     * The connection string used to connect to a database
     * @return the connection string or null if not applicable
     */
    public String  getConnectionString();
    
    /**
     * Connect to the database
     * @return true if successful
     */
    public boolean connect();
    
    /**
     * Disconnect from the database
     */
    public void    disconnect();
    
    /**
     * The connection status
     * @return true if connected
     */
    public boolean isConnected();
    
    /**
     * Test if a connection to the database is possible with the given string
     * @param connectString used to try the connection
     * @return true if connection is possible
     */
    public boolean testConnection(String connectString);
    
    /**
     * Calculates the timestamp in milliseconds since start of epoch when a match was
     * changed the last time
     * @return the timestamp of the last change to a match
     */
    public long getMaxMtTimestamp();
    
    /**
     * 
     * @param timestamp
     * @return 
     */
    public List<LocalDate> getChangedDates(long timestamp);
    
    /**
     * 
     * @param fromTable
     * @param toTable
     * @param date
     * @param all
     * @return 
     */
    public List<CounterModelMatch> update(int fromTable, int toTable, java.time.LocalDate date, boolean all);
    
    /**
     * Write a result into the database
     * @param mtNr the match number
     * @param mtMS the individual match number in a team match
     * @param mtSets the results of all games
     * @param mtWalkOverA the w/o flag of home team
     * @param mtWalkOverX the w/o flag of away team
     * @return true if successful
     */
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX);
    
    /**
     * Get a list of players
     * @param naName filter by nation
     * @return an array of players
     */
    public Player[] listPlayers(String naName);
    
    /**
     * Get a list of group entries with standing in case of round robin groups
     * @param cpNames limit to list of competitions
     * @param grStage limit to group stage
     * @param grNames limit ot list of groups
     * @return list of entries
     */
    public Entry[] listEntries(List<String> cpNames, String grStage, List<String> grNames);
    
    /**
     * Get a list of matches according to schedule
     * @param mtTimestamp limit to those changed since
     * @param from limit to those scheduled at or later
     * @param to limit to those scheduleed at or before
     * @param fromTable limit to those scheduled on table or higher
     * @param toTable limit to those scheduled on table or lower
     * @param individual returns individual matches within a team match
     * @param notStarted returns only matches which have no result yet
     * @param notFinished returns only matches which are not yet finished
     * @return a list of schedules
     */
    public Match[] listMatches(
            long mtTimestamp, java.time.LocalDateTime from, java.time.LocalDateTime to, int fromTable, int toTable, 
            boolean individual, boolean notStarted, boolean notFinished);
    
    
    /**
     * Get a list of all match times of given day
     * @param day limit result to matches scheduled for this day. 
     * If 9 then the match times of all days are returned
     * @return a list with unique scheduled times
     */
    public List<Long> getTimes(int day);
    
    /**
     * Get a list of all matches scheduled on tables between fromTable and toTable
     * @param fromTable 
     * @param toTable
     * @return 
     */
    public List<CounterModelMatch> getCurrentTeamMatches(int fromTable, int toTable);
    
    /**
     * Get a the settings to configure the database
     * @return 
     */
    public IDatabaseSettings getSettingsPanel();
    
    /**
     * 
     * @return 
     */
    public countermanager.prefs.Properties getProperties();
    
    /**
     * 
     * @param props 
     */
    public void putProperties(countermanager.prefs.Properties props);
}
