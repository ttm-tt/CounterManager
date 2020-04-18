/* Copyright (C) 2020 Christoph Theis */

/* global getParameterByName */

$(document).ready(function() {
    var venues = getParameterByName('venues', 'update').split(',');
    
    for (var i = 0; i < venues.length; i++)
      update(venues[i]);
});


function update(name) {
    var timeout = getParameterByName('timeout', 5);
    
    $.ajax({
        url: name + '.js', 
        ifModified: true,
        cache: false,
        dataType: 'json',
        success: function(data) {
            try {
                if (data == null)
                    return;

                for (var i = 0; i < data.length; i++) {
                    updateData(JSON.parse(data[i]));
                }
            } catch (err) {
                
            }

        },    
        statusCode: {
            500: function() {
                timeout = 90;
            }
        },
        complete: function() {
            if (getParameterByName('no-update', 0) == 0)
                setTimeout(function() {update(name);}, timeout * 1000);                                        
        }
    });
}

function updateData(data) {
    if (data.mtTable == 0)
        return;
    
    var tr = findRow(data.mtTable);

    tr.html(formatRow(data));
    
    if (tr.children('.player').attr('onClick') == null) {
        var onclick = 'window.open("tickeronematch.html?venues=' + getParameterByName('venues', 'update') + '&table=' + data.mtTable + '", "oneMatch");';

        tr.children('.player, .service, .timeout').attr('onClick', onclick);
    }
    
    $('table tr:nth-child(even)').addClass('even');
    $('table tr:nth-child(odd)').addClass('odd');
    $('table tr:nth-child(even)').removeClass('odd');
    $('table tr:nth-child(odd)').removeClass('even');
}

