package nc.ift.ic.barcode;

public interface IBarcodeWS {

	/**
	 * �������ϱ����ȡ������Ϣ������xml��ʽ����
	 * @param code
	 * @return
	 */
	public String GetProductInfoByCode(String productCode);
	
	/**
	 * ��ȡ��������
	 * @param orderNo
	 * @return
	 */
	public String GetProductionOrderByNo(String orderNo);
	
	/**
	 * д���Ʒ��⣬������Դ�����������������깤����Ͳ���Ʒ���
	 * @param xml
	 * @return
	 */
	public String PostGoodsReceiveNote(String xml);
	
	/**
	 * ��ȡ���ⵥ�����Ͱ������۳��⣬��������͵�������
	 * @param transationType
	 * @param orderNo
	 * @return
	 */
	public String GetDeliveryNoteByNo(String transationType, String orderNo);
	
	/**
	 * ��д���ⵥɨ������
	 * @param xml
	 * @return
	 */
	public String PostDeliveryNoteDetailScanQty(String xml);
	
	
	/**
	 * ��д���ⵥʵ������
	 * @param xml
	 * @return
	 */
	public String PostOtherDeliveryNoteDetailActualQty(String xml);
	
	
	/**
	 * д��������
	 * @param xml
	 * @return
	 */
	public String PostTransferReceiveNote(String xml);
	
	/**
	 * д��ת�����������
	 * @param xml
	 * @return
	 */
	public String PostTransferOutNote(String xml);
	
	/**
	 * д��ת����������
	 * @param xml
	 * @return
	 */
	public String PostTransferInNote(String xml);
	
	
}
