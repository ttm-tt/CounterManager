/* Copyright (C) 2020 Christoph Theis */

/* global java, Packages */

// An XmlRpc JS Script must have an object with the name of that script.
// All functions are invoked on that object.
var ttm = new function() {

    // -----------------------------------------------------------------------
    // Save a database connection
    var connection = null;

    // -----------------------------------------------------------------------
    function getConnection() {
        if (connection != null) {
            try {
                // Test if the connection is still valid
                connection.getMetaData()();
            } catch (e) {
                connection = null;
            }
        }

        // If the connection is not valid, i.e. it is null, create a new one.
        if (connection == null) {
            connection = java.sql.DriverManager.getConnection(
                Packages.countermanager.model.CounterModel.getDefaultInstance().getConnectString());
                
            if (connection != null)
                connection.setAutoCommit(true);
        }

        return connection;
    }
    
    var dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    

    // -----------------------------------------------------------------------
    this.listPlayers = function(args) {
        var naName = arguments.length > 0 ? args.get('naName') : null;

        var db = Packages.countermanager.model.CounterModel.getDefaultInstance().getDatabase();        
        
        var list = db.listPlayers(naName);
        
        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();
        
        for (var idx = 0; idx < list.length; idx++)
            array.add(list[idx].convertToMap(""));

        return array;
    };


    // -----------------------------------------------------------------------
    this.listSchedules = function(args) {
        var from = null;
        var to = null;
        // fromTable could be min and max displayed tables
        var fromTable = 0; // Packages.countermanager.model.CounterModel.getDefaultInstance().getFromTable();
        var toTable = 999; // Packages.countermanager.model.CounterModel.getDefaultInstance().getToTable();
        var mtTimestamp = 0;
        
        if (arguments.length > 0 && args.size() > 0) {
            if (args.get('date') !== null) {                
                from = to = Packages.java.time.LocalDateTime.parse(this.formatDateTime(args.get('date')));
                from = from.withHour(0).withMinute(0).withSecond(0).withNano(0);
                to = to.withHour(23).withMinute(59).withSecond(59).withNano(999999);
            }            
            if (args.get('from') !== null) {
                from = Packages.java.time.LocalDateTime.parse(this.formatDateTime(args.get('from')));  
                if (to === null)
                    to = Packages.java.time.LocalDateTime.parse('2070-12-31T23:59:59.999');
            }            
            if (args.get('to') !== null) {
                to = Packages.java.time.LocalDateTime.parse(this.formatDateTime(args.get('to')));
                if (from === null)
                    from = Packages.java.time.LocalDateTime.parse('1970-01-01T00:00:00.000');
            }
        
            if (args.get('table') !== null)
                fromTable = toTable = args.get('table');
            if (args.get('fromTable') !== null)
                fromTable = args.get('fromTable');
            if (args.get('toTable') !== null)
                toTable = args.get('fromTable');

            if (args.get('mtTimestamp') !== null)
                mtTimestamp = args.get('mtTimestamp');           
        }
        
        // If no date was define the default is now
        if (from === null && to === null) {
            from = Packages.java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            to = Packages.java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999);
        }
        
        var db = Packages.countermanager.model.CounterModel.getDefaultInstance().getDatabase();        
        
        var list = db.listMatches(mtTimestamp, from, to, fromTable, toTable, false, false, false);
        
        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();
        
        for (var idx = 0; idx < list.length; idx++)
            array.add(list[idx].convertToMap(""));

        return array;
    };


    // -----------------------------------------------------------------------
    this.listStandings = function(args) {
        var where = " AND gr.grModus = 1 ";
        
        if (arguments.length > 0) {
            if (args.get('cpName') != null && args.get('cpName') != '')
                where += " AND cpName = '" + args.get('cpName') + "'";
            
            if (args.get('cpNameList') != null && args.get('cpNameList') != '')
                where += " AND cpName IN ('" + args.get('cpNameList').replace(",", "','") + "')";
            
            if (args.get('grStage') != null && args.get('grStage') != '')
                where += " AND grStage = '" + args.get('grStage') + "'";
        }
        
        var sql = 
            "SELECT cpName, cpDesc, cpType, grName, grDesc, grStage, grSize, grWinner, st.stNr, tb.stPos, " +
            "       mtMatchPoints, mtPointsA, mtPointsX, mtMatchesA, mtMatchesX, mtSetsA, mtSetsX, mtBallsA, mtBallsX, mtMatchCount, " +
            "       tmName, tmDesc, naName " +
            "  FROM CpList cp INNER JOIN GrList gr ON cp.cpID = gr.cpID CROSS APPLY TbSortFunc(gr.grID) tb INNER JOIN StTeamList st ON tb.stID = st.stID " +
            " WHERE cp.cpType = 4 AND gr.grModus = 1 AND tmName IS NOT NULL " + where + " " +
            " ORDER BY cpName, grStage, grName, st.stNr";
            
        var connection = getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();

        var row;

        while (result.next()) {
            var idx = 0;
            var cpType;
            
            row = new java.util.Hashtable();
            
            row.put('cpName', result.getString(++idx));
            row.put('cpDesc', result.getString(++idx));
            row.put('cpType', new java.lang.Integer( (cpType = result.getInt(++idx)) ));
            row.put('grName', result.getString(++idx));
            row.put('grDesc', result.getString(++idx));
            row.put('grStage', result.getString(++idx));
            row.put('grSize', new java.lang.Integer(result.getInt(++idx)));
            row.put('grWinner', new java.lang.Integer(result.getInt(++idx)));
            row.put('stNr', new java.lang.Integer(result.getInt(++idx)));
            row.put('stPos', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPoints', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPointsA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPointsX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchesA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchesX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtGamesA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtGamesX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtPointsA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtPointsX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchCount', new java.lang.Integer(result.getInt(++idx)));
            row.put('tmName', result.getString(++idx));
            row.put('tmDesc', result.getString(++idx));
            row.put('naName', result.getString(++idx));
                        
            array.add(row);
        }

        result.close();
        
        statement.close();
        
        sql = 
            "SELECT cpName, cpDesc, cpType, grName, grDesc, grStage, grSize, grWinner, st.stNr, tb.stPos, " +
            "       mtMatchPoints, mtPointsA, mtPointsX, mtMatchesA, mtMatchesX, mtSetsA, mtSetsX, mtBallsA, mtBallsX, mtMatchCount, " +
            "       plpsLast, plpsFirst, plnaName, " +
            "       bdpsLast, bdpsFirst, bdnaName " +
            "  FROM CpList cp INNER JOIN GrList gr ON cp.cpID = gr.cpID CROSS APPLY TbSortFunc(gr.grID) tb INNER JOIN StDoubleList st ON tb.stID = st.stID " +
            " WHERE (cp.cpType = 2 OR cp.cpType = 3) AND plpsLast IS NOT NULL " + where + 
            " ORDER BY cpName, grStage, grName, tb.stPos";
            
        statement = connection.createStatement();
        result = statement.executeQuery(sql);
        
        while (result.next()) {
            var idx = 0;
            
            row = new java.util.Hashtable();
            
            row.put('cpName', result.getString(++idx));
            row.put('cpDesc', result.getString(++idx));
            row.put('cpType', new java.lang.Integer(result.getInt(++idx)));
            row.put('grName', result.getString(++idx));
            row.put('grDesc', result.getString(++idx));
            row.put('grStage', result.getString(++idx));
            row.put('grSize', new java.lang.Integer(result.getInt(++idx)));
            row.put('grWinner', new java.lang.Integer(result.getInt(++idx)));
            row.put('stNr', new java.lang.Integer(result.getInt(++idx)));
            row.put('stPos', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPoints', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPointsA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPointsX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchesA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchesX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtGamesA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtGamesX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtPointsA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtPointsX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchCount', new java.lang.Integer(result.getInt(++idx)));
            row.put('plpsLast', result.getString(++idx));
            row.put('plpsFirst', result.getString(++idx));
            row.put('plnaName', result.getString(++idx));
            row.put('bdpsLast', result.getString(++idx));
            row.put('bdpsFirst', result.getString(++idx));
            row.put('bdnaName', result.getString(++idx));
                        
            array.add(row);
        }

        result.close();
        
        statement.close();
        
        sql = 
            "SELECT cpName, cpDesc, cpType, grName, grDesc, grStage, grSize, grWinner, st.stNr, tb.stPos, " +
            "       mtMatchPoints, mtPointsA, mtPointsX, mtMatchesA, mtMatchesX, mtSetsA, mtSetsX, mtBallsA, mtBallsX, mtMatchCount, " +
            "       psLast, psFirst, naName " +
            "  FROM CpList cp INNER JOIN GrList gr ON cp.cpID = gr.cpID CROSS APPLY TbSortFunc(gr.grID) tb INNER JOIN StSingleList st ON tb.stID = st.stID " +
            " WHERE cp.cpType = 1 AND psLast IS NOT NULL " + where + 
            " ORDER BY cpName, grStage, grName, tb.stPos";
            
        statement = connection.createStatement();
        result = statement.executeQuery(sql);
        
        while (result.next()) {
            var idx = 0;
            
            row = new java.util.Hashtable();
            
            row.put('cpName', result.getString(++idx));
            row.put('cpDesc', result.getString(++idx));
            row.put('cpType', new java.lang.Integer(result.getInt(++idx)));
            row.put('grName', result.getString(++idx));
            row.put('grDesc', result.getString(++idx));
            row.put('grStage', result.getString(++idx));
            row.put('grSize', new java.lang.Integer(result.getInt(++idx)));
            row.put('grWinner', new java.lang.Integer(result.getInt(++idx)));
            row.put('stNr', new java.lang.Integer(result.getInt(++idx)));
            row.put('stPos', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPoints', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPointsA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchPointsX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchesA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchesX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtGamesA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtGamesX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtPointsA', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtPointsX', new java.lang.Integer(result.getInt(++idx)));
            row.put('mtMatchCount', new java.lang.Integer(result.getInt(++idx)));
            row.put('psLast', result.getString(++idx));
            row.put('psFirst', result.getString(++idx));
            row.put('naName', result.getString(++idx));
                        
            array.add(row);
        }

        result.close();
        
        statement.close();
        return array;
    };
    
    // -----------------------------------------------------------------------
    this.setResult = function(args) {
        if (arguments.length == 0)
            return false;

        if (args instanceof java.util.List) {
            it = args.iterator();
            while (it.hasNext())
                setResult(it.next());

            return true;
        }

        // Fixe Argumente: Spielnummer, Ergebnis
        var mtNr = args.get('mtNr');
        var mtSets = args.get('mtSets');
        var mtBestOf = args.get('mtBestOf');

        // Optionale Argumente: Mannschaftsspielnummer, w/o
        var mtMS = (args.get('mtMS') != "" ? args.get('mtMS') : 0);
        var mtReverse = (args.get('mtReverse') != null ? args.get('mtReverse') : 0);
        var mtWalkOverA = (args.get('mtWalkOverA') != null ? args.get('mtWalkOverA') : 0);
        var mtWalkOverX = (args.get('mtWalkOverX') != null ? args.get('mtWalkOverX') : 0);
        var mtInjuredA  = (args.get('mtInjuredA') != null ? args.get('mtInjuredA') : 0);
        var mtInjuredX  = (args.get('mtInjuredX') != null ? args.get('mtInjuredX') : 0);

        var sets;
        var i;

        if (mtSets instanceof java.lang.String) {
            if (mtReverse == 1) {
                sets = "";

                for (i = 0; i < mtSets.length(); i += 4) {
                    sets = sets + mtSets.substring(i+2, i+4);
                    sets = sets + mtSets.substring(i+0, i+2);
                }
            } else {
                sets = mtSets;
            }
        } else {
            for (i = 0; i < mtSets.size(); i++) {
                if (mtSets.get(i).get(mtReverse ? 1 : 0) < 9)
                    sets += '0';
                sets += mtSets.get(i).get(mtReverse ? 1 : 0);
                if (sets.get(i).get(mtReverse ? 0 : 1) < 9)
                    sets += '9';
                sets += mtSets.get(i).get(mtReverse ? 0 : 1);
            }
        }

        while (sets.length < mtBestOf * 4)
            sets = sets + '0000';

        var sql = 'mtSetResultProc ' + 
                mtNr + ', ' + mtMS + ', ' + 
                mtBestOf + ', ' + '\'' + sets + '\'' + ', ' +
                mtWalkOverA + ', ' + mtWalkOverX + ', ' +
                mtInjuredA + ', ' + mtInjuredX;

        var connection = this.getConnection();

        connection.setAutoCommit(false);

        var statement = connection.createStatement();

        statement.executeUpdate(sql);

        connection.commit();
        // connection.close();
        
        connection.setAutoCommit(true);

        if (false) {
            // java.lang.System.out.println('mtNr=' + mtNr + ', mtMS=' + mtMS + ', mtBestOf=' + mtBestOf + ', mtSets=' + sets);
            java.lang.System.out.println(sql);
            // return true;
        }

        return true;
    };


    // -----------------------------------------------------------------------
    this.listNextMatches = function(args) {
        var from = null; 
        var to = null; 
        var mtTimestamp = 0;
        // Possibly default to the visible range
        var fromTable = 0;
        var toTable = 999;
        var mtNr = 0;        
        var notStarted = false;
        var notFinished = false;
        var all = false;
        
        if (arguments.length > 0 && args.size() > 0) {
            if (args.get('date') !== null) {                
                from = to = Packages.java.time.LocalDateTime.parse(this.formatDateTime(args.get('date')));
                from = from.withHour(0).withMinute(0).withSecond(0).withNano(0);
                to = to.withHour(23).withMinute(59).withSecond(59).withNano(999999);
            }            
            if (args.get('from') !== null) {
                from = Packages.java.time.LocalDateTime.parse(this.formatDateTime(args.get('from')));
                if (to === null)
                    to = Packages.java.time.LocalDateTime.parse('2070-12-31T23:59:59.999');
            }            
            if (args.get('to') !== null) {
                to = Packages.java.time.LocalDateTime.parse(this.formatDateTime(args.get('to')));
                if (from === null)
                    from = Packages.java.time.LocalDateTime.parse('1970-01-01T00:00:00.000');
            }
            
            if (args.get('table') !== null)
                fromTable = toTable = args.get('table');
            if (args.get('fromTable') !== null)
                fromTable = args.get('table');
            if (args.get('toTable') !== null)
                toTable = args.get('table');

            if (args.get('mtTimestamp') !== null)
                mtTimestamp = args.get('mtTimestamp');     
            
            if (args.get('notFinished') !== null)
                notFinished = args.get('notFinished') != 0;  // Accept conversion
            if (args.get('notStarted') !== null)
                notStarted = args.get('notStarted') != 0;    // Accept conversion
            
            if (args.get('all') !== null)
                all = args.get('all') != 0;                  // Accept converson
        }
        
        // If no date was define the default is now
        if (from === null && to === null) {
            from = Packages.java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            to = Packages.java.time.LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999);
        }

        var db = Packages.countermanager.model.CounterModel.getDefaultInstance().getDatabase();    
        if (db === null)
            return new Packages.java.util.Vector();
        
        var list = db.listMatches(mtTimestamp, from, to, fromTable, toTable, true, notStarted, notFinished);
        
        // Ergebnis muss ein Java-Typ sein
        var array = new Packages.java.util.Vector();
        
        var set = new Packages.java.util.HashSet();
        
        for (var idx = 0; idx < list.length; idx++) {
            // If not all then accept only the earliest match on table
            if (!all && set.contains(list[idx].mtTable))
                continue;
            
            set.add(list[idx].mtTable);
            
            // Swap A/X in team matches
            if (list[idx].mtReverse) {
                var pl = list[idx].plA;
                list[idx].plA = list[idx].plX;
                list[idx].plX = pl;
                
                pl = list[idx].plB;
                list[idx].plB = list[idx].plY;
                list[idx].plY = pl;
                
                var tm = list[idx].tmA;
                list[idx].tmA = list[idx].tmX;
                list[idx].tmX = tm;
                
                for (i = 0; i < list[idx].mtResult.length; ++idx) {
                    var r = list[idx].mtResult[i][0];
                    list[idx].mtResult[i][0] = list[idx].mtResult[i][1];
                    list[idx].mtResult[i][1] = r;
                }
                
                var r = list[idx].mtResA;
                list[idx].mtResA = list[idx].mtResX;
                list[idx].mtResX = r;    
                
                r = list[idx].mttmResA;
                list[idx].mttmResA = list[idx].mttmResX;
                list[idx].mttmResX = r;    
                
                var w = list[idx].mtWalkoverA;
                list[idx].mtWalkOverA = list[idx].mtWalkOverX;
                list[idx].mtWalkOverX = w;
            }
        
            var row = list[idx].convertToMap("");
        
            // Add flags if liveticker input is available
            row.put('ltActive', 
                Packages.countermanager.model.CounterModel.getDefaultInstance().isLivetickerActive(
                        list[idx].mtTable - Packages.countermanager.model.CounterModel.getDefaultInstance().getTableOffset()
                )
            );

            row.put('mtService', 
                Packages.countermanager.model.CounterModel.getDefaultInstance().getServiceAX(list[idx].mtTable, list[idx].mtNr, list[idx].mtMS)
            );
            
            array.add(row);
        }

        return array;
    };
    
    
    this.listTimes = function(args) {
        var where = "1";        
        
        if (arguments.length == 0 || args.size() == 0) {
            where += " AND DAY(mt.mtDateTime) = DAY(CURRENT_TIMESTAMP)";
            whereTeam = where;
        } else {
            if ( args.get('day') != null )
                where += " AND DAY(mt.mtDateTime) = " + args.get('day');
        }
         
        var sql = "SELECT DISTINCT mtDateTime FROM MtList WHERE " + where;
        
        var connection = getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();

        var row;

        while (result.next()) {
            row = new java.util.HashTable();
            row.put('mtDateTime', this.getTime(result, 1));
            
            array.add(row);
        }

        result.close();
        
        statement.close();
        
        return array();
    };
    
    // get current matches
    this.getCurrentTeamMatches = function(args) {
        return Packages.countermanager.model.CounterModel.getDefaultInstance().getDatabase().getCurrentTeamMatches(1, 999);
    };
    
    // Some helpers
    this.getTime = function(result, idx) {
        var date = result.getTimestamp(idx);
        if (date === null)
            return 0;
        
        return 1. * date.getTime(); //  + date.getTimezoneOffset() * 60 * 1000;
    };
    
    this.getGames = function(result, mtReverse, idx) {
        var res = new java.util.Vector();
        for (var i = 0; i < 7; i++) {
            var game = new java.util.Vector();
            var resA = result.getInt(idx++);
            var resX = result.getInt(idx++);
            
            if (mtReverse) {
                game.add(resX);
                game.add(resA);
            } else {
                game.add(resA);
                game.add(resX);                
            }
            
            res.add(game);
        }
        
        return res;
    };
    
    this.isoDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    
    this.formatDateTime = function(time) {
        if (typeof time == 'string') // Convert to ISO without trailing 'Z'
            return new Date(time).toISOString().substring(0, 23);
        else if (typeof time == 'number')
            return this.formatDateTime(new java.util.Date(time));
        else if (typeof time != 'object')
            return '';
        else if (time instanceof java.util.Date)
            return this.isoDateFormat.format(time);
        else if (time instanceof java.lang.Number)
            return this.formatDateTime(new java.util.Date(time.longValue()));
        else
            return '';
    };
};


