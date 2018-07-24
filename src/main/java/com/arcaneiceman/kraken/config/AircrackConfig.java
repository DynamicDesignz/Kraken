package com.arcaneiceman.kraken.config;

import com.arcaneiceman.kraken.util.ConsoleCommandUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.arcaneiceman.kraken.util.ConsoleCommandUtil.executeCommandInConsole;

@Configuration
public class AircrackConfig {

    @Autowired
    public AircrackConfig() throws IOException, InterruptedException, ExecutionException {
        String testAircrack = ConsoleCommandUtil.executeCommandInConsole(
                1, TimeUnit.SECONDS,
                ConsoleCommandUtil.OutputStream.OUT,
                "aircrack-ng");
        if(!testAircrack.contains("No file to crack specified."))
            throw new RuntimeException("Aircrack was not found on this machine!");
        else
            LoggerFactory.getLogger(AircrackConfig.class).info("Aircrack Found!");
    }


}
