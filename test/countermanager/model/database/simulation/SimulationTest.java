/* Copyright (C) 2020 Christoph Theis */
package countermanager.model.database.simulation;

import countermanager.model.CounterModelMatch;
import java.time.LocalDate;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimulationTest {
    
    public SimulationTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
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
    public void testUpdate() {
        Simulation sim = new Simulation(0, 0);
        List list = sim.update(1, 1, LocalDate.MIN, true);
        assertEquals(1, list.size());
    }
    
    @Test
    public void testUpdateResult() {
        Simulation sim = new Simulation(0, 0);
        List<CounterModelMatch> list = sim.update(1, 1, LocalDate.MIN, true);
     
        {
            int[][] mtResult = {{0, 1}};
            sim.updateResult(1, 0, mtResult, 0, 0);
            list = sim.update(1, 1, LocalDate.MIN, true);
            assertEquals(2, list.size());
            assertEquals(0, list.get(0).mtResA);
            assertEquals(0, list.get(0).mtResA);
        }
        
        {
            int[][] mtResult = {{11, 3}, {0, 1}};
            sim.updateResult(1, 0, mtResult, 0, 0);
            list = sim.update(1, 1, LocalDate.MIN, true);
            assertEquals(2, list.size());
            assertEquals(1, list.get(0).mtResA);
            assertEquals(0, list.get(0).mtResX);
        }
        
    }
}
