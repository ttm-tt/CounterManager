/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts;

import org.junit.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;

import countermanager.http.HTTP;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;


public class BaseJsTest {
    protected WebDriver driver;    
    
    public BaseJsTest() {
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
    
    
    protected Object executeScript(String script) {
        try {
            // Clear log
            ((JavascriptExecutor) driver).executeScript(
                    "document.getElementById('log').innerHTML = '';");
            
            // Embed the script in a try-catch clause and write the stack trace 
            // with <br> instead of \n to the log
            return ((JavascriptExecutor) driver).executeScript(
                "try { " + script + 
                "} catch (e) { " +
                "    console.log(e.message); " +
                "    console.log(e.stack.replace(/(?:\\r\\n|\\r|\\n)/g, '<br>')); " +
                "    throw e;" +
                "}"
            );  
        } catch (JavascriptException ex) {
            return null;
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            throw(ex);
        }
    }
}
