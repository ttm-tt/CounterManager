/* Copyright (C) 2020 Christoph Theis */

import * as Counter from '../counter.js';
import * as CounterData from '../counter_data.js';

var basedata = CounterData.create();

// States
// In first game, service change required
export const midFirstGame = {
    setHistory : [[4,3], [0,0], [0,0], [0,0], [0,0], [0,0], [0,0]]
};

// End first game
export const endFirstGame = {
    setHistory : [[10,3], [0,0], [0,0], [0,0], [0,0], [0,0], [0,0]]    
};

// Finished first game
export const finishedFirstGame = {
    setHistory : [[11,3], [0,0], [0,0], [0,0], [0,0], [0,0], [0,0]],  
    sideChange : CounterData.SideChange.BEFORE
};

// Mid last game (best-of 5)
export const midLastGame = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [4,3], [0,0], [0,0]]    
};

// Mid last game (best-of 5) before side change
export const midLastGameBefore = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [5,3], [0,0], [0,0]],
    sideChange : CounterData.SideChange.BEFORE
};

// Mid last game (best-of 5)
export const midLastGameAfter = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [5,3], [0,0], [0,0]],
    sideChange : CounterData.SideChange.AFTER
};

// End last game (best-of 5)
export const endLastGame = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [10,3], [0,0], [0,0]]    
};

// End last game (best-of 5)
export const finishedLastGame = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [11,3], [0,0], [0,0]]    
};

export const finishedLastGameBefore = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [11,3], [0,0], [0,0]],
    sideChange: CounterData.SideChange.BEFORE
};


// Service
// Service left
export const serviceLeft = {
    service : CounterData.Service.A,
    serviceLeft : true,
    serviceRight : false,
    serviceDouble: CounterData.ServiceDouble.BX
};

export const serviceLeftAY = {
    service : CounterData.Service.A,
    serviceLeft : true,
    serviceRight : false,
    serviceDouble: CounterData.ServiceDouble.AY    
};

export const serviceLeftBX = {
    service : CounterData.Service.A,
    serviceLeft : true,
    serviceRight : false,
    serviceDouble: CounterData.ServiceDouble.BX  
};

export const serviceLeftBY = {
    service : CounterData.Service.A,
    serviceLeft : true,
    serviceRight : false,
    serviceDouble: CounterData.ServiceDouble.BY  
};

// Service right X -> B
export const serviceRight = {
    service : CounterData.Service.X,
    serviceLeft : false,
    serviceRight : true,
    serviceDouble: CounterData.ServiceDouble.XB
};

// Service double X -> A
export const serviceRightXA = {
    service : CounterData.Service.X,
    serviceLeft : false,
    serviceRight : true,
    serviceDouble: CounterData.ServiceDouble.XA
};

// Service double Y -> B
export const serviceRightYB = {
    service : CounterData.Service.X,
    serviceLeft : false,
    serviceRight : true,
    serviceDouble: CounterData.ServiceDouble.YB
};

// First service left
export const firstServiceLeft = {
    firstService : CounterData.Service.A,
    firstServiceDouble: CounterData.ServiceDouble.BX
};

// First service right
export const firstServiceRight = {
    firstService : CounterData.Service.X,
    serviceDouble: CounterData.ServiceDouble.XB
};

// Timeout left running
export const timeoutLeftRunning = {
    timeoutLeft : true,
    timeoutLeftRunning : true,
    gameMode : CounterData.GameMode.RUNNING,
    timeMode : CounterData.TimeMode.TIMEOUT
};

// Timeout right running
export const timeoutRightRunning = {
    timeoutRight : true,
    timeoutRightRunning : true,
    gameMode : CounterData.GameMode.RUNNING,
    timeMode : CounterData.TimeMode.TIMEOUT
};

// Yellow card left
export const yellowCardLeft = {
    cardLeft : CounterData.Cards.YELLOW
};

