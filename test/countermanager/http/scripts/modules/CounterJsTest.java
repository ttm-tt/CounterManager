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
public class CounterJsTest {
    
    private WebDriver driver;    
    
    public CounterJsTest() {
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
        driver.get("http://localhost/scripts/modules/CounterJsTest.html");
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
 
    @Test
    public void test_01_gameStarted() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testGameStarted();";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameStarted(null);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameStarted([0,0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameStarted([1,0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertTrue(ret);                
    }

    @Test
    public void test_02_gameFinished() {
        String script;
        Boolean ret;
        
        script = "return Testdata.testGameFinished();";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished(null);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished([0,0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished([1,0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertFalse(ret);                
        
        script = "return Testdata.testGameFinished([11,0]);";
        ret = (Boolean) ((JavascriptExecutor) driver).executeScript(script);
        assertTrue(ret);                
    }
}