/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.monitor;

import com.google.gson.Gson;
import countermanager.driver.CounterData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import countermanager.http.BaseJsTest;
import org.junit.After;
import org.junit.Before;

import countermanager.model.database.simulation.Simulation;
import countermanager.model.CounterModelMatch;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.runners.Parameterized.Parameter;
import org.openqa.selenium.WebElement;



@RunWith(Parameterized.class)
public class MonitorJsTest extends BaseJsTest{
    @Parameters
    public static Collection<Object[]> swapScreen() {
        return Arrays.asList(new Object[][] {
            // {counter, monitor}
            {false, false}, 
            {false, true}, 
            {true,  false}, 
            {true,  true}
        });
    }
    
    @Parameter(0)        
    public boolean swapCounter = true;
    @Parameter(1)        
    public boolean swapMonitor = false;
    
    public MonitorJsTest() {
        
    }

    static Gson json = new Gson();
    
    @Before
    @Override
    public void setUp() {
        super.setUp();

        // Load monitor.html without refresh
        loadHtml("/monitor/monitor.html?layout=1920x1080&table=1&timeout=0&swap=" + (swapMonitor ? "1" : "0"));
        executeScript("$('head').append('<script type=\"module\" src=\"/monitor/MonitorJsTest.js\"></script>');");
    }
    
    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }
    
    
    @Test
    public void test_000_setup() {
        WebElement el = findElement("#log");
        Assert.assertNotNull(el);
        
        Object ret = executeScript("return Monitor;");
        Assert.assertNotNull(ret);
        
        ret = executeScript("return CounterData;");
        Assert.assertNotNull(ret);
        
        ret = executeScript("return console.log;");
        Assert.assertNotNull(ret);
    }
    
    
    @Test
    public void test_010_initialize() {
        // #content.invisible
        Assert.assertNotNull(findElement("#content.invisible"));
    }
    
    
    @Test
    public void test_020_initMatch() {
        String script;
        Object ret;

        Simulation db = new Simulation(0, 0);
        CounterModelMatch match = db.update(1, 1, java.time.LocalDate.now(), false).get(0);
        
        script = "Monitor.setCurrentMatch(JSON.parse('" + json.toJson(match) + "')); return true;";
        ret = executeScript(script);
        Assert.assertTrue((Boolean) ret);
        
        script = "return Monitor.getCurrentMatch();";
        ret = executeScript(script);
        Assert.assertNotNull(ret);
    }
    
    
    @Test
    public void test_030_testDataMatch() {
        String script;
        Object ret;

        Simulation db = new Simulation(1, 1);
        CounterModelMatch match = db.update(1, 1, java.time.LocalDate.now(), false).get(0);
        match.mttmResA = 1;
        match.mttmResX = 2;

        // Start with setting a match
        script = "Monitor.setCurrentMatch(JSON.parse('" + json.toJson(match) + "')); return true;";
        executeScript(script);
        
        // Set some counter data for the match
        CounterData data = createDataFromMatch(match);
        Map<String, Object> map = new java.util.HashMap<>() {{
            put("gameMode", CounterData.GameMode.RUNNING);
            put("timeMode", CounterData.TimeMode.MATCH);
            put("serviceRight", true);
            put("setsLeft", 1);
            put("setsRight", 2);
            put("setHistory", new int[][]{{11, 8}, {10, 12}, {6, 11}, {4, 3}, {0, 0}, {0, 0}, {0, 0}});
        }};
        
        data = mergeData(data, map);

        if (swapCounter)
            data = swapData(data);
        
        script = "Monitor.setCurrentData(JSON.parse('" + json.toJson(data) + "')); Monitor.onSuccess(); return true;";
        ret = executeScript(script);
        Assert.assertTrue((Boolean) ret);
        
        // Content is now visible
        Assert.assertFalse(elementHasClass(findElement("#content"), "invisible"));       
        
        // Verify HTML data
        // Counter is always as seen from umpire but we hide the swap in a function call
        boolean swap = swapCounter ^ swapMonitor;
        
        Assert.assertTrue("flaga", findElement("#flaga img").getAttribute("src").endsWith(swap ? "XYZ.png" : "ABC.png"));
        Assert.assertTrue("flagx", findElement("#flagx img").getAttribute("src").endsWith(swap ? "ABC.png" : "XYZ.png"));
        Assert.assertEquals(
                "teamresult",
                swap ? 
                        "" + match.mttmResX + ' ' + ":" + ' ' + match.mttmResA : 
                        "" + match.mttmResA + ' ' + ":" + ' ' + match.mttmResX,
                findElement("#teamresult span").getText()
        );
        Assert.assertTrue("nationleft", findElement("#nationleft").getText().startsWith(swap ? "XYZ" : "ABC"));
        Assert.assertTrue("nationright", findElement("#nationright").getText().startsWith(swap ? "ABC" : "XYZ"));
        Assert.assertTrue("nameleft", findElement("#nameleft").getText().startsWith(swap ? "Player X" : "Player A"));
        Assert.assertTrue("nameright", findElement("#nameright").getText().startsWith(swap ? "Player A" : "Player X"));
        Assert.assertEquals("gamesleft", swap ? "2" : "1", findElement("#gamesleft").getText());
        Assert.assertEquals("gamesright", swap ? "1" : "2", findElement("#gamesright").getText());
        Assert.assertEquals("pointsleft", swap ? "3" : "4", findElement("#pointsleft").getText());
        Assert.assertEquals("pointsright", swap ? "4" : "3", findElement("#pointsright").getText());
    }
    
    
    private CounterData createDataFromMatch(CounterModelMatch match) {
        // Create the data for a match
        countermanager.driver.ttm.CounterDataTTM cd = new countermanager.driver.ttm.CounterDataTTM();
        cd.setPlayerNrLeft(match.plA.plNr);
        cd.setPlayerNrRight(match.plX.plNr);
        cd.setSetHistory(new int[][] {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}});
        cd.setGameNr(match.mtNr);
        cd.setBestOf(5);
        cd.setGameMode(CounterData.GameMode.RESET);
        cd.setTimeMode(CounterData.TimeMode.NONE);
        
        return cd;
    }
    
    
    private CounterData mergeData(CounterData cd, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            char[] c = entry.getKey().toCharArray();
            c[0] = Character.toUpperCase(c[0]);
            String name = new String(c);
            java.lang.reflect.Method method = null;
            try {
                Class clazz = entry.getValue().getClass();
                if (clazz.equals(Integer.class))
                    clazz = Integer.TYPE;
                else if (clazz.equals(Boolean.class))
                    clazz = Boolean.TYPE;
                method = cd.getClass().getMethod("set" + name, clazz);
                method.invoke(cd, entry.getValue());
            }catch (Exception ex) {
                Logger.getLogger(BaseJsTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return cd;
    }
    
    
    private CounterData swapData(CounterData cd) {
        int tmp;
        
        tmp = cd.getSetsLeft();
        cd.setSetsLeft(cd.getSetsRight());
        cd.setSetsRight(tmp);
        
        int[][] setHistory = cd.getSetHistory();
        for (int[] set : setHistory) {
            tmp = set[0];
            set[0] = set[1];
            set[1] = tmp;
        }
        cd.setSetHistory(setHistory);
        
        tmp = cd.getPlayerNrLeft();
        cd.setPlayerNrLeft(cd.getPlayerNrRight());
        cd.setPlayerNrRight(tmp);
        
        {
            boolean b = cd.getServiceLeft();
            cd.setServiceLeft(cd.getServiceRight());
            cd.setServiceRight(b);
        }
        
        cd.setSwapped(!cd.isSwapped());
        
        return cd;
    }
}
