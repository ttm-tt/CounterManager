/* Copyright (C) 2020 Christoph Theis */

/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/*
 * Parameter:
 *   table:                 Which table to display
 *   swap:                  Swap the sides
 *   compareTimeout:        Time after which a game is forcefully displayed, even without any changes
 *   timeToBlank:           Time a finished game will be shown before going blank for the next match
 *   errorTimeout:          Time after which the monitor will go blank if no data have been received
 *   flag:                  What to show as flag: none | nation | region
 *   noUpdate:              No updates of content (for debugging)
 *   showService:           Indicates who has the service
 *   nameLength:            Max. Laenge der Namen (default: alles)
 *   firstNameLength:       Max. Laenge der Voramen (default: alles)
 *   lastNameLength:        Max. Laenge der Nachnamen (default: alles)
 *   showRunningTimeout:    Flag if a running timeout is shown (default: 1)
 *   prestart:              Zeit vor Spielstart, ab wann angezeigt wird
 *   
 */

import * as CounterData from '../scripts/modules/counter_data.js';

var table = 1;
var compareTimeout = 2;
var timeToBlank = 30;
var errorTimeout = 60;
var flag = 'nation';
var currentMatch = null;
var currentData = null;
var lastUpdateTime = null;
var noUpdate = false;
var showRunningTimeout = true;
var prestart = 3600;
var nameLength = -1;
var lastNameLength = -1;
var firstNameLength = -1;
var teamNameLength = -1;
var showService = true;


$(document).ready(function() {
    compareTimeout = parseInt(getParameterByName('compareTimeout', compareTimeout));
    timeToBlank = parseInt(getParameterByName("timeToBlank", timeToBlank));
    errorTimeout = parseInt(getParameterByName('errorTimeout', errorTimeout));
    flag = getParameterByName('flag', flag);
    table = parseInt(getParameterByName('table', table));
    noUpdate = parseInt(getParameterByName("timeout", 1)) === 0;
    showService = parseInt(getParameterByName('showService', 1)) !== 0;
    showRunningTimeout = parseInt(getParameterByName('showRunningTimeout', 1)) !== 0;
    prestart = parseInt(getParameterByName('prestart', prestart));
    nameLength = getParameterByName("nameLength", nameLength);
    lastNameLength = getParameterByName("lastNameLength", nameLength);
    firstNameLength = getParameterByName("firstNameLength", nameLength);
    teamNameLength = getParameterByName("teamNameLength", nameLength);
    
    if (!showRunningTimeout) {
        $('<style>')
            .prop('type', 'text/css')
            .html('.timeouts.running {visibility: hidden !important;}')
            .appendTo('head')
        ;
    }
    
    if (typeof(localStorage) != 'undefined') {
        $(window).bind('storage', function(e) {
            if (e.originalEvent.key !== "table=" + table)
                return;
            if (e.originalEvent.newValue === null)
                return;

            var data = JSON.parse(e.originalEvent.newValue);
            if (data === null)
                return;

            onStorage(data); 
        });

        var value = localStorage.getItem("table=" + table);
        var data = value === null ? null : JSON.parse(value);
        if (data !== null)
            onStorage(data);
    }
    
    update();
});


/*
 * Invisible:
 *  1) currentData == null && currentMatch == null:  true
 *  2) currentData == 'RESET'   && currentMatch == null:  true
 *  3) currentData != 'RESET'   && currentMatch == null:  true
 *  4) currentData == null && currentMatch != null:  true
 *  5) currentData != null && currentMatch != null:  false
 */

function update() {   
    // doUpdateMatch and doUpdateData must be called in sequence, but not sync.
    // Ajax calls with async = false will block and cannot be aborted with a timeout
    doUpdateMatch();
}

