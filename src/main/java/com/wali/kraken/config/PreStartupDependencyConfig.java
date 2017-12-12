package com.wali.kraken.config;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by Wali on 10/14/2017.
 */
@Configuration
public class PreStartupDependencyConfig {

    private ResourceLoader resourceLoader;
    private String temporaryFolderBase;
    private Logger log = LoggerFactory.getLogger(PreStartupDependencyConfig.class);

    private String aircrackInvocationString;

    private String operatingSystem;

    public String getAircrackInvocationString() {
        return aircrackInvocationString;
    }

    @Autowired
    public PreStartupDependencyConfig(Environment environment, ResourceLoader resourceLoader) throws Exception{
        temporaryFolderBase = environment.getProperty("kraken.tmp-folder.base");
        this.resourceLoader = resourceLoader;

        // Create Directory
        Files.createDirectories(Paths.get(temporaryFolderBase));

        // Get OS
        operatingSystem = System.getProperty("os.name");
        boolean isWindows = operatingSystem.startsWith("Windows");
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

    private String executeCommandInConsole(long timeout,
                                           TimeUnit timeUnit,
                                           String... commands) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process process = pb.start();
        if(timeout != 0 && timeUnit != null)
            process.waitFor(timeout, timeUnit);
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder("");
        while ((line = br.readLine()) != null){
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    private String executeCommandInConsole(String... commands) throws IOException, InterruptedException {
        return executeCommandInConsole(0,null , commands);
    }

    private void setupAircrackForLinux() throws IOException, InterruptedException {

        // Set Aircrack invocation String
        aircrackInvocationString = "aircrack-ng";

        //Test Aircrack
        String testAircrack = "";
        try{ testAircrack = executeCommandInConsole(aircrackInvocationString); }
        catch (Exception ignored){}
        if(testAircrack.contains("No file to crack specified."))
            return;

        // At this point Aircrack is not setup

        // Check for sudo access
        String testSudo = executeCommandInConsole(100, TimeUnit.MILLISECONDS, "sudo", "uname");
        if(!testSudo.contains("Linux"))
            throw new RuntimeException("There is no sudo access to setup Aircrack");

        // Test if Deb is supported
        String testForDeb = null;
        try{ testForDeb = executeCommandInConsole("cat", "/etc/debian_version"); }
        catch (Exception ignored){}
        if(Objects.equals(testForDeb, ""))
            throw new RuntimeException("Linux Operating System does not support deb setup");

        String testForDPKG = null;
        try{ testForDPKG = executeCommandInConsole("dpkg", "--version"); }
        catch (Exception ignored){}
        if(Objects.equals(testForDPKG, ""))
            throw new RuntimeException("Linux Operating System does not support dpkg");

        String testForAPT = null;
        try{ testForAPT = executeCommandInConsole("apt-get", "--version"); }
        catch (Exception ignored){}
        if(Objects.equals(testForAPT,""))
            throw new RuntimeException("Lixux Operating System does not support apt");


        boolean is64Bit = executeCommandInConsole("uname", "-i").contains("x86_64");
        if(is64Bit){
            copyFileToTemporaryDirectory("aircrack/linux/deb/64bit/", "aircrack-ng_1.1-6_amd64.deb", false);
            String aircrackDebLocation = temporaryFolderBase + "/" + "aircrack" + "/" + "linux" + "/" + "deb" +
                    "/" + "64bit" + "/" + "aircrack-ng_1.1-6_amd64.deb";

            String dpkgString = executeCommandInConsole("sudo", "dpkg", "-i", aircrackDebLocation);
            if(Objects.equals(dpkgString, ""))
                throw new RuntimeException("Could not perform a dpkg -i installation");

            String aptInstallion = executeCommandInConsole("sudo", "apt-get", "-f", "install");
            if(Objects.equals(aptInstallion, ""))
                throw new RuntimeException("Could not perform a apt-get -f install");
        }
        else {
            copyFileToTemporaryDirectory("aircrack/linux/deb/32bit/", "aircrack-ng_1.1-6_i386.deb", false);
            String aircrackDebLocation = temporaryFolderBase + "/" + "aircrack" + "/" + "linux" + "/" + "deb" +
                    "/" + "32bit" + "/" + "aircrack-ng_1.1-6_i386.deb";

            String dpkgString = executeCommandInConsole("sudo", "dpkg", "-i", aircrackDebLocation);
            if(Objects.equals(dpkgString, ""))
                throw new RuntimeException("Could not perform a dpkg -i installation");

            String aptInstallion = executeCommandInConsole("sudo", "apt-get", "-f", "install");
            if(Objects.equals(aptInstallion, ""))
                throw new RuntimeException("Could not perform a apt-get -f install");
        }


        // Test Run Aircrack
        testAircrack = executeCommandInConsole(aircrackInvocationString);
        if(!testAircrack.contains("No file to crack specified."))
            throw new RuntimeException("Aircrack was setup but failed invocation test!");
    }

    private void setupAircrackForWindows() throws IOException, InterruptedException {

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

        // Test Run Aircrack
        String testAircrack = executeCommandInConsole(aircrackInvocationString);
        if(!testAircrack.contains("No file to crack specified."))
            throw new RuntimeException("Aircrack was setup but failed invocation test!");
    }


}
