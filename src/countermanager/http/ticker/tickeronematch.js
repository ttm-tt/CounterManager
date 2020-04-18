/* Copyright (C) 2020 Christoph Theis */

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
            if (data == null)
                return;

            for (var i = 0; i < data.length; i++) {
                updateData(JSON.parse(data[i]));
            }

        },             
        statusCode : {
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
    var mtTable = getParameterByName('table', 1);
    if (data.mtTable != mtTable)
        return;
    
    $('#table').html('T. ' + data.mtTable);
    $('#event').html(data.cpDesc);
    $('#group').html(data.grDesc);
    $('#round').html(data.mtRoundString);

    if (data.tmA != null)
        $('#teamleft').html(data.tmA.tmDesc);
    else
        $('#teamleft').html('&nbsp;');
    
    if (data.tmX != null)
        $('#teamright').html(data.tmX.tmDesc);
    else
        $('#teamright').html('&nbsp;');    
   
    if (data.tmA != null && data.tmX != null) {
        var link = data.resultLocation + data.cpName + '_' + data.grName.replace(/\//g, '_') + '_' + data.mtNr + '.html';
        if (parent != undefined && $('iframe#teamMatches', parent.document).length > 0)
            $('#teamcenter').html('<a href="#" onClick="parent.window.open(\'' + link + '\', \'teamMatches\');">' + data.tmA.mtRes + '&nbsp;:&nbsp;' +data.tmX.mtRes + '</a>');
        else
            $('#teamcenter').html(data.tmA.mtRes + '&nbsp;:&nbsp;' +data.tmX.mtRes);
    } else
        $('#teamcenter').html('');
    
    var twoFlags = 
            data.plB != undefined && data.plY != undefined && 
            (data.plA.naName != data.plA.naName || data.plX.naName != data.plY.naName);
    
    $('#playerleft').html(formatPlayers(data.plA, data.plB, twoFlags));    
    $('#playerright').html(formatPlayers(data.plX, data.plY, twoFlags));
    
    $('#gamesleft').html(data.mtResA);
    $('#gamesright').html(data.mtResX);
    
    if (data.gameRunning && data.mtSets.length > data.mtResA + data.mtResX) {
        $('#pointsleft').html(data.mtSets[data.mtResA + data.mtResX][0]);
        $('#pointsright').html(data.mtSets[data.mtResA + data.mtResX][1]);
    } else if ((data.mtResA > 0 || data.mtResX > 0) && data.mtSets.length > (data.mtResA + data.mtResX - 1)) {
        $('#pointsleft').html(data.mtSets[data.mtResA + data.mtResX - 1][0]);
        $('#pointsright').html(data.mtSets[data.mtResA + data.mtResX - 1][1]);
    } else {        
        $('#pointsleft').html('&nbsp;');
        $('#pointsright').html('&nbsp;');
    }
        
    var i = 0;
    for (i = 0; i < data.mtBestOf; i++)
        $('#gamehistory #game' + (i+1)).css('visibility', 'visible');
    for (; i < 7; i++)
        $('#gamehistory #game' + (i+1)).css('visibility', 'hidden');
    
    if (data.gameRunning || data.mtResA > 0 || data.mtResX > 0) {
        for (i = 0; i < data.mtSets.length; i++) {
            if (data.mtSets[i][0] < 11 && data.mtSets[i][1] < 11)
                break;;
            
            $('#gamehistory #game' + (i+1)).html('' + data.mtSets[i][0] + '&nbsp;:&nbsp;' + data.mtSets[i][1]);
            
            if (data.mtSets[i][0] < data.mtSets[i][1] + 2 && data.mtSets[i][1] < data.mtSets[i][0] + 2)
                break;        
        }
        
        for (; i < 7; i++)
            $('#gamehistory #game' + (i+1)).html('&nbsp;');        
        
    } else if (data.mtResA == 0 && data.mtResX == 0) {
        for (i = 0; i < 7; i++)
            $('#gamehistory #game' + (i+1)).html('&nbsp;');
    }   
}


function formatPlayers(plA, plB, twoFlags) {
    if (plA == null || plA.plNr == 0)
        return '';
    
    var names = plA.psFirst + '&nbsp;' + plA.psLast + '&nbsp;(' + plA.naName + ')';
    var flags = '<img src="/flags/' + plA.naName + '.svg"></img>';

    if (plB != null && plB.plNr > 0) {
        names += '<br>';
        names += plB.psFirst + '&nbsp;' + plB.psLast + '&nbsp;(' + plB.naName + ')';
        
        if (twoFlags || plB.naName !== plA.naName) {
            flags += '&nbsp;'
            flags += '<img src="/flags/' + plB.naName + '.svg"></img>';
        }
    }

    return flags + '<br><br>' + names;
}


// Function to put the page into an iframe
function load() {
  var iframe = parent.document.getElementById("oneMatch");
  if (iframe == null)
    return;

    // Set margin to 0
    document.body.style.margin = '0px';

    // iframe.style.display = "body";
    iframe.style.height = document.body.scrollHeight + 'px';  
    this.saveX = parent.document.body.scrollLeft;
    this.saveY = parent.document.body.scrollTop;     

    parent.scrollTo(0, iframe.offsetTop);
}

function close() {
  var iframe = parent.document.getElementById("oneMatch");
  if (iframe == null)
    return;

  iframe.style.height = "0px";
  parent.scrollTo(this.saveX, this.saveY);
}      



