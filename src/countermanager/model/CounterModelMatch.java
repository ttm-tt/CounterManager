/* Copyright (C) 2020 Christoph Theis */

package countermanager.model;

import javax.xml.bind.annotation.*;


@XmlRootElement
public class CounterModelMatch implements countermanager.driver.IGameData {

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Player {
        public int    plNr;
        public String plExtId = "";
        public String psLast = "";
        public String psFirst = "";
        public String naName = "";
        public String naRegion = "";

        /**
         * @return the plNr
         */
        public int getPlNr() {
            return plNr;
        }

        /**
         * @return the plExtId
         */
        public String getPlExtId() {
            return plExtId;
        }

        /**
         * @return the psLast
         */
        public String getPsLast() {
            return psLast;
        }

        /**
         * @return the psFirst
         */
        public String getPsFirst() {
            return psFirst;
        }

        /**
         * @return the naName
         */
        public String getNaName() {
            return naName;
        }

        /**
         * @return the naRegion
         */
        public String getNaRegion() {
            return naRegion;
        }
    }
    
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Team {
        public String naName = "";
        public String naRegion = "";
        public String tmName = "";
        public String tmDesc = "";

        public String getNaName() {
            return naName;
        }

        public void setNaName(String naName) {
            this.naName = naName;
        }

        public String getNaRegion() {
            return naRegion;
        }

        public void setNaRegion(String naRegion) {
            this.naRegion = naRegion;
        }

        public String getTmName() {
            return tmName;
        }

        public void setTmName(String tmName) {
            this.tmName = tmName;
        }

        public String getTmDesc() {
            return tmDesc;
        }

        public void setTmDesc(String tmDesc) {
            this.tmDesc = tmDesc;
        }
    }
    
    public int      cpType;
    public int      grModus;
    
    public String   cpName; 
    public String   cpDesc;
    public String   grName;
    public String   grDesc;
    public String   grStage;
    public int      grSize;
    public int      grWinner;
    public int      grNofRounds;
    public int      grNofMatches;
    public int      mtNr;
    public int      mtMS;
    public int      mtRound;
    public int      mtMatch;
    public int      mtReverse;
    public int      mtBestOf;
    public int      mtMatches;
    
    public long     mtDateTime;        
    public int      mtTable;
    
    public Player   plA = new Player();
    public Player   plB = new Player();
    public Player   plX = new Player();
    public Player   plY = new Player();
    
    public int      mtResA;
    public int      mtResX;

    public Team     tmA = new Team();
    public Team     tmX = new Team();

    public int      mttmResA;
    public int      mttmResX;

    public int      mtResult[][];
    
    public long     mtTimestamp;
    
    // The following is required to allow XmlRpc to inspect and retrieve the fields
    /**
     * @return the cpType
     */
    public int getCpType() {
        return cpType;
    }

    /**
     * @return the grModus
     */
    public int getGrModus() {
        return grModus;
    }

    /**
     * @return the cpName
     */
    public String getCpName() {
        return cpName;
    }

    /**
     * @return the cpDesc
     */
    public String getCpDesc() {
        return cpDesc;
    }

    /**
     * @return the grName
     */
    public String getGrName() {
        return grName;
    }

    /**
     * @return the grDesc
     */
    public String getGrDesc() {
        return grDesc;
    }

    /**
     * @return the grStage
     */
    public String getGrStage() {
        return grStage;
    }

    /**
     * @return the grSize
     */
    public int getGrSize() {
        return grSize;
    }

    /**
     * @return the mtNr
     */
    public int getMtNr() {
        return mtNr;
    }

    /**
     * @return the mtMS
     */
    public int getMtMS() {
        return mtMS;
    }

    /**
     * @return the mtRound
     */
    public int getMtRound() {
        return mtRound;
    }

    /**
     * @return the mtMatch
     */
    public int getMtMatch() {
        return mtMatch;
    }

    /**
     * @return the mtReverse
     */
    public int getMtReverse() {
        return mtReverse;
    }

    /**
     * @return the mtBestOf
     */
    public int getMtBestOf() {
        return mtBestOf;
    }

    /**
     * @return the mtMatches
     */
    public int getMtMatches() {
        return mtMatches;
    }

    /**
     * @return the mtDateTime
     */
    public long getMtDateTime() {
        return mtDateTime;
    }

    /**
     * @return the mtTable
     */
    public int getMtTable() {
        return mtTable;
    }

    /**
     * @return the plA
     */
    public Player getPlA() {
        return plA;
    }

    /**
     * @return the plB
     */
    public Player getPlB() {
        return plB;
    }

    /**
     * @return the plX
     */
    public Player getPlX() {
        return plX;
    }

    /**
     * @return the plY
     */
    public Player getPlY() {
        return plY;
    }
    
    
    /**
     * @return tmA
     */
    public Team getTmA() {
        return tmA;
    }
    
    /**
     * @return tmX
     */
    public Team getTmX() {
        return tmX;
    }

    /**
     * @return the mtResA
     */
    public int getMtResA() {
        return mtResA;
    }

    /**
     * @return the mtResX
     */
    public int getMtResX() {
        return mtResX;
    }

    /**
     * @return the mttmResA
     */
    public int getMttmResA() {
        return mttmResA;
    }

    /**
     * @return the mttmResX
     */
    public int getMttmResX() {
        return mttmResX;
    }
    
    
    public int[][] getMtResult() {
        return mtResult;
    }

    public java.util.Date getMtTimestamp() {
        return new java.util.Date(mtTimestamp);
    }

}