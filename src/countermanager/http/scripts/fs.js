/* Copyright (C) 2020 Christoph Theis */

/* global java, Packages */

// An XmlRpc JS Script must have an object with the name of that script.
// All functions are invoked on that object.
var fs = new function() {
    this.folderContent = function(args) {
        var array = new java.util.Vector();
        
        if (arguments.length == 0 || args.get('alias') == null)
            return array;
        
        var alias = args.get('alias');
        if (!alias.startsWith('/'))
            alias = '/' + alias;
        
        var path = Packages.countermanager.http.HTTP.getDefaultInstance().resolveAlias(alias);
        if (path == null)
            return array;
        
        var ext = undefined;
        if (args.get('ext') != null)
            ext = args.get('ext').split(",");
        
        var files = path.listFiles();
        for (var i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                continue;
            
            if (!files[i].isFile())
                continue;
            
            if (ext !== undefined) {
                for (var j = 0; j < ext.length; j++) {
                    if (files[i].getName().endsWith('.' + ext[j])) {
                        array.add(files[i].getName());
                        break;
                    }
                }                       
            } else {
                array.add(files[i].getName());            
            }
        }
        
        return array;
    };
};