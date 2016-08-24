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
	public static final String response_success = "<html>" +
												"<head></head>" +
												"<body>" +
													"<center><div><input type=\"text\"" +
														"style=\"text-align:center;width:150px;border:2px solid #dadada;" +
														"border-radius:7px;font-size:14px;padding:5px;outline:none;border-color:#9ae59a;" +
														"box-shadow:0 0 10px #9ae59a;\" value=\"Success\" readonly>" +
													"</div></center>" +
												"</body>" +
											"</html>" ;
	
	public static final String response_fail_part1 = "<html>" +
											"<head></head>" +
											"<body>" +
												"<center><div><input type=\"text\"" +
														"style=\"text-align:center;width:150px;border:2px solid #dadada;" +
														"border-radius:7px;font-size:14px;padding:5px;outline:none;border-color:#ff9999;" +
														"box-shadow:0 0 10px #ff9999;\" value=\"Failed - ";
														
	public static final String response_fail_part2 =  "\" + readonly>" +
													"</div></center>" +
											"</body>" +
										"</html>" ;
	
	//Added this temporarily Need to read from Configs
	public static List<String> hostedFunctions = new ArrayList<String>();
	
	/** Request Status */
	public static String status_undefined = "-";
	public static String status_running = "Running";
	public static String status_cancelled = "Cancelled";
	public static String status_complete_notfound = "Complete - Not Found";
	public static String status_complete_found = "Complete - Found";
	
}
