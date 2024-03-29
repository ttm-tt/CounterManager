/* Copyright (C) 2020 Christoph Theis */

/*
 * Base logic to count a score
 */

import * as CounterData from './counter_data.js';
import CounterSettings from '../../counter/counter_settings.js';

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


export function incStrokes(data) {
    ++data.strokes;
    
    fireListeners();
}


export function decStrokes(data) {
    if (data.strokes > 0)
        --data.strokes;
    
    fireListeners();
}


export function clrStrokes(data) {
    data.strokes = 0;
    
    fireListeners();
}


/**
 * 
 * @param {CounterData} data
 */
export function swapSides(data) {
    const cg = data.setsLeft + data.setsRight;
    const sc = CounterSettings.sideChange;
    const sl = CounterSettings.sideChangeLastGame ? 5 : 0;
    
    if (data.gameMode === CounterData.GameMode.NONE)
        startTimer(data);
    
    // Nothing to do when match is finished
    if (data.gameMode == CounterData.GameMode.END)
        return;
    
    // Nothing to do when the match has finished
    if (data.hasMatchFinished())
        return;
    
    // Remember who had the last service before changing
    if (data.sideChange === CounterData.SideChange.BEFORE) {
        data.lastService = data.service;
        data.lastServiceDouble = data.serviceDouble;        
    }
    
    // Change sides except that would be a required change and we don't do that
    if (sc || data.sideChange == CounterData.SideChange.NONE)
        data.swap();
    
    // Do we have to change sides?
    if (data.sideChange != CounterData.SideChange.NONE) {
        data.sideChange = -data.sideChange;
        
        // If in the last game, just reverse service
        if (data.bestOf > 1 && cg === data.bestOf - 1 && data.hasGameStarted(cg)) {
            // This must be the middle of the last game
            // For doubles it would be the reverse of the previous service / return
            changeServicePrev(data);
            // And correct for the service change done above
            data.service = -data.service;
            data.serviceDouble = -data.serviceDouble;
        } else if (data.sideChange === CounterData.SideChange.AFTER) {
            // Each game the other players have service
            data.service = ((cg % 2) == 0) ? -data.firstService : +data.firstService;
            data.serviceDouble = ((cg % 2) == 0) ? -data.firstServiceDouble : +data.firstServiceDouble;
        } else {
            // Restore last service
            data.service = data.lastService;
            data.serviceDouble = data.lastServiceDouble;            
        }
    }
    
    // Recalculate which side has the service
    data.serviceLeft = data.hasServiceLeft();
    data.serviceRight = data.hasServiceRight();

    // If we are in GameMode NONE we switch to WARMUP
    if (data.gameMode == CounterData.GameMode.RESET) {
        data.gameMode = CounterData.GameMode.WARMUP;
        data.timeMode = CounterData.TimeMode.PREPARE;
        data.time = CounterData.TimeInterval.PREPARE;
        startTimer(data);
    }
    
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
    toggleTimeout(data, Side.LEFT);    
    fireListeners();
}


export function toggleTimeoutRight(data) {
    toggleTimeout(data, Side.RIGHT);    
    fireListeners();
}


export function setWOLeft(data) {
    if (!wo(data, Side.LEFT))
        return;
    
    fireListeners();
}


export function setWORight(data) {
    if (!wo(data, Side.RIGHT))
        return;
    
    fireListeners();
}


export function toggleYLeft(data) {
    toggleCard(data, CounterData.Cards.YELLOW, Side.LEFT);
    fireListeners();
}


export function toggleYR1PLeft(data) {
    toggleCard(data, CounterData.Cards.YR1P, Side.LEFT);    
    fireListeners();
}


export function toggleYR2PLeft(data) {
    toggleCard(data, CounterData.Cards.YR2P, Side.LEFT);    
    fireListeners();
}


export function toggleYRight(data) {
    toggleCard(data, CounterData.Cards.YELLOW, Side.RIGHT);    
    fireListeners();
}


export function toggleYR1PRight(data) {    
    toggleCard(data, CounterData.Cards.YR1P, Side.RIGHT);
    fireListeners();
}


export function toggleYR2PRight(data) {    
    toggleCard(data, CounterData.Cards.YR2P, Side.RIGHT);
    fireListeners();
}


