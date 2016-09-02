package servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import managers.jobmgr.GearmanJobManager;
import managers.workermgr.GearmanWorkerManager;
import objects.PasswordList;
import objects.Request;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import util.UtilityFunctions;
import config.ConfigurationLoader;
import constants.Constants;


public class ServerWebUIServlet extends HttpServlet
{
	private static final long serialVersionUID = 6645066635774263607L;
	private long time_at_start;
	private Queue<Request> results;
	private ArrayList<Request> cachedResults;
	
    public ServerWebUIServlet(){
    	time_at_start=System.currentTimeMillis();
    	results = new LinkedList<Request>();
    	cachedResults = new ArrayList<Request>();
    }
    
    /*
     *  Queue a Result to be sent to UI
     */
    public void queueResultForWebUI(Request res){
    	results.add(res);
    }
    
    /*
     * Handles All Update Requests From Web UI
     */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
    	//System.out.println("INFO : WebUI requested " + request.getParameter("type"));
    	JSONObject returnObject = new JSONObject();
    	
    	String typeParameter = request.getParameter("type");
    	
    	if(typeParameter == null){
    		// New Request
    		if(ServletFileUpload.isMultipartContent(request)){
    			Request newRequest = new Request();
    			try{
    				newRequest.createRequest(request);
    				GearmanJobManager.getInstance().addRequest(newRequest);
    				System.out.println(UtilityFunctions.craftwebuiFormReply(true, Constants.newrequest_response_success));
    				response.getWriter().println(UtilityFunctions.craftwebuiFormReply(true, Constants.newrequest_response_success));
    			}
    			catch(Exception e){
    				System.out.println("Request could not be made " + e.getMessage());
    				response.getWriter().println(UtilityFunctions.craftwebuiFormReply(false, e.getMessage()));
    			}
    		}
    		// Add Password
    		else{
    			try{
    				PasswordList newList = new PasswordList(request.getParameter("name-passwordlist"),request.getParameter("path-passwordlist"));
    				if(ConfigurationLoader.getInstance().getAvailablePasswordLists().containsKey(newList.getName())){throw new RuntimeException("Password List Name Already Exists");}
    				ConfigurationLoader.getInstance().getAvailablePasswordLists().put(request.getParameter("name-passwordlist"),newList);
    				UtilityFunctions.insertPasswordListIntoDB(newList);
    				response.getWriter().println(UtilityFunctions.craftwebuiFormReply(true, Constants.addpassword_response_success));
    			}
    			catch(Exception e){response.getWriter().println(UtilityFunctions.craftwebuiFormReply(false, e.getMessage()));}
    		}
    	}
    	else if (typeParameter.equals("status")){
    		//Update Web UI Status
    		response.getWriter().println(handle_status(returnObject));
    	}
    	else if (typeParameter.equals("init")){
    		//Update Web UI at Initialization
    		response.getWriter().println(handle_initialize(returnObject));
    	}
    	else if (typeParameter.equals("passwordlists")){
    		//Update Web UI of Password Lists
    		response.getWriter().println(handle_passwordlists(returnObject));
    	}
    	else if (typeParameter.equals("activerequest")){
    		//Update Active Request
    		response.getWriter().println(handle_activerequest(returnObject));
    	}
    	else{
    		System.out.println("Unknown Request from Web UI");
    		response.sendError(500);
    	}
    }
    
	@SuppressWarnings("unchecked")
	JSONObject handle_initialize(JSONObject returnObject){
    	
    	// Algorithms
    	JSONArray algo = new JSONArray();
    	for(String s : ConfigurationLoader.getInstance().getHostedAlgorithms()){
    		algo.add(new String(s));
    	}
    	returnObject.put("Algorithms", algo);
    	
    	//Cached Results
    	JSONArray results = new JSONArray();
    	for(Request r : cachedResults){
    		JSONObject resObj = new JSONObject();
    		resObj.put("Uuid", r.getId().toString());
    		resObj.put("Identifier",r.getCaptureIdentifier());
    		resObj.put("Filename", r.getFileName());
    		resObj.put("Password", r.getResult());
    		resObj.put("Status", r.getStatus());
    		results.add(resObj);
    	}
    	
    	returnObject.put("Port", Constants.WebUIPort);
    	returnObject.put("StartupMode",Constants.StartupMode);
    	returnObject.put("Results",results);
		return returnObject;
    }
	
	@SuppressWarnings("unchecked")
	JSONObject handle_passwordlists(JSONObject returnObject){
		// PasswordLists
    	JSONArray pLists = new JSONArray();
    	JSONArray pListSizes = new JSONArray();
    	JSONArray pListPaths = new JSONArray();
    	JSONArray pListLineCounts = new JSONArray();
    	JSONArray pListCharsets = new JSONArray();
    	
    	for(Entry<String, PasswordList> entry : ConfigurationLoader.getInstance().getAvailablePasswordLists().entrySet()){
    		pLists.add(new String(entry.getKey()));
    		pListSizes.add(entry.getValue().getSize());
    		pListLineCounts.add(entry.getValue().getLineCount());
    		pListPaths.add(entry.getValue().getPath().toString());
    		pListCharsets.add(entry.getValue().getCharset().toString());
    	}
    	returnObject.put("PasswordLists", pLists);
    	returnObject.put("PasswordListSizes", pListSizes);
    	returnObject.put("PasswordListLineCounts", pListLineCounts);
    	returnObject.put("PasswordListPaths", pListPaths);
    	returnObject.put("PasswordListCharsets",pListCharsets);    	
		return returnObject;
	}
    
    @SuppressWarnings({ "unchecked"})
	JSONObject handle_status(JSONObject returnObject){
    	//Time
    	long curr_time = System.currentTimeMillis() - time_at_start;
    	String time = null;
    	if(TimeUnit.MILLISECONDS.toMinutes(curr_time) < 60){
    		time = Long.toString(TimeUnit.MILLISECONDS.toMinutes(curr_time)) + " M";
    	}
    	else if (TimeUnit.MILLISECONDS.toHours(curr_time) < 24){
    		time = Long.toString(TimeUnit.MILLISECONDS.toHours(curr_time)) + " H";
    	}
    	else if (TimeUnit.MILLISECONDS.toDays(curr_time) < 30){
    		time = Long.toString(TimeUnit.MILLISECONDS.toDays(curr_time)) + " D";
    	}
    	returnObject.put("Uptime",time);
    	
    	//Online Workers
    	returnObject.put("Onlineworkers", GearmanWorkerManager.getInstance().getOnlineWorkers());
    	
    	//Active Workers
    	returnObject.put("Activeworkers", GearmanWorkerManager.getInstance().getActiveWorkers());
    	
    	//Requests in Queue
    	returnObject.put("Requestsinqueue", GearmanJobManager.getInstance().getRequestQueueSize());
    	
    	//Completed Requests
    	returnObject.put("Completedrequests", cachedResults.size());
    	
    	//Gearman Status
    	if(UtilityFunctions.sendTextCommandToGearmanServer("Version").isEmpty()){returnObject.put("GearmanStatus", false);}else{returnObject.put("GearmanStatus", true);};
    	
    	//Memory Usage
    	returnObject.put("Memory",getMemoryUsage() + "%");
    	
    	//Request Result
    	if(!results.isEmpty()){
    		Request resultToSend = results.remove();
    		cachedResults.add(resultToSend);
    		JSONObject result = new JSONObject();
    		result.put("Uuid", resultToSend.getId().toString());
    		result.put("Identifier",resultToSend.getCaptureIdentifier());
    		result.put("Filename", resultToSend.getFileName());
    		result.put("Password", resultToSend.getResult());
    		result.put("Status", resultToSend.getStatus());
    		returnObject.put("Result",result);
    	}
    	
    	return returnObject;
    }
    
    private long getMaxHeapSize()
    {
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    private Long getHeapUsed() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
    }

    private Integer getMemoryUsage()
    {
        return new Float(((float) getHeapUsed() / (float) getMaxHeapSize()) * 100).intValue();
    }
    
    @SuppressWarnings("unchecked")
	JSONObject handle_activerequest(JSONObject returnObject){
    	//If there is no active request, send empty object.
    	if(!GearmanJobManager.getInstance().hasActiveRequest()){
    		return returnObject;
    	}
    	Request activeRequest = Request.newInstance(GearmanJobManager.getInstance().getCurrentRequest());
    	returnObject.put("Uuid", activeRequest.getId().toString());
    	returnObject.put("Type", activeRequest.getType());
    	returnObject.put("Ssid", activeRequest.getCaptureIdentifier());
    	returnObject.put("File", activeRequest.getFileName());
    	
    	JSONArray completedLists = new JSONArray();
    	for(Entry<UUID, String> entry : GearmanJobManager.getInstance().getCompletedPasswordLists().entrySet()){
    		completedLists.add(new String(entry.getValue()));
    	}
    	returnObject.put("Completedplist",completedLists);
    	
    	JSONArray pendingLists = new JSONArray();
    	for(String s : GearmanJobManager.getInstance().getPendingPasswordLists()){
    		pendingLists.add(new String(s));
    	}
    	returnObject.put("Pendingplist", pendingLists);
    	
    	returnObject.put("Runningplist", GearmanJobManager.getInstance().getCurrentList());
    	returnObject.put("Completedchunks", GearmanJobManager.getInstance().getCurrentPasswordListJobsComplete());
    	returnObject.put("Totalchunks", GearmanJobManager.getInstance().getCurrentPasswordListJobsTotal());
    	return returnObject;
    }
    
    

	
    
}