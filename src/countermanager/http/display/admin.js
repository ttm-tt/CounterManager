/* Copyright (C) 2020 Christoph Theis */


var template =             
    '<tr>' +
    '<td><input id="url" class="url" size="80" value=""></input></td>' +
    '<td><select id="type"><option value="duration">Duration</option><option value="count" selected="selected">Count</option></select></td>' +
    '<td><input id="value" class="value" size="4" value="1"></input></td>' +
    '<td><button type="button" id="del" class="del" onclick="del($(this))">-</button></td>' +
    '<td><button type="button" id="add" class="add" onclick="add($(this))">+</button></td>' +
    '</tr>';


function format(str) {
    var res = str;
    for (var i = 1; i < arguments.length; i++)
        res = res.replace('{' + (i-1) + '}', arguments[i]);
    
    return res;
}

                
$(document).ready(function() {
    var tr = format(template, '', '');
    $('#scene tbody').append(tr);   
    $('.editable-select').editableSelect();
    xmlrpc(
        "../RPC2", "sceneadmin.list", [],
            function success(data) {
                var files = JSON.parse(data);
                var instances = $('.editable-select');
                for (var i = 0; i < files.length; i++)
                    instances.editableSelect('add', files[i]);
            }
    );    
});

function load() {
    var scene = $('#scenes').val();
    
    if (scene == "")
        return;
    
    xmlrpc(
        "/RPC2", "sceneadmin.load", [scene],
        function success(data) {
            data = JSON.parse(data['scenes']);
            if (data == undefined)
                return;

            $('#scene tbody').empty();

            for (var i = 0; i < data.length; i++) {
                var tr = '';

                tr = template;

                if (data[i].value == undefined) {
                    data[i].type = 'duration';
                    data[i].value = data[i].duration;
                }

                $('#scene tbody').append(tr);
                $('#scene tbody tr:last-child #url').val(data[i].url);
                $('#scene tbody tr:last-child #type').val(data[i].type);
                $('#scene tbody tr:last-child #value').val(data[i].value);
            }

            if (data.length == 0) {
                var tr = format(template, '', '');
                $('#scene tbody').append(tr);
            }
        }
    );
}
    
function save() {
    var scenes = [];
    var rows = $('#scene tbody tr');
    var scene = $('#scenes').val();
    
    if (scene == "")
        return;    
    
    for (var i = 0; i < rows.length; i++) {
        var url = $(rows[i]).find('#url').val();
        var type = $(rows[i]).find('#type').val();
        var value = parseInt($(rows[i]).find('#value').val());
        
        if (url == '' || value == 0)
            continue;
        
        scenes[scenes.length] = {url : url, type : type, value : value};
    }
    
    xmlrpc(
        "/RPC2", "sceneadmin.save", [scene, scenes],
            function success(data) {
                
            }
    );    
}    

function remove() {
    var scene = $('#scenes').val();
    
    if (scene == "")
        return;    
    
    if (scene == "")
        return;    
    
    xmlrpc(
        "/RPC2", "sceneadmin.remove", [scene],
            function success(data) {
                location.reload();
            }
    );    
}    

function add(button) {
    var tr = template;
    var row = button.parent().parent();
    row.after(tr);
}


function del(button) {
    var row = button.parent().parent();
    row.remove();
    
    if ($('#scenes tbody tr').length == 0) {
        var tr = format(template, '', '');
        $('#scenes tbody').append(tr);
    }
}


