/* Copyright (C) 2020 Christoph Theis */

/*
 * Parameter
 * cpName: Filter for event
 * cpNameList: dto, but a list of events
 * grStage: Filter for group stage
 * noUpdate: No update, default 0
 * oneGroup: Nur eine Gruppe je Seite, default 0
 * timeout: Zeit, bis weitergeschalten wird
 */

$(document).ready(function() {
    update();
});

function update() {
    if (parent != this && !parent.show())
        return;
    
    var options = {
        cpName : getParameterByName('cpName', ''),
        cpNameList : getParameterByName('cpNameList', ''),
        grStage : getParameterByName('grStage', '')
    };

    xmlrpc(
        "../RPC2", "ttm.listStandings", [options], 
        function success(data) {
            show(data, 0);
        }                     
        , function error(e) {
        }
    )
}


function show(data, start) {
    if (start >= data.length) {
        update();

        return;
    }                

    $('#entries tbody').empty();

    var origStart = start;
    var i = start;
    var rowCount = 0;
    
    var maxHeight = document.documentElement.clientHeight;
    var oneGroup = getParameterByName('oneGroup', 0) > 0;

    for (; i < data.length; i++) {
        // Add caption with each new group
        if (i == start || data[i].cpName != data[i-1].cpName || data[i].grName != data[i-1].grName) {
            // But first check if the old group fits on page
            var height = $('#entries tbody').height();
            if (height > maxHeight - 10) {
                $('#entries tbody tr.last').remove();
                
                // Set i to start, so we will start with this group again next time
                i = start;
                
                break;
            }
            
            start = i;
            
            $('#entries tbody tr').removeClass('last');

            // Start new page with new event
            if (start > origStart && (oneGroup || data[start].cpName != data[start-1].cpName || data[start].grStage != data[start-1].grStage))
                break;
            
            var tr = 
                '<tr class="last caption">' +
                '<td class="event" colspan="2">' + data[i].cpName + " - " + data[i].grDesc + '</td>' +
                '<td class="matches">' + 'Mts' + '</td>' +
                '<td class="matchpoints">' + 'Pts' + '</td>' +
                '<td class="pos">' + 'Pos' + '</td>' +
                '</tr>';

            rowCount = 0;
            
            $('#entries tbody').append(tr);
        }
        
        var tr = formatEntry(data[i], ++rowCount);

        $('#entries tbody').append(tr);
    }
    
    // Check the height again after the last match
    var height = $('#entries tbody').height();
    if (height > maxHeight - 10) 
        $('#entries tbody tr.last').remove();
    else    
        start = i;

    // Show next data (if there is any)
    if (getParameterByName('noUpdate', 0) == 0)
        setTimeout(function() {show(data, start);}, getParameterByName('timeout', start - origStart) * 1000);
}


// ----------------------------------------------------------------
// Helpers
function formatEntry(st, rowCount) {
    var ret;

    ret = '<tr class="last ' + ((rowCount % 2) == 0 ? 'even' : 'odd') + '">';
    
    if (st.cpType == 4) {
        ret += 
            '<td class="assoc">' + st.naName + '</td>' +
            '<td class="names">' + st.tmDesc + '</td>' +
            '<td class="matches">' + st.mtMatchCount + 
            '<td class="matchpoints">' + st.mtMatchPoints + 
            '<td class="pos">' + (st.stPos + st.grWinner - 1) + '</td>';
    } else if (st.cpType == 1) {
        ret += 
            '<td class="assoc">' + st.naName + '</td>' +
            '<td class="names">' + st.psLast + '</td>' +
            '<td class="matches">' + st.mtMatchCount + '</td>' + 
            '<td class="matchpoints">' + st.mtMatchPoints + '</td>' + 
            '<td class="pos">' + (st.stPos + st.grWinner - 1) + '</td>';
    } else {
        ret += 
            '<td class="assoc">' + st.plnaName + '<br>' + st.bdnaName + '</td>' +
            '<td class="names">' + st.plpsLast + '<br>' + st.bdpsLast + '</td>' +
            '<td class="matches">' + st.mtMatchCouont + '</td>' + 
            '<td class="matchpoints">' + st.mtMatchPoints + '</td>' + 
            '<td class="pos">' + (st.stPos + st.grWinner - 1) + '</td>';                    
    }

    ret += '</tr>';
    return ret;
}

