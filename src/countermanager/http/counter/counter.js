/* Copyright (C) 2020 Christoph Theis */

var counterMatch = null;
var counterData = null;
var timer = null;
var lastUpdateTime = 0;
var lastSentTime = 0;
var table = 1;
var debug = 0;
var prestart = 300;


/*
 * Parameters 
 *      debug       default 0   Enable debug fields
 *      showTimer   default 0   Show the timer
 *      table       default 1   Select table ("auto" will let the server decide which one)
 *      noCards     default 0   Hide the yellow / red cards selection
 *      prestart    default 300 When a lock screen must be removed
 */


// Service / return
// firstService: -1: left, +1: right, 0: undefined
var firstService = 0;

/*
    A   X
    B   Y
    
    Left serves first   Right serves first
    B -> X  1           B <- X  1
    A <- X  2           B -> Y  4
    A -> Y  3           A <- Y  3
    B <- Y  4           A -> X  2

    0: Do nothing
    1: swap none
    2: swap left
    3: swap both
    4: swap right
 */

var firstServiceDouble = 0;

// formatted names of players
var namePlA = null;
var namePlB = null;
var namePlX = null;
var namePlY = null;

var CmdEnum = {
        NONE : 0,                // 0
        GET_DATA : 1,            // 1
        GET_DATA_BROADCAST : 2,  // 2
        SET_DATA : 3,            // 3
        SWAP_PLAYERS : 4,        // 4
        RESET_ALERT : 5,         // 5
        RESET : 6,               // 6
        AUTO_TABLE : 7,          // 7    
        SET_RESULT : 8,          // 8
        LOCK_SCREEN : 9,         // 9
        UNLOCK_SCREEN : 10       // 10
};

$(document).ready(function() {
    debug = getParameterByName('debug', 0);
    
    if (debug)
        $('#debug').show();
    
    prestart = parseInt(getParameterByName('prestart', '' + prestart));
    
    showTimer = getParameterByName('showTimer', 0);
    if (showTimer == 0)
        $('div#timer').addClass('hidden');
    
    table = getParameterByName('table', '');
    
    if (table === '') {
        // If no table is specified then use 'auto' to avoid an automatic redirect to table 1
        if (window.location.search === '')
            window.location.replace(window.location.href + '?table=auto');
        else
            window.location.replace(window.location.href + '&table=auto');
        
        return;
    }
    
    if (table === 'auto') {
        $.ajax({
            url: '../counter/command',
            type : 'POST',
            data : JSON.stringify({table: 0, command: CmdEnum.AUTO_TABLE, body: null}),
            dataType: 'json',
            success: function(data) {
                if (data <= 0) {
                    alert('All connections are busy, please try again later');
                    return;
                }
                
                window.location.replace(window.location.href.replace('table=auto', 'table=' + data));
                return;
            }
        }); 
        
        return;
    }
    else {
        doInitialize();
    }
    
});

function doInitialize() {
    initializeData();
    initializeForm();
    
    if (getParameterByName('noCards', 0) == 1)
        $('tr.cards').hide();
    
    $('#names .name.left, #names .name.right')
        .css('cursor', 'pointer')
        .click(function() {
            swapDouble($(this));
        })
    ;

    restoreData();
    
    connectHttp();    
}


function connectHttp() {
    var timeout = 150; // 150ms, slightly larger than the timeout of the ajax call    
    var ct = (new Date()).getTime();
    
    if (lastUpdateTime > lastSentTime || lastSentTime < ct - 2000) {
        // Something changed or periodical updates
        send(CmdEnum.GET_DATA, JSON.stringify(counterData));
    }

    if (lastUpdateTime < ct - 2000) {
        // Periodical update lastStorage if nothing has changed
        storeData();
    }

    // If an error occured (lastSentTime set to 0( delay the next call
    if (lastSentTime === 0)
        timeout = 500;
    
    setTimeout(function() {connectHttp();}, timeout);
}

// ================================================================
// Functions from the form

// Swap players
function swapSides() {
    var i = 0;
    
    // Swap values of counterData
    var tmp = counterData.setsLeft;
    counterData.setsLeft = counterData.setsRight;
    counterData.setsRight = tmp;

    for (i = 0; i < counterData.setHistory.length; i++) {
        tmp = counterData.setHistory[i][0];
        counterData.setHistory[i][0] = counterData.setHistory[i][1];
        counterData.setHistory[i][1] = tmp;
    }
    
    tmp = counterData.serviceLeft;
    counterData.serviceLeft = counterData.serviceRight;
    counterData.serviceRight = tmp;
    
    tmp = counterData.timeoutLeft;
    counterData.timeoutLeft = counterData.timeoutRight;
    counterData.timeoutRight = tmp;       

    tmp = counterData.timeoutLeftRunning;
    counterData.timeoutLeftRunning = counterData.timeoutRightRunning;
    counterData.timeoutRightRunning = tmp;
    
    tmp = counterData.injuredLeft;
    counterData.injuredLeft = counterData.injuredRight;
    counterData.injuredRight = tmp;  
    
    tmp = counterData.injuredLeftRunning;
    counterData.injuredLeftRunning = counterData.injuredRightRunning;
    counterData.injuredRightRunning = tmp;
    
    tmp = counterData.cardLeft;
    counterData.cardLeft = counterData.cardRight;
    counterData.cardRight = tmp;
    
    swapState($('#serviceleft'), $('#serviceright'));
    swapState($('#timeoutleft'), $('#timeoutright'));
    swapState($('#injuredleft'), $('#injuredright'));
    
    swapState($('#yellowleft'), $('#yellowright'));
    swapState($('#yr1pleft'), $('#yr1pright'));
    swapState($('#yr2pleft'), $('#yr2pright'));
    
    for (i = 1; i <= 7; i++) {
        var resA = parseInt($('#game' + i + ' .points.left').html());
        var resX = parseInt($('#game' + i + ' .points.right').html());

        $('#game' + i + ' .points.left').html(resX);
        $('#game' + i + ' .points.right').html(resA);
        
        if (i == 1) {
        } else if ( i == (counterData.setsLeft + counterData.setsRight + 1) && 
                    resA == 0 && resX == 0 && i <= counterData.bestOf &&
                    2 * counterData.setsLeft < counterData.bestOf &&
                    2 * counterData.setsRight < counterData.bestOf ) {
            // XXX But only of "startGame" is not selected or we are going forward
            // Game is finished. 
            // Toggle inactive of this and the previous one. 
            // disable "+" of the previous game
            // But test, why we are doing this: only when we reached the 5:x,
            $('#game' + (i-1) + ' .action .plus').addClass('disabled');
            $('#game' + (i-1)).toggleClass('inactive');
            $('#game' + i).toggleClass('inactive'); 
            
            $('#startGame').toggleClass('disabled');
        } else if ( i == counterData.bestOf && 
                    (resA == 5 && resX < 5 || resX == 5 && resA < 5) ) {
            // Change sides in the last possible game at 5:x
            // This game can either only go backwards (before the change sides) 
            // or only go forwards (after change sides)
            // Only with the next point can we go on
            // Hide "Start Game" before we go on, show again after change side
            // But only if only either "+" or "-" is shown
            if ( $('#game' + i + ' .action.left .plus').hasClass('disabled') ^ $('#game' + i + ' .action.left .minus').hasClass('disabled') ) {
                $('#game' + i + ' .action .plus').toggleClass('disabled');
                $('#game' + i + ' .action .minus').toggleClass('disabled');
                $('#startGame').toggleClass('disabled');
            }
            
            // And the receiver changes, which means swap the players on the 
            // side which have not the service
            if (counterData.serviceLeft)
                swapDouble($('#names .name.right'));
            if (counterData.serviceRight)
                swapDouble($('#names .name.left'));
            
            // Hard coded fix
            if (true) {
                switch (counterData.serviceDouble) {
                    case 4:
                        counterData.serviceDouble = 2;
                        break;
                        
                    case 2 :
                        counterData.serviceDouble = 4;
                        break;                                
                }
            }
        }            
    }
    
    if (counterData.gameMode == 'RESET') {
        counterData.gameMode = 'WARMUP';
        startPrepareTimer();        
    }
    
    swapPlayers();
    
    calculateData();
    
    storeData();
}

