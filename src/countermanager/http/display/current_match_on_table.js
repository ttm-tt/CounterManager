/* Copyright (C) 2020 Christoph Theis */

/*
 * Parameter
 * timeout: time to show each page
 * all:    0: nur aktuelles Spiel, 1: auch die naechsten Spiele
 * date:   Auswahl Datum (yyyy-mm-dd)
 * day:    Auswahl Tag
 * prestart:  default 3600        Zeit vor Spielstart, ab wann angezeigt wird
 * mintime:    default 60          Mindestzeit in Sekunden, die ein fertiges Spiel angezeigt wird
 * fromTable: Ab Tisch
 * toTable:   Bis Tisch
 * table:     Nur Tisch
 * tableList: Liste von Tichen
 * flag: Choose either 'none', 'nation' or 'region' to show the flag
 * noUpdate: Anzeige stehen lassen
 * nameLength: Max. Laenge der Namen (default: alles)
 * firstNameLength: Max. Laenge der Voramen (default: alles)
 * lastNameLength:  Max. Laenge der Nachnamen (default: alles)
 * teamNameLength:  Max. Laenge der Teamnamen (default: alles)
 */

var args = {};

var nameLength = 0;
var lastNameLength = 0;
var firstNameLength = 0;
var teamNameLength = 0;
var flag = 'nation';
var minTime = 60;
var prestart = 3600;

var matches = [];
var mtTimestamp = 0;

import * as Matches from '../scripts/modules/current_matches.js';

nameLength = getParameterByName("nameLength", 0);
lastNameLength = getParameterByName("lastNameLength", nameLength);
firstNameLength = getParameterByName("firstNameLength", nameLength);
teamNameLength = getParameterByName("teamNameLength", nameLength);
flag = getParameterByName("flag", "nation");
minTime = getParameterByName("minTime", minTime);
prestart = getParameterByName("prestart", prestart);

// Set configuration
Matches.setConfig({minTime: minTime, prestart: prestart});

args = {
    'all' : getParameterByName('all', 1),
    'notFinished' : 1, 
    'notStarted' : 0                     
};

if (getParameterByName('date', '') != '')
    args['date'] = getParameterByName('date', formatDate(new Date()));

if (getParameterByName('fromTable', 0) != 0)
    args['fromTable'] = getParameterByName('fromTable', 0);

if (getParameterByName('toTable', 0) != 0)
    args['toTable'] = getParameterByName('toTable', 0);

if (getParameterByName('table', 0) != 0)
    args['table'] = getParameterByName('table', 0);

if (getParameterByName('tableList', '') != '')
    args['tableList'] = getParameterByName('tableList');

if (parent != undefined && parent.loadFromCache != undefined) {
    var data = parent.loadFromCache();
    if (data != undefined && data.length > 0) {
        data = JSON.parse(data);
        
        Matches.rebuild(matches, data.matches);
        args['mtTimestamp'] = data.mtTimestamp;
    }
}

// TODO: Nur bis jetzt, also 'to' : date anhaengen.
update(args );

function update(args) {
    if (parent !== this && parent.show !== undefined && !parent.show())
        return;

    xmlrpc(
            "../RPC2", "ttm.listNextMatches", [args],
            function success(data) {
                Matches.rebuild(matches, data);
                mtTimestamp = Matches.updateMtTimestamp(data, mtTimestamp);
            },
            function error(err) {},
            function final() {show(0, 0, mtTimestamp);}
    );
}


function show(start, idx, mtTimestamp) {
    if (size(matches) === 0) {
        setTimeout(function() {update(args);}, 2000);
        return;
    }

    if (start >= size(matches)) {
        // Reload
        var from = null;
        for (const i in matches) {
            if (matches[i] === null)
                continue;

            if (from === null || from > matches[i].mtDateTime)
                from = matches[i].mtDateTime;
        }

        // Reset attribute
        args['all'] = getParameterByName('all', 1);
        delete args['mtTimestamp'];
        delete args['notFinished'];
        if (from !== undefined)
            args['from'] = from;
        else
            delete args['from'];

        // TODO: to berechnen
        update(args);

        return;
    }

    // Get latest matches
    args['mtTimestamp'] = mtTimestamp;

    // But with finished matches, too
    delete args['notFinished'];

    xmlrpc("../RPC2", "ttm.listNextMatches", [args],
        function success(data) {
            Matches.updateResult(matches, data);
            mtTimestamp = Matches.updateMtTimestamp(data, mtTimestamp);
        },
        function error(e) {},
        function final() {doShow(start, idx, Matches.mtTimestamp);}
    );
}

