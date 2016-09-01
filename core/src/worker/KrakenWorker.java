package worker;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import logger.LOG;
import objects.Messages;

import org.apache.commons.codec.binary.Base64;
import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

import util.UtilityFunctions;
import constants.Constants;

public class KrakenWorker implements GearmanFunction{

	Gearman gearman;
	GearmanServer server;
	GearmanWorker worker;
	Path pathToCapFile = Paths.get(Constants.TemporaryFolderLocation, "pcap.cap");
	Path pathToListFile = Paths.get(Constants.TemporaryFolderLocation, "pwfile.txt");
	
	public static long time_at_start;
	
	public void createWorker(){
		gearman = Gearman.createGearman();
		server = gearman.createGearmanServer(Constants.JobServerIP, Constants.JobServerPort);
		worker = gearman.createGearmanWorker();
		worker.setClientID("kraken-worker");
		for(String function :Constants.hostedFunctions){
			switch (function){
			case "WPA/WPA2":
				worker.addFunction(function, new KrakenWorker());
				break;
			default:
				LOG.getLogger().error("Cannot add function " + function + " . Work Function not Found");
				break;
			}
		}
		//worker.setLostConnectionPolicy(GearmanLostConnectionPolicy.);
		worker.setReconnectPeriod(10, TimeUnit.SECONDS);
		worker.setClientID("kraken-worker");
		worker.addServer(server);
	}

	@Override
	public byte[] work(String function, byte[] jobData, GearmanFunctionCallback arg2) throws Exception {
		Messages.Reply.Builder replyBuilder = Messages.Reply.newBuilder();
		switch (function){
		case "WPA/WPA2":
			LOG.getLogger().info("Got Job. Unpacking...");
			
			//Unparsing Job
			Messages.Job job = Messages.Job.parseFrom(jobData);
			LOG.getLogger().info("Job Id : "+job.getId());
			replyBuilder.setId(job.getId());
			
			//Testing if Cap File is Valid
			LOG.getLogger().info("Testing for Valid Cap File");
			UtilityFunctions.testForValidCrack(Base64.decodeBase64(job.getReffile()), job.getSsid());
			LOG.getLogger().info("Success!");
			//Creating Password File
			createPlist(job);
			//Create PcapFile
			createCapFile(job);
			
			//Time the worker begin to crack
			time_at_start=System.currentTimeMillis();
			
			//Crack
			LOG.getLogger().info("Cracking....");
			replyBuilder = crackJob(replyBuilder,job);
			
			break;
		default:
			LOG.getLogger().error("There was supported function called " + function);
			break;
		}
		
		LOG.getLogger().info("Sending Reply with ID : " + replyBuilder.getId());
		return replyBuilder.build().toByteArray();
	}


	public void createPlist(Messages.Job job) throws IOException{
		//Files.deleteIfExists(pathToListFile);
		FileWriter fileWriter = new FileWriter(pathToListFile.toFile(),false);
		for (int i=0; i<job.getValuestotestList().size(); i++){
			byte[] byteValue = job.getValuestotestList().get(i).getBytes(job.getCharset());
			fileWriter.write(new String(byteValue));
			fileWriter.write("\n");
		}
		fileWriter.close();
	}

	public void createCapFile(Messages.Job job) throws IOException{
		//Files.deleteIfExists(pathToCapFile);
		byte[] decodedBytes = Base64.decodeBase64(job.getReffile());
		Files.write(pathToCapFile, decodedBytes);
	}

	public Messages.Reply.Builder crackJob(Messages.Reply.Builder replyBuilder, Messages.Job job) throws IOException{
		ProcessBuilder pb = new ProcessBuilder("aircrack-ng", pathToCapFile.toString(), "-b", job.getSsid(), "-w", pathToListFile.toString() );
		Process process = pb.start();
		BufferedReader br = null;
		br = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null){			
			switch (line){
			case "KEY FOUND":
				//Add code
				System.out.println("Key Found Yeay!");
				replyBuilder.setStatus(false);
				break;
			case "Passphrase not in dictionary":
				replyBuilder.setStatus(false);
				System.out.println("Not Found");
				break;
			}
		}
		br.close();
		return replyBuilder;
	}



}
