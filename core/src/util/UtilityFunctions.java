package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import logger.LOG;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.util.log.Log;

import constants.Constants;

public class UtilityFunctions {
	
	/**
	 * Sends Text Command to Gearman Server
	 * 
	 * Returns string response from the server
	 * 
	 * If String is empty, then failed
	 */
	public static String sendTextCommandToServer(String command){
		Socket pingSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		String ret = "";
		try {
			pingSocket = new Socket("127.0.0.1", Constants.GearmanServerPort);
			out = new PrintWriter(pingSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
			out.println(command);
			while (true) {
			    final String line = in.readLine();
			    if (line == null || line.endsWith(".")){break;}
			    else if( line.equals(Constants.GearmanVersion)){
			    	ret = ret + line;
			    	break;
			    }
			    ret = ret + line;
			}
			out.close();
			in.close();
			pingSocket.close();
		} catch (IOException e) {LOG.getLogger().error("Gearman Server is Not Online!");}
		return ret;
	}
	
	
	/**
	 * Returns the next available name for CAP File
	 */
	public static String getUniqueFilename( String name )
	{
		File file = new File(name);
	    String baseName = FilenameUtils.getBaseName( file.getName() );
	    String extension = FilenameUtils.getExtension( file.getName() );
	    int counter = 1;
	    while(file.exists())
	    {
	        file = new File( file.getParent(), baseName + "-" + (counter++) + "." + extension );
	    }
	    return file.getName();
	}

	/* 
	 * Test File
	 * Takes File Content, writes it to disks
	 * Attempts to run Air Crack NG to test if file runs
	 * Deletes File
	 * Throws Exception at failure 
	 */
	public static void testForValidCrack(byte[] packetCaptureContent, String ssid) throws Exception{
		if(packetCaptureContent == null) {throw new RuntimeException("Empty File");}
		Path tempFilePath = Paths.get(Constants.TemporaryFolderLocation,"temp.cap");
		FileOutputStream fOut = new FileOutputStream(tempFilePath.toFile());
		fOut.write(packetCaptureContent);
		fOut.close();
		ProcessBuilder pb = new ProcessBuilder("aircrack-ng", tempFilePath.toString(), "-b", ssid);
		Process process = pb.start();
		BufferedReader br = null;
		br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		String error = "Unknown";
		while ((line = br.readLine()) != null){
			if(line.contains(Constants.VALID_FILE)){error = "";}
			else if (line.contains(Constants.INVALID_MAC)){error = "The BSSID provided was not present in the file";}
			else if (line.contains(Constants.INVALID_FILE)){error = "The File Format is not a .cap File";}
		}
		br.close();
		Files.deleteIfExists(tempFilePath);
		if(!error.isEmpty()){
			throw new RuntimeException(error);
		}
	}
	
	public static boolean hasPasswordFileExtension(String name){
		for(int i=0; i<Constants.PasswordListExtensions.size(); i++){
			if(name.endsWith(Constants.PasswordListExtensions.get(i))){
				return true;
			}
		}
		return false;
	}
	
}
