/* Copyright (C) 2020 Christoph Theis */

/*
 * Parameter
 * timeout: time to show each page
 * all:    0: nur aktuelles Spiel, 1: auch die naechsten Spiele
 * date:   Auswahl Datum (yyyy-mm-dd)
 * day:    Auswahl Tag
 * prestart:  default 3600         Zeit vor Spielstart, ab wann angezeigt wird
 * fromTable: Ab Tisch
 * toTable:   Bis Tisch
 * table:     Nur Tisch
 * tableList: Liste von Tichen
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

var matches = [];
var mtTimestamp = 0;

import * as Matches from '../scripts/modules/current_matches.js';

nameLength = getParameterByName("nameLength", 0);
lastNameLength = getParameterByName("lastNameLength", nameLength);
firstNameLength = getParameterByName("firstNameLength", nameLength);
teamNameLength = getParameterByName("teamNameLength", nameLength);

// Set configuration
Matches.setConfig({includeAllTeamMatches: true});

if (parent != undefined && parent.loadFromCache != undefined) {
    var data = parent.loadFromCache();
    if (data != undefined && data.length > 0) {
        data = JSON.parse(data);
        
        Matches.rebuild(matches, data.matches);
        mtTimestamp = data.mtTimestamp;
    }
}

update(args);


function update(args) {
    if (parent != this && !parent.show())
        return;

    xmlrpc(
        "../RPC2", "ttm.getCurrentTeamMatches", [args],
        function success(data) {
            Matches.rebuild(matches, data);
        },
        function error(err) {},
        function final() {show();}
    );    
}


function show() {
    var tr;
    
    $('#table tbody').empty();
    
    if (matches.length > 0) {
        tr = formatCaption(matches);
        $('table tbody').append(tr);

        for (var i in matches) {
            tr = formatMatch(matches, i);
            $('table tbody').append(tr);
        }
    }
    
    // Show next data (if there is any)
    var timeout = getParameterByName('timeout', 1) * 1000;
    if (getParameterByName('noUpdate', 0) == 0)
        setTimeout(function() { update(args); }, timeout);
}


function formatCaption() {
    var tr;
    
    tr = '<tr class="caption">';
    
    tr += '<td class="team">';
    tr += '<span class="top">' + formatTeam(matches[0].tmA.tmDesc) + '</span>';
    tr += '<span class="bottom">' + formatTeam(matches[0].tmX.tmDesc) + '</span>';
    tr += '</td>';
    
    tr += '<td class="points" colspan="' + matches[0].mtBestOf + '">';
    tr += '<span class="top"></span>';
    tr += '<span class="bottom"></span>';
    tr += '</td>';

    tr += '<td class="matches">';
    tr += '<span class="top">' + matches[0].mttmResA + '</span>';
    tr += '<span class="bottom">' + matches[0].mttmResX + '</span>';
    tr += '</td>';

    tr += '</tr>';
    
    return tr;
}

function formatMatch(ms) {
    var tr;
    var match = matches[ms];
    
    // TODO: Leave old match for about 60 seconds
    
    tr = '<tr class="match';
    if (match.cpType == 1)
        tr += ' single ';
    else
        tr += ' double ';
    
    if (isFinished(match)) {
        if (match.mtMS == match.mtMatches)
            tr += ' started';
        else if (isStarted(matches[ms + 1]))
            tr += ' finished';
        else
            tr += ' finished';
    }
    else if (isStarted(match))
        tr += ' started';
    else if (match.mtMS == 1)
        tr += ' started';
    else if (isFinished(matches[ms - 1])) {
        tr += ' not-started';
    }
    else
        tr += ' not-started';
    tr += '">';
    
    tr += '<td class="names">';
    
    tr += '<span class="top">';
    if (match.plA.plNr > 0)
        tr += formatPlayer(match.plA.psLast, match.plA.psFirst);
    if (match.plB.plNr > 0)
        tr += '<br>' + formatPlayer(match.plB.psLast, match.plB.psFirst);
    tr += '</span>';
    
    tr += '<span class="bottom">';
    if (match.plX.plNr > 0)
        tr += formatPlayer(match.plX.psLast, match.plX.psFirst);
    if (match.plY.plNr > 0)
        tr += '<br>' + formatPlayer(match.plY.psLast, match.plY.psFirst);
    tr += '</span>';
    
    tr += '</td>';
    
    var i;
    for (i = 0; i < (match.mtResult === undefined ? 0 : match.mtResult.length); i++) {
        tr += '<td class="points">';
        
        tr += '<span class="top">';
        if (match.mtResult[i][0] > 0 || match.mtResult[i][1] > 0)
            tr += match.mtResult[i][0];
        tr += '</span>';
        tr += '<span class="bottom">';
        if (match.mtResult[i][0] > 0 || match.mtResult[i][1] > 0)
            tr += match.mtResult[i][1];
        tr += '</span>';
        
        tr += '</td>';            
    }
    
    for (; i < match.mtBestOf; i++)
        tr += '<td class="points"></td>';
    
    tr += '<td class="games">';
    if (isStarted(match)) {
        tr += '<span class="top">' + match.mtResA + '</span>';
        tr += '<span class="bottom">' + match.mtResX + '</span>';
    }
    tr += '</td>';
    
    tr += '</tr>';
    
    return tr;
}


// Helpers
function isFinished(mt) {
    if (mt === undefined)
        return false;

    if (mt.mtMatches > 1 && (2 * mt.mttmResA > mt.mtMatches || 2 * mt.mttmResX > mt.mtMatches))
        return true;
    
    if (2 * mt.mtResA > mt.mtBestOf || 2 * mt.mtResX > mt.mtBestOf)
        return true;

    return false;
}

function isStarted(mt) {
    if (mt === undefined)
        return false;
    
    if (isFinished(mt))
        return true;
    
    if (mt.mtResult !== undefined && mt.mtResult.length > 0 && (mt.mtResult[0][0] > 0 || mt.mtResult[0][1] > 0))
        return true;

    return false;
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


