package com.arcaneiceman.kraken.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ConsoleCommandUtil {

    public static String executeCommandInConsole(long timeout,
                                                 TimeUnit timeUnit,
                                                 String... commands) throws InterruptedException, IOException {
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process process = pb.start();
        if (timeout != 0 && timeUnit != null)
            process.waitFor(timeout, timeUnit);
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder("");
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    public static String executeCommandInConsole(String... commands) throws IOException, InterruptedException {
        return executeCommandInConsole(0, null, commands);
    }
}
