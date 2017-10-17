package com.wali.kraken.services;

import com.wali.kraken.config.Constants;
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

    public ServiceFunctions(Environment environment) {
        this.environment = environment;
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
        Path tempFilePath = Paths.get(
                environment.getProperty("kraken.TemporaryFilesFolder", "./kraken-tmp/"),
                "temp.cap");

        // Write the file out to a temporary location
        FileOutputStream fOut = new FileOutputStream(tempFilePath.toFile());
        fOut.write(passwordCaptureFileInBytes);
        fOut.close();

        // Use Process Builder to check if the file is valid
        ProcessBuilder pb = new ProcessBuilder("aircrack-ng", tempFilePath.toString(), "-b", SSID);
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
            throw new RuntimeException("Aircrack process unexpectedly shut down without " +
                    "validating file");
    }
}
