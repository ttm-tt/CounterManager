/* Copyright (C) 2020 Christoph Theis */

/*
 *  Parameter:
 *      noCards     Don't display the row with cards in the input   default 0
 *      fromTable   Show only tables starting at                    default all
 *      toTable     Show only tables up to                          default all
 */


$(document).ready(function() {
    // TODO: Nur bis jetzt, also 'to' : date anhaengen.
    var args = { };
    if (getParameterByName('fromTable', 0) != 0)
        args['fromTable'] = getParameterByName('fromTable', 0);
    if (getParameterByName('toTable', 0) != 0)
        args['toTable'] = getParameterByName('toTable', 0);
    
    update([args]);
});

function update(args) {
    $.ajax({
        url: '../counter/list_matches',
        type: 'POST',
        data: JSON.stringify(args),
        dataType: 'json',
        success: function(data) {
            var i;
            
            $('#matches tbody').empty();
            for (i = 0; i < data.length; i++) {
                if (data[i].mtTable == 0)
                    continue;

                var tr = formatLine(data[i]);
                $('#matches tbody').append(tr);
            }
        },
        error: function() {},       
        complete: function() {
            setTimeout(function() {update(args);}, 60 * 1000);            
        }
    });
}

function formatLine(match) {
    var res = '<tr>';
    var nocards = '';
    if (getParameterByName('noCards', 0) == 1)
        nocards = '&no-cards=1';
    
    res += '<td class="table"><a href="counter.html?table=' + match.mtTable + nocards + '" target="_blank">Table ' + match.mtTable + '</a></td>';

    // iOS could not parse an ISO date time. So just cut out what we want to use
    res += '<td class="time">' + formatTime(match.mtDateTime) + '</td>';

    res += '<td class="mtnr">' + match.mtNr + '</td>';

    res += '<td class="event">' + match.cpName + '</td>';

    if (match.cpType == 4) {
        var tmAtmDesc = match.tmA.tmDesc == undefined ? '' : match.tmA.tmDesc;
        var tmXtmDesc = match.tmX.tmDesc == undefined ? '' : match.tmX.tmDesc;
        res += '<td class="name left">' + tmAtmDesc.substring(0, 7) + '</td>' +
                '<td class="name center">-</td>' +
                '<td class="name right">' + tmXtmDesc.substring(0, 7) + '</td>';
    } else if (match.cpType == 2 || match.cpType == 3) {
        var plApsLast = match.plA.psLast == undefined ? '' : match.plA.psLast.substring(0, 7);
        var plBpsLast = match.plB.psLast == undefined ? '' : match.plB.psLast.substring(0, 7);
        var plXpsLast = match.plX.psLast == undefined ? '' : match.plX.psLast.substring(0, 7);
        var plYpsLast = match.plY.psLast == undefined ? '' : match.plY.psLast.substring(0, 7);

        var plApsFirst = match.plA.psFirst == undefined ? '' : match.plA.psFirst.substring(0, 1) + '. ';
        var plBpsFirst = match.plB.psFirst == undefined ? '' : match.plB.psFirst.substring(0, 1) + '. ';
        var plXpsFirst = match.plX.psFirst == undefined ? '' : match.plX.psFirst.substring(0, 1) + '. ';
        var plYpsFirst = match.plY.psFirst == undefined ? '' : match.plY.psFirst.substring(0, 1) + '. ';

        res += '<td class="name left">' + plApsFirst+ plApsLast + '<br>' + plBpsFirst + plBpsLast + '</td>' +
                '<td class="name center">-</td>' +
                '<td class="name right">' + plXpsFirst + plXpsLast + '<br>' + plYpsFirst + plYpsLast + '</td>';
    } else {
        var plApsLast = match.plA.psLast == undefined ? '' : match.plA.psLast.substring(0, 7);
        var plXpsLast = match.plX.psLast == undefined ? '' : match.plX.psLast.substring(0, 7);

        var plApsFirst = match.plA.psFirst == undefined ? '' : match.plA.psFirst.substring(0, 1) + '. ';
        var plXpsFirst = match.plX.psFirst == undefined ? '' : match.plX.psFirst.substring(0, 1) + '. ';

        res += '<td class="name left">' + plApsFirst + plApsLast + '</td>' +
                '<td class="name center">-</td>' +
                '<td class="name right">' + plXpsFirst + plXpsLast + '</td>';
    }

    if (match.cpType == 4) {
        res += '<td class="result">' + match.mttmResA + '&nbsp;-&nbsp;' + match.mttmResX + '</td>';        
    } else {
        res += '<td class="result">' + match.mtResA + '&nbsp;-&nbsp;' + match.mtResX + '</td>';
    }
    res += '</tr>';

    return res;
}
