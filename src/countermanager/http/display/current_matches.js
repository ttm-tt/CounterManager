/* Copyright (C) 2020 Christoph Theis */

/*
 * Parmeters
 *      config:     default 'default'   Specific settings from <config>.css
 *      theme:      default 'default'   Fonts / Colors from themes/<theme>.css
 *      timeout:    default 5           Dauer der Anzeige
 *      noUpdate:   default 0           Anzeige stehen lassen
 *      prestart:   default 3600        Zeit vor Spielstart, ab wann angezeigt wird
 *      mintime:    default 60          Mindestzeit in Sekunden, die ein fertiges Spiel angezeigt wird
 *      fromTable:  default ''          Anzeige von Tisch
 *      toTable:    default ''          Anzeige bis Tisch
 *      day:        default 0           Auswahl Tag
 *      date:       default ''          Auswahl Datum
 *      all:        default 0           Alle Spiele anzeigen
 *      rows:       default 999         Max. Anzahl Zeilen
 *      nameLength: default 11          Max. Laenge der Namen
 *      showTeams:  default 0           Mannschaften statt Spieler anzeigen
 *      mintime:    default 60          Mindestzeit in Sekunden, die ein fertiges Spiel angezeigt wird
 */


var args = {};

var minTime = 60;    // [s]
var prestart = 3600; // [s]

var matches = [];
var mtTimestamp = 0;

import * as Matches from '../scripts/modules/current_matches.js';

minTime = getParameterByName("minTime", minTime);
prestart = getParameterByName("prestart", prestart);

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
    args['date'] = getParameterByName('date', formatDate(date));

if (getParameterByName('fromTable', 0) != 0)
    args['fromTable'] = getParameterByName('fromTable', 0);

if (getParameterByName('toTable', 0) != 0)
    args['toTable'] = getParameterByName('toTable', 0);

if (getParameterByName('tableList', '') != '')
    args['tableList'] = getParameterByName('tableList');

// TODO: Nur bis jetzt, also 'to' : date anhaengen.
update(args);

function update(args) {
    if (parent != this && parent.show !== undefined && !parent.show())
        return;

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
            if (matches[i] == undefined)
                continue;

            if (from == undefined || from > matches[i].mtDateTime)
                from = matches[i].mtDateTime;
        }

        // Attribute zuruecksetzen
        args['all'] = getParameterByName('all', 1);
        if (from !== undefined)
            args['from'] = from;
        else
            delete args['from'];
        delete args['notFinished'];
        delete args['mtTimestamp'];
        
        // TODO: to berechnen
        update(args);

        return;
    }

    var data = [];
    var i;

    if (mtTimestamp != undefined)
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
        parent.storeInCache(JSON.stringify({'matches' : matches, 'mtTimestamp' : mtTimestamp}));

    $('#matches tbody').empty();

    var rowCount = 0;
    var count = getParameterByName('rows', 999);  
    var tables = Object.keys(matches);

    while (count > 0 && start < tables.length) {
        var table = tables[start];
        
        if (matches[table] == undefined) {
            ++start;
            continue;
        }

        // Ignore matches withouot table
        if (matches[table][0].mtTable == 0) {
            ++start;
            continue;
        }

        var mt = matches[table][0];

        // Double / Mixed need at least 2 rows
        if ( (mt.cpType == 2 || mt.cpType == 3) && count < 2)
            break;

        var need = 1;
        if (mt.cpType == 2 || mt.cpType == 3)
            need += 1;
        else if (mt.cpType == 4) {
            if (Matches.isStarted(mt) && getParameterByName('individual', 0) > 0) {
                if (mt.plBplNr > 0)
                    need += 2;
                else if (mt.plAplNr > 0)
                    need += 1;
            }
        }

        if (need > count)
            break;

        var tr = formatMatch(mt, ++rowCount);
        $('#matches tbody').append(tr);

        if ($('#matches tbody').height() > document.documentElement.clientHeight - 10) {
            $('#matches tbody tr[visibilitiy=hidden]').remove();
            break;
        }

        $('#matches tbody tr').css('visibility', '');

        start += 1;
        count -= need;

        if (start == tables.length)
            break;
    }

    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 2 * rowCount) * 1000;
    if (getParameterByName('noUpdate', 0) == 0)
        setTimeout(function() {show(start);}, timeout);
}


