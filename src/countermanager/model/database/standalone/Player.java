/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Player {
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
        
        if (psFirst != null && !psFirst.isEmpty()) {
            s += psFirst;
            s += " ";
        }
        
        s += psLast;
        
        if (nation != null && nation.naName != null && !nation.naName.isEmpty()) {
            s += " (";
            s += nation.naName;
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
    Nation nation;
}
