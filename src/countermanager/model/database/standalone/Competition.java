/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Competition implements Comparable<Competition> {
    public Competition() {
        cpID = UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return cpDesc;
    }
    
    @XmlAttribute
    @XmlID
    String cpID;
    
    @XmlAttribute
    String cpName;
    
    @XmlAttribute
    String cpDesc;
    
    @XmlAttribute
    int    cpType;
    
    @XmlAttribute
    int    mtBestOf;
    
    @XmlAttribute
    int    mtMatches;

    @Override
    public int compareTo(Competition cp) {
        return cpName.compareTo(cp.cpName);
    }
}
