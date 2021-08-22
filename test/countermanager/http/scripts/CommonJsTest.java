/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts;

import countermanager.http.BaseJsTest;
import org.junit.Assert;
import org.junit.Test;

import org.openqa.selenium.JavascriptExecutor;


public class CommonJsTest extends BaseJsTest{

    public CommonJsTest() {
    }
    
    @Test
    public void testFormatString() {
        // Load some piece of html with the script tag
        driver.get("http://localhost:" + HTTP_PORT + "/scripts/CommonJsTest.html");

        String paramres[][] = {
            { "var s = \"123456789\"; return formatString(s, -1);", "123456789" },
            { "var s = \"123456789\"; return formatString(s, 0);", "" },
            { "var s = \"123456789\"; return formatString(s, 1);", "1" },
            { "var s = \"123456789\"; return formatString(s, 10);", "123456789" },
        };

        for (String pr[] : paramres) {
            Object ret = ((JavascriptExecutor) driver).executeScript(pr[0]);
            Assert.assertEquals(pr[0], pr[1], ret);
        }
    }
    
    @Test
    public void testFormatRound() {
        // Load some piece of html with the script tag
        driver.get("http://localhost:" + HTTP_PORT + "/scripts/CommonJsTest.html");
        
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
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 3}; return formatRound(mt);", "Pos&nbsp;5&ndash;8" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 4}; return formatRound(mt);", "Pos&nbsp;5&ndash;8" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 5}; return formatRound(mt);", "Pos&nbsp;9&ndash;12" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 6}; return formatRound(mt);", "Pos&nbsp;9&ndash;12" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 7}; return formatRound(mt);", "Pos&nbsp;13&ndash;16" },
            { "var mt = {grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 8}; return formatRound(mt);", "Pos&nbsp;13&ndash;16" },
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
