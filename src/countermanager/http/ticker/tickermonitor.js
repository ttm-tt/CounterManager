/* Copyright (C) 2020 Christoph Theis */

/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/*
 * Parameter:
 *   noFlag:         Anzeige der Flagge unterdruecken
 *   swap:           Swap the sides
 *   
 */

var noFlag = false;

$(document).ready(function() {
    noFlag = parseInt(getParameterByName('noFlag', 0));
    
    var venues = getParameterByName('venues', 'update').split(',');
    
    for (var i = 0; i < venues.length; i++)
      update(venues[i]);
    
});


var noFlag = 0;
var currentMatch = undefined;
var currentData = undefined;
var lastUpdateTime = undefined;

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

    setCurrentData(data);
}


function setCurrentData(data) {
    setCaption(data);
    
    setNames(data);
        
    $('#gamesleft').html(data.mtResA);
    $('#gamesright').html(data.mtResX);
    
    $('#schedule').addClass('hidden');
    $('#match').removeClass('hidden');                
 
    if (data === undefined) {
        $('#pointsleft').html('');
        $('#pointsright').html('');
        
        $('#gamesleft').html('');
        $('#gamesright').html('');
    } else if (!data.matchRunning) {
        $('#pointsleft').html('');
        $('#pointsright').html('');
        
        $('#gamesleft').html('');
        $('#gamesright').html('');
    } else if (data.mtSets.length < data.mtResA + data.mtResX) {
        $('#pointsleft').html('0');
        $('#pointsright').html('0');        
    } else if (!data.gameRunning) {
        var resA = data.mtSets[data.mtResA + data.mtResX - 1][0];
        var resX = data.mtSets[data.mtResA + data.mtResX - 1][1];

        $('#pointsleft').html(resA);
        $('#pointsright').html(resX);

        if (resA > resX)
            $('#gamesleft').html(data.mtResA - 1);
        else
            $('#gamesright').html(data.mtResX - 1);
    } else if (2 * data.mtResA > data.mtBestOf || 2 * data.mtResX > data.mtBestOf) {
        var resA = data.mtSets[data.mtResA + data.mtResX - 1][0];
        var resX = data.mtSets[data.mtResA + data.mtResX - 1][1];

        $('#pointsleft').html(resA);
        $('#pointsright').html(resX);

        if (resA > resX)
            $('#gamesleft').html(data.mtResA - 1);
        else
            $('#gamesright').html(data.mtResX - 1);     
    } else if (data.mtSets.length == data.mtResA + data.mtResX) {
        $('#pointsleft').html('0');
        $('#pointsright').html('0');        
    } else {
        $('#pointsleft').html(data.mtSets[data.mtResA + data.mtResX][0]);
        $('#pointsright').html(data.mtSets[data.mtResA + data.mtResX][1]);
    }
    
    if (data !== undefined) {
        if (data.timeoutLeft)
            $('#timeoutleft').removeClass('hidden');
        else
            $('#timeoutleft').addClass('hidden');            
        
        if (data.timeoutRight)
            $('#timeoutright').removeClass('hidden');
        else
            $('#timeoutright').addClass('hidden');
        
        if (data.timeoutLeftRunning || data.timeoutRightRunning) {
            if (data.timeoutLeftRunning)
                $('#timeoutleft').addClass('running');
            else
                $('#timeoutright').addClass('running');
        } else {
            $('#timeoutleft').removeClass('running');
            $('#timeoutright').removeClass('running');
        }                
    }
    
    var i = 0;
    for (i = 0; i < data.mtSets.length; i++) {   
        if (i == data.mtResA + data.mtResX)
            break;
        
        // Don't show last game if it is shown in the large display
        if (i == data.mtResA + data.mtResX - 1) {
            if (!data.gameRunning)
                break;
            
            if (2 * data.mtResA > data.mtBestOf || 2 * data.mtResX > data.mtBestOf) {
                if (data.matchRunning)
                    break;
            }
        }
        
        if (data.mtSets[i][0] || data.mtSets[i][1]) {
            if (data.mtSets[i][0] > data.mtSets[i][1])
                $('#game' + (i+1)).html(data.mtSets[i][1]);
            else
                $('#game' + (i+1)).html('-' + data.mtSets[i][0]);
        } else {
            $('#game' + (i+1)).html('');
        }
        
        if (data.mtSets[i][0] > data.mtSets[i][1]) {
            $('#game' + (i+1)).addClass('invert');
        } else {
            $('#game' + (i+1)).removeClass('invert');
        }
    }
    
    for (; i < 7; i++) {
        $('#game' + (i+1)).html('');   
        $('#game' + (i+1)).removeClass('invert');
    }
}


