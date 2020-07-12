/* Copyright (C) 2020 Christoph Theis */
/**
 * Base logic to maintain a list of upcoming matches, groups by table.
 * It is up to the implementation to request and update the data
 */


// Enable debug
var debug = false;

/**
 * Set debug flag
 * @param b value of debug
 */
export function setDebug(b) {
    debug = b;
}

/**
 * Rebuild matches with new data with removing finished matches
 * Finished and expired matches are discarded and new matches added to the list
 * @param matches the current list of matches
 * @param data list of matches from the server
 * @param ct current time
 * @param minTime minimum time a finished match should be shown
 * @param prestart time before schedule from when a match may be shown
 */
export function rebuild(matches, data, ct = ((new Date()).getTime()), minTime = 60, prestart = 3600) {
    sortData(data);

    initialize(matches);
    removeFinished(matches, data, ct, minTime);
    updateUnfinished(matches, data);
    finalize(matches, data, ct, prestart);

    updateMtTimestamp(data);
}

/**
 * Update matches with new data. but don't remove finished
 * Finished and expired matches are discarded and new matches added to the list
 * @param matches the current list of matches
 * @param {type} data
 */
export function update(matches, data) {
    sortData(data);
    
    // Only first match per table is updated
    updateResult(matches, data);

    updateMtTimestamp(data);
}


/**
 * Sort data received from server
 * @param {type} data
 * @returns data
 */
export function sortData(data) {
    if (!data)
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
 * @param {type} data
 * @param {type} mtTimestamp
 * @returns updated mtTimestamp
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
 * @param matches the current list of matches
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
 * @param matches the current list of matches
 */
export function clearResult(matches) {
    for (const i in matches) {
        if (isFinished(matches[i]))
            matches[i].mtResult = null;
    }
}


/**
 * Remove finished matches from matches no longer displayed
 * @param matches the current list of matches
 * @param data
 * @param ct current time
 * @param minTime the minimum time a finished match schal be shown
 */
export function removeFinished(matches, data, ct, minTime) {
    for (const i in matches) {
        if (isFinished(matches[i]) && matches[i].mtTimestamp < (ct - minTime * 1000)) {
            if (debug)
                console.log("Remove inished: remove nr " + matches[i].mtNr);
            
            matches[i] = null;
        } else {
            if (debug) {
                console.log(
                        "Remove finished: keep nr " + matches[i].mtNr + 
                        " finished = " + isFinished(matches[i]) + "," + 
                        " timestamp = " + matches[i].mtTimestamp + "," +
                        " ct - minTime = " + (ct - minTime * 1000)
                );
            }
        }
    }
}


/**
 * Update all unfinished matches with new result
 * @param matches the current list of matches
 * @param {type} data
 * @returns {undefined}
 */
export function updateUnfinished(matches, data) {
    for (const i in data) {
        const mtTable = data[i].mtTable;
        
        if (matches.length <= mtTable)
            matches[mtTable] = null;
        
        const match = matches[mtTable];
        
        if (matches[mtTable] === null) {
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
 * @param matches the current list of matches
 * @param data
 * @param ct current time
 * @param prestart time before the schedule a match may be shown
 */
export function finalize(matches, data, ct, prestart) {

    for (const i in matches) {
        matches[i] = [matches[i]];
    }

    for (const i in data) {
        const mtTable = data[i].mtTable;
        
        // Match is too far in the future and should not be shown yet
        if (data[i].mtDateTime > ct + (prestart * 1000))
            continue;
        
        // No more matches, but safety condition
        if (matches[mtTable] === null || matches[mtTable][0] === null)
            continue;

        // Same (team) match
        if (matches[mtTable][0].mtNr === data[i].mtNr && matches[mtTable][0].mtMS === data[i].mtMS)
            continue;

        const length = matches[mtTable].length;

        // Only one next team match of current match
        if (length > 1 && matches[mtTable][length - 1].mtNr === data[i].mtNr)
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
 * @param matches the current list of matches
 * @param {type} data
 */
export function updateResult(matches, data) {
    if (!data)
        return;

    for (const i in data) {
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
 * @param mt the match
 * @return true if the match has finished
 */
export function isFinished(mt) {
    if (mt === undefined || mt === null)
        return false;

    if (mt.mtMatches > 1 && (2 * mt.mttmResA > mt.mtMatches || 2 * mt.mttmResX > mt.mtMatches))
        return true;

    if (mt.mtWalkOverA || mt.mtWalkOverX) {
        if (debug)
            console.log('Finished:  nr = ' + mt.mtNr + ', woA = ' + mt.mtWalkOverA + ', woX = ' + mt.mtWalkOverX);
        return true;
    }

    if (2 * mt.mtResA > mt.mtBestOf || 2 * mt.mtResX > mt.mtBestOf) {
        if (debug)
            console.log('Finished: nr = ' + mt.mtNr + ', resA = ' + mt.mtResA + ', resX = ' + mt.mtResX + ', bestOf = ' + mt.mtBestOf);
        return true;
    }

    return false;
}


/**
 * Check if a match has started
 * @param mt the match
 * @param ct the currenttime
 * @returns true if the match has started
 */
export function isStarted(mt, ct = new Date().getTime()) {
    if (mt === undefined || mt === null)
        return false;

    // Any finished match must have started
    if (isFinished(mt))
        return true;

    if (mt.mtResult !== undefined && mt.mtResult.length > 0 && (mt.mtResult[0][0] > 0 || mt.mtResult[0][1] > 0))
        return true;

    if (mt.cpType == 4) {
        if ((mt.mtDateTime > ct && mt.mtResA === 0 && mt.mtResX === 0) ||
                (mt.plAplNr === undefined || mt.plAplNr === 0 || mt.plXplNr === undefined || mt.plXplNr === 0)) {
            return false;
        }
    } else {
        if ((mt.mtDateTime > ct && mt.mtResA === 0 && mt.mtResX === 0) ||
                (mt.plAplNr === undefined || mt.plAplNr === 0 || mt.plXplNr === undefined || mt.plXplNr === 0)) {
            return false;
        }
    }

    return true;
}
