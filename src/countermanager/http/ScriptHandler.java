/* Copyright (C) 2020 Christoph Theis */

/* Copyright (C) 2020 Christoph Theis */

package countermanager.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import countermanager.prefs.Properties;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import javax.script.Invocable;
import javax.script.ScriptException;


public class ScriptHandler implements HttpHandler {

    private javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
    private javax.script.ScriptEngine jsengine = manager.getEngineByName("js");

    private String alias;
    private String script;
    
    // Resolve path for scripts: sources, dist, cwd
    private static File[] pathes = new File[] {
            new File("../src/countermanager/http/scripts"),
            new File(Properties.getIniFile().getParent() + File.separator + "http/scripts"),
            new File(".")
    };
    
    public ScriptHandler(String alias, String script) {
        this.alias = alias;
        this.script = script;
    }
    
    @Override
    public void handle(HttpExchange he) throws IOException {
        String uri = he.getRequestURI().getPath();
        
        if (uri.contains("/../")) {
            HTTP.sendErrorResponse(he, 404);  // Eigentlich etwas illegales
        }
        
        // Resolve actual path of script
        
        File file = null;
        
        for (File path : pathes) {
            file = new File(path, script);
            if (file.exists())
                break;
        }
                        
        if (file == null || !file.exists()) {
            HTTP.sendErrorResponse(he, 404);
            
            return;
        }    
        
        // Resolve uri
        if (alias != null && !alias.isEmpty()) {
            if (!uri.startsWith(alias)) {
                HTTP.sendErrorResponse(he, 404);
                
                return;
            }
            
            uri = uri.substring(alias.length());
        }
        
        if (uri.startsWith("/"))
            uri = uri.substring(1);
        
        if (uri.endsWith("/"))
            uri = uri.substring(0, uri.length() - 1);
        
        // uri now contains a method path, or nothing at all
        // Split along "/" to resolve to a object path
        // But filter out empty elements
        String[] tmp = java.util.stream.Stream.of(uri.split("/")).filter(w -> !w.isEmpty()).toArray(String[]::new);
        
        // If empty set to last non-empty part of 
        if (tmp.length == 0) {
            String[] aliasSplit = java.util.stream.Stream.of(alias.split("/")).filter(w -> !w.isEmpty()).toArray(String[]::new);
            tmp = new String[] {aliasSplit[aliasSplit.length - 1]};            
        }
        
        Map<String, List<String>> args = splitQuery(he.getRequestURI());
        
        try {
            // Eval script
            jsengine.eval(new FileReader(file));
            
            jsengine.put("he", he);
            
            // TODO full path
            Object ret = ((Invocable) jsengine).invokeFunction(tmp[0], args);
            if (ret == null) {
                // Should be done by caller
            } else if (ret instanceof String) { 
                he.getResponseHeaders().add("Content-Type", "text/html");
                byte[] b = ret.toString().getBytes(Charset.forName("UTF-8"));
                he.sendResponseHeaders(200, b.length);
                he.getResponseBody().write(b);                
            } else {
                he.getResponseHeaders().add("Content-Type", "application/json");
                String json = new Gson().toJson(ret);
                byte[] b = json.getBytes(Charset.forName("UTF-8"));
                he.sendResponseHeaders(200, b.length);
                he.getResponseBody().write(b);                
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            Logger.getLogger(ScriptHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }  
    
    private Map<String, List<String>> splitQuery(URI uri) {
        if (uri.getQuery() == null || uri.getQuery().isEmpty()) {
            return Collections.emptyMap();
        }
        return Arrays.stream(uri.getQuery().split("&"))
                .map(this::splitQueryParameter)
                .collect(Collectors.groupingBy(SimpleImmutableEntry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue, toList())));
    }

    public SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new SimpleImmutableEntry<>(key, value);
    }    
}
