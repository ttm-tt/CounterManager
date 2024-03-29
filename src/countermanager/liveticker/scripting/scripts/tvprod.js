//@ sourceURL=F:\user\cht\CounterManager\src\countermanager\http\scripts\tvprod.js
/* Copyright (C) 2020 Christoph Theis */

function tableChanged(table, matchList, dataList) {
    this.formatRow = function(m, d) {
        var row = [];
        var i = 0;
        
        // debugger;
        
        // Timestamp
        // row.push(Date.now());
        // Use place holder instead, so we can compare with previous result
        row.push("<CURRENT_TIMESTAMP>");
        
        // Round of
        if (m.grModus == 1)
            row.push('F' + m.mtRound);
        else {
            switch (m.grSize >> (m.mtRound - 1)) {
                case 2 :
                    row.push('F');
                    break;
                case 4 :
                    row.push('SF');
                    break;
                case 8 :
                    row.push('QF');
                    break;
                default :
                    row.push('R' + (m.grSize >> (m.mtRound - 1)));
                    break;
            }
        }
        
        // cpSex
        switch (m.cpSex) {
            case 1 :
                row.push('M');
                break;                
            case 2 :
                row.push('W');
                break;
            case 3 :
                row.push('X');
                break;
        }
        
        // First player: First. Last, Nation
        row.push(m.plA.psFirst === null ? '' : m.plA.psFirst);
        row.push(m.plA.psLast === null ? '' : m.plA.psLast);
        row.push(m.plA.naName === null ? '' : m.plA.naName);
        
        // dto partner
        row.push(m.plB.psFirst === null ? '' : m.plB.psFirst);
        row.push(m.plB.psLast === null ? '' : m.plB.psLast);
        row.push(m.plB.naName === null ? '' : m.plB.naName);
        
        // dto other pair
        row.push(m.plX.psFirst === null ? '' : m.plX.psFirst);
        row.push(m.plX.psLast === null ? '' : m.plX.psLast);
        row.push(m.plX.naName === null ? '' : m.plX.naName);

        row.push(m.plY.psFirst === null ? '' : m.plY.psFirst);
        row.push(m.plY.psLast === null ? '' : m.plY.psLast);
        row.push(m.plY.naName === null ? '' : m.plY.naName);
        
        // Umpire and assistent
        row.push(m.up1.psFirst === null ? '' : m.up1.psFirst);
        row.push(m.up1.psLast === null ? '' : m.up1.psLast);
        row.push(m.up1.naName === null ? '' : m.up1.naName);
        
        row.push(m.up2.psFirst === null ? '' : m.up2.psFirst);
        row.push(m.up2.psLast === null ? '' : m.up2.psLast);
        row.push(m.up2.naName === null ? '' : m.up2.naName);
        
        // Actual score and serve
        if (d == null) {
            for (i = 0; i < 4; i++)
                row.push("");
            for (i = 0; i < 8; i++)
                row.push("");
            for (i = 0; i < 2; i++)
                row.push("");
        } else {
            var gameMode = d.getGameMode();
            var swapped = d.isSwapped();
            var setHistory = d.getSetHistory();
            var cg = d.getSetsLeft() + d.getSetsRight();
            var sl = swapped ? d.getServiceRight() : d.getServiceLeft();
            var sr = swapped ? d.getServiceLeft() : d.getServiceRight();

            // Current result, if any
            if (gameMode == 'END') {
                row.push(setHistory[cg - 1][swapped ? 1 : 0]);
                row.push(""); // Service
                row.push(setHistory[cg - 1][swapped ? 0 : 1]);
                row.push(""); // Service
                
                // Exclude last game in history
                cg = cg - 1;
            } else if (gameMode == 'RUNNING') {
                row.push(setHistory[cg][swapped ? 1 : 0]);
                row.push(sl ? "X" : ""); // Service
                row.push(setHistory[cg][swapped ? 0 : 1]);
                row.push(sr ? "X" : ""); // Service
            } else {
                row.push("");
                row.push("");
                row.push("");
                row.push("");
            }
            
            // Add 7 games
            for (i = 0; i < 7; i++) {
                if (i >= setHistory.length) {
                    row.push("");
                    row.push("");
                } else if (i >= cg) {
                    // Exclude current game
                    row.push("");
                    row.push("");
                } else {
                    row.push(setHistory[i][swapped ? 1 : 0]);
                    row.push(setHistory[i][swapped ? 0 : 1]);                
                }
            }

            // Timeout, match or set point player A
            if ((swapped ? d.isTimeoutRightRunning() : d.isTimeoutLeftRunning())) {
                row.push("T");
            } else if (cg < setHistory.length && setHistory[cg][swapped ? 1 : 0] >= 10 && setHistory[cg][swapped ? 1 : 0] > setHistory[cg][swapped ? 0 : 1]) {
                // Last point, except if we are finished or in a break
                if (d.gameMode == 'END')
                    row.push("");
                else if (d.timeMode == 'BREAK')
                    row.push("");
                else if (2 * (swapped ? d.getSetsRight() : d.getSetsLeft()) + 1 == m.mtBestOf) {
                    // Match point
                    row.push("M");
                } else {
                    // Set point
                    row.push("S");
                }
            } else {
                row.push("");
            }
            
            // And for player X
            if ((swapped ? d.isTimeoutLeftRunning() : d.isTimeoutRightRunning())) {
                row.push("T");
            } else if (cg < setHistory.length && setHistory[cg][swapped ? 0 : 1] >= 10 && setHistory[cg][swapped ? 0 : 1] > setHistory[cg][swapped ? 1 : 0]) {
                // Last point, except if we are finished or in a break
                if (d.gameMode == 'END')
                    row.push("");
                else if (d.timeMode == 'BREAK')
                    row.push("");
                else if (2 * (swapped ? d.getSetsLeft() : d.getSetsRight()) + 1 == m.mtBestOf) {
                    // Match point
                    row.push("M");
                } else {
                    // Set point
                    row.push("S");
                }
            } else {
                row.push("");
            }
        }        
        
        // Games
        row.push(m.mtResA);
        row.push(m.mtResX);
        
        // Event type
        switch (m.cpType) {
            case 1 : // Singles
                row.push('S');
                break;
            case 2 : // Doubles
                row.push('D');
                break;
            case 3 :  // Mixed (Doubles)
                row.push('D');
                break;
            case 4 : // Depends on the actual match
                if ((m.plB != null && m.plB.plNr != 0) || (m.plY != null && m.plY.plNr != 0))
                    row.push('D');
                else
                    row.push('S');
                break;
            default: // Should never happen
                row.push("");
                break;
        }
        
        // And empty field
        row.push("");
        
        return row.join(";");
    };
    
    
    if (matchList.length != dataList.length)
        return "";
    
    var ret = [];    
    
    var iter = matchList.keySet().iterator();
    while (iter.hasNext()) {
        var t = iter.next();
        var m = matchList.get(t);
        var d = dataList.get(t);
        
        if (m === null)
            continue;
     
        ret.push(formatRow(m, d) + "\n");
    }
    
    return ret.join("\n");
}


