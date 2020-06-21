/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import com.google.gson.Gson;
import countermanager.model.CounterModelMatch;
import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Match {
    public Match() {
        mtID = UUID.randomUUID().toString();
        mtResult = "";
    }
    
    @XmlAttribute
    @XmlID
    String mtID;
    
    @XmlAttribute
    int mtNr;
    
    @XmlElement
    @XmlIDREF
    Competition cp;
    
    @XmlAttribute
    int mtRound;
    
    @XmlAttribute
    @XmlIDREF
    Player plA;
    
    @XmlAttribute
    @XmlIDREF
    Player plB;
    
    @XmlAttribute
    @XmlIDREF
    Player plX;
    
    @XmlAttribute
    @XmlIDREF
    Player plY;
    
    @XmlAttribute
    long mtDateTime;
    
    @XmlAttribute
    int    mtTable;
    
    @XmlAttribute
    String mtResult;
    
    @XmlAttribute
    int    mtResA;
    
    @XmlAttribute
    int    mtResX;
    
    public CounterModelMatch toMatch() {
        CounterModelMatch match = new CounterModelMatch();
        
        match.cpType = 1;
        match.grModus = 1;
        
        if (cp != null) {
            match.cpName = cp.cpName;
            match.cpDesc = cp.cpDesc;
            match.grName = cp.cpName;
            match.grDesc = cp.cpDesc;
        }
        
        match.grStage = "Main Draw";
        match.grSize = 10;
        match.grWinner = 1;
        match.grNofRounds = 0;
        match.grNofMatches = 0;
        match.mtNr = mtNr;
        match.mtMS = 0;
        match.mtRound = mtRound;
        match.mtMatch = 1;
        match.mtReverse = false;
        match.mtBestOf = cp.mtBestOf;
        match.mtMatches = 0;
        match.mtDateTime = mtDateTime;
        match.mtTable = mtTable;

        if (plA != null) {
            match.plA.naName = plA.nation.naName;
            match.plA.naRegion = plA.nation.naRegion;
            match.plA.plExtID = "";
            match.plA.plNr = plA.plNr;
            match.plA.psFirst = plA.psFirst;
            match.plA.psLast = plA.psLast;
        }
    
        if (plB != null) {
            match.plB.naName = plB.nation.naName;
            match.plB.naRegion = plB.nation.naRegion;
            match.plB.plExtID = "";
            match.plB.plNr = plB.plNr;
            match.plB.psFirst = plB.psFirst;
            match.plB.psLast = plB.psLast;
        }
    
        if (plX != null) {
            match.plX.naName = plX.nation.naName;
            match.plX.naRegion = plX.nation.naRegion;
            match.plX.plExtID = "";
            match.plX.plNr = plX.plNr;
            match.plX.psFirst = plX.psFirst;
            match.plX.psLast = plX.psLast;
        }
         
        if (plY != null) {
            match.plY.naName = plY.nation.naName;
            match.plY.naRegion = plY.nation.naRegion;
            match.plY.plExtID = "";
            match.plY.plNr = plY.plNr;
            match.plY.psFirst = plY.psFirst;
            match.plY.psLast = plY.psLast;
        }
        
        if (mtResult != null) {
            match.mtResult = json.fromJson(mtResult, int[][].class);
        }
        
        match.mtResA = mtResA;
        match.mtResX = mtResX;
        
        return match;
    }
    
    public void setResult(int[][] mtSets) {
        mtResult = json.toJson(mtSets);
        mtResA = mtResX = 0;
        for (int[] res : mtSets) {
            if (res[0] >= 11 && res[0] >= res[1] + 2)
                mtResA++;
            if (res[1] >= 11 && res[1] >= res[0] + 2)
                mtResX++;
        }
    }

    static Gson json = new Gson();
}
