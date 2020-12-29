/* Copyright (C) 2020 Christoph Theis */

/*
 * Parmeters
 *      config:     default 'default'
 *      timeout:    default 5           Dauer der Anzeige
 *      noUpdate:   default 0           Anzeige stehen lassen
 *      rows:       default 999         Max. Anzahl Zeilen
 *      nameLength: default -1          Max. Laenge der Namen (default: alles)
 *      firstNameLength: default -1     Max. Laenge der Voramen (default: alles)
 *      lastNameLength:  default -1     Max. Laenge der Nachnamen (default: alles)
 *      teamNameLength:  default -1     Max. Laenge der Teamnamen (default: alles)
 *      showTeams:  default 0           Mannschaften statt Spieler anzeigen
 *      mintime:    default 60          Mindestzeit in Sekunden, die ein fertiges Spiel angezeigt wird
 */

var args = {};

var noUpdate = false;
var nameLength = -1;
var lastNameLength = -1;
var firstNameLength = -1;
var teamNameLength = -1;

$(document).ready(function() {
    noUpdate = parseInt(getParameterByName("timeout", 1)) === 0;
    nameLength = getParameterByName("nameLength", nameLength);
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

    args = {
    };
    
    if (getParameterByName('cpName', '') != '')
        args['cpName'] = getParameterByName('cpName');
    
    if (getParameterByName('grStage', '') != '')
        args['grStage'] = getParameterByName('grStage');
    
    if (getParameterByName('grName', '') != '')
        args['grName'] = getParameterByName('grName');
    
    if (getParameterByName('mtRound', '') != '')
        args['mtRound'] = getParameterByName('mtRound');

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
                
                // Get last update time
                var mtTimestamp = undefined;
                for (i = 0; i < data.length; i++) {
                    if (mtTimestamp == undefined || mtTimestamp < data[i].mtTimestamp)
                        mtTimestamp = data[i].mtTimestamp;
                }
                
                data.sort(function(a, b) {
                // " ORDER BY cp.cpName, gr.grStage, gr.grName, mt.mtRound, mt.mtMatch";
                    if ((a.cpName || '') < (b.cpName || ''))
                        return -1;
                    if ((a.cpName || '') > (b.cpName || ''))
                        return +1;
                    if ((a.grStage || '') < (b.grStage || ''))
                        return -1;
                    if ((a.grStage || '') > (b.grStage || ''))
                        return +1;
                    if ((a.grName || '') < (b.grName || ''))
                        return -1;
                    if ((a.grName || '') > (b.grName || ''))
                        return +1;
                    if ((a.mtRound || 0) < (b.mtRound || 0))
                        return -1;
                    if ((a.mtRound || 0) > (b.mtRound || 0))
                        return +1;
                    if ((a.mtMatch || 0) < (b.mtMatch || 0))
                        return -1;
                    if ((a.mtMatch || 0) > (b.mtMatch || 0))
                        return +1;
                    
                    return 0;
                });

                show(data, 0, mtTimestamp);
            }
    , function error(e) {
        show(matches, 0, undefined);
    }
    );
}


function show(matches, start, mtTimestamp) {
    if (start >= size(matches)) {
        // Reload
        update(matches, args);

        return;
    }

    if (parent != undefined && parent.storeInCache != undefined)
        parent.storeInCache(JSON.stringify({'matches': matches, 'mtTimestamp': mtTimestamp}));

    $('#table tbody').empty();

    var matchCount = 0;
    var count = getParameterByName('rows', 999);

    while (count > 0 && start < matches.length) {
        var mt = matches[start];
        
        var tr = formatMatch(mt, ((++matchCount % 2) == 0 ? 'even' : 'odd'));
        $('#table tbody').append(tr);

        if ($('#table tbody').height() > document.documentElement.clientHeight - 10) {
            $('#table tbody tr.last').remove();
            break;
        }

        $('#table tbody tr').removeClass('last');

        start += 1;
        count -= 1;

        if (start == matches.length)
            break;
        
        if ( matches[start].cpName != mt.cpName ||
             matches[start].grName != mt.grName ||
             matches[start].mtRound != mt.mtRound )
            break;
        
    }

    if (noUpdate)
        return;
    
    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 3 * matchCount + 1) * 1000;
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

    var caption = '<tr class="caption last "' + clazz + '>';
    if (mt.mtTable != undefined && mt.mtTable != 0)
        caption += '<td class="table">' + 'T.&nbsp;' + mt.mtTable + '</td>';
    else
        caption += '<td class="table"></td>';
    
    if (mt.mtDateTime != undefined && mt.mtDateTime != 0)
        caption += '<td class="time">' + formatTime(mt.mtDateTime) + '</td>';
    else
        caption += '<td class="time"></td>';
    
    caption += '<td class="event">' + 
            mt.cpName + '&nbsp;&dash;&nbsp;' + 
            mt.grDesc + (mt.grNofRounds === 1 ? '' : '&nbsp;&dash;&nbsp;' + mt.mtRoundStr) + '&nbsp;&dash;&nbsp;' +
            '#' + mt.mtMatch +
            '</td>';
    caption += '<td class="games">' + 'Gms' + '</td>';
    if (mt.cpType == 4)
        caption += '<td class="matches">' + 'Mts' + '</td>';
    caption += '</tr>';

    var singleOrDouble = (mt.plBnaName != undefined || mt.plYnaName != undefined) ? 'double' : 'single';
    var left = '<tr class="players left last ' + clazz + ' ' + singleOrDouble + '">';
    var right = '<tr class="players right last ' + clazz + ' ' + singleOrDouble + '">';

    left += '<td class="assoc">';
    if (mt.plAnaName !== undefined)
        left += mt.plAnaName;
    else if (mt.cpType === 4 && mt.tmAnaName !== undefined)
        left += mt.tmAnaName;
        
    if (mt.plBnaName !== undefined && mt.plBnaName !== '')
        left += '<br>' + mt.plBnaName;
    left += '</td>';

    right += '<td class="assoc">';
    if (mt.plXnaName !== undefined)
        right += mt.plXnaName;
    else if (mt.cpType === 4 && mt.tmXnaName !== undefined)
        right += mt.tmXnaName;
    
    if (mt.plYnaName !== undefined && mt.plYnaName !== '')
        right += '<br>' + mt.plYnaName;
    right += '</td>';
    
    left += '<td colspan="2" class="names">';
    right += '<td colspan="2" class="names">';
    
    if (mt.cpType == 4 && getParameterByName('showTeams', 0) == 1) {
        left += mt.tmAtmDesc;
        right += mt.tmXtmDesc;
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

    if (!isStarted(mt)) {
        left += '<td class="games">' + '' + '</td>';
        right += '<td class="games">' + '' + '</td>';

    } else {
        left += '<td class="games">' + mt.mtResA + '</td>';
        right += '<td class="games">' + mt.mtResX + '</td>';
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
    return '' +
        '<span class="last">' + formatString(psLast, lastNameLength) + '</span>' +
        '<span class="first">' + (psLast !== undefined && psLast !== '' ? ', ' : '') + formatString(psFirst, firstNameLength) + '</span>'
    ;
}