export function doUpdateMatch() {
    // Next step is either called in the else branch or in the complete-callback
    if (currentData === null || currentData.gameMode == 'RESET') {
        // Any error getting currentMatch leads to case 1) and 2) above
        // Case 3) will never happen because we don't read currentMatch in this case
        // So in case of error always set to invisible
        $.ajax({
            url: '/counter/match?table=' + table,
            dataType: 'json',
            cache: false,
            timeout: 100, // 100 ms
            statusCode : {
                404: function() {
                    if (currentData === null || currentData.gameMode == 'RESET') {
                        currentMatch = null;  
                        
                        onError();
                    }
                }
            },
            error : function() {
                if (currentData === null || currentData.gameMode == 'RESET')
                    currentMatch = null;
                
                onError();
            },
            success : function(data) {
                try {
                    setCurrentMatch(data);
                } catch (err) {
                    logError(err);
                }
            },
            complete : function() {
                doUpdateData();
            }
        });
    } else {
        doUpdateData();
    }
}

export function doUpdateData() {
    // setTimeout is either called in the else branch or in the complete-callback
    if (currentData === null || currentMatch !== null) {
        // Case 4) and 5) of above: never set to invisible
        $.ajax({
            url: '/counter/data?table=' + table,
            dataType: 'json',
            cache: false,
            timeout: 100, // 100 ms
            statusCode : {
                404: function() {
                    onError();
                }
            },
            error: function() {
                onError();
            },
            success: function(data) {
                try {
                    setCurrentData(data);
                    
                    onSuccess();                    
                } catch (err) {
                    logError(err);
                }
            },
            complete : function() {
                var timeout = 1000;
                if (currentData !== null && currentData.gameMode != 'RESET')
                    timeout = 500;

                if (noUpdate === false)
                    setTimeout(function() {update();}, timeout);                
            }
        });
    } else {
        currentData = null;
        var timeout = 1000;
        if (noUpdate === false)
            setTimeout(function() {update();}, timeout);        
    }    
}

export function onError() {
    // Case 1) and 4): Only 1) set to invisible after timeout
    if (lastUpdateTime === null)
        lastUpdateTime = new Date().getTime();
    else if (lastUpdateTime < new Date().getTime() - errorTimeout * 1000)
        $('#content').addClass('invisible');                        
}


export function onSuccess() {
    // Case 2), 3), 5): only 3) and 5) set to visible
    if (currentMatch !== null) {
        lastUpdateTime = new Date().getTime();
        $('#content').removeClass('invisible');
    } else if (lastUpdateTime === null)
        lastUpdateTime = new Date().getTime();
    else if (lastUpdateTime < new Date().getTime() - errorTimeout * 1000)
        $('#content').addClass('invisible');    
}

export function onStorage(data) {
    if (data === null)
        return;
    
    // If older than one day, skip
    if (data.lastUpdateTime === null || data.lastUpdateTime < (new Date()).getTime() - 86400) 
        return;
        
    if (currentData === null || currentData.gameMode == 'RESET') {
        if (data.counterMatch !== null && data.counterMatch !== null)
            try {
                setCurrentMatch(data.counterMatch);
            } catch (err) {
                logError(err);
            }
    }
        
    if (currentData === null || currentMatch !== null) {
        if (data.counterData !== null) {
            try {   
                setCurrentData(data.counterData);
                
                onSuccess();
            } catch (err) {
                logError(err);
            }
        }
    }
}


export function getCurrentMatch() {
    return currentMatch;
}


export function setCurrentMatch(match) {   
    // Avoid unnecessary work if this is the same data as before
    if ( currentMatch !== null && match !== null && 
         currentMatch.mtNr == match.mtNr && 
         currentMatch.mtMS == match.mtMS &&
         currentMatch.mtTimestamp == match.mtTimestamp ) {
        checkPrestart();
        return;
    }

    if (currentData !== null && currentData.gameMode != 'RESET') {
        // Don't accept other matches when we are running
        // But do accept match data if this is an update of the current match,
        // e.g. player names are now known
        if ( currentMatch !== null && match !== null && 
             (match.mtNr != currentMatch.mtNr || match.mtMS != currentMatch.mtMS))
            return;
    }

    currentMatch = match;
    
    // Fields depend on currentData, so set them only if we initialize them
    if (currentData === null) {
        var swap = parseInt(getParameterByName("swap", 0));
        
        setCaption(swap);
        setNames(swap);
    }
    
    if (currentMatch.cpType == 4) {
        $('body').addClass('team');
    } else {
        $('body').removeClass('team');
    }
    
    if (currentMatch.cpType == 4)
        $('#caption').addClass('large-font');
    else
        $('#caption').removeClass('large-font');    
    
    if (currentMatch.cpType == 1)
        $('#names').addClass('large-font');
    else if (currentMatch.cpType == 2 || currentMatch.cpType == 3) 
        $('#names').removeClass('large-font');
    else if (currentMatch.plB.plNr == 0)
        $('#names').addClass('large-font');
    else
        $('#names').removeClass('large-font');
    
    $('#schedule').html(formatTime(currentMatch.mtDateTime));
    
    if (currentMatch !== null)
        $('#event span').html(currentMatch.cpDesc);

    checkPrestart();
}


