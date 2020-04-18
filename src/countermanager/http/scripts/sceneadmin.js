/* Copyright (C) 2020 Christoph Theis */

// Interface for admininistration of scenes

var sceneadmin = new function() {
    
    var gson = new com.google.gson.Gson();
    var charsetUTF = java.nio.charset.Charset.forName("UTF-8");
    var basedir = Packages.countermanager.prefs.Properties.getIniFile().getParent() + java.io.File.separator;
    
    this.save = function(name, scenes) {
        var file = new java.io.File(basedir + "http/display/scenes", name + ".json");
        var os = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), charsetUTF);

        os.write(gson.toJson(scenes));

        os.close();       
        
        return true;
    };
    
    this.load = function(name, timestamp) {
        var file = new java.io.File(basedir + "http/display/scenes", name + ".json");
        if (!file.exists()) {
            return '{}';
        }
        
        var lastModified = new java.util.Date(file.lastModified());
        
        if (timestamp == lastModified)
            return '{}';
        
        var bis = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), charsetUTF));
        var ret = "";
        var line;
        while ( (line = bis.readLine()) )
            ret += line;
        bis.close();
        
        var map = new java.util.HashMap();
        map.put('scenes', ret);
        map.put('timestamp', lastModified);
        
        return map;        
    };
    
    this.list = function() {
        if (!new java.io.File(basedir + "http/display/scenes").exists())
            new java.io.File(basedir + "http/display/scenes").mkdir();
        
        var list = new java.io.File(basedir + "http/display/scenes").listFiles();
            
        var ret = new java.util.ArrayList();
        for (var i = 0; i < list.length; i++) {
            if (list[i].getName().endsWith(".json"))
                ret.add(list[i].getName().substring(0, list[i].getName().length() - 5));
        }
        
        return gson.toJson(ret);
    };
    
    this.remove = function(name) {
        var file = new java.io.File(basedir + "http/display/scenes", name + ".json");
        if (file.exists()) {
            return file.delete();
        }
        
        return false;
    }
};

