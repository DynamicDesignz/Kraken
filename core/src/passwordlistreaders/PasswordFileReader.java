package passwordlistreaders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

import config.ConfigurationLoader;
import constants.Constants;
import objects.JobDescripter;
import objects.Messages;
import objects.PasswordList;
import objects.Request;
import objects.Messages.Job;
import util.Pair;

public abstract class PasswordFileReader {
	
	Request currentRequest;
	PasswordList passwordList;
	
	public PasswordFileReader(Request cR, String cL) throws Exception {
		currentRequest = cR;
		passwordList = ConfigurationLoader.getInstance().getPasswordListFromName(cL);
		if(passwordList == null){
			throw new RuntimeException("Could Not Find List!");
		}
	}
	
	public  Pair<Messages.Job,Integer> getNextJob(Vector<JobDescripter> pendingJobList){
		try{
			//Get Index of Next Job in the Job Array
			int index = indexOfNextJob(pendingJobList);
			JobDescripter jd = (JobDescripter) pendingJobList.get(index);
			
			//Creating ProtoBuf Job From local object JobDescripter
			//Building ProtoBuf Job
			Messages.Job.Builder jobBuilder = Messages.Job.newBuilder();
			//Set UUID
			jobBuilder.setId(jd.identifier.toString());
			//Set Type
			jobBuilder.setType(jd.type);
			//Set Identifier
			jobBuilder.setSsid(currentRequest.getCaptureIdentifier());
			//Read Capture File (file to brute force)
			Path pathToCaptureFile = Paths.get(Constants.TemporaryFolderLocation , currentRequest.getFileName());
			String captureFileContent = new String(Files.readAllBytes(pathToCaptureFile),StandardCharsets.UTF_8);
			jobBuilder.setReffile(captureFileContent);
			//Set List File Character Set
			jobBuilder.setCharset(passwordList.getCharset().name());
			
			jobBuilder = readIntoJobBuilder(jobBuilder, jd);
			return new Pair<Job, Integer>(jobBuilder.build(), index);
		}
		catch(Exception e){
			e.printStackTrace();
			//Return Null if something bad happens
			return null;
		}
		
	}
	
	protected abstract int indexOfNextJob(Vector<JobDescripter> arr);
	
	protected abstract Messages.Job.Builder readIntoJobBuilder(Messages.Job.Builder jobBuilder, JobDescripter jobDescripter) throws Exception;


}