function swapPlayers() {
    // Swap the names only
    var tmp = counterData.playerNrLeft;
    counterData.playerNrLeft = counterData.playerNrRight;
    counterData.playerNrRight = tmp;
    
    tmp = namePlA;
    namePlA = namePlX;
    namePlX = tmp;

    tmp = namePlB;
    namePlB = namePlY;
    namePlY = tmp;
    
    // Swap display values
    
    if (counterMatch && counterMatch.cpType == 4) {
        if (counterData.playerNrLeft == counterMatch.plA.plNr || counterData.playerNrLeft == 0xFFFF) {
            $('#teamleft').html(counterMatch.tmA.tmDesc);
            $('#teamright').html(counterMatch.tmX.tmDesc);
            $('#teamresult').html(counterMatch.mttmResA + '&nbsp;-&nbsp;' + counterMatch.mttmResX);
        } else if (counterData.playerNrLeft == counterMatch.plX.plNr || counterData.playerNrLeft == 0xFFFE) {
            $('#teamleft').html(counterMatch.tmX.tmDesc);
            $('#teamright').html(counterMatch.tmA.tmDesc);
            $('#teamresult').html(counterMatch.mttmResX + '&nbsp;-&nbsp;' + counterMatch.mttmResA);            
        }
    }
    
    showPlayers();
    
    if (counterData.timeMode != 'BREAK')
        storeData();
}


// Swap players in double (who is on the right on their side)
function swapDouble(field) {
    if (counterData === null)
        return;
    
    if (namePlB === null || namePlY === null)
        return;
    
    if (firstService === 0)
        return;
    
    var hasService =
        field.hasClass('left') && counterData.serviceLeft || 
        field.hasClass('right') && counterData.serviceRight
    ;
    
    // XXX Check if game is running: if yes, swap only one side
    if (hasService) {
            switch (firstServiceDouble) {
                case 1 : // B -> X => A -> Y
                    firstServiceDouble = 3;
                    break;
                    
                case 2 : // A -> X => B -> Y
                    firstServiceDouble = 4;
                    break;
                    
                case 3 : // A -> Y => B -> X
                    firstServiceDouble = 1;
                    break;
                    
                case 4 : // B -> Y => A -> X
                    firstServiceDouble = 2;
                    break;
            }
            
            switch (counterData.serviceDouble) {
                case 1 :
                    counterData.serviceDouble = 3;
                    break;
                    
                case 2 :
                    counterData.serviceDouble = 4;
                    break;
                    
                case 3 :
                    counterData.serviceDouble = 1;
                    break;
                    
                case 4 :
                    counterData.serviceDouble = 2;
                    break;
            }
    } else {
        if (counterData.serviceLeft) {
            switch (firstServiceDouble) {
                case 1 : // B -> X => B -> Y
                    firstServiceDouble = 4;
                    break;
                    
                case 2 : // A -> X => A -> Y
                    firstServiceDouble = 3;
                    break;
                    
                case 3 : // A -> Y => A -> X
                    firstServiceDouble = 2;
                    break;
                    
                case 4 : // B -> Y => B -> X
                    firstServiceDouble = 1;
                    break;
            }
            
            switch (counterData.serviceDouble) {
                case 1 : 
                    counterData.serviceDouble = 4;
                    break;
                    
                case 2 :
                    counterData.serviceDouble = 3;
                    break;
                    
                case 3 :
                    counterData.serviceDouble = 2;
                    break;
                    
                case 4 :
                    counterData.serviceDouble = 1;
                    break;
            }
        } else if (counterData.serviceRight) {
            switch (firstServiceDouble) {
                case 1 : // B <- X => A <- X
                    firstServiceDouble = 2;
                    break;
                    
                case 2 : // A <- X => B <- X
                    firstServiceDouble = 1;
                    break;
                    
                case 3 : // A <- Y => B <- Y
                    firstServiceDouble = 4;
                    break;
                    
                case 4 : // B <- Y => A <- Y
                    firstServiceDouble = 3;
                    break;
            }
            
            switch (counterData.serviceDouble) {
                case 1 : 
                    counterData.serviceDouble = 2;
                    break;
                    
                case 2 :
                    counterData.serviceDouble = 1;
                    break;
                    
                case 3 :
                    counterData.serviceDouble = 4;
                    break;
                    
                case 4 :
                    counterData.serviceDouble = 3;
                    break;
            }
        }
    }
    
    showPlayers();
}

