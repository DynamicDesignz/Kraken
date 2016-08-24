/**
 * 
 */
package managers.jobmgr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import logger.LOG;
import managers.workermgr.GearmanWorkerManager;
import objects.Messages;
import objects.JobDescripter;
import objects.PasswordList;
import objects.Request;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanServer;

import passwordlistreaders.LinearReader;
import passwordlistreaders.PasswordFileReader;
import passwordlistreaders.RandomReader;
import servlet.ServerWebUIServlet;
import util.Pair;
import config.ConfigurationLoader;
import constants.Constants;


public enum GearmanJobManager {
	INSTANCE;
	public static GearmanJobManager getInstance(){return INSTANCE;}

	Thread t;
	private GearmanClient client;
	private Vector <Request> globalRequestList;
	private ServerWebUIServlet resultServlet = null;

	//Locks
	Object newRequestsLock;

	//Current Request Status Variables
	private Request currentRequest = null;
	private String currentList = null;
	private Vector<String> pendingPasswordLists;
	private ConcurrentHashMap<UUID,String> completedPasswordLists;
	private Vector <JobDescripter> pendingJobList;
	private ConcurrentHashMap <UUID,JobDescripter> runningJobList;
	private int currentPasswordListJobsComplete;
	private int currentPasswordListJobsTotal;
	private PasswordFileReader pWordFileReader;
	private TimerTask runningJobTimeDaemon;
	
	private boolean cancelledFlag = false;

	GearmanJobManager(){

		//Initialize Gearman Variables
		Gearman gearman = Gearman.createGearman();
		client = gearman.createGearmanClient();
		GearmanServer server = gearman.createGearmanServer("127.0.0.1", Constants.GearmanServerPort);
		client.addServer(server);

		//Initialize Lists and Variables
		globalRequestList = new Vector<Request>();
		newRequestsLock = new Object();
		pendingPasswordLists = new Vector<String>();
		completedPasswordLists = new ConcurrentHashMap<UUID,String>();
		pendingJobList = new Vector<JobDescripter>();
		runningJobList = new ConcurrentHashMap<UUID,JobDescripter>();
		currentPasswordListJobsComplete = 0;
		currentPasswordListJobsTotal = 0;

		//Add T Thread's job
		t = new Thread("JobManager"){
			public void run(){
				try{JobManagerLoop();}
				catch(Exception e){
					System.err.println("ERROR : Fatal Error in Job Manager : "+ e.getMessage());
					e.printStackTrace();
					System.exit(1);
				}
			}
		};
		
		//Set Timer Daemon
		runningJobTimeDaemon = new TimerTask(){
			public void run(){
				incrementRunningTaskTimeAndRecover();
			}
		};
		new Timer().schedule(runningJobTimeDaemon, 0, 1000);
		
	}

	public void passResultServlet(ServerWebUIServlet rServ){
		resultServlet = rServ;
	}

	public void startGearmanJobManager(){
		t.start();
	}
	
	private boolean isDuplicateRequest(Request r){
		if(globalRequestList.contains(r)){return true;}
		if(currentRequest!=null){
			if(currentRequest.equals(r)){return true;}
		}
		return false;
	}

	public void addRequest(Request r) throws Exception{
		synchronized(newRequestsLock){
			LOG.getLogger().info("Adding Request {}",r.toString());
			if(isDuplicateRequest(r)){
				LOG.getLogger().error("Detected duplicate Request. Discarding...");
				throw new RuntimeException("Duplicate Request!");}
			//Notify JobManagerLoop if its waiting
			globalRequestList.add(r);
			newRequestsLock.notify();
		}
	}	

	private void JobManagerLoop() throws InterruptedException{
		while(true){

			synchronized(newRequestsLock){
				//Wait if No New Requests Are in Queue or busy with currentRequest
				while(globalRequestList.isEmpty() || currentRequest!=null){
					newRequestsLock.wait();
				}
				//Start New Request
				LOG.getLogger().info("Starting on Next Request");
				currentRequest = globalRequestList.firstElement();
				globalRequestList.remove(currentRequest);
				currentRequest.setStatus(Constants.status_running);
				setCancelledFlag(false);
			}

			//Print Request
			LOG.getLogger().info("Starting Request {}",currentRequest.toString());

			synchronized(this){
				//Copy PasswordLists from request to pending List
				for(String s : currentRequest.getPasswordLists()){
					pendingPasswordLists.add(s);
				}
			}

			//Create Job Descriptors For First List
			createJobDescriptersForNextPendingPasswordList();
			
			//If there are jobs, submit them to Gearman Workers			
			int jobsToSend = Math.min(GearmanWorkerManager.getInstance().getOnlineWorkers(),pendingJobList.size());// If the list is smaller than the number of workers	
			for(int i=0; i<jobsToSend; i++){submitJobToWorker();}
		}
	}

