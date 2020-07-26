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
    // Nothing to do when match is finished
    if (data.gameMode == CounterData.GameMode.END)
        return;
    
    data.swap();
    
    // Calculate new state
    // Do we have to change sides?
    if (data.sideChange != CounterData.SideChange.NONE) {
        data.sideChange = -data.sideChange;
        
        if (data.sideChange === CounterData.SideChange.AFTER) {
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
    const cg = data.setsLeft + data.setsRight;
    if (data.gameFinished(cg)) {
        // This game has finished, go to next and updates games
        if (data.setHistory[cg][0] > data.setHistory[cg][1])
            ++data.setsLeft;
        else
            ++data.setsRight;
    } else if (cg > 0 && !data.gameStarted(cg)) {
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


export function setTimeoutLeft(data) {
    
}


export function setTimeoutRight(data) {
    
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
export function changeServiceRequired(data) {
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
    if (data.gameFinished(cg))
        return false;
    
    // Add point
    data.setHistory[cg][side] += 1;
    
    // Calculate new state:
    if (data.gameFinished(cg)) {
        // Game finished, change side required (or match is finished)
        data.sideChange = CounterData.SideChange.BEFORE;
        
        // Remember who had the last service
        data.lastService = data.service;
        data.lastServiceDouble = data.serviceDouble;
    } 
    
    if (!data.gameFinished(cg) && changeServiceRequired(data)) {
        // Not finished, but change service required
        changeServiceNext(data);
    }
    
    // In the last game at 5:x change side required
    if (cg === data.bestOf && 
            data.setHistory[cg][side] === 5 && 
            data.setHistory[cg][side == Side.LEFT ? Side.RIGHT : Side.LEFT] < 5) {
        data.sideChange = CounterData.SideChange.BEFORE;
    } else if (data.sideChange === CounterData.SideChange.AFTER) {
        data.sideChange = CounterData.SideChange.NONE;
    }
    
    // And update match and time status
    if (data.matchFinished()) {
        // Not GameMode.END, this is only set when you click 'End Match'
        data.timeMode = CounterData.TimeMode.NONE;
    } else if (data.gameFinished(cg)) {
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
        
    if (!data.gameStarted(cg))
        return false;
    
    if (changeServiceRequired(data)) {
        // Not finished, but change service required
        changeServicePrev(data);
    }
    
    data.setHistory[cg][side] -= 1;
    
    // We can't be at a side change
    data.sideChange = CounterData.SideChange.NONE;
    
    return true;
}

// Toggle service
function toggleService(data, side) {
    if (side === Side.LEFT) {
        if (data.service === CounterData.Service.LEFT) {
            data.service = CounterData.Service.NONE;
            data.serviceDouble = CounterData.ServiceDouble.NONE;
        } else if (data.service === CounterData.Service.NONE) {
            data.service = CounterData.Service.LEFT;
            data.serviceDouble = CounterData.ServiceDouble.BX;            
        } else {
            data.service = -data.service;
            data.serviceDouble = -data.serviceDouble;
        }
    } else if (side === Side.RIGHT) {
        if (data.service === CounterData.Service.RIGHT) {
            data.service = CounterData.Service.NONE;
            data.serviceDouble = CounterData.ServiceDouble.NONE;
        } else if (data.service === CounterData.Service.NONE) {
            data.service = CounterData.Service.RIGHT;
            data.serviceDouble = CounterData.ServiceDouble.XB;            
        } else {
            data.service = -data.service;
            data.serviceDouble = -data.serviceDouble;
        }
    }
    
    // Calculate back who started with the service
    // We can do that only if we are not in expedite
    if (!data.expedite) {
        if (data.service === CounterData.Service.NONE) {
            data.firstService = CounterData.Service.NONE;
            data.firstServiceDouble = CounterData.ServiceDouble.NONE;
        } else {
            const cg = data.setsLeft + data.setsRight;
            const cp = data.setHistory[cg][0] + data.setHistory[cg][1];
            
            // Service repeats every 4th point for 2 points
            if ( (cp % 4) / 2 )
                data.firstService = -data.service; // 2nd and 3rd rally
            else 
                data.firstService = data.service;  // 0th and 1st rally
            
            // Double repeats every 8th point
            // TODO
        }
    }

    data.serviceLeft = (data.service === CounterData.Service.LEFT);
    data.serviceRight = (data.service === CounterData.Service.RIGHT);

    // And in any case we prepare the match
    if (data.gameMode === CounterData.GameMode.RESET) {
        data.gameMode = CounterData.GameMode.WARMUP;
        data.timeMode = CounterData.TimeMode.PREPARE;
    }
    
    return true;
}


// Change service for next point
function changeServiceNext(data) {
    data.service = -data.service;
    data.serviceLeft = (data.service === CounterData.Service.LEFT);
    data.serviceRight = (data.service === CounterData.Service.RIGHT);

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
    data.serviceLeft = (data.service === CounterData.Service.LEFT);
    data.serviceRight = (data.service === CounterData.Service.RIGHT);

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

