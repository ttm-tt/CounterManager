/* Copyright (C) 2020 Christoph Theis */


package countermanager.model.database.standalone;

import javax.xml.bind.annotation.*;

@XmlRootElement
public class Database {
    @XmlElementWrapper(name="competitions")
    @XmlElement(name="competition")
    @SuppressWarnings("UseOfObsoleteCollectionType")
    java.util.Vector<Competition> competitions = new java.util.Vector<>();
    
    @XmlElementWrapper(name="groups")
    @XmlElement(name="group")
    @SuppressWarnings("UseOfObsoleteCollectionType")
    java.util.Vector<Group> groups = new java.util.Vector<>();
    
    @XmlElementWrapper(name="nations")
    @XmlElement(name="nation")
    @SuppressWarnings("UseOfObsoleteCollectionType")
    java.util.Vector<Nation> nations = new java.util.Vector<>();
    
    @XmlElementWrapper(name="players")
    @XmlElement(name="player")
    @SuppressWarnings("UseOfObsoleteCollectionType")
    java.util.Vector<Player> players = new java.util.Vector<>();
    
    @XmlElementWrapper(name="matches")
    @XmlElement(name="match")
    @SuppressWarnings("UseOfObsoleteCollectionType")
    java.util.Vector<Match> matches = new java.util.Vector<>();
    
    @XmlAttribute
    int lastMtNr = 0;
}