export function toggleStartGame(data) {
    // When we start a game it must be running
    // If we stop a game we don't know where we land
    if (data.timeMode !== CounterData.TimeMode.MATCH) {
        data.timeMode = CounterData.TimeMode.MATCH;
        data.gameMode = CounterData.GameMode.RUNNING;
        
        // And no timeout running
        data.timeoutLeftRunning = false;
        data.timeoutRightRunning = false;
    } else if (data.timeMode === CounterData.TimeMode.MATCH) {
        data.timeMode = CounterData.TimeMode.NONE;
    }
    
    startTimer(data);
    
    fireListeners();
}


export function toggleExpedite(data) {
    data.expedite = !data.expedite;
    fireListeners();
}


export function swapPlayers(data) {
    if (data.gameMode === CounterData.GameMode.RESET ||
        data.gameMode === CounterData.GameMode.WARMUP) {
        // In case we are at the beginning of a match treat it like swapSides,
        // there is nothing we want to keep. Except, maybe, for service,
        // but we don't know if it was assigned because of player or of side.
        swapSides(data);
    } else {
        data.swapPlayers();  
    }
    fireListeners();
}


export function endMatch(data) {
    if (data.gameMode !== CounterData.GameMode.RUNNING)
        return;
    
    if (!data.hasMatchFinished())
        return;
    
    // Update games
    let cg = data.setsLeft + data.setsRight;
    if (data.setHistory[cg][0] > data.setHistory[cg][1])
        ++data.setsLeft;
    else
        ++data.setsRight;
    
    data.gameMode = CounterData.GameMode.END;
    fireListeners();
}

// -----------------------------------------------------------------------
// Helpers
export function isChangeServiceRequired(data) {
    if (!data)
        return false;
    
    const cg = data.setsLeft + data.setsRight;
    const game = data.setHistory[cg];
    const sc = (data.bestOf > 1 && cg === data.bestOf - 1 ? CounterSettings.serviceChangeLastGame : CounterSettings.serviceChange);
    const win = (data.bestOf > 1 && cg === data.bestOf - 1 ? CounterSettings.pointsToPlayLastGame : CounterSettings.pointsToPlay);
    
    if (data.expedite)
        return true;
    
    if (game[0] >= win - 1 && game[1] >= win - 1)
        return true;
    if ( ((game[0] + game[1]) % Math.max(1, sc)) === 0 )
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
    const cs = (data.bestOf > 1 && cg === data.bestOf - 1 ? CounterSettings.serviceChangeLastGame : CounterSettings.serviceChange);
    const sl = CounterSettings.sideChangeLastGame ? 5 : 0;
    
    // Reset strokes
    data.strokes = 0;
    
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
    } 
    
    if (isChangeServiceRequired(data)) {
        // Not finished, but change service required
        changeServiceNext(data);
    }
    
    // In the last game at 5:x change side required
    // The next point removes that flag
    if (data.sideChange === CounterData.SideChange.AFTER)
        data.sideChange = CounterData.SideChange.NONE;
    else if (data.bestOf > 1 && cg === data.bestOf - 1 && sl > 0 &&
            data.setHistory[cg][side] === sl && 
            data.setHistory[cg][side === Side.LEFT ? Side.RIGHT : Side.LEFT] < sl) {
        data.sideChange = CounterData.SideChange.BEFORE;
    }
    
    if (data.gameMode === CounterData.GameMode.NONE)
        startTimer(data);
    
    // And update match and time status
    if (data.hasMatchFinished()) {
        // Not GameMode.END, this is only set when you click 'End Match'
        data.timeMode = CounterData.TimeMode.NONE;
    } else if (data.hasGameFinished(cg)) {
        data.gameMode = CounterData.GameMode.RUNNING;
        data.timeMode = CounterData.TimeMode.BREAK;
    } else if (data.setHistory[cg][0] + data.setHistory[cg][1] >= 18) {
        data.gameMode = CounterData.GameMode.RUNNING;
        data.timeMode = CounterData.TimeMode.NONE;        
    } else {
        data.gameMode = CounterData.GameMode.RUNNING;
        data.timeMode = CounterData.TimeMode.MATCH;
    }
    
    return true;
}

