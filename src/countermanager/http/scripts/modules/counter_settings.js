/* Copyright (C) 2020 Christoph Theis */

/*
 * Definition of counter settings
 */

const CounterSettings = {
    pointsToPlay : 11,            // All games until
    pointsToPlayLastGame : 11,    // Last game until
    leadToWin : 2,                // Points to be ahead to win
    leadToWinLastGame : 2,        // Points to be ahead to win last game
    serviceChange : 2,            // Change service after that many points
    serviceChangeLastGame : 2,    // Change service after that many points in last game
    sideChange : true,            // Change sides between games
    sideChangeLastGame : 5        // Change sides in last game after that many points
};

export default CounterSettings;