// ----------------------------------------------------------------
// Helpers
function formatMatch(mt, rowCount) {
    var ct = new Date().getTime();
    var ret;           
    ret = '<tr class="' + ((rowCount % 2) ? 'odd' : 'even') + '" style="visibility:hidden">';

    if (mt.cpType == 4) {
        if ( !Matches.isStarted(mt) ) {
            ret += 
                '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>' +
                '<td class="event">' + mt.cpName + '</td>' +
                '<td class="names left" colspan="2">' + formatTeam(mt.tmAtmDesc) + '</td>' + 
                '<td class="names center">' + '-' + '</td>' +
                '<td class="names right" colspan="2">' + formatTeam(mt.tmXtmDesc) + '</td>' + 
                '<td class="time">' + formatTime(mt.mtDateTime) + '</td>';
        } else {
            ret +=
                '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>' +
                '<td class="event">' + mt.cpName + '</td>' +
                '<td class="names left" colspan="2">' + formatTeam(mt.tmAtmDesc) + '</td>' + 
                '<td class="names center">' + '-' + '</td>' +
                '<td class="names right" colspan="2">' + formatTeam(mt.tmXtmDesc) + '</td>' + 
                '<td class="matches">' + mt.mttmResA + '&nbsp;:&nbsp;' + mt.mttmResX + '</td>';
        }  

        if ( Matches.isStarted(mt) && getParameterByName('individual', 0) > 0 ) {
            if (mt.plBplNr > 0) {
                ret += '</tr>';
                ret += '<tr class="individual ' + ((rowCount % 2) ? 'odd' : 'even') + '" style="visibility:hidden">';
                ret += 
                    '<td class="table"></td>' +
                    '<td class="event"></td>' +
                    '<td class="names left" colspan="2">' + formatPlayer(mt.plApsLast) + '<br>' + formatPlayer(mt.plBpsLast) + '</td>' +
                    '<td class="names center">' + '-' + '</td>' +
                    '<td class="names right" colspan="2">' + formatPlayer(mt.plXpsLast) + '<br>' + formatPlayer(mt.plYpsLast) + '</td>' +
                    '<td class="games">' + mt.mtResA + '&nbsp;:&nbsp;' + mt.mtResX + '</td>';
            } else if (mt.plAplNr > 0) {
                ret += '</tr>';
                ret += '<tr class="individual ' + ((rowCount % 2) ? 'odd' : 'even') + '">';
                ret += 
                    '<td class="table"></td>' +
                    '<td class="event"></td>' +
                    '<td class="names left" colspan="2">' + formatPlayer(mt.plApsLast) + '</td>' +
                    '<td class="names center">' + '-' + '</td>' +
                    '<td class="names right" colspan="2">' + formatPlayer(mt.plXpsLast) + '</td>' +
                    '<td class="games">' + mt.mtResA + '&nbsp;:&nbsp;' + mt.mtResX + '</td>';
            }
        }
    } else if (mt.cpType == 1) {
        if ( !Matches.isStarted(mt) ) {
            ret +=
                '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>' +
                '<td class="event">' + mt.cpName + '</td>' +
                '<td class="assoc left">' + mt.plAnaName + '</td>' +
                '<td class="names left">' + formatPlayer(mt.plApsLast) + '</td>' + 
                '<td class="names center">' + '-' + '</td>' +
                '<td class="assoc right">' + mt.plXnaName + '</td>' +
                '<td class="names right">' + formatPlayer(mt.plXpsLast) + '</td>' + 
                '<td class="time">' + formatTime(mt.mtDateTime) + '</td>';
        } else {
            ret +=
                '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>' +
                '<td class="event">' + mt.cpName + '</td>' +
                '<td class="assoc left">' + mt.plAnaName + '</td>' +
                '<td class="names left">' + formatPlayer(mt.plApsLast) + '</td>' + 
                '<td class="names center">' + '-' + '</td>' +
                '<td class="assoc right">' + mt.plXnaName + '</td>' +
                '<td class="names right">' + formatPlayer(mt.plXpsLast) + '</td>' + 
                '<td class="games">' + mt.mtResA + '&nbsp;:&nbsp;' + mt.mtResX + '</td>';
        }
    } else {
        if ( (mt.mtDateTime > ct && mt.mtResA == 0 && mt.mtResX == 0) || 
             (mt.plAplNr == undefined || mt.plAplNr == 0 || mt.plXplNr == undefined || mt.plXplNr == 0)) {                        
            ret +=
                '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>' +
                '<td class="event">' + mt.cpName + '</td>' +
                '<td class="assoc left">' + mt.plAnaName + '<br>' + mt.plBnaName + '</td>' +
                '<td class="names left">' + formatPlayer(mt.plApsLast) + '<br>' + formatPlayer(mt.plBpsLast) + '</td>' + 
                '<td class="names center">' + '-' + '</td>' +
                '<td class="assoc right">' + mt.plXnaName + '<br>' + mt.plYnaName + '</td>' +
                '<td class="names right">' + formatPlayer(mt.plXpsLast) + '<br>' + formatPlayer(mt.plYpsLast) + '</td>' + 
                '<td class="time">' + formatTime(mt.mtDateTime) + '</td>';
        } else {
            ret +=
                '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>' +
                '<td class="event">' + mt.cpName + '</td>' +
                '<td class="assoc left">' + mt.plAnaName + '<br>' + mt.plBnaName + '</td>' +
                '<td class="names left">' + formatPlayer(mt.plApsLast) + '<br>' + formatPlayer(mt.plBpsLast) + '</td>' + 
                '<td class="names center">' + '-' + '</td>' +
                '<td class="assoc right">' + mt.plXnaName + '<br>' + mt.plYnaName + '</td>' +
                '<td class="names right">' + formatPlayer(mt.plXpsLast) + '<br>' + formatPlayer(mt.plYpsLast) + '</td>' + 
                '<td class="games">' + mt.mtResA + '&nbsp;:&nbsp;' + mt.mtResX + '</td>';
        }
    }

    ret += '</tr>';

    return ret;
}

function formatTeam(team) {
    return formatString(team, 11);
}


function formatPlayer(player) {
    return formatString(player, 7);
}