export function getCurrentData() {
    return currentData;
}


export function setCurrentData(data) {

    if ( currentData !== null && data !== null ) {
        // Compare against latest data
        if (currentData.updateTime >= data.updateTime)
            return;
            
        // Avoid unnecessary work if this is the same data as before
        // XXX Replaced by the lines above
    }
 
    currentData = data;
    
    checkPrestart();
    
    // If names are swapped with respect to A/X in the match
    var swapNames = (currentData !== null) &&  currentData.swapped;

    // If left/right as seen from the umpire shall be swapped on the display
    var swap = parseInt(getParameterByName("swap", 0)) > 0;

    // Names are derived from currentMatch
    setCaption(swap ^ swapNames);        
    setNames(swap ^ swapNames);
        
    if ( (currentData === null || currentData.gameMode == 'RESET') && 
         (currentMatch === null || currentMatch.cpType != 4 || currentMatch.mtMS == 1) ) {
        $('#schedule').removeClass('hidden');
        $('#match').addClass('hidden');        
    } else {
        $('#schedule').addClass('hidden');
        $('#match').removeClass('hidden');                
    }
 
    // Test if currentData is valid
    if ( showService && currentData !== null ) {
        if (currentData.gameMode == 'RUNNING' && currentData.timeMode != 'BREAK') {
            if (swap ? currentData.serviceRight : currentData.serviceLeft)
                $('#serviceleft').removeClass('invisible');
            else
                $('#serviceleft').addClass('invisible');

            if (swap ? currentData.serviceLeft : currentData.serviceRight)
                $('#serviceright').removeClass('invisible');
            else
                $('#serviceright').addClass('invisible');
        } else {
            $('#serviceleft').addClass('invisible');
            $('#serviceright').addClass('invisible');            
        }
    } else {
        $('#serviceleft').addClass('invisible');
        $('#serviceright').addClass('invisible');                    
    }
    
    if (currentData === null) {
        $('#pointsleft').html('');
        $('#pointsright').html('');
        
        $('#gamesleft').html('');
        $('#gamesright').html('');
    } else if (currentData.gameMode == 'RESET') {
        $('#pointsleft').html('');
        $('#pointsright').html('');
        
        $('#gamesleft').html('');
        $('#gamesright').html('');
    } else if (currentData.gameMode == 'WARMUP') {
        $('#pointsleft').html('');
        $('#pointsright').html('');
        
        $('#gamesleft').html('0');
        $('#gamesright').html('0');
    } else if (currentData.setHistory.length < currentData.setsLeft + currentData.setsRight) {
        $('#pointsleft').html('0');
        $('#pointsright').html('0');        

        $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
        $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);    
    } else if (currentData.timeMode == 'BREAK') {
        var cg = currentData.setsLeft + currentData.setsRight;
        
        // For the first 30 seconds show the last game, then go blank
        if ((timeToBlank > 60 - currentData.time) || 
            (cg < currentData.setHistory.length && (currentData.setHistory[cg][0] + currentData.setHistory[cg][1])) > 0) {
            var resA = currentData.setHistory[cg][swap ? 1 : 0];
            var resX = currentData.setHistory[cg][swap ? 0 : 1];
            
            $('#pointsleft').html(resA);
            $('#pointsright').html(resX);
            
            if (resA > resX) {
                $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
                $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);
            } else {
                $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
                $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);
            }
        } else {
            $('#pointsleft').html('');
            $('#pointsright').html('');

            $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
            $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);    
        }
    } else if (currentData.gameMode == 'END') {
        $('#pointsleft').html('');
        $('#pointsright').html('');

        $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
        $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);    
    } else if (2 * currentData.setsLeft > currentData.bestOf || 2 * currentData.setsRight > currentData.bestOf) {
        var resA = currentData.setHistory[currentData.setsLeft + currentData.setsRight - 1][swap ? 1 : 0];
        var resX = currentData.setHistory[currentData.setsLeft + currentData.setsRight - 1][swap ? 0 : 1];

        $('#pointsleft').html(resA);
        $('#pointsright').html(resX);

        if (resA > resX) {
            $('#gamesleft').html((swap ? currentData.setsRight : currentData.setsLeft) - 1);
            $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);
        } else {
            $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
            $('#gamesright').html((swap ? currentData.setsLeft : currentData.setsRight) - 1);
        }
    } else if (currentData.setHistory.length == currentData.setsLeft + currentData.setsRight) {
        $('#pointsleft').html('0');
        $('#pointsright').html('0');        

        $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
        $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);    
    } else {
        $('#pointsleft').html(currentData.setHistory[currentData.setsLeft + currentData.setsRight][swap ? 1 : 0]);
        $('#pointsright').html(currentData.setHistory[currentData.setsLeft + currentData.setsRight][swap ? 0 : 1]);

        $('#gamesleft').html(swap ? currentData.setsRight : currentData.setsLeft);
        $('#gamesright').html(swap ? currentData.setsLeft : currentData.setsRight);    
    }
    
    if (currentData !== null) {
        if (swap ? currentData.timeoutRight : currentData.timeoutLeft)
            $('#timeoutleft').removeClass('hidden');
        else
            $('#timeoutleft').addClass('hidden');            
        
        if (swap ? currentData.timeoutLeft : currentData.timeoutRight)
            $('#timeoutright').removeClass('hidden');
        else
            $('#timeoutright').addClass('hidden');
        
        if (currentData.timeMode == 'TIMEOUT') {
            if (swap ^ currentData.timeoutLeftRunning)
                $('#timeoutleft').addClass('running');
            else
                $('#timeoutright').addClass('running');
        } else {
            $('#timeoutleft').removeClass('running');
            $('#timeoutright').removeClass('running');
        }
        
        switch (swap ? currentData.cardRight : currentData.cardLeft) {
            case 'NONE' :
                $('#cardleft').addClass('hidden');
                $('#cardleft').html('');
                break;
                
            case 'YELLOW' :
                $('#cardleft').removeClass('hidden');
                $('#cardleft').html('');
                break;
                
            case 'YR1P' :
                $('#cardleft').removeClass('hidden');
                $('#cardleft').html('1');
                break;
                
            case 'YR2P' :
                $('#cardleft').removeClass('hidden');
                $('#cardleft').html('2');
                break;
        }
        
        switch (swap ? currentData.cardLeft : currentData.cardRight) {
            case 'NONE' :
                $('#cardright').addClass('hidden');
                $('#cardright').html('');
                break;
                
            case 'YELLOW' :
                $('#cardright').removeClass('hidden');
                $('#cardright').html('');
                break;
                
            case 'YR1P' :
                $('#cardright').removeClass('hidden');
                $('#cardright').html('1');
                break;
                
            case 'YR2P' :
                $('#cardright').removeClass('hidden');
                $('#cardright').html('2');
                break;
        }
    }
    
    var i = 0;
    for (i = 0; i < currentData.setHistory.length; i++) {   
        if (i == currentData.setsLeft + currentData.setsRight)
            break;
        
        // Don't show last game if it is shown in the large display
        if (i == currentData.setsLeft + currentData.setsRight - 1) {
            if (currentData.timeMode == 'BREAK' && timeToBlank > 60 - currentData.time)
                break;
            
            if (2 * currentData.setsLeft > currentMatch.mtBestOf || 2 * currentData.setsRight > currentMatch.mtBestOf) {
                if (currentData.gameMode != 'END')
                    break;
            }
        }
        
        if (currentData.setHistory[i][0] || currentData.setHistory[i][1]) {
            if (swap) {
                if (currentData.setHistory[i][1] > currentData.setHistory[i][0])
                    $('#game' + (i+1)).html(currentData.setHistory[i][0]);
                else
                    $('#game' + (i+1)).html('-' + currentData.setHistory[i][1]);
            } else {
                if (currentData.setHistory[i][0] > currentData.setHistory[i][1])
                    $('#game' + (i+1)).html(currentData.setHistory[i][1]);
                else
                    $('#game' + (i+1)).html('-' + currentData.setHistory[i][0]);
                
            }
        } else {
            $('#game' + (i+1)).html('');
        }
        
        if (currentData.setHistory[i][swap ? 1 : 0] > currentData.setHistory[i][swap ? 0 : 1]) {
            $('#game' + (i+1)).addClass('invert');
        } else {
            $('#game' + (i+1)).removeClass('invert');
        }
    }
    
    for (; i < 7; i++) {
        $('#game' + (i+1)).html('');   
        $('#game' + (i+1)).removeClass('invert');
    }
}


