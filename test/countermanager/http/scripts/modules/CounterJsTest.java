/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts.modules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import countermanager.http.BaseJsTest;
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
        loadHtml("/scripts/modules/CounterJsTest.html");
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
        assertTrue((Boolean) ret.get("serviceLeft"));
        assertFalse((Boolean) ret.get("serviceRight"));
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
        
        assertFalse((Boolean) ret.get("serviceLeft"));
        assertFalse((Boolean) ret.get("serviceRight"));
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

        // Set service left with swapped sides
        script = "return Testdata.testToggleServiceLeft(Object.assign(" +
                    "{}, Testdata.data[1]" +
                 "));";
        ret = (Map) executeScript(script);

        // X (now on the right side) has service
        assertTrue((Boolean) ret.get("serviceLeft"));
        assertEquals(+1L, ret.get("service"));
        assertEquals(+1L, ret.get("firstService"));
        assertEquals(-1L, ret.get("firstServiceDouble"));
        
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
        
        // Change sides before the match starts
        script = "return Testdata.testSwapSides(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        
        assertTrue((Boolean) ret.get("swapped"));
        
        // It is easier to get these numbers as short than to cast around
        assertEquals(-2, ((Number) ret.get("playerNrLeft")).shortValue());
        assertEquals(-1, ((Number) ret.get("playerNrRight")).shortValue());
        
        // Change sides at 5 in the last game
        script = "return Testdata.testSwapSides(Object.assign({}, Testdata.data[0], Testdata.midLastGameBefore));";
        ret = (Map) executeScript(script);
        
        // It is easier to get these numbers as short than to cast around
        assertEquals(-2, ((Number) ret.get("playerNrLeft")).shortValue());
        assertEquals(-1, ((Number) ret.get("playerNrRight")).shortValue());
        assertEquals(-1L, ret.get("sideChange"));
        
        // Change sides back at 5 in the last game
        script = "return Testdata.testSwapSides(Object.assign({}, Testdata.data[0], Testdata.midLastGameAfter));";
        ret = (Map) executeScript(script);
        
        // It is easier to get these numbers as short than to cast around
        assertEquals(-2, ((Number) ret.get("playerNrLeft")).shortValue());
        assertEquals(-1, ((Number) ret.get("playerNrRight")).shortValue());
        assertEquals(+1L, ret.get("sideChange"));
        
        // Swap sides after one game has finished
        // With firstService B -> X current service would be X - A
        script = "return Testdata.testSwapSides(Object.assign(" +
                        "{}, Testdata.data[0], Testdata.finishedFirstGame, " +
                        "Testdata.firstServiceLeft, Testdata.serviceRightYB" +
                "));";
        ret = (Map) executeScript(script);
        // Service on the left side
        assertTrue((Boolean) ret.get("serviceLeft"));
        assertFalse((Boolean) ret.get("serviceRight"));
        // X-side has service
        assertEquals(1, ((Number) ret.get("service")).intValue());
        // Per default it will be X->B
        assertEquals(-1, ((Number) ret.get("serviceDouble")).intValue());
        // First service stay the same until a new double service is choosen
        assertEquals(-1, ((Number) ret.get("firstService")).intValue());
        assertEquals(+1, ((Number) ret.get("firstServiceDouble")).intValue());
        
        // Change after 1st game and back again
        script = "return Testdata.testSwapSides(Testdata.testSwapSides(Object.assign(" +
                        "{}, Testdata.data[0], Testdata.finishedFirstGame, " +
                        "Testdata.firstServiceLeft, Testdata.serviceRightYB" +
                ")));";
        ret = (Map) executeScript(script);
        // Service on the left side
        assertFalse((Boolean) ret.get("serviceLeft"));
        assertTrue((Boolean) ret.get("serviceRight"));
        // X-side has service
        assertEquals(1, ((Number) ret.get("service")).intValue());
        // Per default it will be X->B
        assertEquals(4, ((Number) ret.get("serviceDouble")).intValue());
        // First service stay the same until a new double service is choosen
        assertEquals(-1, ((Number) ret.get("firstService")).intValue());
        assertEquals(+1, ((Number) ret.get("firstServiceDouble")).intValue());
    }
    
    
    @Test
    public void test_08_addPoint() {
        String script;
        Map ret;

        // Start 1st game
        script = "return Testdata.testAddPointLeft(Testdata.data[0]);";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
                
        assertEquals("RUNNING", ret.get("gameMode"));
        assertEquals("MATCH", ret.get("timeMode"));
        assertEquals(1L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        
        // End 1st game with service
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.endFirstGame, " +
                    "Testdata.firstServiceLeft, Testdata.serviceLeftAY" +
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        // SideChange.BEFORE
        assertEquals(1L, ret.get("sideChange"));
        // Service should be Y->B
        assertTrue((Boolean) ret.get("serviceRight"));
        assertEquals(1L, ret.get("service"));
        assertEquals(4L, ret.get("serviceDouble"));
        
        // After end 1st game
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.finishedFirstGame " + 
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));

        // Mid last game
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.midLastGame, " + 
                    "Testdata.firstServiceLeft, Testdata.serviceRightYB" +
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(5L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        assertTrue((Boolean) ret.get("serviceLeft"));
        assertEquals(-1L, ret.get("service"));
        assertEquals(+1L, ret.get("serviceDouble"));
        // SideChange.BEFORE
        assertEquals(1L, ((Map) ret).get("sideChange"));

        // Mid last game before side change
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.midLastGameBefore " + 
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(5L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        // SideChange.BEFORE
        assertEquals(1L, ((Map) ret).get("sideChange"));

        // Mid last game after side change
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.midLastGameAfter, " + 
                    "Testdata.firstServiceLeft, Testdata.serviceLeftBY " +
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(6L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        assertTrue((Boolean) ret.get("serviceLeft"));
        assertEquals(-1L, ret.get("service"));
        assertEquals(-4L, ret.get("serviceDouble"));
        // SideChange.NONE
        assertEquals(0L, ((Map) ret).get("sideChange"));
        
        // End last game
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.endLastGame " + 
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        
        // After end last game
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.finishedLastGame " + 
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
    }
    
    
    @Test
    public void test_09_subPoint() {
        String script;
        Map ret;
        
        // No sub at start of game
        script = "return Testdata.testSubPointLeft(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals(0L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        
        // No sub at 0
        // Add 1 point right then sub one left
        script = "return Testdata.testSubPointLeft(Testdata.testAddPointRight(Testdata.data[0]));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals(0L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        
        // After end 1st game
        script = "return Testdata.testSubPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.finishedFirstGame, " + 
                    "Testdata.firstServiceLeft, Testdata.serviceRightYB" +
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(10L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
        assertTrue((Boolean) ret.get("serviceLeft"));
        assertEquals(-1L, ret.get("service"));
        assertEquals(3L, ret.get("serviceDouble"));
        assertEquals(0L, ret.get("sideChange"));

        // Mid last game before side change
        script = "return Testdata.testSubPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.midLastGameBefore, " + 
                    "Testdata.firstServiceLeft, Testdata.serviceLeftBX " +
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(4L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
        assertTrue((Boolean) ret.get("serviceRight"));
        assertEquals(+1L, ret.get("service"));
        assertEquals(4L, ret.get("serviceDouble"));
        assertEquals(0L, ret.get("sideChange"));

        // Mid last game after side change
        script = "return Testdata.testSubPointLeft(Object.assign(" +
                    "{}, Testdata.data[0], Testdata.midLastGameAfter" + 
                 "));";
        ret = (Map) executeScript(script);        
        assertNotNull(ret);
        assertEquals(5L, ((Map<String, List<List>>) ret).get("setHistory").get(4).get(0));
    }
    
    
    @Test
    public void test_10_toggleTimeout() {
        String script;
        Map ret;
        
        // Set timeout left (not really legal before tha match has started)
        script = "return Testdata.testToggleTimeoutLeft(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertTrue((Boolean) ret.get("timeoutLeft"));
        assertTrue((Boolean) ret.get("timeoutLeftRunning"));
        
        // Toggle timeout left from true to false
        script = "return Testdata.testToggleTimeoutLeft(Object.assign({}, Testdata.data[0], Testdata.timeoutLeftRunning));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertFalse((Boolean) ret.get("timeoutLeft"));
        assertFalse((Boolean) ret.get("timeoutLeftRunning"));        
    }
    
    
    @Test
    public void test_11_toggleCard() {
        String script;
        Map ret;
        
        // Set Yellow from None
        script = "return Testdata.testToggleYLeft(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals("YELLOW", ret.get("cardLeft"));
        
        // Reset Yellow from Yellos
        script = "return Testdata.testToggleYLeft(Object.assign({}, Testdata.data[0], Testdata.yellowCardLeft));";
        ret = (Map) executeScript(script);
        assertEquals("NONE", ret.get("cardLeft"));
        
        // Set YR1P from None
        script = "return Testdata.testToggleYR1PLeft(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        assertEquals("YR1P", ret.get("cardLeft"));
        
        // Reset YR1P from YR2P
        script = "return Testdata.testToggleYR1PLeft(Object.assign({}, Testdata.data[0], Testdata.yr2pCardLeft));";
        ret = (Map) executeScript(script);
        assertEquals("YELLOW", ret.get("cardLeft"));                
    }
    
    
    @Test
    public void test_12_toggleStartGame() {
        String script;
        Map ret;
        
        // Verify initial state
        script = "return Testdata.data[0];";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals("RESET", ret.get("gameMode"));
        assertEquals("NONE", ret.get("timeMode"));
        
        // Toggle startGame to true
        script = "return Testdata.testToggleStartGame(Testdata.data[0]);";
        ret = (Map) executeScript(script);
        assertEquals("RUNNING", ret.get("gameMode"));
        assertEquals("MATCH", ret.get("timeMode"));
        
        // Toggle startGame from warmup
        script = "return Testdata.testToggleStartGame(Object.assign(" +
                 "{}, Testdata.data[0], Testdata.warmupPrepare" +
                 "));";
        ret = (Map) executeScript(script);
        assertEquals("RUNNING", ret.get("gameMode"));
        assertEquals("MATCH", ret.get("timeMode"));
        
        // Toggle startGame from match
        script = "return Testdata.testToggleStartGame(Object.assign(" +
                 "{}, Testdata.data[0], Testdata.runningMatch" +
                 "));";
        ret = (Map) executeScript(script);
        assertEquals("RUNNING", ret.get("gameMode"));
        assertEquals("NONE", ret.get("timeMode"));
        
        // Stop running timeout
        script = "return Testdata.testToggleStartGame(Object.assign(" +
                "{}, Testdata.data[0], Testdata.timeoutLeftRunning" +
                "));";
        ret = (Map) executeScript(script);
        assertEquals("RUNNING", ret.get("gameMode"));
        assertEquals("MATCH", ret.get("timeMode"));
        assertTrue((Boolean) ret.get("timeoutLeft"));
        assertFalse((Boolean) ret.get("timeoutLeftRunning"));
    }
    
    
    @Test
    public void test_13_endMatch() {
        String script;
        Map ret;
        
        // Invalid: Set before start of match
        script = "return Testdata.testEndMatch(Object.assign({}, Testdata.data[0]));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals("RESET", ret.get("gameMode"));
        
        // Valid: Set at end of match
        script = "return Testdata.testEndMatch(Object.assign(" + 
                "{}, Testdata.data[0], Testdata.finishedLastGame, Testdata.runningMatch" +
                "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals("END", ret.get("gameMode"));
        assertEquals(3L, ret.get("setsLeft"));
    }
    
    
    @Test 
    public void test_14_toggleExpedite() {
        String script;
        Map ret;
        
        // Switch on expedite
        script = "return Testdata.testToggleExpedite(Object.assign(" +
                "{}, Testdata.data[0], Testdata.serviceLeft, Testdata.midLastGameAfter" +
                "));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertTrue((Boolean) ret.get("expedite"));
        assertTrue((Boolean) ret.get("serviceLeft"));
        
        // Switch off expedite
        script = "return Testdata.testToggleExpedite(Object.assign(" +
                "{}, Testdata.data[0], Testdata.serviceLeft, Testdata.expedite, Testdata.midLastGameAfter" +
                "));";
        ret = (Map) executeScript(script);
        assertFalse((Boolean) ret.get("expedite"));
        
        // Add one point
        script = "return Testdata.testAddPointLeft(Object.assign(" +
                "{}, Testdata.data[0], Testdata.expedite, " +
                "Testdata.firstServiceLeft, Testdata.serviceLeftBY, " +
                "Testdata.midLastGameAfter" +
                "));";
        ret = (Map) executeScript(script);
        assertTrue((Boolean) ret.get("expedite"));
        assertTrue((Boolean) ret.get("serviceRight"));
    }
    
    
    @Test
    public void test_15_walkover() {
        String script;
        Map ret;
        
        // W/O left before start of match
        script = "return Testdata.testWOLeft(Object.assign({}, Testdata.data[0]));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals("END", ret.get("gameMode"));
        assertEquals(3L, ret.get("setsRight"));
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(2).get(1));
        
        // W/O right before start of match
        script = "return Testdata.testWORight(Object.assign({}, Testdata.data[0]));";
        ret = (Map) executeScript(script);
        assertNotNull(ret);
        assertEquals("END", ret.get("gameMode"));
        assertEquals(3L, ret.get("setsLeft"));
        assertEquals(11L, ((Map<String, List<List>>) ret).get("setHistory").get(2).get(0));
   }
}