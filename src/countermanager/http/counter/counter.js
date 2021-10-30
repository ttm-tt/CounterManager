/* Copyright (C) 2020 Christoph Theis */

import * as Counter from '../scripts/modules/counter.js';
import * as CounterData from '../scripts/modules/counter_data.js';
import CounterSettings from '/counter/counter_settings.js';

var counterMatch = null;
var counterData = CounterData.create();
var timer = null;
var lastUpdateTime = 0;
var lastSentTime = 0;
var table = 1;
var debug = false;
var prestart = 300;
var showTimer = false;


/*
 * Parameters 
 *      debug       default 0   Enable debug fields
 *      showTimer   default 0   Show the timer
 *      table       default 1   Select table ("auto" will let the server decide which one)
 *      noCards     default 0   Hide the yellow / red cards selection
 *      prestart    default 300 When a lock screen must be removed
 */


// formatted names of teams and players
var teamA = null;
var teamX = null;
var namePlA = null;
var namePlB = null;
var namePlX = null;
var namePlY = null;

var CmdEnum = Object.freeze({
        NONE : 0,                // 0
        GET_DATA : 1,            // 1
        GET_DATA_BROADCAST : 2,  // 2
        SET_DATA : 3,            // 3
        SWAP_PLAYERS : 4,        // 4
        RESET_ALERT : 5,         // 5
        RESET : 6,               // 6
        AUTO_TABLE : 7,          // 7    
        SET_RESULT : 8,          // 8
        LOCK_SCREEN : 9,         // 9
        UNLOCK_SCREEN : 10       // 10
});

debug = getParameterByName('debug', 0) > 0;

if (debug)
    $('#debug').show();

prestart = parseInt(getParameterByName('prestart', '' + prestart));

showTimer = getParameterByName('showTimer', 0);
if (showTimer == 0)
    $('div#timer').addClass('hidden');

table = getParameterByName('table', '');

if (table === '') {
    // If no table is specified then use 'auto' to avoid an automatic redirect to table 1
    if (window.location.search === '')
        window.location.replace(window.location.href + '?table=auto');
    else
        window.location.replace(window.location.href + '&table=auto');
} else if (table === 'auto') {
    $.ajax({
        url: '../counter/command',
        type : 'POST',
        data : JSON.stringify({table: 0, command: CmdEnum.AUTO_TABLE, body: null}),
        dataType: 'json',
        success: function(data) {
            if (data <= 0) {
                alert('All connections are busy, please try again later');
                return;
            }

            window.location.replace(window.location.href.replace('table=auto', 'table=' + data));
            return;
        }
    }); 
} else {
    doInitialize();
}

function doInitialize() {
    if (getParameterByName('noCards', 0) > 0)
        $('tr.cards').hide();
    
    $('#names .name.left, #names .name.right')
        .css('cursor', 'pointer')
    ;
    
    Counter.addListener(updateScreen);
    // Store local storage
    Counter.addListener(function() {
        storeData();
    });
    
    $('[data-counter]').each(function() {
        $(this).on('click', function() {updateData($(this));});
    });
    
    restoreData();
    
    connectHttp(); 
}


function connectHttp() {
    var timeout = CounterSettings.ajaxTimeout + 50; // slightly larger than the timeout of the ajax call    
    var ct = (new Date()).getTime();
    
    if (lastUpdateTime > lastSentTime || lastSentTime < ct - 2000) {
        // Something changed or periodical updates
        send(CmdEnum.GET_DATA, JSON.stringify(counterData));
    }

    if (lastUpdateTime < ct - 2000) {
        // Periodical update lastStorage if nothing has changed
        storeData();
    }

    // If an error occured (lastSentTime set to 0( delay the next call
    if (lastSentTime === 0)
        timeout = 500;
    
    setTimeout(function() {connectHttp();}, timeout);
}

function storeData() {    
    updateLastUpdateTime();
    
    if (typeof(localStorage) !== 'undefined') {
        var gameEnabled = 0;
        for (var i = 1; i <= 7; i++) {
            if ($('#game' + i).hasClass('inactive') == false) {
                if ($('#game' + i + ' .action .plus').hasClass('disabled') == false)
                    gameEnabled = i;
                else
                    gameEnabled = -i;
            }
        }
        
        if (counterData !== null)
            counterData.updateTime = (new Date()).getTime();
        
        localStorage.setItem("table=" + table, JSON.stringify({
            counterData : counterData, 
            counterMatch : counterMatch,
            gameEnabled : gameEnabled,
            lastUpdateTime : lastUpdateTime
        }));
    }
}


