/* Copyright (C) 2020 Christoph Theis */
/**
 * Base logic to maintain a list of upcoming matches, grouped by table.
 * It is up to the implementation to request and update the data
 */

// Options
var config = {
    minTime: 60,   // [s] minimum time a finished match must be schown
    prestart: 3600, // [s] time before start a match is schown
    includeAllTeamMatches: false  // Flag if all team matches are included
};

// Enable debug
var debug = false;

/**
 * Set config
 * @param {Object} cfg
 */
export function setConfig(cfg) {
    config = Object.assign({}, config, cfg);
}

/**
 * Set debug flag
 * @param {boolean} b value of debug
 */
export function setDebug(b) {
    debug = b;
}

/**
 * Rebuild matches with new data with removing finished matches
 * Finished and expired matches are discarded and new matches added to the list
 * @param {Array} matches the current list of matches
 * @param {Array} data list of matches from the server
 * @param {Date} ct current time
 */
export function rebuild(matches, data, ct = ((new Date()).getTime())) {
    sortData(data);

    initialize(matches);
    removeNotStarted(matches);
    removeFinished(matches, data, ct);
    updateUnfinished(matches, data, ct);
    finalize(matches, data, ct);

    updateMtTimestamp(data);
}

/**
 * Update matches with new data. but don't remove finished
 * Finished and expired matches are discarded and new matches added to the list
 * @param {Array} matches the current list of matches
 * @param {Array} data
 */
export function update(matches, data) {
    sortData(data);
    
    // Only first match per table is updated
    updateResult(matches, data);

    updateMtTimestamp(data);
}


/**
 * Sort data received from server
 * @param {Array} data
 * @returns {Array}
 */
export function sortData(data) {
    if (!data || !Array.isArray(data))
        return;

    // Sort array by table, date / time, nr and team match
    data.sort(function (a, b) {
        var res = a.mtTable - b.mtTable;
        if (!res)
            res = a.mtDateTime - b.mtDateTime;
        if (!res)
            res = a.mtNr - b.mtNr;
        if (!res)
            res = a.mtMS - b.mtMS;

        return res;
    });

    return data;
}


/**
 * Get the maximum mtTimestamp of new data and current one
 * @param {Object} data
 * @param {number} mtTimestamp
 * @returns {number} updated mtTimestamp
 */
export function updateMtTimestamp(data, mtTimestamp) {
    if (!data)
        return mtTimestamp;
    for (const i in data) {
        if (data[i].mtTimestamp > mtTimestamp)
            mtTimestamp = data[i].mtTimestamp;
    }
    
    return mtTimestamp;
}


/**
 * Initializes matches structure: each entry is just a plain match
 * @param {Array} matches the current list of matches
 */
export function initialize(matches) {
    // Iterate over nun-numeric arrays with for..in
    for (const i in matches) {
        if (Array.isArray(matches[i]))
            matches[i] = matches[i][0];
    }
}


/**
 * Clear result from all finished matches so we don't show them again
 * @param {Array} matches the current list of matches
 */
export function clearResult(matches) {
    for (const i in matches) {
        if (isFinished(matches[i]))
            matches[i].mtResult = null;
    }
}


/**
 * Remove finished matches from list so they are no longer displayed
 * @param {Array} matches the current list of matches
 * @param {Object} data
 * @param {Date} ct current time
 */
export function removeFinished(matches, data, ct) {
    for (const i in matches) {
        if (isFinished(matches[i]))  {
            if (matches[i].mtTimestamp < (ct - config.minTime * 1000)) {
                if (debug)
                    console.log("Remove finished on table " + i + ": remove nr " + matches[i].mtNr);

                matches[i] = null;
            } else {
                if (debug) {
                    console.log(
                            "Remove finished on table " + i + ": keep nr " + matches[i].mtNr + 
                            " finished = " + isFinished(matches[i]) + "," + 
                            " timestamp = " + matches[i].mtTimestamp + "," +
                            " ct - minTime = " + (ct - config.minTime * 1000)
                    );
                }
            }
        } else if (matches[i] !== null) {
            if (debug) {
                console.log(
                    "Remove finished on table " + i + ": keep unfinished nr " + matches[i].mtNr
                );
            }            
        } else {
            if (debug) {
                console.log(
                    "Match on table " + i + " is null"
                );   
            }
        }
    }
}


/**
 * Remove not started matches from list
 * @param {Array} matches the current list of matches
 */
export function removeNotStarted(matches) {
    for (const i in matches) {
        if (!isStarted(matches[i])) {
            if (debug)
                console.log("Remove not started: remove nr " + matches[i].mtNr);
            
            matches[i] = null;
        }
    }
}


/**
 * Update all unfinished matches with new result
 * @param {Array} matches the current list of matches
 * @param {Object} data list of updates
 * @param {Integer} ct current time
 */