// Sub point
function subPoint(data, side) {
    let cg = data.setsLeft + data.setsRight;
    const cs = (data.bestOf > 1 && cg === data.bestOf - 1 ? CounterSettings.serviceChangeLastGame : CounterSettings.serviceChange);
    const sl = CounterSettings.sideChangeLastGame ? 5 : 0;
    
    // Reset strokes
    data.strokes = 0;
    
    if (data.setHistory.length <= cg)
        return false;
        
    if (!data.hasGameStarted(cg))
        return false;
    
    // We can't sub a point at zero
    if (data.setHistory[cg][side] === 0)
        return false;
    
    // We can't sub a point just after a side change in the last game
    if (data.sideChange == CounterData.SideChange.AFTER)
        return false;
    
    if (isChangeServiceRequired(data)) {
        // Not finished, but change service required
        changeServicePrev(data);
    }
    
    if (data.hasGameFinished(cg)) {
        // If the game is finished it has been running before
        data.timeMode = CounterData.TimeMode.NONE; 
    }
    
    data.setHistory[cg][side] -= 1;

    // In the last game at 5:x a side change is required
    // Going further back removes that flag
    if (data.sideChange == CounterData.SideChange.BEFORE)
        data.sideChange = CounterData.SideChange.NONE;
    else if (data.bestOf > 1 && cg === data.bestOf - 1 && sl > 0 && 
            data.setHistory[cg][side] == sl &&
            data.setHistory[cg][side === Side.LEFT ? Side.RIGHT : Side.LEFT] < sl) {
        data.sideChange = CounterData.SideChange.AFTER;
    }
    
    return true;
}

