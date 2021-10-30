/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package countermanager.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import countermanager.prefs.Properties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptException;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcInvocationHandler;
import redstone.xmlrpc.XmlRpcServer;

/**
 *
 * @author chtheis
 */
public class XmlRpc implements HttpHandler {
    public XmlRpc() {

    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        if (he.getRequestMethod().equals("POST")) {
            InputStream is = he.getRequestBody();
            java.io.Writer writer = new java.io.CharArrayWriter(1024);
            
            xmlrpcServer.execute(is, writer);
            
            byte[] response = writer.toString().getBytes(HTTP.UTF8);
            
            he.getResponseHeaders().set("Content-Type", "text/xml");
            he.sendResponseHeaders(200, response.length);
            he.getResponseBody().write(response);
        } else {
            he.getResponseHeaders().add("Allow", "POST");
            he.sendResponseHeaders(405, 0);
        }

        he.getResponseBody().close();
    }

    private XmlRpcServer xmlrpcServer = new XmlRpcServer() {
        private javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
        private javax.script.ScriptEngine jsengine = manager.getEngineByName("js");

        private Map<String, Long> handlerSet = new java.util.HashMap<>();
        
        // Einen Handler fuer den Namensraum "name" suchen.
        // Das ist per default ein Script mit diesem Namen.
        @Override
        public XmlRpcInvocationHandler getInvocationHandler( String name )
        {
            XmlRpcInvocationHandler handler =  super.getInvocationHandler(name);
            
            String fileName = name.replaceAll("\\.", File.separator);
            
            File script = new File("../src/countermanager/http/scripts", fileName + ".js");
            if (!script.exists())
                script = new File(Properties.getIniFile().getParent() + File.separator + "http/scripts", fileName + ".js");
            if (!script.exists())
                return null;
            
            if (handler != null && handlerSet.keySet().contains(name) && handlerSet.get(name) == script.lastModified())
                return handler;
            
            try {                
                jsengine.eval(new FileReader(script));
                xmlrpcServer.addInvocationHandler( name, (handler = new JavaScriptXmlRpcInvocationHandler(jsengine.get(name))) );                
                handlerSet.put(name, script.lastModified());
                return super.getInvocationHandler(name);
            } catch (FileNotFoundException | ScriptException ex) {
                Logger.getLogger(XmlRpc.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            return null;
        }

        class JavaScriptXmlRpcInvocationHandler implements XmlRpcInvocationHandler {
            private Object thiz;

            public JavaScriptXmlRpcInvocationHandler(Object thiz) {
                this.thiz = thiz;
            }
            
            @Override
            public Object invoke(String method, List arguments) {
                Object ret;
                
                try {
                    if (jsengine == null)
                        ret = new XmlRpcException("No JS engine");
                    else {
                        ret = ((Invocable) jsengine).invokeMethod(thiz, method, arguments.toArray());
                    }
                } catch (ScriptException e) {
                    // Fehler im Script
                    System.err.println(e);
                    ret = new XmlRpcException(e.toString());
                } catch (NoSuchMethodException e) {
                    // Fehlende Funktion
                    System.err.println("Missing function " + method);
                    ret = new XmlRpcException(e.toString());
                } catch (Throwable e) {
                    // Sonstige (SQL) Fehler
                    System.err.println(e.toString());
                    ret = new XmlRpcException(e.toString());
                }
                                    
                return ret;
            }
        };
    };

    public void test() {
        try {
            // ((Invocable) jsengine).invoke("setResult", params.toArray());
            java.util.HashMap h = new java.util.HashMap();
            h.put("mtTimeStamp", "2010-03-12 00:00:00.000");
            java.util.ArrayList v = new java.util.ArrayList();
            v.add(h);

            XmlRpcClient xmlrpcClient = new XmlRpcClient("http://localhost/RPC2", false);
            Object ret = xmlrpcClient.invoke("ttm.listSchedules", v);
            System.out.println(ret);
        }
        catch (XmlRpcException ex) {
            ex.printStackTrace(System.err);
        } catch (XmlRpcFault ex) {
            ex.printStackTrace(System.err);
        } catch (MalformedURLException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
