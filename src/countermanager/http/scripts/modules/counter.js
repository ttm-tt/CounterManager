/* Copyright (C) 2020 Christoph Theis */

import * as CounterData from './counter_data.js';

/*
 * Base logic to count a score
 */

/**
 * 
 * @param {CounterData} data
 */
export function addPointLeft(data) {
    // Common logic, returns true if data has changed
    if (!addPoint(data, Side.LEFT))
       return;
   
    // Update UI
    fireListeners();
}

/**
 * 
 * @param {CounterData} data
 */
export function subPointLeft(data) {
    if (!subPoint(data, Side.LEFT))
        return;
    
    // Update UI
    fireListeners();
}

/**
 * 
 * @param {CounterData} data
 */
export function addPointRight(data) {
    // Common logic, returns true if data has changed
    if (!addPoint(data, Side.RIGHT))
       return;
   
    // Update UI
    fireListeners();
}

/**
 * 
 * @param {CounterData} data
 */
export function subPointRight(data) {
    if (!subPoint(data, Side.RIGHT))
        return;
    
    // Update UI
    fireListeners();
}

/**
 * 
 * @param {CounterData} data
 */
export function swapSides(data) {
    const cg = data.setsLeft + data.setsRight;
    
    // Nothing to do when match is finished
    if (data.gameMode == CounterData.GameMode.END)
        return;
    
    // Nothing to do when the match has finished
    if (data.hasMatchFinished())
        return;
    
    data.swap();
    
    // Calculate new state
    // Do we have to change sides?
    if (data.sideChange != CounterData.SideChange.NONE) {
        data.sideChange = -data.sideChange;
        
        // If in the last game, just reverse service
        if (cg === data.bestOf - 1 && data.hasGameStarted(cg)) {
            // This must be the middle of the last game
            // In this case just reverse the service
            data.service = -data.service;
            data.serviceDouble = -data.serviceDouble;
        } else if (data.sideChange === CounterData.SideChange.AFTER) {
            // Service starts on the same side
            data.service = data.firstService;
            data.serviceDouble = data.firstServiceDouble;
        } else {
            // Restore last service
            data.service = data.lastService;
            data.serviceDouble = data.lastServiceDouble;            
        }
    }
    
    // If we are in GameMode NONE we switch to WARMUP
    if (data.gamMode == CounterData.GameMode.NONE)
        data.gameMode = CounterData.GameMode.WARMUP;
    
    // A side change after end game increments the result
    if (data.hasGameFinished(cg)) {
        // This game has finished, go to next and updates games
        if (data.setHistory[cg][0] > data.setHistory[cg][1])
            ++data.setsLeft;
        else
            ++data.setsRight;
    } else if (cg > 0 && !data.hasGameStarted(cg)) {
        // This game has not started, go to previous (if there is one)
        if (data.setHistory[cg-1][0] > data.setHistory[cg-1][1])
            --data.setsLeft;
        else
            --data.setsRight;    
    }
    
    // Update UI
    fireListeners();
}


export function toggleServiceLeft(data) {
    if (!toggleService(data, Side.LEFT))
        return;
    
    fireListeners();
}


export function toggleServiceRight(data) {
    if (!toggleService(data, Side.RIGHT))
        return;
    
    fireListeners();
    
}


export function toggleServiceDoubleLeft(data) {
    if (!toggleServiceDouble(data, Side.LEFT))
        return;
    
    fireListeners();
}


export function toggleServiceDoubleRight(data) {
    if (!toggleServiceDouble(data, Side.RIGHT))
        return;
    
    fireListeners();
    
}


export function toggleTimeoutLeft(data) {
    
}


export function toggleTimeoutRight(data) {
    
}


export function setWOLeft(data) {
    
}


export function setWORight(data) {
    
}


export function setYLeft(data) {
    
}


export function setYR1Left(data) {
    
}


export function setYR2Left(data) {
    
}


export function setYRight(data) {
    
}


export function setYR1Right(data) {
    
}


export function setYR2Right(data) {
    
}


export function toggleStartGame(data) {
    
}


export function toggleExpedite(data) {
    
}


export function swapPlayers(data) {
    data.swapPlayers();
    
    fireListeners();
}


export function endMatch(data) {
    
}

// -----------------------------------------------------------------------
// Helpers
export function isChangeServiceRequired(data) {
    if (!data)
        return false;
    
    const cg = data.setsLeft + data.setsRight;
    const game = data.setHistory[cg];
    
    if (data.expedite)
        return true;
    
    if (game[0] >= 10 && game[1] >= 10)
        return true;
    if ( ((game[0] + game[1]) % 2) === 0 )
        return true;
    
    return false;
}


// -----------------------------------------------------------------------
// Internal helpers, no need to export

// enum LEFT / RIGHT
const Side = Object.freeze({
    LEFT : 0,
    RIGHT : 1
});