function setCaption(swap) {
    if (currentMatch === null)
        return;
    
    var nationleft, nationright;
    var flaga = '', flagb = '', flagx = '', flagy = ''; 
    
    switch (currentMatch.cpType) {
        case 1 :
            nationleft = (swap ? currentMatch.plX.naName : currentMatch.plA.naName);
            nationright = (swap ? currentMatch.plA.naName : currentMatch.plX.naName);
            
            flaga = formatFlag(swap ? currentMatch.plX : currentMatch.plA);
            flagx = formatFlag(swap ? currentMatch.plA : currentMatch.plX);
            
            $('#teamresult span').html('');
            
            break;
            
        case 2 :
        case 3 :
            nationleft = (swap ? currentMatch.plX.naName + '&thinsp;/&thinsp;' + currentMatch.plY.naName : currentMatch.plA.naName + '&thinsp;/&thinsp;' +currentMatch.plB.naName);   
            nationright = (swap ? currentMatch.plA.naName + '&thinsp;/&thinsp;' +currentMatch.plB.naName : currentMatch.plX.naName + '&thinsp;/&thinsp;' + currentMatch.plY.naName);    

            flagb = formatFlag(swap ? currentMatch.plX : currentMatch.plA);
            flaga = formatFlag(swap ? currentMatch.plY : currentMatch.plB);
            flagx = formatFlag(swap ? currentMatch.plA : currentMatch.plX);
            flagy = formatFlag(swap ? currentMatch.plB : currentMatch.plY);
            
            $('#teamresult span').html('');

            break;
            
        case 4 :
            nationleft = (swap ? currentMatch.tmX.tmName : currentMatch.tmA.tmName);
            nationright = (swap ? currentMatch.tmA.tmName : currentMatch.tmX.tmName);
            
            flaga = formatFlag(swap ? currentMatch.tmX : currentMatch.tmA);
            flagx = formatFlag(swap ? currentMatch.tmA : currentMatch.tmX);
                
            if (swap)
                $('#teamresult span').html(currentMatch.mttmResX + '&nbsp;:&nbsp;' + currentMatch.mttmResA);
            else
                $('#teamresult span').html(currentMatch.mttmResA + '&nbsp;:&nbsp;' + currentMatch.mttmResX);
            break;
    }
    
    $('#nationleft span').html(nationleft);
    $('#nationright span').html(nationright);
    
    if ($('#flaga span').html() !== flaga)
        $('#flaga span').html(flaga);
    if ($('#flagb span').html() !== flagb)
        $('#flagb span').html(flagb);
    if ($('#flagx span').html() !== flagx)
        $('#flagx span').html(flagx);
    if ($('#flagy span').html() !== flagy)
        $('#flagy span').html(flagy);
}


