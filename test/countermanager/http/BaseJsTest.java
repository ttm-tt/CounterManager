/* Copyright (C) 2020 Christoph Theis */
package countermanager.http;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.FileOutputStream;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
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
import org.junit.Rule;
import org.junit.rules.TestName;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.ui.ExpectedCondition;


@Ignore
public class BaseJsTest {
    @Rule
    public TestName testName = new TestName();
    
    private static String REPORT_DIR = "../coverage/";
    protected static jscover.Main main = new Main();
    protected WebDriver driver;   
    private static String JSCOVER_PORT = "3129";
    protected static int HTTP_PORT = 8085;
    protected static String hn = "localhost";
    
    private static boolean DO_COVERAGE = false;


    public BaseJsTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                main.runMain(new String[] {
                    "-ws",
                    "--port=" + JSCOVER_PORT,
                    "--proxy",
                    "--local-storage",
                    "--no-instrument=/scripts/jquery.js",
                    "--no-instrument-reg=.*Test\\.js",
                    "--report-dir=" + REPORT_DIR,
                    "--log=INFO"
                }); }
        }).start();
        
        // We can't use localhost because then chrome will bypass the proxy
        // (despite the docs for proxy-bypass-list=<-loopback>)
        // So in order for confusion we use the hostname instead
        try {
           hn = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(BaseJsTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        HTTP.getDefaultInstance().startHttpServer(HTTP_PORT);
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
                        REPORT_DIR,
                        REPORT_DIR
                    });
                } catch (IOException ex) {
                    Logger.getLogger(BaseJsTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }
    
    @Before
    public void setUp() {
        Proxy proxy = new Proxy().setHttpProxy("localhost:" + JSCOVER_PORT);
        
        ChromeOptions options = new ChromeOptions();
        // Tested in Google Chrome 59 on Linux. More info on:
        // https://developers.google.com/web/updates/2017/04/headless-chrome
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        
        // We need it large enough to hold a Full HD screen
        options.addArguments("--window-size=1920,1080");

        // Enable loging in chrome including Level.INFO (but that doesn't work yet)
        options.addArguments("--enable-logging=stderr --v=1");  
        
        if (DO_COVERAGE) {
            options.addArguments("--proxy-server=\"http://localhost:" + JSCOVER_PORT + "\"");
            options.addArguments("--proxy-bypass-list=\"<-loopback>\"");
        }
        
        // Enable logging in webdriver
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, java.util.logging.Level.ALL);
        options.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);

        // JScover proxy
        if (DO_COVERAGE)
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

            if (DO_COVERAGE) {
                executeScript("window.jscoverFinished = false;");
                executeScript("jscoverage_report('', function(){window.jscoverFinished=true;});");
                (new WebDriverWait(driver, 10))
                    .until((ExpectedCondition<Boolean>) new ExpectedCondition<Boolean>() {
                        @Override
                        public Boolean apply(WebDriver d) {
                            return (Boolean)((JavascriptExecutor) driver).executeScript("return window.jscoverFinished;");
                        }
                });
            }
            
            // How to figure out if a test has failed?
            try {
                byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (FileOutputStream fos = new FileOutputStream(
                        new File(REPORT_DIR, new java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss")
                                .format(new Date()) + "-" + testName.getMethodName() + ".png"))
                ) {
                    fos.write(png);
                }
            } catch (IOException ex) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            }
            
            driver.quit();
        }        
    }  
    
    
    protected WebElement findElement(String css) {
        try {
            return driver.findElement(By.cssSelector(css));
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            return null;
        }
    }
    
    
    public boolean elementHasClass(WebElement el, String clazz) {
        // Missing element returns false (nothing to search in)
        if (el == null)
            return false;
        
        // Missing or empty class returns true (nothing to search for)
        if (clazz == null || clazz.isBlank())
            return true;
        
        String clazzes = el.getAttribute("class");
        return Arrays.asList(clazzes.split(" ")).contains(clazz);
    }    
    
    
    protected void loadHtml(String page) {
        driver.get("http://" + hn + ":" + HTTP_PORT + page);        
    }
    
    
    protected Object executeScript(String script) {
        try {
            // Clear log
            ((JavascriptExecutor) driver).executeScript(
                    "var el = document.getElementById('log'); " +
                    "if (el) el.innerHTML = '';");
            
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
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            return null;
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, ex.getLocalizedMessage(), ex);
            throw(ex);
        }
    }
}
