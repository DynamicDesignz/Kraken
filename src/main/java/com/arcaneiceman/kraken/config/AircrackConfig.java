package com.arcaneiceman.kraken.config;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

import static com.arcaneiceman.kraken.util.ConsoleCommandUtil.executeCommandInConsole;

@Configuration
public class AircrackConfig {

    @Autowired
    public AircrackConfig() throws IOException, InterruptedException {
        String testAircrack = executeCommandInConsole("aircrack-ng");
        if(!testAircrack.contains("No file to crack specified."))
            throw new RuntimeException("Aircrack was not found on this machine!");
        else
            LoggerFactory.getLogger(AircrackConfig.class).info("Aircrack Found!");
    }


}
