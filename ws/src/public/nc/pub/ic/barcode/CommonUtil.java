package nc.pub.ic.barcode;

import java.util.HashMap;

/**
 * 定义所有的常量
 * @author thinkpad
 *
 */
public class CommonUtil {

	//处理成功
	public static int EX_CODE_SUCCESS = 0;
	
	//处理失败
	public static int EX_CODE_FAIL = 1;
	
	
	public static void putSuccessResult(HashMap<String, Object> para){
		para.put("EX_CODE", CommonUtil.EX_CODE_SUCCESS);
	}
	
	public static void putFailResult(HashMap<String, Object> para, String msg){
		para.put("EX_CODE", CommonUtil.EX_CODE_FAIL);
		para.put("EX_MSG", msg);
	}
}
