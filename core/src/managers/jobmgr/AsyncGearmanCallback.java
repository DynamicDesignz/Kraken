package managers.jobmgr;

import java.util.UUID;

import logger.LOG;
import objects.Messages.Reply;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;

import com.google.protobuf.InvalidProtocolBufferException;

public class AsyncGearmanCallback implements GearmanJobEventCallback<String>  {

	@Override
	public void onEvent(String attachment, GearmanJobEvent event) {
		switch (event.getEventType()) {
		case GEARMAN_JOB_SUCCESS: // Job completed successfully
			GearmanJobManager.getInstance().jobComplete(UUID.fromString(attachment));
			Reply r = null;
			try {
				r = Reply.parseFrom(event.getData());
				if (r.getStatus()){
					//Password was Found
					LOG.getLogger().info("Password Found ");
					GearmanJobManager.getInstance().PasswordFound(r.getAnswer());
				}
				else{
					GearmanJobManager.getInstance().sendNextJob();
				}
			} 
			catch (InvalidProtocolBufferException e) {
				LOG.getLogger().error("ERR : Could not read reply from Worker" + e.getMessage());
			}
			break;
		case GEARMAN_SUBMIT_FAIL: // The job submit operation failed
			LOG.getLogger().error("ERR : Gearman Server might not be working");
			break;
		case GEARMAN_JOB_FAIL: // The job's execution failed
			LOG.getLogger().error("ERR : Job Execution Failed. Attempting Recovery...");
			GearmanJobManager.getInstance().recoverChunk(UUID.fromString(attachment));
			GearmanJobManager.getInstance().sendNextJob();
			break;
		case GEARMAN_EOF:
			break;
		case GEARMAN_JOB_DATA:
			break;
		case GEARMAN_JOB_STATUS:
			break;
		}
	}


}
