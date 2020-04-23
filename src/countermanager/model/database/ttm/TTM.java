/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.model.database.ttm;

import countermanager.model.CounterModelMatch;
import countermanager.model.database.IDatabase;
import countermanager.model.database.IDatabaseSettings;
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
        if (server.equals("(local)"))
            sb.append("localhost");
        else
            sb.append(server);
        sb.append(";");
        
        String[] database = dbName.split("\\\\");
        sb.append("databaseName=").append(database[0]).append(";");
        if (database.length > 1)
            sb.append("instanceName=").append(database[1]).append(";");
        
        if (windowsAuth)
            sb.append("integratedSecurity=true;");
        else
            sb.append("user=").append(user).append(";").append("password=").append(pwd).append(";");
        
        connectionString = sb.toString();
    }
    
    
    public TTM(String connectionString) {
        String[] parts = connectionString.split(";");
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (String part : parts) {
            String[] tmp = part.split("=");
            map.put(tmp[0], tmp.length > 1 ? tmp[1] : "");
        }
        
        String database[] = map.get("DATABASE").split("\\\\");
        
        StringBuilder buffer = new StringBuilder();
        buffer.append("jdbc:sqlserver://");
        if (!map.containsKey("SERVER"))
            buffer.append("localhost");
        else if (map.get("SERVER").equals("(local)"))
            buffer.append("localhost");
        else
            buffer.append(map.get("SERVER"));
        buffer.append(";");
        
        buffer.append("databaseName=").append(database[0]).append(";");
        if (database.length > 1)
            buffer.append("instanceName=").append(database[1]).append(";");
        
        if (!map.containsKey("Trusted_Connection") || !map.get("Trusted_Connection").equals("Yes"))
            buffer.append("user=").append(map.get("UID")).append(";").append("password=").append(map.get("PWD")).append(";");
        else
            buffer.append("integratedSecurity=true;");
        
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
            "SELECT cpType, cpName, cpDesc, grName, grDesc, grStage, grModus, grSize, " +
            "       gr.grWinner, gr.grNofRounds, gr.grNofMatches, " +
            "       mtTimeStamp, mtNr, 0 AS mtMS, mtRound, mtMatch, " +
            "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, " +
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
            "       cpType = 1 AND mtTable >= ? AND mtTable <= ? " +

            // Doubles and Mixed
            "UNION " +                        
            "SELECT cpType, cpName, cpDesc, grName, grDesc, grStage, grModus, grSize, " +
            "       gr.grWinner, gr.grNofRounds, gr.grNofMatches, " +
            "       mtTimeStamp, mtNr, 0 AS mtMS, mtRound, mtMatch, " +
            "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, " +
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
            "       (cpType = 2 OR cpType = 3) AND mtTable >= ? AND mtTable <= ? " +

            // Team (Individual)
            "UNION " + 
            "SELECT cp.cpType, cp.cpName, cp.cpDesc, gr.grName, gr.grDesc, gr.grStage, gr.grModus, gr.grSize, " +
            "       gr.grWinner, gr.grNofRounds, gr.grNofMatches, " +
            "       mt.mtTimeStamp, mt.mtNr, mt.mtMS AS mtMS, mt.mtRound, mt.mtMatch, " +
            "       mt.mtTable, mt.mtDateTime, mt.mtBestOf, mt.mtMatches, mt.mtReverse, " +
            "       mt.mtResA, mt.mtResX, " +
            "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, plAnaRegion, " +
            "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, plBnaRegion, " +
            "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, plXnaRegion, " +
            "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, plYnaRegion, " +
            "       stA.tmName \"tmAtmName\", stA.tmDesc \"tmAtmDesc\", stA.naName \"tmAnaName\", stA.naRegion \"tmAnaRegion\", " +
            "       stX.tmName \"tmXtmName\", stX.tmDesc \"tmXtmDesc\", stX.naName \"tmXnaName\", stX.naRegion \"tmXnaRegion\", " +
            "       mt.mtResA \"mttmResA\", mt.mtResX \"mttmResX\", " +
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
            "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
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
            "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
            "       mt.stA IS NOT NULL AND mt.stX IS NOT NULL AND " +
            // If all == false then exclude finished matches
            (all ? "" : (
            "       (mt.mtResA IS NULL OR 2 * mt.mtResA < mt.mtBestOf) AND " + 
            "       (mt.mtResX IS NULL OR 2 * mt.mtResX < mt.mtBestOf) AND " +
            "       (mt.mtWalkOverA IS NULL OR mt.mtWalkOverA = 0) AND " +
            "       (mt.mtWalkOverX IS NULL OR mt.mtWalkOverX = 0) AND " 
            // "       2 * mttmResA < mt.mtMatches AND 2 * mttmResX < mt.mtMatches AND "
            )) +
            "       cp.cpType = 4 AND mt.mtTable >= ? AND mt.mtTable <= ? " +

            // Order: By Table, Time, Individual.
            // Also for Round and Match-in-Round for RR-groups with all matches 
            // at the same time and table.
            " ORDER BY mtTable, mtDateTime, mtMS, mtRound, mtMatch";
        
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
                        int     mtReverse   = result.getInt(idx++);
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

                        match.mtResA      = mtResA;
                        match.mtResX      = mtResX;

                        match.plA.plNr    = plAplNr % 10000;
                        match.plA.plExtId = plAplExtId;
                        match.plA.psLast  = plApsLast;
                        match.plA.psFirst = plApsFirst;
                        match.plA.naName  = plAnaName;
                        match.plA.naRegion = plAnaRegion;

                        match.plB.plNr    = plBplNr % 10000;
                        match.plB.plExtId = plBplExtId;
                        match.plB.psLast  = plBpsLast;
                        match.plB.psFirst = plBpsFirst;
                        match.plB.naName  = plBnaName;
                        match.plB.naRegion = plBnaRegion;

                        match.plX.plNr    = plXplNr % 10000;
                        match.plX.plExtId = plXplExtId;
                        match.plX.psLast  = plXpsLast;
                        match.plX.psFirst = plXpsFirst;
                        match.plX.naName  = plXnaName;
                        match.plX.naRegion = plXnaRegion;

                        match.plY.plNr    = plYplNr % 10000;
                        match.plY.plExtId = plYplExtId;
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
}
