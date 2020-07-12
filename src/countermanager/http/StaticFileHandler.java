/* Copyright (C) 2020 Christoph Theis */

package countermanager.http;

import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

class StaticFileHandler implements HttpHandler {
    
    private File[] pathes;
    private String alias;
    
    public StaticFileHandler(File path, String alias) {
        this(new File[] {path}, alias);
    }
    
    public StaticFileHandler(File[] pathes, String alias) {
        this.pathes = pathes;
        this.alias = alias;
    }
    
    public File[] getPathes() {
        return pathes;
    }
    
    public String getAlias() {
        return alias;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        if (!he.getRequestMethod().equals("GET")) {
            he.getResponseHeaders().add("Allow", "GET");
            HTTP.sendErrorResponse(he, 405);

            return;
        }
        
        String uri = he.getRequestURI().getPath();
        
        if (uri.contains("/../")) {
            HTTP.sendErrorResponse(he, 404);  // Eigentlich etwas illegales
        }
        
        if (alias != null && !alias.isEmpty()) {
            if (!uri.startsWith(alias)) {
                HTTP.sendErrorResponse(he, 404);
                
                return;
            }
            
            uri = uri.substring(alias.length());
        }
        
        File file = null;
        
        if (pathes == null) {
            file = new File(uri);
        } else {
            for (File path : pathes) {
                file = new File(path, uri);
                if (file.exists())
                    break;
            }
        }
        
        if (file == null || !file.exists()) {
            HTTP.sendErrorResponse(he, 404);
            
            return;
        } else if (file.isDirectory()) {
            File index = new File(file, "index.html");
            if (!index.exists()) {
                handleDirectory(he, file);
                
                return;
            }
            
            file = index;
        }
        
        String mime = null;
        
        // Sometimes probeContentType will return text/plain for html and js files
        if (file.getName().endsWith(".html"))
            mime = "text/html";
        else if (file.getName().endsWith(".js"))
            mime = "text/javascript";
        else
            mime = Files.probeContentType(file.toPath());
        
        he.getResponseHeaders().add("Content-Type", mime == null ? "application/octet-stream" : mime);
        
        String etag = Integer.toHexString( (file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode() );
        if (he.getRequestHeaders().containsKey("ETag")) {
            if (etag.equals(he.getRequestHeaders().get("ETag").toString())) {
                he.sendResponseHeaders(304, 0);
                he.getResponseBody().close();
            }
        }
        
        he.getResponseHeaders().add("ETag", etag);
        
        he.sendResponseHeaders(200, file.length());
        byte b[] = new byte[0x10000];
        int  count;
        try (FileInputStream fis = new FileInputStream(file)) {
            while ( (count = fis.read(b)) >= 0 )
                he.getResponseBody().write(b, 0, count);
        }
        he.getResponseBody().close();
    }
    
    
    private void handleDirectory(HttpExchange he, File file) throws IOException {
        List<String> array = new java.util.ArrayList<>();
        String pattern = HTTP.getQuery(he, "pattern");
        String ext = HTTP.getQuery(he, "ext");
        
        if (!pattern.isEmpty())
            pattern = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".?");
        
        File[] files = file.listFiles();
        for (File f : files) {
            if (!f.isFile())
                continue;
            
            if (!ext.isEmpty() && !f.getName().endsWith("." + ext))
                continue;
            
            if (!pattern.isEmpty() && !f.getName().matches(pattern))
                continue;
            
            array.add(f.getName());
        }
        
        String resp = new GsonBuilder().create().toJson(array);
        
        he.getResponseHeaders().add("Content-Type", "application/json");
        he.sendResponseHeaders(200, resp.getBytes().length);
        he.getResponseBody().write(resp.getBytes());
        he.getResponseBody().close();
    }
    
}
