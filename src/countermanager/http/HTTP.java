/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package countermanager.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import countermanager.driver.*;
import countermanager.model.*;
import countermanager.prefs.Properties;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ini4j.MultiMap;



/**
 *
 * @author chtheis
 */
public class HTTP {

    final private static Gson json = new GsonBuilder().create();
    
    private static HTTP http;

    public static void sendErrorResponse(HttpExchange he, int code) throws IOException {

        String message = "";
        switch (code) {
            case 404 :
                message = "Not found";
                break;
                
            case 405 :
                message = "Method not allowed";
                break;
                
            case 500 :
                message = "Internal Server Error";
                break;
        }
        
        he.sendResponseHeaders(code, message.getBytes().length);
        he.getResponseBody().write(message.getBytes());
        he.getResponseBody().close();        
    }
    
    private HTTP() throws IOException {
    }
    
    public static String getQuery(HttpExchange he, String key) {
        return getQuery(he, key, null);
    }
    
    public static String getQuery(HttpExchange he, String key, String def) {
        if (he.getRequestURI().getQuery() == null)
            return def;
        
        String queries[] = he.getRequestURI().getQuery().split("&");
        for (String query : queries) {
            if (query.startsWith(key + "="))
                return query.substring(key.length() + 1);
        }
        
        return def;
    }
    
