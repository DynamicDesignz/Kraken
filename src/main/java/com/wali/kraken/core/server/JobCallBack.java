package com.wali.kraken.core.server;

import com.wali.kraken.domain.overwire.Reply;
import com.wali.kraken.services.ServiceFunctions;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Wali on 10/14/2017.
 */
public class JobCallBack implements GearmanJobEventCallback<String> {

    private ProcessingCore processingCore;

    private ExecutorService executorService;

    private ServiceFunctions serviceFunctions;

    private Logger logger = LoggerFactory.getLogger(JobCallBack.class);

    public JobCallBack(ProcessingCore processingCore, ServiceFunctions serviceFunctions) {
        this.processingCore = processingCore;
        this.serviceFunctions = serviceFunctions;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onEvent(String jobDescriptorKey, GearmanJobEvent gearmanJobEvent) {
        // Parse id String
        long[] id;
        switch (gearmanJobEvent.getEventType()) {
            // Job completed successfully
            case GEARMAN_JOB_SUCCESS:
                logger.info("Got a successful callback for job with id {}", jobDescriptorKey);

                // Parse id String
                id = serviceFunctions.getJobDescriptorFromKey(jobDescriptorKey);

                // Parse Reply
                Reply reply = getReplyFromGearman(gearmanJobEvent);
                if (reply == null) {
                    logger.error("Could not process data from call back for job with id {}", jobDescriptorKey);

                    // Recover Job
                    processingCore.recoverJob(jobDescriptorKey);

                    // Not Marking as Job Complete ; Send Next
                    executorService.execute(() -> processingCore.process(
                            id[0],
                            id[1],
                            null));
                    return;
                }

                if(reply.isFound()){
                    processingCore.valueFound(jobDescriptorKey, reply.getAnswer());

                    // Mark Request Complete ; Send Next
                    executorService.execute(() -> processingCore.process(
                            id[0],
                            null,
                            null));
                    return;
                }

                // Marking Job as Complete, Send Next Job
                executorService.execute(() -> processingCore.process(
                        id[0],
                        id[1],
                        id[2]));
                return;
            case GEARMAN_SUBMIT_FAIL: // The job submit operation failed
                logger.error("Gearman Server Submission failure! ");
                return;
            case GEARMAN_JOB_FAIL: // The job's execution failed
                logger.info("Got a failed callback for job with id {}", jobDescriptorKey);

                // Parse id String
                id = serviceFunctions.getJobDescriptorFromKey(jobDescriptorKey);

                // Recover Job
                processingCore.recoverJob(jobDescriptorKey);

                // Not Marking as Job Complete ; Send Next
                executorService.execute(() -> processingCore.process(
                        id[0],
                        id[1],
                        null));
                return;
            case GEARMAN_EOF:
                break;
            case GEARMAN_JOB_DATA:
                break;
            case GEARMAN_JOB_STATUS:
                break;
        }
    }

    private Reply getReplyFromGearman(GearmanJobEvent gearmanJobEvent) {
        Reply reply = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(gearmanJobEvent.getData());
            ois = new ObjectInputStream(bis);
            reply = (Reply) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return reply;
    }
}
