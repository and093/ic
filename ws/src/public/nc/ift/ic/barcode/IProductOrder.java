package nc.ift.ic.barcode;

public interface IProductOrder {

	/**
	 * ��ȡ��������
	 * @param batchcode
	 * @return
	 */
	public String getProductOrder(String batchcode);
	
	/**
	 * �����깤����Ͳ���Ʒ���
	 * @param xml
	 * @return
	 */
	public String saveProductInbound_requireNew(String xml);
	
}