// YR1P card left
export const yr1pCardLeft = {
    cardLeft : CounterData.Cards.YR1P
};

// YR2P card left
export const yr2pCardLeft = {
    cardLeft : CounterData.Cards.YR2P
};

// Game status WARMUP + PREPARE
export const warmupPrepare = {
    gameMode : CounterData.GameMode.WARMUP,
    timeMode : CounterData.TimeMode.PREPARE
};

// Game status MATCH + RUNNING
export const runningMatch = {
    gameMode : CounterData.GameMode.RUNNING,
    timeMode : CounterData.TimeMode.MATCH
};


// Expedite left
export const expedite = {
    expedite : true
};


// Data
export const data = [
    // We need to store a copy
    Object.assign({}, basedata),       // normal
    Object.assign({}, basedata).swap() // Sides reverted
];


// -----------------------------------------------------------------------
// Test methods. If we change data make a copy before so we don't change test data
export function testMatchStarted(data) {
    return data.hasMatchStarted();
}

export function testMatchFinished(data) {
    return data.hasMatchFinished();
}

export function testGameStarted(data, idx) {
    return data.hasGameStarted(idx);
}

export function testGameFinished(data, idx) {
    return data.hasGameFinished(idx);
}

export function testAddPointLeft(data) {
    var d = deepCopy(data);
    Counter.addPointLeft(d);
    return d;
}

export function testAddPointRight(data) {
    var d = deepCopy(data);
    Counter.addPointRight(d);
    return d;
}

export function testSubPointLeft(data) {
    var d = deepCopy(data);
    Counter.subPointLeft(d);
    return d;
}

export function testSubPointRight(data) {
    var d = deepCopy(data);
    Counter.subPointRight(d);
    return d;
}

export function testSwapSides(data) {
    var d = deepCopy(data);
    Counter.swapSides(d);
    return d;
}

export function testToggleServiceLeft(data) {
    var d = deepCopy(data);
    Counter.toggleServiceLeft(d);
    return d;
}

export function testToggleServiceRight(data) {
    var d = deepCopy(data);
    Counter.toggleServiceRight(d);
    return d;
}

export function testToggleServiceDoubleLeft(data) {
    var d = deepCopy(data);
    Counter.toggleServiceDoubleLeft(d);
    return d;
}

export function testToggleServiceDoubleRight(data) {
    var d = deepCopy(data);
    Counter.toggleServiceDoubleRight(d);
    return d;
}

export function testToggleTimeoutLeft(data) {
    var d = deepCopy(data);
    Counter.toggleTimeoutLeft(d);
    return d;
}


export function testToggleYLeft(data) {    
    var d = deepCopy(data);
    Counter.toggleYLeft(d);
    return d;
}


export function testToggleYR1PLeft(data) {    
    var d = deepCopy(data);
    Counter.toggleYR1PLeft(d);
    return d;
}


export function testToggleYR2PLeft(data) {    
    var d = deepCopy(data);
    Counter.toggleYR2PLeft(d);
    return d;
}


export function testToggleStartGame(data) {
    var d = deepCopy(data);
    Counter.toggleStartGame(d);
    return d;
}


export function testEndMatch(data) {
    var d = deepCopy(data);
    Counter.endMatch(d);
    return d;
}


export function testToggleExpedite(data) {
    var d = deepCopy(data);
    Counter.toggleExpedite(d);
    return d;
}


export function testWOLeft(data) {
    var d = deepCopy(data);
    Counter.setWOLeft(d);
    return d;
}


export function testWORight(data) {
    var d = deepCopy(data);
    Counter.setWORight(d);
    return d;
}


// Fake a deep copy:
function deepCopy(data) {
    // Object.assign is a shallow copy and thus changes in setHistory will
    // affect the original data. On the other hand JSON will remove the functins.
    // So we do a shallow clone and then overwrite the data with JSON
    return Object.assign({}, data, JSON.parse(JSON.stringify(data)));
}