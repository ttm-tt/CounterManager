/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts.modules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import countermanager.http.scripts.BaseJsTest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CounterJsTest extends BaseJsTest {
    
    public CounterJsTest() {
    }
    
    @Before
    @Override
    public void setUp() {
        super.setUp();

        // Load some piece of html with the script tag
        // We can't use localhost because then chrome will bypass the proxy
        // (despite the docs for proxy-bypass-list=<-loopback>)
        // So in order for confusion we use the hostname instead
        String hn = "localhost";
        try {
           hn = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(CounterJsTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        driver.get("http://" + hn + "/scripts/modules/CounterJsTest.html");
    }
    
    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }
    
    
    @Test
    public void test_01_matchStarted() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testMatchStarted(Testdata.data[0]);";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testMatchStarted(Object.assign({}, Testdata.data[0], {gameMode: 'WARMUP'}));";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);                
        
        script = "return Testdata.testMatchStarted(Object.assign({}, Testdata.data[0], Testdata.midFirstGame));";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);                
        
        script = "return Testdata.testMatchStarted(Object.assign({}, Testdata.data[0], Testdata.finishedLastGame));";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);                
    }    
    
    
    @Test
    public void test_02_matchFinished() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testMatchFinished(Testdata.data[0]);";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testMatchFinished(Object.assign({}, Testdata.data[0], {gameMode: 'WARMUP'}));";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testMatchFinished(Object.assign({}, Testdata.data[0], Testdata.finishedFirstGame));";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testMatchFinished(Object.assign({}, Testdata.data[0], Testdata.finishedLastGame));";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);                
    }    


    @Test
    public void test_03_gameStarted() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testGameStarted(Testdata.data[0], 0);";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameStarted(Object.assign({}, Testdata.data[0], Testdata.midFirstGame), 0);";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);                
        
        script = "return Testdata.testGameStarted(Object.assign({}, Testdata.data[0], Testdata.finishedFirstGame), 0);";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);                
    }
    

    @Test
    public void test_04_gameFinished() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testGameFinished(Testdata.data[0], 0);";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished(Object.assign({}, Testdata.data[0], Testdata.midFirstGame), 0);";
        ret = (Boolean) executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished(Object.assign({}, Testdata.data[0], Testdata.finishedFirstGame), 0);";
        ret = (Boolean) executeScript(script);
        assertTrue(ret);    
    }
    
    
    @Test
    public void test_05_toggleService() {
        String script;
        Map ret;
        
        // Set service left
        script = "return Testdata.testToggleServiceLeft(Object.assign(" +
                    "{}, Testdata.data[0]" +
                 "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        
        assertNotNull(ret.get("service"));
        assertEquals(-1L, ret.get("service"));
        assertNotNull(ret.get("firstService"));
        assertEquals(-1L, ret.get("firstService"));
        assertNotNull(ret.get("firstServiceDouble"));
        assertEquals(+1L, ret.get("firstServiceDouble"));
        
        // Clear service left
        script = "return Testdata.testToggleServiceLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.serviceLeft, Testdata.firstServiceLeft" +
                 "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        
        assertEquals(0L, ret.get("service"));
        assertEquals(0L, ret.get("firstService"));
        assertEquals(0L, ret.get("firstServiceDouble"));
        
        // Toggle service left
        script = "return Testdata.testToggleServiceLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.serviceRight, Testdata.firstServiceRight" +
                 "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        
        assertEquals(-1L, ret.get("service"));
        assertEquals(-1L, ret.get("firstService"));
        assertEquals(+1L, ret.get("firstServiceDouble"));

        // Set service left at the end of the first game (10:3)
        script = "return Testdata.testToggleServiceLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.endFirstGame" +
                 "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        
        assertEquals(-1L, ret.get("service"));
        assertEquals(-1L, ret.get("firstService"));
        assertEquals(+3L, ret.get("firstServiceDouble"));

        // Set service left at the end of the last game
        script = "return Testdata.testToggleServiceLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.endLastGame" +
                 "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        
        assertEquals(-1L, ret.get("service"));
        assertEquals(+1L, ret.get("firstService"));
        
        // TODO: Double
    }
    
    
    @Test
    public void test_06_toggleServiceDouble() {
        String script;
        Map    ret;
        
        // Initial state, no service selected
        script = "return Testdata.testToggleServiceDoubleLeft(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        
        assertEquals(0L, ret.get("service"));
        assertEquals(0L, ret.get("serviceDouble"));
        assertEquals(0L, ret.get("firstService"));
        assertEquals(0L, ret.get("firstServiceDouble"));
    }
    
    
    @Test
    public void test_07_swapSides() {
        String script;
        Map ret;
        
        script = "return Testdata.testSwapSides(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        
        assertTrue((Boolean) ret.get("swapped"));
        assertTrue((Boolean) ret.get("swappedPlayers"));
        
        // It is easier to get these numbers as short than to cast around
        assertEquals(-2, ((Number) ret.get("playerNrLeft")).shortValue());
        assertEquals(-1, ((Number) ret.get("playerNrRight")).shortValue());
    }
    
    
    @Test
    public void test_08_addPointLeft() {
        String script;
        Object ret;

        // Start 1st game
        script = "return Testdata.testAddPointLeft(Testdata.data[0]);";
        ret = executeScript(script);        
        assertNotNull(ret);
                
        assertEquals("RUNNING", ((Map) ret).get("gameMode"));
        assertEquals("MATCH", ((Map) ret).get("timeMode"));
        assertEquals(1L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        
        // End 1st game
        script = "return Testdata.testAddPointLeft(" +
                    "Object.assign({}, Testdata.data[0], Testdata.endFirstGame)" + 
                 ");";
        ret = executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        // SideChange.BEFORE
        assertEquals(1L, ((Map) ret).get("sideChange"));
        
        // After end 1st game
        script = "return Testdata.testAddPointLeft(" +
                    "Object.assign({}, Testdata.data[0], Testdata.finishedFirstGame)" + 
                 ");";
        ret = executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));

        // Mid last game
        script = "return Testdata.testAddPointLeft(" +
                    "Object.assign({}, Testdata.data[0], Testdata.midLastGame)" + 
                 ");";
        ret = executeScript(script);        
        assertNotNull(ret);
        assertEquals(5L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        // SideChange.BEFORE
        assertEquals(1L, ((Map) ret).get("sideChange"));
        
        // End last game
        script = "return Testdata.testAddPointLeft(" +
                    "Object.assign({}, Testdata.data[0], Testdata.endLastGame)" + 
                 ");";
        ret = executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        
        // After end last game
        script = "return Testdata.testAddPointLeft(" +
                    "Object.assign({}, Testdata.data[0], Testdata.finishedLastGame)" + 
                 ");";
        ret = executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
    }
}