package com.arcaneiceman.kraken.util;

import com.amazonaws.util.IOUtils;
import com.arcaneiceman.kraken.util.exceptions.SystemException;
import org.zalando.problem.Status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.arcaneiceman.kraken.util.ConsoleCommandUtil.OutputStream.OUT;

public class ConsoleCommandUtil {

    public enum OutputStream {
        ERROR, OUT
    }

    public static String executeCommandInConsole(long timeout,
                                                 TimeUnit timeUnit,
                                                 OutputStream outputStream,
                                                 String... commands) {
        try {
            // Create Two Spawned Threads for Handling output and error
            ExecutorService newFixedThreadPool = Executors.newFixedThreadPool(2);

            ProcessBuilder pb = new ProcessBuilder(commands);
            Process process = pb.start();
            Future<String> output = newFixedThreadPool.submit(() -> IOUtils.toString(process.getInputStream()));
            Future<String> error = newFixedThreadPool.submit(() -> IOUtils.toString(process.getErrorStream()));

            newFixedThreadPool.shutdown();

            if (timeout != 0 && timeUnit != null)
                if (!process.waitFor(timeout, timeUnit))
                    process.destroy();
                else
                    process.waitFor();

            switch (outputStream) {
                case ERROR:
                    return error.get();
                case OUT:
                    return output.get();
                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SystemException(12312, "Execution Failure : " + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
        }
    }

    public static String executeCommandInConsole(String... commands) {
        return executeCommandInConsole(0, null, OUT, commands);
    }
}
