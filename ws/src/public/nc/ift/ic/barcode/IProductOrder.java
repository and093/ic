package nc.ift.ic.barcode;

public interface IProductOrder {

	/**
	 * 获取生产订单
	 * @param batchcode
	 * @return
	 */
	public String getProductOrder(String batchcode);
	
	/**
	 * 保存完工报告和产成品入库
	 * @param xml
	 * @return
	 */
	public String saveProductInbound_requireNew(String xml);
	
	
	/**
	 * 回滚产成品入库
	 * @param guid
	 * @return
	 */
	public String rollbackProductInbound_requireNew(String guid);
	
}
