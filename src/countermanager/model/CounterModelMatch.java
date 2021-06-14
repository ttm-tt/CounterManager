/* Copyright (C) 2020 Christoph Theis */

package countermanager.model;

import countermanager.model.database.Umpire;
import javax.xml.bind.annotation.*;


@XmlRootElement
public class CounterModelMatch extends countermanager.model.database.Match implements countermanager.driver.IGameData {

    public CounterModelMatch() {
        plA = new Player();
        plB = new Player();
        plX = new Player();
        plY = new Player();
        
        tmA = new Team();
        tmX = new Team();
        
        // Not store in xml 
        up1 = new Umpire();
        up2 = new Umpire();
    }
    
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Player extends countermanager.model.database.Player {

        /**
         * @return the plNr
         */
        public int getPlNr() {
            return plNr;
        }

        /**
         * @return the plExtId
         */
        public String getPlExtID() {
            return plExtID;
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
         * @return the naDesc
         */
        public String getNaDesc() {
            return naDesc;
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
    public static class Team extends countermanager.model.database.Team {

        public String getNaName() {
            return naName;
        }
        
        public void setNaName(String naName) {
            this.naName = naName;
        }

        public String getNaDesc() {
            return naDesc;
        }
        
        public void setNaDesc(String naDesc) {
            this.naDesc = naDesc;
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
    
    // The following is required to allow XmlRpc to inspect and retrieve the fields
    /**
     * @return the cpType
     */
    public int getCpType() {
        return cpType;
    }

    /**
     * @return the cpSex
     */
    public int getCpSex() {
        return cpSex;
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
    public boolean getMtReverse() {
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
        return (long) mtDateTime;
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
        return (Player) plA;
    }

    /**
     * @return the plB
     */
    public Player getPlB() {
        return (Player) plB;
    }

    /**
     * @return the plX
     */
    public Player getPlX() {
        return (Player) plX;
    }

    /**
     * @return the plY
     */
    public Player getPlY() {
        return (Player) plY;
    }
    
    
    /**
     * @return tmA
     */
    public Team getTmA() {
        return (Team) tmA;
    }
    
    /**
     * @return tmX
     */
    public Team getTmX() {
        return (Team) tmX;
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
        return new java.util.Date((long) mtTimestamp);
    }

}