package com.wali.kraken.services;

import com.wali.kraken.config.Constants;
import com.wali.kraken.config.PreStartupDependencyConfig;
import com.wali.kraken.domain.core.JobDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ServiceFunctions {

    private Environment environment;

    private String aircrackInvocationString; ;

    @Autowired
    public ServiceFunctions(Environment environment, PreStartupDependencyConfig preStartupDependencyConfig) {
        this.environment = environment;
        this.aircrackInvocationString = preStartupDependencyConfig.getAircrackInvocationString();
        if(aircrackInvocationString == null)
            throw new RuntimeException("Aircrack invocation string was null which should not be possible");
    }

    /**
     * Tests if file is a valid password capture file that can be processed by AirCrackNG
     *
     * @param passwordCaptureFileInBytes Password capture file (usually a .cap) in bytes
     * @param SSID                       The SSID we are looking for (this file may have many SSID hashes captured)
     * @throws IOException throws IOException if
     */
    public void testForValidCrack(byte[] passwordCaptureFileInBytes, String SSID) throws IOException {
        // If byte array is null, return empty File
        if (passwordCaptureFileInBytes == null)
            throw new RuntimeException("Empty File");

        // Create a Path
        Path tempFilePath = Paths.get(environment.getProperty("kraken.tmp-folder.base"), "temp.cap");

        // Write the file out to a temporary location
        FileOutputStream fOut = new FileOutputStream(tempFilePath.toFile());
        fOut.write(passwordCaptureFileInBytes);
        fOut.close();

        // Use Process Builder to check if the file is valid
        ProcessBuilder pb = new ProcessBuilder(aircrackInvocationString, tempFilePath.toString(), "-b", SSID);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String error = null;
        while ((line = br.readLine()) != null) {
            if (line.contains(Constants.VALID_FILE))
                error = "";
            else if (line.contains(Constants.INVALID_MAC))
                error = "The BSSID provided was not present in the file";
            else if (line.contains(Constants.INVALID_FILE))
                error = "The File Format is not a .cap File";
        }
        br.close();

        // Delete the temporary file
        Files.deleteIfExists(tempFilePath);

        if (error == null)
            throw new RuntimeException("Aircrack process unexpectedly shut down without validating file");
    }

    /*


     */
    public String generateJobDescriptorKey(JobDescriptor jobDescriptor){
        if(jobDescriptor.getQueueNumber() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Queue Number");
        if(jobDescriptor.getCandidateValueListDescriptor() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Candidate Value List Descriptor");
        if(jobDescriptor.getCandidateValueListDescriptor().getQueueNumber() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Candidate Value List Descriptor Queue Number");
        if(jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Crack Request Descriptor");
        if(jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getQueueNumber() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Crack Request Descriptor Queue Number");
        return jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getQueueNumber() +
                "-" +
                jobDescriptor.getCandidateValueListDescriptor().getQueueNumber() +
                "-" +
                jobDescriptor.getQueueNumber();
    }

    public long[] getJobDescriptorFromKey(String key){
        String[] asStringTokens = key.split("-");
        long[] asLongTokens = new long[asStringTokens.length];
        for(int i = 0; i<asLongTokens.length; i++){
            asLongTokens[i] = Long.parseLong(asStringTokens[i]);
        }
        return asLongTokens;
    }
}
