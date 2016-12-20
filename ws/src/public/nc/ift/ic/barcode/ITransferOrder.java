package nc.ift.ic.barcode;

public interface ITransferOrder {

	/**
	 * 保存转库和其他出库
	 * @param xml
	 * @return
	 */
	public String saveTransferOut_requireNew(String xml);
	
	/**
	 * 保存其他入库
	 * @param xml
	 * @return
	 */
	public String saveTransferIn_requireNew(String xml);
	
}
