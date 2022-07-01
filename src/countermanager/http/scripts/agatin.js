//@ sourceURL=F:\user\cht\CounterManager\src\countermanager\http\scripts\agatin.js
/* Copyright (C) 2020 Christoph Theis */

function counterChanged(matchList, dataList) {
    this.formatRow = function(m, d) {
        if (m === null)
            return "";
        
        switch (m.cpType) {
            case 1 : // Single
                return "<type>single</type><single>" + formatSingle(m, d) + "</single>";
                
            case 2 : // Double
            case 3 : // Mixed
                return "<type>pairs</type><pairs>" + formatPair(m, d) + "</pairs>";
                
            case 4 : // Team
                if (m.nmType == 1)
                    return "<type>team</type><team>" + formatTeam(m, d) + "</team>";
                else if (m.nmType == 2)
                    return "<type>teampairs</type><teampairs>" + formatTeamPairs(m, d) + "</teampairs>";
                else // not possible
                    return "";
                
        }
        return "";
    };
    
    this.formatSingle = function(m, d) {
        return "" +
            "<P1T1Country>" + formatPlayerCountry(m.plA, true) + "</P1T1Country>" +
            "<P1T2Country>" + formatPlayerCountry(m.plX, true) + "</P1T2Country>" +
            "<T1set>" + formatGameA(m, d) + "</T1set>" +
            "<T1point>" + formatPointA(m, d) + "</T1point>" +
            "<T2set>" + formatGameX(m, d) + "</T2set>" +
            "<T2point>" + formatPointX(m, d) + "</T2point>"
        ;
    };
        
    this.formatPair = function(m, d) {
        return "" +
            "<P1T1Country>" + formatPlayerCountry(m.plA, false) + "</P1T1Country>" +
            "<P2T1Country>" + formatPlayerCountry(m.plB, false) + "</P2T1Country>" +
            "<P1T2Country>" + formatPlayerCountry(m.plX, false) + "</P1T2Country>" +
            "<P2T2Country>" + formatPlayerCountry(m.plY, false) + "</P2T2Country>" +
            "<T1set>" + formatGameA(m, d) + "</T1set>" +
            "<T1point>" + formatPointA(m, d) + "</T1point>" +
            "<T2set>" + formatGameX(m, d) + "</T2set>" +
            "<T2point>" + formatPointX(m, d) + "</T2point>"
        ;        
    };
        
    this.formatTeam = function(m, d) {
        return "" +
            "<P1T1>" + formatPlayer(m.plA, true) + "</P1T1>" +
            "<P1T2>" + formatPlayer(m.plX, true) + "</P1T2>" +
            "<T1set>" + formatGameA(m, d) + "</T1set>" +
            "<T1point>" + formatPointA(m, d) + "</T1point>" +
            "<T2set>" + formatGameX(m, d) + "</T2set>" +
            "<T2point>" + formatPointX(m, d) + "</T2point>" +
            "<T1name>" + (m.tmA === null ? "" : m.tmA.tmName) + "</T1name>" +
            "<T2name>" + (m.tmX === null ? "" : m.tmX.tmName) + "</T2name>" +
            "<T1score>" + formatMatchA(m, d) + "</T1score>" +
            "<T2score>" + formatMatchX(m, d) + "</T2score>"
        ;
    };
        
    this.formatTeamPairs = function(m, d) {
        return "" +
            "<P1T1>" + formatPlayer(m.plA, false) + "</P1T1>" +
            "<P2T1>" + formatPlayer(m.plB, false) + "</P2T1>" +
            "<P1T2>" + formatPlayer(m.plX, false) + "</P1T2>" +
            "<P2T2>" + formatPlayer(m.plY, false) + "</P2T2>" +
            "<T1set>" + formatGameA(m, d) + "</T1set>" +
            "<T1point>" + formatPointA(m, d) + "</T1point>" +
            "<T2set>" + formatGameX(m, d) + "</T2set>" +
            "<T2point>" + formatPointX(m, d) + "</T2point>" +
            "<T1name>" + (m.tmA === null ? "" : m.tmA.tmName) + "</T1name>" +
            "<T2name>" + (m.tmX === null ? "" : m.tmX.tmName) + "</T2name>" +
            "<T1score>" + formatMatchA(m, d) + "</T1score>" +
            "<T2score>" + formatMatchX(m, d) + "</T2score>"
        ;        
    };
    
    this.formatMatchA = function(m, d) {
        if (m === null)
            return "0";
// debugger;
        return m.mttmResA;
    };
    
    this.formatMatchX = function(m, d) {
        if (m === null)
            return "0";

        return m.mttmResX;
    };
    
    this.formatGameA = function(m, d) {
        if (d === null)
            return "0";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();

        return swapped ? d.getSetsRight() : d.getSetsLeft();
    };
    
    this.formatGameX = function(m, d) {
        if (d === null)
            return "0";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();

        return swapped ? d.getSetsLeft() : d.getSetsRight();        
    };
    
    this.formatPointA = function(m, d) {
        // debugger;
        if (d === null)
            return "0";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();

        if (gameMode == 'END') {
            return ""; // setHistory[cg - 1][swapped ? 1 : 0];
        } else if (gameMode == 'RUNNING') {
            return setHistory[cg][swapped ? 1 : 0];            
        } else {
            return "";
        }
    };
    
    this.formatPointX = function(m, d) {
        if (d === null)
            return "0";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();

        if (gameMode == 'END') {
            return ""; // setHistory[cg - 1][swapped ? 0 : 1];
        } else if (gameMode == 'RUNNING') {
            return setHistory[cg][swapped ? 0 : 1];            
        } else {
            return "";
        }        
    };
    
    this.formatPlayerCountry = function(pl, firstname) {
        if (pl === null)
            return "";
        if (pl.psLast === null || pl.psLast === "")
            return "";
        
        return formatPlayer(pl, firstname) + " (" + pl.naName + ")";
    };
    
    this.formatPlayer = function(pl, firstname) {
        if (pl === null)
            return "";
        if (pl.psLast === null || pl.psLast === "")
            return "";

        if (firstname && pl.psFirst !== null && pl.psFirst !== "")
            return pl.psFirst.substring(0, 1) + ". " + pl.psLast;
        else
            return pl.psLast;
    };
        
    if (matchList.length != dataList.length)
        return "";
    
    var ret = [];    
    ret.push("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
    ret.push("<table>");
    
    for (var i = 0; i < matchList.length; i++) {
        var m = matchList[i];
        var d = dataList[i];
        
        if (m === null)
            continue;
     
        var r = formatRow(m, d);
        ret.push("<t" + m.mtTable + ">" + r + "</t" + m.mtTable + ">");
    }
    
    ret.push("</table>");
    
    return ret.join("\n");
}
