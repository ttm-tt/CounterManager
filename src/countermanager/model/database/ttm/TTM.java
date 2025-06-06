/* Copyright (C) 2020 Christoph Theis */

package countermanager.model.database.ttm;

import countermanager.model.CounterModelMatch;
import countermanager.model.database.Entry;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
import countermanager.model.database.Player;
import countermanager.model.database.Match;
import countermanager.model.database.Team;
import countermanager.model.database.Umpire;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author chtheis
 */
public final class TTM implements IDatabase {

    final static String DATABASE_PREF = "Database";
    final static String SERVER_PREF = "Server";
    final static String WINDOWS_AUTH_PREF = "WindowsAuth";
    final static String USER_PREF = "User";
    final static String PWD_PREF = "Password";
    
    private String  connectionString;
    private boolean connected = false;
    
    private Connection updateMatchConnection = null;
    private Connection updateResultConnection = null;
    
    private Map<String, PreparedStatement> stmtMap = new java.util.HashMap<>();
    
    
    public TTM() {
        countermanager.prefs.Properties prefs = getProperties();
        
        String dbName = prefs.getString(DATABASE_PREF, "");
        String server = prefs.getString(SERVER_PREF, "(local)");
        boolean windowsAuth = prefs.getBoolean(WINDOWS_AUTH_PREF, true);
        String user = prefs.getString(USER_PREF, null);
        String pwd = prefs.getString(PWD_PREF, null);
        
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:sqlserver://");
        if (server.equalsIgnoreCase("(local)"))
            sb.append("localhost");
        else
            sb.append(server);
        sb.append(";");
        
        String[] database = dbName.split("\\\\");
        sb.append("databaseName=").append(database[0]).append(";");
        if (database.length > 1)
            sb.append("instanceName=").append(database[1]).append(";");
        
        if (windowsAuth)
            sb.append("integratedSecurity=true;trustServerCertificate=true;encrypt=true;");
        else
            sb.append("user=").append(user).append(";").append("password=").append(pwd).append(";").append("integratedSecurity=false;trustServerCertificate=true;encrypt=true;");
        
        connectionString = sb.toString();
    }
    
    
    public TTM(String connectionString) {
        String[] parts = connectionString.split(";");
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (String part : parts) {
            String[] tmp = part.split("=");
            map.put(tmp[0].toLowerCase(), tmp.length > 1 ? tmp[1] : "");
        }
        
        String database[] = map.get("database").split("\\\\");
        
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:sqlserver://");
        if (!map.containsKey("server"))
            buffer.append("localhost");
        else if (map.get("server").equals("(local)"))
            buffer.append("localhost");
        else
            buffer.append(map.get("server"));
        buffer.append(";");
        
        buffer.append("databaseName=").append(database[0]).append(";");
        if (database.length > 1)
            buffer.append("instanceName=").append(database[1]).append(";");
        
        if (!map.containsKey("trusted_connection") || !map.get("trusted_connection").equalsIgnoreCase("yes"))
            buffer.append("user=").append(map.get("uid")).append(";").append("password=").append(map.get("pwd")).append(";").append("integratedSecurity=false;trustServerCertificate=true;encrypt=true;");
        else
            buffer.append("integratedSecurity=true;trustServerCertificate=true;encrypt=true;");
        
        this.connectionString = buffer.toString();
    }
    
    @Override
    public String getConnectionString() {
        return connectionString;        
    }

    @Override
    public boolean connect() {
        connected = true;
        return true;
    }

    @Override
    public void disconnect() {
        try {
            if (updateMatchConnection != null)
                updateMatchConnection.close();                    
        } catch (SQLException e) {
            
        }
        
        updateMatchConnection = null;

        try {
            if (updateResultConnection != null)
                updateResultConnection.close();                    
        } catch (SQLException e) {
            
        }
        
        updateResultConnection = null;
        
        connected = false;
    }     
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public boolean testConnection(String connectString) {
        try (Connection conn = getConnection(connectString, true)) {
            return testConnection(conn);
        } catch (Exception ex) {
            
        }
        
        return false;
    }

    private final Object syncUpdateMatch = new Object();
    
    @Override
    public long getMaxMtTimestamp() {
        long ret = 0;
        
        final String sql = 
                "SELECT MAX(mtTimeStamp) FROM MtList"
        ;
        
        synchronized (syncUpdateMatch) {
            if (!testConnection(updateMatchConnection)) {
                updateMatchConnection = getConnection(connectionString, true);
            }
                
            try (java.sql.Statement stmt = updateMatchConnection.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery(sql)) {

                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    ret = ts.getTime();
                }
            } catch (Throwable t) {
                t.printStackTrace();                
                try {
                    updateMatchConnection.close();
                } catch (java.sql.SQLException ex) {

                }

                updateMatchConnection = null;

                ret = 0;
            }
        }
     
