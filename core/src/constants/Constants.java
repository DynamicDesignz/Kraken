package constants;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Constants {
	
	/** Startup Mode Options */
	public static String StartupMode = "";
	public static final String ServerOnly = "ServerOnly";
	public static final String ServerAndWorker = "ServerAndWorker";
	public static final String WorkerOnly = "WorkerOnly";
	
	/** Gearman Server Options **/
	public static String GearmanServerType = "Java";
	public static String GearmanVersion = "0.6.6";
	public static String GearmanExecuteCommand = "java -jar /home/wali/workspace/Kraken/lib/java-gearman-service-0.6.6.jar";

	/** Port Configurations */
	public static int GearmanServerPort = 4730;
	public static int WebUIPort = 8080;
	
	/** Gearman Worker Options **/
	public static String JobServerIP = "127.0.0.1";
	public static int JobServerPort = 4730;
	
	/** Directory Configurations */
	public static String PasswordListFolderLocation = "";
	public static String TemporaryFolderLocation = "";
		
	/** Advanced Configurations */
	public static List<String> PasswordListExtensions = new ArrayList<String>();
	public static int QueryGearmanWorkersInterval = 5000;
	public static int SizeOfWorkerJob = 10000;
	public static String SearchPattern = "Random";
	public static int InvocationAttempts = 3;
	
	/** AIR CRACK ERRORS */
	public static final String VALID_FILE = "Please specify a dictionary";
	public static final String QUIT_SEQUENCE = "Quitting aircrack-ng...";
	public static final String INVALID_MAC = "Invalid BSSID";
	public static final String INVALID_FILE = "Unsupported file format";
	
	/** SUPPORTED PASSWORD LIST ENCODINGS */
	public static Charset[] SupportedCharsets = new Charset[]{StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8, StandardCharsets.UTF_16, StandardCharsets.US_ASCII};
	
	/** New Request Responses */
	public static final String newrequest_response_success = "Request Added!";
	
	/** Add Password HTML Responses **/
	public static final String addpassword_response_success = "Password List Added!";
	
	//Added this temporarily Need to read from Configs
	public static List<String> hostedFunctions = new ArrayList<String>();
	
	/** Request Status */
	public static String status_undefined = "-";
	public static String status_running = "Running";
	public static String status_cancelled = "Cancelled";
	public static String status_complete_notfound = "Complete - Not Found";
	public static String status_complete_found = "Complete - Found";
	
}
