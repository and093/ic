package nc.pub.ic.barcode;

import java.util.HashMap;

import nc.bs.logging.Log;

public class LoggerUtil {

	private static HashMap<String, Log> loggerPluginMap = new HashMap<String, Log>();
	
	private String module;
	
	private Log log;
	
	private LoggerUtil(){
	}
	
	public static LoggerUtil getSyncLogger(){
		LoggerUtil logobj = new LoggerUtil();
		logobj.module = "sync";
		logobj.log = getLogger(logobj.module);
		return logobj;
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
	
	public void error(Object msg, Throwable throwable) {
		log.error(msg, throwable);
	}
	
	public void error(Object msg) {
		log.error(msg);
	}
	
	public void debug(Object msg) {
		log.error(msg);
	}
	
	public void info(Object msg) {
		log.error(msg);
	}
	
}
