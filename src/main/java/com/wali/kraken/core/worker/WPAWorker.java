package com.wali.kraken.core.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wali.kraken.config.PreStartupDependencyConfig;
import com.wali.kraken.domain.overwire.Job;
import com.wali.kraken.domain.overwire.Reply;
import com.wali.kraken.enumerations.RequestType;
import com.wali.kraken.services.ServiceFunctions;
import org.apache.tomcat.util.codec.binary.Base64;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Created by Wali on 12/3/2017.
 */
public class WPAWorker implements GearmanFunction {

    private ServiceFunctions serviceFunctions;
    private Logger log = LoggerFactory.getLogger(WPAWorker.class);
    private ObjectMapper objectMapper;

    private String aircrackInvocationString;

    private Path pathToPCapFile;
    private Path pathToCandidateValueListFile;

    WPAWorker(ServiceFunctions serviceFunctions,
              Environment environment,
              PreStartupDependencyConfig preStartupDependencyConfig){
        this.serviceFunctions = serviceFunctions;
        this.objectMapper = new ObjectMapper();

        String krakenTempFolder = environment.getProperty("kraken.tmp-folder.base");
        String WPAworkerFolderPrefix = "wpa-temp-folder";
        pathToPCapFile = Paths.get(krakenTempFolder, WPAworkerFolderPrefix, "pcap.cap");
        pathToCandidateValueListFile = Paths.get(krakenTempFolder, WPAworkerFolderPrefix, "candidate-value-list.txt");

        aircrackInvocationString = preStartupDependencyConfig.getAircrackInvocationString();
    }

    @Override
    public byte[] work(String function, byte[] jobData, GearmanFunctionCallback gearmanFunctionCallback) throws Exception {
        log.info("Got WPA Job");
        if(RequestType.valueOf(function) != RequestType.WPA)
            throw new RuntimeException("WPA Function was triggered but function called was " + function );

        // Deserialize Job
        Job job = serviceFunctions.deserializeJob(jobData);
        Map<String,String> serializedMap = objectMapper.readValue(job.getMetadataMap(), new TypeReference<Map<String, String>>() {});
        if(!serializedMap.containsKey("SSID"))
            throw new RuntimeException("WPA Function was triggered but metadata map did not have SSID");
        byte[] pCapFile = Base64.decodeBase64(job.getToMatchValueInBase64());
        String jobId = job.getQueueNumber() + "-" +
                job.getCandidateValueListDescriptorQueueNumber() + "-"
                + job.getCrackRequestQueueNumber();

        log.info("Starting to process WPA job with id {}", jobId);

        // Test for Valid Crack
        serviceFunctions.testForValidCrack(pCapFile, "worker-temp-folder", serializedMap.get("SSID"));

        // Write Password Capture To Disk
        if (!Files.exists(pathToPCapFile.getParent()))
            Files.createDirectories(pathToPCapFile.getParent());
        Files.write(pathToPCapFile, pCapFile, StandardOpenOption.CREATE);

        // Write Candidate Value List to Disk
        if (!Files.exists(pathToCandidateValueListFile.getParent()))
            Files.createDirectories(pathToCandidateValueListFile.getParent());
        FileWriter fileWriter = new FileWriter(pathToCandidateValueListFile.toFile(),false);
        String[] candidateValueArray = job.getColonDelimitedCandidateValues().split(":");
        for(String candidateValue : candidateValueArray ){
            byte[] byteValue = candidateValue.getBytes(job.getCharSet());
            fileWriter.write(new String(byteValue));
            fileWriter.write("\n");
        }
        fileWriter.close();

        // Crack
        String result = crack(serializedMap.get("SSID"));

        // Clean Up
        Files.deleteIfExists(pathToPCapFile);
        Files.deleteIfExists(pathToCandidateValueListFile);

        // Send Reply
        Reply reply;
        if(result == null)
            reply = new Reply(false, null);
        else
            reply = new Reply(true, result);

        log.info("Sending Reply for Job with id {}",jobId);

        return serviceFunctions.serializeReply(reply);
    }


    private String crack(String SSID) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(aircrackInvocationString,
                pathToPCapFile.toString(),
                "-b",
                SSID, "-w",
                pathToCandidateValueListFile.toString() );
        Process process = pb.start();
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = br.readLine()) != null){
            if(line.contains("Current passphrase")){
                line = line.replaceAll(".*:", "");
                break;
            }
            else if (line.contains("Passphrase not in dictionary")){
                line = null;
                break;
            }
        }
        br.close();
        return line;
    }

}
