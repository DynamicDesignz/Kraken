package servlet;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import managers.jobmgr.GearmanJobManager;
import objects.Request;



public class WorkerWebUIServlet extends HttpServlet
{
	private static final long serialVersionUID = -4398153267502740296L;
	private long time_at_start;
	private Request currentRequest;
	
    public WorkerWebUIServlet(){
    	time_at_start=System.currentTimeMillis();
    }
    
    /*
     * Handles All Update Requests From Web UI
     */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		JSONObject returnObject = new JSONObject();
		System.out.println("the value of request is "+request.toString());

		String typeParameter = request.getParameter("type");
		System.out.println("the value of typeParameter is "+typeParameter);
 
		if(typeParameter == null) {
			// Do nothing
		}else if(typeParameter.equals("status")){
			// Update Web UI Status
			response.getWriter().println(handle_status(returnObject));
		}else{
			System.out.println("Unknown Request from Web UI");
			response.sendError(500);
		}
    }
	
	@SuppressWarnings("unchecked")
	JSONObject handle_status(JSONObject returnObject) {
		// Compute how long the Worker has been cracking
		long cracking_time = System.currentTimeMillis()-time_at_start;
		String time = null;
		
		if (TimeUnit.MILLISECONDS.toMinutes(cracking_time)<60){
			time = Long.toString(TimeUnit.MILLISECONDS.toMinutes(cracking_time)) + " M";
		}else if(TimeUnit.MICROSECONDS.toHours(cracking_time)<24){
			time = Long.toString(TimeUnit.MILLISECONDS.toHours(cracking_time))+" H";
		}else if (TimeUnit.MILLISECONDS.toDays(cracking_time)<30){
			time = Long.toString(TimeUnit.MILLISECONDS.toDays(cracking_time)) + " D";
		}
		
		returnObject.put("krakenTime", time);
		
		if(GearmanJobManager.getInstance().hasActiveRequest()){
			// currentRequest that the Worker has been working on
			currentRequest = Request.newInstance(GearmanJobManager.getInstance().getCurrentRequest());
			
			returnObject.put("currentId", currentRequest.getId().toString());
			returnObject.put("currentType", currentRequest.getType());
			returnObject.put("currentSsid", currentRequest.getCaptureIdentifier());
			returnObject.put("currentCapfile", currentRequest.getFileName());
		}
		
		return returnObject;
	}
    
    
}