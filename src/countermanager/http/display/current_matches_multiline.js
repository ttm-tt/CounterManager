/* Copyright (C) 2020 Christoph Theis */

/*
 * Parmeters
 *      config:             default 'default'
 *      timeout:            default 5           Dauer der Anzeige
 *      noUpdate:           default 0           Anzeige stehen lassen
 *      prestart:           default 3600        Zeit vor Spielstart, ab wann angezeigt wird
 *      mintime:            default 60          Mindestzeit in Sekunden, die ein fertiges Spiel angezeigt wird
 *      fromTable:          default ''          Anzeige von Tisch
 *      toTable:            default ''          Anzeige bis Tisch
 *      date:               default ''          Auswahl Datum
 *      all:                default 0           Alle Spiele anzeigen
 *      rows:               default 999         Max. Anzahl Zeilen
 *      nameLength:         default -1          Max. Laenge der Namen (default: alles)
 *      firstNameLength:    default -1          Max. Laenge der Voramen (default: alles)
 *      lastNameLength:     default -1          Max. Laenge der Nachnamen (default: alles)
 *      teamNameLength:     default -1          Max. Laenge der Teamnamen (default: alles)
 *      showTeams:          default 0           Mannschaften statt Spieler anzeigen
 *      flag:               default 'nation'    Choose either 'none', 'nation' or 'region' to show the flag
 *      showService:        default 1           Show who has the srevice
 */

var args = {};

var noUpdate = false;
var nameLength = -1;
var lastNameLength = -1;
var firstNameLength = -1;
var teamNameLength = -1;
var flag = 'nation';
var showService = false;
var minTime = 60;    // [s]
var prestart = 3600; // [s]

var matches = [];
var mtTimestamp = 0;

import * as Matches from '../scripts/modules/current_matches.js';

noUpdate = parseInt(getParameterByName("timeout", 1)) === 0;
nameLength = getParameterByName("nameLength", nameLength);
lastNameLength = getParameterByName("lastNameLength", nameLength);
firstNameLength = getParameterByName("firstNameLength", nameLength);
teamNameLength = getParameterByName("teamNameLength", nameLength);
flag = getParameterByName("flag", "nation");
showService = getParameterByName("showService", 1) != 0;
minTime = getParameterByName("minTime", minTime);
prestart = getParameterByName("prestart", prestart);

if (getParameterByName('debug', 0) != 0)
    Matches.setDebug(true);

// Set configuration
Matches.setConfig({minTime: minTime, prestart: prestart});

if (parent != undefined && parent.loadFromCache != undefined) {
    var data = parent.loadFromCache();
    if (data != undefined && data.length > 0) {
        data = JSON.parse(data);
        Matches.rebuild(matches, data.matches);
        mtTimestamp = data.mtTimestamp;
    }
}

args = {
    'all': getParameterByName('all', 1),
    'notFinished': 1,
    'notStarted': 0
};

if (getParameterByName('date', '') != '')
    args['date'] = getParameterByName('date', formatDate(new Date()));

if (getParameterByName('fromTable', 0) != 0)
    args['fromTable'] = getParameterByName('fromTable', 0);

if (getParameterByName('toTable', 0) != 0)
    args['toTable'] = getParameterByName('toTable', 0);

update(args);

function update(args) {
    if (parent !== null && parent != this && parent.show !== undefined && !parent.show())
        return;
    
    // Not beyond prestart seconds in the future
    args['to'] = (new Date()).getTime() + prestart * 1000;

    xmlrpc(
            "../RPC2", "ttm.listNextMatches", [args],
            function success(data) {
                Matches.rebuild(matches, data);
                mtTimestamp = Matches.updateMtTimestamp(data, mtTimestamp);
            },
            function error(err) {},
            function final() {show(0, mtTimestamp);}
    );
}

function show(start, mtTimestamp) {
    if (size(matches) === 0) {
        setTimeout(function() {update(args);}, 2000);
        return;
    }

    if (start >= size(matches)) {
        // Reload
        var from = undefined;
        for (var i in matches) {
            if (matches[i] == undefined || matches[i][0] == undefined)
                continue;
            
            if (Matches.isFinished(matches[i][0]))
                continue;

            if (from == undefined || from > matches[i][0].mtDateTime)
                from = matches[i][0].mtDateTime;
        }

        // Attribute zuruecksetzen
        args['all'] = getParameterByName('all', 1);
        delete args['notFinished'];
        delete args['mtTimestamp'];
        if (from !== undefined)
            args['from'] = from;
        else
            delete args['from'];
        
        // But not beyond prestart seconds in the future
        args['to'] = (new Date()).getTime() + prestart * 1000;

        update(args);

        return;
    }

    // Get latest matches
    args['mtTimestamp'] = mtTimestamp;

    xmlrpc("../RPC2", "ttm.listNextMatches", [args],
        function success(data) {
            Matches.updateResult(matches, data);
            mtTimestamp = Matches.updateMtTimestamp(data, mtTimestamp);
        },
        function error(e) {},
        function final() {doShow(start, mtTimestamp);}
    );
}

