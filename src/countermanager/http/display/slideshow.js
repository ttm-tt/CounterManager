/* Copyright (C) 2020 Christoph Theis */

var path = null;
var patthern = null;
var ext  = null;

var urls = [];

$(document).ready(function() {
    path = getParameterByName('path', null);
    if (!path.startsWith('/'))
        path = '/' + path;
    if (!path.endsWith('/'))
        path = path + '/';
    
    pattern = getParameterByName('pattern', null);
    
    ext = getParameterByName('ext', null);
    
    update();
});

function update() {
    if (parent != this && !parent.show())
        return;

    $.getJSON(path, {'ext' : ext, 'pattern' : pattern})
        .done(
            function(data) {
                urls = [];
                for (var i = 0; i < data.length; i++) {
                    urls.push(path + data[i]);
                }
            }
        )
        .always(
            function() {
                doComplete(0);
            }
        );
}

function doComplete(idx) {
    if (idx == urls.length) {
        update();
        return;
    }
    
    $('#img').attr('src', urls[idx]);
    
    if (getParameterByName('noUpdate', 0) != 0)
        return;
    
    var timeout = getParameterByName('timeout', 10);
    
    setTimeout(function() {doComplete(idx + 1);}, timeout * 1000);
}

