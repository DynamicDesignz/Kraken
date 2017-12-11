package com.wali.kraken.core.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

/**
 * Created by Wali on 12/3/2017.
 */
@Profile("server")
@Service("ServerManager")
public class ServerManager {

    @Autowired
    public ServerManager(Environment environment) throws Exception{
        int gearmanServerPort = Integer.parseInt(
                environment.getProperty("kraken.server.gearman-server-port", "4730"));
        Class classToLoad = Class.forName ("org.gearman.impl.Main");
        Method[] methods = classToLoad.getMethods();
        Object[] params = new String[4];
        params[0] = "-p";
        params[1] = Integer.toString(gearmanServerPort);
        params[2] = "";
        params[3] = "";
        methods[0].invoke(null, new Object[] {params});
    }
}
