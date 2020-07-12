/* Copyright (C) 2020 Christoph Theis */

/**
 * Contains test data for current_matches.js
 */

/* global Matches */

export var testct = 0; 

// Initial data: 2 matches, one overdue, one in the future
var basedata = [
    {
        mtNr : 1,
        mtMS : 0,
        mtDateTime : testct,  // test current time
        mtTable : 1,
        mtMatches : 1,
        mtBestOf : 5,
        mtResA : 0,
        mtResX : 0,
        mtResult : [[0, 0], [0, 0], [0,0], [0, 0], [0, 0]],
        plAplNr : 1,
        plApsLast : 'Player',
        plApsFirst : 'A',
        plXpsLast : 'Player',
        plXpsFirst : 'X',
        mtTimestamp : testct - 10 * 60 * 1000
    },
    {
        mtNr : 2,
        mtMS : 0,
        mtDateTime : testct + 10 * 60 * 1000, // 10 minutes later
        mtTable : 1,
        mtMatches : 1,
        mtBestOf : 5,
        mtResA : 0,
        mtResX : 0,
        mtResult : [[0, 0], [0, 0], [0,0], [0, 0], [0, 0]],
        plAplNr : 1,
        plApsLast : 'Player',
        plApsFirst : 'A',
        plXpsLast : 'Player',
        plXpsFirst : 'X',
        mtTimestamp : testct - (10 - 1) * 60 * 1000
    },
    {
        mtNr : 3,
        mtMS : 0,
        mtDateTime : testct + 20 * 60 * 1000, // 20 minutes later
        mtTable : 1,
        mtMatches : 1,
        mtBestOf : 5,
        mtResA : 0,
        mtResX : 0,
        mtResult : [[0, 0], [0, 0], [0,0], [0, 0], [0, 0]],
        plAplNr : 1,
        plApsLast : 'Player',
        plApsFirst : 'A',
        plXpsLast : 'Player',
        plXpsFirst : 'X',
        mtTimestamp : testct - (10 - 2) * 60 * 1000
    } ,   
    {
        mtNr : 4,
        mtMS : 0,
        mtDateTime : testct + 120 * 60 * 1000, // > prestart minutes later
        mtTable : 1,
        mtMatches : 1,
        mtBestOf : 5,
        mtResA : 0,
        mtResX : 0,
        mtResult : [[0, 0], [0, 0], [0,0], [0, 0], [0, 0]],
        plAplNr : 1,
        plApsLast : 'Player',
        plApsFirst : 'A',
        plXpsLast : 'Player',
        plXpsFirst : 'X',
        mtTimestamp : testct - (10 - 3) * 60 * 1000
    }    
];

export var data = [
    [
        // Initial data
        basedata[0],
        basedata[1],
        basedata[2],
        basedata[3]
    ],
    [
        // Update match one
        Object.assign({}, basedata[0], {
                mtTimestamp : testct,
                mtResult : [[1, 0], [0, 0], [0,0], [0, 0], [0, 0]]
            }
        ),
        basedata[1],
        basedata[2],
        basedata[3]
    ],
    [
        // First match is finished
        Object.assign({}, basedata[0], {
                mtTimestamp : testct + 10 * 1000,
                mtResA : 3,
                mtResX : 0,
                mtResult : [[11, 0], [11, 0], [11,0], [0, 0], [0, 0]]
            }
        ),
        basedata[1],
        basedata[2],
        basedata[3]
    ]
];


// -----------------------------------------------------------------------------
// Test methods

/*
 * Test isFinished, isStarted
 */
export function testIsStarted(mt, ct) {
    return Matches.isFinished(mt, ct);
}

export function testIsFinished(mt) {
    return Matches.isFinished(mt);
}

/*
 * Empty initiaizer
 */
export function testEmptyInitialize() {
    Matches.initialize();
    return Matches.matches;
}

/*
 * Initialize with start data
 */
export function testInitialize() {
    Matches.initialize();
    return Matches.matches;
}


export function testRemoveFinished(data, ct) {
    Matches.removeFinished(data, ct);
    return Matches.matches;
}


export function testUpdateUnfinished(data) {
    Matches.updateUnfinished(data);
    return Matches.matches;
}

export function testFinalize(data, ct) {
    Matches.finalize(data, ct);
    return Matches.matches;
}

export function testRebuild(data, ct) {
    Matches.rebuild(data, ct);
    return Matches.matches;
}

export function testUpdate(data, ct) {
    Matches.update(data, ct);
    return Matches.matches;
}