// Add point
function addPoint(data, side) {
    const cg = data.setsLeft + data.setsRight;
    
    if (data.setHistory.length <= cg)
        return false;
        
    // Left or right already won
    if (data.hasGameFinished(cg))
        return false;
    
    // We can't add a point just before a side change in the last game
    if (data.sideChange === CounterData.SideChange.BEFORE)
        return false;
    
    // Add point
    data.setHistory[cg][side] += 1;
    
    // Calculate new state:
    if (data.hasGameFinished(cg)) {
        // Game finished, change side required (or match is finished)
        // No difference to hasMatchFinished, we can just ignore the changes
        data.sideChange = CounterData.SideChange.BEFORE;
        
        // Remember who had the last service
        data.lastService = data.service;
        data.lastServiceDouble = data.serviceDouble;
    } 
    
    if (!data.hasGameFinished(cg) && isChangeServiceRequired(data)) {
        // Not finished, but change service required
        changeServiceNext(data);
    }
    
    // In the last game at 5:x change side required
    // The next point removes that flag
    if (data.sideChange === CounterData.SideChange.AFTER)
        data.sideChange = CounterData.SideChange.NONE;
    else if (cg === data.bestOf - 1 && 
            data.setHistory[cg][side] === 5 && 
            data.setHistory[cg][side === Side.LEFT ? Side.RIGHT : Side.LEFT] < 5) {
        data.sideChange = CounterData.SideChange.BEFORE;
    }
    
    // And update match and time status
    if (data.hasMatchFinished()) {
        // Not GameMode.END, this is only set when you click 'End Match'
        data.timeMode = CounterData.TimeMode.NONE;
    } else if (data.hasGameFinished(cg)) {
        data.gameMode = CounterData.GameMode.RUNNING;
        data.timeMode = CounterData.TimeMode.BREAK;
    } else {
        data.gameMode = CounterData.GameMode.RUNNING;
        data.timeMode = CounterData.TimeMode.MATCH;
    }
        
    return true;
}

// Sub point
function subPoint(data, side) {
    let cg = data.setsLeft + data.setsRight;
    
    if (data.setHistory.length <= cg)
        return false;
        
    if (!data.hasGameStarted(cg))
        return false;
    
    // We can't sub a point just after a side change in the last game
    if (data.sideChange == CounterData.SideChange.AFTER)
        return false;
    
    if (isChangeServiceRequired(data)) {
        // Not finished, but change service required
        changeServicePrev(data);
    }
    
    data.setHistory[cg][side] -= 1;

    // In the last game at 5:x a side change is required
    // Going further back removes that flag
    if (data.sideChange == CounterData.SideChange.BEFORE)
        data.sideChange = CounterData.SideChange.NONE;
    else if (cg === data.bestOf - 1 && 
            data.setHistory[cg][side] == 5 &&
            data.setHistory[cg][side === Side.LEFT ? Side.RIGHT : Side.LEFT] < 5) {
        data.sideChange = CounterData.SideChange.AFTER;
    }
    
    return true;
}

// Toggle service
function toggleService(data, side) {
    if (side === Side.LEFT) {
        if (data.hasServiceLeft()) {
            data.service = CounterData.Service.NONE;
            data.serviceDouble = CounterData.ServiceDouble.NONE;
        } else if (!data.playersSwapped) {
            data.service = CounterData.Service.A;
            data.serviceDouble = CounterData.ServiceDouble.BX;            
        } else {
            data.service = CounterData.Service.X;
            data.serviceDouble = CounterData.ServiceDouble.XB;
        }
    } else if (side === Side.RIGHT) {
        if (data.hasServiceRight()) {
            data.service = CounterData.Service.NONE;
            data.serviceDouble = CounterData.ServiceDouble.NONE;
        } else if (!data.playersSwapped) {
            data.service = CounterData.Service.X;
            data.serviceDouble = CounterData.ServiceDouble.XB;            
        } else {
            data.service = CounterData.Service.A;
            data.serviceDouble = CounterData.ServiceDouble.AY;
        }
    }
        
    // Calculate back who started with the service
    // We can do that only if we are not in expedite
    if (!data.expedite) {
        if (data.service === CounterData.Service.NONE) {
            data.firstService = CounterData.Service.NONE;
            data.firstServiceDouble = CounterData.ServiceDouble.NONE;
        } else {
            calculateFirstService(data);
        }
    }

    data.serviceLeft = data.hasServiceLeft();
    data.serviceRight = data.hasServiceRight();

    // And in any case we prepare the match
    if (data.gameMode === CounterData.GameMode.RESET) {
        data.gameMode = CounterData.GameMode.WARMUP;
        data.timeMode = CounterData.TimeMode.PREPARE;
    }
    
    return true;
}


