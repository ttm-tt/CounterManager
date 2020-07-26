/* Copyright (C) 2020 Christoph Theis */

/*
 * Definition of the data model used in live ticker input
 */

export const GameMode = Object.freeze({
    RESET : 'RESET', 
    RUNNING : 'RUNNING', 
    WARMUP : 'WARMUP', 
    END : 'END', 
    INACTIVE : 'INACTIVE'
});

export const TimeMode = Object.freeze({
    NONE : 'NONE', 
    PREPARE : 'PREPARE', 
    MATCH : 'MATCH', 
    BREAK : 'BREAK', 
    TIMEOUT : 'TIMEOUT', 
    INJURY : 'INJURY'
});

export const Cards = Object.freeze({
    NONE : 'NONE',
    YELLOW : 'YELLOW',
    YR1P : 'YR1P',
    YR2P : 'YR2P'
});

// Internal: Are we before a mandatory change sides, e.g. end of game, last game is 5:x?
// We cannot go forward without changing sides, but change sides back and forth 
// should be the same as no side change
// One enum for both situation, because they cannot happen simultaneously, 
// but from the result we know which one it is
export const SideChange = Object.freeze({
    NONE   : 0,
    BEFORE : +1,
    AFTER  : -1
});

// Service
export const Service = Object.freeze({
    NONE : 0,
    LEFT : -1,
    RIGHT : +1
});

// Service Double
export const ServiceDouble = Object.freeze({
    NONE : 0,
    BX : 1,    // B -> X
    XA : 2,    // X -> A
    AY : 3,    // A -> Y
    YB : 4,    // Y -> B
    XB : -1,   // X -> B
    AX : -2,   // A -> X
    YA : -3,   // Y -> A
    BY : -4    // B -> Y
});

// Default player left / right
export const PlayerDefault = Object.freeze({
    LEFT : 0xFFFF,
    RIGHT : 0xFFFE
});