// increment points
function increment(button) {
    var td = button.parent();
    var resA = parseInt(td.parent().find('.points.left').html());
    var resX = parseInt(td.parent().find('.points.right').html());

    // Check for 'overflow'
    if (resA >= 11 && resA >= resX + 2 || resX >= 11 && resX >= resA + 2) {
        return;
    }
    
    if (resA == 0 && resX == 0)
        counterData.serviceDouble = firstServiceDouble;
    
    if (td.hasClass('left')) {
        td = td.parent().find('.points.left');
        td.html(++resA);
    } else {
        td = td.parent().find('.points.right');
        td.html(++resX);                    
    }

    // Check for 'overflow'
    if (resA >= 11 && resA >= resX + 2 || resX >= 11 && resX >= resA + 2) {
        // Match is finished. Who has the next service?
        // In expedite try firstService, else we can calculate back
        if ( isChecked($('#setExpedite'))) {
            // We are in expedite system. But when did we start?        
            switch (firstService) {
                case 0 :
                    break;  // We just don't know
                
                case -1 :
                    // If we are in 1st, 3rd, ... game (0-based), then right wil have next service
                    setChecked($('#serviceleft'), ((counterData.setsLeft + counterData.setsRight) % 2) == 1);
                    setChecked($('#serviceright'), ((counterData.setsLeft + counterData.setsRight) % 2) == 0);
                    break;
                    
                case +1 :
                    // If we are in 1st, 3rd, ... game (0-based), then left wil have next service
                    setChecked($('#serviceleft'), ((counterData.setsLeft + counterData.setsRight) % 2) == 0);
                    setChecked($('#serviceright'), ((counterData.setsLeft + counterData.setsRight) % 2) == 1);
                    break;
            }
        } else if (resA + resX > 20) {  
            // The one who has the service now will serve the next game
        } else if ( (resA + resX - 1) % 4 >= 2) {
            // The other served, don't change
        } else {
            swapState($('#serviceleft'), $('#serviceright'));
        }
        
        // Disable startGame button
        $('#startGame').addClass('disabled');
        td.parent().find('.plus').addClass('disabled');
    } else {                
        // In Expedite, after 10:10 or every 2 points the service changes
        if ( isChecked($('#setExpedite')) || (resA + resX) >= 20 || ((resA + resX) % 2) == 0) {
            swapState($('#serviceleft'), $('#serviceright'));
            if (counterData.serviceDouble > 0) {
                // if current service is left and it is A->X or B->Y, then go backwards
                // if current service is right and it is X->B or Y->A, then go backwards
                // else go forward
                if (counterData.serviceLeft && (counterData.serviceDouble == 2 || counterData.serviceDouble == 4))
                    counterData.serviceDouble = 1 + ((counterData.serviceDouble - 1 - 1 + 4) % 4);
                else if (counterData.serviceRight && (counterData.serviceDouble == 1 || counterData.serviceDouble == 3))
                    counterData.serviceDouble = 1 + ((counterData.serviceDouble - 1 - 1 + 4) % 4);
                else
                    counterData.serviceDouble = 1 + ((counterData.serviceDouble - 1 + 1) % 4);

                showPlayers();
            }
        }
        
        if (!isChecked($('#setExpedite')) && (resA + resX) < 18) {
            if (counterData.gameMode == 'RESET' || counterData.timeMode == 'BREAK')
                counterData.gameTime = 600;

            if (counterData.timeMode != 'MATCH')
                startGameTimer();
        }
    }
    
    if ( counterData.setsLeft + counterData.setsRight == counterData.bestOf - 1) {
        // When we are in the last possible game at 5:x, force a "Swap Sides"
        if (resA == 5 && resX < 5 && td.hasClass('left') ||
            resX == 5 && resA < 5 && td.hasClass('right')) {
            // We can only go backward. After Swap Sides, only forward
            td.parent().find('.plus').addClass('disabled');
            td.parent().find('.minus').removeClass('disabled');
            
            $('#startGame').addClass('disabled');
            
            // startGame will toggle the button state, but we want to stop the timer
            // only explicit starting or implicit (increment) shall start the timer again
            setChecked($('#startGame'), true);
            startGame($('#startGame'));
        } else {
            td.parent().find('.minus').removeClass('disabled');
        }
    }
        
    // For simplicity recalculate data
    calculateData();
    
    storeData();
    
    // beep(50);
}


// decrement points
function decrement(button) {
    var td = button.parent();
    var resA = parseInt(td.parent().find('.points.left').html());
    var resX = parseInt(td.parent().find('.points.right').html());

    // If the match is finished, only the winner can be counted down
    if (resA >= 11 && resA >= resX + 2 && td.hasClass('right'))
        return;

    if (resX >= 11 && resX >= resA + 2 && td.hasClass('left'))
        return;   

    // Nothing below 0
    if (td.hasClass('left') && resA == 0)
        return;

    if (td.hasClass('right') && resX == 0)
        return;

    if (resA >= 11 && resA >= resX + 2 || resX >= 11 && resX >= resA + 2) {
        // Match was finished. Who had the previous service?
        if (counterData.expedite) {
            // I can't tell. When did expedite system start?
        } else if (resA + resX > 20) {
            // The one who had the service before will serve this game
        } else if ( (resA + resX - 1) % 4 >= 2) {
            // The other served, don't change
        } else {
            swapState($('#serviceleft'), $('#serviceright'));
        }
        
        $('#startGame').removeClass('disabled');
        
        td.parent().find('.plus').removeClass('disabled');
        
    } else {
        // In expedite, after 10:10 or every 2 points the service changes
        if ( counterData.expedite || (resA + resX) >= 20 || ((resA + resX) % 2) == 0) {
            swapState($('#serviceleft'), $('#serviceright'));
            
            if (counterData.serviceDouble > 0) {
                // if current service is left and it is A->X or B->Y, then go forwards
                // if current service is right and it is X->B or Y->A, then go forwards
                // else go backwards
                if (counterData.serviceLeft && (counterData.serviceDouble == 2 || counterData.serviceDouble == 4))
                    counterData.serviceDouble = 1 + ((counterData.serviceDouble - 1 + 1) % 4);
                else if (counterData.serviceRight && (counterData.serviceDouble == 1 || counterData.serviceDouble == 3))
                    counterData.serviceDouble = 1 + ((counterData.serviceDouble - 1 + 1) % 4);
                else
                    counterData.serviceDouble = 1 + ((counterData.serviceDouble - 1 - 1 + 4) % 4);

                showPlayers();
            }
        }       
    }

    if (td.hasClass('left')) {
        td = td.parent().find('.points.left');
        td.html(--resA);
    } else if (td.hasClass('right')) {
        td = td.parent().find('.points.right');
        td.html(--resX);                    
    }

    if ( counterData.setsLeft + counterData.setsRight == counterData.bestOf - 1) {
        if (resA == 5 && resX < 5 && td.hasClass('left') ||
            resX == 5 && resA < 5 && td.hasClass('right')) {
            td.parent().find('.minus').addClass('disabled');
            td.parent().find('.plus').removeClass('disabled');
            
            $('#startGame').addClass('disabled');
        } else {
            td.parent().find('.minus').removeClass('disabled');
            
            if (resA < 5 && resX < 5) {
                td.parent().find('.plus').removeClass('disabled');
                $('#startGame').removeClass('disabled');
            }
        }
    }
        
    // It shall not be a w/o
    counterData.abandonOrAbort = false;

    // For simplicity recalculate data
    calculateData();
    
    storeData();
}


// Set /reset a timeout
function timeout(checkbox) {
    toggleChecked(checkbox);
    
    var selected = isChecked(checkbox);    
    var td = checkbox.parent();
    if (td.hasClass('left'))
        counterData.timeoutLeft = selected;
    else if (td.hasClass('right'))
        counterData.timeoutRight = selected;

    if (selected) {
        startTimeoutTimer();
        counterData.timeoutLeftRunning = td.hasClass('left');
        counterData.timeoutRightRunning = td.hasClass('right');
    }
    
    storeData();
}            


