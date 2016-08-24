package logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LOG {
	private static  Logger sl4jLogger = null;
	
	public LOG(){
		sl4jLogger = LoggerFactory.getLogger(LOG.class);
	}
	
	public static Logger getLogger(){
		return sl4jLogger;
	}
}
