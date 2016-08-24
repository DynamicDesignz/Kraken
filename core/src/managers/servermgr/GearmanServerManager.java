package managers.servermgr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import logger.LOG;
import util.UtilityFunctions;
import constants.Constants;

public enum GearmanServerManager {
	INSTANCE;
	public static GearmanServerManager getInstance(){return INSTANCE;}
	ProcessBuilder pb;
	private Thread t = null;
	
	GearmanServerManager(){
		t = new Thread("GearmanServer"){
			public void run(){
				//Check if Gearman server is running. If not, start it
				String status = UtilityFunctions.sendTextCommandToServer("version");
				int invocations = 1;
				while(status.isEmpty()){
					LOG.getLogger().info("Gearman Server Invocation Attempt " + invocations);
					try{invokeServer();}catch(Exception e){e.printStackTrace();}
					status = UtilityFunctions.sendTextCommandToServer("version");
					LOG.getLogger().info("Failed. Trying " + (Constants.InvocationAttempts-invocations) + " more times");
					if((Constants.InvocationAttempts-invocations)<= 0){
						LOG.getLogger().info("Failed to Start Gearman. Check Gearman Settings.");
						System.exit(1);
					}
					invocations++;
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}	
		};
		t.setDaemon(true);
	}
	
	public void startGearmanServer(){
		t.start();
	}
	
//	private void invokeServer() throws IOException{
//		String[] args = Constants.GearmanExecuteCommand.split(" ");
//		ArrayList<String> argArray = new ArrayList<String>();
//		argArray.addAll(Arrays.asList(args));
//		argArray.add("-p");
//		argArray.add(Integer.toString(Constants.GearmanServerPort));
//		pb = new ProcessBuilder(argArray);
//		pb.start();
//		LOG.getLogger().info("Started "+ Constants.GearmanServerType + " Gearman Server at Port" + Constants.GearmanServerPort);
//	}
	
	private void invokeServer(){
		try{
			if(Constants.GearmanServerType.equals("Java")){runJavaGearman();}
			else if(Constants.GearmanServerType.equals("Native")){runNativeGearman();}
			else{throw new RuntimeException("Invalid Gearman Server Type. [It should be \"Native\" or \"Java\"]. Exiting!");}
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void runJavaGearman() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, IOException{
		Class classToLoad = Class.forName ("org.gearman.impl.Main");
		Method[] methods = classToLoad.getMethods();
		Object[] params = new String[4];
		params[0] = "-p";
	    params[1] = Integer.toString(Constants.GearmanServerPort);
		params[2] = "";
	    params[3] = "";
		methods[0].invoke(null, new Object[] {params});
	}
	
	private void runNativeGearman() throws IOException{
		String[] args = Constants.GearmanExecuteCommand.split(" ");
		ArrayList<String> argArray = new ArrayList<String>();
		argArray.addAll(Arrays.asList(args));
		argArray.add("-p");
		argArray.add(Integer.toString(Constants.GearmanServerPort));
		pb = new ProcessBuilder(argArray);
		pb.start();
		LOG.getLogger().info("Started "+ Constants.GearmanServerType + " Gearman Server at Port" + Constants.GearmanServerPort);
	}
}