// Set /reset a injured flag
function injured(checkbox) {
    toggleChecked(checkbox);
    
    var selected = isChecked(checkbox);
    var td = checkbox.parent();
    
    if (td.hasClass('left'))
        counterData.injuredLeft = selected;
    else if (td.hasClass('right'))
        counterData.injuredRight = selected;

    if (selected) {
        startInjuryTimer();
        counterData.injuredLeftRunning = td.hasClass('left');
        counterData.injuredRightRunning = td.hasClass('right');
    }
    
    storeData();
}    


// Lost a match w/o
function wo(button) {
    // Not when the match is finished
    if (counterData.gameMode == 'END')
        return;
    
    var left = button.hasClass('left');
    var name = (left ? $('.name.left').html() : $('.name.right').html()).replace(/&nbsp;/g, ' ' );
    
    // Confirm that the game shall be really a w/o. Really really.
    if (!isChecked(button) && !confirm('Are you sure that ' + name + ' *LOST* by w/o?'))        
        return;
    
    // If this button is to be checked,make sure the otheris not checked
    if (!isChecked(button)) {
        var other = (left ? $('#woright') : $('#woleft'));
        if (isChecked(other))
            wo(other);
    }
    
    toggleChecked(button);
    var selected = isChecked(button);
    var setsLeft = 0, setsRight = 0;

    for (var i = 1; i <= 7; i++) {
        var tr = $('#counter #game' + i);

        if (tr.hasClass('hidden'))
            break;

        var resA = parseInt(tr.find('.points.left').html());
        var resX = parseInt(tr.find('.points.right').html());

        if (!selected) {
            if (resX == 11 && resA == 0)
                tr.find('.points.right').html('0');
            else if (resA == 11 && resX == 0)
                tr.find('.points.left').html('0');
            
            continue;
        }
        
        if (resA >= 11 && resA >= resX + 2) {
            if ( 2 * ++setsLeft == counterData.bestOf + 1)
                break;
        } else if (resX >= 11 && resX >= resA + 2) {
            if (2 * ++setsRight == counterData.bestOf + 1)
                break;
        } else if (left) {
            if (resA > 9)
                resX = resA + 2;
            else
                resX = 11;
            
            tr.find('.points.right').html(resX);
            
            if (2 * ++setsRight == counterData.bestOf + 1)
                break;            
        } else {
            if (resX > 9)
                resA = resX + 2;
            else
                resA = 11;
            
            tr.find('.points.left').html(resA);
            
            if (2 * ++setsLeft == counterData.bestOf + 1)
                break;
        }
    }

    if (counterData.gameMode == 'RESET')        
        counterData.gameMode = 'RUNNING';
    
    counterData.abandonOrAbort = true;
        
    calculateData();                
    
    storeData();
}


// End a game
function endMatch() {
    if (counterData == null)
        return;
    
    if (counterData.gameMode != 'RUNNING')
        return;

    if (2 * counterData.setsLeft == counterData.bestOf + 1 || 
        2 * counterData.setsRight == counterData.bestOf + 1) {

        counterData.gameMode = 'END';

        $('#counter .game').addClass('inactive');
        
        // Should already be done by calculateData
        if (timer)
            clearInterval(timer);
        
        timer = null;
        
        setChecked($('#startGame'), false);        
        
        setChecked($('#endMatch'), true);
    }
    
    calculateData();
    // $('#games').html(counterData.setsLeft + '&nbsp;-&nbsp;' + counterData.setsRight);    
    
    clearData();
}


// Set who has the service
function setService(button) {
    toggleChecked(button);
    
    var selected = isChecked(button);
    
    if (selected) {
        if (button.hasClass('left'))
            setChecked($('#serviceright'), false);
        else if (button.hasClass('right'))
            setChecked($('#serviceleft'), false);
    }

    counterData.serviceLeft = isChecked($('#serviceleft'));
    counterData.serviceRight = isChecked($('#serviceright'));
    
    if (counterData.gameMode == 'RESET') {
        $('#startGame').removeClass('disabled');
        
        counterData.gameMode = 'WARMUP';
        startPrepareTimer();
    }
    
    if (selected) {
        var cs = counterData.setsLeft + counterData.setsRight;; // current game
        var fs = counterData.serviceLeft ? -1 : +1;             // temp. first service
        
        if (counterData.setHistory.length <= cs) {
            // Illegal or start of match
            fs = (cs == 0 ? fs : 0);
        } else if (counterData.setHistory[cs][0] == 0 && counterData.setHistory[cs][1] == 0) {
            // Start of game: if this is an even gaame it's just the other way round
            // Games are 0-based, meaning "cs == 1" is in fact the 2nd game
            fs = ((cs % 2) == 1) ? -fs : +fs;  
        } else if (counterData.expedite) {
            // Expedite within a game: do nothing: 
            // if all is well this is just be start of expedite
            fs = firstService; 
        } else {
            // Somewhere within a game, but not expedite:
            // Recalculate first service, first going back to start of game, 
            // then even further to start of match
            var cp = counterData.setHistory[cs][0] + counterData.setHistory[cs][1];
            if ((cs % 2) == 1) {
                fs = -fs;  // In the 2nd, 4th, ... game (0-based)
            }
            
            if (cp > 20) { 
                // After 10 : 10, change every point and go back to 10 : 10
                if ((cp % 2) == 1)
                  fs = -fs;
                  
                cp = 20;                
            }
            
            // Until 10:10 change every 2nd point
            if ((cp % 4) > 1) {
                fs = -fs;
            }
        }
        
        firstService = fs;
        
        // If firstServiceDouble is set, keep it:
        // Changing service from left to right does not change where player stay
        // If firstServiceDouble is not yet set, set it to 1 to keep players where they are
        if (firstServiceDouble === 0)
            firstServiceDouble = 1;
    } else {
        firstService = 0;
        firstServiceDouble = 0;
    }
    
    storeData();
}

// function Y, 1P, 2P cards
function setCard(button) {
    toggleChecked(button);
    
    var selected = isChecked(button);
    
    counterData.cardLeft = 'NONE';
    if (isChecked($('#yellowleft')))
        counterData.cardLeft = 'YELLOW';
    if (isChecked($('#yr1pleft')))
        counterData.cardLeft = 'YR1P';
    if (isChecked($('#yr2pleft')))
        counterData.cardLeft = 'YR2P';
    
    counterData.cardRight = 'NONE';
    if (isChecked($('#yellowright')))
        counterData.cardRight = 'YELLOW';
    if (isChecked($('#yr1pright')))
        counterData.cardRight = 'YR1P';
    if (isChecked($('#yr2pright')))
        counterData.cardRight = 'YR2P';    
}

