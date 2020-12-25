/* Copyright (C) 2020 Christoph Theis */
package countermanager.driver.ttm;

import com.google.gson.Gson;
import countermanager.driver.CounterConfig;
import countermanager.driver.CounterData;
import countermanager.driver.ICounterCallback;
import countermanager.http.HTTP;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;


public class CounterDriverTTMTest {
    protected static int HTTP_PORT = 8085;
    protected static CounterDriverTTM driver;
    
    private static Gson json = new Gson();

    public CounterDriverTTMTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws java.io.IOException {
        HTTP.getDefaultInstance().startHttpServer(HTTP_PORT);
        driver = new countermanager.driver.ttm.CounterDriverTTM();
        driver.setCallbackProcs(new TestCounterCallback());
        driver.openConnection(new countermanager.prefs.Properties());
    }
    
    @AfterClass
    public static void tearDownClass() {
        driver.closeConnection();
        HTTP.getDefaultInstance().stopHttpServer();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testAutoTable() {
        int counter = 1;
        String body = "";
        CounterDriverTTM.Message msg = new CounterDriverTTM.Message(counter, CounterDriverTTM.Command.AUTO_TABLE, body);
        
        // first free table should be 1, or course
        String ret = driver.handleCommand(msg);
        assertNotNull(ret);
        assertSame(1, json.fromJson(ret, Integer.TYPE));

        // And next should be 2
        ret = driver.handleCommand(msg);
        assertNotNull(ret);
        assertSame(2, json.fromJson(ret, Integer.TYPE));
    }
    
    //
    private static class TestCounterCallback implements ICounterCallback {

        @Override
        public void setBaudrateCallback(boolean ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getVersionCallback(int counter, String version) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getSerialNumberCallback(int counter, String serialNumber) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void pushCounterButtonCallback(int counter, boolean ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setDigitsCallback(int counter, boolean ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void switchPlayerNumbersCallback(int counter, boolean ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setCounterConfigCallback(int counter, boolean ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getCounterConfigCallback(int counter, CounterConfig counterConfig) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getCounterDataCallback(int counter, CounterData counterData) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setGameNumberCallback(int counter, boolean result) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getGameNumberCallback(int counter, int gameNr) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setPlayerNumbersCallback(int counter, boolean result) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getSetResultsCallback(int counter, String results) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setDateTimeCallback(int counter, boolean ret) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getDateTimeCallback(int counter, String dateTime) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onErrorCallback(int counter, int errCode) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void getPlayerNumbersCallback(int counter, int playerNrLeft, int playerNrRight) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void resetCallback(int counter, boolean result) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void resetAlertCallback(int counter, boolean result) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}
