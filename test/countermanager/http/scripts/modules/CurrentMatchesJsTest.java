/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts.modules;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;

import countermanager.http.HTTP;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CurrentMatchesJsTest {
    
    private WebDriver driver;    
    
    public CurrentMatchesJsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        HTTP.getDefaultInstance().startHttpServer(80);
        WebDriverManager.chromedriver().setup();
    }
    
    @AfterClass
    public static void tearDownClass() {
        HTTP.getDefaultInstance().stopHttpServer();
    }
    
    @Before
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        // Tested in Google Chrome 59 on Linux. More info on:
        // https://developers.google.com/web/updates/2017/04/headless-chrome
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        
        // Enable loging in chrome including Level.INFO (but that doesn't work yet)
        options.addArguments("--enable-logging=stderr --v=1");  
        
        // Enable logging in webdriver
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, java.util.logging.Level.ALL);
        options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

        driver = new ChromeDriver(options);

        // Load some piece of html with the script tag
        driver.get("http://localhost/scripts/modules/CurrentMatchesJsTest.html");
    }
    
    @After
    public void tearDown() {
        if (driver != null) {
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            for (LogEntry log : logs) {
                Date date = new Date(log.getTimestamp());
                Logger.getLogger(getClass().getName()).log(log.getLevel(), "[" + date.toString() + "]: " + log.getMessage());
            }
            
            for (WebElement we : driver.findElements(By.cssSelector("#log span"))) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, we.getText());
            }
            
            driver.quit();
        }        
    }
    
    // Execute script and catch exception
    private Object executeScript(String script)
    {
        try {
            return ((JavascriptExecutor) driver).executeScript(script);            
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            throw(ex);
        }
    }
    
    /*
    * Test isFinished, ...
    */
    @Test
    public void test_1_IsFinished() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testIsFinished(Testdata.data[0][0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);        
        
        script = "return Testdata.testIsFinished(Testdata.data[0][1]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);        
        
        script = "return Testdata.testIsFinished(Testdata.data[0][2]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);        
        
        script = "return Testdata.testIsFinished(Testdata.data[1][0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);        
        
        script = "return Testdata.testIsFinished(Testdata.data[2][0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertTrue(ret);        
    }

    /*
    * Test initial preconditions
    */
    @Test
    public void test_2_EmptyInitialize() {
        String script = "return Testdata.testEmptyInitialize();";
        List<List> ret = (List) ((JavascriptExecutor) driver).executeScript(script);
        assertEquals(0, ret.size());
    }
    
    /*
    * Test the basics steps when rebuilding the match list
    */
    @Test
    public void test_3_RebuildSteps() {
        String script;
        
        {
            List<Map> ret;

            script = "return Testdata.testInitialize();";
            ret = (List) executeScript(script);
            assertEquals(0, ret.size());
        }

        {
            List<Map> ret;

            script = "return Testdata.testRemoveFinished(Testdata.data[0], Testdata.testct);";
            ret = (List) executeScript(script);
            assertEquals(0, ret.size());
        }

        {
            List<Map> ret;

            script = "return Testdata.testUpdateUnfinished(Testdata.data[0]);";
            ret = (List) executeScript(script);
            // ret[0], i.e. table 0, is always null
            assertEquals(2, ret.size());
            assertNull(ret.get(0));
            assertNotNull(ret.get(1));
            assertEquals(1L, ret.get(1).get("mtNr"));
        }

        {
            List<List<Map>> ret;
            
            script = "return Testdata.testFinalize(Testdata.data[0], Testdata.testct);";
            ret = (List) executeScript(script);
            assertEquals(2, ret.size());
            assertNull(ret.get(0));
            assertNotNull(ret.get(1));
            assertEquals(2, ret.get(1).size());
            assertEquals(1L, ret.get(1).get(0).get("mtNr"));
            assertEquals(2L, ret.get(1).get(1).get("mtNr"));
            
            // Disable debug
            executeScript("Matches.setDebug(false);");
        }
    }
    
    
    /*
    * Test the complete method to feed initial data
    * The outcome is the same as in testRebuildSteps
    */
    @Test
    public void test_4_States() {
        String script;
        List<List<Map>> ret;
        
        // Setup with new data
        script = "return Testdata.testRebuild(Testdata.data[0], Testdata.testct);";
        ret = (List) executeScript(script);

        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(1L, ret.get(1).get(0).get("mtNr"));
        assertEquals(2L, ret.get(1).get(1).get("mtNr"));

        // Update the current match
        script = "return Testdata.testUpdate([Testdata.data[1][0]], Testdata.testct + 10 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(1L, ret.get(1).get(0).get("mtNr"));
        assertEquals(2L, ret.get(1).get(1).get("mtNr"));
        
        // And a rebuild with updated data
        script = "return Testdata.testRebuild(Testdata.data[1], Testdata.testct + 20 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(1L, ret.get(1).get(0).get("mtNr"));
        assertEquals(2L, ret.get(1).get(1).get("mtNr"));
        
        // Update with a finished match
        script = "return Testdata.testUpdate([Testdata.data[2][0]], Testdata.testct + 30 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(1L, ret.get(1).get(0).get("mtNr"));
        assertEquals(2L, ret.get(1).get(1).get("mtNr"));
        
        // Rebuild with a finished match, it should still be shown
        script = "return Testdata.testRebuild(Testdata.data[2], Testdata.testct + 40 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(1L, ret.get(1).get(0).get("mtNr"));
        assertEquals(2L, ret.get(1).get(1).get("mtNr"));
        
        // Update past mintime with a finished match, it should still be shown
        script = "return Testdata.testUpdate([Testdata.data[2][0]], Testdata.testct + 80 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(1L, ret.get(1).get(0).get("mtNr"));
        assertEquals(2L, ret.get(1).get(1).get("mtNr"));
        
        // Rebuild past mintime with a finished match, it should not be shown
        script = "return Testdata.testRebuild(Testdata.data[2], Testdata.testct + 90 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(1, ret.get(1).size());
        assertEquals(2L, ret.get(1).get(0).get("mtNr"));
        
        // Update past 3rd match but before prestart with a finished match
        script = "return Testdata.testUpdate([Testdata.data[2][0]], Testdata.testct + 30 * 60 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(1, ret.get(1).size());
        assertEquals(2L, ret.get(1).get(0).get("mtNr"));
        
        // Rebuild past 3rd match but before prestart with a finished match, it should not be shown
        script = "return Testdata.testRebuild(Testdata.data[2], Testdata.testct + 30 * 60 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(2L, ret.get(1).get(0).get("mtNr"));
        assertEquals(3L, ret.get(1).get(1).get("mtNr"));        
        
        // Update past prestart with a finished match
        script = "return Testdata.testUpdate([Testdata.data[2][0]], Testdata.testct + 90 * 60 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(2, ret.get(1).size());
        assertEquals(2L, ret.get(1).get(0).get("mtNr"));
        assertEquals(3L, ret.get(1).get(1).get("mtNr"));
        
        // Rebuild past restart with a finished match, it should not be shown
        script = "return Testdata.testRebuild(Testdata.data[2], Testdata.testct + 90 * 60 * 1000);";
        ret = (List) executeScript(script);
        
        assertEquals(2, ret.size());
        assertNull(ret.get(0));
        assertNotNull(ret.get(1));
        assertEquals(3, ret.get(1).size());
        assertEquals(2L, ret.get(1).get(0).get("mtNr"));
        assertEquals(3L, ret.get(1).get(1).get("mtNr"));        
        assertEquals(4L, ret.get(1).get(2).get("mtNr"));        
    }
}
