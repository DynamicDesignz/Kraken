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
import worker.KrakenWorker;



public class WorkerWebUIServlet extends HttpServlet
{
	private static final long serialVersionUID = -4398153267502740296L;
	private Request currentRequest;
	
    public WorkerWebUIServlet(){
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
		
		returnObject.put("krakenTime", workerKrakenTime());
		
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
    
	private String workerKrakenTime(){
		long time_at_start = KrakenWorker.time_at_start;
		long cracking_time=0;
		String time = null;
		
		if(time_at_start>0){
			cracking_time = System.currentTimeMillis()-time_at_start;
			
			if (TimeUnit.MILLISECONDS.toMinutes(cracking_time)<60){
				time = Long.toString(TimeUnit.MILLISECONDS.toMinutes(cracking_time)) + " M";
			}else if(TimeUnit.MICROSECONDS.toHours(cracking_time)<24){
				time = Long.toString(TimeUnit.MILLISECONDS.toHours(cracking_time))+" H";
			}else if (TimeUnit.MILLISECONDS.toDays(cracking_time)<30){
				time = Long.toString(TimeUnit.MILLISECONDS.toDays(cracking_time)) + " D";
			}
		}else {
			time = "0 M";
		}
		return time;
	}
    
}