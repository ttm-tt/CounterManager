/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import com.google.gson.Gson;
import countermanager.model.CounterModelMatch;
import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Match implements Comparable<Match> {
    public Match() {
        mtID = UUID.randomUUID().toString();
        mtResult = "";
    }
    
    @XmlAttribute
    @XmlID
    String mtID;
    
    @XmlAttribute
    int mtNr;
    
    @XmlAttribute
    int mtMS;
    
    @XmlElement
    @XmlIDREF
    Group gr;
    
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
    
    @XmlAttribute
    int    mttmResA;
    
    @XmlAttribute
    int    mttmResX;
    
    public boolean isFinished() {
        if (2 * mtResA > gr.cp.mtBestOf && mtResA > mtResX + 1)
            return true;
        
        if (2 * mtResX > gr.cp.mtBestOf && mtResX > mtResA + 1)
            return true;
        
        return false;
    }
    
    public CounterModelMatch toMatch() {
        CounterModelMatch match = new CounterModelMatch();
        
        match.cpType = 1;
        match.grModus = 1;
        
        if (gr != null) {
            match.cpName = gr.cp.cpName;
            match.cpDesc = gr.cp.cpDesc;
            match.cpType = gr.cp.cpType;
            match.grName = gr.grName;
            match.grDesc = gr.grDesc;
        }
        
        match.grStage = "Main Draw";
        match.grSize = 10;
        match.grWinner = 1;
        match.grNofRounds = 0;
        match.grNofMatches = 0;
        match.mtNr = mtNr;
        match.mtMS = mtMS;
        match.mtRound = mtRound;
        match.mtMatch = 1;
        match.mtReverse = false;
        match.mtBestOf = gr.cp.mtBestOf;
        match.mtMatches = gr.cp.mtMatches;
        match.mtDateTime = mtDateTime;
        match.mtTable = mtTable;

        if (plA != null) {
            match.plA.naName = plA.na.naName;
            match.plA.naRegion = plA.na.naRegion;
            match.plA.plExtID = "";
            match.plA.plNr = plA.plNr;
            match.plA.psFirst = plA.psFirst;
            match.plA.psLast = plA.psLast;
        }
    
        if (plB != null) {
            match.plB.naName = plB.na.naName;
            match.plB.naRegion = plB.na.naRegion;
            match.plB.plExtID = "";
            match.plB.plNr = plB.plNr;
            match.plB.psFirst = plB.psFirst;
            match.plB.psLast = plB.psLast;
        }
    
        if (plX != null) {
            match.plX.naName = plX.na.naName;
            match.plX.naRegion = plX.na.naRegion;
            match.plX.plExtID = "";
            match.plX.plNr = plX.plNr;
            match.plX.psFirst = plX.psFirst;
            match.plX.psLast = plX.psLast;
        }
         
        if (plY != null) {
            match.plY.naName = plY.na.naName;
            match.plY.naRegion = plY.na.naRegion;
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
        
        if (isFinished()) {
            if (mtResA > mtResX)
                ++mttmResA;
            else if (mtResX > mtResA)
                ++mttmResX;
        }
    }

    static Gson json = new Gson();

    @Override
    public int compareTo(Match mt) {
        if (mtTable < mt.mtTable)
            return -1;
        if (mtTable > mt.mtTable)
            return +1;

        if (mtDateTime < mt.mtDateTime)
            return -1;
        if (mtDateTime > mt.mtDateTime)
            return +1;

        if (mtNr < mt.mtNr)
            return -1;
        if (mtNr > mt.mtNr)
            return +1;

        if (mtMS < mt.mtMS)
            return -1;
        if (mtMS > mt.mtMS)
            return +1;

        return 0;
    }
}
