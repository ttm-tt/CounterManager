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
 *      nameLength:         default 0           Max. Laenge der Namen (default: alles)
 *      firstNameLength:    default 0           Max. Laenge der Voramen (default: alles)
 *      lastNameLength:     default 0           Max. Laenge der Nachnamen (default: alles)
 *      teamNameLength:     default 0           Max. Laenge der Teamnamen (default: alles)
 *      showTeams:          default 0           Mannschaften statt Spieler anzeigen
 *      flag:               default 'nation'    Choose either 'none', 'nation' or 'region' to show the flag
 *      showService:        default 1           Show who has the srevice
 */

var args = {};

var nameLength = 0;
var lastNameLength = 0;
var firstNameLength = 0;
var teamNameLength = 0;
var flag = 'nation';
var showService = false;
var prestart = 3600;

$(document).ready(function() {
    nameLength = getParameterByName("nameLength", 0);
    lastNameLength = getParameterByName("lastNameLength", nameLength);
    firstNameLength = getParameterByName("firstNameLength", nameLength);
    teamNameLength = getParameterByName("teamNameLength", nameLength);
    flag = getParameterByName("flag", "nation");
    showService = getParameterByName("showService", 1) != 0;
    prestart = parseInt(getParameterByName("prestart", prestart)) * 1000;
    
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
    
    if (getParameterByName('date', '') != '')
        args['date'] = getParameterByName('date', formatDate(date));
    
    if (getParameterByName('fromTable', 0) != 0)
        args['fromTable'] = getParameterByName('fromTable', 0);

    if (getParameterByName('toTable', 0) != 0)
        args['toTable'] = getParameterByName('toTable', 0);

    // TODO: Nur bis jetzt, also 'to' : date anhaengen.
    update([], args);
});

function update(matches, args) {
    if (parent !== null && parent != this && !parent.show())
        return;

    xmlrpc(
            "../RPC2", "ttm.listNextMatches", [args],
            function success(data) {
                var i;
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
                
                // Prepare finished matches which were shown before
                // If the match is finished remove the points so we show the final result only
                for (i in matches) {
                    if (isFinished(matches[i]))
                        matches[i].mtResult = null;
                }

                // A) Angefangene Spiele durch updates ersetzen
                // B) Fertige Spiele einmal anzeigen

                // 1) Fertige Spiele in der Vergangenheit koennen neu auftauchen

                // First: Replace all finished matches with the next unfinished one, if there is one
                //        but only if the next match is due to be displayed
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
        setTimeout(function() {update([], args);}, 2000);
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
        delete args['notFinished'];
        delete args['mtTimestamp'];
        if (from !== undefined)
            args['from'] = from;
        else
            delete args['from'];
        
        // TODO: to berechnen
        update(matches, args);

        return;
    }

    // Get latest matches
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
                        if (matches[data[i].mtTable].mtNr == data[i].mtNr && matches[data[i].mtTable].mtMS == data[i].mtMS) {
                            matches[data[i].mtTable] = data[i];
                        }
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
        parent.storeInCache(JSON.stringify({'matches': matches, 'mtTimestamp': mtTimestamp}));

    for (i in matches)
        data[data.length] = matches[i];

    data.sort(function(a, b) {
        return a.mtTable - b.mtTable;
    });

    $('#table tbody').empty();

    var matchCount = 0;
    var count = getParameterByName('rows', 999);
    var date = new Date();

    if (getParameterByName('date', '') !== '')
        date = new Date(getParameterByName('date', formateDate(date)));
    else if (getParameterByName('day', '') !== '')
        date.setDate(getParameterByName('day', date.getDate()));

    var ct = date.getTime();
    
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
        
        if (!isStarted(mt) && mt.mtDateTime > (ct + prestart)) {
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

        if (start == data.length)
            break;
    }

    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 3 * matchCount + 1) * 1000;
    if (getParameterByName('noUpdate', 0) == 0)
        setTimeout(function() { show(matches, start); }, timeout);
}


// ----------------------------------------------------------------
// Helpers
function isFinished(mt) {
    if (mt == undefined)
        return false;

    if (mt.mtMatches > 1 && (2 * mt.mttmResA > mt.mtMatches || 2 * mt.mttmResX > mt.mtMatches))
        return true;
    
    if (mt.mtWalkOverA != 0 || mt.mtWalkOverX != 0)
        return true;
    
    if (2 * mt.mtResA > mt.mtBestOf || 2 * mt.mtResX > mt.mtBestOf)
        return true;

    return false;
}


function isStarted(mt) {
    if (mt == undefined)
        return false;

    if (isFinished(mt))
        return true;
    
    if (mt.mtResult !== undefined && mt.mtResult.length > 0 && (mt.mtResult[0][0] > 0 || mt.mtResult[0][1] > 0))
        return true;

    var ct = new Date().getTime();

    if (mt.cpType == 4) {
        if ((mt.mtDateTime > ct && mt.mtResA == 0 && mt.mtResX == 0) ||
                (mt.plAplNr == undefined || mt.plAplNr == 0 || mt.plXplNr == undefined || mt.plXplNr == 0)) {
            return false;
        }
    } else {
        if ((mt.mtDateTime > ct && mt.mtResA == 0 && mt.mtResX == 0) ||
                (mt.plAplNr == undefined || mt.plAplNr == 0 || mt.plXplNr == undefined || mt.plXplNr == 0)) {
            return false;
        }
    }

    return true;
}


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

    if (!isStarted(mt)) {
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
        } else if (mt.mtResA + mt.mtResX > 0 && 
                mt.mtResult[mt.mtResA + mt.mtResX][0] == 0 && 
                mt.mtResult[mt.mtResA + mt.mtResX][1] == 0) {
            // Start of next game or match finished
            left += '<td class="points">' + mt.mtResult[mt.mtResA + mt.mtResX - 1][0] + '</td>';
            right += '<td class="points">' + mt.mtResult[mt.mtResA + mt.mtResX - 1][1] + '</td>';            

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
        if (isStarted(mt) || mt.mttmResA > 0 || mt.mttmResX > 0) {
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

