package com.wali.kraken.core.worker;

import com.wali.kraken.domain.overwire.Job;
import com.wali.kraken.domain.overwire.Reply;
import com.wali.kraken.services.ServiceFunctions;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Wali on 12/3/2017.
 */
public class WPAWorker implements GearmanFunction {

    private ServiceFunctions serviceFunctions;
    private Logger log = LoggerFactory.getLogger(WPAWorker.class);

    WPAWorker(ServiceFunctions serviceFunctions){
        this.serviceFunctions = serviceFunctions;
    }

    @Override
    public byte[] work(String function, byte[] jobData, GearmanFunctionCallback gearmanFunctionCallback) throws Exception {

        Job job = serviceFunctions.deserializeJob(jobData);

        log.info("Got Job with Id {}");


        // TODO : JOB STUFF
        Reply reply = new Reply(false, null);

        return serviceFunctions.serializeReply(reply);
    }
}
