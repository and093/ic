package nc.pub.ic.barcode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ResourceUtil {

	private static Properties PROPER;
	
	private static void intiProperties(){
		if(PROPER == null){
			PROPER = new Properties();
			try {
				PROPER.load(ResourceUtil.getResourceAsStream(Thread.currentThread().getContextClassLoader(), "nc/config/ic/barcode/system.properties"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static Properties getPro(){
		intiProperties();
		return PROPER;
	}
	/**
	 * Returns a resource on the classpath as a Stream object
	 * 
	 * @param loader
	 *            The classloader used to load the resource
	 * @param resource
	 *            The resource to find
	 * @return The resource
	 * @throws IOException
	 *             If the resource cannot be found or read
	 */
	public static InputStream getResourceAsStream(ClassLoader loader,
			String resource) throws IOException {
		InputStream in = null;
		if (loader != null)
			in = loader.getResourceAsStream(resource);
		if (in == null)
			in = ClassLoader.getSystemResourceAsStream(resource);
		if (in == null)
			throw new IOException("Could not find resource " + resource);
		return in;
	}

	/**
	 * 根据文件的类路径得到实际路径
	 * 
	 * @param classPath
	 * @return
	 */
	public static String getRealPath(String classPath) {
		return Thread.currentThread().getContextClassLoader().getResource(
				classPath).getPath();
	}

}
