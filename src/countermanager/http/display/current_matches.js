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

$(document).ready(function() {
    if (parent != undefined && parent.loadFromCache != undefined) {
        var data = parent.loadFromCache();
        if (data != undefined && data.length > 0) {
            data = JSON.parse(data);
            show(data['matches'], size(data['matches']), data['mtTimestamp']);

            return;
        }
    }

    var date = new Date();

    args = {
        'all': getParameterByName('all', 1),
        'notFinished': 1,
        'notStarted': 0
    };
    
    if (getParameterByName('day', 0) != 0)
        args['day'] = getParameterByName('day', 0);
    
    if (getParameterByName('date', '') != '')
        args['date'] = getParameterByName('date', formatDate(date));
    
    if (getParameterByName('fromTable', 0) != 0)
        args['fromTable'] = getParameterByName('fromTable', 0);

    if (getParameterByName('toTable', 0) != 0)
        args['toTable'] = getParameterByName('toTable', 0);

    if (getParameterByName('tableList', '') != '')
        args['tableList'] = getParameterByName('tableList');

    // TODO: Nur bis jetzt, also 'to' : date anhaengen.
    update({}, args);
});

function update(matches, args) {
    if (parent != this && !parent.show())
        return;

    xmlrpc(
        "../RPC2", "ttm.listNextMatches", [args], 
        function success(data) {
            var i;
            var prestart = parseInt(getParameterByName('prestart', '3600')) * 1000;
            var minTime = parseInt(getParameterByName('mintime', '60')) * 1000;
            var date = new Date();

            if (getParameterByName('date', '') !== '')
                date = new Date(getParameterByName('date', formateDate(date)));
            else if (getParameterByName('day', '') !== '')
                date.setDate(getParameterByName('day', date.getDate()));

            var ct = date.getTime();

            // Sort array by table, date / time, nr and team match
            data.sort(function(a, b) {
                var res = a.mtTable - b.mtTable;
                if (!res)
                    res = a.mtDateTime - b.mtDateTime;
                if (!res)
                    res = a.mtNr - b.mtNr;
                if (!res)
                    res = a.mtMS - b.mtMS;

                return res;
            });
                
            // A) Angefangene Spiele durch updates ersetzen
            // B) Fertige Spiele einmal anzeigen

            // 1) Fertige Spiele in der Vergangenheit koennen neu auftauchen

            // First: Replace all finished matches with the next unfinished one, if there is one
            for (i = 0; i < data.length; i++) {
                if (isFinished(matches[data[i].mtTable]) && !isFinished(data[i])) {
                    if (data[i].mtDateTime > (ct + prestart))
                        ;  // Naechstes Spiel nicht zu frueh anzeigen
                    else if (matches[data[i].mtTable].mtTimestamp > (ct - minTime))
                        ;  // Ergebnis mindestens minTime anzeigen (mtTimestamp ist die letzte Aenderung)
                    else
                        matches[data[i].mtTable] = undefined;  // Spiel loeschen
                }
            }

            // And remove all last finished matches
            for (i in matches) {
                if (isFinished(matches[i]) && matches[i].mtTimestamp < (ct - minTime))
                    matches[i] = undefined;
            }

            for (i = 0; i < data.length; i++) {
                if (matches[data[i].mtTable] == undefined) {
                    // If there is no match on a table, only replace with unfinished once.
                    // Finished matches which had been displayed once were already removed.
                    if (!isFinished(data[i]))
                        matches[data[i].mtTable] = data[i];
                }
            }

            // Second: Replace all matches with an update
            for (i = 0; i < data.length; i++) {
                if (matches[data[i].mtTable] != undefined &&
                        matches[data[i].mtTable].mtNr == data[i].mtNr &&
                        matches[data[i].mtTable].mtMS == data[i].mtMS) {
                    matches[data[i].mtTable] = data[i];
                }
            }

            // Third: Replace all unfinished matches with the first unfinished one
            for (i in matches) {
                if (!isFinished(matches[i]))
                    matches[i] = undefined;
            }

            for (i = 0; i < data.length; i++) {
                if (matches[data[i].mtTable] == undefined) {
                    if (!isFinished(data[i]))
                        matches[data[i].mtTable] = data[i];
                }
            }

            // Get last update time
            var mtTimestamp = undefined;
            for (i = 0; i < data.length; i++) {
                if (mtTimestamp == undefined || mtTimestamp < data[i].mtTimestamp)
                    mtTimestamp = data[i].mtTimestamp;
            }

            show(matches, 0, mtTimestamp);
        }                     
        , function error(e) {                            
            show(matches, 0, undefined);
        }
    );
}


