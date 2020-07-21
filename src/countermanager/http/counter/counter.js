/* Copyright (C) 2020 Christoph Theis */

import * as Counter from '../scripts/modules/counter.js';
import * as CounterData from '../scripts/modules/counter_data.js';

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


// formatted names of players
var namePlA = 'Player A';
var namePlB = null;
var namePlX = 'Player X';
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
        .click(function() {
            swapDouble($(this));
        })
    ;
    
    Counter.addListener(updateScreen);
    
    $('[data-counter]').each(function() {
        $(this).on('click', function() {updateData($(this));});
    });

    restoreData();
    
    connectHttp();    
}


function connectHttp() {
    var timeout = 150; // 150ms, slightly larger than the timeout of the ajax call    
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
            lastUpdateTime : lastUpdateTime,
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
        timeout: 100, // 100 ms
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
    // Don't overwrite a running match
    if (counterData != null && counterData.gameMode != 'RESET')
        return;
    
    if (match === null)
        return;
    
    counterMatch = match;     
    
    checkPrestart();
}


function resetMatch() {
    counterMatch = null;
    counterData = CounterData.create();
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
function updateData(input) {
    const what = input.data('counter');
    Counter[what](counterData);
}

// -----------------------------------------------------------------------
function updateScreen() {
    if (counterData === null)
        counterData = CounterData.create();
    
    const swap = 
        counterData.playerNrLeft === CounterData.PlayerDefault.RIGHT ||
        counterMatch !== null && counterMatch.plA !== null && counterMatch.plA.plNr === counterData.playerNrRight;

    // Header
    $('#schedule .table').html('Table: ' + table);
    if (counterMatch) {
        $('#schedule .start').html('Start: ' + formatTime(counterMatch.mtDateTime));
        $('#schedule .event').html('Event: ' + counterMatch.cpDesc);
        
        $('#caption #nameleft').html(namePlB === null ? namePlA : namePlA + '<br>' + namePlB);
        $('#caption #nameright').html(namePlY === null ? namePlX : namePlX + '<br>' + namePlY);
    } else {
        $('#schedule .start').html('');
        $('#schedule .event').html('');
    }
    
    $('#caption #games').html(counterData.resA + '&nbsp;-&nbsp;' + counterData.resX);
    
    $('#serviceleft').attr('checked', counterData.serviceLeft);
    $('#timeoutleft').attr('checked', counterData.timeoutLeft);
    $('#woleft').attr('checked', counterData.woLeft);
    
    $('#serviceright').attr('checked', counterData.serviceRight);
    $('#timeoutright').attr('checked', counterData.timeoutRight);
    $('#woright').attr('checked', counterData.woRight);
    
    $('#yellowleft').attr('checked', counterData.cardLeft > CounterData.Cards.NONE);
    $('#yr1pleft').attr('checked', counterData.cardLeft > CounterData.Cards.YELLOW);
    $('#yr2pleft').attr('checked', counterData.cardLeft > CounterData.Cards.YR1P);
    
    $('#yellowleft').attr('checked', counterData.cardRight > CounterData.Cards.NONE);
    $('#yr1pleft').attr('checked', counterData.cardRight > CounterData.Cards.YELLOW);
    $('#yr2pleft').attr('checked', counterData.cardRight > CounterData.Cards.YR1P);
        
    for (let i = 0; i < counterData.setHistory.length; ++i) {
        $('tr#game' + (i+1) + ' td.points.left').html(counterData.setHistory[i][0]);
        $('tr#game' + (i+1) + ' td.points.right').html(counterData.setHistory[i][1]);
        
        if (i + 1 < counterData.resA + counterData.resX)
            $('#result' + (i+1)).html(counterData.setHistory[i][0] > counterData.setHistory[i][1] ? 
                '' + counterData.setHistory[i][1] : '-' + counterData.setHistory[i][0]);
    }
    
    $('#startGame').attr('checked', counterData.timeMode === CounterData.TimeMode.MATCH);
    $('#expedite').attr('checked', counterData.expedite);
    // Nothing for swap names
    $('#endMatch').attr('checked', counterData.GameMode === CounterData.GameMode.END);
}
