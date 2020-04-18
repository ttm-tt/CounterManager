var clock = new StationClock("clock");

function init() {
    clock.dialColor = 'rgb(0, 0, 0)';
    switch (getParameterByName('body', 'none')) {
        case 'none' :
            clock.body = StationClock.NoBody;
            break;

        case 'small' :
            clock.body = StationClock.SmallWhiteBody;
            break;

        case 'round' :
            clock.body = StationClock.RoundBody;
            break;

        case 'green' :
            clock.body = StationClock.RoundGreenBody;
            break;

        case 'square' :
            clock.body = StationClock.SquareBody;
            break;

        case 'vienna' :
            clock.body = StationClock.ViennaBody;
            break;
    }

    clock.dial = StationClock.SwissStrokeDial;
    clock.hourHand = StationClock.SwissHourHand;
    clock.minuteHand = StationClock.SwissMinuteHand;
    // clock.secondHand = StationClock.SwissSecondHand;
    clock.secondHand = StationClock.BarSecondHand;
    clock.boss = StationClock.NoBoss;
    clock.minuteHandBehavoir = StationClock.BouncingMinuteHand;
    clock.secondHandBehavoir = StationClock.OverhastySecondHand;

    animate();
}

function animate() {
    clock.draw();
    window.setTimeout("animate()", 50);
}

$(document).ready(function() {
    init();
});