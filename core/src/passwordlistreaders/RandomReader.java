package passwordlistreaders;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Vector;
import java.util.stream.Stream;

import config.ConfigurationLoader;
import constants.Constants;
import objects.JobDescripter;
import objects.Messages;
import objects.Messages.Job.Builder;
import objects.PasswordList;
import objects.Messages.Job;
import objects.Request;

public class RandomReader extends PasswordFileReader {
	
	private Random randomGenerator;
	FileOutputStream fOut;

	public RandomReader(Request cR, String cL) throws Exception{
		super(cR,cL);
		randomGenerator = new Random();
	}

	@Override
	protected int indexOfNextJob(Vector<JobDescripter> arr) {
		return randomGenerator.nextInt(arr.size());
	}

	@Override
	protected Builder readIntoJobBuilder(Builder jobBuilder, JobDescripter jobDescripter) throws Exception {
		//Read Lines From the PasswordList
		Stream<String> lines = null;
		try {
			lines = Files.lines(passwordList.getPath(),passwordList.getCharset());
			lines = lines.skip(jobDescripter.Start-1);
			lines = lines.limit(Constants.SizeOfWorkerJob);
			//Actual Read Now
			Object[] candidateValues = lines.toArray();
			for(Object value : candidateValues){jobBuilder.addValuestotest((String) value);}
			
		} catch (IOException e) {throw new RuntimeException(e);}
		finally{
			if(lines!=null){lines.close();}
		}
		return jobBuilder;
	}

}
