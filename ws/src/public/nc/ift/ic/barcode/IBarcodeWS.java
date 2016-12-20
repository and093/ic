package nc.ift.ic.barcode;

public interface IBarcodeWS {

	/**
	 * 根据物料编码读取物料信息，返回xml格式数据
	 * @param code
	 * @return
	 */
	public String GetProductInfoByCode(String productCode);
	
	/**
	 * 读取生产订单
	 * @param orderNo
	 * @return
	 */
	public String GetProductionOrderByNo(String orderNo);
	
	/**
	 * 写入成品入库，根据来源的生产订单，生成完工报告和产成品入库
	 * @param xml
	 * @return
	 */
	public String PostGoodsReceiveNote(String xml);
	
	/**
	 * 读取出库单，类型包括销售出库，其他出库和调拨出库
	 * @param transationType
	 * @param orderNo
	 * @return
	 */
	public String GetDeliveryNoteByNo(String transationType, String orderNo);
	
	/**
	 * 回写出库单扫码数量
	 * @param xml
	 * @return
	 */
	public String PostDeliveryNoteDetailScanQty(String xml);
	
	
	/**
	 * 回写出库单实发数量
	 * @param xml
	 * @return
	 */
	public String PostOtherDeliveryNoteDetailActualQty(String xml);
	
	
	/**
	 * 写入调拨入库
	 * @param xml
	 * @return
	 */
	public String PostTransferReceiveNote(String xml);
	
	/**
	 * 写入转库和其他出库
	 * @param xml
	 * @return
	 */
	public String PostTransferOutNote(String xml);
	
	/**
	 * 写入转库的其他入库
	 * @param xml
	 * @return
	 */
	public String PostTransferInNote(String xml);
	
	
}