function setNames(swap) {    
    if (currentMatch === null)
        return;
    
    var nameleft = formatPlayersLeft(swap);
    var nameright = formatPlayersRight(swap);
    
    $('#nameleft span').html(nameleft);
    $('#nameright span').html(nameright);
}

function formatPlayersLeft(swap) {
    var namePlA = formatName(currentMatch.plA);
    var namePlB = formatName(currentMatch.plB);
    var namePlX = formatName(currentMatch.plX);
    var namePlY = formatName(currentMatch.plY);

    // Player and partner left side
    const pl = swap ? namePlX : namePlA;
    const bd = swap ? namePlY : namePlB;
    
    if (pl === null)
        return swap ? 'Player X' : 'Player A';
    
    if (bd === null)
        return pl;
    
    if ( (currentMatch.plB === null || currentMatch.plB.plNr === 0) &&
         (currentMatch.plY === null || currentMatch.plY.plNr === 0) )
     return pl;
 
    // If no currentData exists we assume ServiceDouble.NONE
    if (currentData === null)
        return pl + '<br>' + bd;
    
    // Both player left side exist
    if (!swap) {
        switch (currentData.serviceDouble) {
            case CounterData.ServiceDouble.NONE :
            case CounterData.ServiceDouble.BX :
            case CounterData.ServiceDouble.XB :
            case CounterData.ServiceDouble.BY :
            case CounterData.ServiceDouble.YB :
                return pl + '<br>' + bd;
            default:
                return bd + '<br>' + pl;
        }
    } else {
        switch (currentData.serviceDouble) {
            case CounterData.ServiceDouble.NONE :
            case CounterData.ServiceDouble.BY :
            case CounterData.ServiceDouble.YB :
            case CounterData.ServiceDouble.AY :
            case CounterData.ServiceDouble.YA :
                return pl + '<br>' + bd;
            default:
                return bd + '<br>' + pl;   
        }
    }    
}

