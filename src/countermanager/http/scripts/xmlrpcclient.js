/* http://www.zentus.com/js/xmlrpc.js.html */
/*
 * Copyright (c) 2008 David Crawshaw <david@zentus.com>
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

/*
 * An XML-RPC library for JavaScript.
 *
 * The xmlrpc() function is the public entry point.
 */

/*
 * Execute an XML-RPC method and return the response to 'callback'.
 * Parameters are passed as JS Objects, and the callback function is
 * given a single JS Object representing the server's response.
 */
var xmlrpc = function(server, method, params, callback, callErr, callFinal, async = true) {
    if (callErr == null)
        callErr = alert;

    var request = window.XMLHttpRequest ? new XMLHttpRequest()
        : new ActiveXObject("MSXML2.XMLHTTP.3.0");
    request.open("POST", server, async);
    request.onreadystatechange = function() {
        if (request.readyState != 4)
            return; // TODO: callbacks?
        try {
            if (request.status != 200) {
                callErr("connection error " + request.status);
                return;
            }

            var ret = null;
            try {
                if (request.responseXML)
                    ret = xmlrpc.parseResponse(request.responseXML);
                else
                    throw "bad xml: '" + request.responseText + "'";
            } catch (err) {
                err.message = "xmlrpc: " + err.message;
                callErr(err);
                throw err;
            }

            try {
                callback(ret, request);
            } catch (err) {
                err.message = "callback: " + err.message;
                callErr(err);
                throw err;
            }
        } finally {
            if (callFinal)
                callFinal();
        }
    };

    var sending = xmlrpc.writeCall(method, params);
    request.send(sending);
};

xmlrpc.writeCall = function(method, params) {
    var out;
    var par;
    
    out = "<?xml version=\"1.0\"?>\n";
    out += "<methodCall>\n";
    out += "<methodName>"+ method + "</methodName>\n";

    out += "<params>\n";
        
    for (var i=0; i < params.length; i++) {
        par = "";
        out += "<param><value>";
        out += xmlrpc.writeParam(params[i], par);
        out += "</value></param>";
    }

    out += "</params>\n";

    out += "</methodCall>\n";
    return out;
};

xmlrpc.writeParam = function(param, par) {
    if (param == null) {
        par += "<nil />";
        return par;
    }
        
    switch (typeof(param)) {
        case "boolean":     
            par += "<boolean>" + (param ? 1 : 0) + "</boolean>";
            return par;
        case "string":
            param = param.replace(/</g, "&lt;");
            param = param.replace(/&/g, "&amp;");
            par += "<string>" + param + "</string>";
            return par;
        case "undefined":   
            par += "<nil/>";
            return par;
        case "number":
            if (Math.abs(param) > 0x7FFFFFFF || /\./.test(param))
                par += "<double>" + param + "</double>";
            else
                par += "<int>" + param + "</int>";
            return par;
        case "object":
            if (param.constructor == Array) {
                par += "<array><data>\n";
                for (var i in param) {
                    par += "<value>";
                    par += xmlrpc.writeParam(param[i], "");
                    par += "</value>\n";
                }
                par += "</data></array>";
                return par;
            } else if (param.constructor == Date) {
                par += "<dateTime.iso8601>";
                par += param.getUTCFullYear();
                if (param.getUTCMonth() + 1 < 10)
                    par += "0";
                par += (param.getUTCMonth() + 1);
                if (param.getUTCDate() < 10)
                    par += "0";
                par += param.getUTCDate() + "T";
                if (param.getUTCHours() < 10)
                    par += "0";
                par += param.getUTCHours() + ":";
                if (param.getUTCMinutes() < 10)
                    par += "0";
                par += param.getUTCMinutes() + ":";
                if (param.getUTCSeconds() < 10)
                    par += "0";
                par += param.getUTCSeconds();
                par += "</dateTime.iso8601>";
                return par;
            } else { /* struct */
                par += "<struct>\n";
                for (var i in param) {
                    par += "<member>";
                    par += "<name>" + i + "</name><value>";
                    par += xmlrpc.writeParam(param[i], "");
                    par += "</value></member>\n";
                }
                par += "</struct>\n";
                return par;
            }
    }
};

xmlrpc.parseResponse = function(dom) {
    var methResp = dom.childNodes[dom.childNodes.length - 1];
    if (methResp.nodeName != "methodResponse")
        throw "malformed <methodResponse>, got " + methResp.nodeName;

    var params = methResp.childNodes[0];
    if (params.nodeName == "fault")  {
        var fault = xmlrpc.parse(params.childNodes[0]);
        throw fault["faultString"];
    }
    if (params.nodeName != "params")
        throw "malformed <params>, got <" + params.nodeName + ">";

    var param = params.childNodes[0];
    if (param.nodeName != "param")
        throw "malformed <param>, got <" + param.nodeName + ">";

    var value = param.childNodes[0];
    if (value.nodeName != "value")
        throw "malformed <value>, got <" + value.nodeName + ">";

    return xmlrpc.parse(value);
};

xmlrpc.parse = function(value) {
    if (value.nodeName != "value")
        throw "parser: expected <value>";

    var type = value.childNodes[0];
    if (type == null)
        throw "parser: expected <value> to have a child";
    switch (type.nodeName) {
        case "boolean":
            return type.childNodes[0].data == "1" ? true : false;
        case "i4":
        case "int":
            return parseInt(type.childNodes[0].data);
        case "double":
            return parseFloat(type.childNodes[0].data);
        case "#text": // Apache XML-RPC 2 doesn't wrap strings with <string>
            return type.data;
        case "string":
            return type.childNodes.length > 0 ? type.childNodes[0].data : "";
        case "array":
            var data = type.childNodes[0];
            var res = new Array(data.childNodes.length);
            for (var i=0; i < data.childNodes.length; i++)
                res[i] = xmlrpc.parse(data.childNodes[i]);
            return res;
        case "struct":
            var members = type.childNodes;
            var res = {};
            for (var i=0; i < members.length; i++) {
                var name = members[i].childNodes[0].childNodes[0].data;
                var value = xmlrpc.parse(members[i].childNodes[1]);
                res[name] = value;
            }
            return res;
        case "dateTime.iso8601":
            var s = type.childNodes[0].data;
            var d = new Date();
            d.setUTCFullYear(s.substr(0, 4));
            d.setUTCMonth(parseInt(s.substr(4, 2)) - 1);
            d.setUTCDate(s.substr(6, 2));
            d.setUTCHours(s.substr(9, 2));
            d.setUTCMinutes(s.substr(12, 2));
            d.setUTCSeconds(s.substr(15, 2));
            return d;
        case "base64":
            alert("TODO base64"); // XXX
        default:
            throw "parser: expected type, got <"+type.nodeName+">";
    }
}
