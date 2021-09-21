/* Copyright (C) 2020 Christoph Theis */


$(document).ready(function() {
    // Load specific css file
    var fileref=document.createElement("link");
    fileref.setAttribute("rel", "stylesheet");
    fileref.setAttribute("type", "text/css");
    fileref.setAttribute("href", getParameterByName('config', 'default') + '.css');
    document.getElementsByTagName("head")[0].appendChild(fileref);

    update();
});

var scene = [];
var newScene = undefined;
var timestamp = 0;
var idx = -1;
var endtime = undefined;
var count = -1;
var cache = new Array();

function show() {
    // Nothing to change if there is only one page to show
    if (scene.length == 1) {
        // In this case nextURL will always return false so we would not reload
        // a frame showing a single URL. So check here with parent if the parent
        // will advance to the next URL
        if (parent != this && !parent.show())
            return false;
        
        return true;        
    }
    
    if (count == 0) {
        if (endtime == undefined)
            endtime = (new Date()).getTime();
        return false;
    }

    if (count > 0)
        --count;

    return true;
}



function update() {
    xmlrpc(
        "../RPC2", "sceneadmin.load", [getParameterByName('config', 'default'), timestamp], 
        function success(data) {
            try {
                // Data ia a map {timestamp: x, scenes: y}
                if (data['timestamp'] != undefined)
                    timestamp = data['timestamp'];
                
                // map[scenes'] may be undefined, e.g. if not changed since last request.
                // In this case we behave as if nothing has changed.
                if (data['scenes'] != undefined)
                    newScene = JSON.parse(data['scenes']);
                else
                    newScene = undefined;
            } catch (err) {
                timestamp = 0;
                newScene = undefined;
            }
        },                    
        function error() {
        },
        function complete() {
            try {
                doComplete();
            } catch (err) {
                logError(err);
            }
            setTimeout(function() {update();}, 1000);                                        
        }
    );
}

function doComplete() {
    var equal = false;
    
    if (scene != undefined && newScene != undefined) {        
        equal = scene.length == newScene.length;
        
        // Remove unreferenced cache entries
        for (var i = 0; i < scene.length; i++) {
            equal = equal &&
                scene[i].url == newScene[i].url &&
                scene[i].type == newScene[i].type &&
                scene[i].value == newScene[i].value;
            
            var found = false;
            for (var j = 0; !found && j < newScene.length; j++) {
                found = scene[i].url == newScene[j].url;
            }

            if (!found)
                cache[scene[i].url] = undefined;
        }
    }
    
    if (equal)
        newScene = undefined;
    
    if (newScene != undefined) {
        scene = newScene;
        newScene = undefined;
        idx = -1;
        starttime = undefined;
    }

    if (nextUrl()) {
        if (++idx  >= scene.length) {
            idx = 0;
            
            // End of list. Check with parent if the parent wants to advance.
            // We will reach this point only if there are more than 1 URL
            // in the list (see show). 
            if (parent != this && !parent.show())
                return;
        }

        if (idx < scene.length) {
            if (scene[idx].type == 'count') {
                count = scene[idx].value;
                endtime = undefined;
            } else if (scene[idx].type == 'duration') {
                count = -1;
                endtime = (new Date()).getTime() + scene[idx].value * 1000;
            } else {
                count = -1;
                endtime = (new Date()).getTime() + 10000;
            }

            // Force reload of page
            var url = scene[idx].url;
            if (url.indexOf('?') < 0)
                url += '?_=' + (new Date()).getTime();
            else
                url += '&_=' + (new Date()).getTime();

            let parentSearch = new URLSearchParams(window.location.search);
            let clientSearch = new URLSearchParams(url.substr(url.indexOf('?')));
            
            for (const [key, value] of parentSearch) {
                if (!clientSearch.has(key))
                    clientSearch.append(key, value);
            }
            
            url = url.substr(0, url.indexOf('?')) + '?' + clientSearch.toString();
            $('#if').attr('src', url);
        }
    }            
}

function nextUrl() {
    if (idx < 0 || idx >= scene.length)
        return true;

    if (endtime != undefined && endtime < (new Date()))
        return true;

    return false;
}

function storeInCache(data) {
    cache[scene[idx].url] = data;
}

function loadFromCache() {
    if (idx < 0 || idx >= scene.lenght)
        return undefined;

    return cache[scene[idx].url];
}

