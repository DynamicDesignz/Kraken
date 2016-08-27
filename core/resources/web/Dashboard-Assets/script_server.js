/* Script for Server Dashboard */

//Connect DOM objects to Javascript Variables



//Define Update functions

function update_init(response){
    console.log(response);
    updateObject = JSON.parse(response);
    for(var i=0; i<updateObject["PasswordLists"].length; i++){
        $("#nr_password_list_container").append("<div class = \"checkbox\"><label><input type = \"checkbox\" name=\"request-plist\" value=\""+updateObject["PasswordLists"][i]+"\">"+updateObject["PasswordLists"][i]+" <span> - "+updateObject["PasswordListSizes"][i]+" </span></label></div>");
    }
    
    for(var i=0; i<updateObject["Algorithms"].length; i++){
        $("#nr_algo").append("<option value=\""+updateObject["Algorithms"][i]+"\">"+updateObject["Algorithms"][i]+"</option>");
    }
    
    for(var i=0; i<updateObject["Results"].length; i++){
        addResultToResultSection(updateObject["Results"][i]);
    }
    
    $(".splitcol-right").height($(".splitcol-left").height);
};

//local variable
var completedRequests = 0;

var update_status = function (response){
    console.log(response);
    updateObject = JSON.parse(response);
    
    //Global Elements
    $("#glbl_uptime").children(".content").text(updateObject["Uptime"]);
    $("#glbl_onlineworkers").children(".content").text(updateObject["Onlineworkers"]);
    $("#glbl_activeworkers").children(".content").text(updateObject["Activeworkers"]);
    $("#glbl_requestsinqueue").children(".content").text(updateObject["Requestsinqueue"]);
    $("#glbl_usedmemory").children(".content").text(updateObject["Memory"]);
    $("#glbl_completedrequests").children(".content").text(updateObject["CompletedRequests"]);
    if(updateObject["GearmanStatus"]){
        $("#glbl_gearmanstatus").children(".content").css({"background-color":"green"});
    }
    else{
        $("#glbl_gearmanstatus").children(".content").css({"background-color":"red"});
    }
    if(updateObject.hasOwnProperty("Result")){
        completedRequests = completedRequests +1;
        $("#glbl_completedrequests").children(".content").text(completedRequests);
    }
    
    
    
    //Results Section
    if(updateObject.hasOwnProperty("Result")){
        addResultToResultSection(updateObject["Result"]);
    }
};

function addResultToResultSection(resultObject){
    if($("#results_noresult").is(":visible")){
            $("#results_noresult").hide(100);
            $("#results-list").show(100);
    }
     $("#results-list").append("<div class=\"result-entry\"><div>UUID : <span>"+resultObject["Uuid"]+"</span></div><div>Identifier : <span>"+resultObject["Identifier"]+"</span></div>                                <div>File : <span>"+resultObject["Filename"]+"</span></div><div>Password : <span>"+resultObject["Password"]+"</span></div><div>Status : <span>"+resultObject["Status"]+"</span><div></div>");
    
}

var update_activerequest_status = function (response){
    console.log(response);
    updateObject = JSON.parse(response);
    if(jQuery.isEmptyObject(updateObject)){
        //Empty Request Remove
        $("#activerequest_info").hide(1000);
        $("#activerequest_norequest").show(1000);
    }
    else{
        if(!$("#activerequest_info").is(":visible")){
            $("#activerequest_info").show(1000);
            $("#activerequest_norequest").hide(1000);
        }
        $("#act_id").children(".content").text(updateObject["Uuid"]);
        $("#act_type").children(".content").text(updateObject["Type"]);
        $("#act_ssid").children(".content").text(updateObject["Ssid"]);
        $("#act_capfile").children(".content").text(updateObject["File"]);
        
        $("#act_currentplist").children(".content").empty();
        $("#act_currentplist").children(".content").append("<button type=\"button\" class=\"btn btn-success\">"+updateObject["Runningplist"]+"</button>");
        
        $("#act_plistprogress").children(".content").empty();
        //Completed Lists
        for(var i=0; i<updateObject["Completedplist"].length; i++){
            $("#act_plistprogress").children(".content").append("<button type=\"button\" class=\"btn btn-primary\">"+updateObject["Completedplist"][i]+"</button>");
        }
        //Running Lists
        $("#act_plistprogress").children(".content").append("<button type=\"button\" class=\"btn btn-success\">"+updateObject["Runningplist"]+"</button>");
        //Pending List
        for(var i=0; i<updateObject["Pendingplist"].length; i++){
            $("#act_plistprogress").children(".content").append("<button type=\"button\" class=\"btn btn-warning\">"+updateObject["Pendingplist"][i]+"</button>");
        }
        $("#act_currentplistprogress").children(".content").text(updateObject["Completedchunks"].toString()+"/"+updateObject["Totalchunks"].toString()+" Jobs");
        
        var percentage = (((updateObject["Completedchunks"])/updateObject["Totalchunks"])*100.0).toFixed(1);
        
        $("#act_currentplistprogressbar").children(".progress-bar").width(percentage + '%');
        $("#act_currentplistprogressbar").children(".content").text(percentage.toString()+"% Complete");    
        console.log(percentage.toString()+"% Complete")
    }
};

var update_newrequest = function (response){
    
};

var submit_newrequest = function (response){
    
};

function iframeLoad(){
   $("#form-result-frame").show(1000);
    $("#iframe-ghost-div").hide(1000);
    setTimeout(function(){
        document.getElementById('nr_request_form').reset();
        $('#form-result-frame').hide(1000,function(){
            $('#form-result-frame').attr('src', "");
        });
        $("#iframe-ghost-div").show(1000);
    }, 5000);
    //send_request_to_servlet(false, "activerequest", "none", update_activerequest_status);
}

function pulseAddRequest(){
    $('html, body').animate({ scrollTop: $("#new_request").offset().top }, 1200,"easeOutQuint");
    $("#new_request").animate( { backgroundColor: "#9ae59a" }, 500 )
            .animate( { backgroundColor: "transparent" }, 500 );
}

function pulseResults(){
    $('html, body').animate({ scrollTop: $("#request_list").offset().top }, 1200,"easeOutQuint");
    $("#request_list").animate( { backgroundColor: "#9ae59a" }, 500 )
            .animate( { backgroundColor: "transparent" }, 500 );
}

// AJAX Call Function
function send_request_to_servlet(isform, parameter, payload, return_call){
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (xhttp.readyState == 4 && xhttp.status == 200){return_call(xhttp.response);}
    }
    xhttp.open("POST", "/serverwebui", true);
    xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhttp.send("type="+parameter+"&payload="+payload);
}

//Direct Execution

//Update Status Variables every 3 seconds
setInterval(function(){send_request_to_servlet(false, "status", "none", update_status);}, 2000);

//Update Active Request Status every 10 seconds
setInterval(function(){send_request_to_servlet(false, "activerequest", "none", update_activerequest_status);},2000)

//On load, request Initialization 
$(document).ready(function(){
    send_request_to_servlet(false, "init" , "none" ,update_init);
    send_request_to_servlet(false, "status", "none", update_status);
    send_request_to_servlet(false, "activerequest", "none", update_activerequest_status);
    $("#activerequest_info").hide();
    $("#activerequest_norequest").show();
    $("#results-list").hide();
    $("#results_noresult").show();
    $("#form-result-frame").hide();
    $("#iframe-ghost-div").show();
});

