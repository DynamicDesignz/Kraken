package com.wali.kraken.services;

import com.wali.kraken.config.Constants;
import com.wali.kraken.config.PreStartupDependencyConfig;
import com.wali.kraken.domain.core.JobDescriptor;
import com.wali.kraken.domain.overwire.Job;
import com.wali.kraken.domain.overwire.Reply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ServiceFunctions {

    private Environment environment;

    private String aircrackInvocationString;

    private Logger log = LoggerFactory.getLogger(ServiceFunctions.class);

    @Autowired
    public ServiceFunctions(Environment environment, PreStartupDependencyConfig preStartupDependencyConfig) {
        this.environment = environment;
        this.aircrackInvocationString = preStartupDependencyConfig.getAircrackInvocationString();
        if (aircrackInvocationString == null)
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

    /**
     * Sends Text Command to Gearman Server
     * <p>
     * Returns string response from the server
     * <p>
     * If String is empty, then failed
     */
    public String sendTextCommandToGearmanServer(String command) {
        int gearmanServerPort = Integer.parseInt(
                environment.getProperty("gearman.server.port", "4730"));
        Socket pingSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        String ret = "";
        try {
            pingSocket = new Socket("127.0.0.1", gearmanServerPort);
            out = new PrintWriter(pingSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(pingSocket.getInputStream()));
            out.println(command);
            while (true) {
                final String line = in.readLine();
                if (line == null || line.endsWith(".")) {
                    break;
                } else if (line.equals(Constants.GearmanVersion)) {
                    ret = ret + line;
                    break;
                }
                ret = ret + line;
            }
            out.close();
            in.close();
            pingSocket.close();
        } catch (IOException e) {
            log.error("Gearman Server is Not Online!");
        }
        return ret;
    }

    /*


     */
    public String generateJobDescriptorKey(JobDescriptor jobDescriptor) {
        if (jobDescriptor.getQueueNumber() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Queue Number");
        if (jobDescriptor.getCandidateValueListDescriptor() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Candidate Value List Descriptor");
        if (jobDescriptor.getCandidateValueListDescriptor().getQueueNumber() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Candidate Value List Descriptor Queue Number");
        if (jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Crack Request Descriptor");
        if (jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getQueueNumber() == null)
            throw new RuntimeException("Cannot Generate Job Descriptor Key without Crack Request Descriptor Queue Number");
        return jobDescriptor.getCandidateValueListDescriptor().getCrackRequestDescriptor().getQueueNumber() +
                "-" +
                jobDescriptor.getCandidateValueListDescriptor().getQueueNumber() +
                "-" +
                jobDescriptor.getQueueNumber();
    }

    public long[] getJobDescriptorFromKey(String key) {
        String[] asStringTokens = key.split("-");
        long[] asLongTokens = new long[asStringTokens.length];
        for (int i = 0; i < asLongTokens.length; i++) {
            asLongTokens[i] = Long.parseLong(asStringTokens[i]);
        }
        return asLongTokens;
    }

    public byte[] serializeReply(Reply obj) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] serializeJob(Job obj) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
            try (ObjectOutputStream o = new ObjectOutputStream(b)) {
                o.writeObject(obj);
            }
            return b.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Reply deserializeReply(byte[] bytes) {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return (Reply) o.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Job deserializeJob(byte[] bytes) {
        try (ByteArrayInputStream b = new ByteArrayInputStream(bytes)) {
            try (ObjectInputStream o = new ObjectInputStream(b)) {
                return (Job) o.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
