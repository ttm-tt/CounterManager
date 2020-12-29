/* Copyright (C) 2020 Christoph Theis */
package countermanager.http.scripts.modules;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import countermanager.http.BaseJsTest;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CounterDataJsTest extends BaseJsTest {
    
    public CounterDataJsTest() {
    }
    
    @Before
    @Override
    public void setUp() {
        super.setUp();

        // Load some piece of html with the script tag
        loadHtml("/scripts/modules/CounterDataJsTest.html");
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