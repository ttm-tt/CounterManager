/* Copyright (C) 2020 Christoph Theis */
package countermanager.liveticker.ttm;

import countermanager.driver.ttm.CounterDataTTM;
import countermanager.model.CounterModelMatch;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;


public class TTMTest {
    public TTMTest() {
       
    }
        
    @BeforeClass
    public static void setUpClass() throws java.io.IOException {

    }
    
    @AfterClass
    public static void tearDownClass() {

    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void test_010_getUpdateString() {
        long ct = System.currentTimeMillis();
        TTM ttm = new TTM();
        
        // Fill in some value, we don't care of they are real matches
        ttm.updates.put(0, "Test counter 1");
        ttm.updates.put(1, "Test counter 2");
        
        String ret = ttm.getUpdateString(ct);
        assertNotNull(ret);
        assertEquals(0, ttm.msgList.size());
        assertEquals(2, ttm.updates.size());
        assertEquals(ret, ttm.lastUpdateString);
        
        // Set update delay to something larger
        ttm.lastUpdateString = "";
        ttm.setUploadDelay(2);
        ret = ttm.getUpdateString(ct);
        assertNull(ret);
        assertEquals(1, ttm.msgList.size());
        assertEquals(2, ttm.updates.size());
        
        // Now some time later, but don't add a new string
        ret = ttm.getUpdateString(ct + 3000);
        assertNotNull(ret);
        assertEquals(0, ttm.msgList.size());
        assertEquals(2, ttm.updates.size());
    }
    
    
    @Test
    public void test_020_doUpdateCounter() {
        long ct = System.currentTimeMillis();
        TTM ttm = new TTM();
        
        ttm.doCounterChanged(1, new CounterDataTTM(), new CounterModelMatch(), ct);
        
        assertEquals(1, ttm.updates.size());
        assertNotNull(ttm.updates.get(1));
        assertEquals(1, ttm.expires.size());
        assertEquals(ct, ttm.expires.get(1).longValue());
    }    
}
