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
        var sql = "SELECT plNr, psLast, psFirst, naName, plExtID FROM PlList";

        if (arguments.length > 0 && args.get('naName') != null)
          sql += " WHERE naName = '" + args.get('naName') + "' ";

        sql += " ORDER BY plNr";

        var naName=null;
        var connection = getConnection();
        var statement  = connection.createStatement();
        var result     = statement.executeQuery(sql);

        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            var row = new java.util.Hashtable();

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('plNr', new java.lang.Integer(result.getInt('plNr') % 10000));

            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            // psLast wird wirklich auf plLast gemappt etc.
            row.put('psLast', result.getString('psLast').trim());
            row.put('psFirst', result.getString('psFirst').trim());
            naName = result.getString('naName');
            if (naName != null) {
                naName = naName.trim();
            } else {
                naName = '';
            }
            row.put('naName', naName);

            row.put('plExtID', result.getString('plExtID').trim());

            array.add(row);
        }

        result.close();

        statement.close();
        // connection.close();

        return array;
    };


    // -----------------------------------------------------------------------
    this.listSchedule = function(args) {
        var sql =
            "SELECT cpName, cpType, grDesc, grModus, grSize, mtRound, mtMatch, mtBestOf, mtNr, " +
            "       mtReverse, mtResA, mtResX, " +
            "       tmAtmName, tmAtmDesc, tmXtmName, tmXtmDesc, mtDateTime, mtTable, mtTimeStamp " +
            "  FROM MtTeamList mt" +
                "   INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "   INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cp.cpType = 4" +
            " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
            "       AND tmAtmName IS NOT NULL AND tmXtmName IS NOT NULL ";

        if (arguments.length == 0 || args.size() == 0)
            sql += " AND DAY(mtDateTime) = DAY(CURRENT_TIMESTAMP)";
        else {
            if ( args.get('mtTimestamp') != null )
                sql += " AND mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";
            if (args.get('day') != null)
                sql += " AND DAY(mtDateTime) = " + args.get('day');
            if (args.get('from') != null)
                sql += " AND mt.mtDateTime >= CAST('" + this.formatDateTime(args.get('from')) + "' AS DATETIME)";
            if (args.get('to') != null)
                sql += " AND mt.mtDateTime <= CAST('" + this.formatDateTime(args.get('to')) + "' AS DATETIME)";
            if (args.get('date') != null)
                sql += " AND CONVERT(date, mt.mtDateTime) = '" + args.get('date') + "'";
            if (args.get('table') != null)
                sql += " AND mtTable= " + args.get('table');
        }

        sql += " ORDER BY mtDateTime, mtTable";

        var connection = this.getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();

        var row;
        var ts;
        var grModus;
        var grSize;
        var mtRound;
        var mtMatch;

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            row.put('cpName', result.getString('cpName').trim());
            row.put('cpType', new java.lang.Integer(result.getInt('cpType')));
            row.put('grDesc', result.getString('grDesc').trim());
            grModus = result.getInt('grModus');
            grSize = result.getInt('grSize');
            mtRound = result.getInt('mtRound');
            mtMatch = result.getInt('mtMatch');
            row.put('grModus', new java.lang.Integer(grModus));
            row.put('grSize', new java.lang.Integer(grSize));
            row.put('mtRound', new java.lang.Integer(mtRound));
            row.put('mtMatch', new java.lang.Integer(mtMatch));
            if (grModus == 1)
                row.put('mtRoundStr', "Rd: " + mtRound);
            else if ((grSize >> mtRound) == 1 && mtMatch == 1)
                row.put('mtRoundStr', "Rd: F");
            else if ((grSize >> mtRound) == 2 && mtMatch <= 2)
                row.put('mtRoundStr', "Rd: SF");
            else if ((grSize >> mtRound) == 4 && mtMatch <= 4)
                row.put('mtRoundStr', "Rd: QF");
            else if (grModus == 2)
                row.put('mtRoundStr', "Rd of " + (2 * (grSize >> mtRound)));
            else
                row.put('mtRoundStr', "Rd: " + mtRound);
            row.put('mtBestOf', new java.lang.Integer(result.getInt('mtBestOf')));

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('mtNr', new java.lang.Integer(result.getInt('mtNr')));
            row.put('mtReverse', new java.lang.Integer(result.getInt('mtReverse')));
            row.put('mtResA', new java.lang.Integer(result.getInt('mtResA')));
            row.put('mtResX', new java.lang.Integer(result.getInt('mtResX')));

            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('tmAtmName', result.getString('tmAtmName').trim());
            row.put('tmAtmDesc', result.getString('tmAtmDesc').trim());

            row.put('tmXtmName', result.getString('tmXtmName').trim());
            row.put('tmXtmDesc', result.getString('tmXtmDesc').trim());

            row.put('mtDateTime', this.getTime(result, 'mtDateTime'));
            row.put('mtTable', new java.lang.Integer(result.getShort('mtTable')));

            row.put('mtTimestamp', this.getTime(result, 'mtTimestamp'));

            array.add(row);
        }

        result.close();

        sql =
            "SELECT cpName, cpType, grDesc, grModus, grSize, mtRound, mtMatch, mtBestOf, mtNr, mtResA, mtResX,  " +
            "   plAplNr, plApsLast, plApsFirst, plAnaName, plAplExtID, " +
            "   plBplNr, plBpsLast, plBpsFirst, plBnaName, plBplExtID, " +
            "   plXplNr, plXpsLast, plXpsFirst, plXnaName, plXplExtID, " +
            "   plYplNr, plYpsLast, plYpsFirst, plYnaName, plYplExtID, " +
            "   mtDateTime, mtTable, mtTimeStamp " +
            "  FROM MtDoubleList mt " +
            "    INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "    INNER JOIN CpList cp ON gr.cpID = cp.cpID AND (cpType = 2 OR cpType = 3) " +
            " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
            "       AND plAplNr IS NOT NULL AND plXplNR IS NOT NULL ";

        if (arguments.length == 0 || args.size() == 0)
            sql += " AND DAY(mtDateTime) = DAY(CURRENT_TIMESTAMP)";
        else {
            if ( args.get('mtTimestamp') != null )
                sql += " AND mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";
            if (args.get('day') != null)
                sql += " AND DAY(mtDateTime) = " + args.get('day');
            if (args.get('from') != null)
                sql += " AND mt.mtDateTime >= CAST('" + this.formatDateTime(args.get('from')) + "' AS DATETIME)";
            if (args.get('to') != null)
                sql += " AND mt.mtDateTime <= CAST('" + this.formatDateTime(args.get('to')) + "' AS DATETIME)";
            if (args.get('date') != null)
                sql += " AND CONVERT(date, mt.mtDateTime) = '" + args.get('date') + "'";
            if (args.get('table') != null)
                sql += " AND mtTable= " + args.get('table');
        }

        sql += " ORDER BY mtDateTime, mtTable";

        result = statement.executeQuery(sql);
        
        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            row.put('cpName', result.getString('cpName').trim());
            row.put('cpType', new java.lang.Integer(result.getInt('cpType')));
            row.put('grDesc', result.getString('grDesc').trim());
            grModus = result.getInt('grModus');
            grSize = result.getInt('grSize');
            mtRound = result.getInt('mtRound');
            mtMatch = result.getInt('mtMatch');
            row.put('grModus', new java.lang.Integer(grModus));
            row.put('grSize', new java.lang.Integer(grSize));
            row.put('mtRound', new java.lang.Integer(mtRound));
            row.put('mtMatch', new java.lang.Integer(mtMatch));
            if (grModus == 1)
                row.put('mtRoundStr', "Rd: " + mtRound);
            else if ((grSize >> mtRound) == 1 && mtMatch == 1)
                row.put('mtRoundStr', "Rd: F");
            else if ((grSize >> mtRound) == 2 && mtMatch <= 2)
                row.put('mtRoundStr', "Rd: SF");
            else if ((grSize >> mtRound) == 4 && mtMatch <= 4)
                row.put('mtRoundStr', "Rd: QF");
            else if (grModus == 2)
                row.put('mtRoundStr', "Rd: 1/" + (grSize >> mtRound));
            else
                row.put('mtRoundStr', "Rd: " + mtRound);
            row.put('mtBestOf', new java.lang.Integer(result.getInt('mtBestOf')));

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('mtNr', new java.lang.Integer(result.getInt('mtNr')));
            row.put('mtResA', new java.lang.Integer(result.getInt('mtResA')));
            row.put('mtResX', new java.lang.Integer(result.getInt('mtResX')));

            row.put('plAplNr', new java.lang.Integer(result.getInt('plAplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plApsLast', result.getString('plApsLast').trim());
            row.put('plApsFirst', result.getString('plApsFirst').trim());
            row.put('plAnaName', result.getString('plAnaName').trim());
            row.put('plAplExtID', result.getString('plAplExtID').trim());

            row.put('plBplNr', new java.lang.Integer(result.getInt('plBplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plBpsLast', result.getString('plBpsLast').trim());
            row.put('plBpsFirst', result.getString('plBpsFirst').trim());
            row.put('plBnaName', result.getString('plBnaName').trim());
            row.put('plBplExtID', result.getString('plBplExtID').trim());

            row.put('plXplNr', new java.lang.Integer(result.getInt('plXplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plXpsLast', result.getString('plXpsLast').trim());
            row.put('plXpsFirst', result.getString('plXpsFirst').trim());
            row.put('plXnaName', result.getString('plXnaName').trim());
            row.put('plXplExtID', result.getString('plXplExtID').trim());

            row.put('plYplNr', new java.lang.Integer(result.getInt('plYplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plYpsLast', result.getString('plYpsLast').trim());
            row.put('plYpsFirst', result.getString('plYpsFirst').trim());
            row.put('plYnaName', result.getString('plYnaName').trim());
            row.put('plYplExtID', result.getString('plYplExtID').trim());

            row.put('mtDateTime', this.getTime(result, 'mtDateTime'));
            row.put('mtTable', new java.lang.Integer(result.getShort('mtTable')));

            row.put('mtTimestamp', this.getTime(result, 'mtTimestamp'));

            array.add(row);
        }

        result.close();

        sql =
            "SELECT cpName, cpType, grDesc, grModus, grSize, mtRound, mtMatch, mtBestOf, mtNr, mtResA, mtResX,  " +
            "   plAplNr, plApsLast, plApsFirst, plAnaName, plAplExtID, " +
            "   plXplNr, plXpsLast, plXpsFirst, plXnaName, plXplExtID, " +
            "   mtDateTime, mtTable, mtTimeStamp " +
            "  FROM MtSingleList mt " +
            "    INNER JOIN GrList gr ON mt.grID = gr.grID " +
            "    INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cpType = 1 " +
            " WHERE mtDateTime IS NOT NULL AND mtTable IS NOT NULL " +
            "       AND plAplNr IS NOT NULL AND plXplNR IS NOT NULL ";

        if (arguments.length == 0 || args.size() == 0)
            sql += " AND DAY(mtDateTime) = DAY(CURRENT_TIMESTAMP)";
        else {
            if ( args.get('mtTimestamp') != null )
                sql += " AND mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";
            if (args.get('day') != null)
                sql += " AND DAY(mtDateTime) = " + args.get('day');
            if (args.get('from') != null)
                sql += " AND mt.mtDateTime >= CAST('" + this.formatDateTime(args.get('from')) + "' AS DATETIME)";
            if (args.get('to') != null)
                sql += " AND mt.mtDateTime <= CAST('" + this.formatDateTime(args.get('to')) + "' AS DATETIME)";
            if (args.get('date') != null)
                sql += " AND CONVERT(date, mt.mtDateTime) = '" + args.get('date') + "'";
            if (args.get('table') != null)
                sql += " AND mtTable= " + args.get('table');
        }

        sql += " ORDER BY mtDateTime, mtTable";

        result = statement.executeQuery(sql);

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            row.put('cpName', result.getString('cpName').trim());
            row.put('cpType', new java.lang.Integer(result.getInt('cpType')));
            row.put('grDesc', result.getString('grDesc').trim());
            grModus = result.getInt('grModus');
            grSize = result.getInt('grSize');
            mtRound = result.getInt('mtRound');
            mtMatch = result.getInt('mtMatch');
            row.put('grModus', new java.lang.Integer(grModus));
            row.put('grSize', new java.lang.Integer(grSize));
            row.put('mtRound', new java.lang.Integer(mtRound));
            row.put('mtMatch', new java.lang.Integer(mtMatch));
            if (grModus == 1)
                row.put('mtRoundStr', "Rd: " + mtRound);
            else if ((grSize >> mtRound) == 1 && mtMatch == 1)
                row.put('mtRoundStr', "Rd: F");
            else if ((grSize >> mtRound) == 2 && mtMatch <= 2)
                row.put('mtRoundStr', "Rd: SF");
            else if ((grSize >> mtRound) == 4 && mtMatch <= 4)
                row.put('mtRoundStr', "Rd: QF");
            else if (grModus == 2)
                row.put('mtRoundStr', "Rd: 1/" + (grSize >> mtRound));
            else
                row.put('mtRoundStr', "Rd: " + mtRound);
            row.put('mtBestOf', new java.lang.Integer(result.getInt('mtBestOf')));

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('mtNr', new java.lang.Integer(result.getInt('mtNr')));
            row.put('mtResA', new java.lang.Integer(result.getInt('mtResA')));
            row.put('mtResX', new java.lang.Integer(result.getInt('mtResX')));

            row.put('plAplNr', new java.lang.Integer(result.getInt('plAplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plApsLast', result.getString('plApsLast').trim());
            row.put('plApsFirst', result.getString('plApsFirst').trim());
            row.put('plAnaName', result.getString('plAnaName').trim());
            row.put('plAplExtID', result.getString('plAplExtID').trim());

            row.put('plXplNr', new java.lang.Integer(result.getInt('plXplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plXpsLast', result.getString('plXpsLast').trim());
            row.put('plXpsFirst', result.getString('plXpsFirst').trim());
            row.put('plXnaName', result.getString('plXnaName').trim());
            row.put('plXplExtID', result.getString('plXplExtID').trim());

            row.put('mtDateTime', this.getTime(result, 'mtDateTime'));
            row.put('mtTable', new java.lang.Integer(result.getShort('mtTable')));

            row.put('mtTimestamp', this.getTime(result, 'mtTimestamp'));

            array.add(row);
        }

        result.close();

        statement.close();
        // connection.close();

        return array;
    };


    // -----------------------------------------------------------------------
    this.listNomination = function(args) {
        var sql =  "SELECT cpName, grDesc, grModus, grSize, mtRound, mtMatch, mtBestOf, mtNr, mtMS, mtReverse, mtResA, mtResX, " +
                   "       plAplNr, plApsLast, plApsFirst, plAnaName, plAplExtID, " +
                   "       plXplNr, plXpsLast, plXpsFirst, plXnaName, plXplExtID, " +
                   "       mtDateTime, mtTable, mtTimestamp " +
                   "  FROM MtIndividualList a " +
                   "   INNER JOIN GrList gr ON a.grID = gr.grID " +
                   "   INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                   " WHERE plAnaName IS NOT NULL AND plXnaName IS NOT NULL ";

        if (arguments.length == 0 || args.size() == 0)
            sql += " AND DAY(mtDateTime) = DAY(CURRENT_TIMESTAMP)";
        else {
            if ( args.get('mtTimestamp') != null )
                sql += " AND mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";
            if (args.get('day') != null)
                sql += " AND DAY(mtDateTime) = " + args.get('day');
            if (args.get('from') != null)
                sql += " AND mt.mtDateTime >= CAST('" + this.formatDateTime(args.get('from')) + "' AS DATETIME)";
            if (args.get('to') != null)
                sql += " AND mt.mtDateTime <= CAST('" + this.formatDateTime(args.get('to')) + "' AS DATETIME)";
            if (args.get('date') != null)
                sql += " AND CONVERT(date, mt.mtDateTime) = '" + args.get('date') + "'";
            if (args.get('table') != null)
                sql += " AND mtTable= " + args.get('table');
            if (args.get('mtNr') != null)
                sql += " AND mtNr = " + args.get('mtNr');
        }

        sql += " ORDER BY mtDateTime, mtTable, mtMS";

        var connection = this.getConnection();
        var statement  = connection.createStatement();
        var result     = statement.executeQuery(sql);

        var grModus;
        var grSize;
        var mtRound;
        var mtMatch;
        var mtReverse;
        
        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            var row = new java.util.Hashtable();

            row.put('cpName', result.getString('cpName').trim());
            row.put('grDesc', result.getString('grDesc').trim());
            grModus = result.getInt('grModus');
            grSize = result.getInt('grSize');
            mtRound = result.getInt('mtRound');
            mtMatch = result.getInt('mtMatch');
            row.put('grModus', new java.lang.Integer(grModus));
            row.put('grSize', new java.lang.Integer(grSize));
            row.put('mtRound', new java.lang.Integer(mtRound));
            row.put('mtMatch', new java.lang.Integer(mtMatch));
            if (grModus == 1)
                row.put('mtRoundStr', "Rd: " + mtRound);
            else if ((grSize >> mtRound) == 1 && mtMatch == 1)
                row.put('mtRoundStr', "Rd: F");
            else if ((grSize >> mtRound) == 2 && mtMatch <= 2)
                row.put('mtRoundStr', "Rd: SF");
            else if ((grSize >> mtRound) == 4 && mtMatch <= 4)
                row.put('mtRoundStr', "Rd: QF");
            else if (grModus == 2)
                row.put('mtRoundStr', "Rd: 1/" + (grSize >> mtRound));
            else
                row.put('mtRoundStr', "Rd: " + mtRound);
            row.put('mtBestOf', new java.lang.Integer(result.getInt('mtBestOf')));

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('mtNr', new java.lang.Integer(result.getInt('mtNr')));
            row.put('mtMS', new java.lang.Integer(result.getInt('mtMS')));
            
            mtReverse = result.getInt(mtReverse);
            
            row.put('mtReverse', new java.lang.Integer(mtReverse));
            row.put('mtResA', new java.lang.Integer(result.getInt('mtResA')));
            row.put('mtResX', new java.lang.Integer(result.getInt('mtResX')));

            row.put(mtReverse ? 'plXplNr' : 'plAplNr', new java.lang.Integer(result.getInt('plAplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            // psLast wird wirklich auf plLast gemappt etc. Siehe listPlayers.
            row.put(mtReverse ? 'plXpsLast' : 'plApsLast', result.getString('plApsLast').trim());
            row.put(mtReverse ? 'plXpsFirst' : 'plApsFirst', result.getString('plApsFirst').trim());
            row.put(mtReverse ? 'plXnaName' : 'plAnaName', result.getString('plAnaName').trim());
            row.put(mtReverse ? 'plXplExtID' : 'plAplExtID', result.getString('plAplExtID').trim());

            // Die Reihenfolge muss mit der von der Query uebereinstimmen?
            row.put(mtReverse ? 'plAplNr' : 'plXplNr', new java.lang.Integer(result.getInt('plXplNr') % 10000));
            row.put(mtReverse ? 'plApsLast' : 'plXpsLast', result.getString('plXpsLast').trim());
            row.put(mtReverse ? 'plApsFirst' : 'plXpsFirst', result.getString('plXpsFirst').trim());
            row.put(mtReverse ? 'plAnaName' : 'plXnaName', result.getString('plXnaName').trim());
            row.put(mtReverse ? 'plAplExtID' : 'plXplExtID', result.getString('plXplExtID').trim());

            row.put('mtDateTime', this.getTime(result, 'mtDateTime'));

            row.put('mtTable', new java.lang.Integer(result.getInt('mtTable')));
            
            row.put('mtTimestamp', this.getTime(result, 'mtTimestamp'));

            array.add(row);
        }

        statement.close();
        // connection.close();

        return array;
    };


    // -----------------------------------------------------------------------
    this.listIndividual = function(args) {
        var connection = this.getConnection();

        var sql = "SELECT mtNr, mtMS, mtReverse, mtResA, mtResX, " +
                  "       plAplNr, plApsLast, plApsFirst, plAnaName, plAplExtID, " +
                  "       plXplNr, plXpsLast, plXpsFirst, plXnaName, plXplExtID, " +
                  "       mtDateTime, mtTable, mtTimestamp " +
                  "  FROM MtIndividualList " +
                  " WHERE plAplNr IS NOT NULL AND plXplNr IS NOT NULL ";

        if (arguments.length == 0 || args.size() == 0)
            sql += " AND DAY(mtDateTime) = DAY(CURRENT_TIMESTAMP)";
        else {
            if ( args.get('mtTimestamp') != null )
                sql += " AND mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";
            if (args.get('day') != null)
                sql += " AND DAY(mtDateTime) = " + args.get('day');
            if (args.get('from') != null)
                sql += " AND mt.mtDateTime >= CAST('" + this.formatDateTime(args.get('from')) + "' AS DATETIME)";
            if (args.get('to') != null)
                sql += " AND mt.mtDateTime <= CAST('" + this.formatDateTime(args.get('to')) + "' AS DATETIME)";
            if (args.get('date') != null)
                sql += " AND CONVERT(date, mt.mtDateTime) = '" + args.get('date') + "'";
            if (args.get('table') != null)
                sql += " AND mtTable= " + args.get('table');
            if (args.get('mtNr') != null)
                sql += " AND mtNr = " + args.get('mtNr');
        }

        sql += " ORDER BY mtNr, mtMS";

        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        var mtReverse;
        
        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();
        
        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            var row = new java.util.Hashtable();

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('mtNr', new java.lang.Integer(result.getInt('mtNr')));
            row.put('mtMS', new java.lang.Integer(result.getInt('mtMS')));
            
            mtReverse = result.getInt('mtReverse');            
            row.put('mtReverse', new java.lang.Integer(mtReverse));
            
            row.put('mtResA', new java.lang.Integer(result.getInt('mtResA')));
            row.put('mtResX', new java.lang.Integer(result.getInt('mtResX')));

            row.put(mtReverse ? 'plXplNr' : 'plAplNr', new java.lang.Integer(result.getInt('plAplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            // psLast wird wirklich auf plLast gemappt etc. Siehe listPlayers.
            row.put(mtReverse ? 'plXpsLast' : 'plApsLast', result.getString('plApsLast').trim());
            row.put(mtReverse ? 'plXpsFirst' : 'plApsFirst', result.getString('plApsFirst').trim());
            row.put(mtReverse ? 'plXnaName' : 'plAnaName', result.getString('plAnaName').trim());
            row.put(mtReverse ? 'plXplExtID' : 'plAplExtID', result.getString('plAplExtID').trim());

            // Die Reihenfolge muss mit der von der Query uebereinstimmen?
            row.put(mtReverse ? 'plAplNr': 'plXplNr', new java.lang.Integer(result.getInt('plXplNr') % 10000));
            row.put(mtReverse ? 'plApsLaset' : 'plXpsLast', result.getString('plXpsLast').trim());
            row.put(mtReverse ? 'plApsFirst' : 'plXpsFirst', result.getString('plXpsFirst').trim());
            row.put(mtReverse ? 'plAnaName' : 'plXnaName', result.getString('plXnaName').trim());
            row.put(mtReverse ? 'plAplExtID' : 'plXplExtID', result.getString('plXplExtID').trim());

            row.put('mtDateTime', result.getDate('mtDateTime'));
            row.put('mtTable', new java.lang.Integer(result.getInt('mtTable')));

            row.put('mtTimestamp', this.getTime('mtTimestamp'));

            array.add(row);
        }

        statement.close();
        // connection.close();

        return array;
    };


    // -----------------------------------------------------------------------
    this.listSeeding = function(args) {
        var sql = "SELECT cpName, cpType, grStage, grName, tmName, tmDesc, stNr, stPos " +
                  "  FROM StTeamList st INNER JOIN GrList gr ON st.grID = gr.grID INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                  " WHERE tmName IS NOT NULL ";
    /*
        if ( arguments.length > 0 && args.get('cpName') != null )
            sql += " AND cpName = '" + args.get('cpName') + "'";
        if (arguments.length > 0 && args.get('grStage') != null)
            sql += " AND grStage = '" + args.get('grStage') + "'";
        if (arguments.length > 0 && args.get('grName') != null)
            sql += " AND grName = '" + args.get('grName') + "'";
    */
        sql += " ORDER BY cpName, grStage, grName, stPos";

        var connection = this.getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();

        var row;

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('cpName', result.getString('cpName').trim());
            row.put('cpType', new java.lang.Integer(result.getInt('cpType')));
            row.put('grStage', result.getString('grStage').trim());
            row.put('grName', result.getString('grName').trim());
            row.put('tmName', result.getString('tmName').trim());
            row.put('tmDesc', result.getString('tmDesc').trim());

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('stNr', new java.lang.Integer(result.getInt('stNr')));
            row.put('stPos', new java.lang.Integer(result.getInt('stPos')));

            array.add(row);

            break;
        }

        result.close();

        sql =
            "SELECT cpName, cpType, grStage, grName, " +
            "   plplNr, plpsLast, plpsFirst, plnaName, plplExtID, " +
            "   bdplNr, bdpsLast, bdpsFirst, bdnaName, bdplExtID, " +
            "   stNr, stPos " +
            "  FROM StDoubleList st " +
            "   INNER JOIN GrList gr ON st.grID = gr.grID " +
            "   INNER JOIN CpList cp ON gr.cpID = cp.cpID AND (cp.cpType = 2 OR cp.cpType = 3) " +
            " WHERE plplNr IS NOT NULL ";
    /*
        if (arguments.length > 0 && args.get('cpName') != null )
            sql += " AND cpName = '" + args.get('cpName') + "'";
        if (arguments.length > 0 && args.get('grStage') != null)
            sql += " AND grStage = '" + args.get('grStage') + "'";
        if (arguments.length > 0 && args.get('grName') != null)
            sql += " AND grName = '" + args.get('grName') + "'";
    */
        sql += " ORDER BY cpName, grStage, grName, stPos";


        result = statement.executeQuery(sql);

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('cpName', result.getString('cpName').trim());
            row.put('cpType', new java.lang.Integer(result.getInt('cpType')));
            row.put('grStage', result.getString('grStage').trim());
            row.put('grName', result.getString('grName').trim());

            row.put('plAplNr', new java.lang.Integer(result.getInt('plplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plApsLast', result.getString('plpsLast').trim());
            row.put('plApsFirst', result.getString('plpsFirst').trim());
            row.put('plAnaName', result.getString('plnaName').trim());
            row.put('plAplExtID', result.getString('plplExtID').trim());

            row.put('plBplNr', new java.lang.Integer(result.getInt('bdplNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plBpsLast', result.getString('bdpsLast').trim());
            row.put('plBpsFirst', result.getString('bdpsFirst').trim());
            row.put('plBnaName', result.getString('bdnaName').trim());
            row.put('plBplExtID', result.getString('bdplExtID').trim());

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('stNr', new java.lang.Integer(result.getInt('stNr')));
            row.put('stPos', new java.lang.Integer(result.getInt('stPos')));

            array.add(row);
        }


        result.close();

        sql =
            "SELECT cpName, cpType, grStage, grName, " +
            "   plNr, psLast, psFirst, naName, plExtID, " +
            "   stNr, stPos " +
            "  FROM StSingleList st " +
            "   INNER JOIN GrList gr ON st.grID = gr.grID " +
            "   INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cp.cpType = 1 " +
            " WHERE plNr IS NOT NULL ";
    /*
        if ( arguments.length > 0 && args.get('cpName') != null )
            sql += " AND cpName = '" + args.get('cpName') + "'";
        if ( arguments.length > 0 && args.get('grStage') != null )
            sql += " AND grStage = '" + args.get('grStage') + "'";
        if ( arguments.length > 0 && args.get('grName') != null )
            sql += " AND grName = '" + args.get('grName') + "'";
    */
        sql += " ORDER BY cpName, grStage, grName, stPos";


        result = statement.executeQuery(sql);

        while (result.next()) {
            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('cpName', result.getString('cpName').trim());
            row.put('cpType', new java.lang.Integer(result.getInt('cpType')));
            row.put('grStage', result.getString('grStage').trim());
            row.put('grName', result.getString('grName').trim());

            row.put('plAplNr', new java.lang.Integer(result.getInt('plNr') % 10000));
            // getString paddet den String auf die max. Laenge von 8 / 64 Zeichen
            row.put('plApsLast', result.getString('psLast').trim());
            row.put('plApsFirst', result.getString('psFirst').trim());
            row.put('plAnaName', result.getString('naName').trim());
            row.put('plAplExtID', result.getString('plExtID').trim());

            // JavaScript wuerde hier ein double liefern, ich will aber ausdruecklich ein Integer
            row.put('stNr', new java.lang.Integer(result.getInt('stNr')));
            row.put('stPos', new java.lang.Integer(result.getInt('stPos')));

            array.add(row);
        }


        result.close();

        statement.close();
        // connection.close();

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
    this.listMatches = function(args) {
        var where = "";
        var whereTeam = "";
        
        if ( args.get('mtTimestamp') != null )
            where += " AND mt.mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";

        if (args.get('cpName') != null)
            where += " AND cp.cpName = '" + args.get('cpName') + "'";
        
        if (args.get('grStage') != null)
            where += " AND gr.grStage = '" + args.get('grStage') + "'";
        
        if (args.get('mtRound') != null)
            where += " AND mt.mtRound = " + args.get('mtRound');
        
        whereTeam = where;
        
        var sql =
                // Singles
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, " +
                "       gr.grStage, gr.grName, gr.grDesc, gr.grModus, gr.grSize, " +
                "       gr.grQualRounds, gr.grNofRounds, gr.grNofMatches, " +
                "       mt.mtNr, 0 AS mtMS, mt.mtRound, mt.mtMatch, " +
                "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, mt.mtTimestamp, " +
                "       mt.mtWalkOverA, mt.mtWalkOverX, " +
                "       mt.mtResA, mt.mtResX, 0 AS mtTeamResA, 0 AS mtTeamResX, " +
                "       NULL AS tmAnaName, NULL AS tmAtmName, NULL AS tmAtmDesc, " +
                "       NULL AS tmXnaNAme, NULL AS tmXtmName, NULL AS tmXtmDesc, " +
                "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, " +
                "       NULL AS plBplNr, NULL AS plBplExtId, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, " +
                "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, " +
                "       NULL AS plYplNr, NULL AS plYplExtId, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, " +
                "       mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "       mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "       mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "       mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtSingleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 " +
                "       LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 " +
                "       LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 " +
                "       LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 " +
                "       LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 " +
                "       LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 " +
                "       LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 " +
                " WHERE " + 
                "       cpType = 1 " + 
                where + " " +

                // Doubles and Mixed
                "UNION " +
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, " +
                "       gr.grStage, gr.grName, gr.grDesc, gr.grModus, gr.grSize, " +
                "       gr.grQualRounds, gr.grNofRounds, gr.grNofMatches, " +
                "       mt.mtNr, 0 AS mtMS, mt.mtRound, mt.mtMatch, " +
                "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, mt.mtTimestamp, " +
                "       mt.mtWalkOverA, mt.mtWalkOverX, " +
                "       mt.mtResA, mt.mtResX, 0 AS mtTeamResA, 0 AS mtTeamResX, " +
                "       NULL AS tmAnaName, NULL AS tmAtmName, NULL AS tmAtmDesc, " +
                "       NULL AS tmXnaName, NULL AS tmXtmName, NULL AS tmXtmDesc, " +
                "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, " +
                "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, " +
                "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, " +
                "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, " +
                "       mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "       mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "       mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "       mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtDoubleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 " +
                "       LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 " +
                "       LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 " +
                "       LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 " +
                "       LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 " +
                "       LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 " +
                "       LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 " +
                " WHERE " +
                "       (cpType = 2 OR cpType = 3) " + 
                where + " " +
                
                // Team (Individual)
                "UNION " +
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, " +
                "       gr.grStage, gr.grName, gr.grDesc, gr.grModus, gr.grSize, " +
                "       gr.grQualRounds, gr.grNofRounds, gr.grNofMatches, " +
                "       mt.mtNr, mt.mtMS AS mtMS, mt.mtRound, mt.mtMatch, " +
                "       mt.mtTable, mt.mtDateTime, mt.mtBestOf, mt.mtMatches, MtList.mtReverse, mt.mtTimestamp, " +
                "       mt.mtWalkOverA, mt.mtWalkOverX, " +
                "       mt.mtResA, mt.mtResX, MtList.mtResA AS mtTeamResA, MtList.mtResX AS mtTeamResX, " +
                "       stA.naName AS tmAnaName, stA.tmName AS tmAtmName, stA.tmDesc AS tmAtmDesc, " +
                "       stX.naName AS tmXnaName, stX.tmName AS tmXtmName, stX.tmDesc AS tmXtmDesc, " +
                "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, " +
                "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, " +
                "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, " +
                "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, " +
                "       mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "       mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "       mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "       mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtList  " +
                "       INNER JOIN GrList gr ON MtList.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN StTeamList stA ON MtList.stA = stA.stID " +
                "       INNER JOIN StTeamList stX ON MtList.stX = stX.stID " +
                "       LEFT OUTER JOIN MtIndividualList mt ON mt.mtID = MtList.mtID " +
                "       LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 AND mtSet1.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 AND mtSet2.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 AND mtSet3.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 AND mtSet4.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 AND mtSet5.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 AND mtSet6.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 AND mtSet7.mtMS = mt.mtMS " +
                " WHERE " +
                "       cp.cpType = 4 " + 
                whereTeam +

                // Order: By Table, Time, Individual.
                // Also for Round and Match-in-Round for RR-groups with all matches
                // at the same time and table.
                " ORDER BY cp.cpName, gr.grStage, gr.grName, mt.mtRound, mt.mtMatch";

        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();
        
        // array.add(sql);
        // return array;
        
        // java.lang.System.out.println(sql);
        var connection = getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        var row;

        var lastMtTable = -1;

        while (result.next()) {
            var idx = 1;
            var cpName = result.getString(idx++);
            var cpDesc = result.getString(idx++);
            var cpType = result.getInt(idx++);
            var grStage = result.getString(idx++);
            var grName = result.getString(idx++);
            var grDesc = result.getString(idx++);
            var grModus = result.getInt(idx++);
            var grSize = result.getInt(idx++);
            var grQualRounds = result.getInt(idx++);
            var grNofRounds = result.getInt(idx++);
            var grNofMatches = result.getInt(idx++);
            var mtNr = result.getInt(idx++);
            var mtMS = result.getInt(idx++);
            var mtRound = result.getInt(idx++);
            var mtMatch = result.getInt(idx++);
            var mtTable = result.getInt(idx++);
            var mtDateTime = this.getTime(result, idx++);
            var mtBestOf = result.getInt(idx++);
            var mtMatches = result.getInt(idx++);
            var mtReverse = result.getInt(idx++);
            var mtTimestamp = this.getTime(result, idx++);
            var mtWalkOverA = result.getInt(idx++);
            var mtWalkOverX = result.getInt(idx++);
            var mtResA = result.getInt(idx++);
            var mtResX = result.getInt(idx++);
            var mtTeamResA = result.getInt(idx++);
            var mtTeamResX = result.getInt(idx++);
            var tmAnaName = result.getString(idx++);
            var tmAtmName = result.getString(idx++);
            var tmAtmDesc = result.getString(idx++);
            var tmXnaName = result.getString(idx++);
            var tmXtmName = result.getString(idx++);
            var tmXtmDesc = result.getString(idx++);
            var plAplNr = result.getInt(idx++) % 10000;
            var plAplExtID = result.getString(idx++);
            var plApsLast = result.getString(idx++);
            var plApsFirst = result.getString(idx++);
            var plAnaName = result.getString(idx++);
            var plBplNr = result.getInt(idx++) % 10000;
            var plBplExtID = result.getString(idx++);
            var plBpsLast = result.getString(idx++);
            var plBpsFirst = result.getString(idx++);
            var plBnaName = result.getString(idx++);
            var plXplNr = result.getInt(idx++) % 10000;
            var plXplExtID = result.getString(idx++);
            var plXpsLast = result.getString(idx++);
            var plXpsFirst = result.getString(idx++);
            var plXnaName = result.getString(idx++);
            var plYplNr = result.getInt(idx++) % 10000;
            var plYplExtID = result.getString(idx++);
            var plYpsLast = result.getString(idx++);
            var plYpsFirst = result.getString(idx++);
            var plYnaName = result.getString(idx++);
            var mtSets = this.getGames(result, mtReverse, idx);
            
            lastMtTable = mtTable;

            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            row.put('cpName', cpName);
            row.put('cpDesc', cpDesc);
            row.put('cpType', new java.lang.Integer(cpType));
            row.put('grStage', grStage);
            row.put('grName', grName);
            row.put('grDesc', grDesc);
            row.put('mtDateTime', mtDateTime);
            row.put('mtTimestamp', mtTimestamp);
            row.put('mtNr', new java.lang.Integer(mtNr));
            row.put('mtMS', new java.lang.Integer(mtMS));
            row.put('grModus', new java.lang.Integer(grModus));
            row.put('grSize', new java.lang.Integer(grSize));
            row.put('grNofRounds', new java.lang.Integer(grNofRounds));
            row.put('grNofMatches', new java.lang.Integer(grNofMatches));
            row.put('mtRound', new java.lang.Integer(mtRound));
            row.put('mtMatch', new java.lang.Integer(mtMatch));
            if (grQualRounds > 0 && mtRound <= grQualRounds) {
                if (grQualRounds == 1)
                    row.put('mtRoundStr', 'Qual.');
                else 
                    row.put('mtRoundStr', 'Qual. Rd: ' + mtRound);
            } else if (grModus == 1)
                row.put('mtRoundStr', "Rd: " + mtRound);
            else if ((grSize >> mtRound) == 1 && mtMatch == 1)
                row.put('mtRoundStr', "Rd: F");
            else if ((grSize >> mtRound) == 2 && mtMatch <= 2)
                row.put('mtRoundStr', "Rd: SF");
            else if ((grSize >> mtRound) == 4 && mtMatch <= 4)
                row.put('mtRoundStr', "Rd: QF");
            else if (grModus == 2)
                row.put('mtRoundStr', "Rd of " + (2 * (grSize >> mtRound)));
            else
                row.put('mtRoundStr', "Rd: " + mtRound);
            row.put('mtTable', new java.lang.Integer(mtTable));
            row.put('mtMatches', new java.lang.Integer(mtMatches));
            row.put('mtBestOf', new java.lang.Integer(mtBestOf));
            row.put('mtReverse', new java.lang.Integer(mtReverse));
            
            row.put('mtWalkOverA',new java.lang.Integer(mtWalkOverA));
            row.put('mtWalkOverX',new java.lang.Integer(mtWalkOverX));

            if (mtResA != null && mtResX != null) {
                row.put('mtResA', new java.lang.Integer(mtReverse ? mtResX : mtResA));
                row.put('mtResX', new java.lang.Integer(mtReverse ? mtResA : mtResX));
            }

            if (cpType == 4) {
                if (tmAnaName == null)
                    tmAnaName = '';
                if (tmXnaName == null)
                    tmXnaName = '';
                
                if (tmAtmName == null)
                    tmAtmName = '';
                if (tmXtmName == null)
                    tmXtmName = '';
                
                if (tmAtmDesc == null)
                    tmAtmDesc = '';
                if (tmXtmDesc == null)
                    tmXtmDesc = '';
                
                row.put('mtTeamResA', new java.lang.Integer(mtReverse ? mtTeamResX : mtTeamResA));
                row.put('mtTeamResX', new java.lang.Integer(mtReverse ? mtTeamResA : mtTeamResX));
                row.put('tmAnaName', mtReverse ? tmXnaName : tmAnaName);
                row.put('tmAtmName', mtReverse ? tmXtmName : tmAtmName);
                row.put('tmAtmDesc', mtReverse ? tmXtmDesc : tmAtmDesc);
                row.put('tmXnaName', mtReverse ? tmAnaName : tmXnaName);
                row.put('tmXtmName', mtReverse ? tmAtmName : tmXtmName);
                row.put('tmXtmDesc', mtReverse ? tmAtmDesc : tmXtmDesc);
            }

            if (mtReverse && plXpsLast != null || !mtReverse && plApsLast != null) {
                row.put('plAplNr', new java.lang.Integer(mtReverse ? plXplNr : plAplNr));
                row.put('plApsLast', mtReverse ? plXpsLast : plApsLast);
                row.put('plApsFirst', mtReverse ? plXpsFirst : plApsFirst);
                row.put('plAnaName', mtReverse ? plXnaName : plAnaName);
                row.put('plAplExtID', mtReverse ? plXplExtID : plAplExtID);
            }

            if (mtReverse && plYpsLast != null || !mtReverse && plBpsLast != null) {
                row.put('plBplNr', new java.lang.Integer(mtReverse ? plYplNr : plBplNr));
                row.put('plBpsLast', mtReverse ? plYpsLast : plBpsLast);
                row.put('plBpsFirst', mtReverse ? plYpsFirst : plBpsFirst);
                row.put('plBnaName', mtReverse ? plYnaName : plBnaName);
                row.put('plBplExtID', mtReverse ? plYplExtID : plBplExtID);
            }

            if (mtReverse && plApsLast != null || !mtReverse && plXpsLast != null) {
                row.put('plXplNr', new java.lang.Integer(mtReverse ? plAplNr : plXplNr));
                row.put('plXpsLast', mtReverse ? plApsLast : plXpsLast);
                row.put('plXpsFirst', mtReverse ? plApsFirst : plXpsFirst);
                row.put('plXnaName', mtReverse ? plAnaName : plXnaName);
                row.put('plXplExtID', mtReverse ? plAplExtID : plXplExtID);
            }

            if (mtReverse && plBpsLast != null || !mtReverse && plYpsLast != null) {
                row.put('plYplNr', new java.lang.Integer(mtReverse ? plBplNr : plYplNr));
                row.put('plYpsLast', mtReverse ? plBpsLast : plYpsLast);
                row.put('plYpsFirst', mtReverse ? plBpsFirst : plYpsFirst);
                row.put('plYnaName', mtReverse ? plBnaName : plYnaName);
                row.put('plYplExtID', mtReverse ? plBplExtID : plYplExtID);
            }
            
            row.put('mtSets', mtSets);

            array.add(row);
        }

        result.close();

        statement.close();
        // connection.close();

        // array.add(sql);
        return array;
    };
    
    
    // -----------------------------------------------------------------------
    this.listNextMatches = function(args) {
        var where = "";
        var whereTeam = "";
        
        var all = false;
        
        if (arguments.length == 0 || args.size() == 0) {
            where += " AND DAY(mt.mtDateTime) = DAY(CURRENT_TIMESTAMP)";
            whereTeam = where;
        } else {
            if ( args.get('mtTimestamp') != null )
                where += " AND mt.mtTimestamp > CAST('" + this.formatDateTime(args.get('mtTimestamp')) + "' AS DATETIME)";
            if (args.get('day') != null) 
                where += " AND DAY(mt.mtDateTime) = " + args.get('day');
            if (args.get('from') != null)
                where += " AND mt.mtDateTime >= CAST('" + this.formatDateTime(args.get('from')) + "' AS DATETIME)";
            if (args.get('to') != null)
                where += " AND mt.mtDateTime <= CAST('" + this.formatDateTime(args.get('to')) + "' AS DATETIME)";
            if (args.get('date') != null)
                where += " AND CONVERT(date, mt.mtDateTime) = '" + args.get('date') + "'";
            if (args.get('table') != null)
                where += " AND mt.mtTable= " + args.get('table');
            if (args.get('fromTable') != null)
                where += " AND mt.mtTable >= " + args.get('fromTable');
            else if (args.get('tableList') != null)
                where += " AND mt.mtTable IN (" + args.get('tableList') + ")";
            else
                where += " AND mt.mtTable >= " + Packages.countermanager.model.CounterModel.getDefaultInstance().getFromTable();
            
            if (args.get('toTable') != null)
                where += " AND mt.mtTable <= " + args.get('toTable');
            else if (args.get('tableList') != null)
                where += "";  // Dummy, tableList is handled above
            else
                where += " AND mt.mtTable <= " + Packages.countermanager.model.CounterModel.getDefaultInstance().getToTable();
            
            var hasDate = 
                    args.get('day') != null || args.get('date') != null || args.get('to') != null; 
                    // Not 'from', because without 'to' we would read to the end. Better to combine from without to with something like 'day of from'
            
            if (!hasDate)
                where += " AND DAY(mt.mtDateTime) = DAY(CURRENT_TIMESTAMP)";
            
            whereTeam = where;
            
            if (args.get('notFinished') != null && args.get('notFinished') != 0) {
                where += " AND ( (2 * mt.mtResA < mt.mtBestOf AND 2 * mt.mtResX < mt.mtBestOf) ) " +
                         " AND ( (mt.mtWalkOverA = 0 OR mt.mtWalkOverA IS NULL) AND (mt.mtWalkOverX = 0 OR mt.mtWalkOverX IS NULL) ) ";
                whereTeam += 
                        " AND ( (2 * mt.mtResA < mt.mtBestOf AND 2 * mt.mtResX < mt.mtBestOf OR mt.mtResA IS NULL AND mt.mtResX IS NULL) )" +
                        " AND ( (2 * MtList.mtResA < MtList.mtMatches AND 2 * MtList.mtResX < MTList.mtMatches) ) " +
                        " AND ( (mt.mtWalkOverA = 0 OR mt.mtWalkOverA IS NULL) AND (mt.mtWalkOverX = 0 OR mt.mtWalkOverX IS NULL) ) ";
            }
            if (args.get('notStarted') != null && args.get('notStarted') != 0) {
                where += " AND (mt.mtResA = 0 AND mt.mtResX = 0) " +
                         " AND ( (mt.mtWalkOverA = 0 OR mt.mtWalkOverA IS NULL) AND (mt.mtWalkOverX = 0 OR mt.mtWalkOverX IS NULL) ) ";
                whereTeam += " AND (mt.mtResA = 0 AND mt.mtResX = 0 AND MtList.mtResA = 0 AND MtList.mtResX = 0) " +
                             " AND ( (mt.mtWalkOverA = 0 OR mt.mtWalkOverA IS NULL) AND (mt.mtWalkOverX = 0 OR mt.mtWalkOverX IS NULL) ) ";
            }
            
            if (args.get('all') != null && args.get('all') != 0)
                all = true;
        }
        
        var sql =
                // Singles
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, " +
                "       gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, " +
                "       gr.grQualRounds, gr.grNofRounds, gr.grNofMatches, " +
                "       mtNr, 0 AS mtMS, mtRound, mtMatch, " +
                "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, " +
                "       mt.mtTimestamp, mt.mtChecked, " +
                "       mt.mtWalkOverA, mt.mtWalkOverX, " +
                "       mt.mtResA, mt.mtResX, 0 AS mtTeamResA, 0 AS mtTeamResX, " +
                "       NULL AS tmAnaName, NULL AS tmAtmName, NULL AS tmAtmDesc, " +
                "       NULL AS tmXnaNAme, NULL AS tmXtmName, NULL AS tmXtmDesc, " +
                "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, " +
                "       NULL AS plBplNr, NULL AS plBplExtId, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, " +
                "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, " +
                "       NULL AS plYplNr, NULL AS plYplExtId, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, " +
                "       mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "       mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "       mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "       mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtSingleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 " +
                "       LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 " +
                "       LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 " +
                "       LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 " +
                "       LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 " +
                "       LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 " +
                "       LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 " +
                " WHERE " + 
                "       (mt.stA IS NULL OR mt.tmAtmID IS NOT NULL) AND " +
                "       (mt.stX IS NULL OR mt.tmXtmID IS NOT NULL) AND " +
                // "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
                "       cpType = 1 AND mtDateTime IS NOT NULL " + 
                where + " " +

                // Doubles and Mixed
                "UNION " +
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, " +
                "       gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, " +
                "       gr.grQualRounds, gr.grNofRounds, gr.grNofMatches, " +
                "       mtNr, 0 AS mtMS, mtRound, mtMatch, " +
                "       mtTable, mtDateTime, mtBestOf, mtMatches, 0 AS mtReverse, " + 
                "       mt.mtTimestamp, mt.mtChecked, " +
                "       mt.mtWalkOverA, mt.mtWalkOverX, " +
                "       mt.mtResA, mt.mtResX, 0 AS mtTeamResA, 0 AS mtTeamResX, " +
                "       NULL AS tmAnaName, NULL AS tmAtmName, NULL AS tmAtmDesc, " +
                "       NULL AS tmXnaName, NULL AS tmXtmName, NULL AS tmXtmDesc, " +
                "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, " +
                "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, " +
                "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, " +
                "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, " +
                "       mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "       mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "       mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "       mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtDoubleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 " +
                "       LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 " +
                "       LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 " +
                "       LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 " +
                "       LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 " +
                "       LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 " +
                "       LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 " +
                " WHERE " +
                "       (mt.stA IS NULL OR mt.tmAtmID IS NOT NULL) AND " +
                "       (mt.stX IS NULL OR mt.tmXtmID IS NOT NULL) AND " +
                // "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
                "       (cpType = 2 OR cpType = 3) AND mtDateTime IS NOT NULL " + 
                where + " " +
                
                // Team (Individual)
                "UNION " +
                "SELECT cp.cpName, cp.cpDesc, cp.cpType, " +
                "       gr.grName, gr.grDesc, gr.grModus, gr.grSize, gr.grWinner, " +
                "       gr.grQualRounds, gr.grNofRounds, gr.grNofMatches, " +
                "       mt.mtNr, mt.mtMS AS mtMS, mt.mtRound, mt.mtMatch, " +
                "       mt.mtTable, mt.mtDateTime, mt.mtBestOf, mt.mtMatches, MtList.mtReverse, " + 
                "       mt.mtTimestamp, mt.mtChecked, " +
                "       mt.mtWalkOverA, mt.mtWalkOverX, " +
                "       mt.mtResA, mt.mtResX, MtList.mtResA AS mtTeamResA, MtList.mtResX AS mtTeamResX, " +
                "       stA.naName AS tmAnaName, stA.tmName AS tmAtmName, stA.tmDesc AS tmAtmDesc, " +
                "       stX.naName AS tmXnaName, stX.tmName AS tmXtmName, stX.tmDesc AS tmXtmDesc, " +
                "       plAplNr, plAplExtId, plApsLast, plApsFirst, plAnaName, " +
                "       plBplNr, plBplExtId, plBpsLast, plBpsFirst, plBnaName, " +
                "       plXplNr, plXplExtId, plXpsLast, plXpsFirst, plXnaName, " +
                "       plYplNr, plYplExtId, plYpsLast, plYpsFirst, plYnaName, " +
                "       mtSet1.mtResA, mtSet1.mtResX, mtSet2.mtResA, mtSet2.mtResX, " +
                "       mtSet3.mtResA, mtSet3.mtResX, mtSet4.mtResA, mtSet4.mtResX, " +
                "       mtSet5.mtResA, mtSet5.mtResX, mtSet6.mtResA, mtSet6.mtResX, " +
                "       mtSet7.mtResA, mtSet7.mtResX " +
                "  FROM MtList  " +
                "       INNER JOIN GrList gr ON MtList.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN StTeamList stA ON MtList.stA = stA.stID " +
                "       INNER JOIN StTeamList stX ON MtList.stX = stX.stID " +
                "       LEFT OUTER JOIN MtIndividualList mt ON mt.mtID = MtList.mtID " +
                "       LEFT OUTER JOIN MtSet mtSet1 ON mtSet1.mtID = mt.mtID AND mtSet1.mtSet = 1 AND mtSet1.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet2 ON mtSet2.mtID = mt.mtID AND mtSet2.mtSet = 2 AND mtSet2.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet3 ON mtSet3.mtID = mt.mtID AND mtSet3.mtSet = 3 AND mtSet3.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet4 ON mtSet4.mtID = mt.mtID AND mtSet4.mtSet = 4 AND mtSet4.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet5 ON mtSet5.mtID = mt.mtID AND mtSet5.mtSet = 5 AND mtSet5.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet6 ON mtSet6.mtID = mt.mtID AND mtSet6.mtSet = 6 AND mtSet6.mtMS = mt.mtMS " +
                "       LEFT OUTER JOIN MtSet mtSet7 ON mtSet7.mtID = mt.mtID AND mtSet7.mtSet = 7 AND mtSet7.mtMS = mt.mtMS " +
                " WHERE " +
                "       (mt.stA IS NULL OR mt.tmAtmID IS NOT NULL) AND " +
                "       (mt.stX IS NULL OR mt.tmXtmID IS NOT NULL) AND " +
                // "       plAplNr IS NOT NULL AND plXplNr IS NOT NULL AND " +
                // "       tmAtmName IS NOT NULL AND tmXtmName IS NOT NULL AND " +
                "       cp.cpType = 4 AND mt.mtDateTime IS NOT NULL " + 
                whereTeam +

                // Order: By Table, Time, Individual.
                // Also for Round and Match-in-Round for RR-groups with all matches
                // at the same time and table.
                " ORDER BY mt.mtTable, mt.mtDateTime, mt.mtNr, mtMS";


        // Ergebnis muss ein Java-Typ sein
        var array = new java.util.Vector();
        
        // array.add(sql);
        // return array;
        
        // java.lang.System.out.println(sql);
        var connection = getConnection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(sql);

        var row;

        var lastMtTable = -1;

        while (result.next()) {
            var idx = 1;
            var cpName = result.getString(idx++);
            var cpDesc = result.getString(idx++);
            var cpType = result.getInt(idx++);
            var grName = result.getString(idx++);
            var grDesc = result.getString(idx++);
            var grModus = result.getInt(idx++);
            var grSize = result.getInt(idx++);
            var grWinner = result.getInt(idx++);
            var grQualRounds = result.getInt(idx++);
            var grNofRounds = result.getInt(idx++);
            var grNofMatches = result.getInt(idx++);
            var mtNr = result.getInt(idx++);
            var mtMS = result.getInt(idx++);
            var mtRound = result.getInt(idx++);
            var mtMatch = result.getInt(idx++);
            var mtTable = result.getInt(idx++);
            var mtDateTime = this.getTime(result, idx++);
            var mtBestOf = result.getInt(idx++);
            var mtMatches = result.getInt(idx++);
            var mtReverse = result.getInt(idx++);
            var mtTimestamp = this.getTime(result, idx++);
            var mtChecked = result.getInt(idx++);
            var mtWalkOverA = result.getInt(idx++);
            var mtWalkOverX = result.getInt(idx++);
            var mtResA = result.getInt(idx++);
            var mtResX = result.getInt(idx++);
            var mtTeamResA = result.getInt(idx++);
            var mtTeamResX = result.getInt(idx++);
            var tmAnaName = result.getString(idx++);
            var tmAtmName = result.getString(idx++);
            var tmAtmDesc = result.getString(idx++);
            var tmXnaName = result.getString(idx++);
            var tmXtmName = result.getString(idx++);
            var tmXtmDesc = result.getString(idx++);
            var plAplNr = result.getInt(idx++) % 10000;
            var plAplExtID = result.getString(idx++);
            var plApsLast = result.getString(idx++);
            var plApsFirst = result.getString(idx++);
            var plAnaName = result.getString(idx++);
            var plBplNr = result.getInt(idx++) % 10000;
            var plBplExtID = result.getString(idx++);
            var plBpsLast = result.getString(idx++);
            var plBpsFirst = result.getString(idx++);
            var plBnaName = result.getString(idx++);
            var plXplNr = result.getInt(idx++) % 10000;
            var plXplExtID = result.getString(idx++);
            var plXpsLast = result.getString(idx++);
            var plXpsFirst = result.getString(idx++);
            var plXnaName = result.getString(idx++);
            var plYplNr = result.getInt(idx++) % 10000;
            var plYplExtID = result.getString(idx++);
            var plYpsLast = result.getString(idx++);
            var plYpsFirst = result.getString(idx++);
            var plYnaName = result.getString(idx++);
            var mtSets = this.getGames(result, mtReverse, idx);
            
            if (!all && mtTable == lastMtTable)
                continue;

            lastMtTable = mtTable;

            // Die Zeilen ebenfalls in einen Java-Typ einpacken
            row = new java.util.Hashtable();

            row.put('cpName', cpName);
            row.put('cpDesc', cpDesc);
            row.put('cpType', new java.lang.Integer(cpType));
            row.put('grName', grName);
            row.put('grDesc', grDesc);
            row.put('mtDateTime', mtDateTime);
            row.put('mtNr', new java.lang.Integer(mtNr));
            row.put('mtMS', new java.lang.Integer(mtMS));
            row.put('grModus', new java.lang.Integer(grModus));
            row.put('grSize', new java.lang.Integer(grSize));
            row.put('grQualRounds', new java.lang.Integer(grQualRounds));
            row.put('grNofRounds', new java.lang.Integer(grNofRounds));
            row.put('grNofMatches', new java.lang.Integer(grNofMatches));
            row.put('mtRound', new java.lang.Integer(mtRound));
            row.put('mtMatch', new java.lang.Integer(mtMatch));
            if (grQualRounds > 0 && mtRound <= grQualRounds) {
                if (grQualRounds == 1)
                    row.put('mtRoundStr', 'Qual.');
                else 
                    row.put('mtRoundStr', 'Qual. Rd: ' + mtRound);
            } else if (grModus == 1)
                row.put('mtRoundStr', "Rd: " + mtRound);
            else if (grWinner == 1 && (grSize >> mtRound) == 1 && mtMatch == 1)
                row.put('mtRoundStr', "Rd: F");
            else if (grWinner == 1 && (grSize >> mtRound) == 2 && mtMatch <= 2)
                row.put('mtRoundStr', "Rd: SF");
            else if (grWinner == 1 && (grSize >> mtRound) == 4 && mtMatch <= 4)
                row.put('mtRoundStr', "Rd: QF");
            else if (grModus == 2)
                row.put('mtRoundStr', "Rd of " + (2 * (grSize >> mtRound)));   
            else if (mtRound == 1)
                row.put('mtRoundStr', "Rd: 1");
            else if (grModus == 4) {
                var nof = grSize >> mtRound;
                var m = mtMatch - 1;
                var from = (m / nof) * nof ;
                var to   = (from + nof);
                // Ein Spiel geht um 2 Plaetze
                row.put('mtRoundStr', "Pos. " + (grWinner + 2 * from) + "&dash;" + (grWinner + 2 * to - 1));
            }           
            else
                row.put('mtRoundStr', "Rd: " + (mtRound - grQualRounds));
            row.put('mtTable', new java.lang.Integer(mtTable));
            row.put('mtMatches', new java.lang.Integer(mtMatches));
            row.put('mtBestOf', new java.lang.Integer(mtBestOf));
            row.put('mtReverse', new java.lang.Integer(mtReverse));
            
            row.put('mtTimestamp', mtTimestamp);
            row.put('mtChecked', mtChecked);
            
            row.put('mtWalkOverA',new java.lang.Integer(mtWalkOverA));
            row.put('mtWalkOverX',new java.lang.Integer(mtWalkOverX));

            if (mtResA != null && mtResX != null) {
                row.put('mtResA', new java.lang.Integer(mtReverse ? mtResX : mtResA));
                row.put('mtResX', new java.lang.Integer(mtReverse ? mtResA : mtResX));
            }

            if (cpType == 4) {
                if (tmAnaName == null)
                    tmAnaName = '';
                if (tmXnaName == null)
                    tmXnaName = '';
                
                if (tmAtmName == null)
                    tmAtmName = '';
                if (tmXtmName == null)
                    tmXtmName = '';
                
                if (tmAtmDesc == null)
                    tmAtmDesc = '';
                if (tmXtmDesc == null)
                    tmXtmDesc = '';
                
                row.put('mtTeamResA', new java.lang.Integer(mtReverse ? mtTeamResX : mtTeamResA));
                row.put('mtTeamResX', new java.lang.Integer(mtReverse ? mtTeamResA : mtTeamResX));
                row.put('tmAnaName', mtReverse ? tmXnaName : tmAnaName);
                row.put('tmAtmName', mtReverse ? tmXtmName : tmAtmName);
                row.put('tmAtmDesc', mtReverse ? tmXtmDesc : tmAtmDesc);
                row.put('tmXnaName', mtReverse ? tmAnaName : tmXnaName);
                row.put('tmXtmName', mtReverse ? tmAtmName : tmXtmName);
                row.put('tmXtmDesc', mtReverse ? tmAtmDesc : tmXtmDesc);
            }

            if (mtReverse && plXpsLast != null || !mtReverse && plApsLast != null) {
                row.put('plAplNr', new java.lang.Integer(mtReverse ? plXplNr : plAplNr));
                row.put('plApsLast', mtReverse ? plXpsLast : plApsLast);
                row.put('plApsFirst', mtReverse ? plXpsFirst : plApsFirst);
                row.put('plAnaName', mtReverse ? plXnaName : plAnaName);
                row.put('plAplExtID', mtReverse ? plXplExtID : plAplExtID);
            }

            if (mtReverse && plYpsLast != null || !mtReverse && plBpsLast != null) {
                row.put('plBplNr', new java.lang.Integer(mtReverse ? plYplNr : plBplNr));
                row.put('plBpsLast', mtReverse ? plYpsLast : plBpsLast);
                row.put('plBpsFirst', mtReverse ? plYpsFirst : plBpsFirst);
                row.put('plBnaName', mtReverse ? plYnaName : plBnaName);
                row.put('plBplExtID', mtReverse ? plYplExtID : plBplExtID);
            }

            if (mtReverse && plApsLast != null || !mtReverse && plXpsLast != null) {
                row.put('plXplNr', new java.lang.Integer(mtReverse ? plAplNr : plXplNr));
                row.put('plXpsLast', mtReverse ? plApsLast : plXpsLast);
                row.put('plXpsFirst', mtReverse ? plApsFirst : plXpsFirst);
                row.put('plXnaName', mtReverse ? plAnaName : plXnaName);
                row.put('plXplExtID', mtReverse ? plAplExtID : plXplExtID);
            }

            if (mtReverse && plBpsLast != null || !mtReverse && plYpsLast != null) {
                row.put('plYplNr', new java.lang.Integer(mtReverse ? plBplNr : plYplNr));
                row.put('plYpsLast', mtReverse ? plBpsLast : plYpsLast);
                row.put('plYpsFirst', mtReverse ? plBpsFirst : plYpsFirst);
                row.put('plYnaName', mtReverse ? plBnaName : plYnaName);
                row.put('plYplExtID', mtReverse ? plBplExtID : plYplExtID);
            }
            
            row.put('mtSets', mtSets);

            // Add flags if liveticker input is available
            row.put('ltActive', 
                Packages.countermanager.model.CounterModel.getDefaultInstance().isLivetickerActive(
                        mtTable - Packages.countermanager.model.CounterModel.getDefaultInstance().getTableOffset()
                )
            );
    
            row.put('mtService', 
                Packages.countermanager.model.CounterModel.getDefaultInstance().getServiceAX(mtTable, mtNr, mtMS)
            );
            
            array.add(row);
        }

        result.close();

        statement.close();
        // connection.close();

        // array.add(sql);
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
        if (typeof time == 'string')
            return time;
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