function restoreData() {    
    counterData = CounterData.create();
    counterMatch = null;
    
    if (typeof(localStorage) !== 'undefined' && localStorage.getItem("table=" + table) != null) {        
        let data = null;
        
        try {
            data = JSON.parse(localStorage.getItem("table=" + table));
        } catch (e) {
            return;
        }
        
        // If older than one day, skip
        if (data.lastUpdateTime === null || data.lastUpdateTime < (new Date()).getTime() - 86400) 
            return;
        
        if (!confirm("Do you want to recover from a saved session?")) {
            clearData();
            updateScreen();
            return;
        }
        
        setMatch(data.counterMatch);
        
        // counterMatch = data.counterMatch;
        counterData = Object.assign(counterData, data.counterData);
        
        // But without the locked attribute
        counterData.locked = false;
    }        
}

function clearData() {
    if (typeof(localStorage) != 'unknown') 
        localStorage.removeItem("table=" + table);
}

// -----------------------------------------------------------------------
// Communication with server

function onMessage(msg) {
    // document.getElementById('log').value = document.getElementById('log').value + msg.toString() + '\n';
    switch (msg.command) {
        case CmdEnum.GET_DATA : 
        case CmdEnum.GET_DATA_BROADCAST : 
            getData(msg);
            break;

        case CmdEnum.SET_DATA :
            setMatch(JSON.parse(msg.data));                            
            break;

        case CmdEnum.SWAP_PLAYERS :
            swapPlayers();
            break;

        case CmdEnum.RESET_ALERT :
            resetAlert();
            break;

        case CmdEnum.RESET :
            resetMatch();
            break;
            
        case CmdEnum.SET_RESULT :
            setResult(JSON.parse(msg.data));
            break;
            
        case CmdEnum.LOCK_SCREEN :
            lockScreen();
            break;
            
        case CmdEnum.UNLOCK_SCREEN :
            unlockScreen();
            break;
    }
}

// Send data to server
function send(command, body) {
    const data = {table: table, command: command, data: body};
    
    $.ajax({
        url: '../counter/command',
        type : 'POST',
        data : JSON.stringify(data),
        dataType: 'json',
        timeout: CounterSettings.ajaxTimeout, // 200 ms
        success: function(data) {
            lastSentTime = (new Date()).getTime();
            
            if (data == null)
                return;

            for (var i = 0; i < data.length; i++)
                onMessage(data[i]);

        },
        error: function() {lastSentTime = 0;}
    });
}


// -----------------------------------------------------------------------
// Request from server: Send data
function getData(msg) {
    if (counterData.timeMode == 'RUNNING')
        counterData.gameTime = counterData.time;
    
    send(msg.command, JSON.stringify(counterData));
}


// Command from server: set game data
function setMatch(match) {
    // No (trival) changes
    if (counterMatch === null && match === null)
        return;
    
    if (counterMatch !== null && match !== null && 
            counterMatch.mtTimestamp === match.mtTimestamp)
        return;
    
    // Don't overwrite a running match
    if (counterData != null && counterData.gameMode != 'RESET') {
        // In this state we don't accept any not-a-match
        if (match === null)
            return;
        
        // Same if we don't have a match yet, because we can't know if the
        // new match is the one we are counting
        if (counterMatch === null)
            return;
        
        // We accept only the match we are counting (for names)
        if (counterMatch.mtNr !== match.mtNr || counterMatch.mtMS !== match.mtMS)
            return;

        // Detect missing player
        const swapped = counterData.swapped;
        
        const missing = 
                (namePlA === null && counterMatch.plA.plNr != 0) ||
                (namePlX === null && counterMatch.plX.plNr != 0) ||
                (namePlB === null && counterMatch.plB.plNr != 0) ||
                (namePlY === null && counterMatch.plY.plNr != 0)
        ;
        if (!missing)
            return;

        if (counterMatch.plA.plNr !== 0)
            namePlA = formatPlayer(counterMatch.plA);
        else
            namePlA = null;
        if (counterMatch.plB.plNr !== 0)
            namePlB = formatPlayer(counterMatch.plB);
        else
            namePlB = null;
        
        if (counterMatch.plX.plNr !== 0)
            namePlX = formatPlayer(counterMatch.plX);
        else
            namePlX = null;
        if (counterMatch.plY.plNr !== 0)
            namePlY = formatPlayer(counterMatch.plY);
        else
            namePlY = null;
        
        updateScreen();
        
        return;
    }
    
    counterMatch = match;   
    counterData = CounterData.create(match);

    if (match !== null) {
        if (match.cpType == 4) {
            teamA = formatTeam(match.tmA);
            teamX = formatTeam(match.tmX);
        } else {
            teamA = null;
            teamX = null;
        }
        
        namePlA = formatPlayer(match.plA);
        namePlB = formatPlayer(match.plB);
        namePlX = formatPlayer(match.plX);
        namePlY = formatPlayer(match.plY);
        
    } else {
        teamA = 'Team A';
        teamX = 'Team X';
        
        namePlA = null;
        namePlB = null;
        namePlX = null;
        namePlY = null;
    }
    
    updateScreen();
    
    checkPrestart();
}


