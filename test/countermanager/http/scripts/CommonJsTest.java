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


public class CommonJsTest {

    private WebDriver driver;    
    
    public CommonJsTest() {
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

        driver = new ChromeDriver(options);
    }
    
    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }        
    }
    
    @Test
    public void testFormatRound() {
        // Load some piece of html with the script tag
        driver.get("http://localhost/scripts/CommonJsTest.html");
        
        String paramres[][] = {
            // Round Robin
            { "var mt = {grModus: 1, grSize: 4, grWinner: 1, mtRound: 1, mtMatch: 1}; return formatRound(mt);", "Rd.&nbsp;1" },
            // KO
            { "var mt = {grModus: 2, grSize: 16, grWinner: 1, mtRound: 1, mtMatch: 1}; return formatRound(mt);", "Rd.&nbsp;of&nbsp;16" },
            { "var mt = {grModus: 2, grSize: 16, grWinner: 1, mtRound: 2, mtMatch: 1}; return formatRound(mt);", "QF" },
            { "var mt = {grModus: 2, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 1}; return formatRound(mt);", "SF" },
            { "var mt = {grModus: 2, grSize: 16, grWinner: 1, mtRound: 4, mtMatch: 1}; return formatRound(mt);", "F" },
            // Play-Off
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 1}; return formatRound(mt);", "SF" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 2}; return formatRound(mt);", "SF" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 3}; return formatRound(mt);", "Pos&nbsp;5&mdash;8" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 4}; return formatRound(mt);", "Pos&nbsp;5&mdash;8" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 5}; return formatRound(mt);", "Pos&nbsp;9&mdash;12" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 6}; return formatRound(mt);", "Pos&nbsp;9&mdash;12" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 7}; return formatRound(mt);", "Pos&nbsp;13&mdash;16" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 8}; return formatRound(mt);", "Pos&nbsp;13&mdash;16" },
            // Not finals
            { "var mt = {grModus: 2, grSize: 16, grWinner: 17, mtRound: 3, mtMatch: 1}; return formatRound(mt);", "Rd.&nbsp;3" },
            { "var mt = {grModus: 2, grSize: 16, grWinner: 1, grNofRounds: 3, mtRound: 3, mtMatch: 1}; return formatRound(mt);", "Rd.&nbsp;3" },
            // Illegal
            { "return formatRound(undefined);", ""},
        };

        for (String pr[] : paramres) {
            Object ret = ((JavascriptExecutor) driver).executeScript(pr[0]);
            Assert.assertEquals(pr[0], pr[1], ret);
        }
    }
}
