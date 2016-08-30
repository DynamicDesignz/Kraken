package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logger.LOG;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import objects.PasswordList;
import util.UtilityFunctions;
import constants.Constants;

public enum ConfigurationLoader {
	INSTANCE;
	public static ConfigurationLoader getInstance(){return INSTANCE;}

	Yaml yaml = null;

	private HashMap <String,PasswordList> availablePasswordLists = new HashMap<String,PasswordList>();
	private ArrayList <String> hostedAlgorithms= new ArrayList<String>();

	ConfigurationLoader(){
		yaml = new Yaml();

	}

	@SuppressWarnings("unchecked")
	public void loadConfigurations() throws Exception{
		Map<String, Object> configMap = (Map<String, Object>) yaml.load(new FileInputStream(new File("config.yml")));

		/** General Configurations **/

		//Setting Startup Mode
		Constants.StartupMode = (String) configMap.get("StartupMode");

		//Setting Misc Configurations
		Constants.TemporaryFolderLocation = (String) configMap.get("TemporaryFilesFolder");
		Files.createDirectories(Paths.get(Constants.TemporaryFolderLocation));
		
		//Web UI Port
		Constants.WebUIPort = (int) configMap.get("WebUIPort");
		
		//Loading Available Functions
		Constants.hostedFunctions = (List<String>) configMap.get("HostedFunctions");
		for (String function : Constants.hostedFunctions){
			hostedAlgorithms.add(function);
		}

		/** Server Configurations **/
		if(Constants.StartupMode.contains("Server")){

			//Setting Gearman Server Settings
			Constants.GearmanServerType = (String) configMap.get("GearmanServerType");
			Constants.GearmanVersion = (String) configMap.get("GearmanVersion");
			Constants.GearmanExecuteCommand = (String) configMap.get("GearmanExecuteCommand");

			//Setting Gearman Server Port
			Constants.GearmanServerPort = (int) configMap.get("GearmanServerPort");

			//Advanced Server Settings
			Constants.QueryGearmanWorkersInterval = (int) configMap.get("QueryGearmanWorkersInterval");
			Constants.SizeOfWorkerJob = (int) configMap.get("SizeOfWorkerJob");
			Constants.SearchPattern = (String) configMap.get("SearchPattern");

			//Loading Password Configurations
			LOG.getLogger().info("Loading Password Lists....");
			Path defaultListPath = Paths.get(Constants.TemporaryFolderLocation, "defaultList.txt");
			if(!Files.exists(defaultListPath)){
				InputStream is = this.getClass().getClassLoader().getResourceAsStream("password_list/defaultList.txt");
				Files.write(defaultListPath, IOUtils.toByteArray(is));
			}
			PasswordList defaultList = new PasswordList("defaultList.txt",defaultListPath.toString());
			availablePasswordLists.put("defaultList.txt", defaultList);
			
			if(!Files.exists(Paths.get(Constants.TemporaryFolderLocation, "PasswordList.db"))){
				UtilityFunctions.createEmptyPasswordListDB();
			}
			UtilityFunctions.loadPasswordListsFromDB();
		}

		/** Worker Configurations **/
		if(Constants.StartupMode.contains("Worker")){
			//Setting Gearman Server IP and Port
			Constants.JobServerIP = (String) configMap.get("JobServerIP");
			Constants.JobServerPort = (int) configMap.get("JobServerPort");
		}
	}

	public HashMap<String,PasswordList> getAvailablePasswordLists(){
		return availablePasswordLists;
	}

	public PasswordList getPasswordListFromName(String name){
		return availablePasswordLists.get(name);
	}

	public ArrayList<String> getHostedAlgorithms(){
		return hostedAlgorithms;
	}



}
