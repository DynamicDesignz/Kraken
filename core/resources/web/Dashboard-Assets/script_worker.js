/* Script for Worker Dashboard */

// Connect DOM objects to Javascript Variables

// Define Update functions
function initWebUI(){
	// Initiate elements
	$("#kraken_time").children(".content").text("0");
	$("#current_id").children(".content").text("NA");
	$("#current_type").children(".content").text("NA");
	$("#current_ssid").children(".content").text("NA");
	$("#current_capfile").children(".content").text("NA");
}
var update_status = function(response){
	console.log(response);
	updateObject = JSON.parse(response);
	
	// Update elements
	$("#kraken_time").children(".content").text(updateObject["krakenTime"]);
	$("#current_id").children(".content").text(updateObject["currentId"]);
	$("#current_type").children(".content").text(updateObject["currentType"]);
	$("#current_ssid").children(".content").text(updateObject["currentSsid"]);
	$("#current_capfile").children(".content").text(updateObject["currentCapfile"]);
};

//AJAX Call Function
function send_request_to_servlet(parameter, payload, return_call){
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState == 4 && xhttp.status == 200){return_call(xhttp.response);}
    }
    xhttp.open("POST", "/workerwebui", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhttp.send("type="+parameter+"&payload="+payload);
}

// Update Status Variables every 2 seconds
setInterval(function(){send_request_to_servlet("status", "none", update_status);}, 2000);

// On load, request initialization
$(document).ready(function(){
	initWebUI();
	send_request_to_servlet("status", "none", update_status);
});

