package nc.ift.ic.barcode;

public interface IBarcodeWS {

	/**
	 * 根据物料编码读取物料信息，返回xml格式数据
	 * @param code
	 * @return
	 */
	public String getProductInfoByCode(String code);
	
}