function resetMatch() {
    counterMatch = null;
    counterData = CounterData.create();
    
    updateScreen();
}

function updateLastUpdateTime() {
    lastUpdateTime = (new Date()).getTime();
}


function lockScreen() {
    if (checkPrestart())
        return;
    
    $('body').addClass('locked');
    counterData.locked = true;
}

function unlockScreen() {
    $('body').removeClass('locked');
    counterData.locked = false;
}

var unlockClick = 0;
var unlockClickTimer = null;
function unlockAction() {
    if (unlockClickTimer !== null)
        clearTimeout(unlockClickTimer);
    
    unlockClickTimer = null;
    
    if (++unlockClick >= 5)
        unlockScreen();
    else
        unlockClickTimer = setTimeout(function() {unlockClick = 0;}, 1000);
}

function checkPrestart() {
    var unlock = 
        prestart === 0 ||
        counterData !== null && counterData.gameMode !== 'RESET' ||
        counterMatch !== null  && counterMatch.mtDateTime < (new Date()).getTime() + prestart * 1000
    ;

    if (unlock && $('body').hasClass('locked'))
        unlockScreen();
    
    return unlock;
}


// -----------------------------------------------------------------------
// Format team
function formatTeam(tm) {
    if (!tm || !tm.tmName)
        return null;
    
    return tm.tmDesc;
}


// Format players name
function formatPlayer(pl) {
    if (!pl || !pl.plNr)
        return null;
    
    var str = '' + pl.plNr + '&nbsp;';
    if (pl.psFirst != '')
        str += pl.psFirst.substring(0, 1) + '.' + '&nbsp;';
    
    if (window.matchMedia("(orientation: landscape)").matches)
        str += pl.psLast;
    else
        str += formatString(pl.psLast, 7);
    
    if (pl.naName != '')
        str += '&nbsp;' + '(' + pl.naName + ')';
    
    return str;
}


