package main;

import java.net.URI;
import java.net.URL;

import logger.LOG;
import managers.jobmgr.GearmanJobManager;
import managers.servermgr.GearmanServerManager;
import managers.workermgr.GearmanWorkerManager;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import config.ConfigurationLoader;
import constants.Constants;
import servlet.ServerWebUIServlet;
import servlet.WorkerWebUIServlet;
import worker.KrakenWorker;

public class Kraken {

	static ServerWebUIServlet ServerUIServlet = null;
	static WorkerWebUIServlet WorkerUIServlet = null;


	public static void main(String[] args) throws Exception{
		//Creating Logger 
		LOG logger = new LOG();
		//Load Configurations
		LOG.getLogger().info("Loading Configurations....");
		try{
			ConfigurationLoader.getInstance().loadConfigurations();
		}
		catch(Exception e){
			LOG.getLogger().error("Could Not Load Configurations. Exiting");
			e.printStackTrace();
			System.exit(1);
		}
		LOG.getLogger().info("Done! Startup Mode : " + Constants.StartupMode);

		//Create New Jetty Server
		Server server = new Server(Constants.WebUIPort);

		//Getting Web URI
		ClassLoader cl = Kraken.class.getClassLoader();
		URL webRootLocation = cl.getResource("web/ServerDashboard.html");
		if (webRootLocation == null)
		{
			throw new IllegalStateException("Unable to determine webroot URL location");
		}
		URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/ServerDashboard.html$","/"));

		//Setting Resources
		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setDirectoriesListed(true);
		resourceHandler.setResourceBase(webRootUri.toASCIIString());
		//Display ServerDashboard as welcome file if Server or ServerAndWorker
		if(Constants.StartupMode.contains("Server")){
			resourceHandler.setWelcomeFiles(new String[]{ "ServerDashboard.html" });
		}
		else{
			resourceHandler.setWelcomeFiles(new String[]{ "WorkerDashboard.html" });
		}
		
		//Setting Servlet
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		if(Constants.StartupMode.contains("Server")){
			ServerUIServlet = new ServerWebUIServlet();
			context.addServlet(new ServletHolder(ServerUIServlet), "/serverwebui");
		}
		if(Constants.StartupMode.contains("Worker")){	
			WorkerUIServlet = new WorkerWebUIServlet();
			context.addServlet(new ServletHolder(WorkerUIServlet), "/workerwebui");
		}		

		//Setting Resources and Servlet to the Server
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resourceHandler, context});
		server.setHandler(context);
		server.setHandler(handlers);

		//Starting the Gearman Server if on Server Mode
		if(Constants.StartupMode.contains("Server")){
			LOG.getLogger().info("Starting GearmanServerManager....");
			GearmanServerManager.getInstance().startGearmanServer();
			LOG.getLogger().info("Done!");

		}
		
		//Starting Workers If it's on Worker Mode
		if(Constants.StartupMode.contains("Worker")){
			LOG.getLogger().info("Starting Worker....");
			KrakenWorker kWorker = new KrakenWorker();
			kWorker.createWorker();
			LOG.getLogger().info("Done!");
		}

		//Starting the Worker, and Job Manager
		if(Constants.StartupMode.contains("Server")){
			LOG.getLogger().info("Starting GearmanJobManager....");
			GearmanJobManager.getInstance().startGearmanJobManager();
			GearmanJobManager.getInstance().passResultServlet(ServerUIServlet);
			LOG.getLogger().info("Done!");
			LOG.getLogger().info("Starting GearmanWorkerManager....");
			GearmanWorkerManager.getInstance().startGearmanWorkerManager();
			LOG.getLogger().info("Done!");
		}

		//Starting Kraken
		LOG.getLogger().info("Starting Kraken....");
		server.start();
		server.join();
	}

}