function doShow(start, idx, mtTimestamp) {
    if (parent != undefined && parent.storeInCache != undefined)
        parent.storeInCache(JSON.stringify({'matches' : matches, 'mtTimestamp' : mtTimestamp}));

    var date = new Date();

    if (getParameterByName('date', '') !== '')
        date = new Date(getParameterByName('date', formateDate(date)));
    else if (getParameterByName('day', '') !== '')
        date.setDate(getParameterByName('day', date.getDate()));

    var tables = Object.keys(matches);
    
    var mt = undefined;
    var currentIdx = idx;
    
    while (start < tables.length) {
        var table = tables[start];

        if (matches[table] === null) {
            ++start;
            continue;
        }

        if (idx >= matches[table].length) {
            idx = 0;
            ++start;
            continue;
        }

        if (matches[table].length <= idx) {
            idx = 0;
            ++start;
            continue;
        }
        
        if (matches[table][idx] === null) {
            ++idx;
            continue;
        }

        mt = matches[table][idx];

        // Ignore matches without table
        if (mt.mtTable == 0) {
            ++idx;
            continue;
        }
        
        // Ignore matches without teams or players and all after
        // Not existing values in the database will not be set in the match object
        if (mt.cpType == 4 && (mt.tmAtmName === undefined || mt.tmXtmName === undefined)) {
            idx = 0;
            ++start;
            continue;
        }

        if (mt.cpType != 4 && (mt.plAplNr === undefined || mt.plXplNr === undefined)) {
            idx = 0;
            ++start;
            continue;
        }

        formatMatch(mt, idx);
        currentIdx = idx;
        
        if (getParameterByName('noUpdate', 0) > 0)
            return;

        if (++idx == matches[table].length) {
            idx = 0;
            start += 1;
        }

        break;
    }

    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 5) * 1000;
    if (currentIdx == 0)
        timeout *= 1.5;
    else
        timeout /= 1.5;
        
    setTimeout(function() {show(start, idx, mtTimestamp);}, timeout);
}


