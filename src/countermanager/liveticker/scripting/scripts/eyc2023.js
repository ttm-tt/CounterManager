/* Copyright (C) 2023 Christoph Theis */

/* global java */

var scriptOptions = {
    
};

getScriptOptions = function() {
    return scriptOptions;
};


setScriptOptions = function(so) {
    scriptOptions = so;
    return scriptOptions;
};


function tableChanged(table, matchList, dataList) {

    this.formatMatchA = function(m, d) {
        if (m === null)
            return "0";

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
            return setHistory[cg - 1][swapped ? 1 : 0];
        } else if (gameMode == 'RUNNING') {
            return setHistory[cg][swapped ? 1 : 0];
        } else if (gameMode == 'BREAK') {
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
    
    this.formatTable = function(t, m, d) {
            if (m === null || d === null)
            return "";
                    
        if (m === undefined || d === undefined)
            return "";
        
        var ret = {};
    
        ret.table = t;
        
        if (m.tmA !== undefined && m.tmA.tmName !== undefined)
            ret.tmAtmName = m.tmA.tmName;
        if (m.tmX !== undefined && m.tmX.tmName !== undefined)
            ret.tmXtmName = m.tmX.tmName;
        
        ret.plAnaName = m.plA.naName;
        ret.plApsLast = m.plA.psLast;
        ret.plApsFirst = m.plA.psFirst;

        if (m.plB !== undefined) {
            ret.plBnaName = m.plB.naName;
            ret.plBpsLast = m.plB.psLast;
            ret.plBpsFirst = m.plB.psFirst;            
        }
        
        ret.plXnaName = m.plX.naName;
        ret.plXpsLast = m.plX.psLast;
        ret.plXpsFirst = m.plX.psFirst;
        
        if (m.plY !== undefined) {
            ret.plYnaName = m.plY.naName;
            ret.plYpsLast = m.plY.psLast;
            ret.plYpsFirst = m.plY.psFirst;            
        }

        if (m.cpType == 4) {
            ret.tmmtResA = formatMatchA(m, d);
            ret.tmmtResX = formatMatchX(m, d);
        }
        
        ret.mtResA = formatGameA(m, d);
        ret.mtResX = formatGameX(m, d);
        
        ret.resA = formatPointA(m, d);
        ret.resX = formatPointX(m, d);
        
        ret.service = formatService(m, d);
        
        return ret;
    };
    
    this.formatService = function(m, d) {
        if (d === null || d === undefined)
            return '';
        
        var gameMode = d.getGameMode();
        var swapped = d.isSwapped();
        var setHistory = d.getSetHistory();
        var cg = d.getSetsLeft() + d.getSetsRight();
        var sl = swapped ? d.getServiceRight() : d.getServiceLeft();
        var sr = swapped ? d.getServiceLeft() : d.getServiceRight();
        
        // Must be != and not !==, or the comparision does not work
        if (gameMode != 'RUNNING')
            return '';
        
        if (sl)
            return 'serviceL.png';
        else if (sr)
            return 'serviceR.png';
        else
            return '';
    };
    
    
    // -------------------------------------------------------------------------
    if (matchList.length != dataList.length)
       return "";
    
    var ret = [];    
    
    var t = java.lang.Integer.valueOf(table);
    var m = matchList.get(t);
    var d = dataList.get(t);
    
    ret.push(formatTable(t, m, d));

    return {
        file : 'table' + table + '.js',
        content : JSON.stringify(ret)
    };
};



