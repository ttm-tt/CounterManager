/* Copyright (C) 2020 Christoph Theis */
package countermanager.liveticker.ttm;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import countermanager.model.database.Match;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;


public class UpdateMatchTest {
    public UpdateMatchTest() {
       
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
    public void test_010_roundToString() {
        Gson json = new Gson();
        Type type = new TypeTokenImpl().getType();

        // We define the values we want to set as a JSON string, it is easier.
        String paramres[][] = {
            // Round Robin
            { "{grModus: 1, grSize: 4, grWinner: 1, mtRound: 1, mtMatch: 1}", "Rd.&nbsp;1" },
            // KO
            { "{grModus: 2, grSize: 16, grWinner: 1, mtRound: 1, mtMatch: 1}", "Rd.&nbsp;of&nbsp;16" },
            { "{grModus: 2, grSize: 16, grWinner: 1, mtRound: 2, mtMatch: 1}", "QF" },
            { "{grModus: 2, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 1}", "SF" },
            { "{grModus: 2, grSize: 16, grWinner: 1, mtRound: 4, mtMatch: 1}", "F" },
            // Play-Off
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 1}", "SF" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 2}", "SF" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 3}", "Pos&nbsp;5&mdash;8" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 4}", "Pos&nbsp;5&mdash;8" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 5}", "Pos&nbsp;9&mdash;12" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 6}", "Pos&nbsp;9&mdash;12" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 7}", "Pos&nbsp;13&mdash;16" },
            { "{grModus: 4, grSize: 16, grWinner: 1, mtRound: 3, mtMatch: 8}", "Pos&nbsp;13&mdash;16" },
            // Not finals
            { "{grModus: 2, grSize: 16, grWinner: 17, mtRound: 3, mtMatch: 1}", "Rd.&nbsp;of&nbsp;4" },
            { "{grModus: 2, grSize: 16, grWinner: 1, grNofRounds: 3, mtRound: 3, mtMatch: 1}", "Rd.&nbsp;3" },
            // TODO Qualification round(s)
        };
        
        for (String[] pr : paramres) {
            Match cm = json.fromJson(pr[0], type);                
            assertEquals(pr[0], pr[1], UpdateMatch.roundToString(cm));
        }
        
        assertTrue(true);
    }

    @Ignore
    @Test
    public void test_999_dummy() {
        // UpdateMatch mt = new UpdateMatch();
    }

    private static class TypeTokenImpl extends TypeToken<Match> {

        public TypeTokenImpl() {
        }
    }
    
}