function show(matches, start, mtTimestamp) {
    if (size(matches) === 0) {
        setTimeout(function() {update({}, args);}, 2000);
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
        update(matches, args);

        return;
    }

    var data = [];
    var i;

    if (mtTimestamp != undefined)
        args['mtTimestamp'] = mtTimestamp;

    xmlrpc("../RPC2", "ttm.listNextMatches", [args],
        function success(data) {
            // Sort array by table, date / time, nr and team match
            data.sort(function(a, b) {
                var res = a.mtTable - b.mtTable;
                if (!res)
                    res = a.mtDateTime - b.mtDateTime;
                if (!res)
                    res = a.mtNr - b.mtNr;
                if (!res)
                    res = a.mtMS - b.mtMS;

                return res;
            });
            
            for (var i = 0; i < data.length; i++) {
               if (matches[data[i].mtTable] != undefined) {
                   // No resceduled matches, only updates of results
                   if (matches[data[i].mtTable].mtNr == data[i].mtNr && matches[data[i].mtTable].mtMS == data[i].mtMS)
                       matches[data[i].mtTable] = data[i];
               }

               if (mtTimestamp == undefined || mtTimestamp < data[i].mtTimestamp)
                   mtTimestamp = data[i].mtTimestamp;
           } 
        }
        , function error(e) {
        }
        , function final() {
            doShow(matches, start, mtTimestamp);
        }
    );
}


function doShow(matches, start, mtTimestamp) {
    var data = [];
    var i;
    
    if (parent != undefined && parent.storeInCache != undefined)
        parent.storeInCache(JSON.stringify({'matches' : matches, 'mtTimestamp' : mtTimestamp}));

    for (i in matches) 
        data[data.length] = matches[i];

    data.sort(function(a, b) {return a.mtTable - b.mtTable;});

    $('#matches tbody').empty();

    var rowCount = 0;
    var count = getParameterByName('rows', 999);  
    while (count > 0 && start < data.length) {
        if (data[start] == undefined) {
            ++start;
            continue;
        }

        // Ignore matches withouot table
        if (data[start].mtTable == 0) {
            ++start;
            continue;
        }

        var mt = data[start];

        // Double / Mixed need at least 2 rows
        if ( (mt.cpType == 2 || mt.cpType == 3) && count < 2)
            break;

        var need = 1;
        if (mt.cpType == 2 || mt.cpType == 3)
            need += 1;
        else if (mt.cpType == 4) {
            if (isStarted(mt) && getParameterByName('individual', 0) > 0) {
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

        if (start == data.length)
            break;
    }

    if (false) {
        while (count-- > 0)
            $('#matches tbody').append(
                '<tr class="' + ((++rowCount % 2) ? 'odd' : 'even') + '">' +
                '<td colspan="8">&nbsp;</td>' +
                '</tr>'
            );
    }

    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 2 * rowCount) * 1000;
    if (getParameterByName('noUpdate', 0) == 0)
        setTimeout(function() {show(matches, start);}, timeout);
}


// ----------------------------------------------------------------
// Helpers
function isFinished(data) {
    if (data == undefined)
        return false;

    if (2 * data.mtResA < data.mtBestOf && 2 * data.mtResX < data.mtBestOf)
        return false;

    return true;
} 


function isStarted(mt) {
    if (mt == undefined)
        return false;
    
    var ct = new Date().getTime();
    
    if (mt.cpType == 4) {
        if ( (mt.mtDateTime > ct && mt.mtResA == 0 && mt.mtResX == 0) || 
             (mt.plAplNr == undefined || mt.plAplNr == 0 || mt.plXplNr == undefined || mt.plXplNr == 0)) { 
             return false;
        }
    } else {
        if ( (mt.mtDateTime > ct && mt.mtResA == 0 && mt.mtResX == 0) || 
             (mt.plAplNr == undefined || mt.plAplNr == 0 || mt.plXplNr == undefined || mt.plXplNr == 0)) {     
             return false;
        }                           
    }

    return true;
}


function formatMatch(mt, rowCount) {
    var ct = new Date().getTime();
    var ret;           
    ret = '<tr class="' + ((rowCount % 2) ? 'odd' : 'even') + '" style="visibility:hidden">';

    if (mt.cpType == 4) {
        if ( !isStarted(mt) ) {
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

        if ( isStarted(mt) && getParameterByName('individual', 0) > 0 ) {
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
        if ( !isStarted(mt) ) {
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

