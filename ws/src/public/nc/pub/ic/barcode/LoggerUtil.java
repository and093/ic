package nc.pub.ic.barcode;

import java.util.HashMap;

import nc.bs.logging.Log;

public class LoggerUtil {

	private static HashMap<String, Log> loggerPluginMap = new HashMap<String, Log>();
	
	private static Log log;
	
	static{
		log = getLogger("sync");
	}
	
	private static Log getLogger(String module){
		synchronized(loggerPluginMap){
			Log log = loggerPluginMap.get(module);
			if(log == null){
				log = Log.getInstance(module);
				loggerPluginMap.put(module, log);
			}
			return log;
		}
	}
	
	public static void error(Object msg, Throwable throwable) {
		log.error(msg, throwable);
	}
	
	public static void error(Object msg) {
		log.error(msg);
	}
	
	public static void debug(Object msg) {
		log.error(msg);
	}
	
	public static void info(Object msg) {
		log.error(msg);
	}
	
}
