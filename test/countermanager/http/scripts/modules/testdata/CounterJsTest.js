/* Copyright (C) 2020 Christoph Theis */

import * as Counter from '../counter.js';
import * as CounterData from '../counter_data.js';

var basedata = [
    CounterData.create()
];

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
    setHistory : [[11,3], [0,0], [0,0], [0,0], [0,0], [0,0], [0,0]]    
};

// Mid last game (best-of 5)
export const midLastGame = {
    bestOf : 5,
    setsLeft : 2,
    setsRight : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [4,3], [0,0], [0,0]]    
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


// Service
// Service left
export const serviceLeft = {
    service : CounterData.Service.LEFT,
    serviceLeft : true,
    serviceRight : false,
    serviceDouble: CounterData.ServiceDouble.BX
};

// Service right
export const serviceRight = {
    service : CounterData.Service.RIGHT,
    serviceLeft : false,
    serviceRight : true,
    serviceDouble: CounterData.ServiceDouble.XB
};

// First service left
export const firstServiceLeft = {
    firstService : CounterData.Service.LEFT,
    serviceDouble: CounterData.ServiceDouble.BX
};

// First service right
export const firstServiceRight = {
    firstService : CounterData.Service.RIGHT,
    serviceDouble: CounterData.ServiceDouble.XB
};


// Data
export const data = [
    basedata[0]  // empty
];


// -----------------------------------------------------------------------
// Test methods. If we change data make a copy before so we don't change test data
export function testGameStarted(data, idx) {
    return data.gameStarted(idx);
}

export function testGameFinished(data, idx) {
    return data.gameFinished(idx);
}

export function testAddPointLeft(data) {
    var d = Object.assign({}, data);
    Counter.addPointLeft(d);
    return d;
}

export function testAddPointRight(data) {
    var d = Object.assign({}, data);
    Counter.addPointRight(d);
    return d;
}

export function testSubPointLeft(data) {
    var d = Object.assign({}, data);
    Counter.subPointLeft(d);
    return d;
}

export function testSubPointRight(data) {
    var d = Object.assign({}, data);
    Counter.subPointRight(d);
    return d;
}

export function testSwapSides(data) {
    var d = Object.assign({}, data);
    Counter.swapSides(d);
    return d;
}

export function testToggleServiceLeft(data) {
    var d = Object.assign({}, data);
    Counter.toggleServiceLeft(d);
    return d;
}

export function testToggleServiceRight(data) {
    var d = Object.assign({}, data);
    Counter.toggleServiceRight(d);
    return d;
}

export function testToggleServiceDoubleLeft(data) {
    var d = Object.assign({}, data);
    Counter.toggleServiceDoubleLeft(d);
    return d;
}

export function testToggleServiceDoubleRight(data) {
    var d = Object.assign({}, data);
    Counter.toggleServiceDoubleRight(d);
    return d;
}
