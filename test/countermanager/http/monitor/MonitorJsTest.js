/* Copyright (C) 2020 Christoph Theis */

$('body').append('<div id="log" style="display:none"></div>');

// Webdriver can't access output of console.log, only of console.warn and .error
// As a workaround we put the logs into its own div and read that after the test.
console.log = function(message) {document.getElementById('log').innerHTML += '<span>' + message + '</span>';};

import * as Monitor from '/monitor/monitor.js';
window.Monitor = Monitor;

import * as CounterData from '/scripts/modules/counter_data.js';
window.CounterData = CounterData;