function doShow(start, mtTimestamp) {
    if (parent != undefined && parent.storeInCache != undefined)
        parent.storeInCache(JSON.stringify({'matches': matches, 'mtTimestamp': mtTimestamp}));

    $('#table tbody').empty();

    var matchCount = 0;
    var count = getParameterByName('rows', 999);
    var tables = Object.keys(matches);

    while (count > 0 && start < tables.length) {
        var table = tables[start];

        if (matches[table] === null){
            ++start;
            continue;
        }
        
        var mt = matches[table][0];
        
        if (mt === null || mt === undefined) {
            ++start;
            continue;
        }
        
        // Ignore matches without table
        if (mt.mtTable == 0) {
            ++start;
            continue;
        }

        var tr = formatMatch(mt, ((++matchCount % 2) == 0 ? 'even' : 'odd'));
        $('#table tbody').append(tr);

        if ($('#table tbody').height() > document.documentElement.clientHeight - 10) {
            $('#table tbody tr.last').remove();
            break;
        }

        $('#table tbody tr').removeClass('last');

        start += 1;
        count -= 1;

        if (start === tables.length)
            break;
    }

    if (noUpdate)
        return;
    
    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 3 * matchCount + 1) * 1000;
    setTimeout(function() { show(start); }, timeout);
}


// ----------------------------------------------------------------
// Helpers

