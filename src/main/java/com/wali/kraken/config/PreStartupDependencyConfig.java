package com.wali.kraken.config;

import com.google.common.io.ByteStreams;
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
    private org.slf4j.Logger log;

    @Autowired
    public PreStartupDependencyConfig(Environment environment, ResourceLoader resourceLoader) throws IOException, URISyntaxException {
        temporaryFolderBase = environment.getProperty("kraken.tmp-folder.base", "./kraken-temp-folder");
        this.resourceLoader = resourceLoader;
        log = org.slf4j.LoggerFactory.getLogger(PreStartupDependencyConfig.class);

        // Create Directory
        Files.createDirectories(Paths.get(temporaryFolderBase));

        // Get OS and Bit
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        boolean is64Bit = isWindows ?
                (System.getenv("ProgramFiles(x86)") != null) :
                (System.getProperty("os.arch").contains("64"));

        // Copy / Test Aircrack
        if (isWindows)
            if (is64Bit) {
                // 64 Bit Files
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "aircrack-ng-avx.exe");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "aircrack-ng-avx2.exe");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygcrypto-1.0.0.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cyggcc_s-seh-1.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygpcre-1.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygsqlite3-0.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygssp-0.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygstdc++-6.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygwin1.dll");
                copyFileToTemporaryDirectory("aircrack/windows/64bit/", "cygz.dll");
            } else {
                // 32 Bit Files
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "aircrack-ng-avx.exe");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "aircrack-ng-avx2.exe");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygcrypto-1.0.0.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cyggcc_s-1.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygpcre-1.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygsqlite3-0.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygssp-0.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygstdc++-6.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygwin1.dll");
                copyFileToTemporaryDirectory("aircrack/windows/32bit/", "cygz.dll");
            }
        else
            testForLinuxAircrack();

        // Copy default wpa password list
        copyFileToTemporaryDirectory(Constants.CANDIDATE_VALUE_LIST_DIRECTORY + "/" + "wpa" + "/", "default-list.txt");
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

    private void copyFileToTemporaryDirectory(String resourcePath, String filename) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath + filename);
        InputStream resourceInputStream = resource.getInputStream();
        byte[] fileAsArray = ByteStreams.toByteArray(resourceInputStream);
        File dir = new File(temporaryFolderBase + "\\" + resourcePath);
        if (dir.exists()) dir.delete();
        dir.mkdirs();
        Files.write(Paths.get(dir.getAbsolutePath(), filename), fileAsArray);
    }

    private void testForLinuxAircrack() {

    }


}
