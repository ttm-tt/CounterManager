/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Nation implements Comparable<Nation> {
    public Nation() {
        naID = UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return naDesc;
    }
    
    @XmlAttribute
    @XmlID
    String naID;
    
    @XmlAttribute
    String naName;
    
    @XmlAttribute
    String naRegion;
    
    @XmlAttribute
    String naDesc;

    @Override
    public int compareTo(Nation na) {
        return naName.compareTo(na.toString());
    }
}