function formatMatch(mt, clazz) {
    var ret = '';

    var caption = '<tr class="caption last">';
    caption += '<td class="table" colspan="' + (flag === 'none' ? '1' : '2') + '">' + 'T.&nbsp;' + mt.mtTable + '</td>';
    if (showService) {
        caption += '<td class="playerservice"></td>';
    }
    caption += '<td class="time">' + formatTime(mt.mtDateTime) + '</td>';
    caption += '<td class="event">' + mt.cpName + '&nbsp;' + mt.grDesc + '&nbsp;' + formatRound(mt) + '</td>';
    caption += '<td class="points">' + 'Pts' + '</td>';
    caption += '<td class="games">' + 'Gms' + '</td>';
    if (mt.cpType == 4)
        caption += '<td class="matches">' + 'Mts' + '</td>';
    caption += '</tr>';

    var singleOrDouble = (mt.plBnaName != undefined || mt.plYnaName != undefined) ? 'double' : 'single';
    var left = '<tr class="players left last ' + clazz + ' ' + singleOrDouble + '">';
    var right = '<tr class="players right last ' + clazz + ' ' + singleOrDouble + '">';

    if (flag !== 'none') {
        left += '<td class="flag">';
        right += '<td class="flag">';
        
        var prop = flag === 'region' ? 'naRegion' : 'naName';
        
        if (mt['plA' + prop] !== undefined)
            left += formatFlag(mt['plA' + prop]);
        else if (mt.cpType === 4 && mt['tmA' + prop] !== undefined)
            left += formatFlag(mt['tmA' + prop]);
        
        if (mt['plB' + prop] !== undefined && mt['plB' + prop] !== '')
            left += '<br>' + formatFlag(mt['plB' + prop]);
    
        if (mt['plX' + prop] !== undefined)
            right += formatFlag(mt['plX' + prop]);
        else if (mt.cpType === 4 && mt['tmX' + prop] !== undefined)
            right += formatFlag(mt['tmX' + prop]);
        
        if (mt['plY' + prop] !== undefined && mt['plY' + prop] !== '')
            right += '<br>' + formatFlag(mt['plY' + prop]);
        
        left += '</td>';
        right += '</td>';
    }
    
    left += '<td class="assoc">';
    if (mt.plAnaName !== undefined)
        left += formatAssoc(mt.plAnaName);
    else if (mt.cpType === 4 && mt.tmAnaName !== undefined)
        left += formatAssoc(mt.tmAnaName);
        
    if (mt.plBnaName !== undefined && mt.plBnaName !== '')
        left += '<br>' + formatAssoc(mt.plBnaName);
    
    left += '</td>';

    right += '<td class="assoc">';
    if (mt.plXnaName !== undefined)
        right += formatAssoc(mt.plXnaName);
    else if (mt.cpType === 4 && mt.tmXnaName !== undefined)
        right += formatAssoc(mt.tmXnaName);
    
    if (mt.plYnaName !== undefined && mt.plYnaName !== '')
        right += '<br>' + formatAssoc(mt.plYnaName);
    right += '</td>';
    
    // Show service
    if (showService) {
        left += '<td class="playerservice ' + (showService && mt.mtService < 0 ? 'service' : '') + '"><div></div></td>';
        right += '<td class="playerservice '  + (showService && mt.mtService > 0 ? 'service' : '') + '"><div></div></td>';
    }
    
    left += '<td colspan="2" class="names">';
    right += '<td colspan="2" class="names">';
    
    if (mt.cpType == 4 && getParameterByName('showTeams', 0) == 1) {
        left += formatTeam(mt.tmAtmDesc);
        right += formatTeam(mt.tmXtmDesc);
    } else {
        if (mt.plAplNr != 0) {
            left += formatPlayer(mt.plApsLast, mt.plApsFirst);

            if (mt.plBplNr != undefined)
                left += '<br>' + formatPlayer(mt.plBpsLast, mt.plBpsFirst);
        }

        if (mt.plXplNr != 0) {
            right += formatPlayer(mt.plXpsLast, mt.plXpsFirst);

            if (mt.plYplNr != undefined)
                right += '<br>' + formatPlayer(mt.plYpsLast, mt.plYpsFirst);
        }
    }
    
    left += '</td>';
    right += '</td>';
    
    var mtResA = mt.mtResA, mtResX = mt.mtResX;

    if (!Matches.isStarted(mt)) {
        left += '<td class="points"></td>';
        right += '<td class="points"></td>';

        left += '<td class="games"></td>';
        right += '<td class="games"></td>';
    } else {
        if (!mt.ltActive) {
            // Without liveticker we don't have good values for the points
            // In this case stick with the games
            left += '<td class="points"></td>';
            right += '<td class="points"></td>';            
        } else if (mt.mtResult === undefined || mt.mtResult === null || mt.mtResult.length === 0) {
            // Nothing, because we don't know any better
            left += '<td class="points"></td>';
            right += '<td class="points"></td>';
        } else if (mt.mtResult.length <= (mt.mtResA + mt.mtResX)) {
            // E.g. final result when all matches have been played
            // Take the last result
            left += '<td class="points">' + mt.mtResult[mt.mtResult.length - 1][0] + '</td>';
            right += '<td class="points">' + mt.mtResult[mt.mtResult.length - 1][1] + '</td>'; 

            // If the match has just finished show games as if we are still in the last game
            if (mtResA == mt.mtBestOf)
                --mtResA;
            if (mtResX == mt.mtBestOf)
                --mtResX;
        } else if (!mt.mtGameRunning) {
            if (mt.mtResA == 0 && mt.mtResX == 0) {
                left += '<td class="points"></td>';
                right += '<td class="points"></td>';                 
            } else {
                // Start of next game or match finished
                left += '<td class="points">' + mt.mtResult[mt.mtResA + mt.mtResX - 1][0] + '</td>';
                right += '<td class="points">' + mt.mtResult[mt.mtResA + mt.mtResX - 1][1] + '</td>'; 
            }

            // If the match has just finished show games as if we are still in the last game
            if (mtResA == mt.mtBestOf)
                --mtResA;
            if (mtResX == mt.mtBestOf)
                --mtResX;
        } else {
            // During a game
            left += '<td class="points">' + mt.mtResult[mt.mtResA + mt.mtResX][0] + '</td>';
            right += '<td class="points">' + mt.mtResult[mt.mtResA + mt.mtResX][1] + '</td>';
        }

        // Games, which may have been changed above
        left += '<td class="games">' + mtResA + '</td>';
        right += '<td class="games">' + mtResX + '</td>';
    }

    if (mt.cpType == 4) {
        if (Matches.isStarted(mt) || mt.mttmResA > 0 || mt.mttmResX > 0) {
            left += '<td class="matches">' + mt.mttmResA + '</td>';
            right += '<td class="matches">' + mt.mttmResX + '</td>';
        } else {
            left += '<td class="matches"></td>';
            right += '<td class="matches"></td>';
        }
    }        
    
    
    left += '</tr>';
    right += '</tr>';

    ret += caption + left + right;

    return ret;
}

function formatTeam(team) {
    return formatString(team, teamNameLength, '\u2026');
}


function formatPlayer(psLast, psFirst) {
    var last = formatString(psLast, lastNameLength);
    var first = formatString(psFirst, firstNameLength);
    
    return '' +
        '<span class="last">' + last + '</span>' +
        '<span class="first">' + (first !== undefined && first !== '' ? ', ' : '') + first + '</span>'
    ;
}


function formatFlag(name) {
    if (name === undefined || name == '' || flag === 'none')
        return '';

    return '<img src="' + '../flags/' + name + '.png"></img>';
}

function formatAssoc(name) {
    if (name === undefined || name == '')
        return '';

    return '<span class="assoc">' + name + '</span>';
}