// Create new instance
export function create() {
    return {
        /* SideChange    */ sideChange : SideChange.NONE,
        /* Sevice        */ service : Service.NONE,
        /* ServiceDouble */ serviceDouble : ServiceDouble.NONE,
        /* Service       */ firstService : Service.NONE,
        /* ServiceDouble */ firstServiceDouble : ServiceDouble.NONE,
        /* Sevice        */ lastService : Service.NONE,
        /* ServiceDouble */ lastServiceDouble : ServiceDouble.NONE,
        
        /* boolean       */ woLeft : false,
        /* boolean       */ woRight : false,
        
        /* boolean       */ swappedPlayers: false,
        
        /* String  */  alertText : '',
        /* boolean */  expedite : false,
        /* int     */  gameTime : 0,
        /* Cards   */  cardLeft : Cards.NONE,
        /* Cards   */  cardRight : Cards.NONE,
        /* boolean */  locked : false,

        /* boolean  */  alert : false,
        /* GameMode */  gameMode : GameMode.RESET,
        /* boolean  */  serviceLeft : false,
        /* boolean  */  serviceRight : false,
        /* boolean  */  abandonOrAbort : false,
        /* int      */  gameNr : 0,
        /* int      */  playerNrLeft : PlayerDefault.LEFT,
        /* int      */  playerNrRight : PlayerDefault.RIGHT,
        /* int      */  bestOf : 5,
        /* TimeMode */  timeMode : TimeMode.NONE,
        /* boolean  */  timeoutLeft : false,
        /* boolean  */  timeoutRight : false,
        /* boolean  */  timeoutLeftRunning : false,
        /* boolean  */  timeoutRightRunning : false,
        /* boolean  */  injuredLeft : false,
        /* boolean  */  injuredRight : false,
        /* boolean  */  injuredLeftRunning : false,
        /* boolean  */  injuredRightRunning : false,
        /* int      */  time : 0,
        /* int      */  setsLeft : 0,
        /* int      */  setsRight : 0,
        /* int[][]  */  setHistory : [[0,0],[0,0],[0,0],[0,0],[0,0],[0,0],[0,0]],
        /* boolean  */  swapped : false,
        /* long     */  updateTime : 0,


        swap : function() {
            this.swapPlayers();
            this.swapCard();
            this.swapService();
            this.swapTimeout();
            this.swapTimeoutRunning();
            this.swapInjured();
            this.swapInjuredRunning();
            this.swapSets();
            this.swapSetHistory();
            
            this.swapped = !this.swapped;
        },

        swapPlayers : function() {
            const player = this.playerNrLeft;
            this.playerNrLeft = this.playerNrRight;
            this.playerNrRight = player;
            
            this.swappedPlayers = !this.swappedPlayers;
        },

        swapCard : function () {
            const card = this.cardLeft;
            this.cardLeft = this.cardRight;
            this.cardRight = card;
        },

        swapTimeout : function() {
            const timeout = this.timeoutLeft;
            this.timeoutLeft = this.timeoutRight;
            this.timeoutRight = timeout;
        },

        swapTimeoutRunning : function() {
            const running = this.timeoutLeftRunning;
            this.timeoutLeftRunning = this.timeoutRightRunning;
            this.timeoutRightRunning = running;
        },

        swapInjured : function() {
            const injured = this.injuredLeft;
            this.injuredLeft = this.injuredRight;
            this.injuredRight = injured;
        },

        swapInjuredRunning : function() {
            const running = this.injuredLeftRunning;
            this.injuredLeftRunning = this.injuredRightRunning;
            this.injuredRightRunning = running;
        },

        swapService : function() {
            const service = this.serviceLeft;
            this.serviceLeft = this.serviceRight;
            this.serviceRight = service;
            
            this.service = -this.service;
            this.serviceDouble = -this.serviceDouble;
        },

        swapSets : function() {
            const sets = this.setsLeft;
            this.setsLeft = this.setsRight;
            this.setsRight = sets;
        },

        swapSetHistory : function() {
            for (const i in this.setHistory) {
                const set = this.setHistory[i][0];
                this.setHistory[i][0] = this.setHistory[i][1];
                this.setHistory[i][1] = set;
            }
        },
        
        matchStarted : function() {
            if (this.gameMode !== GameMode.RESET)
                return true;
            
            if (this.setHistory[0] > 0 || this.setHistory[1] > 0)
                return true;
            
            return false;
        },
        
        matchFinished : function() {
            if (this.gameMode === GameMode.END)
                return true;

            let resA = this.setsLeft, resX = this.setsRight;
            
            // Finished by looking at games
            if (2 * resA > this.bestOf || 2 * resX > this.bestOf)
                return true;

            if (this.setHistory.length <= resA + resX)
                return false;

            // Not finished by games, but what about the current one?
            if (!this.gameFinished(resA + resX))
                return false;

            // Current game is finished, so add to resA + resX and check again
            if (this.setHistory[resA + resX][0] > this.setHistory[resA + resX][1])
                resA += 1;
            else
                resX += 1;

            if (2 * resA > this.bestOf || 2 * resX > this.bestOf)
                return true;

            return false;            
        },
        
        gameStarted : function(idx) {
            if (idx < 0 || idx >= this.setHistory.length)
                return false;

            if (this.setHistory[idx][0] > 0 || this.setHistory[idx][1] > 0)
                return true;

            return false;    
            
        },
        
        gameFinished : function(idx) {
            if (idx < 0 || idx >= this.setHistory.length)
                return false;

            if (this.setHistory[idx][0] >= 11 && this.setHistory[idx][0] >= this.setHistory[idx][1] + 2)
                return true;

            if (this.setHistory[idx][1] >= 11 && this.setHistory[idx][1] >= this.setHistory[idx][0] + 2)
                return true;

            return false;            
        }
    };
}