    public static HTTP getDefaultInstance() {
        if (http == null) {
            try {
                http = new HTTP();
            } catch (IOException ex) {
                Logger.getLogger(HTTP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return http;
    }

    HttpServer httpServer = null;
    MultiMap<String, HttpHandler> handlerMap = new org.ini4j.BasicMultiMap<>();
    Set<String> aliasSet = new java.util.HashSet<>();
    Set<String> scriptSet = new java.util.HashSet<>();

    public boolean startHttpServer(int port) {
        if (httpServer != null)
            return false;
        
        try {
            httpServer = HttpServer.create();
            httpServer.bind(new InetSocketAddress(port), 5);
        } catch (IOException ex) {
            Logger.getLogger(HTTP.class.getName()).log(Level.SEVERE, null, ex);
            httpServer = null;
            
            return false;
        }
        
        httpServer.setExecutor(Executors.newCachedThreadPool());
        
        // Kein favicon.ico
        httpServer.createContext("/favicon.ico", new HttpHandler() {

            @Override
            public void handle(HttpExchange he) throws IOException {
                HTTP.sendErrorResponse(he, 404);
            }            
        });

        // Counter input: select table
        httpServer.createContext("/counter/list_matches", new HttpHandler() {
            @Override
            public void handle(HttpExchange he) throws IOException {
                if (!he.getRequestMethod().equals("POST")) {
                    he.getResponseHeaders().add("Allow", "POST");
                    he.sendResponseHeaders(405, 0);
                    he.close();
                    
                    return;
                }

                List<CounterModelMatch> list = new java.util.ArrayList<>();
                
                try {
                    CounterModel model = CounterModel.getDefaultInstance();
                    int fromTable = Integer.parseInt(getQuery(he, "fromTable", "" + model.getFromTable()));
                    int toTable = Integer.parseInt(getQuery(he, "toTable", "" + model.getToTable()));

                    for (int table = fromTable; table <= toTable; table++) {
                        if (!model.isCounterActive(table - model.getTableOffset()))
                            continue;
                        
                        CounterModelMatch match = model.getCounterMatch(table - model.getTableOffset());
                        if (match != null)
                            list.add(match);
                    }
                
                    byte[] response = json.toJson(list).getBytes();
                    he.getResponseHeaders().set("Content-Type", "application/json");
                    he.sendResponseHeaders(200, response.length);
                    he.getResponseBody().write(response,0, response.length);
                } catch (Exception e) {
                    HTTP.sendErrorResponse(he, 500);
                }
                finally {
                    he.getResponseBody().close();
                }
            }            
        });

        // Monitor
        httpServer.createContext("/counter/match", new HttpHandler() {

            @Override
            public void handle(HttpExchange he) throws IOException {
                try {
                    CounterModel model = CounterModel.getDefaultInstance();
                    int table = Integer.parseInt(getQuery(he, "table"));
                    CounterModelMatch match = model.getCounterMatch(table - model.getTableOffset());
                    if (match == null)
                        he.sendResponseHeaders(404, 0);
                    else {
                        byte[] response = json.toJson(match).getBytes();
                        he.getResponseHeaders().set("Content-Type", "application/json");
                        he.sendResponseHeaders(200, response.length);
                        he.getResponseBody().write(response,0, response.length);
                    }
                } catch (Exception e) {
                    he.sendResponseHeaders(500, 0);
                }
                finally {
                    he.getResponseBody().close();
                }
            }
        });
        
        httpServer.createContext("/counter/data", new HttpHandler() {

            @Override
            public void handle(HttpExchange he) throws IOException {
                try {
                    CounterModel model = CounterModel.getDefaultInstance();
                    int table = Integer.parseInt(getQuery(he, "table"));
                    CounterData data = model.getCounterData(table - model.getTableOffset());
                    if (data == null)
                        he.sendResponseHeaders(404, 0);
                    else {
                        byte[] response = json.toJson(data).getBytes();
                        he.getResponseHeaders().set("Content-Type", "application/json");
                        he.sendResponseHeaders(200, response.length);
                        he.getResponseBody().write(response, 0, response.length);
                    }
                } catch (Exception e) {
                    he.sendResponseHeaders(500, 0);
                } finally {
                    he.getResponseBody().close();
                }
            }
        });

        // XmlRpc
        httpServer.createContext("/RPC2", new XmlRpc());
        
        // Error logging
        httpServer.createContext("/logError", new HttpHandler() {
            @Override
            public void handle(HttpExchange he) throws IOException {
                try {
                    if (he.getRequestMethod().equals("POST")) {                    
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte b[] = new byte[0x10000];
                        int count;
                        while ( (count = he.getRequestBody().read(b)) >= 0 )
                            bos.write(b, 0, count);

                        Map<String, Object> err = json.fromJson(bos.toString(), Map.class);
                        String msg = he.getRemoteAddress().getHostString() + ": ";
                        if (!err.containsKey("message"))
                            msg += " " + bos.toString();
                        else {
                            msg += " " + err.get("message").toString();
                            if (err.containsKey("stack"))
                                msg += " stack: " + err.get("stack").toString();
                            if (err.containsKey("fileName"))
                                msg += " @ file: " + err.get("fileName").toString();
                            if (err.containsKey("lineNumber"))
                                msg += " line: " + err.get("lineNumber").toString();
                        }
                        Logger.getLogger(HTTP.class.getName()).log(Level.INFO, msg);
                        if (!err.containsKey("message") || !err.containsKey("fileName")) {                                 
                            System.err.println(bos.toString());
                        } else {
                            System.err.println(msg);                            
                        }
                        CounterModel.getDefaultInstance().sendSMS(msg);
                    }
                } catch (Exception e) {
                    Logger.getLogger(HTTP.class.getName()).log(Level.SEVERE, e.getMessage());
                } finally {
                    he.sendResponseHeaders(200, 0);
                    he.close();
                }
            }            
        });
        
        // Zum debuggen haben die Files aus dem src-Zweig Vorrang
        httpServer.createContext("/", new StaticFileHandler(new File[] {
            new File("../src/countermanager/http/"),
            new File(Properties.getIniFile().getParent() + File.separator + "http")
        }, null));

        for (Map.Entry<String, HttpHandler> set : handlerMap.entrySet()) {
            httpServer.createContext(set.getKey(), set.getValue());
        }
        
        httpServer.start();
        
        return true;
    }


    public void stopHttpServer() {
        if (httpServer != null)
            httpServer.stop(0);
        
        httpServer = null;
    }
    
    
    public boolean isHttpServerRunning() {
        return httpServer != null;
    }

    @SuppressWarnings("CallToThreadDumpStack")
    public void setScriptFiles(String scriptFiles) {

        Set<String> copyScriptSet = new java.util.HashSet<>(scriptSet);
        scriptSet.clear();
        
        for (String script : scriptFiles.split(File.pathSeparator)) {
            String[] tmp = script.split("=");
            
            if (tmp.length != 2)
                continue;
            
            String alias = tmp[0];
            String scriptFile = tmp[1];
            
            if (!alias.startsWith("/"))
                alias = "/" + alias;
            
            scriptSet.add(alias);
            copyScriptSet.remove(alias);
            
            addHandler(alias, new ScriptHandler(alias, scriptFile));
        }
        
        for (String alias : copyScriptSet) {
            removeHandler(alias);
        }
    }
    
    
    public void setAliases(String addDirs) {
        if (addDirs == null)
            return;
        
        Set<String> copyAliasSet = new java.util.HashSet<>(aliasSet);
        aliasSet.clear();
        
        for (String dir : addDirs.split(File.pathSeparator)) {
            if (dir.isEmpty())
                continue;

            String[] tmp = dir.split("=");
            if (tmp.length != 2)
                continue;
            
            String alias = tmp[0];
            String localPath = tmp[1];

            if (!alias.startsWith("/"))
                alias = "/" + alias;

            aliasSet.add(alias);
            copyAliasSet.remove(alias);

            addStaticFileHandler(alias, localPath);
        }  
        
        // Remove obsolete handlers
        for (String alias : copyAliasSet) {
            removeHandler(alias);
        }
    }
    
    
    public File resolveAlias(String alias) {
        HttpHandler handler = handlerMap.get(alias);
        if (handler != null && handler instanceof StaticFileHandler) {
            File[] pathes = ((StaticFileHandler) handler).getPathes();
            if (pathes.length == 1)
                return pathes[0];
        }
        
        return null;
    }        
    
    
    public void addStaticFileHandler(String path, String localPath) {
        addHandler(path, new StaticFileHandler(new File(localPath), path));
    }
    
    
    public void addHandler(String path, HttpHandler handler) {
        handlerMap.remove(path);
        
        handlerMap.put(path, handler);
        if (httpServer != null)
            httpServer.createContext(path, handler);
    }
    
    
    public void removeHandler(String path) {
        handlerMap.remove(path);
    }
}