// ----------------------------------------------------------------
function formatMatch(mt, idx) {
    var ct = new Date().getTime();

    var history = '';
    
    if (mt.cpType == 4) {
        if (Matches.isStarted(mt) || mt.mttmResA > 0 || mt.mtTreamResX > 0) {
            $('#top .matches').html(mt.mttmResA);
            $('#bottom .matches').html(mt.mttmResX);
        } else {
            $('#top .matches').html('');
            $('#bottom .matches').html('');            
        }
    }

    if (Matches.isFinished(mt)) {
        $('#caption .text').html('Final Score from Table ' + mt.mtTable);
        $('#top .games').html(mt.mtResA);
        $('#bottom .games').html(mt.mtResX);

        $('#top .points').html(mt.mtResult[mt.mtResA + mt.mtResX - 1][0]);
        $('#bottom .points').html(mt.mtResult[mt.mtResA + mt.mtResX - 1][1]);

        for (var i = 0; i < mt.mtResult.length; i++) {
            if (i >= mt.mtResA + mt.mtResX)
                break;

            if (mt.mtResult[i][0] > mt.mtResult[i][1])
                history += '<span class="game">' + mt.mtResult[i][1] + '</span>';
            else
                history += '<span class="game">-' + mt.mtResult[i][0] + '</span>';
        }
    } else if (!Matches.isStarted(mt) && mt.mtDateTime > ct) {
        $('#caption .text').html('Next on Table ' + mt.mtTable);
        $('#top .points').html('');
        $('#bottom .points').html('');

        $('#top .games').html('');
        $('#bottom .games').html('');

        $('#top .matches').html('');
        $('#bottom .matches').html('');
    } else {
        if (idx == 0) {
            $('#caption .text').html('Now on Table ' + mt.mtTable);

            if (!Matches.isStarted(mt)) {
                $('#top .points').html('');
                $('#bottom .points').html('');

                $('#top .games').html('');
                $('#bottom .games').html('');
            } else {
                $('#top .games').html(mt.mtResA);
                $('#bottom .games').html(mt.mtResX);

                if (!mt.ltActive) {
                    // Without liveticker we don't have reliable values for points
                    // In this case just stick with the games
                    $('#top .points').html('0');
                    $('#bottom .points').html('0');
                } else if (mt.mtResult == undefined || mt.mtResult.length <= (mt.mtResA + mt.mtResX)) {
                    $('#top .points').html('0');
                    $('#bottom .points').html('0');
                } else if (mt.mtResA + mt.mtResX > 0 &&
                        mt.mtResult[mt.mtResA + mt.mtResX][0] == 0 &&
                        mt.mtResult[mt.mtResA + mt.mtResX][1] == 0) {
                    $('#top .points').html(mt.mtResult[mt.mtResA + mt.mtResX - 1][0]);
                    $('#bottom .points').html(mt.mtResult[mt.mtResA + mt.mtResX - 1][1]);
                } else {
                    $('#top .points').html(mt.mtResult[mt.mtResA + mt.mtResX][0]);
                    $('#bottom .points').html(mt.mtResult[mt.mtResA + mt.mtResX][1]);
                }

                for (var i = 0; i < mt.mtResult.length; i++) {
                    if (i >= mt.mtResA + mt.mtResX)
                        break;

                    if (mt.mtResult[i][0] > mt.mtResult[i][1])
                        history += '<span class="game">' + mt.mtResult[i][1] + '</span>';
                    else
                        history += '<span class="game">-' + mt.mtResult[i][0] + '</span>';
                }
            }
        } else {
            $('#caption .text').html('Next on Table ' + mt.mtTable);
            $('#top .points').html('');
            $('#bottom .points').html('');
            $('#top .games').html('');
            $('#bottom .games').html('');
            $('#top .matches').html('');
            $('#bottom .matches').html('');
        }
    }

    $('#history td.history').html(history);

    if (idx > 0 || !Matches.isStarted(mt) && mt.mtDateTime > ct)
        $('#what td').html(
                'Start:&nbsp;' + formatTime(mt.mtDateTime) + '<br>' +
                mt.cpName + '&nbsp;&dash;&nbsp;' + mt.grDesc + (mt.grNofRounds === 1 ? '' : '&nbsp;&dash;&nbsp;' + formatRound(mt)));
    else
        $('#what td').html(
                mt.cpName + '&nbsp;&dash;&nbsp;' + mt.grDesc + (mt.grNofRounds === 1 ? '' : '&nbsp;&dash;&nbsp;' + formatRound(mt)));

    var prop = flag === 'region' ? 'naRegion' : 'naName';
        
    if (mt.cpType == 4) {
        var topPlayer = '';
        var botPlayer = '';

        if (mt.plAplNr !== undefined && mt.plAplNr != 0) {
            topPlayer = formatPlayer(mt.plApsLast, mt.plApsFirst);
            if (mt.plBplNr !== undefined && mt.plBplNr != 0) {
                if (mt.plBnaName != mt.plAnaName)
                    topPlayer += ' (' + mt.plAnaName + ')';
                topPlayer += '<br>';
                topPlayer += formatPlayer(mt.plBpsLast, mt.plBpsFirst);
                if (mt.plAnaName != mt.plBnaName)
                    topPlayer += ' (' + mt.plBnaName + ')';
            }
        } else {
            topPlayer = formatTeam(mt.tmAtmDesc);
        }

        if (mt.plXplNr !== undefined && mt.plXplNr != 0) {
            botPlayer = formatPlayer(mt.plXpsLast, mt.plXpsFirst);
            if (mt.plYplNr !== undefined && mt.plYplNr != 0) {
                if (mt.plYnaName != mt.plXnaName)
                    botPlayer += ' (' + mt.plXnaName + ')';
                botPlayer += '<br>';
                botPlayer += formatPlayer(mt.plYpsLast, mt.plYpsFirst);
                if (mt.plXnaName != mt.plYnaName)
                    botPlayer += ' (' + mt.plYnaName + ')';
            }
        } else {
            botPlayer = formatTeam(mt.tmXtmDesc);
        }

        if (flag !== 'none')
            $('#top .assoc').html(formatFlag(mt['tmA' + prop]));
        $('#top .names').html(topPlayer);
        if (flag !== 'none')
            $('#bottom .assoc').html(formatFlag(mt['tmA' + prop]));
        $('#bottom .names').html(botPlayer);
    } else if (mt.cpType == 1) {
        if (flag !== 'none')
            $('#top .assoc').html(formatFlag(mt['plA' + prop]));
        $('#top .names').html(formatString(mt.plApsFirst) + ' ' + formatString(mt.plApsLast));
        if (flag !== 'none')
            $('#bottom .assoc').html(formatFlag(mt['plX' + prop]));
        $('#bottom .names').html(formatString(mt.plXpsFirst) + ' ' + formatString(mt.plXpsLast));
    } else {
        if (flag !== 'none')
            $('#top .assoc').html(formatFlag(mt['plA' + prop]) + '<br>' + formatFlag(mt['plB' + prop]));
        $('#top .names').html(formatString(mt.plApsFirst) + ' ' + formatString(mt.plApsLast) + '<br>' + formatString(mt.plBpsFirst) + ' ' + formatString(mt.plBpsLast));
        if (flag !== 'none')
            $('#bottom .assoc').html(formatFlag(mt['plX' + prop]) + '<br>' + formatFlag(mt['plY' + prop]));
        $('#bottom .names').html(formatString(mt.plXpsFirst) + ' ' + formatString(mt.plXpsLast) + '<br>' + formatString(mt.plYpsFirst) + ' ' + formatString(mt.plYpsLast));
    }
    
    // Show matches only for team events
    if (mt.cpType == 4)
        $('.matches').show();
    else
        $('.matches').hide();
}

function formatTeam(team) {
    return formatString(team, teamNameLength, '\u2026');
}


function formatPlayer(psLast, psFirst) {
    return '' +
            '<span class="first">' + formatString(psFirst, lastNameLength) + ' </span>' +
            '<span class="last">' + formatString(psLast, firstNameLength) + '</span>'
            ;
}

function formatFlag(name) {
    if (name === undefined || name == '')
        return '';

    return '<img src="' + '../flags/' + name + '.png"></img>';
}
