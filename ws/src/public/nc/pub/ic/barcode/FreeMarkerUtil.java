package nc.pub.ic.barcode;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;

import org.apache.log4j.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MruCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

public class FreeMarkerUtil {

	private static Configuration cfg; 
	public static Logger log = Logger.getLogger(FreeMarkerUtil.class);
	
	static{
		try {
	        cfg = new Configuration();
	        cfg.setTemplateLoader(new ClassTemplateLoader(FreeMarkerUtil.class, "/"));
	        //cfg.setServletContextForTemplateLoading(Constant.SERVLET_CONTEXT, "WEB-INF/templates");
	        //cfg.setDirectoryForTemplateLoading(new File(Constant.SERVLET_CONTEXT));
	        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
	        cfg.setObjectWrapper(ObjectWrapper.BEANS_WRAPPER);
	        cfg.setDefaultEncoding("UTF-8");
	        cfg.setOutputEncoding("UTF-8");
	        cfg.setLocale(Locale.US);
	        cfg.setTemplateUpdateDelay(9000000);	//调试的时候设为0，正式发布要设为一个比较大的值 
	        cfg.setCacheStorage(new MruCacheStorage(20, 250));
	        cfg.setNumberFormat("0.##########");
		} catch (Exception e) {
			log.error("初始化freeMarker 失败", e);
        }
	}
	
	/**
	 * 将freeMarker模版解析到String中
	 * @param parameter
	 * @param templatePath
	 * @return
	 */
	public static String process(HashMap<String, Object> parameter, String templatePath){
        StringWriter out = new StringWriter();
        try {
        	Template t = cfg.getTemplate(templatePath);
            t.process(parameter, out);
        } catch (Exception e) {
        	log.error("freeMarker error", e);
        }
        return out.toString();
	}

}
