package nc.ift.ic.barcode;

public interface IOutboundOrder {

	/**
	 * ��ȡ���ⵥ�����Ͱ������۳��⣬��������͵�������
	 * @param transationType
	 * @param orderNo
	 * @return
	 */
	public String getOutboundOrder(String transationType, String orderNo);
	
	/**
	 * ��д���ⵥ������ɨ������
	 * @param xml
	 * @return
	 */
	public String saveOutboundBarcodeNum_requireNew(String xml);

	/**
	 * ��д���ⵥ��ʵ������
	 * @param xml
	 * @return
	 */
	public String saveOuntboundOutNum_requireNew(String xml);
}
