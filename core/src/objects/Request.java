package objects;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import util.UtilityFunctions;
import constants.Constants;

public class Request {
	
	//Request Object Variables
	private UUID id;
	private String type;
	private String captureIdentifier;
	private String fileName;
	private ArrayList<String> passwordLists;
	private String result = "Not Found";
	
	private String status = Constants.status_undefined;
		
	public Request(){
		passwordLists = new ArrayList<String>();
	}
	
	public Request(UUID id, String type, String captureIdentifier, String fileName, ArrayList<String> passwordLists){
		this.id = id;
		this.type = type;
		this.captureIdentifier = captureIdentifier;
		this.fileName = fileName;
		passwordLists = new ArrayList<String>();
		for(String s : passwordLists){
			this.passwordLists.add(new String(s));
		}
	}
	
	/** Copy Constructor */
	public static Request newInstance(Request r){
		return new Request(r.id,r.type,r.captureIdentifier,r.fileName,r.passwordLists);
	}

	public void createRequest(HttpServletRequest request) throws Exception{
		//Creating UUID
		id = UUID.randomUUID();
		
		byte fileContent[] = null;
		DiskFileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		List<FileItem> fileItems = upload.parseRequest(request);
		Iterator<FileItem> i = fileItems.iterator();
		while ( i.hasNext () ) {
			FileItem item = i.next();
			if ( item.isFormField () ){
				if(item.getFieldName().equals("request-type") ){
					//Assigning Type
					type = item.getString();
				}
				else if (item.getFieldName().equals("request-captureidentifier")){
					//Assigning BSSID
					captureIdentifier = item.getString();
				}
				else if (item.getFieldName().equals("request-plist")){
					//Adding PasswordLists
					passwordLists.add(item.getString());
				}
			}
			else{
				if(item.getSize() > 20*1024*1024 ){throw new RuntimeException("Reference File Too Large! ( > 20mb )");}
				//Assigning FileName
				fileName = UtilityFunctions.getUniqueFilename(item.getName());
				fileContent = item.get();
			}
		}
		
		if(passwordLists.isEmpty()){ throw new RuntimeException("No Password Lists Added");}
		
		//Testing File to see if SSID is valid
		UtilityFunctions.testForValidCrack(fileContent, captureIdentifier);
		
		//Setting Path to Reference File 
		Path pathToFile = Paths.get(Constants.TemporaryFolderLocation, fileName);
		Files.write(pathToFile, Base64.encodeBase64(fileContent));
	}
	
	
	@Override
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append("--- Printing Request ---\n");
		out.append("Request UUID is :" + id + "\n");
		out.append("Request file is : " + fileName + "\n");
		out.append("Request Type: " + type + "\n");
		out.append("Request SSID: " + captureIdentifier + "\n");
		out.append("Request Password Lists are:" + "\n");
		for(int i=0; i<passwordLists.size(); i++ ){
			Path p  = Paths.get(passwordLists.get(i));
			out.append("\t -"+ p.getFileName().toString()+ "\n");
		}
		out.append("--- End of Request --- " + "\n");
		return out.toString();
	}

	@Override
	public boolean equals(Object o) 
	{
		if (o instanceof Request) 
		{
			Request c = (Request) o;
			if(c.captureIdentifier.equals(captureIdentifier) && c.fileName.equals(fileName) && c.passwordLists.equals(passwordLists)){
				return true;
			}
		}
		return false;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getCaptureIdentifier() {
		return captureIdentifier;
	}

	public void setCaptureIdentifier(String captureIdentifier) {
		this.captureIdentifier = captureIdentifier;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public ArrayList<String> getPasswordLists() {
		return passwordLists;
	}

	public void setPasswordLists(ArrayList<String> passwordLists) {
		this.passwordLists = passwordLists;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
	
	
	
}
