package nc.ift.ic.barcode;

public interface IOutboundOrder {

	/**
	 * 读取出库单，类型包括销售出库，其他出库和调拨出库
	 * @param transationType
	 * @param orderNo
	 * @return
	 */
	public String getOutboundOrder(String transationType, String orderNo);
	
	/**
	 * 回写出库单的条码扫码数量
	 * @param xml
	 * @return
	 */
	public String saveOutboundBarcodeNum_requireNew(String xml);

	/**
	 * 回写出库单的实发数量
	 * @param xml
	 * @return
	 */
	public String saveOuntboundOutNum_requireNew(String xml);
}
