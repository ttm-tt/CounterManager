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
    if(results == null)
        return def;
    else
        return decodeURIComponent(results[1].replace(/\+/g, " "));
}

function formatString(s, len, appdx) {
    if (s === undefined)
        return '';
    
    if (len < 0)
        return '';

    if (len === undefined || len == 0)
        return s;
    
    if (s.length <= len)
        return s;
    
    if (appdx === undefined)
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