// Toggle service
function toggleService(data, side) {
    if (side === Side.LEFT) {
        if (data.serviceLeft) {
            // Clear service
            data.service = CounterData.Service.NONE;
            data.serviceDouble = CounterData.ServiceDouble.NONE;
        } else if (!data.swapped) {
            // Service is now A
            data.service = CounterData.Service.A;
            data.serviceDouble = CounterData.ServiceDouble.BX;            
        } else {
            // Service is now X
            data.service = CounterData.Service.X;
            data.serviceDouble = CounterData.ServiceDouble.XB;
        }
    } else if (side === Side.RIGHT) {
        if (data.serviceRight) {
            // Clear service
            data.service = CounterData.Service.NONE;
            data.serviceDouble = CounterData.ServiceDouble.NONE;
        } else if (!data.swapped) {
            // Service is now X
            data.service = CounterData.Service.X;
            data.serviceDouble = CounterData.ServiceDouble.XB;            
        } else {
            // Service is now A
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
        data.time = CounterData.TimeInterval.PREPARE;
        
        startTimer(data);
    }
    
    return true;
}


// Toggle service double
function toggleServiceDouble(data, side) {
    // Nothing to do if no side has the service
    if (data.service === CounterData.Service.NONE)
        return false;
    
    // Toggle side with service: change both sides (BX becomes AY)
    if ( side === Side.LEFT && data.serviceLeft ||
         side === Side.RIGHT && data.serviceRight ) {
        
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
        // Overflow: 1 became 0, -4 becomes -5
        if (data.serviceDouble == 0)
            data.serviceDouble = 4;
        else if (data.serviceDouble === -5)
            data.serviceDouble = -1;
        // Reverse service
        data.serviceDouble = -data.serviceDouble;
    }

    // Now calculate who would have the first service in this game
    calculateFirstServiceDouble(data);
    
    return true;
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
    
    const cs = (data.bestOf > 1 && cg === data.bestOf - 1 ? CounterSettings.serviceChangeLastGame : CounterSettings.serviceChange);
    const sl = CounterSettings.sideChangeLastGame ? 5 : 0;

    // Service repeats every 4th (2*cs) point  for 2 points (cs)
    if ( Math.floor((cp % (2 * cs)) / Math.max(cs, 1)) )
        data.firstService = -data.service; // 2nd and 3rd rally
    else 
        data.firstService = data.service;  // 0th and 1st rally
    
    // Check if we are after the side change in the last game, if there is any
    // Going forward or backward, the moment one reaches 5 and the other have less
    // we will at the point of side change. Going on or further back will reset
    // the flag.
    if ( data.bestOf > 1 && cg == data.bestOf - 1 && sl > 0 && (
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
    const cs = (data.bestOf > 1 && cg === data.bestOf - 1 ? CounterSettings.serviceChangeLastGame : CounterSettings.serviceChange);
    const sl = CounterSettings.sideChangeLastGame ? 5 : 0;

    // Running in the cycle where are we starting from the beginning
    const offset = Math.floor((cp % (4 * cs)) / Math.max(1, cs));

    // Double, repeats every 8th (4 * cs) point for 2 (cs) points
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
    if ( data.bestOf > 1 && cg == data.bestOf - 1 && sl > 0 && (
            data.sideChange == CounterData.SideChange.AFTER || 
            data.setHistory[cg][0] > 5 ||
            data.setHistory[cg][1] > 5) ) {
        data.firstServiceDouble = -data.firstServiceDouble;
    }  else if ((cg % 2) == 1) {
        data.firstServiceDouble = -data.firstServiceDouble;
    }    
}


function toggleTimeout(data, side) {
    let to = (side === Side.LEFT ? 'timeoutLeft' : 'timeoutRight');
    let rg = to + 'Running';
    
    data[to] = !data[to];
    data[rg] = data[to];
    
    // Set time mode accordingly:
    // If we enter timeout set it to TIMEOUT
    // If we leave timeout and no side has timeout running, initialize it, 
    // the next point will start match time again.
    if (data[to])
        data.timeMode = CounterData.TimeMode.TIMEOUT;
    else if (!data.timeoutLeftRunning && !data.timeoutRightRunning)
        data.timeMode = CounterData.TimeMode.NONE;
    
    if (data.timeMode == CounterData.TimeMode.TIMEOUT)
        data.time = CounterData.TimeInterval.TIMEOUT;
}


function toggleCard(data, which, side) {
    let card = (side === Side.LEFT ? 'cardLeft' : 'cardRight');
    
    // Map cards to 0..n
    let map = {
        'NONE' : 0,
        'YELLOW' : 1,
        'YR1P' : 2,
        'YR2P' : 3
    };
    
    // And reverse
    let rev = {
        0 : CounterData.Cards.NONE,
        1 : CounterData.Cards.YELLOW,
        2 : CounterData.Cards.YR1P,
        3 : CounterData.Cards.YR2P
    };
    
    if (map[data[card]] >= map[which])
        data[card] = rev[map[which] - 1];
    else
        data[card] = rev[map[which]];
}


function wo(data, side) {
    let other = (side === Side.LEFT ? Side.RIGHT : Side.LEFT);

    if (data.hasMatchFinished())
        return false;
    
    for (let i = 0; !data.hasMatchFinished(); i++) {
        let pts = (data.beestOf > 1 && i === data.bestOf - 1 ? CounterSettings.pointsToPlayLastGame : CounterSettings.pointsToPlay);
        let win = (data.bestOf > 1 && i === data.bestOf - 1 ? CounterSettings.leadToWinLastGame : CounterSettings.leadToWin);

        if (!data.hasGameFinished(i)) {
            data.setHistory[i][other] = Math.max(data.setHistory[i][side] + win, pts);
        }
        
        if (i <= data.setsLeft + data.setsRight) {
            if (data.setHistory[i][0] > data.setHistory[i][1])
                ++data.setsLeft;
            else if (data.setHistory[i][1] > data.setHistory[i][0])
                ++data.setsRight;
        }        
    }
    
    if (side === Side.LEFT)
        data.woLeft = true;
    else
        data.woRight = true;
    
    data.gameMode = CounterData.GameMode.END;  
    
    return true;
}
 
// -----------------------------------------------------------------------
// Update GUI
export function addListener(callback) {
    listeners.push(callback);
}

function fireListeners() {
    for (let li of listeners) {
        li();
    }
}


// -----------------------------------------------------------------------
// Timer
// start timer
var currentSec;
export function startTimer(data) {
    clearInterval(tid);
    tid = setInterval(function() {onTimer(data);}, 100, data);
    currentSec = Math.floor(Date.now() / 1000);
}

function onTimer(data) {
    if (Math.floor(Date.now() / 1000) === currentSec)
        return;
    
    currentSec = Math.floor(Date.now() / 1000);
    
    if (data === undefined || data === null)
        return;
    
    switch (data.gameMode) {
        case CounterData.GameMode.NONE:
        case CounterData.GameMode.END:
            return;
    }
    
    switch (data.timeMode) {
        case CounterData.TimeMode.NONE:
            break;
            
        case CounterData.TimeMode.MATCH: 
            if (data.gameTime > 0)
                --data.gameTime;
            break;
            
        default:
            if (data.time > 0)
                --data.time;
            break;
    }
    
    fireListeners();
}


// -----------------------------------------------------------------------
// Timer ID
var tid = setInterval(function() {onTimer();}, 1000); 

// Internal administration
var listeners = [];

