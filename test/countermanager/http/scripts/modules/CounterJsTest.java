/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts.modules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import countermanager.http.scripts.BaseJsTest;
import java.util.List;
import java.util.Map;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CounterJsTest extends BaseJsTest {
    
    public CounterJsTest() {
    }
    
    @Before
    @Override
    public void setUp() {
        super.setUp();

        // Load some piece of html with the script tag
        driver.get("http://localhost/scripts/modules/CounterJsTest.html");
    }
    
    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }
    

    @Test
    public void test_01_gameStarted() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testGameStarted(Testdata.data[0], 0);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameStarted(Object.assign({}, Testdata.data[0], Testdata.midFirstGame), 0);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertTrue(ret);                
        
        script = "return Testdata.testGameStarted(Object.assign({}, Testdata.data[0], Testdata.finishedFirstGame), 0);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertTrue(ret);                
    }

    @Test
    public void test_02_gameFinished() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testGameFinished(Testdata.data[0], 0);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished(Object.assign({}, Testdata.data[0], Testdata.midFirstGame), 0);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished(Object.assign({}, Testdata.data[0], Testdata.finishedFirstGame), 0);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertTrue(ret);    
    }
    
    
    @Test
    public void test_03_toggleService() {
        String script;
        Map ret;
        
        script = "return Testdata.testToggleServiceLeft(Testdata.data[0]);";
        ret = (Map) ((JavascriptExecutor) driver).executeScript(script);
        
        assertNotNull(ret.get("service"));
        assertEquals(-1, ((Number) ret.get("service")).intValue());
        assertNotNull(ret.get("firstService"));
        assertEquals(-1, ((Number) ret.get("firstService")).intValue());
    }
    
    
    @Test
    public void test_04_swapSides() {
        String script;
        Map ret;
        
        script = "return Testdata.testSwapSides(Testdata.data[0]);";
        ret = (Map) ((JavascriptExecutor) driver).executeScript(script);
        
        assertTrue((Boolean) ret.get("swapped"));
        assertTrue((Boolean) ret.get("swappedPlayers"));
        assertEquals(-2, ((Number) ret.get("playerNrLeft")).shortValue());
        assertEquals(-1, ((Number) ret.get("playerNrRight")).shortValue());
    }
    
    
    @Test
    public void test_05_addPointLeft() {
        String script;
        Object ret;
        
        script = "return Testdata.testAddPointLeft(Testdata.data[0]);";
        ret = executeScript(script);        
        assertNotNull(ret);
                
        assertEquals("RUNNING", ((Map) ret).get("gameMode"));
        assertEquals("MATCH", ((Map) ret).get("timeMode"));
        assertEquals(1L, ((Map<String, List<List>>) ret).get("setHistory").get(0).get(0));
    }
}