/* Copyright (C) 2020 Christoph Theis */

/*
 
 #caption    [Time][Table][Event][Group][Round]
 #team       [Team A - TeamX][ResA : ResX]
 #detail
 [Player A] [G1][G2]... [ResA] 
 -----------------------------
 [Player X] [G1][G2]... [ResX]
 
 */
$(document).ready(function() {
    var venues = getParameterByName('venues', 'update').split(',');

    for (var i = 0; i < venues.length; i++)
        update(venues[i]);
});


function update(name) {
    var timeout = getParameterByName('timeout', 5);
    
    $.ajax({
        url: '../' + name + '.js',
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

    var caption = '<div class="caption">';
    caption += '<div class="time">' + formatTime(data.mtDateTime) + '</div>';
    caption += '<div class="table">' + 'T.&nbsp;' + data.mtTable + '</div>';
    caption += '<div class="event">' + data.cpName + '</div>';
    caption += '<div class="group">' + data.grDesc + '</div>';
    caption += '<div class="round">' + data.mtRoundString + '</div>';
    caption += '</div>';

    var team = '';

    if (hasTeam) {
        team += '<div class="team">';
        team += '<div class="names">';
        team += (data.tmA == null ? '&nbsp;' : data.tmA.tmDesc);
        team += '&nbsp;-&nbsp;'
        team += (data.tmX == null ? '&nbsp;' : data.tmX.tmDesc);
        team += '</div>';
        team += '<div class="result">';
        if (data.tmA != null && data.tmX != null) {
            team += data.tmA.mtRes + '&nbsp;-&nbsp;' + data.tmX.mtRes;
        }
        team += '</div>';
        team += '</div>';
    }

    var detail = '<div class="detail"><div class="match">';

    var left = '<div class="left">';
    var right = '<div class="right">';

    left += '<div class="player">' + formatPlayers(data.plA, data.plB, hasTeam) + '</div>';
    right += '<div class="player">' + formatPlayers(data.plX, data.plY, hasTeam) + '</div>';

    left += formatGames(data, 0);
    right += formatGames(data, 1);

    left += '<div class="result">' + data.mtResA + '</div>';
    right += '<div class="result">' + data.mtResX + '</div>';

    left += '</div>';
    right += '</div>';

    detail += left + right + '</div></div>';

    return '<td>' + caption + team + detail + '</td>';

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

function formatPlayers(plA, plB, isTeam) {
    if (plA == null || plA.plNr == 0)
        return '';

    var s = plA.psLast + ',&nbsp;' + plA.psFirst;
    if (!isTeam)
        s += '&nbsp;(' + plA.naName + ')';

    if (plB != null && plB.plNr > 0) {
        s += '<br>';
        s += plB.psLast + ',&nbsp;' + plB.psFirst;

        if (!isTeam)
            s += '&nbsp;(' + plB.naName + ')';
    }

    return s;
}

function formatGames(data, j) {
    var s = ''
    var i = 0;

    for (i = 0; i < 7; i++) {
        if (data.mtSets == null || i == data.mtSets.length || !data.matchRunning)
            break;

        if (data.mtSets[i][0] == 0 && data.mtSets[i][1] == 0)
            break;

        s += '<div class="game">' + data.mtSets[i][j] + '</div>';
    }

    for (; i < 7; i++) {
        s += '<div class="game">&nbsp;</div>';
    }

    return s;
}
