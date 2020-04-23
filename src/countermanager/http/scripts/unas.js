/* Copyright (C) 2020 Christoph Theis */

/* global Packages */

function fixtures(matchList) {
    /*
        Returns a json array like from 
        https://results.ittf.com/ittf-web-results/html/TTE5146/match/d2020-03-04.json
        [{
            "Desc":"Women's Singles - Preliminary Round 2 - Match 8",
            "Time":"Wed 4, 10:00",
            RTime":"10:00",
            "IsTeam": false,
            "Loc":"T1",
            "Home":{
                "Org":"QAT", (Mixed for different associations)
                "Desc":"ALI Maryam/SOMEONE Else"
            },
            "Away":{
                "Org":"CHN", (MIX for different associations)
                "Desc":"QIAN Tianyi/SOMEONE Else"
            }
        }, ... ]

        Or for team matches
        https://results.ittf.com/ittf-web-results/html/TTE5171/match/d2020-01-26.json
        [{
            "Desc": "Men's Teams - Semifinals - Match 2",
            "Time": "Sun 26, 10:00",
            "Loc": "T1",
            "RTime": "10:00",
            "IsTeam": true,
            "Home": {
                "Org": "SVK",
                "Desc": "Slovakia",
            },
            "Away": {
                "Org": "HKG",
                "Desc": "Hong Kong, China",
            },
            "SubMatches": [{
                "Home": {
                    "Org": "SVK",
                    "Desc": "PISTEJ Lubomir/VALUCH Alexander",
                },
                "Away": {
                    "Org": "HKG",
                    "Desc": "HO Kwan Kit/NG Pak Nam",
                },
            }, ... ]
        },  ...  ]
    */
   
    // Helpers
    this.Desc = function(m) {
        return m.cpDesc + ' - ' + m.grDesc + ' - ' + this.Round(m) + ' - Match ' + m.mtMatch;
    };
    
    this.RTime = function(m) {
        this.df = new Packages.java.text.SimpleDateFormat("EEEE d, hh:MM");
        return this.df.format(m.mtDateTime);
    };
    
    this.Time = function(m) {
        this.df = new Packages.java.text.SimpleDateFormat("hh:MM");
        return this.df.format(m.mtDateTime);
    };
    
    this.HomeAwayPlayer = function(pl, bd) {
        var player = {Org: '', Desc: ''};
        if (pl === undefined || pl === null || pl.plNr === 0)
            return player;
        
        if (pl.plNr === 0)
            player.Org = '';
        else if (bd.plNr === 0 || pl.naName === bd.naName)
            player.Org = pl.naName;
        else
            player.Org = pl.naName + '/' + bd.naName;  // Or 'MIX'
        
        if (pl.plNr === 0)
            player.Desc = '';
        else
            player.Desc = pl.psLast + ' ' + pl.psFirst;
        
        if (bd.plNr !== 0)
            player.Desc += '/' + bd.psLast + ' ' + bd.psFrst;
        
        return player;
    };
    
    this.HomeAwayTeam = function(tm) {
        var team = {Org: '', Desc: ''};
        if (tm === undefined || tm === null)
            return team;
        
        team.Org = tm.tmName;
        team.Desc = tm.tmDesc;
        
        return team;
    };
    
    // SubMatches
    this.SubMatch = function(m) {
        var sm = {};
        
        sm.Home = this.HomeAwayPlayer(m.plA, m.plB);
        sm.Away = this.HomeAway(m.plX, m.plY);
        
        return sm;
    };
    
    // Format round
    this.Round = function(m) {
        if (m.grModus == 1)
            return 'Round ' + m.mtRound;
        else if (m.grWinner > 1)
            return 'Round ' + m.mtRound;
        else if ((m.grSize >> m.mtRound) == 1 && m.mtMatch == 1)
            return 'Final';
        else if ((m.grSize >> m.mtRound) == 2 && m.mtMatch <= 2)
            return 'Semifinals';
        else if ((m.grSize >> m.mtRound) == 4 && m.mtMatch <= 4)
            return 'Quarterfinals';
        else if (m.grModus == 2)
            return 'Round of ' + (2 * (m.grSize >> m.mtRound));
        else if (m.grModus == 4) {
            var nof = m.grSize >> m.mtRound;
            var m = m.mtMatch - 1;
            var from = (m / nof) * nof ;
            var to   = (from + nof);
            // Ein Spiel geht um 2 Plaetze
            return 'Pos ' + (m.grWinner + 2 * from) + '-' + (m.grWinner + 2 * to - 1);
        }           
        else
            return 'Round ' + m.mtRound;
    };

    // Collect results in an array
    var ret = [];
    
    // Matches are sorted in a way that team's individual matches are grouped
    // by team match and sorted in ascending order. We need to create objects
    // for each top level match and put individual team matches in a list of 
    // such objects
    
    var match = {};
    var mtNr = 0;
    
    matchList.forEach(function(m) {
        if (mtNr !== m.mtNr) {
            // Match has changed:
            // add last match (if any) to list and start new one
            if (mtNr !== 0)
                ret.push(match);
            
            match = {};
            mtNr = m.mtNr;
            
            match.Desc = this.Desc(m);
            match.RTime = this.RTime(m);
            match.Time = this.Time(m);

            if (m.cpType === 4) { // Team match
                match.Home = this.HomeAwayTeam(m.tmA);
                match.Away = this.HomeAwayTeam(m.tmX);
                
                match.SubMatches = [];
            } else {
                match.Home = this.HomeAwayPlayer(m.plA, m.plB);
                match.Away = this.HomeAwayPlayer(m.plX, m.plY);
            }
        }
        
        if (m.cpType == 4) {
            // Add new individual match
            var sm = {};
            sm,Home = this.HomeAwayPlayer(m.plA, m.plB);
            sm.Away = this.HomeAwayPlayer(m.plX, m.plY);
            
            match.SubMatches.push(sm);
        }         
    });
    
    // Matches are added when mtNr changes, so we still have to add the last match.
    if (mtNr !== 0)
        ret.push(match);
    
    return JSON.stringify(ret);
}


