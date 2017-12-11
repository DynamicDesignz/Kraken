package com.wali.kraken.core.worker;

import com.wali.kraken.config.PreStartupDependencyConfig;
import com.wali.kraken.enumerations.RequestType;
import com.wali.kraken.services.ServiceFunctions;
import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Wali on 12/3/2017.
 */
@Component
@Profile("worker")
public class KrakenWorker {

    private ServiceFunctions serviceFunctions;

    @Autowired
    public KrakenWorker(Environment environment,
                        ServiceFunctions serviceFunctions,
                        PreStartupDependencyConfig preStartupDependencyConfig) throws UnknownHostException {
        int gearmanServerPort = Integer.parseInt(
                environment.getProperty("gearman.server.port", "4730"));
        String gearmanServerHost = environment.getProperty("gearman.server.host", "127.0.0.1");

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
