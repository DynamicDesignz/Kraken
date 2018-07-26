package com.arcaneiceman.kraken.config;

import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Configuration
public class DependencyConfig {

    private static Logger log = LoggerFactory.getLogger(DependencyConfig.class);

    @Autowired
    public DependencyConfig() throws IOException, InterruptedException, ExecutionException {
        String testAircrack = ConsoleCommandUtil.executeCommandInConsole(
                1, TimeUnit.SECONDS,
                ConsoleCommandUtil.OutputStream.OUT,
                "aircrack-ng");
        if (testAircrack != null && !testAircrack.contains("No file to crack specified."))
            throw new RuntimeException("Aircrack was not found on this machine!");
        else
            log.info("Aircrack Found!");
        String testCrunch = ConsoleCommandUtil.executeCommandInConsole(
                1, TimeUnit.SECONDS,
                ConsoleCommandUtil.OutputStream.ERROR,
                "crunch");
        if (testCrunch != null && !testCrunch.contains("crunch version"))
            throw new RuntimeException("Crunch was not found on this machine!");
        else
            log.info("Crunch Found");
    }


}
