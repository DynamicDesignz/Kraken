package com.wali.kraken.config;

import com.google.common.io.ByteStreams;
import com.wali.kraken.enumerations.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by Wali on 10/14/2017.
 */
@Configuration
public class PreStartupDependencyConfig {

    private ResourceLoader resourceLoader;
    private String temporaryFolderBase;
    private Logger log = LoggerFactory.getLogger(PreStartupDependencyConfig.class);

    private String aircrackInvocationString;

    private OS operatingSystem;

    public String getAircrackInvocationString() {
        return aircrackInvocationString;
    }

    public OS getOperatingSystem() {
        return operatingSystem;
    }

    @Autowired
    public PreStartupDependencyConfig(Environment environment, ResourceLoader resourceLoader) throws IOException, URISyntaxException {
        temporaryFolderBase = environment.getProperty("kraken.tmp-folder.base");
        this.resourceLoader = resourceLoader;

        // Create Directory
        Files.createDirectories(Paths.get(temporaryFolderBase));

        // Get OS
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        if(isWindows)
            // System is windows
            setupAircrackForWindows();
        else
            setupAircrackForLinux();

        // Copy default wpa password list
        copyFileToTemporaryDirectory(Constants.CANDIDATE_VALUE_LIST_DIRECTORY + "/" + "wpa" + "/",
                "default-list.txt", isWindows);
    }

    @PreDestroy
    public void deleteTemporaryDirectory() throws IOException {
        Files.walkFileTree(Paths.get(temporaryFolderBase), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyFileToTemporaryDirectory(String resourcePath, String filename, boolean isWindows) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath + filename);
        InputStream resourceInputStream = resource.getInputStream();
        byte[] fileAsArray = ByteStreams.toByteArray(resourceInputStream);
        File dir;
        if(isWindows)
            dir = new File(temporaryFolderBase + "\\" + resourcePath);
        else
            dir = new File(temporaryFolderBase + "/" + resourcePath);
        if (dir.exists()) dir.delete();
        dir.mkdirs();
        Files.write(Paths.get(dir.getAbsolutePath(), filename), fileAsArray);
    }

    private void setupAircrackForLinux() {
        // Setting Operating System
        operatingSystem = OS.LINUX;

        // Set Aircrack invocation String
        aircrackInvocationString = "aircrack-ng";

        // TODO : RUN AIRCRACK WITH COMMON COMMAND TO SEE IF ITS THERE

        // ELSE THROW EXCEPTION
//        ProcessBuilder pb = new ProcessBuilder(aircrackInvocationString, tempFilePath.toString(), "-b", SSID);
//        Process process = pb.start();
//        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
//        String line;
//        String error = null;
//        while ((line = br.readLine()) != null) {
//            if (line.contains(Constants.VALID_FILE))
//                error = "";
//            else if (line.contains(Constants.INVALID_MAC))
//                error = "The BSSID provided was not present in the file";
//            else if (line.contains(Constants.INVALID_FILE))
//                error = "The File Format is not a .cap File";
//        }
//        br.close();
    }

    private void setupAircrackForWindows() throws IOException{
        // Set OS as Windows
        operatingSystem = OS.WINDOWS;

        boolean is64Bit = (System.getenv("ProgramFiles(x86)") != null);
        if (is64Bit) {
            // 64 Bit Files
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "aircrack-ng-avx.exe", true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "aircrack-ng-avx2.exe",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygcrypto-1.0.0.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cyggcc_s-seh-1.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygpcre-1.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygsqlite3-0.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygssp-0.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygstdc++-6.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygwin1.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygz.dll",true);

            // Set Aircrack Invocation String
            aircrackInvocationString =
                    temporaryFolderBase + "\\" + "aircrack" + "\\" + "windows" + "\\" + "64bit" + "\\" + "aircrack-ng-avx.exe";

        } else {
            // 32 Bit Files
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "aircrack-ng-avx.exe",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "aircrack-ng-avx2.exe",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygcrypto-1.0.0.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cyggcc_s-1.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygpcre-1.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygsqlite3-0.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygssp-0.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygstdc++-6.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygwin1.dll",true);
            copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygz.dll",true);

            aircrackInvocationString =
                    temporaryFolderBase + "\\" + "aircrack" + "\\" + "windows" + "\\" + "32bit" + "\\" + "aircrack-ng-avx.exe";
        }
    }


}
