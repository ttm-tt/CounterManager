/* Copyright (C) 2020 Christoph Theis */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package countermanager.driver.ttm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import countermanager.driver.CounterConfig;
import countermanager.driver.DigitsData;
import countermanager.driver.IGameData;
import countermanager.driver.ICounterCallback;
import countermanager.driver.ICounterProperties;
import countermanager.prefs.Preferences;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 *
 * @author chtheis
 */
public class CounterDriverTTM implements countermanager.driver.ICounterDriver {
    
    // Time between the updates
    private final static long UPDATE_TIME = 1000;
    
    // Minimum time to sleep between updates
    private final static long MINIMUM_DELAY = 500;
    
    private long aliveTimeout = 10;

    @Override
    public void lockScreen(int counter) {
        if (connections.get(counter) != null) {
            connections.get(counter).removeIf(msg -> msg.command == Command.UNLOCK_SCREEN.ordinal());
            connections.get(counter).add(new Message(counter + offsetTable, Command.LOCK_SCREEN, ""));
        }
    }

    @Override
    public void unlockScreen(int counter) {
        if (connections.get(counter) != null) {
            connections.get(counter).removeIf(msg -> msg.command == Command.LOCK_SCREEN.ordinal());
            connections.get(counter).add(new Message(counter + offsetTable, Command.UNLOCK_SCREEN, ""));
        }
    }
    
    public static class CounterPropertiesTTM implements ICounterProperties {
        private long aliveTimeout;

        /**
         * @return the aliveTimeout
         */
        public long getAliveTimeout() {
            return aliveTimeout;
        }

        /**
         * @param aliveTimeout the aliveTimeout to set
         */
        public void setAliveTimeout(long aliveTimeout) {
            this.aliveTimeout = aliveTimeout;
        }
    }
    
    static class Message {
        public int table;
        public int command;
        public String data;
        
        public Message(int table, Command command, String data) {
            this.table = table;
            this.command = command.ordinal();
            this.data = data;
        }
    }
    
    enum Command {
        NONE,                // 0
        GET_DATA,            // 1
        GET_DATA_BROADCAST,  // 2
        SET_DATA,            // 3
        SWAP_PLAYERS,        // 4
        RESET_ALERT,         // 5
        RESET,               // 6
        AUTO_TABLE,          // 7
        SET_RESULT,          // 8
        LOCK_SCREEN,         // 9
        UNLOCK_SCREEN        // 10
    }
    
    // final private static Gson json = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
    final private static Gson json = new GsonBuilder().create();
    
    private Map<Integer, List<Message>> connections = new java.util.HashMap<>();
    private ICounterCallback cbObject = null;
    private boolean debug = false;
    private int offsetTable = 0;
    private int lastTable = 24;    
    private Map<Integer, Long> aliveMap = new java.util.HashMap<>();
    private Map<Integer, String> addressMap = new java.util.HashMap<>();
    