	/**
	 * Creates Job Descriptors For Next Pending PasswordList
	 * Creates PasswordFileReader
	 * @return True if Jobs Were Created : False if Request is complete
	 */
	private synchronized void createJobDescriptersForNextPendingPasswordList(){
		if(pendingPasswordLists.isEmpty()){return;}
		try{
			currentList = pendingPasswordLists.remove(0);
			PasswordList pList = ConfigurationLoader.getInstance().getPasswordListFromName(currentList);
			if(pList == null){throw new RuntimeException("Could Not Find List!");}

			for(long i=0; i<pList.getLineCount(); i=i+Constants.SizeOfWorkerJob){
				JobDescripter c = new JobDescripter(UUID.randomUUID(),currentList,i+1,Math.min(i+Constants.SizeOfWorkerJob, pList.getLineCount()),currentRequest.getType());
				pendingJobList.add(c);
				currentPasswordListJobsTotal++;
			}
			
			if(Constants.SearchPattern.equals("Optimize")){
				if(pList.getLineCount()< 200000000){
					pWordFileReader = new RandomReader(currentRequest,currentList);
				}else{
					pWordFileReader = new LinearReader(currentRequest,currentList);
				}
			}
			else if(Constants.SearchPattern.equals("Linear")){
				pWordFileReader = new LinearReader(currentRequest,currentList);
			}
			else if(Constants.SearchPattern.equals("Random")){
				pWordFileReader = new RandomReader(currentRequest,currentList);
			}
			
		}
		catch(Exception e){
			e.printStackTrace();
			LOG.getLogger().error("Password List " + currentList + " could not be found... Moving to next list");
			createJobDescriptersForNextPendingPasswordList();
		}
	}
	
	/**
	 *  submits Job to Worker
	 *  
	 *  Fetches Job Content from PasswordFileReader
	 *  
	 *  If the there was an exception and pair returns null, then there is something wrong with the Password List 
	 *  and we can skip this List
	 *  
	 *  We then remove job from the pendingJobList and it to the runningJobList and submit it to Gearman
	 */
	
	private synchronized void submitJobToWorker(){
		Pair<Messages.Job,Integer> pair= pWordFileReader.getNextJob(pendingJobList);
		if(pair == null){
			pendingJobList.clear();
			LOG.getLogger().error("Could not find next job from PasswordList");
			createJobDescriptersForNextPendingPasswordList();
		}
		
		JobDescripter jd = pendingJobList.get(pair.b);
		pendingJobList.remove(jd);
		runningJobList.put(jd.identifier,jd);
		client.submitJob(currentRequest.getType(), pair.a.toByteArray(), jd.identifier.toString(), new AsyncGearmanCallback());
		LOG.getLogger().info("Job with id {} was sent",jd.identifier.toString());
	}
	
	/**
	 * Sends Next Job to Gearman Server
	 *	Checks:
	 *	if pendingJobList is empty AND runningJobList is empty
	 *		if pendingPasswordList is empty
	 *			if isCancelledFlag
	 *				Request Complete, it was Cancelled
	 *			else
	 *				Request Complete, Password Not Found
	 *		else
	 *			Process the next Password List
	 *			Send Jobs
	 *	
	 *	else if pendingJobList is empty and runningJobList is NOT empty
	 *		Don't send any more jobs. We are waiting on the success/failure or current jobs
	 *	
	 *	else
	 *		Send a Job
	 */
	public synchronized void sendNextJob(){
		if(pendingJobList.isEmpty() && runningJobList.isEmpty()){
			if(pendingPasswordLists.isEmpty()){
				if(isCancelledFlag()){
					RequestComplete("Password Not Found", Constants.status_cancelled);
				}
				else{
					RequestComplete("Password Not Found", Constants.status_complete_notfound);
				}
				
			}
			else{
				completedPasswordLists.put(UUID.randomUUID(), currentList);
				currentPasswordListJobsComplete = 0;
				currentPasswordListJobsTotal = 0;
				createJobDescriptersForNextPendingPasswordList();
				int jobsToSend = Math.min(GearmanWorkerManager.getInstance().getOnlineWorkers(),pendingJobList.size());// If the list is smaller than the number of workers	
				for(int i=0; i<jobsToSend; i++){submitJobToWorker();}
			}
		}
		else if(pendingJobList.isEmpty() && !runningJobList.isEmpty()){
			//Do not submit a Job. We are waiting for all jobs of this request to return
			return;
		}
		else {
			submitJobToWorker();
		}

	}

