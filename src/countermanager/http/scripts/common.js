/* Copyright (C) 2020 Christoph Theis */

function getParameterByName(name, def) {
    name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.search);
    if(results == null && parent != undefined) {
        // If parent defines getParameterByName, use it.
        if (false && parent.getParameterByName !== undefined)
            return parent.getParameterByName(name, def);
         
         // Else try with parents location search
        results = regex.exec(parent.window.location.search);
    }
    
    // Check possible unwanted values of results and return def in these cases
    if(results === null)
        return def;
    else if (!Array.isArray(results))
        return def;
    else if (results.length < 2)
        return def;
    else if (typeof results[1] !== 'string')
        return def;
    else if (results[1].length === 0)
        return def;
    else
        return decodeURIComponent(results[1].replace(/\+/g, " "));
}

function formatString(s, len, appdx) {
    if (s === undefined)
        return '';
    
    if (len === undefined || len < 0)
        return s;
    
    if (len == 0)
        return '';
    
    if (s.length <= len)
        return s;
    
    if (appdx === undefined)
        appdx = '';
    
    if (len < appdx.length)
        appdx = '';
    
    return s.substring(0, len - appdx.length) + appdx;
}


function formatTime(time) {
    // iOS could not parse an iso date time, so cut out what we want
    if ( typeof time == 'string')
        return time.slice(11, 16);
    else if (typeof time != 'number')
        return '';
    
    var d = new Date(time);
    var hours = d.getHours();
    var minutes = d.getMinutes();

    var s = '';
    if (hours < 10)
        s += '0';
    s += hours;
    s += ':';
    if (minutes < 10)
        s += '0';
    s += minutes;

    return s;
}


function formatDate(date) {
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate();
    
    return cts;
}


function formatDateTime(date) {
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate() + ' ' + 
        (date.getHours() < 10 ? '0' : '') + date.getHours() + ':' + 
        (date.getMinutes() < 10 ? '0' : '') + date.getMinutes() + ':' + 
        (date.getSeconds() < 10 ? '0' : '') + date.getSeconds() + '.' + 
        (date.getMilliseconds() < 10 ? '00' : (date.getMilliseconds < 100 ? '0' : '')) + date.getMilliseconds();

    return cts;
}

function formatISODateTime(date) {
    if (typeof date != 'object')
        return formatISODateTime(new Date(date));
    
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate() + 'T' + 
        (date.getHours() < 10 ? '0' : '') + date.getHours() + ':' + 
        (date.getMinutes() < 10 ? '0' : '') + date.getMinutes() + ':' + 
        (date.getSeconds() < 10 ? '0' : '') + date.getSeconds() + '.' + 
        (date.getMilliseconds() < 10 ? '00' : (date.getMilliseconds < 100 ? '0' : '')) + date.getMilliseconds();

    return cts;
}

function formatISODate(date) {
    if (typeof date != 'object')
        return formatISODate(new Date(date));
    
    var cts = 
        date.getFullYear() + '-' + 
        (date.getMonth() < 9 ? '0' : '') + (date.getMonth() + 1) + '-' + 
        (date.getDate() < 10 ? '0' : '') + date.getDate();

    return cts;
}

function formatRound(match) {
    if (typeof match !== 'object')
        return '';
    
    if (!match.grModus || !match.mtRound || !match.mtMatch)
        return '';
    
    // Default where we don't use or can't use special round strings
    if (match.grModus == 1)
        return 'Rd.&nbsp;' + match.mtRound;
    if (match.grNofRounds || match.grNofMatches)
        return 'Rd.&nbsp;' + match.mtRound;
    if (match.grWinner != 1)
        return 'Rd.&nbsp;' + match.mtRound;
    if (!match.grSize)
        return 'Rd.&nbsp;' + match.mtRound;
    
    // Final, Semifinal, Quarterfinal
    // First we need max number of rounds
    var maxRounds = 1;
    while ((1 << maxRounds) < match.grSize)
        ++maxRounds;
    
    if (match.mtRound == maxRounds && match.mtMatch == 1)
        return 'F';
    if (match.mtRound == maxRounds - 1 && match.mtMatch <= 2)
        return 'SF';
    if (match.mtRound == maxRounds - 2 && match.mtMatch <= 4)
        return 'QF';
    
    // KO: Use "Rd of ..."
    if (match.grModus == 2)
        return 'Rd.&nbsp;of&nbsp;' + (1 << (maxRounds - match.mtRound + 1));
    
    // PLO: Use Pos x .. y
    var chunksize = match.grSize >> match.mtRound;
    var chunkno = Math.floor((match.mtMatch - 1) / chunksize);
    
    return 'Pos&nbsp;' + (1 + 2 * chunkno * chunksize) + '&mdash;' + (1 + 2 * (chunkno + 1) * chunksize - 1);    
}

function beep(duration) {
    var ctx = new(window.audioContext || window.webkitAudioContext);
    var type = 0;  // 0: sinus, 1: rechteck, 2: saegezahn, 3: dreieck

    duration = +duration;

    // Only 0-4 are valid types.
    // type = (type % 5) || 0;

    var osc = ctx.createOscillator();

    osc.type = type;

    osc.connect(ctx.destination);
    osc.noteOn(0);

    setTimeout(function () {
        osc.noteOff(0);
    }, duration);

}


// Size of an object, i.e. number of properties
function size(obj) {
    if (Object.keys != undefined)
        return Object.keys(obj).length;

    var size = 0;
    var key;

    for (key in obj) {
        if (obj.hasOwnProperty(key))
            ++size;
    }

    return size;
}


var throttleErrors = {};
function logError(e) {
    try {
        var ct = (new Date()).getDate();
        if (throttleErrors[e.message] === undefined) 
            throttleErrors[e.message] = 0;
        
        if (throttleErrors[e.message] > ct)
            return;

        // Disable for 60 seconds
        throttleErrors[e.message] = ct + 60 * 1000;

        var data = JSON.stringify(e, Object.getOwnPropertyNames(e));
        
        $.ajax({
            data: data,
            url: '../logError',
            type: 'POST',
            contentType: 'application/json'
        });
    } catch (err) {
        
    }
}

// Global error handler
window.onerror = function(message, url, line) {
    logError({message: message, fileName: url, lineNumber: line});
    return false;
};