// Format player left side
function formatPlayersLeft() {
    const swap = counterData.swapped;
    // Player and partner left side
    const pl = swap ? namePlX : namePlA;
    const bd = swap ? namePlY : namePlB;
    
    if (pl === null)
        return swap ? 'Player X' : 'Player A';
    
    if (bd === null)
        return pl;
    
    // Both player left side exist
    if (!swap) {
        switch (counterData.serviceDouble) {
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
        switch (counterData.serviceDouble) {
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

// Format player left side
function formatPlayersRight() {
    const swap = counterData.swapped;
    // Player and partner left side
    const pl = swap ? namePlA : namePlX;
    const bd = swap ? namePlB : namePlY;
    
    if (pl === null)
        return swap ? 'Player A' : 'Player X';
    
    if (bd === null)
        return pl;
    
    // Both player left side exist
    if (!swap) {
        switch (counterData.serviceDouble) {
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
        switch (counterData.serviceDouble) {
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

// -----------------------------------------------------------------------
function updateData(input) {
    const what = input.data('counter');
    // Confirm some actions
    switch (what) {
        case 'setWOLeft' :
        case 'setWORight' :
        {
            let name = (what === 'setWOLeft' ? $('.name.left').html() : $('.name.right').html())
                    .replace(/&nbsp;/g, ' ' )
                    .replace(/<br>/g, ' / ')
            ;
            
            if (!confirm("Are you sure that \n    " + name + "\n*LOST* by w/o?"))
                return;
            
            break;
        }
    }
    
    Counter[what](counterData);
}

// -----------------------------------------------------------------------
function updateScreen() {
    if (counterData === null)
        counterData = CounterData.create();
    
    const cg = counterData.setsLeft + counterData.setsRight;
    const swap = counterData.swapped;

    // Header
    $('#schedule .table').html('Table: ' + table);
    if (counterMatch) {
        $('#schedule .start').html('Start: ' + formatTime(counterMatch.mtDateTime));
        $('#schedule .event').html('Event: ' + counterMatch.cpDesc);
        $('#schedule .nr').html('Match: ' + counterMatch.mtNr + (counterMatch.mtMS > 0 ? ' - Individual Match: ' + counterMatch.mtMS : ''));
        if (swap)
            $('#teamresult').html(counterMatch.mttmResX + '&nbsp;-&nbsp;' + counterMatch.mttmResA);        
        else
            $('#teamresult').html(counterMatch.mttmResA + '&nbsp;-&nbsp;' + counterMatch.mttmResX);        
    } else {
        $('#schedule .start').html('');
        $('#schedule .event').html('');
        $('#schedule .nr').html('');
        $('#teamresult').html('');        
    }
    
    if (counterMatch && counterMatch.cpType == 4) {
        // Team match
        $('#caption #teams').removeClass('hidden');
    } else {
        $('#caption #teams').addClass('hidden');
    }
    
    if (counterData.swapped) {
        $('#caption #teamleft').html(teamX);
        $('#caption #teamright').html(teamA);
    } else {
        $('#caption #teamleft').html(teamA);
        $('#caption #teamright').html(teamX);        
    }

    $('#caption #nameleft').html(formatPlayersLeft());
    $('#caption #nameright').html(formatPlayersRight());

    $('#caption #games').html(counterData.setsLeft + '&nbsp;-&nbsp;' + counterData.setsRight);
    
    $('#serviceleft').attr('checked', counterData.serviceLeft);
    $('#timeoutleft').attr('checked', counterData.timeoutLeft);
    $('#woleft').attr('checked', counterData.woLeft);
    
    $('#serviceright').attr('checked', counterData.serviceRight);
    $('#timeoutright').attr('checked', counterData.timeoutRight);
    $('#woright').attr('checked', counterData.woRight);
    
    $('#yellowleft').attr('checked', counterData.cardLeft > CounterData.Cards.NONE);
    $('#yr1pleft').attr('checked', counterData.cardLeft > CounterData.Cards.YELLOW);
    $('#yr2pleft').attr('checked', counterData.cardLeft > CounterData.Cards.YR1P);
    
    $('#yellowright').attr('checked', counterData.cardRight > CounterData.Cards.NONE);
    $('#yr1pright').attr('checked', counterData.cardRight > CounterData.Cards.YELLOW);
    $('#yr2pright').attr('checked', counterData.cardRight > CounterData.Cards.YR1P);
    
    // Games beyond bestOf are hidden
    for (let i = 0; i < 7; ++i) {
        if (i < counterData.bestOf)
            $('tr#game' + (i+1)).removeClass('hidden');
        else
            $('tr#game' + (i+1)).addClass('hidden');
    }
    
    for (let i = 0; i < counterData.setHistory.length; ++i) {
        // Set game result
        $('tr#game' + (i+1) + ' td.points.left').html(counterData.setHistory[i][0]);
        $('tr#game' + (i+1) + ' td.points.right').html(counterData.setHistory[i][1]);
        
        // Current game is active, all others are inactive, unless End Of Game
        if (counterData.gameMode === CounterData.GameMode.END)
            $('tr#game' + (i+1)).addClass('inactive');
        else if (i === cg)
            $('tr#game' + (i+1)).removeClass('inactive');
        else
            $('tr#game' + (i+1)).addClass('inactive');
        
        if (i + 1 < counterData.setsLeft + counterData.setsRight)
            $('#result' + (i+1)).html(counterData.setHistory[i][0] > counterData.setHistory[i][1] ? 
                '' + counterData.setHistory[i][1] : '-' + counterData.setHistory[i][0]);
    }
    
    if (counterData.sideChange === CounterData.SideChange.BEFORE)
        $('tr#game' + (cg + 1) + ' td.action .plus').addClass('disabled');
    else
        $('tr#game' + (cg + 1) + ' td.action .plus').removeClass('disabled');
    
    if (counterData.sideChange === CounterData.SideChange.AFTER)
        $('tr#game' + (cg + 1) + ' td.action .minus').addClass('disabled');
    else
        $('tr#game' + (cg + 1) + ' td.action .minus').removeClass('disabled');
    
    // History
    for (let i = 0; i < counterData.setHistory.length; ++i) {
        if (i >= counterData.setsLeft + counterData.setsRight)
            $('#results #result' + (i+1)).html('');
        else if (counterData.setHistory[i][0] > counterData.setHistory[i][1])
            $('#results #result' + (i+1)).html(counterData.setHistory[i][1]);
        else
            $('#results #result' + (i+1)).html('-' + counterData.setHistory[i][0]);
    }
    
    // Command buttons
    let gameRunning = 
            counterData.timeMode == CounterData.TimeMode.MATCH ||
            (counterData.setHistory.length > cg && 
             (counterData.setHistory[cg][0] + counterData.setHistory[cg][1]) >= 18 && 
              counterData.timeMode === CounterData.TimeMode.NONE)
    ;
    
    $('#startGame').attr('checked', gameRunning);
    $('#setExpedite').attr('checked', counterData.expedite);
    // Nothing for swap names
    if (counterData.hasMatchFinished())
        $('#endMatch').removeClass('disabled');
    else
        $('#endMatch').addClass('disabled');
    
    $('#endMatch').attr('checked', counterData.gameMode === CounterData.GameMode.END);
}
