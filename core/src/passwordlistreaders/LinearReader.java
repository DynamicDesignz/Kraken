package passwordlistreaders;

import java.util.Scanner;
import java.util.Vector;

import objects.JobDescripter;
import objects.Messages.Job.Builder;
import objects.Request;

public class LinearReader extends PasswordFileReader{
	Scanner scanner;
		
	public LinearReader(Request cR, String cL) throws Exception{
		super(cR,cL);
		scanner = new Scanner(passwordList.getPath());
		System.out.println("INFO: Linear Scanner Opened");
	}
	
	@Override
	protected int indexOfNextJob(Vector<JobDescripter> arr) {
		return 0;
	}

	@Override
	protected Builder readIntoJobBuilder(Builder jobBuilder, JobDescripter jobDescripter) throws Exception {
		long range  = (jobDescripter.End - jobDescripter.Start) + 1;
		for(long i=0; i<range; i++){
			jobBuilder.addValuestotest(scanner.nextLine());
		}
		
		if(!scanner.hasNext()){
			System.out.println("INFO: Linear Scanner Closed");
			scanner.close();
		}
		
		return jobBuilder;
	}

}