function formatPlayersRight(swap) {
    var namePlA = formatName(currentMatch.plA);
    var namePlB = formatName(currentMatch.plB);
    var namePlX = formatName(currentMatch.plX);
    var namePlY = formatName(currentMatch.plY);

    // Player and partner left side
    const pl = swap ? namePlA : namePlX;
    const bd = swap ? namePlB : namePlY;
    
    if (pl === null)
        return swap ? 'Player A' : 'Player X';
    
    if (bd === null)
        return pl;
        
    if ( (currentMatch.plB === null || currentMatch.plB.plNr === 0) &&
         (currentMatch.plY === null || currentMatch.plY.plNr === 0) )
     return pl;
    
    // If no currentData exists we assume ServiceDouble.NONE
    if (currentData === null)
        return pl + '<br>' + bd;
    
    // Both player left side exist
    if (!swap) {
        switch (currentData.serviceDouble) {
            case CounterData.ServiceDouble.NONE :
            case CounterData.ServiceDouble.AX :
            case CounterData.ServiceDouble.XA :
            case CounterData.ServiceDouble.BX :
            case CounterData.ServiceDouble.XB :
                return pl + '<br>' + bd;
            default:
                return bd + '<br>' + pl;
        }
    } else {
        switch (currentData.serviceDouble) {
            case CounterData.ServiceDouble.NONE :
            case CounterData.ServiceDouble.AX :
            case CounterData.ServiceDouble.XA :
            case CounterData.ServiceDouble.AY :
            case CounterData.ServiceDouble.YA :
                return pl + '<br>' + bd;
            default:
                return bd + '<br>' + pl;      
        }
    }
}


function formatName(pl) {
    // return pl.psFirst.substring(0, 1) + '.&nbsp;' + pl.psLast.substring(0, 10);
    var last = formatString(pl.psLast, lastNameLength);
    var first = formatString(pl.psFirst, firstNameLength);
    
    var name = '' + (first === '' ? last : last + '&nbsp;' + first);
    
    return formatString(name, nameLength);
}


function formatFlag(pl) {
    if (flag === 'none')
        return '';
    
    // pl is not a player, so to be on the safe side use array access
    var name = (flag === 'region' ? pl['naRegion'] : pl['naName']);
    
    if (name === undefined || name === '')
        return '';
    
    // <img> tag is not closed!
    return '<img src="' + '/flags/' + name + '.png" onerror="$(this).addClass(\'invisible\'); return false;" class="">';
}


function checkPrestart() {
    var ct = new Date();
    
    var show = 
        prestart === 0 ||
        currentData === null ||
        currentData.gameMode !== 'RESET' ||
        currentMatch === null ||
        currentMatch.mtDateTime < (ct.getTime() + prestart * 1000)
    ;

    if ($('#content').hasClass('showlogo') != !show) {
        if (show) {
            $('#content').removeClass('showlogo');
        } else {
            $('#content').addClass('showlogo');
        }
    }
}
