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

// Mid last game (best-of 5)
export const midLastGame = {
    bestOf : 5,
    resA : 2,
    resX : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [4,3], [0,0], [0,0]]    
};

// End last game (best-of 5)
export const endLastGame = {
    bestOf : 5,
    resA : 2,
    resX : 2,
    setHistory : [[11,3], [3,11], [11,3], [3,11], [10,3], [0,0], [0,0]]    
};


// Service
// Service left
export const serviceLeft = {
    service : CounterData.Service.LEFT,
    serviceLeft : true,
    serviceRight : false
    // TODO: Double
};

// Service right
export const serviceRight = {
    service : CounterData.Service.RIGHT,
    serviceLeft : false,
    serviceRight : true
    // TODO: Double
};


// Data
export var data = [
    basedata[0]  // empty
];


// -----------------------------------------------------------------------
export function testGameStarted(data) {
    return Counter.gameStarted(data);
}

export function testGameFinished(data) {
    return Counter.gameFinished(data);
}

export function testAddPointLeft(data) {
    Counter.addPointLeft(data);
    return data;
}

export function testAddPointRight(data) {
    Counter.addPointRight(data);
    return data;
}

export function testSubPointLeft(data) {
    Counter.addPointLeft(data);
    return data;
}

export function testSubPointRight(data) {
    Counter.addPointLeft(data);
    return data;
}

export function testSwapSides(data) {
    Counter.swapSides(data);
    return data;
}