// Start Match
function startGame(button) {
    toggleChecked(button);
    
    var selected = isChecked(button);
    
    if (!selected && counterData.timeMode == 'MATCH' && timer) {
        clearInterval(timer);
        timer = null;
        
        return;
    }

    if (counterData.gameMode == 'RESET' || counterData.timeMode == 'BREAK') {
        counterData.gameTime = 600;
    }

    if (selected) {
        $('#game' + (counterData.setsLeft + counterData.setsRight + 1)).removeClass('inactive');
        $('#setExpedite').removeClass('disabled');
        
        if (counterData.setHistory == null)
            counterData.setHistory = [];
        while (counterData.setHistory.length < counterData.bestOf)
            counterData.setHistory.push([0, 0]);
    }
    
    counterData.gameMode = 'RUNNING';
    
    startGameTimer();
    
    storeData();    
}


// Enter / leave expedite system
function setExpedite(button) {
    toggleChecked(button);
    
    var selected = isChecked(button);
    
    counterData.expedite = selected;
    
    calculateData();

    storeData();
}


// Set alert
function setAlert() {
    var ans = prompt('What is your problem');
    if (ans != null && ans != '') {
        counterData.alertText = ans;
        counterData.alert = true;

        $('#setAlert').addClass('alertpending');
    }        
}

// ===============================================================
// Helper functions
// Initialize my data
function initializeData() {
    counterData = {
        alert: false,
        expedite: false,
        gameMode: 'RESET',
        service: 1,
        abandonOrAbort: false,
        gameNr: 0xFFFF,
        playerNrLeft: 0xFFFF,
        playerNrRight: 0xFFFE,
        bestOf: 5,
        timeMode: 'NONE',
        timeoutLeft: false,
        timeoutRight: false,
        timeoutLeftRunning: false,
        timeoutRightRunning: false,
        injuredLeft: false,
        injuredRight: false,
        injuredLeftRunning: false,
        injuredRightRunning: false,
        cardLeft: 'NONE',
        cardRight: 'NONE',
        gameTime: 600,
        time: 0,
        setsLeft: 0,
        setsRight: 0,
        setHistory: [],
        swapped: false,
        serviceLeft: false,
        serviceRight: false,
        serviceDouble: 0,
        locked: false
    };
    
    if (debug) {
        namePlA = 'Player A';
        namePlB = 'Player B';
        namePlX = 'Player X';
        namePlY = 'Player Y';
    } else {
        namePlA = 'Player Left';
        namePlB = null;
        namePlX = 'Player Right';
        namePlY = null;
    }
}


function calculateData() {
    var setsLeft = 0, setsRight = 0;
    var setHistory = [];
    var timeMode = counterData.timeMode;
    var gameMode = counterData.gameMode;
    var bestOf   = counterData.bestOf;
    
    var i = 0;
    var tr = '';
    
    for (i = 1; i <= 7; i++) {
        tr = $('#counter #game' + i);

        if (tr.hasClass('hidden'))
            break;

        var resA = parseInt(tr.find('.points.left').html());
        var resX = parseInt(tr.find('.points.right').html());

        setHistory.push( [resA, resX] );

        if (resA >= 11 && resA >= resX + 2) {
            $('#results #result' + i).html(resX);
            
            if (2 * ++setsLeft == bestOf + 1) {
                break;
            }
            
        } else if (resX >= 11 && resX >= resA + 2) {
            $('#results #result' + i).html('-' + resA);
            
            if (2 * ++setsRight == bestOf + 1) {
                break;
            }
        } else if (resA == 0 && resX == 0) {
            $('#results #result' + i).html('');
            
            // If a game was played we are in a BREAK.
            if (setsLeft > 0 || setsRight > 0) {
                timeMode = 'BREAK';
                gameMode = 'RUNNING';

                if (counterData.timeMode != timeMode) {
                    startBreakTimer();
                }
            } 
            
            if (!tr.hasClass('inactive'))
                $('#setExpedite').removeClass('disabled');
                 
            break;
        } else {
            $('#results #result' + i).html('');
            
            // This game may be counted, but not the next or previous one
            $('#game' + i).removeClass('inactive');            

            if (i > 1) {
                $('#game' + (i-1)).addClass('inactive');
            }

            if (i < 7) {
                $('#game' + (i+1)).addClass('inactive');                
            }

            timeMode = 'MATCH';
            gameMode = 'RUNNING';

            if (resA + resX == 1) {
                // First point starts the time (here)
                // If the timer is running, start it again
                if (counterData.timeMode == 'NONE' || counterData.timeMode == 'BREAK')
                    counterData.gameTime = 600;
            } 
            
            // Stop (and restart) timer if it is not in MATCH
            if (counterData.timeMode != timeMode) {
                if (timer != null) {
                    clearInterval(timer);
                    timer = null;
                }                                
            }

            // Stop / resume timer if necessary
            if (resA + resX >= 18) {
                // Stop time, no more expedite system
                if (timer != null) {
                    clearInterval(timer);
                    timer = null;
                }

                // No timer is running
                timeMode = 'NONE';

                $('#setExpedite').addClass('disabled');
            } else {
                // Show setExpedite
                $('#setExpedite').removeClass('disabled');
            }

            break;
        }
    }
    
    // Show result, but make sure to correct the games
    var showSetsLeft = setsLeft, showSetsRight = setsRight;
    if (i >  1 && resA == 0 && resX == 0) {
        // Next game not started
        // Before we change side show old result, after show new result
        var tr = $('#game' + (i-1));
        var resA = parseInt(tr.find('.points.left').html());
        var resX = parseInt(tr.find('.points.right').html());

        if (!tr.hasClass('inactive')) {
            if (resA > resX)
                --showSetsLeft;
            else
                --showSetsRight;
        }
    } else if ( (2 * setsLeft > bestOf || 2 * setsRight > bestOf) && counterData.gameMode !== 'END') {
        // Match won. Before we go to "End Match" show old result, after show new result
        var tr = $('#game' + i);
        var resA = parseInt(tr.find('.points.left').html());
        var resX = parseInt(tr.find('.points.right').html());

        if (!tr.hasClass('inactive')) {
            if (resA > resX)
                --showSetsLeft;
            else
                --showSetsRight;
        }
    }
    
    // Reset action buttons of the remaining
    for (++i ; i < 7; i++) {
        tr = $('#counter #game' + i);

        if (tr.hasClass('hidden'))
            break;
        
        tr.addClass('inactive');
        $('#results #result' + i).html('');            
    }
    
    for (++i; i < 7; i++) {
        $('#results #result' + i).html('');
    }

    // Fill setHistory to the required length
    while (setHistory.length < counterData.bestOf)
        setHistory.push([0, 0]);

    // Enable / Disable certain flags
    if (2 * setsLeft > counterData.bestOf || 
        2 * setsRight > counterData.bestOf) {
        
        $('#endMatch').removeClass('disabled');
        
        // Stop the timer
        if (timer)
            clearInterval(timer);
        timer = null;
        
        setChecked($('#startGame'), false);
    } else {
        // Hide endMatch
        $('#endMatch').addClass('disabled');
    }
    
    // Write games
    $('#games').html(showSetsLeft + '&nbsp;-&nbsp;' + showSetsRight);

    counterData.timeMode = timeMode;                
    counterData.gameMode = gameMode;
    counterData.setsLeft = setsLeft;
    counterData.setsRight = setsRight;
    counterData.setHistory = setHistory;    

    counterData.serviceLeft = isChecked($('#serviceleft'));
    counterData.serviceRight = isChecked($('#serviceright'));
    
    counterData.expedite = isChecked($('#setExpedite'));
}

