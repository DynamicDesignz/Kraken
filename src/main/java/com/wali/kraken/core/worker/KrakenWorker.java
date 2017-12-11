package com.wali.kraken.core.worker;

import com.wali.kraken.config.PreStartupDependencyConfig;
import com.wali.kraken.enumerations.RequestType;
import com.wali.kraken.services.ServiceFunctions;
import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by Wali on 12/3/2017.
 */
@Component
@Profile("worker")
public class KrakenWorker {

    private Logger log = LoggerFactory.getLogger(KrakenWorker.class);

    @Autowired
    public KrakenWorker(Environment environment,
                        ServiceFunctions serviceFunctions,
                        PreStartupDependencyConfig preStartupDependencyConfig) throws UnknownHostException, InterruptedException {
        int gearmanServerPort = Integer.parseInt(
                environment.getProperty("kraken.worker.gearman-port"));
        String gearmanServerHost = environment.getProperty("kraken.worker.gearman-host");
        if(gearmanServerHost == null)
            throw new RuntimeException("Kraken Worker : Gearman Server Host not defined!");
        if(gearmanServerPort == 0)
            throw new RuntimeException("Kraken Worker : Gearman Server Port not defined!");

        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(s -> s.equals("server"))){
            log.info("Activating 2 second delay for server beans to start");
            Thread.sleep(2000);
        }

        String output = serviceFunctions.sendTextCommandToGearmanServer(gearmanServerHost, gearmanServerPort, "status");
        if(output == null)
            throw new RuntimeException("Gearman Server at host/port " +
                    gearmanServerHost + "/"  + gearmanServerPort + " unreachable");

        Gearman gearman = Gearman.createGearman();
        GearmanServer server = gearman.createGearmanServer(gearmanServerHost, gearmanServerPort);
        GearmanWorker worker = gearman.createGearmanWorker();

        for(RequestType requestType : RequestType.values()){
            if(requestType == RequestType.WPA)
                worker.addFunction(requestType.name(), new WPAWorker(serviceFunctions, environment, preStartupDependencyConfig));
        }

        InetAddress ip = InetAddress.getLocalHost();
        worker.setClientID("kraken-worker-" + ip.getHostAddress());
        worker.setReconnectPeriod(5, TimeUnit.SECONDS);
        worker.addServer(server);
    }
}
