package nc.ift.ic.barcode;

public interface ITransferOrder {

	/**
	 * ����ת�����������
	 * @param xml
	 * @return
	 */
	public String saveTransferOut_requireNew(String xml);
	
	/**
	 * �����������
	 * @param xml
	 * @return
	 */
	public String saveTransferIn_requireNew(String xml);
	
}
