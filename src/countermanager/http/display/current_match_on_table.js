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
 * noFlag: Anzeige der Flagge unterdruecken
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

$(document).ready(function() {
    nameLength = getParameterByName("nameLength", 0);
    lastNameLength = getParameterByName("lastNameLength", nameLength);
    firstNameLength = getParameterByName("firstNameLength", nameLength);
    teamNameLength = getParameterByName("teamNameLength", nameLength);
    
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
        'all' : getParameterByName('all', 1),
        'notFinished' : 1, 
        'notStarted' : 0                     
    };

    if (getParameterByName('day', 0) != 0)
        args['day'] = getParameterByName('day', 0);

    if (getParameterByName('date', '') != '')
        args['date'] = getParameterByName('date', formatDate(date));

    if (getParameterByName('fromTable', 0) != 0)
        args['fromTable'] = getParameterByName('fromTable', 0);

    if (getParameterByName('toTable', 0) != 0)
        args['toTable'] = getParameterByName('toTable', 0);

    if (getParameterByName('table', 0) != 0)
        args['table'] = getParameterByName('table', 0);

    if (getParameterByName('tableList', '') != '')
        args['tableList'] = getParameterByName('tableList');

    // TODO: Nur bis jetzt, also 'to' : date anhaengen.
    update( {}, args );
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

                // A) Angefangene Spiele durch updates ersetzen
                // B) Fertige Spiele einmal anzeigen

                // 1) Fertige Spiele in der Vergangenheit koennen neu auftauchen

                // Init: Replace all matches with the first one
                for (i in matches) {
                    matches[i] = matches[i][0];
                }

                // First: 
                // Replace all finished matches with the next unfinished one, if there is one
                // Remove if there is none
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

                // Second: 
                // Replace all matches with an update
                for (i = 0; i < data.length; i++) {
                    if (matches[data[i].mtTable] != undefined &&
                            matches[data[i].mtTable].mtNr == data[i].mtNr &&
                            matches[data[i].mtTable].mtMS == data[i].mtMS) {
                        matches[data[i].mtTable] = data[i];
                    }
                }

                // Third: 
                // Replace all unfinished matches with the first unfinished one
                // We might have to go backwards, e.g. go back to the previous match
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

                // Finish: put all matches in an array and add the next ones
                var ct = new Date().getTime();
                for (i in matches) {
                    matches[i] = [ matches[i] ];
                }

                for (i = 0; i < data.length; i++) {
                    var mtTable = data[i].mtTable;

                    // No more matches, but safety condition
                    if (matches[mtTable] === undefined || matches[mtTable][0] === undefined)
                        continue;

                    // Same (team) match
                    if (matches[mtTable][0].mtNr == data[i].mtNr && matches[mtTable][0].mtMS == data[i].mtMS)
                        continue;

                    // Only one next team match of current match
                    var length = matches[mtTable].length;
                    if (length > 1 && matches[mtTable][length-1].mtNr == data[i].mtNr)
                        continue;

                    // A previous match (which means data[i] is finished)
                    if (matches[mtTable][0].mtDateTime > data[i].mtDateTime)
                        continue;

                    // Last match shown on this table is in the future
                    // Don't show more matches in the future (only one "Next on table")
                    if (matches[mtTable][matches[mtTable].length - 1].mtDateTime > ct + prestart)
                        continue;

                    // Next match on table
                    matches[data[i].mtTable].push(data[i]);
                }

                // Get last update time
                var mtTimestamp = undefined;
                for (i = 0; i < data.length; i++) {
                    if (mtTimestamp == undefined || mtTimestamp < data[i].mtTimestamp)
                        mtTimestamp = data[i].mtTimestamp;
                }

                show(matches, 0, 0, mtTimestamp);
            }
    , function error(e) {
        show(matches, 0, 0, undefined);
    }
    );
}


