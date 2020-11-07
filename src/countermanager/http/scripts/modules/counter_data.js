/* Copyright (C) 2020 Christoph Theis */

/*
 * Definition of the data model used in live ticker input
 */

import CounterSettings from './counter_settings.js';


export const GameMode = Object.freeze({
    RESET    : 'RESET', 
    RUNNING  : 'RUNNING', 
    WARMUP   : 'WARMUP', 
    END      : 'END', 
    INACTIVE : 'INACTIVE'
});

export const TimeMode = Object.freeze({
    NONE    : 'NONE', 
    PREPARE : 'PREPARE', 
    MATCH   : 'MATCH', 
    BREAK   : 'BREAK', 
    TIMEOUT : 'TIMEOUT', 
    INJURY  : 'INJURY'
});

export const TimeInterval = Object.freeze({
    PREPARE : 120,
    MATCH   : 600,
    BREAK   : 60,
    TIMEOUT : 60,
    INJUREY : 600
});

export const Cards = Object.freeze({
    NONE   : 'NONE',
    YELLOW : 'YELLOW',
    YR1P   : 'YR1P',
    YR2P   : 'YR2P'
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

/*
   Service 
   A or X tells which player serves. If A is on the right side, Service.A means
   right side serves to left side

   AX etc. tells which player serves to whom. If A/B are on the right side,
   A is staying on the right side of the table And X on the left, both player
   are the right side players of the pair   
 */

// Service
export const Service = Object.freeze({
    NONE : 0,
    A    : -1,
    X    : +1
});

/*
        B
    4 /   \ 1
     Y     X
    3 \   / 2
        A
 */
// Service Double
export const ServiceDouble = Object.freeze({
    NONE :  0,
    BX   :  1,    // B -> X
    XA   :  2,    // X -> A
    AY   :  3,    // A -> Y
    YB   :  4,    // Y -> B
    XB   : -1,   // X -> B
    AX   : -2,   // A -> X
    YA   : -3,   // Y -> A
    BY   : -4    // B -> Y
});

// Default player left / right
export const PlayerDefault = Object.freeze({
    LEFT  : 0xFFFF,
    RIGHT : 0xFFFE
});

// Create new instance
export function create(match = null) {
    let ret = {
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
        /* int     */  gameTime : TimeInterval.MATCH,
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
            
            return this;
        },

        swapPlayers : function() {
            const player = this.playerNrLeft;
            this.playerNrLeft = this.playerNrRight;
            this.playerNrRight = player;
            
            this.swappedPlayers = !this.swappedPlayers;
            
            return this;
        },

        swapCard : function () {
            const card = this.cardLeft;
            this.cardLeft = this.cardRight;
            this.cardRight = card;
            
            return this;
        },

        swapTimeout : function() {
            const timeout = this.timeoutLeft;
            this.timeoutLeft = this.timeoutRight;
            this.timeoutRight = timeout;
            
            return this;
        },

        swapTimeoutRunning : function() {
            const running = this.timeoutLeftRunning;
            this.timeoutLeftRunning = this.timeoutRightRunning;
            this.timeoutRightRunning = running;
            
            return this;
        },

        swapInjured : function() {
            const injured = this.injuredLeft;
            this.injuredLeft = this.injuredRight;
            this.injuredRight = injured;
            
            return this;
        },

        swapInjuredRunning : function() {
            const running = this.injuredLeftRunning;
            this.injuredLeftRunning = this.injuredRightRunning;
            this.injuredRightRunning = running;
            
            return this;
        },

        swapService : function() {
            const service = this.serviceLeft;
            this.serviceLeft = this.serviceRight;
            this.serviceRight = service;
            
            return this;
        },

        swapSets : function() {
            const sets = this.setsLeft;
            this.setsLeft = this.setsRight;
            this.setsRight = sets;
            
            return this;
        },

        swapSetHistory : function() {
            for (const i in this.setHistory) {
                const set = this.setHistory[i][0];
                this.setHistory[i][0] = this.setHistory[i][1];
                this.setHistory[i][1] = set;
            }
            
            return this;
        },
        
        hasMatchStarted : function() {
            if (this.gameMode !== GameMode.RESET)
                return true;
            
            if (this.setHistory.length === 0)
                return false;
            
            if (this.setHistory[0][0] > 0 || this.setHistory[0][1] > 0)
                return true;
            
            return false;
        },
        
        hasMatchFinished : function() {
            if (this.gameMode === GameMode.END)
                return true;

            let resA = this.setsLeft, resX = this.setsRight;
            
            // Finished by looking at games
            if (2 * resA > this.bestOf || 2 * resX > this.bestOf)
                return true;

            if (this.setHistory.length <= resA + resX)
                return false;

            // Not finished by games, but what about the current one?
            if (!this.hasGameFinished(resA + resX))
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
        
        hasGameStarted : function(idx) {
            if (idx < 0 || idx >= this.setHistory.length)
                return false;

            if (this.setHistory[idx][0] > 0 || this.setHistory[idx][1] > 0)
                return true;

            return false;    
            
        },
        
        hasGameFinished : function(idx) {
            if (idx < 0 || idx >= this.setHistory.length)
                return false;
            
            let pts = (idx === this.bestOf - 1 ? CounterSettings.pointsToPlayLastGame : CounterSettings.pointsToPlay);
            let win = (idx === this.bestOf - 1 ? CounterSettings.leadToWinLastGame : CounterSettings.leadToWin);

            if (this.setHistory[idx][0] >= pts && this.setHistory[idx][0] >= this.setHistory[idx][1] + win)
                return true;

            if (this.setHistory[idx][1] >= pts && this.setHistory[idx][1] >= this.setHistory[idx][0] + win)
                return true;

            return false;            
        },
        
        // Is service on the left side
        hasServiceLeft : function() {
            // We need parenthesis to avoid automatic insertion of ';' after return
            return (
                this.service === Service.A && !this.swappedPlayers ||
                this.service === Service.X && this.swappedPlayers
            );
        },
        
        // Is service on the left side
        hasServiceRight : function() {
            // We need parenthesis to avoid automatic insertion of ';' after return
            return (
                this.service === Service.A && this.swappedPlayers ||
                this.service === Service.X && !this.swappedPlayers
            );
        }
    };
    
    if (match !== null) {
        ret.gameNr = match.mtMS > 1 ? match.mtMS : match.mtNr;
        ret.bestOf = match.mtBestOf;
        
        ret.playerNrLeft = match.plA.plNr;
        ret.playerNrRight = match.plX.plNr;
    }
    
    return ret;
}