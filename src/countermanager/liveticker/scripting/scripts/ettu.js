/* Copyright (C) 2023 Christoph Theis */

/* global java */

var scriptOptions = {
    tvFrom : 1,
    tvTo   : 4
};

getScriptOptions = function() {
    return scriptOptions;
};


setScriptOptions = function(o) {
    scriptOptions = 0;
    return scriptOptions;
};

function tableChanged(table, matchList, dataList) {
    this.formatTVTable = function(t, m, d) {
        return formatTable(t, m, d);
    };
    
    
    this.formatOtherTable = function(t, m, d) {
        return formatTable(t, m, d);
    };
    
    
    this.formatTable = function(t, m, d) {
        if (m === null || d === null)
            return "";
                    
        if (m === undefined || d === undefined)
            return "";
        
        var ret = "" +
                "<T" + t + ">" +
                    "<FlagU1>" + formatFlagPL(m.plA, m.plB) + "</FlagU1>" +
                    "<FlagU2>" + formatFlagBD(m.plA, m.plB) + "</FlagU2>" +
                    "<NameU>"  + formatNames(m.plA, m.plB) + "</NameU>" + 
                    "<ServiceU>" + formatService(false, m, d) + "</ServiceU>" +
                    "<PointU>" + formatPoints(false, m, d) + "</PointU>" +
                    "<SetU>"   + formatGames(false, m, d) + "</SetU>" + 
                    "<CardU>"  + formatCards(false, m, d) + "</CardU>" +
                    "<FlagD1>" + formatFlagPL(m.plX, m.plY) + "</FlagD1>" +
                    "<FlagD2>" + formatFlagBD(m.plX, m.plY) + "</FlagD2>" +
                    "<NameD>"  + formatNames(m.plX, m.plY) + "</NameD>" + 
                    "<ServiceD>" + formatService(true, m, d) + "</ServiceD>" +
                    "<PointD>" + formatPoints(true, m, d) + "</PointD>" +
                    "<SetD>"   + formatGames(true, m, d) + "</SetD>" + 
                    "<CardD>"  + formatCards(true, m, d) + "</CardD>"
        ;

        if (m.cpType === 4) {  // Team
            ret += 
                "<TextUT>" + m.tmA.tmName + ".png" + "</TextUT>" +
                "<PointUT>" + m.mttmResA + "</PointUT>" +
                "<TextDT>" + m.tmX.tmName + ".png" + "</TextDT>" +
                "<PointDT>" + m.mttmResX + "</PointDT>"
            ;
        }
        
        ret += "</T" + t + ">"
            ;
        
        return ret;        
    };
    
    
    this.formatFlagPL = function(pl, bd) {
        if (pl === null || pl.naName === null || pl.naName === "")
            return "empty.png";
        
        return pl.naName + ".png";
    };
    
    this.formatFlagBD = function(pl, bd) {
        if (bd === null || bd.naName === null || bd.naName === "")
            return "empty.png";
        
        if (pl === null || pl.naName === null || pl.naName === "")
            return formatFlagPL(bd, pl);
                
        if (pl.naName === bd.naName)
            return "empty.png";
        
        return bd.naName + ".png";
    };
    
    
    this.formatNames = function(pl, bd) {
        var retPL = formatName(pl);
        var retBD = formatName(bd);
        
        if (retPL === "")
            return retBD;
        else if (retBD === "")
            return retPL;
        else
            return retPL + "/" + retBD;
    };
    
    
    this.formatName = function(pl) {
        if (pl === null || pl.psLast === null || pl.psLast === "")
            return "";
        
        return pl.psLast;
    };
    
    
    this.formatService = function(lr, m, d) {
        if (d === null || d === undefined)
            return "empty.png";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();
        var sl = swapped ? d.getServiceRight() : d.getServiceLeft();
        var sr = swapped ? d.getServiceLeft() : d.getServiceRight();
        
        return (gameMode === "RUNNING" && (lr ? sl : sr)) ? "focus.png" : "empty.png";
    };

    
    this.formatGames = function(lr, m, d) {
        if (d === null || d === undefined)
            return "0";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();

        return (swapped ^ lr) ? d.getSetsRight() : d.getSetsLeft();
    };

    
    this.formatPoints = function(lr, m, d) {
        if (d === null || d === undefined)
            return "0";
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();

        if (gameMode == 'END') {
            return setHistory[cg - 1][(swapped ^ lr) ? 1 : 0];
        } else if (gameMode == 'RUNNING') {
            return setHistory[cg][(swapped ^ lr) ? 1 : 0];            
        } else {
            return "";
        }
    };
    
    
    this.formatCards = function(lr, m, d) {
        if (d === null || d === undefined)
            return "empty.png";

        var swapped = d.isSwapped();

        return (swapped | lr) ? "empty.png" : "empty.png";
    };

    // -------------------------------------------------------------------------
    if (matchList.length != dataList.length)
       return "";
    
    var ret = [];    
    
    if ((table >= scriptOptions.tvFrom) && (table <= scriptOptions.tvTo)) {  
        // the key in matchList and dataList are java Intger, so convert JS int to Java Integer
        var t = java.lang.Integer.valueOf(table);
        var m = matchList.get(t);
        var d = dataList.get(t);

        ret.push(formatTVTable(t, m, d));
    } else {
        ret.push("<Tables>");
        
        var iter = matchList.keySet().iterator();
        while (iter.hasNext()) {
            var t = iter.next();
            var m = matchList.get(t);
            var d = dataList.get(t);

            if (m === null || d === null)
                continue;
            
            if (m === undefined || d === undefined)
                continue;
            
            if ((t >= scriptOptions.tvFrom) && (t <= scriptOptions.tvTo))
                continue;

            var r = formatOtherTable(t, m, d);
            ret.push(r);
        }

        ret.push("</Tables>");
    };
    
    return {
        file : (((table >= scriptOptions.tvFrom.valueOf()) && (table <= scriptOptions.tvTo.valueOf())) ? ("Table" + table) : "Other") + ".xml",
        content : ret.join("\n")
    };
}