function initializeForm() {
    $('#schedule .start').html('');
    $('#schedule .table').html('');
    $('#schedule .nr').html('');
    
    $('#timer .timer').html('');
    $('#timer .timer').removeClass('warning alert');

    showPlayers();

    $('#games').html('0&nbsp;-&nbsp;0');

    setChecked($('.toggle'), false);
    
    var i = 0;
    
    for (i = 1; i <= counterData.bestOf; i++) {
        $('#game' + i).removeClass('hidden');
        $('#game' + i + ' .points.left').html('0');
        $('#game' + i + ' .points.right').html('0');
    }
    
    for (i = counterData.bestOf + 1; i <= 7; i++) {
        $('#game' + i).addClass('hidden');
        $('#game' + i + ' .points.left').html('0');
        $('#game' + i + ' .points.right').html('0');
    }

    // Enable first game and disable all others.
    for (i = 1; i <= 7; i++) {
        $('#game' + i).addClass('inactive');
        
        $('#game' + i + ' .action .plus').removeClass('disabled');
        $('#game' + i + ' .action .minus').removeClass('disabled');
    }
    
    $('#game1').removeClass('inactive');

    $('#startGame').removeClass('disabled');
    $('#setExpedite').removeClass('disabled');
    $('#endMatch').addClass('disabled');
    
    $('#results .result').html('');
    
    setChecked($('#endMatch'), false);
    
    if (timer)
        clearInterval(timer);

    timer = null;            
}


function showPlayers() {
    var cs = counterData.setsLeft + counterData.setsRight;
    var sd = counterData.serviceDouble;
    
    // After a game is finished cs is the next game
    // But if we are still at the last game, e.g. we went back, we have to correct for this
    if (cs > 0 && $('tr#game' + (cs + 1)).hasClass('inactive') && !$('tr#game' + cs).hasClass('inactive'))
        cs -= 1;
    
    var resA = $('tr#game' + (1 + cs) + ' td.points.left').html();
    var resX = $('tr#game' + (1 + cs) + ' td.points.right').html();
    
    // Present players as defined by counterData.serviceDouble, 
    // except at the beginning of a new game (counterData.serviceDouble is set with the first point)
    if (counterData.serviceDouble == 0)
        sd = firstServiceDouble;
    else if (resA == 0 && resX == 0)
        sd = firstServiceDouble;
    
    var left, right;
    if (namePlB === null)
        left = namePlA;
    else switch (sd) {
        case 2 :
        case 3 : 
            left = namePlB + '<br>' + namePlA;
            break;
        default :
            left = namePlA + '<br>' + namePlB;
            break;
    }
    
    if (namePlY === null)
        right = namePlX;
    else switch (sd) {
        case 3 :
        case 4 :
            right = namePlY + '<br>' + namePlX;
            break;
            
        default :
            right = namePlX + '<br>' + namePlY;
            break;
    }
    
    $('#names .name.left').html(left);
    $('#names .name.right').html(right);
}



// Format players name
function formatPlayer(pl) {
    var str = '' + pl.plNr + '&nbsp;';
    if (pl.psFirst != '')
        str += pl.psFirst.substring(0, 1) + '.' + '&nbsp;';
    
    if (window.matchMedia("(orientation: landscape)").matches)
        str += pl.psLast;
    else
        str += formatString(pl.psLast, 7);
    
    if (pl.naName != '')
        str += '&nbsp;' + '(' + pl.naName + ')';
    
    return str;
}


function storeData() {    
    $('#firstService').html(firstService);
    $('#firstServiceDouble').html(firstServiceDouble);
    $('#counterData.serviceDouble').html(counterData.serviceDouble);
    
    updateLastUpdateTime();
    
    if (typeof(localStorage) !== 'undefined') {
        var gameEnabled = 0;
        for (var i = 1; i <= 7; i++) {
            if ($('#game' + i).hasClass('inactive') == false) {
                if ($('#game' + i + ' .action .plus').hasClass('disabled') == false)
                    gameEnabled = i;
                else
                    gameEnabled = -i;
            }
        }
        
        if (counterData !== null)
            counterData.updateTime = (new Date()).getTime();
        
        localStorage.setItem("table=" + table, JSON.stringify({
            counterData : counterData, 
            counterMatch : counterMatch,
            gameEnabled : gameEnabled,
            lastUpdateTime : lastUpdateTime,
            firstService: firstService,
            firstServiceDouble: firstServiceDouble
        }));
    }
}