export function updateUnfinished(matches, data, ct) {
    for (const i in data) {
        if (data[i] === null || data[i] === undefined)
            continue;
        
        // Match is too far in the future and should not be shown yet
        if (data[i].mtDateTime > ct + (config.prestart * 1000))
            continue;
        
        const mtTable = data[i].mtTable;
        
        if (matches.length <= mtTable)
            matches[mtTable] = null;
        
        if (matches[mtTable] === undefined)
            matches[mtTable] = null;
        
        const match = matches[mtTable];
        
        if (match === null) {
            if (!isFinished(data[i])) {
                if (debug)
                    console.log("Update unfinished: insert item " + i + " (" + data[i].mtNr + ")");
                matches[mtTable] = data[i];
            } else {
                if (debug)
                    console.log("Update unfinished: skip item " + i + " (" + data[i].mtNr + ")");
            } 
        } else if (!isFinished(match) && match.mtNr === data[i].mtNr && match.mtMS === data[i].mtMS) {
            matches[mtTable] = data[i];
            if (debug)
                console.log("Update unfinished: update item " + i + "(" + data[i].mtNr + ")");
        } else {
            if (debug)
                console.log("Update unfinished: skip table " + data[i].mtTable);                
        }
    }
}


/**
 * Finalize matches list: 
 * each entry is a list of current and upcoming matches per table
 * @param {Array} matches the current list of matches
 * @param {Object} data
 * @param {Date} ct current time
 */
export function finalize(matches, data, ct) {

    for (const i in matches) {
        matches[i] = [matches[i]];
    }

    for (const i in data) {
        const mtTable = data[i].mtTable;
        
        // Match is too far in the future and should not be shown yet
        if (data[i].mtDateTime > ct + (config.prestart * 1000))
            continue;
        
        // No more matches, but safety condition
        if (matches[mtTable] === null || matches[mtTable][0] === null)
            continue;

        // Same (team) match
        if (matches[mtTable][0].mtNr === data[i].mtNr && matches[mtTable][0].mtMS === data[i].mtMS)
            continue;

        const length = matches[mtTable].length;

        // Only one next team match of current match
        if (!config.includeAllTeamMatches && length > 1 && matches[mtTable][length - 1].mtNr === data[i].mtNr)
            continue;

        // A previous match (which means data[i] is finished)
        if (matches[mtTable][0].mtDateTime > data[i].mtDateTime)
            continue;

        // Last match shown on this table is alrady in the future
        // Don't show more matches in the future there is only one "Next on table"
        if (matches[mtTable][length - 1].mtDateTime > ct)
            continue;
        
        // Next match on table
        matches[data[i].mtTable].push(data[i]);
    }
}


/**
 * Update results of first match per table, but don't change matches
 * @param {Array} matches the current list of matches
 * @param {Object} data
 */
export function updateResult(matches, data) {
    if (!data)
        return;

    for (const i in data) {
        if (matches[data[i].mtTable] === undefined)
            continue;
        
        if (matches[data[i].mtTable] !== null && matches[data[i].mtTable][0] !== null) {
            // We check the first match per table only, the other ones can't have started    
            // No resceduled matches, only updates of results
            if (matches[data[i].mtTable][0].mtNr == data[i].mtNr && matches[data[i].mtTable][0].mtMS == data[i].mtMS)
                matches[data[i].mtTable][0] = data[i];
        }
    }
}


/**
 * Check if a match has finished
 * @param {Object} mt the match
 * @return {boolean} true if the match has finished
 */
export function isFinished(mt) {
    if (mt === undefined || mt === null)
        return false;
    
    if (mt.syComplete && (mt.grModus = 1)) {
        if (( mt.mttmResA + mt.mttmResX) == mt.mtMatches)
            return true;
    } else {
        if (mt.mtMatches > 1 && ((2 * mt.mttmResA > mt.mtMatches) || (2 * mt.mttmResX > mt.mtMatches)))
            return true;
    }
        
    if (mt.mtWalkOverA || mt.mtWalkOverX) {
        if (debug)
            console.log('Finished:  nr = ' + mt.mtNr + ', woA = ' + mt.mtWalkOverA + ', woX = ' + mt.mtWalkOverX);
        return true;
    }

    if ((2 * mt.mtResA > mt.mtBestOf) || (2 * mt.mtResX > mt.mtBestOf)) {
        if (debug)
            console.log('Finished: nr = ' + mt.mtNr + ', resA = ' + mt.mtResA + ', resX = ' + mt.mtResX + ', bestOf = ' + mt.mtBestOf);
        return true;
    }

    return false;
}


/**
 * Check if a match has started
 * @param {Object} mt the match
 * @param {Date} ct the currenttime
 * @returns {boolean} true if the match has started
 */
export function isStarted(mt, ct = new Date().getTime()) {
    if (mt === undefined || mt === null)
        return false;

    // Any finished match must have started
    if (isFinished(mt))
        return true;

    if (mt.mtResult !== undefined && mt.mtResult.length > 0 && (mt.mtResult[0][0] > 0 || mt.mtResult[0][1] > 0))
        return true;

    if (mt.mtGameRunning)
        return true;

    if (mt.cpType == 4) {
        if ((mt.mtResA === 0 && mt.mtResX === 0) ||
                (mt.plAplNr === undefined || mt.plAplNr === 0 || mt.plXplNr === undefined || mt.plXplNr === 0)) {
            return false;
        }
    } else {
        if ((mt.mtResA === 0 && mt.mtResX === 0) ||
                (mt.plAplNr === undefined || mt.plAplNr === 0 || mt.plXplNr === undefined || mt.plXplNr === 0)) {
            return false;
        }
    }
    
    return true;
}
