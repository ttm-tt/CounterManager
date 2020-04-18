/* Copyright (C) 2020 Christoph Theis */

package countermanager.model.database.league;

class MatchDetails {

    public enum MatchType {
        Single, 
        Double
    }
    
    public MatchType type = MatchType.Single;
    public String plApsFirst;
    public String plApsLast;
    public String plBpsFirst;
    public String plBpsLast;
    public String plXpsFirst;
    public String plXpsLast;
    public String plYpsFirst;
    public String plYpsLast;
    public int    mtTable;
    public long   mtDateTime;
    public int    mtBestOf;
    
    public String tmAtmName;
    public String tmAtmDesc;
    public String tmAnaName;
    public String tmXtmName;
    public String tmXtmDesc;
    public String tmXnaName;
}