function restoreData() {    
    if (typeof(localStorage) !== 'undefined' && localStorage.getItem("table=" + table) != null) {        
        var data = null;
        
        try {
            data = JSON.parse(localStorage.getItem("table=" + table));
        } catch (e) {
            return;
        }
        
        // If older than one day, skip
        if (data.lastUpdateTime === null || data.lastUpdateTime < (new Date()).getTime() - 86400) 
            return;
        
        if (!confirm("Do you want to recover from a saved session?")) {
            clearData();
            return;
        }
        
        firstService = data.firstService;        
        firstServiceDouble = data.firstServiceDouble;
        counterData.serviceDouble = data.counterData.serviceDouble;
        
        setMatch(data.counterMatch);
        
        // counterMatch = data.counterMatch;
        counterData = data.counterData;
        
        // But without the locked attribute
        counterData.locked = false;
        
        if (counterMatch !== null) {
            if (counterData.playerNrLeft == counterMatch.plX.plNr || counterData.playerNrLeft == 0xFFFE) {
                if (counterMatch.cpType == 4) {
                    $('#teamleft').html(counterMatch.tmX.tmDesc);
                    $('#teamright').html(counterMatch.tmA.tmDesc);
                    $('#teamresult').html(counterMatch.mttmResX + '&nbsp;-&nbsp;' + counterMatch.mttmResA);                        
                }

                var tmp = namePlA;
                namePlA = namePlX;
                namePlX = tmp;
                
                tmp = namePlB;
                namePlB = namePlY;
                namePlY = tmp;

                $('#games').html(counterMatch.mtResX + '&nbsp;-&nbsp;' + counterMatch.mtResA);
            }
        }
        
        var i = 0;
        
        if (counterData.gameMode == 'RUNNING') {
            for (i = 1; i <= counterData.setHistory.length; i++) {
                $('#game' + i).removeClass('hidden');
                $('#game' + i + ' .points.left').html(counterData.setHistory[i-1][0]);
                $('#game' + i + ' .points.right').html(counterData.setHistory[i-1][1]);
            }
            
            for (i = counterData.setHistory.length + 1; i <= counterData.bestOf; i++) {
                $('#game' + i).removeClass('hidden');
                $('#game' + i + ' .points.left').html('0');
                $('#game' + i + ' .points.right').html('0');                
            }
            
            for (i = counterData.bestOf + 1; i <= 7; i++) {
                $('#game' + i).addClass('hidden');
                $('#game' + i + ' .points.left').html('0');
                $('#game' + i + ' .points.right').html('0');                                
            }
        
            setChecked($('#serviceleft'), counterData.serviceLeft);
            setChecked($('#serviceright'), counterData.serviceRight);
            
            setChecked($('#timeoutleft'), counterData.timeoutLeft);
            setChecked($('#timeoutright'), counterData.timeoutRight);
            
            setChecked($('#injuredleft'), counterData.injuredLeft);
            setChecked($('#injuredright'), counterData.injuredRight);

            setChecked($('#yellowleft'), false);
            setChecked($('#yellowright'), false);
            
            setChecked($('#yr1pleft'), false);
            setChecked($('#yr1pright'), false);
            
            setChecked($('#yr2pleft'), false);
            setChecked($('#yr2pright'), false);
            
            switch (counterData.cardLeft) {
                // Fall through intended
                case 'YR2P' : setChecked($('#yr2pleft'), true);
                case 'YR1P' : setChecked($('#yr1pleft'), true);
                case 'YELLOW' : setChecked($('#yellowleft'), true);                    
            }
            
            switch (counterData.cardRight) {
                // Fall through intended
                case 'YR2P' : setChecked($('#yr2pright'), true);
                case 'YR1P' : setChecked($('#yr1pright'), true);
                case 'YELLOW' : setChecked($('#yellowright'), true);                    
            }
            
            setChecked($('#setExpedite'), counterData.expedite);
            
            $('#startGame').removeClass('disabled');
            $('#setExpedite').removeClass('disabled');
            
            for (var i = 1; i <= 7; i++) {
                if (data.gameEnabled == -i) {
                    $('#game' + i).removeClass('inactive');
                    $('#game' + i + ' .action .plus').addClass('disabled');
                } else if (data.gameEnabled == i) {
                    $('#game' + i).removeClass('inactive');
                } else {
                    $('#game' + i).addClass('inactive');
                }
            }
            
            // calculateData changes timeMode and gameMode
            var timeMode = counterData.timeMode;
            var gameMode = counterData.gameMode;
            var time = counterData.time;
                        
            calculateData();
            
            counterData.timeMode = timeMode;
            counterData.gameMode = gameMode;
            
            switch (counterData.timeMode) {
                case 'MATCH' :
                    startGameTimer();
                    break;
                    
                case 'TIMEOUT' :                    
                    startTimeoutTimer();
                    counterData.time = time;
                    break;
                    
                case 'BREAK' :
                    startBreakTimer();
                    counterData.time = time;
                    break;
                    
                case 'INJURY' :
                    startInjuryTimer();
                    counterData.time = time;
                    break;
            }
        }
        
        showPlayers();
    } 
}

function clearData() {
    if (typeof(localStorage) != 'unknown') 
        localStorage.removeItem("table=" + table);
}

function swapState(left, right) {
    var tmp = isChecked(left);
    setChecked(left, isChecked(right));
    setChecked(right, tmp);
}

function isChecked(button) {
    return button.attr('checked') ? true : false;
}


function setChecked(button, value) {
    button.attr('checked', value ? 'checked' : false);
}


function toggleChecked(button) {
    button.attr('checked', isChecked(button) ? false : 'checked');
}

function toggleDisabled(button) {
    if (button.hasClass('disabled'))
        button.removeClass('disabled');
    else
        button.addClass('disabled');
}


function startPrepareTimer() {
    setChecked($('#startGame'), false);
    $('#startGame').removeClass('disabled');
    
    counterData.time = 120;
    counterData.timeMode = 'PREPARE';    
    
    if (timer)
        clearInterval(timer);
    
    $('#timer .timer').removeClass('alert warning');
    
    timer = setInterval(function() {
        if (counterData.time > 0)
            counterData.time--;
        
        $('#timer .timer').html('Practice: ' + counterData.time);
    
        storeData();
    }, 1000); 
}


function startGameTimer() {
    setChecked($('#startGame'), true);
    
    // Resume timer
    counterData.time = counterData.gameTime;
    counterData.timeMode = 'MATCH';
    
    if (timer)
        clearInterval(timer);
    
    timer = setInterval(function() {
        if (counterData.time > 0)
            counterData.time--;
        
        counterData.gameTime = counterData.time;

        $('#timer .timer').html('Time: ' + counterData.time);

        if (counterData.time > 60)
            $('#timer .timer').removeClass('alert warning');
        else if (counterData.time > 30)
            $('#timer .timer').addClass('warning').removeClass('alert');
        else if (counterData.time >= 0)
            $('#timer .timer').addClass('alert').removeClass('warning');
    
        storeData();
    }, 1000);
}


function startBreakTimer() {
    setChecked($('#startGame'), false);
    
    counterData.time = 60;
    counterData.timeMode = 'BREAK';    
    
    if (timer)
        clearInterval(timer);
    
    $('#timer .timer').removeClass('alert warning');
    
    timer = setInterval(function() {
        if (counterData.time > 0)
            counterData.time--;
        
        $('#timer .timer').html('Break: ' + counterData.time);
    
        storeData();
    }, 1000); 
}


function startTimeoutTimer() {
    setChecked($('#startGame'), false);
    
    counterData.time = 60;    
    counterData.timeMode = 'TIMEOUT';        
    
    if (timer)
        clearInterval(timer);
    
    timer = setInterval(function() {
        if (counterData.time > 0)
            counterData.time--;
        
        $('#timer .timer').html('Timeout: ' + counterData.time);
    
        storeData();
    }, 1000);
}


function startInjuryTimer() {
    setChecked($('#startGame'), false);
    
    counterData.time = 600;
    counterData.timeMode = 'INJURY';    
    
    if (timer)
        clearInterval(timer);
    
    timer = setInterval(function() {
        if (counterData.time > 0)
            counterData.time--;
        
        $('#timer .timer').html('Injury: ' + counterData.time);
    
        storeData();
    }, 1000);    
}
// ===============================================================
// Communication with server


function onMessage(msg) {
    // document.getElementById('log').value = document.getElementById('log').value + msg.toString() + '\n';
    switch (msg.command) {
        case CmdEnum.GET_DATA : 
        case CmdEnum.GET_DATA_BROADCAST : 
            getData(msg);
            break;

        case CmdEnum.SET_DATA :
            setMatch(JSON.parse(msg.data));                            
            break;

        case CmdEnum.SWAP_PLAYERS :
            swapPlayers();
            break;

        case CmdEnum.RESET_ALERT :
            resetAlert();
            break;

        case CmdEnum.RESET :
            resetMatch();
            break;
            
        case CmdEnum.SET_RESULT :
            setResult(JSON.parse(msg.data));
            break;
            
        case CmdEnum.LOCK_SCREEN :
            lockScreen();
            break;
            
        case CmdEnum.UNLOCK_SCREEN :
            unlockScreen();
            break;
    }
}