// Toggle service double
function toggleServiceDouble(data, side) {
    // Nothing to do if no side has the service
    if (data.service === CounterData.Service.NONE)
        return false;
    
    // Toggle side with service: change both sides (BX becomes AY)
    if ( side === Side.LEFT && data.service === CounterData.Service.A ||
         side === Side.RIGHT && data.service === CounterData.Service.X ) {
        
        // BX becomes AY, XB becomes YA, etc.        
        // So we advance by 2, but we have to check for overflows
        data.serviceDouble += 2;
        
        if (data.serviceDouble < 0) {
            // [-4,-3] -> [-2,-1]
            // OK, no overflow from negative values
        } else if (data.serviceDouble < 2) {
            // [-2,-1] -> [0,1]
            // Overflow from negative values
            data.serviceDouble -= 4;
        } else if (data.serviceDouble < 4) {
            // [1,2] -> [3,4]
            // OK, no overflow from positive values
        } else if (data.serviceDouble > 4) {
            // [3,4] -> [5,6]
            // Overflow from positive values
            data.serviceDouble -= 4;
        }
    } else {
        // BX becomes BY, XB becomes XY
        /*
           +1 -> -4     -1 -> +2
           +2 -> -1     -2 -> +3
           +3 -> -2     -3 -> +4
           +4 -> -3     -4 -> +1
         */
        // One step back, the other one will serve to us
        data.serviceDouble -= 1;
        // Overflow: 1 became 0
        if (data.serviceDouble == 0)
            data.serviceDouble = 4;
        // Reverse service
        data.serviceDouble = -data.serviceDouble;
    }

    // Now calculate who would have the first service in this game
    calculateFirstServiceDouble(data);
}


// Change service for next point
function changeServiceNext(data) {
    data.service = -data.service;
    data.serviceLeft = data.hasServiceLeft();
    data.serviceRight = data.hasServiceRight();

    if (data.serviceDouble !== 0) {
        // 1..4,1 or -4..-1,-4
        ++data.serviceDouble;
        
        // turn-around clockwise: 5 is 1
        if (data.serviceDouble === 5)
            data.serviceDouble = 1;
        
        // turn-around counterclockwise: 0 is -4
        if (data.serviceDouble === 0)
            data.serviceDouble = -4;
    }
}

// Change service for next point
function changeServicePrev(data) {
    data.service = -data.service;
    data.serviceLeft = data.hasServiceLeft();
    data.serviceRight = data.hasServiceRight();

    if (data.serviceDouble !== 0) {
        // 1..4,1 or -4..-1,-4
        --data.serviceDouble;
        
        // turn-around counterclockwise: -5 is -1
        if (data.serviceDouble === -5)
            data.serviceDouble = -1;
        
        // turn-around clockwise: 0 is 4
        if (data.serviceDouble === 0)
            data.serviceDouble = 4;
    }
}


// Calculate who had the service at the beginning of the match
function calculateFirstService(data) {
    const cg = data.setsLeft + data.setsRight;
    const cp = data.setHistory[cg][0] + data.setHistory[cg][1];

    // Service repeats every 4th point for 2 points
    if ( Math.floor((cp % 4) / 2 ) )
        data.firstService = -data.service; // 2nd and 3rd rally
    else 
        data.firstService = data.service;  // 0th and 1st rally
    
    // Check if we are after the side change in the last game
    // Going forward or backward, the moment one reaches 5 and the other have less
    // we will at the point of side change. Going on or further back will reset
    // the flag.
    if ( cg == data.bestOf - 1 && (
            data.sideChange == CounterData.SideChange.AFTER || 
            data.setHistory[cg][0] > 5 ||
            data.setHistory[cg][1] > 5) ) {
        data.firstService = -data.firstService;
    }
    
    calculateFirstServiceDouble(data);
}


function calculateFirstServiceDouble(data) {
    const cg = data.setsLeft + data.setsRight;
    const cp = data.setHistory[cg][0] + data.setHistory[cg][1];
    // Running in the cycle where are we starting from the beginning
    const offset = Math.floor((cp % 8) / 2);

    // Double, repeats every 8th point for 2 points
    if (data.serviceDouble > 0) {
        data.firstServiceDouble = data.serviceDouble -offset;
        if (data.firstServiceDouble <= 0)
            data.firstServiceDouble += 4;
    } else if (data.serviceDouble < 0) {
        data.firstServiceDouble = data.serviceDouble + offset;
        if (data.firstServiceDouble >= 0)
            data.firstServiceDouble -= 4;
    }  
    
    // Check if we are after the side change in the last game
    // Going forward or backward, the moment one reaches 5 and the other have less
    // we will at the point of side change. Going on or further back will reset
    // the flag.
    if ( cg == data.bestOf - 1 && (
            data.sideChange == CounterData.SideChange.AFTER || 
            data.setHistory[cg][0] > 5 ||
            data.setHistory[cg][1] > 5) ) {
        data.firstServiceDouble = -data.firstServiceDouble;
    }    
}

// -----------------------------------------------------------------------
// Update GUI
export function addListener(callback) {
    listeners.push(callback);
}

function fireListeners() {
    for (var li of listeners) {
        li();
    }
}

// -----------------------------------------------------------------------
// Internal administration
var listeners = [];

