/* Copyright (C) 2020 Christoph Theis */

/* global java, Packages, he */

// This is the default handler. Argument is the URI (java.net.URI)
// The handler is (must be) a class with the 3 functions
//  - pageHeader: Called to return the header of the page, e.g. write the <table> tag.
//  - pageLine:   Called for every table, e.g. write a <td> line
//  - pageFooter: Called to return the footer of the page, e.g. write the </table> end tag.

function status(args) {

    // Create the top of the page. Called once at the beginning of processing a request.
    this.pageHeader = function() {
        var noRefresh = args.get("refresh") !== null && args.get("refresh").get(0) === "no";
        
        return "<html>\r\n" +
               "<head>\r\n" + 
                     (noRefresh ? "" : "<meta http-equiv=\"refresh\" content=\"10\"/>\r\n") + 
               "</head>\r\n" +
               "<body>\r\n" + 
               "<table border=\"1\" rules=\"rows\" cellpadding=\"5\" width=\"100%\">\r\n" +
               "    <thead><tr>\r\n" +
               "        <th>Table</th>\r\n" +
               "        <th>State</th>\r\n" +
               "        <th>Remaining</th>\r\n" +
               "        <th>Time</th>\r\n" +
               "        <th colspan=\"7\">Player(s)</th>\r\n" +
               "        <th>Games</th>\r\n" +
               "        <th colspan=\"2\">Result</th>\r\n" +
               "    </tr></thead>\r\n"+
               "<tbody>\r\n";
    };



    // Create one line. Called once for each known table.
    this.pageLine = function(counterData, counterMatch, mtSets, mtGames) {
            var mtTable = counterMatch.mtTable;
            var mtTime = "";
            var mtDateTime = new Date(counterMatch.mtDateTime);
            
	    if (mtDateTime.getHours() < 10)
		    mtTime = mtTime + "0";
	    mtTime = mtTime + mtDateTime.getHours();
	    mtTime = mtTime + ":";
	    if (mtDateTime.getMinutes() < 10)
		    mtTime = mtTime + "0";
	    mtTime = mtTime + mtDateTime.getMinutes();

            var matchState = counterData == null ? "UNKNOWN" : counterData.getGameMode().toString();
	    if (matchState == "RUNNING")
		    matchState = counterData.getTimeMode().toString();

            var matchTime = counterData == null ? 0 : counterData.getTime();

                // counterData.getGameMode() == Packages.countermanager.CounterData.GameMode.RUNNING ?
                // java.lang.String.format("%d", counterData.getTime()) : "";
                
            var reverse = counterMatch.mtReverse;
            
            if (counterData != null) {
                if (counterMatch.plA.plNr == counterData.playerNrRight)
                    reverse = 1;
                else
                    reverse = 0;
            }

            var plAplNr = reverse > 0 ? counterMatch.plX.plNr : counterMatch.plA.plNr;
            var plApsLast = reverse > 0 ? counterMatch.plX.psLast : counterMatch.plA.psLast;
            var plApsFirst = reverse > 0 ? counterMatch.plX.psFirst : counterMatch.plA.psFirst;
            var plAnaName = reverse > 0 ? counterMatch.plX.naName : counterMatch.plA.naName;

            var plBplNr = reverse > 0 ? counterMatch.plY.plNr : counterMatch.plB.plNr;
            var plBpsLast = reverse > 0 ? counterMatch.plY.psLast : counterMatch.plB.psLast;
            var plBpsFirst = reverse > 0 ? counterMatch.plY.psFirst : counterMatch.plB.psFirst;
            var plBnaName = reverse > 0 ? counterMatch.plY.naName : counterMatch.plB.naName;

            var plXplNr = reverse > 0 ? counterMatch.plA.plNr : counterMatch.plX.plNr;
            var plXpsLast = reverse > 0 ? counterMatch.plA.psLast : counterMatch.plX.psLast;
            var plXpsFirst = reverse > 0 ? counterMatch.plA.psFirst : counterMatch.plX.psFirst;
            var plXnaName = reverse > 0 ? counterMatch.plA.naName : counterMatch.plX.naName;

            var plYplNr = reverse > 0 ? counterMatch.plB.plNr : counterMatch.plY.plNr;
            var plYpsLast = reverse > 0 ? counterMatch.plB.psLast : counterMatch.plY.psLast;
            var plYpsFirst = reverse > 0 ? counterMatch.plB.psFirst : counterMatch.plY.psFirst;
            var plYnaName = reverse > 0 ? counterMatch.plB.naName : counterMatch.plY.naName;

            var tmAtmDesc = reverse > 0 ? counterMatch.tmXtmDesc : counterMatch.tmAtmDesc;
            var tmXtmDesc = reverse > 0 ? counterMatch.tmAtmDesc : counterMatch.tmXtmDesc;

            var mtDateTime = java.text.DateFormat.getDateTimeInstance().format(counterMatch.mtDateTime);

	    var bg = "";
	    
	    if (matchState == "MATCH" && !counterData.getTimegameMode() && !counterData.getTimegameBlock()) {
		    if (counterData.getTime() < 30)
			    bg = " bgcolor=\"#FF0000\"";
		    else if (counterData.getTime() < 60)
			    bg = " bgcolor=\"#FFFF00\"";
	    }

            if ((plAplNr == 0 || plXplNr == 0) && (tmAtmDesc != null && tmXtmDesc != null)) {
                return "<tr" + bg + ">" +
                       "    <td align=\"right\">" + mtTable + "</td>" +
                       "    <td>" + matchState + "</td>" +
                       "    <td>" + matchTime + "</td>" +
                       "    <td>" + mtTime + "</td>" +
                       "    <td colspan=\"3\" align+\"center\">" + tmAtmDesc + "</td>" +
                       "    <td>&nbsp;-&nbsp;</td>" +
                       "    <td colspan=\"3\" align+\"center\">" + tmXtmDesc + "</td>" +
                       "    <td align=\"center\">" + mtDateTime + "</td>" +
                       "    <td align=\"center\">" + "<b>" + "" + "</b>" + "</td>" +
                       "</tr>\r\n";
            } else if (plBplNr== 0 && plYplNr == 0) {
                // Singles
                return "<tr" + bg + ">" +
                       "    <td align=\"right\">" + mtTable + "</td>" +
                       "    <td>" + matchState + "</td>" +
                       "    <td>" + matchTime + "</td>" +
                       "    <td>" + mtTime + "</td>" +
                       "    <td align=\"right\">" + plAplNr + "</td>"+
                       "    <td>" + plApsLast + ",&nbsp;" + plApsFirst + "</td>" +
                       "    <td>" + plAnaName + "</td>" +
                       "    <td>&nbsp;-&nbsp;</td>" +
                       "    <td align=\"right\">" + plXplNr + "</td>"+
                       "    <td>" + plXpsLast + ",&nbsp;" + plXpsFirst + "</td>" +
                       "    <td>" + plXnaName + "</td>" +
                       "    <td>" + mtSets + "</td>" +
                       "    <td align=\"center\">" + "<b>" + mtGames + "</b>" + "</td>" +
                       "</tr>\r\n";
            } else {
                // Doubles
                return "<tr" + bg + ">" +
                       "    <td align=\"right\">" + mtTable + "</td>" +
                       "    <td>" + matchState + "</td>" +
                       "    <td>" + matchTime + "</td>" +
                       "    <td>" + mtTime + "</td>" +
                       "    <td align=\"right\">" + plAplNr + "<br>" + plBplNr + "</td>"+
                       "    <td>" + plApsLast + ",&nbsp;" + plApsFirst + "<br>" + plBpsLast + ",&nbsp;" + plBpsFirst + "</td>" +
                       "    <td>" + plAnaName + "<br>" + plBnaName + "</td>" +
                       "    <td>&nbsp;-&nbsp;</td>" +
                       "    <td align=\"right\">" + plXplNr + "<br>" + plYplNr + "</td>"+
                       "    <td>" + plXpsLast + ",&nbsp;" + plXpsFirst + "<br>" + plYpsLast + ",&nbsp;" + plYpsFirst + "</td>" +
                       "    <td>" + plXnaName + "<br>" + plYnaName + "</td>" +
                       "    <td>" + mtSets + "</td>" +
                       "    <td align=\"center\">" + "<b>" + mtGames + "</b>" + "</td>" +
                       "</tr>\r\n";
            }
    };


    // Create the footer of the page. Called once at the end of the processing of a request.
    this.pageFooter = function() {
        return "</tbody></table></body></html>";
    };
    
    var ret = this.pageHeader();
    
    for (var i = 0; i < Packages.countermanager.model.CounterModel.getDefaultInstance().getNumberOfCounters(); i++) {
        var counterData = Packages.countermanager.model.CounterModel.getDefaultInstance().getCounterData(i);
        var counterMatch = Packages.countermanager.model.CounterModel.getDefaultInstance().getCounterMatch(i);

        if (counterMatch != null) {
            var args = [];

            args.push(counterData);
            args.push(counterMatch);

            var reverse = counterMatch.mtReverse > 0;

            var result = Packages.countermanager.model.CounterModel.getDefaultInstance().getResults(i);
            var mtSets = "";
            var mtGames = "";

            // Print "w/o", if the match is a w/o and seems not to be played at all.
            if ( counterData != null && counterData.getAbandonOrAbort() &&
                 (result[0][0] == 0 || result[0][1] == 0) )  {
                mtSets = "w/o";
            } else if (result != null) {
                for (var j = 0; j < result.length; j++) {
                    if (result[j][0] == 0 && result[j][1] == 0)
                        break;

                    if (j > 0)
                        mtSets += "&nbsp;&nbsp;";
                    mtSets += result[j][reverse ? 1 : 0] + ":" + result[j][reverse ? 0 : 1];
                }
            }

            if (result != null && counterData != null) {
                var swap = 
                        counterData.getPlayerNrLeft() == 0xFFFE ||
                        counterData.getPlayerNrLeft() == (counterMatch.mtReverse > 0 ? counterMatch.plA.plNr : counterMatch.plX.plNr);

                if (swap)
                    mtGames = counterData.getSetsRight() + ":" + counterData.getSetsLeft();
                else
                    mtGames = counterData.getSetsLeft() + ":" + counterData.getSetsRight();
            } else {
                mtGames = "";
            }

            var line = this.pageLine(counterData, counterMatch, mtSets, mtGames);
            
            ret += line;
        }
    }

    ret += this.pageFooter();
    
    return ret;
}