function formatRow(data) {
   var row = '';
   
   // If this is a team match, write teams and team result in first line and individual result in second line
   var hasTeam = data.tmA != null && data.tmA.tmName != "" || data.tmX != null && data.tmX.tmName != "";

   var serviceLeft = data.serviceLeft && data.gameRunning ? ' service' : '';
   var serviceRight = data.serviceRight && data.gameRunning ? ' service' : '';

   row += '<td class="time">' + formatTime(data.mtDateTime) + '</td>';
   row += '<td class="table">' + data.mtTable + '</td>';
   row += '<td class="event">' + data.cpName + '</td>';
   row += '<td class="group">';
   row +=   '<a href="' + data.resultLocation + data.cpName + '_' + data.grName.replace(/\//g, '_') + '.html' + '" target="_blank">';
   row +=   data.grDesc;
   row +=   '</a>';
   row += '</td>';
   row += '<td class="round">' + data.mtRoundString + '</td>';
   row += '<td class="player left">';
   if (hasTeam)
       row += '<div class="teamname">' + (data.tmA == null ? '&nbsp;' : data.tmA.tmDesc) + '</div>';
   row += '<div class="playername">' + formatPlayers(data.plA, data.plB) + '</div>';    
   row += '</td>';
   row += '<td class="service left">';
   if (hasTeam)
       row += '<div class="teamservice">&nbsp;</div>';
   row += '<div class="playerservice' + serviceLeft + '"></div>';
   row += '</td>';
   row += '<td class="timeout left">';
   if (hasTeam)
       row += '<div class="teamtimeout">&nbsp;</div>';
   row += '<div class="playertimeout ' + (data.timeoutLeftRunning ? 'timeoutrunning' : '') + '">' + (data.timeoutLeft ? 'T' : '') + '</div>';
   row += '</td>';
   row += '<td class="player center">-</td>';
   row += '<td class="timeout right">';
   if (hasTeam)
       row += '<div class="teamtimeout">&nbsp;</div>';
   row += '<div class="playertimeout ' + (data.timeoutRightRunning ? 'timeoutrunning' : '') + '">' + (data.timeoutRight ? 'T' : '') + '</div>';
   row += '</td>';
   row += '<td class="service right">';
   if (hasTeam)
       row += '<div class="teamservice">&nbsp;</div>';
   row += '<div class="playerservice' + serviceRight + '"></div>';
   row += '</td>';
   row += '<td class="player right">';
   if (hasTeam)
       row += '<div class="teamname">' + (data.tmX == null ? '&nbsp;' : data.tmX.tmDesc) + '</div>';
   row += '<div class="playername">' + formatPlayers(data.plX, data.plY) + '</div>';
   row += '</td>';
   row += formatGames(data, hasTeam);
   row += '<td class="result">';
   if (hasTeam) {
       var link = data.resultLocation + data.cpName + '_' + data.grName.replace(/\//g, '_') + '_' + data.mtNr + '.html';

       row += '<div class="teamresult">';   

       if (data.tmA == null || data.tmX == null)                       
           row += '&nbsp;';
       else
           row += '<a href="' + link + '" target="teamMatches">' + data.tmA.mtRes + '&nbsp;:&nbsp;' + data.tmX.mtRes + '</a>';
       row += '</div>';
   }
   if (data.walkOver)
       row += '<div class="playerresult">w/o</div>';
   else if (data.matchRunning)
       row += '<div class="playerresult">' + data.mtResA + '&nbsp;:&nbsp;' + data.mtResX + '</div>';  
   else
       row += '<div class="playerresult">&nbsp;</div>';
   row += '</td>';

   return row;
}

function findRow(id) {
    // Lookup by id
    var tr = $('table tbody #' + id);
    if (tr != null && tr.length > 0)
        return tr;

    // Not found, insert
    var rows = $('table tbody tr');
    for (var i = 0; i < rows.length; i++) {
        var trID = $(rows[i]).attr('id');
        if (parseInt(trID) > id) {
            $('table tbody #' + trID).before('<tr id="' + id + '"></tr>');

            return $('table tbody #' + id);
        }
    }
    
    $('table tbody').append($('<tr id="' + id + '"></tr>'));
    return $('table tbody #' + id);
}

function formatPlayers(plA, plB) {
    if (plA == null || plA.plNr == 0)
        return '';

    var s = plA.plNr + '&nbsp;' + plA.psLast + ',&nbsp;' + plA.psFirst + '&nbsp;(' + plA.naName + ')';

    if (plB != null && plB.plNr > 0) {
        s += '<br>';
        s += plB.plNr + '&nbsp;' + plB.psLast + ',&nbsp;' + plB.psFirst + '&nbsp;(' + plB.naName + ')';
    }

    return s;
}

function formatGames(data, hasTeam) {
    if (data.mtSets == null || data.mtSets.length == 0  || !data.matchRunning) {
        return ('<td class="game" colspan="7">&nbsp;</td>');
    }
    
    var s = '';
    for (var i = 0; i < 7; i++) {   
        // Wenn finished, fill remaining columns
        if ( (i == data.mtSets.length) || !data.gameRunnung && data.mtSets[i][0] == 0 && data.mtSets[i][1] == 0 ||
              i == (data.mtResA + data.mtResX) && ((2 * data.mtResA > data.mtBestOf) || (2 * data.mtResX > data.mtBestOf)) ) {                         
            s += '<td class="game empty" colspan="' + (7 - i) + '">&nbsp;</td>';

            break;
        }        

        s += '<td class="game">';
        if (hasTeam)
            s += '<div class="teampoints">&nbsp;</div>';

        s += '<div class="playerpoints">'; 
        s += (data.mtSets[i][0] < 10 ? ' ' : '') + data.mtSets[i][0];
        s += '&nbsp;:&nbsp;' + data.mtSets[i][1];
        s += '</div></td>'; 

        if ( data.mtSets[i][0] == 0 && data.mtSets[i][1] == 0 ||
             data.mtSets[i][0] < 11 && data.mtSets[i][1] < 11 ||
             Math.abs(data.mtSets[i][0] - data.mtSets[i][1]) < 2 ) {
            if (i < 6)
                s += '<td class="game empty" colspan="' + (7 - i - 1) + '">&nbsp;</td>';

            break;
        }                    
    }

    return s;
}