        return ret;
    }
    
    @Override
    public List<LocalDate> getChangedDates(long timestamp) {
        List<LocalDate> list = new java.util.ArrayList<>();
        
        final String sql = 
                "SELECT DISTINCT CAST(mtDateTime AS DATE) AS date " + 
                "  FROM mtList mt INNER JOIN GrList gr ON mt.grID = gr.grID " +
                " WHERE gr.grPublished = 1 AND mtDateTime IS NOT NULL AND mtTimestamp > ? " +
                " ORDER BY date ASC "
        ;
        
        synchronized (syncUpdateMatch) {
            if (!testConnection(updateMatchConnection)) {
                updateMatchConnection = getConnection(connectionString, true);
            }
                
            try (java.sql.PreparedStatement stmt = updateMatchConnection.prepareStatement(sql)) {

                stmt.setDate(1, new java.sql.Date(timestamp));
                try (java.sql.ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(rs.getDate(1).toLocalDate());   
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();                
                try {
                    updateMatchConnection.close();
                } catch (java.sql.SQLException ex) {

                }

                updateMatchConnection = null;

                list.clear();
            }
        }
                
        return list;
    }
    
    
    @Override
    public List<CounterModelMatch> update(int fromTable, int toTable, java.time.LocalDate date, boolean all) {
        String sql =             
            // Singles
            "SELECT cp.cpType, cp.cpName, cp.cpDesc, cp.cpSex, " +
            "       gr.grName, gr.grDesc, gr.grStage, gr.grModus, gr.grSize, " +
            "       gr.grWinner, gr.grNofRounds, gr.grNofMatches, " +
            "       mtTimeStamp, mtNr, 0 AS mtMS, mtRound, mtMatch, " +
            "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, 1 AS nmType, " +
            "       up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
            "       up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
            "       mt.mtResA, mt.mtResX, " +
            "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, plAnaRegion, " +
            "       NULL AS plBplNr, NULL AS plBplExtId, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBnaRegion, " +
            "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, plXnaRegion, " +
            "       NULL AS plYplNr, NULL AS plYplExtId, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYnaRegion, " +
            "       NULL AS tmAtmName, NULL AS tmAtmDesc, NULL AS tmAnaName, NULL AS tmAnaRegion, " +
            "       NULL AS tmXtmName, NULL AS tmXtmDesc, NULL AS tmXnaName, NULL As tmXnaRegion, " +
            "       NULL AS mttmResA, NULL AS mttmResX, " +
            "       mtSet1.mtResA, mtSet1.mtResX, " +
            "       mtSet2.mtResA, mtSet2.mtResX, " +
            "       mtSet3.mtResA, mtSet3.mtResX, " +
            "       mtSet4.mtResA, mtSet4.mtResX, " +
            "       mtSet5.mtResA, mtSet5.mtResX, " +
            "       mtSet6.mtResA, mtSet6.mtResX, " +
            "       mtSet7.mtResA, mtSet7.mtResX, " +
            "       mtSet8.mtResA, mtSet8.mtResX, " +
            "       mtSet9.mtResA, mtSet9.mtResX  " +
            "  FROM MtSingleList mt " +
            "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
            "       LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
            "       LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
            "       LEFT OUTER JOIN MtSet mtSet1 ON mt.mtID = mtSet1.mtID AND mtSet1.mtSet = 1 " +
            "       LEFT OUTER JOIN MtSet mtSet2 ON mt.mtID = mtSet2.mtID AND mtSet2.mtSet = 2 " +
            "       LEFT OUTER JOIN MtSet mtSet3 ON mt.mtID = mtSet3.mtID AND mtSet3.mtSet = 3 " +
            "       LEFT OUTER JOIN MtSet mtSet4 ON mt.mtID = mtSet4.mtID AND mtSet4.mtSet = 4 " +
            "       LEFT OUTER JOIN MtSet mtSet5 ON mt.mtID = mtSet5.mtID AND mtSet5.mtSet = 5 " +
            "       LEFT OUTER JOIN MtSet mtSet6 ON mt.mtID = mtSet6.mtID AND mtSet6.mtSet = 6 " +
            "       LEFT OUTER JOIN MtSet mtSet7 ON mt.mtID = mtSet7.mtID AND mtSet7.mtSet = 7 " +
            "       LEFT OUTER JOIN MtSet mtSet8 ON mt.mtID = mtSet8.mtID AND mtSet8.mtSet = 8 " +
            "       LEFT OUTER JOIN MtSet mtSet9 ON mt.mtID = mtSet9.mtID AND mtSet9.mtSet = 9 " +
            " WHERE gr.grPublished = 1 AND " +
            "       CAST(mtDateTime AS DATE) = ? AND " +
            "       (mt.stA IS NULL OR mt.tmAtmID IS NOT NULL) AND " +
            "       (mt.stX IS NULL OR mt.tmXtmID IS NOT NULL) AND " +
            // "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
            // If all == false then exclude finished matches
            (all ? "" : (
            "       2 * mt.mtResA < mtBestOf AND 2 * mt.mtResX < mtBestOf AND " +
            "       mt.mtWalkOverA = 0 AND mt.mtWalkOverX = 0 AND "
            )) +
            "       cp.cpType = 1 AND mtTable >= ? AND mtTable <= ? " +

            // Doubles and Mixed
            "UNION " +                        
            "SELECT cp.cpType, cp.cpName, cp.cpDesc, cp.cpSex, grName, " +
            "       gr.grDesc, gr.grStage, gr.grModus, gr.grSize, " +
            "       gr.grWinner, gr.grNofRounds, gr.grNofMatches, " +
            "       mtTimeStamp, mtNr, 0 AS mtMS, mtRound, mtMatch, " +
            "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, 2 AS nmType, " +
            "       up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
            "       up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
            "       mt.mtResA, mt.mtResX, " +
            "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName,plAnaRegion,  " +
            "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, plBnaRegion, " +
            "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, plXnaRegion, " +
            "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, plAnaRegion, " +
            "       NULL AS tmAtmName, NULL AS tmAtmDesc, NULL AS tmAnaName, NULL AS tmAnaRegion, " +
            "       NULL AS tmXtmName, NULL AS tmXtmDesc, NULL AS tmXnaName, NULL AS tmXnaRegion, " +
            "       NULL AS mttmResA, NULL AS mttmResX, " +
            "       mtSet1.mtResA, mtSet1.mtResX, " +
            "       mtSet2.mtResA, mtSet2.mtResX, " +
            "       mtSet3.mtResA, mtSet3.mtResX, " +
            "       mtSet4.mtResA, mtSet4.mtResX, " +
            "       mtSet5.mtResA, mtSet5.mtResX, " +
            "       mtSet6.mtResA, mtSet6.mtResX, " +
            "       mtSet7.mtResA, mtSet7.mtResX, " +
            "       mtSet8.mtResA, mtSet8.mtResX, " +
            "       mtSet9.mtResA, mtSet9.mtResX  " +
            "  FROM MtDoubleList mt " +
            "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
            "       LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
            "       LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
            "       LEFT OUTER JOIN MtSet mtSet1 ON mt.mtID = mtSet1.mtID AND mtSet1.mtSet = 1 " +
            "       LEFT OUTER JOIN MtSet mtSet2 ON mt.mtID = mtSet2.mtID AND mtSet2.mtSet = 2 " +
            "       LEFT OUTER JOIN MtSet mtSet3 ON mt.mtID = mtSet3.mtID AND mtSet3.mtSet = 3 " +
            "       LEFT OUTER JOIN MtSet mtSet4 ON mt.mtID = mtSet4.mtID AND mtSet4.mtSet = 4 " +
            "       LEFT OUTER JOIN MtSet mtSet5 ON mt.mtID = mtSet5.mtID AND mtSet5.mtSet = 5 " +
            "       LEFT OUTER JOIN MtSet mtSet6 ON mt.mtID = mtSet6.mtID AND mtSet6.mtSet = 6 " +
            "       LEFT OUTER JOIN MtSet mtSet7 ON mt.mtID = mtSet7.mtID AND mtSet7.mtSet = 7 " +
            "       LEFT OUTER JOIN MtSet mtSet8 ON mt.mtID = mtSet8.mtID AND mtSet8.mtSet = 8 " +
            "       LEFT OUTER JOIN MtSet mtSet9 ON mt.mtID = mtSet9.mtID AND mtSet9.mtSet = 9 " +
            " WHERE gr.grPublished = 1 AND " +
            "       CAST(mtDateTime AS DATE) = ? AND " +
            "       (mt.stA IS NULL OR mt.tmAtmID IS NOT NULL) AND " +
            "       (mt.stX IS NULL OR mt.tmXtmID IS NOT NULL) AND " +
            // "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
            // If all == false then exclude finished matches
            (all ? "" : (
            "       2 * mt.mtResA < mtBestOf AND 2 * mt.mtResX < mtBestOf AND " +
            "       mt.mtWalkOverA = 0 AND mt.mtWalkOverX = 0 AND "
            )) +
            "       (cp.cpType = 2 OR cp.cpType = 3) AND mtTable >= ? AND mtTable <= ? " +

            // Team (Individual)
            "UNION " + 
            "SELECT cp.cpType, cp.cpName, cp.cpDesc, cp.cpSex, gr.grName, " +
            "       gr.grDesc, gr.grStage, gr.grModus, gr.grSize, " +
            "       gr.grWinner, gr.grNofRounds, gr.grNofMatches, " +
            "       mt.mtTimeStamp, mt.mtNr, mt.mtMS AS mtMS, mt.mtRound, mt.mtMatch, " +
            "       mt.mtTable, mt.mtDateTime, mt.mtBestOf, mt.mtMatches, mt.mtReverse, nmType, " +
            "       up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
            "       up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
            "       mt.mtResA, mt.mtResX, " +
            "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, plAnaRegion, " +
            "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, plBnaRegion, " +
            "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, plXnaRegion, " +
            "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, plYnaRegion, " +
            "       stA.tmName \"tmAtmName\", stA.tmDesc \"tmAtmDesc\", stA.naName \"tmAnaName\", stA.naRegion \"tmAnaRegion\", " +
            "       stX.tmName \"tmXtmName\", stX.tmDesc \"tmXtmDesc\", stX.naName \"tmXnaName\", stX.naRegion \"tmXnaRegion\", " +
            "       mt.mttmResA \"mttmResA\", mt.mttmResX \"mttmResX\", " +
            "       mtSet1.mtResA, mtSet1.mtResX, " +
            "       mtSet2.mtResA, mtSet2.mtResX, " +
            "       mtSet3.mtResA, mtSet3.mtResX, " +
            "       mtSet4.mtResA, mtSet4.mtResX, " +
            "       mtSet5.mtResA, mtSet5.mtResX, " +
            "       mtSet6.mtResA, mtSet6.mtResX, " +
            "       mtSet7.mtResA, mtSet7.mtResX, " +
            "       mtSet8.mtResA, mtSet8.mtResX, " +
            "       mtSet9.mtResA, mtSet9.mtResX  " +
            "  FROM MtIndividualList mt " +
            "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "       INNER JOIN SyList sy ON gr.syID = sy.syID " +
            "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
            "       LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
            "       LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
            "       INNER JOIN StTeamList stA ON mt.stA = stA.stID " +
            "       INNER JOIN StTeamList stX ON mt.stX = stX.stID " +
            "       LEFT OUTER JOIN MtSet mtSet1 ON mt.mtID = mtSet1.mtID AND mtSet1.mtSet = 1 AND mtSet1.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet2 ON mt.mtID = mtSet2.mtID AND mtSet2.mtSet = 2 AND mtSet2.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet3 ON mt.mtID = mtSet3.mtID AND mtSet3.mtSet = 3 AND mtSet3.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet4 ON mt.mtID = mtSet4.mtID AND mtSet4.mtSet = 4 AND mtSet4.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet5 ON mt.mtID = mtSet5.mtID AND mtSet5.mtSet = 5 AND mtSet5.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet6 ON mt.mtID = mtSet6.mtID AND mtSet6.mtSet = 6 AND mtSet6.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet7 ON mt.mtID = mtSet7.mtID AND mtSet7.mtSet = 7 AND mtSet7.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet8 ON mt.mtID = mtSet8.mtID AND mtSet8.mtSet = 8 AND mtSet8.mtMS = mt.mtMS " +
            "       LEFT OUTER JOIN MtSet mtSet9 ON mt.mtID = mtSet9.mtID AND mtSet9.mtSet = 9 AND mtSet9.mtMS = mt.mtMS " +
            " WHERE gr.grPublished = 1 AND " +
            "       CAST(mt.mtDateTime AS DATE) = ? AND " +
            "       (mt.stA IS NULL OR mt.tmAtmID IS NOT NULL) AND " +
            "       (mt.stX IS NULL OR mt.tmXtmID IS NOT NULL) AND " +
            // "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
            "       mt.stA IS NOT NULL AND mt.stX IS NOT NULL AND " +
            // If all == false then exclude finished matches
            (all ? "" : (
            "       (mt.mtResA IS NULL OR 2 * mt.mtResA < mt.mtBestOf) AND " + 
            "       (mt.mtResX IS NULL OR 2 * mt.mtResX < mt.mtBestOf) AND " +
            "       (mt.mtWalkOverA IS NULL OR mt.mtWalkOverA = 0) AND " +
            "       (mt.mtWalkOverX IS NULL OR mt.mtWalkOverX = 0) AND " +
            "       (((sy.syComplete = 0 OR gr.grModus <> 1) AND 2 * mttmResA < mt.mtMatches AND 2 * mttmResX < mt.mtMatches) OR " +
            "        ((sy.syComplete = 1 AND gr.grModus = 1) AND (mt.mttmResA + mt.mttmResX) < mt.mtMatches)) AND "
            )) +
            "       cp.cpType = 4 AND mt.mtTable >= ? AND mt.mtTable <= ? " +

            // Order: By Table, Time, Individual.
            // Also for Round and Match-in-Round for RR-groups with all matches 
            // at the same time and table.
            " ORDER BY mtTable, mtDateTime, mtNr, mtMS, mtRound, mtMatch";
        
        List<CounterModelMatch> list = new java.util.ArrayList<>();
        
        try {
            synchronized(syncUpdateMatch) {
                if (!testConnection(updateMatchConnection)) {
                    stmtMap.clear();                    
                    updateMatchConnection = getConnection(connectionString, true);
                }
                
                if (!stmtMap.containsKey(sql))
                    stmtMap.put(sql, updateMatchConnection.prepareStatement(sql));
                
                PreparedStatement updateMatchStmt = stmtMap.get(sql);
        
                int par = 0;
                
                updateMatchStmt.setObject(++par, date);
                updateMatchStmt.setInt(++par, fromTable);
                updateMatchStmt.setInt(++par, toTable);
                updateMatchStmt.setObject(++par, date);
                updateMatchStmt.setInt(++par, fromTable);
                updateMatchStmt.setInt(++par, toTable);
                updateMatchStmt.setObject(++par, date);
                updateMatchStmt.setInt(++par, fromTable);
                updateMatchStmt.setInt(++par, toTable);

                try (java.sql.ResultSet result = updateMatchStmt.executeQuery()) {                    
                    while (result.next()) {
                        int idx = 1;
                        
                        int     cpType      = result.getInt(idx++);
                        String  cpName      = getString(result, idx++);
                        String  cpDesc      = getString(result, idx++);
                        int     cpSex       = result.getInt(idx++);
                        String  grName      = getString(result, idx++);
                        String  grDesc      = getString(result, idx++);
                        String  grStage     = getString(result, idx++);
                        int     grModus     = result.getInt(idx++);
                        int     grSize      = result.getInt(idx++);
                        int     grWinner    = result.getInt(idx++);
                        int     grNofRounds = result.getInt(idx++);
                        int     grNofMatches = result.getInt(idx++);
                        long    mtTimeStamp = getTime(result, idx++);
                        int     mtNr        = result.getInt(idx++);
                        int     mtMS        = result.getInt(idx++);
                        int     mtRound     = result.getInt(idx++);
                        int     mtMatch     = result.getInt(idx++);
                        int     mtTable     = result.getInt(idx++);
                        long    mtDateTime  = getTime(result, idx++);
                        int     mtBestOf    = result.getInt(idx++);
                        int     mtMatches   = result.getInt(idx++);
                        boolean mtReverse   = result.getBoolean(idx++);
                        int     nmType      = result.getInt(idx++);
                        
                        int     up1upNr = result.getInt(idx++);
                        String  uppsLast = getString(result, idx++);
                        String  up1psFirst = getString(result, idx++);
                        String  up1naName = getString(result, idx++);
                        String  up1naDesc = getString(result, idx++);
                        String  up1naRegion = getString(result, idx++);
                        
                        int     up2upNr = result.getInt(idx++);
                        String  up2psLast = getString(result, idx++);
                        String  up2psFirst = getString(result, idx++);
                        String  up2naName = getString(result, idx++);
                        String  up2naDesc = getString(result, idx++);
                        String  up2naRegion = getString(result, idx++);
                        
                        int     mtResA      = result.getInt(idx++);
                        int     mtResX      = result.getInt(idx++);
                        
                        int     plAplNr     = result.getInt(idx++);
                        String  plAplExtId  = getString(result, idx++);
                        String  plApsLast   = getString(result, idx++);
                        String  plApsFirst  = getString(result, idx++);
                        String  plAnaName   = getString(result, idx++);
                        String  plAnaRegion = getString(result, idx++);
                        
                        int     plBplNr     = result.getInt(idx++);
                        String  plBplExtId  = getString(result, idx++);
                        String  plBpsLast   = getString(result, idx++);
                        String  plBpsFirst  = getString(result, idx++);
                        String  plBnaName   = getString(result, idx++);
                        String  plBnaRegion = getString(result, idx++);
                        
                        int     plXplNr     = result.getInt(idx++);
                        String  plXplExtId  = getString(result, idx++);
                        String  plXpsLast   = getString(result, idx++);
                        String  plXpsFirst  = getString(result, idx++);
                        String  plXnaName   = getString(result, idx++);
                        String  plXnaRegion = getString(result, idx++);
                        
                        int     plYplNr     = result.getInt(idx++);
                        String  plYplExtId  = getString(result, idx++);
                        String  plYpsLast   = getString(result, idx++);
                        String  plYpsFirst  = getString(result, idx++);
                        String  plYnaName   = getString(result, idx++);
                        String  plYnaRegion = getString(result, idx++);

                        String  tmAtmName   = getString(result, idx++);
                        String  tmAtmDesc   = getString(result, idx++);
                        String  tmAnaName   = getString(result, idx++);
                        String  tmAnaRegion = getString(result, idx++);
                        
                        String  tmXtmName   = getString(result, idx++);
                        String  tmXtmDesc   = getString(result, idx++);
                        String  tmXnaName   = getString(result, idx++);
                        String  tmXnaRegion = getString(result, idx++);
                        
                        int     mttmResA    = result.getInt(idx++);
                        int     mttmResX    = result.getInt(idx++);
                        int     mtResult[][] = new int[mtBestOf][2];
                        
                        for (int i = 0; i < mtBestOf && i < 9; i++) {
                            mtResult[i][0] = result.getShort(idx++);
                            mtResult[i][1] = result.getShort(idx++);
                        }
                        
                        CounterModelMatch match = new CounterModelMatch();

                        match.cpType      = cpType;
                        match.cpName      = cpName;
                        match.cpSex       = cpSex;
                        match.cpDesc      = cpDesc;
                        match.grName      = grName;
                        match.grDesc      = grDesc;
                        match.grStage     = grStage;
                        match.grModus     = grModus;
                        match.grSize      = grSize;
                        match.grWinner    = grWinner;
                        match.grNofRounds = grNofRounds;
                        match.grNofMatches = grNofMatches;
                        match.mtTimestamp = mtTimeStamp;
                        match.mtNr        = mtNr;  // wg. GTS:  % 10000
                        match.mtMS        = mtMS;
                        match.mtRound     = mtRound;
                        match.mtMatch     = mtMatch;
                        match.mtTable     = mtTable;
                        match.mtDateTime  = mtDateTime;
                        match.mtBestOf    = mtBestOf;
                        match.mtMatches   = mtMatches;
                        match.mtReverse   = mtReverse;
                        match.nmType      = nmType;
                        
                        match.up1.upNr     = up1upNr;
                        match.up1.psLast   = uppsLast;
                        match.up1.psFirst  = up1psFirst;
                        match.up1.naName   = up1naName;
                        match.up1.naDesc   = up1naDesc;
                        match.up1.naRegion = up1naRegion;
                        
                        match.up2.upNr     = up2upNr;
                        match.up2.psLast   = up2psLast;
                        match.up2.psFirst  = up2psFirst;
                        match.up2.naName   = up2naName;
                        match.up2.naDesc   = up2naDesc;
                        match.up2.naRegion = up2naRegion;

                        match.mtResA      = mtResA;
                        match.mtResX      = mtResX;

                        match.plA.plNr    = plAplNr % 10000;
                        match.plA.plExtID = plAplExtId;
                        match.plA.psLast  = plApsLast;
                        match.plA.psFirst = plApsFirst;
                        match.plA.naName  = plAnaName;
                        match.plA.naRegion = plAnaRegion;

                        match.plB.plNr    = plBplNr % 10000;
                        match.plB.plExtID = plBplExtId;
                        match.plB.psLast  = plBpsLast;
                        match.plB.psFirst = plBpsFirst;
                        match.plB.naName  = plBnaName;
                        match.plB.naRegion = plBnaRegion;

                        match.plX.plNr    = plXplNr % 10000;
                        match.plX.plExtID = plXplExtId;
                        match.plX.psLast  = plXpsLast;
                        match.plX.psFirst = plXpsFirst;
                        match.plX.naName  = plXnaName;
                        match.plX.naRegion = plXnaRegion;

                        match.plY.plNr    = plYplNr % 10000;
                        match.plY.plExtID = plYplExtId;
                        match.plY.psLast  = plYpsLast;
                        match.plY.psFirst = plYpsFirst;
                        match.plY.naName  = plYnaName;
                        match.plY.naRegion = plYnaRegion;

                        match.tmA.tmName   = tmAtmName;
                        match.tmA.tmDesc   = tmAtmDesc;
                        match.tmA.naName   = tmAnaName;
                        match.tmA.naRegion = tmAnaRegion;
                        
                        match.tmX.tmName   = tmXtmName;
                        match.tmX.tmDesc   = tmXtmDesc;
                        match.tmX.naName   = tmXnaName;
                        match.tmX.naRegion = tmXnaRegion;
                        
                        match.mttmResA    = mttmResA;
                        match.mttmResX    = mttmResX;

                        match.mtResult    = mtResult;
                        
                        list.add(match);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();                
            try {
                updateMatchConnection.close();
            } catch (java.sql.SQLException ex) {
                
            }
            
            updateMatchConnection = null;
            
            list.clear();
        }
        
        return list;
    }
 
    @Override
    public boolean updateResult(int mtNr, int mtMS, int[][] mtSets, int mtWalkOverA, int mtWalkOverX) {
        if (!testConnection(updateResultConnection))
            updateResultConnection = getConnection(connectionString, false);
        
        try (java.sql.Statement statement = updateResultConnection.createStatement()) {
            String  tmp = new String();
            
            for (int i = 0; i < mtSets.length; i++) {
                // Don't localize
                tmp += String.format(new java.util.Locale("en_US"), "%02d%02d", mtSets[i][0], mtSets[i][1]);
            }
            
            statement.executeUpdate(
                    "mtSetResultProc " + mtNr + ", " + mtMS + ", " + mtSets.length + ", " +
                    "'" + tmp + "'" + ", " + mtWalkOverA + ", " + mtWalkOverX);
            
            updateResultConnection.commit();
        } catch (Throwable t) {
            t.printStackTrace();

            try {                
                updateResultConnection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            try {
                updateResultConnection.close();                
            } catch (SQLException ex) {
                
            }

            updateResultConnection = null;
        }
        
        return true;
    }
    
    @Override
    public List<CounterModelMatch> getCurrentTeamMatches(int fromTable, int toTable) {
        return new java.util.ArrayList<>();
    }
    
    
    @Override
    public IDatabaseSettings getSettingsPanel() {
        return new countermanager.model.database.ttm.TTMSettings(this);
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
    
    
    private boolean testConnection(Connection conn) {
        return conn != null;
    }
    
    
    private Connection getConnection(String connectString, boolean autocommit) {
        Connection databaseConnection = null;
        
        try {
            if ( (databaseConnection = java.sql.DriverManager.getConnection(connectString)) == null )
                return null;
            
            databaseConnection.setAutoCommit(autocommit);
            
            return databaseConnection;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (databaseConnection != null)
                    databaseConnection.close();
            } catch (Throwable t) {
                
            }
        }

        return null;
    }

    private static String getString(java.sql.ResultSet rs, int idx)  throws SQLException {
        return rs.getString(idx);
    }
    
    
    private static long getTime(java.sql.ResultSet res, int idx) throws SQLException {
        Date date = res.getTimestamp(idx);
        return date == null ? 0 : date.getTime();
    }
    
    
    private static Player getPlayer(java.sql.ResultSet res, int idx) throws SQLException {
        Player pl = new Player();
        
        pl.plNr = res.getInt(++idx) % 10000;
        pl.psLast = getString(res, ++idx);
        pl.psFirst = getString(res, ++idx);
        pl.naName = getString(res, ++idx);
        pl.naDesc = getString(res, ++idx);
        pl.naRegion = getString(res, ++idx);
        pl.plExtID = getString(res, ++idx);
     
        return pl;
    }
    
    
    private static Team getTeam(java.sql.ResultSet res, int idx) throws SQLException {
        Team tm = new Team();

        tm.tmName = getString(res, ++idx);
        tm.tmDesc = getString(res, ++idx);
        tm.naName = getString(res, ++idx);
        tm.naDesc = getString(res, ++idx);
        tm.naRegion = getString(res, ++idx);
        
        return tm;
    }
    
    
    private static Umpire getUmpire(java.sql.ResultSet res, int idx) throws SQLException {
        Umpire up = new Umpire();
        
        up.upNr = res.getInt(++idx);
        up.psLast = getString(res, ++idx);
        up.psFirst = getString(res, ++idx);
        up.naName = getString(res, ++idx);
        up.naDesc = getString(res, +idx);
        up.naRegion = getString(res, ++idx);
                        
        return up;
    }
    
    
    private static int[][] getResult(java.sql.ResultSet res, int idx) throws SQLException {
        int[][] games = new int[7][2];
        
        for (int i = 0; i < games.length; i++) {
            games[i][0] = res.getInt(++idx);
            games[i][1] = res.getInt(++idx);
        }
        
        return games;
    }
    
    
    // Methods below are accessed from scripts

    @Override
    public Player[] listPlayers(String naName) {
        var sql = "SELECT plNr, psLast, psFirst, naName, naDesc, naRegion, plExtID FROM PlList";

        if (naName != null)
          sql += " WHERE naName = ?";

        sql += " ORDER BY plNr";

        List<Player> list = new java.util.ArrayList<>();
        
        try {
            synchronized(syncUpdateMatch) {
                if (!testConnection(updateMatchConnection)) {
                    stmtMap.clear();                    
                    updateMatchConnection = getConnection(connectionString, true);
                }
                
                if (!stmtMap.containsKey(sql))
                    stmtMap.put(sql, updateMatchConnection.prepareStatement(sql));
                
                PreparedStatement updateMatchStmt = stmtMap.get(sql);
        
                int par = 0;

                if (naName != null)
                    updateMatchStmt.setString(++par, naName);

                try (java.sql.ResultSet result = updateMatchStmt.executeQuery()) {  
                    while (result.next()) {
                        int idx = 0;
                        Player pl = getPlayer(result, idx);

                        list.add(pl);
                    }
                }    
            }
        } catch (Throwable t) {
            t.printStackTrace();                
            try {
                updateMatchConnection.close();
            } catch (java.sql.SQLException ex) {
                
            }
            
            updateMatchConnection = null;
            
            list.clear();
        }
        
        return list.toArray(new Player[0]);
    }

    @Override
    public Match[] listMatches(
            long mtTimestamp, java.time.LocalDateTime from, java.time.LocalDateTime to, int fromTable, int toTable, 
            boolean individual, boolean notStarted, boolean notFinished) {
        List<Match> list = new java.util.ArrayList<>();
        
        String sql;
        
        String where = 
            " AND mtTimestamp > ? " +
            " AND mtDateTime >= ? " +
            " AND mtDateTime <= ? " +
            " AND mtTable >= ? " +
            " AND mtTable <= ? ";
        
        String whereTeam = where;
        
        if (notStarted) {
            where += " AND ISNULL(mtSet1.mtResA, 0) = 0 AND ISNULL(mtSet1.mtResX, 0) = 0 ";
            whereTeam += " AND ISNULL(mt.mtResA, 0) = 0 AND ISNULL(mt.mtResX, 0) = 0 ";
        }
        
        if (notFinished) {
            where += " AND 2 * ISNULL(mt.mtResA, 0) < mt.mtBestOf AND 2 * ISNULL(mt.mtResX, 0) < mt.mtBestOf ";
            whereTeam += " AND 2 * ISNULL(mt.mtResA , 0)< mtMatches AND 2 * ISNULL(mt.mtResX,0) < mtMatches ";
        }
            

        sql =
            "SELECT cp.cpName, cp.cpDesc, cp.cpType, gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, gr.grNofRounds, gr.grNofMatches, NULL AS syComplete, " +
            "   mtRound, mtMatch, 1 AS mtMatches, mtBestOf, mtNr, 0 AS mtMS, mtWalkOverA, mtWalkOverX, mt.mtResA, mt.mtResX, 0 AS mtReverse, " +
            "   plAplNr, plApsLast, plApsFirst, plAnaName, plAnaDesc, plAnaRegion, plAplExtID, " +
            "   NULL AS plBplNr, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBnaDesc, NULL AS plBnaRegion, NULL AS plBplExtID, " +
            "   plXplNr, plXpsLast, plXpsFirst, plXnaName, plXnaDesc, plXnaRegion, plXplExtID, " +
            "   NULL AS plYplNr, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYnaDesc, NULL AS plYnaRegion, NULL AS plYplExtID, " +
            "   NULL AS tmAtmName, NULL AS tmAtmDesc, NULL AS tmAnaName, NULL AS tmAnaDesc, NULL AS tmAnaRegion, " +
            "   NULL AS tmXtmName, NULL AS tmXtmDesc, NULL AS tmXnaName, NULL AS tmXnaDesc, NULL AS tmXnaRegion, " +
            "   NULL AS mttmResA, NULL AS mttmResX, " +
            "   mtDateTime, mtTable, mtTimeStamp, " +
            "   up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
            "   up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
            "   mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
            "   mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
            "   mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
            "   mtSet7.mtResA, mtSet7.mtResX " +
            "  FROM MtSingleList mt " +
            "    INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "    INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cp.cpType = 1 " +
            "    LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
            "    LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
            "    LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 " +
            "    LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 " +
            "    LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 " +
            "    LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 " +
            "    LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 " +
            "    LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 " +
            "    LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 " +
            " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
            "       AND plAplNr IS NOT NULL AND plXplNR IS NOT NULL " +
            where
        ;
        
        sql += " UNION " +
            "SELECT cp.cpName, cp.cpDesc, cp.cpType, gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, gr.grNofRounds, gr.grNofMatches, NULL AS syComplete, " +
            "   mtRound, mtMatch, 1 AS mtMatches, mtBestOf, mtNr, 0 AS mtMS, mtWalkOverA, mtWalkOverX, mt.mtResA, mt.mtResX, 0 AS mtReverse, " +
            "   plAplNr, plApsLast, plApsFirst, plAnaName, plAnaDesc, plAnaRegion, plAplExtID, " +
            "   plBplNr, plBpsLast, plBpsFirst, plBnaName, plBnaDesc, plBnaRegion, plBplExtID, " +
            "   plXplNr, plXpsLast, plXpsFirst, plXnaName, plXnaDesc, plXnaRegion, plXplExtID, " +
            "   plYplNr, plYpsLast, plYpsFirst, plYnaName, plYnaDesc, plYnaRegion, plYplExtID, " +
            "   NULL AS tmAtmName, NULL AS tmAtmDesc, NULL AS tmAnaName, NULL AS tmAnaDesc, NULL AS tmAnaRegion, " +
            "   NULL AS tmXtmName, NULL AS tmXtmDesc, NULL AS tmXnaName, NULL AS tmXnaDesc, NULL AS tmXnaRegion, " +
            "   NULL AS mttmResA, NULL AS mttmResX, " +
            "   mtDateTime, mtTable, mtTimeStamp, " +
            "   up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
            "   up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
            "   mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
            "   mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
            "   mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
            "   mtSet7.mtResA, mtSet7.mtResX " +
            "  FROM MtDoubleList mt " +
            "    INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "    INNER JOIN CpList cp ON gr.cpID = cp.cpID AND (cp.cpType = 2 OR cp.cpType = 3) " +
            "    LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
            "    LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
            "    LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 " +
            "    LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 " +
            "    LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 " +
            "    LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 " +
            "    LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 " +
            "    LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 " +
            "    LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 " +
            " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
            "       AND plAplNr IS NOT NULL AND plXplNR IS NOT NULL " +
            where
        ;
        
        if (!individual)
            sql += " UNION " +
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, gr.grNofRounds, gr.grNofMatches, sy.syComplete, " +
                "   mtRound, mtMatch, mtMatches, mtBestOf, mtNr, 0 AS mtMS, mtWalkOverA, mtWalkOverX, mt.mtResA, mt.mtResX, mtReverse, " +
                "   NULL AS plAplNr, NULL AS plApsLast, NULL AS plApsFirst, NULL AS plAnaName, NULL AS plAnaDesc, NULL AS plAnaRegion, NULL AS plAplExtID, " +
                "   NULL AS plBplNr, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBnaDesc, NULL AS plBnaRegion, NULL AS plBplExtID, " +
                "   NULL AS plXplNr, NULL AS plXpsLast, NULL AS plXpsFirst, NULL AS plXnaName, NULL AS plXnaDesc, NULL AS plXnaRegion, NULL AS plXplExtID, " +
                "   NULL AS plYplNr, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYnaDesc, NULL AS plYnaRegion, NULL AS plYplExtID, " +
                "   tmAtmName, tmAtmDesc, tmAnaName, tmAnaDesc, tmAnaRegion, " +
                "   tmXtmName, tmXtmDesc, tmXnaName, tmXnaDesc, tmXnaRegion, " +
                "   mt.mtResA AS mttmResA, mt.mtResX AS mttmResX, " +
                "   mtDateTime, mtTable, mtTimeStamp, " +
                "   up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
                "   up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
                "   NULL AS mtSet1mtResA, NULL AS mtSet1mtResX, NULL AS mtSet2mtResA, NULL AS mtSet2mtResX, " +
                "   NULL AS mtSet3mtResA, NULL AS mtSet3mtResX, NULL AS mtSet4mtResA, NULL AS mtSet4mtResX, " +
                "   NULL AS mtSet5mtResA, NULL AS mtSet5mtResX, NULL AS mtSet6mtResA, NULL AS mtSet6mtResX, " +
                "   NULL AS mtSet7mtResA, NULL AS mtSet7mtResX " +
                "  FROM MtTeamList mt" +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN SyLisg sy ON gr.syID = sy.syID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cp.cpType = 4" +
                "       LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
                "       LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
                " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
                "       AND tmAtmName IS NOT NULL AND tmXtmName IS NOT NULL " +
                whereTeam
            ;
        else 
            sql += " UNION " +
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, gr.grNofRounds, gr.grNofMatches, sy.syComplete, " +
                "   mtRound, mtMatch, mtMatches, mtBestOf, mtNr, mt.mtMS AS mtMS, mtWalkOverA, mtWalkOverX, mt.mtResA, mt.mtResX, mtReverse, " +
                "   plAplNr, plApsLast, plApsFirst, plAnaName, plAnaDesc, plAnaRegion, plAplExtID, " +
                "   plBplNr, plBpsLast, plBpsFirst, plBnaName, plBnaDesc, plBnaRegion, plBplExtID, " +
                "   plXplNr, plXpsLast, plXpsFirst, plXnaName, plXnaDesc, plXnaRegion, plXplExtID, " +
                "   plYplNr, plYpsLast, plYpsFirst, plYnaName, plYnaDesc, plYnaRegion, plYplExtID, " +
                "   tmA.tmName AS tmAtmName, tmA.tmDesc AS tmAtmDesc, tmA.naName AS tmAnaName, tmA.naDesc AS tmAnaDesc, tmA.naRegion AS tmAnaRegion, " +
                "   tmX.tmName AS tmXtmName, tmX.tmDesc AS tmXtmDesc, tmX.naName AS tmXnaName, tmX.naDesc AS tmXnaDesc, tmX.naRegion AS tmXnaRegion, " +
                "   mt.mttmResA, mt.mttmResX, " +
                "   mtDateTime, mtTable, mtTimeStamp, " +
                "   up1.upNr AS up1upnr, up1.psLast AS up1psLast, up1.psFirst AS up1psFirst, up1.naName AS up1naName, up1.naDesc AS up1naDesc, up1.naRegion AS up1naRegion, " +
                "   up2.upNr AS up2upnr, up2.psLast AS up2psLast, up2.psFirst AS up2psFirst, up2.naName AS up2naName, up2.naDesc AS up2naDesc, up2.naRegion AS up2naRegion, " +
                "   mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "   mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "   mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "   mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtIndividualList mt " +
                "    INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "    INNER JOIN SyList sy ON gr.syID = sy.syID " +
                "    INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cp.cpType = 4 " +
                "    LEFT OUTER JOIN TmTeamList tmA ON mt.tmAtmID = tmA.tmID " +
                "    LEFT OUTER JOIN TmTeamList tmX ON mt.tmXtmID = tmX.tmID " +
                "    LEFT OUTER JOIN UpList up1 ON mt.mtUmpire = up1.upNr " +
                "    LEFT OUTER JOIN UpList up2 ON mt.mtUmpire2 = up2.upNr " +
                "    LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 AND mtSet1.mtMS = mt.mtMS " +
                "    LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 AND mtSet2.mtMS = mt.mtMS " +
                "    LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 AND mtSet3.mtMS = mt.mtMS " +
                "    LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 AND mtSet4.mtMS = mt.mtMS " +
                "    LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 AND mtSet5.mtMS = mt.mtMS " +
                "    LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 AND mtSet6.mtMS = mt.mtMS " +
                "    LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 AND mtSet7.mtMS = mt.mtMS " +
                " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
                "       AND tmAtmID IS NOT NULL AND tmXtmID IS NOT NULL " +
                where + (notFinished ? 
                    " AND (((sy.syComplete = 0 OR gr.grModus <> 1) AND 2 * ISNULL(mt.mttmResA, 0) < mtMatches AND 2 * ISNULL(mt.mttmResX, 0) < mtMatches) " + 
                    "  OR  ((sy.syComplete = 1 AND gr.grModus = 1) AND (ISNULL(mt.mttmResA, 0) + ISNULL(mt.mttmResX, 0)) < mtMatches))  " : "")
            ;
        
        sql += " ORDER BY mtDateTime, mtTable, mtNr, mtMS ";
        
        try {
            synchronized(syncUpdateMatch) {
                if (!testConnection(updateMatchConnection)) {
                    stmtMap.clear();                    
                    updateMatchConnection = getConnection(connectionString, true);
                }
                
                if (!stmtMap.containsKey(sql))
                    stmtMap.put(sql, updateMatchConnection.prepareStatement(sql));
                
                PreparedStatement updateMatchStmt = stmtMap.get(sql);
        
                int par = 0;
                for(int i = 0; i < 3; i++) {
                    updateMatchStmt.setDate(++par, new java.sql.Date(mtTimestamp));
                    updateMatchStmt.setObject(++par, from);
                    updateMatchStmt.setObject(++par, to);
                    updateMatchStmt.setInt(++par, fromTable);
                    updateMatchStmt.setInt(++par, toTable);
                }

                try (java.sql.ResultSet result = updateMatchStmt.executeQuery()) {  
                    while (result.next()) {
                        int idx = 0;
                        Match mt = new Match();
                        mt.cpName = getString(result, ++idx);
                        mt.cpDesc = getString(result, ++idx);
                        mt.cpType = result.getInt(++idx);
                        mt.grName = getString(result, ++idx);
                        mt.grDesc = getString(result, ++idx);
                        mt.grModus = result.getInt(++idx);
                        mt.grSize = result.getInt(++idx);
                        mt.grWinner = result.getInt(++idx);
                        mt.grNofRounds = result.getInt(++idx);
                        mt.grNofMatches = result.getInt(++idx);
                        mt.syComplete = result.getBoolean(++idx); 
                        mt.mtRound = result.getInt(++idx);
                        mt.mtMatch = result.getInt(++idx);
                        mt.mtMatches = result.getInt(++idx);
                        mt.mtBestOf = result.getInt(++idx);
                        mt.mtNr = result.getInt(++idx);
                        mt.mtMS = result.getInt(++idx);
                        mt.mtWalkOverA = result.getBoolean(++idx);
                        mt.mtWalkOverX = result.getBoolean(++idx);
                        mt.mtResA = result.getInt(++idx);
                        mt.mtResX = result.getInt(++idx);
                        mt.mtReverse = result.getBoolean(++idx);

                        mt.plA = getPlayer(result, idx);
                        idx += 7;

                        mt.plB = getPlayer(result, idx);
                        idx += 7;

                        mt.plX = getPlayer(result, idx);
                        idx += 7;

                        mt.plY = getPlayer(result, idx);
                        idx += 7;

                        mt.tmA = getTeam(result, idx);
                        idx += 5;
                        
                        mt.tmX = getTeam(result, idx);
                        idx += 5;

                        mt.mttmResA = result.getInt(++idx);
                        mt.mttmResX = result.getInt(++idx);
                        
                        mt.mtDateTime = result.getTimestamp(++idx).getTime();
                        mt.mtTable = result.getInt(++idx);
                        mt.mtTimestamp = result.getTimestamp(++idx).getTime();
                        
                        mt.up1 = getUmpire(result, idx);
                        idx += 6;

                        mt.up2 = getUmpire(result, idx);
                        idx += 6;
                        
                        mt.mtResult = new int[mt.mtBestOf][2];
                        for (int i = 0; i < mt.mtBestOf && i < 7; i++) {
                            mt.mtResult[i][0] = result.getInt(++idx);
                            mt.mtResult[i][1] = result.getInt(++idx);
                        }

                        list.add(mt);
                    }
                }    
            }
        } catch (Throwable t) {
            t.printStackTrace();                
            try {
                updateMatchConnection.close();
            } catch (java.sql.SQLException ex) {
                
            }
            
            updateMatchConnection = null;
            
            list.clear();
        }
        return list.toArray(Match[]::new);
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