function show(matches, start, idx, mtTimestamp) {
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

        // args['all'] = 1;

        // Attribute zuruecksetzen
        args['all'] = getParameterByName('all', 1);
        delete args['mtTimestamp'];
        delete args['notFinished'];
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

    // But with finished matches, too
    delete args['notFinished'];

    xmlrpc("../RPC2", "ttm.listNextMatches", [args],
        function success(data) {
           for (var i = 0; i < data.length; i++) {
               if (matches[data[i].mtTable] !== undefined && matches[data[i].mtTable][0] !== undefined) {
                   // No resceduled matches, only updates of results
                   if (matches[data[i].mtTable][0].mtNr == data[i].mtNr && matches[data[i].mtTable][0].mtMS == data[i].mtMS)
                       matches[data[i].mtTable][0] = data[i];
               }

               if (mtTimestamp == undefined || mtTimestamp < data[i].mtTimestamp)
                   mtTimestamp = data[i].mtTimestamp;
           } 
        }
        , function error(e) {
        }
        , function final() {
            doShow(matches, start, idx, mtTimestamp);
        }
    );
}

function doShow(matches, start, idx, mtTimestamp) {
    if (parent != undefined && parent.storeInCache != undefined)
        parent.storeInCache(JSON.stringify({'matches' : matches, 'mtTimestamp' : mtTimestamp}));

    var date = new Date();

    if (getParameterByName('date', '') !== '')
        date = new Date(getParameterByName('date', formateDate(date)));
    else if (getParameterByName('day', '') !== '')
        date.setDate(getParameterByName('day', date.getDate()));

    var ct = date.getTime();
    
    var tables = Object.keys(matches);
    
    var mt = undefined;
    var currentIdx = idx;
    
    while (start < tables.length) {
        var table = tables[start];

        if (matches[table] == undefined) {
            ++start;
            continue;
        }

        if (idx >= matches[table].length) {
            idx = 0;
            ++start;
            continue;
        }

        if (matches[table][idx] === undefined) {
            idx = 0;
            ++start;
            continue;
        }

        mt = matches[table][idx];

        // Ignore matches without table
        if (mt.mtTable == 0) {
            ++idx;
            continue;
        }
        
        // Ignore matches not due and all after
        if (!isStarted(mt) && mt.mtDateTime > (ct + parseInt(getParameterByName('prestart', '1800')) * 1000)) {
            idx = 0;
            ++start;
            continue;
        }


        // Ignore matches without players and all after
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
        
    setTimeout(function() {show(matches, start, idx, mtTimestamp);}, timeout);
}


// ----------------------------------------------------------------
// Helpers
function isFinished(mt) {
    if (mt == undefined)
        return false;

    if (mt.mtMatches > 1 && (2 * mt.mtTeamResA > mt.mtMatches || 2 * mt.mtTeamResX > mt.mtMatches))
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

    if (mt.mtSets === undefined)
        return false;

    if (isFinished(mt))
        return true;

    if (mt.mtSets.length > 0 && (mt.mtSets[0][0] > 0 || mt.mtSets[0][1] > 0))
        return true;

    if ((mt.mtResA == 0 && mt.mtResX == 0) ||
            (mt.plAplNr == undefined || mt.plAplNr == 0 || mt.plXplNr == undefined || mt.plXplNr == 0)) {
        return false;
    }

    return true;
}


function formatMatch(mt, idx) {
    var ct = new Date().getTime();

    var history = '';
    
    if (mt.cpType == 4) {
        if (isStarted(mt) || mt.mtTeamResA > 0 || mt.mtTreamResX > 0) {
            $('#top .matches').html(mt.mtTeamResA);
            $('#bottom .matches').html(mt.mtTeamResX);
        } else {
            $('#top .matches').html('');
            $('#bottom .matches').html('');            
        }
    }

    if (isFinished(mt)) {
        $('#caption .text').html('Final Score from Table ' + mt.mtTable);
        $('#top .games').html(mt.mtResA);
        $('#bottom .games').html(mt.mtResX);

        $('#top .points').html(mt.mtSets[mt.mtResA + mt.mtResX - 1][0]);
        $('#bottom .points').html(mt.mtSets[mt.mtResA + mt.mtResX - 1][1]);

        for (var i = 0; i < mt.mtSets.length; i++) {
            if (i >= mt.mtResA + mt.mtResX)
                break;

            if (mt.mtSets[i][0] > mt.mtSets[i][1])
                history += '<span class="game">' + mt.mtSets[i][1] + '</span>';
            else
                history += '<span class="game">-' + mt.mtSets[i][0] + '</span>';
        }
    } else if (!isStarted(mt) && mt.mtDateTime > ct) {
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

            if (!isStarted(mt)) {
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
                } else if (mt.mtSets == undefined || mt.mtSets.length <= (mt.mtResA + mt.mtResX)) {
                    $('#top .points').html('0');
                    $('#bottom .points').html('0');
                } else if (mt.mtResA + mt.mtResX > 0 &&
                        mt.mtSets[mt.mtResA + mt.mtResX][0] == 0 &&
                        mt.mtSets[mt.mtResA + mt.mtResX][1] == 0) {
                    $('#top .points').html(mt.mtSets[mt.mtResA + mt.mtResX - 1][0]);
                    $('#bottom .points').html(mt.mtSets[mt.mtResA + mt.mtResX - 1][1]);
                } else {
                    $('#top .points').html(mt.mtSets[mt.mtResA + mt.mtResX][0]);
                    $('#bottom .points').html(mt.mtSets[mt.mtResA + mt.mtResX][1]);
                }

                for (var i = 0; i < mt.mtSets.length; i++) {
                    if (i >= mt.mtResA + mt.mtResX)
                        break;

                    if (mt.mtSets[i][0] > mt.mtSets[i][1])
                        history += '<span class="game">' + mt.mtSets[i][1] + '</span>';
                    else
                        history += '<span class="game">-' + mt.mtSets[i][0] + '</span>';
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

    if (idx > 0 || !isStarted(mt) && mt.mtDateTime > ct)
        $('#what td').html(
                'Start:&nbsp;' + formatTime(mt.mtDateTime) + '<br>' +
                mt.cpName + '&nbsp;&dash;&nbsp;' + mt.grDesc + (mt.grNofRounds === 1 ? '' : '&nbsp;&dash;&nbsp;' + mt.mtRoundStr));
    else
        $('#what td').html(
                mt.cpName + '&nbsp;&dash;&nbsp;' + mt.grDesc + (mt.grNofRounds === 1 ? '' : '&nbsp;&dash;&nbsp;' + mt.mtRoundStr));

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

        $('#top .assoc').html(formatFlag(mt.tmAnaName));
        $('#top .names').html(topPlayer);
        $('#bottom .assoc').html(formatFlag(mt.tmXnaName));
        $('#bottom .names').html(botPlayer);
    } else if (mt.cpType == 1) {
        $('#top .assoc').html(formatFlag(mt.plAnaName));
        $('#top .names').html(formatString(mt.plApsFirst) + ' ' + formatString(mt.plApsLast));
        $('#bottom .assoc').html(formatFlag(mt.plXnaName));
        $('#bottom .names').html(formatString(mt.plXpsFirst) + ' ' + formatString(mt.plXpsLast));
    } else {
        $('#top .assoc').html(formatFlag(mt.plAnaName) + '<br>' + formatFlag(mt.plBnaName));
        $('#top .names').html(formatString(mt.plApsFirst) + ' ' + formatString(mt.plApsLast) + '<br>' + formatString(mt.plBpsFirst) + ' ' + formatString(mt.plBpsLast));
        $('#bottom .assoc').html(formatFlag(mt.plXnaName) + '<br>' + formatFlag(mt.plYnaName));
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

    if (getParameterByName('noFlag', 0) > 0)
        return '<span class="assoc">' + name + '</span>';

    return '<img src="' + '../flags/' + name + '.png"></img>';
}
