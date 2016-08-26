package managers.workermgr;

import java.util.Arrays;

import logger.LOG;
import managers.jobmgr.GearmanJobManager;
import util.UtilityFunctions;
import constants.Constants;


public enum GearmanWorkerManager {
	INSTANCE;
	public static GearmanWorkerManager getInstance(){return INSTANCE;}
	
	private Thread t;
	private int onlineWorkerCount;
	private int activeWorkerCount;
	private int difference;
	private Object Lock;
	
	
	GearmanWorkerManager(){
		Lock = new Object();
		onlineWorkerCount = 0;
		difference = 0;
		t = new Thread("Worker Manager"){
			public void run(){
				while(true){
					try{
						String output = UtilityFunctions.sendTextCommandToGearmanServer("status");
						if(output == null){System.out.println("WARN : Could not Reach Gearman Server");throw new RuntimeException();}
						String[] workers = output.split("\t");
							
						//Online Workers
						synchronized(Lock){						
							int newWorkerCount = Integer.parseInt(workers[3]);
							difference = newWorkerCount - onlineWorkerCount;
							if(difference < 0){
								LOG.getLogger().info("Adjusting for reduced Worker Count");
							}
							else if(difference > 0){
								LOG.getLogger().info("Adjusting for increased Worker Count");
								for (int i = 0; i<difference; i++){GearmanJobManager.getInstance().newWorkerHasJoined();}
								difference = 0;
							}
							onlineWorkerCount = newWorkerCount;
						}
						
						//Active Workers
						activeWorkerCount = Integer.parseInt(workers[2]);
					}
					catch(Exception e){}
					try {Thread.sleep(Constants.QueryGearmanWorkersInterval);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}
		};
	}
	
	public void startGearmanWorkerManager(){
		t.start();
	}
	
	/**
	 * shouldSendNextJob() Decides if next Job Should be sent.
	 * 
	 * Assert that difference cannot be greater than 0 (because all positive adjustments are done when detected)
	 * 
	 * If difference == 0 : We send a job because previous one was complete
	 * 
	 * Else if difference is negative : we do NOT send a job and increment counter.
	 * 
	 */
	public boolean shouldSendNextJob(){
		boolean shouldSendNextJob = true;
		synchronized(Lock){
			assert(!(difference>0));
			
			if (difference == 0){
				shouldSendNextJob = true;
			}
			else if(difference < 0){
				difference++;
				shouldSendNextJob =  false;
			}
		}
		return shouldSendNextJob;
	}
	
	public int getOnlineWorkers(){
		int workers = 0;
		synchronized(Lock){
			workers = onlineWorkerCount;
		}
		return workers;
		
	}
	
	public int getActiveWorkers(){
		return activeWorkerCount;
	}

}