// Send data to server
function send(command, body) {
    data = {table: table, command: command, data: body};
    
    $.ajax({
        url: '../counter/command',
        type : 'POST',
        data : JSON.stringify(data),
        dataType: 'json',
        timeout: 100, // 100 ms
        success: function(data) {
            lastSentTime = (new Date()).getTime();
            
            if (data == null)
                return;

            for (var i = 0; i < data.length; i++)
                onMessage(data[i]);

        },
        error: function() {lastSentTime = 0;}
    });
}

// Request from server: Send data
function getData(msg) {
    if (counterData.timeMode == 'RUNNING')
        counterData.gameTime = counterData.time;
    
    send(msg.command, JSON.stringify(counterData));
}


// Command from server: set game data
function setMatch(match) {
    // Don't overwrite a running match
    if (counterData != null && counterData.gameMode != 'RESET')
        return;
    
    if (match === null)
        return;
    
    // Unlock screen if we are near start of match
    checkPrestart();
    
    var plA = match.plA;
    var plB = match.plB;
    var plX = match.plX;
    var plY = match.plY;

    var i = 0;

    if (plA.plNr > 0)
        namePlA = formatPlayer(plA);
    else if (debug)
        namePlA = 'Player A';
    else
        namePlA = 'Player Left';

    if (plB.plNr > 0)
        namePlB = formatPlayer(plB);
    else if (debug)
        namePlB = 'Player B';
    else
        namePlB = null;

    if (plX.plNr > 0)
        namePlX = formatPlayer(plX);
    else if (debug)
        namePlX = 'Player X';
    else
        namePlX = 'Player Right';    

    if (plY.plNr > 0)
        namePlY = formatPlayer(plY);
    else if (debug)
        namePlY = 'Player Y';
    else
        namePlY = null;
    
    if (plA.plNr <= 0 && plB.plNr <= 0)
        namePlA = match.tmA.tmDesc;
    
    if (plX.plNr <= 0 && plY.plNr <= 0)
        namePlX = match.tmX.tmDesc;
    
    var left = namePlA;
    if (namePlB !== null)
        left += '<br>' + namePlB;
    
    var right = namePlX;
    if (namePlY !== null)
        right += '<br>' + namePlY;
    
    var d = new Date(match.mtDateTime);

    $('#schedule .table').html('Table: ' + match.mtTable);
    if (match.cpDesc != '')
        $('#schedule .event').html('Event: ' + match.cpDesc);
    else
        $('#schedule .event').html('');
    
    $('#schedule .start').html('Start: ' + formatTime(match.mtDateTime));
    $('#schedule .nr').html('Match: ' + match.mtNr + (match.mtMS > 0 ? ' - Individual Match: ' + match.mtMS : ''));
    
    
    if (match.cpType == 4) {
        $('.teams').removeClass('hidden');
        $('#teamleft').html(match.tmA.tmDesc);
        $('#teamright').html(match.tmX.tmDesc);
        $('#teamresult').html(match.mttmResA + '&nbsp;-&nbsp;' + match.mttmResX);        
    } else {
        $('.teams').addClass('hidden');
    }
    
    $('#names .name.left').html(left); 
    $('#names .name.right').html(right);
    
    $('#games').html(match.mtResA + '&nbsp;-&nbsp;' + match.mtResX);

    for (i = 1; i <= match.mtBestOf; i++) {
        $('#game' + i).removeClass('hidden');
    }

    for (i = match.mtBestOf + 1; i <= 7; i++) {
        $('#game' + i).addClass('hidden');
    }

    counterMatch = match;                

    counterData.playerNrLeft = (plA.plNr > 0 ? plA.plNr : 0xFFFF);
    counterData.playerNrRight = (plX.plNr > 0 ? plX.plNr : 0xFFFE);
    counterData.gameNr = match.mtMS > 1 ? match.mtMS : match.mtNr;
    counterData.bestOf = match.mtBestOf;

    if (match.mtResult != null) {
        // Later disable first game and the buttons for startGame and setExpedite 
        // until the game is acually started
        $('#game1').removeClass('inactive');
        
        for (i = 0; i < match.mtResult.length; i++) {
            $('#game' + (i+1) + ' .points.left').html(match.mtResult[i][0]);
            $('#game' + (i+1) + ' .points.right').html(match.mtResult[i][1]);

            if (i > 0 && (match.mtResult[i][0] > 0 || match.mtResult[i][1] > 0)) {
                $('#game' + i).addClass('inactive');
            }
        }

        // If there is a result, calculate result
        if (match.mtResult[0][0] || match.mtResult[0][1])
            calculateData();
        else {
            $('#game1').removeClass('inactive');
        }
    } else {
        // Later disable first game and the buttons for startGame and setExpedite 
        // until the game is acually started
        $('#game1').removeClass('inactive');
        
        for (i = 2; i <= 7; i++) {
            $('#game' + i).addClass('inactive');
        }
    }
}

function setResult(result) {
    for (i = 0; i < result.length; i++) {
        // Only for games not in use
        if (!$('#game' + (i+1)).hasClass('inactive'))
            break;
        
        $('#game' + (i+1) + ' .points.left').html(result[i][0]);
        $('#game' + (i+1) + ' .points.right').html(result[i][1]);
    }
    
    calculateData();
}

function resetAlert() {
    counterData.alertText = null;
    counterData.alert = false;

    $('#setAlert').removeClass('alertpending');
}

function resetMatch() {
    counterMatch = null;
    initializeData();
    initializeForm();  
}

function updateLastUpdateTime() {
    lastUpdateTime = (new Date()).getTime();
}


function lockScreen() {
    if (checkPrestart())
        return;
    
    $('body').addClass('locked');
    counterData.locked = true;
}

function unlockScreen() {
    $('body').removeClass('locked');
    counterData.locked = false;
}

var unlockClick = 0;
var unlockClickTimer = null;
function unlockAction() {
    if (unlockClickTimer !== null)
        clearTimeout(unlockClickTimer);
    
    unlockClickTimer = null;
    
    if (++unlockClick >= 5)
        unlockScreen();
    else
        unlockClickTimer = setTimeout(function() {unlockClick = 0;}, 1000);
}

function checkPrestart() {
    var unlock = 
        prestart === 0 ||
        counterData !== null && counterData.gameMode !== 'RESET' ||
        counterMatch !== null  && counterMatch.mtDateTime < (new Date()).getTime() + prestart * 1000
    ;

    if (unlock && $('body').hasClass('locked'))
        unlockScreen();
    
    return unlock;
}