    public CounterDriverTTM() {
        countermanager.http.HTTP.getDefaultInstance().addHandler("/counter/command", new HttpHandler() {

            @Override
            public void handle(HttpExchange he) throws IOException {
                if (!he.getRequestMethod().equals("POST")) {
                    he.getResponseHeaders().add("Allow", "POST");
                    he.sendResponseHeaders(405, 0);
                    he.close();
                    
                    return;
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte b[] = new byte[0x10000];
                int count;
                while ( (count = he.getRequestBody().read(b)) >= 0 )
                    bos.write(b, 0, count);
                
                Message msg = json.fromJson(bos.toString(), Message.class);
                
                byte[] resp = handleCommand(msg).getBytes();
                he.getResponseHeaders().set("Content-Type", "application/json");
                he.sendResponseHeaders(200, resp.length);
                he.getResponseBody().write(resp, 0, resp.length);
                he.getResponseBody().close();
                
                String address = null;
                // X-Forwarded-For may contain multiple addresses, take the first one
                if (he.getRequestHeaders().containsKey("X-Forwarded-For"))
                    address = he.getRequestHeaders().getFirst("X-Forwarded-For").split(",")[0].trim();
                else if (he.getRemoteAddress().getAddress() != null)
                     address = he.getRemoteAddress().getAddress().getHostAddress();

                if (address != null && addressMap.get(msg.table - offsetTable) == null)
                    Logger.getLogger(getClass().getName()).log(Level.INFO, "Connection for table " + msg.table + " from: " + address);

                if (address != null)
                    addressMap.put(msg.table - offsetTable, address);
            }
        });
    }
    
    String handleCommand(Message msg) {
        switch (Command.values()[msg.command]) {
            case AUTO_TABLE : {
                int c = 0;
                do {
                    if (aliveMap.get(c) == null)
                        break;

                    if (aliveMap.get(c) + aliveTimeout * 1000 < System.currentTimeMillis())
                        break;

                    ++c;
                } while (true);

                if (c <= lastTable)
                    aliveMap.put(c, System.currentTimeMillis());
                else
                    c = -offsetTable;

                // We modify the argument so the caller can access the table for logging
                msg.table = c + offsetTable;
                
                return json.toJson(c + offsetTable);
            }
            
            case GET_DATA :
            case GET_DATA_BROADCAST : {
                int counter = msg.table - offsetTable;

                aliveMap.put(counter, System.currentTimeMillis());

                cbObject.getCounterDataCallback(counter, json.fromJson(msg.data, CounterDataTTM.class));

                if (connections.get(counter) == null)
                    connections.put(counter, new java.util.ArrayList<>());

                String ret = json.toJson(connections.get(counter));

                connections.get(counter).clear();

                aliveMap.put(counter, System.currentTimeMillis());

                return ret;
            }
                
            default :
                return "";
        }
    }
    
    @Override
    public void activateTrace(int type) {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void closeConnection() {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCounterConfig(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getCounterData(int counter) throws IOException {
        if (aliveMap.get(counter) == null)
            return;
        
        if (aliveMap.get(counter) > 0 && (aliveMap.get(counter) + aliveTimeout * 1000) < System.currentTimeMillis()) {
            if (addressMap.get(counter) != null) {
                // addressMap is also a marker if the timeout has been handled
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Connection expired for table " + (counter + offsetTable) + " from: " + addressMap.get(counter));
                addressMap.remove(counter);
            }
            aliveMap.put(counter, System.currentTimeMillis());
            cbObject.onErrorCallback(counter, 1);
        }
    }

    @Override
    public void getCounterDataBroadcast(int last) throws IOException {
    }

    @Override
    public void getDateTime(int counter, String format) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDriverVersion() {
        return "0.1";
    }

    @Override
    public String getErrorMessage(int errCode) {
        return "<unknown> (" + errCode + ")";
    }

    @Override
    public void getGameNumber(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getPlayerNumbers(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getSerialNumber(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getSetResults(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getVersion(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int openConnection(countermanager.prefs.Properties prefs) throws IOException {
        CounterPropertiesTTM props = (CounterPropertiesTTM) getCounterProperties();
        
        offsetTable = prefs.getInt(countermanager.prefs.Prefs.OFFSET_TABLE_PREF, 1);
        lastTable = prefs.getInt(countermanager.prefs.Prefs.TO_TABLE_PREF, 24) - offsetTable;
        
        return 0;
    }

    @Override
    public void pushCounterButton(int counter, int button) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resetAlert(int counter) throws IOException {
        if (connections.get(counter) != null)
            connections.get(counter).add(new Message(counter + offsetTable, Command.RESET_ALERT, ""));
    }

    @Override
    public void resetCounter(int counter) throws IOException {
        if (connections.get(counter) != null)
            connections.get(counter).add(new Message(counter + offsetTable, Command.RESET, ""));
    }

    @Override
    public void setBaudrate(int baudrate) {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCallbackProcs(ICounterCallback cbObject) {
        this.cbObject = cbObject;
    }

    @Override
    public void setCodeNr(int codeNr) {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCounterConfig(int counter, CounterConfig counterConfig) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDateTime(int counter) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDigits(int counter, DigitsData data) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setGameData(int counter, IGameData gameData) throws IOException {
        if (connections.get(counter) != null) {
            if (connections.get(counter).size() > 0) {
                for (int i = connections.get(counter).size() - 1; i >= 0; i--) {
                    if (connections.get(counter).get(i).command == Command.SET_DATA.ordinal())
                        connections.get(counter).remove(i);
                }
            }
            
            connections.get(counter).add(new Message(counter + offsetTable, Command.SET_DATA, json.toJson(gameData)));
        }
    }

    @Override
    public void setGameNumber(int counter, int gameNumber) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPlayerNumbers(int counter, int playerLeft, int playerRight) throws IOException {
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void switchPlayerNumbers(int counter) throws IOException {        
        if (connections.get(counter) != null)
            connections.get(counter).add(new Message(counter + offsetTable, Command.SWAP_PLAYERS, ""));
    }
    
    @Override
    public void setResult(int counter, int[][] result) throws IOException {
        if (connections.get(counter) != null) {
            connections.get(counter).add(new Message(counter + offsetTable, Command.SET_RESULT, json.toJson(result)));
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public void setPassive(boolean aPassive) {
        
    }

        @Override
    public boolean isBroadcast() {
        return false;
    }
    
    @Override
    public void setBroadcast(boolean broadcast) {
        
    }
    
    @Override
    public IGameData createGameData() {
        return new IGameData() {};
    }
    
    @Override
    public long getUpdateTime() {
        return UPDATE_TIME;
    }
    
    @Override
    public long getMinimumDelay() {
        return MINIMUM_DELAY;
    }
    
    @Override
    public ICounterProperties getCounterProperties() {
        CounterPropertiesTTM props = new CounterPropertiesTTM();
        Preferences.loadProperties(props, this.getClass().getName(), false);
        
        return props;
    }
    
    @Override
    public void setCounterProperties(ICounterProperties props) {
        Preferences.saveProperties(props, this.getClass().getName(), false);
        
        aliveTimeout = ((CounterPropertiesTTM) props).getAliveTimeout();
    }
    
    @Override
    public String getAddress(int counter) {
        return addressMap.get(counter);
    }
}
