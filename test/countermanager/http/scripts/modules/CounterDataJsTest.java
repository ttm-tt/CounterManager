/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts.modules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import countermanager.http.scripts.BaseJsTest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CounterDataJsTest extends BaseJsTest {
    
    public CounterDataJsTest() {
    }
    
    @Before
    @Override
    public void setUp() {
        super.setUp();

        // Load some piece of html with the script tag
        // We can't use localhost because then chrome will bypass the proxy
        // (despite the docs for proxy-bypass-list=<-loopback>)
        // So in order for confusion we use the hostname instead
        String hn = "localhost";
        try {
           hn = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(CounterDataJsTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        driver.get("http://" + hn + "/scripts/modules/CounterDataJsTest.html");
    }
    
    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }
    
    
    @Test
    public void test_01_create() {

    }
}