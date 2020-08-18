/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;

import countermanager.http.HTTP;
import java.io.IOException;
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
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.Proxy;

import jscover.Main;
import org.openqa.selenium.support.ui.ExpectedCondition;


public class BaseJsTest {
    protected static jscover.Main main = new Main();
    protected WebDriver driver;   
    
    public BaseJsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                main.runMain(new String[] {
                    "-ws",
                    "--port=3129",
                    "--proxy",
                    "--local-storage",
                    "--no-instrument-reg=.*Test\\.js",
                    "--report-dir=../coverage/",
                    "--log=INFO"
                }); }
        }).start();
        
        HTTP.getDefaultInstance().startHttpServer(80);
        WebDriverManager.chromedriver().setup();
    }
    
    @AfterClass
    public static void tearDownClass() {
        HTTP.getDefaultInstance().stopHttpServer();
        main.stop();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jscover.report.Main.main(new String[] {
                        "--format=COBERTURAXML",
                        "../coverage/",
                        "../coverage/original-src"
                    });
                } catch (IOException ex) {
                    Logger.getLogger(BaseJsTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }
    
    @Before
    public void setUp() {
        Proxy proxy = new Proxy().setHttpProxy("localhost:3129");
        
        ChromeOptions options = new ChromeOptions();
        // Tested in Google Chrome 59 on Linux. More info on:
        // https://developers.google.com/web/updates/2017/04/headless-chrome
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");

        // Enable loging in chrome including Level.INFO (but that doesn't work yet)
        options.addArguments("--enable-logging=stderr --v=1");  
        
        // options.addArguments("--proxy-server=\"http://localhost:3129\"");
        // options.addArguments("--proxy-bypass-list=\"<-loopback>\"");
        
        // Enable logging in webdriver
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, java.util.logging.Level.ALL);
        options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

        // JScover proxy
        options.setProxy(proxy);
        
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
            
            executeScript("window.jscoverFinished = false;");
            executeScript("jscoverage_report('', function(){window.jscoverFinished=true;});");
            (new WebDriverWait(driver, 10000))
                .until((ExpectedCondition<Boolean>) new ExpectedCondition<Boolean>() {
                    @Override
                    public Boolean apply(WebDriver d) {
                        return (Boolean)((JavascriptExecutor) driver).executeScript("return window.jscoverFinished;");
                    }
            });
            
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
