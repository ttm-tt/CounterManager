/* Copyright (C) 2022 Christoph Theis */
package countermanager.liveticker.atos;

public class AtosData {

    public String getIdMatch() {
        return idMatch;
    }

    public void setIdMatch(String idMatch) {
        this.idMatch = idMatch;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }
    
    public AtosData(String idMatch, String matchString) {
        this.idMatch = idMatch;
        // AABBBCCM
        // AA: Event (MS)
        // BBB: Match (3 digit)
        // CC: only team events. Leave "  "
        // M: Match in team, leave 0
        
        event = matchString.substring(0, 2);
        match = matchString.substring(2, 5);
    }
    
    
    public String idMatch;
    public String event;
    public String match;   
}