function setCaption(data) {
    var nationleft, nationright;
    var flaga = '', flagb = '', flagx = '', flagy = '';    
    
    switch (data.cpType) {
        case 1 :
            nationleft = data.plA.naName;
            nationright = data.plX.naName;
            
            flaga = formatFlag(data.plA.naName);
            flagx = formatFlag(data.plX.naName);
            
            break;
            
        case 2 :
        case 3 :
            if (data.plA.naName == data.plB.naName && data.plX.naName == data.plY.naName) {
                nationleft = data.plA.naName;   
                nationright = data.plX.naName; 
                
                flaga = formatFlag(data.plA.naName);
                flagx = formatFlag(data.plX.naName);
            } else {
                nationleft = (data.plA.naName + '&nbsp;/&nbsp;' + data.plB.naName);   
                nationright = (data.plX.naName + '&nbsp;/&nbsp;' + data.plY.naName);    
                
                flagb = formatFlag(data.plA.naName);
                flaga = formatFlag(data.plB.naName);
                flagx = formatFlag(data.plX.naName);
                flagy = formatFlag(data.plY.naName);
            }
            
            break;
            
        case 4 :
            nationleft = (data.tmA.tmName);
            nationright = (data.tmX.tmName);
            
            flaga = formatFlag(data.plA.naName);
            flagx = formatFlag(data.plX.naName);
                
            $('#teamresult span').html(data.tmA.mtRes + '&nbsp;:&nbsp;' + data.tmX.mtRes);
            break;
    }
    
    $('#nationleft span').html(nationleft);
    $('#nationright span').html(nationright);
    
    $('#flaga span').html(flaga);
    $('#flagb span').html(flagb);
    $('#flagx span').html(flagx);
    $('#flagy span').html(flagy);
}


function setNames(data) {
    
    var nameleft = '', nameright = '';
    switch (data.cpType) {
        case 1 :
            nameleft = formatName(data.plA);
            nameright = formatName(data.plX);
            
            break;
            
        case 2 :
        case 3 :
            nameleft = formatName(data.plA);
            nameright = formatName(data.plX);
            
            nameleft += '<br>' + formatName(data.plB);
            nameright += '<br>' + formatName(data.plY);
            
            break;
            
        case 4 :
            nameleft = formatName(data.plA);
            nameright = formatName(data.plX);
            
            if (data.plB !== undefined && data.plB.plNr > 0) {
                nameleft += '<br>' + formatName(data.plB);
                nameright += '<br>' + formatName(data.plY);
            }
    
            break;
    }
    
    $('#nameleft span').html(nameleft);
    $('#nameright span').html(nameright);
    
    if (data.serviceLeft)
        $('#nameleft span').addClass('service');
    else
        $('#nameleft span').removeClass('service');
    
    if (data.serviceRight)
        $('#nameright span').addClass('service');
    else
        $('#nameright span').removeClass('service');
}


function formatName(pl) {
    if (pl === undefined)
        return '';
    
    // return pl.psFirst.substring(0, 1) + '.&nbsp;' + pl.psLast.substring(0, 10);
    return formatString(pl.psLast, 11);
}


function formatFlag(name) {
    if (noFlag == 1)
        return '';
    
    if (name == '')
        return '';
    
    return '<img src="' + 'img/' + name + '.svg" onload="$(this).removeClass(\'invisible\'); return false;" class="invisible"></img>';
}
