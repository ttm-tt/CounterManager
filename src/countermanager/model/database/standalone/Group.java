/* Copyright (C) 2020 Christoph Theis */
package countermanager.model.database.standalone;

import java.util.UUID;
import javax.xml.bind.annotation.*;

public class Group implements Comparable<Group> {
    public Group() {
        grID = UUID.randomUUID().toString();        
    }
    
    @XmlElement
    @XmlIDREF
    public Competition cp;
    
    @XmlAttribute
    @XmlID
    public String grID;
    
    @XmlAttribute
    public String grName;
    
    @XmlAttribute
    public String grDesc;
    
    @XmlAttribute
    public String grStage;
    
    @Override
    public String toString() {
        return cp.cpName + " - " + grName;
    }

    @Override
    public int compareTo(Group gr) {
        return toString().compareTo(gr.toString());
    }
}