	public synchronized void jobComplete(UUID jobIdentifier){
		currentPasswordListJobsComplete++;
		JobDescripter job = runningJobList.remove(jobIdentifier);
		if(job == null){
			LOG.getLogger().warn("Could not Find Job "+ jobIdentifier + " in Running List. Perhaps it was discarded");
		}
		LOG.getLogger().info("Job with id {} complete. Time to Run {} s",job.identifier.toString(),job.timeRunning);
	}

	public synchronized void recoverChunk(UUID jobIdentifier){
		JobDescripter job = runningJobList.remove(jobIdentifier);
		if(job == null){
			LOG.getLogger().error("Could not Find Job "+ jobIdentifier + " in Running List. Could Not Recover Job");
			return;
		}
		pendingJobList.add(job);
		LOG.getLogger().info("Recovered Job with id {}", jobIdentifier.toString());
	}
	
	public synchronized void PasswordFound(String pWord){
		RequestComplete(pWord,Constants.status_complete_found);
	}
	
	private void RequestComplete(String pWord, String request_status){
		pendingPasswordLists.clear();
		pendingJobList.clear();
		runningJobList.clear();
		completedPasswordLists.clear();
		currentPasswordListJobsComplete = 0;
		currentPasswordListJobsTotal = 0;
		//Delete Ref File
		try {Files.deleteIfExists(Paths.get(Constants.TemporaryFolderLocation,currentRequest.getFileName()));}catch (IOException e) {}
		Request result = Request.newInstance(currentRequest);
		result.setStatus(request_status);
		result.setResult(pWord);
		try{resultServlet.queueResultForWebUI(result);}catch(Exception e){System.out.println("ERR : Result Servlet was null");e.printStackTrace();}
		synchronized(newRequestsLock){
			currentRequest = null;
			newRequestsLock.notify();
		}
	}
	
	private synchronized void incrementRunningTaskTimeAndRecover(){
		for (Entry<UUID,JobDescripter> entry : runningJobList.entrySet()) {
			entry.getValue().timeRunning++;
			if(entry.getValue().timeRunning>300){
				LOG.getLogger().warn("Job with id {} has timed out. Recovering...",entry.getKey().toString());
				entry.getValue().timeRunning=0;
				//recoverChunk(entry.getKey());
			}
		}
	}
	
	public synchronized void cancelRequest(){
		pendingPasswordLists.clear();
		pendingJobList.clear();
		runningJobList.clear();
		completedPasswordLists.clear();
		setCancelledFlag(true);
	}

	public void newWorkerHasJoined(){
		if(currentRequest!= null){
			sendNextJob();
		}
	}

	public int getRequestQueueSize(){
		return globalRequestList.size();
	}

	public boolean hasActiveRequest(){
		if(currentRequest == null){
			return false;
		}
		return true;
	}

	public Request getCurrentRequest() {
		return currentRequest;
	}

	public String getCurrentList() {
		return currentList;
	}

	public ConcurrentHashMap<UUID,String> getCompletedPasswordLists() {
		return completedPasswordLists;
	}

	public Vector<String> getPendingPasswordLists() {
		return pendingPasswordLists;
	}

	public int getCurrentPasswordListJobsComplete() {
		return currentPasswordListJobsComplete;
	}

	public int getCurrentPasswordListJobsTotal() {
		return currentPasswordListJobsTotal;
	}

	public synchronized boolean isCancelledFlag() {
		return cancelledFlag;
	}

	public synchronized void setCancelledFlag(boolean cancelledFlag) {
		this.cancelledFlag = cancelledFlag;
	}
	


}
