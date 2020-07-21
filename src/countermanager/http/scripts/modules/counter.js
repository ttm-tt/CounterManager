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
    data.swap();
    
    // Calculate new state
    // Do we have to change sides?
    if (data.sideChange != CounterData.SideChange.NONE)
        data.sideChange = -data.sideChange;
    
    // If we are in GameMode NONE we switch to WARMUP
    if (data.gamMode == CounterData.GameMode.NONE)
        data.gameMode = CounterData.GameMode.WARMUP;
    
    // Update UI
    fireListeners();
}


export function setServiceLeft(data) {
    if (!toggleService(data, Side.LEFT))
        return;
    
    fireListeners();
}


export function setServiceRight(data) {
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
    // Only change player names but not data
}


export function endMatch(data) {
    
}

// -----------------------------------------------------------------------
// Helpers
export function changeServiceRequired(data) {
    if (!data)
        return false;
    
    const cg = data.resA + data.resX;
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
    const cg = data.resA + data.resX;
    
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
    } 
    
    if (!data.gameFinished(cg) && changeServiceRequired(data)) {
        // Not finished, but change service required
        changeService(data);
    }
    
    // In the last game at 5:x change side required
    if (cg === data.bestOf && 
            data.setHistory[cg][side] === 5 && 
            data.setHistory[cg][side == Side.LEFT ? Side.RIGHT : Side.LEFT] < 5) {
        data.sideChange = CounterData.SideChange.BEFORE;
    }
        
    return true;
}

// Sub point
function subPoint(data, side) {
    let cg = data.resA + data.resX;
    
    if (data.setHistory.length <= cg)
        return false;
        
    if (!data.gameStarted(cg))
        return false;
    
    data.setHistory[cg][side] -= 1;
    
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

    data.serviceLeft = (data.service === CounterData.Service.LEFT);
    data.serviceRight = (data.service === CounterData.Service.RIGHT);
    
    return true;
}


// Change service
function changeService(data) {
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

