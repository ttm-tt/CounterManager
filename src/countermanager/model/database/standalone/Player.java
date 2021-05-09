/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Player implements Comparable<Player> {
    public Player() {
        plID = UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        String s = "";
        
        if (psLast == null || psLast.isEmpty())
            return s;
        
        if (plNr != 0) {
            s += plNr;
            s += " ";
        }
        
        s += psLast;
        
        if (psFirst != null && !psFirst.isEmpty()) {
            s += ", ";
            s += psFirst;
        }
        
        if (na != null && na.naName != null && !na.naName.isEmpty()) {
            s += " (";
            s += na.naName;
            s += ")";
        }
        
        return s;
    }
    
    @XmlAttribute
    @XmlID
    String plID;
    
    @XmlAttribute
    int    plNr;
    
    @XmlAttribute
    String psFirst;
    
    @XmlAttribute
    String psLast;
    
    @XmlAttribute
    int    psSex;
    
    @XmlAttribute
    @XmlIDREF
    Nation na;

    @Override
    public int compareTo(Player pl) {
        return toString().compareTo(pl.toString());
    }
}
