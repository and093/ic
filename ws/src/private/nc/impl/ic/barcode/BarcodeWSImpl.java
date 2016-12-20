package nc.impl.ic.barcode;

import nc.bs.framework.common.NCLocator;
import nc.ift.ic.barcode.IBarcodeWS;
import nc.ift.ic.barcode.IMaterialInfo;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.ift.ic.barcode.IProductOrder;
import nc.ift.ic.barcode.ITOInOrder;
import nc.ift.ic.barcode.ITransferOrder;

public class BarcodeWSImpl implements IBarcodeWS {

	/**
	 * �������ϱ����ȡ������Ϣ������xml��ʽ����
	 * @param code
	 * @return
	 */
	@Override
	public String GetProductInfoByCode(String productCode) {
		IMaterialInfo s = NCLocator.getInstance().lookup(IMaterialInfo.class);
		return s.getMaterialInfo(productCode);
	}
	
	/**
	 * ��ȡ��������
	 * @param orderNo
	 * @return
	 */
	@Override
	public String GetProductionOrderByNo(String orderNo) {
		IProductOrder s = NCLocator.getInstance().lookup(IProductOrder.class);
		return s.getProductOrder(orderNo);
	}

	/**
	 * д���Ʒ��⣬������Դ�����������������깤����Ͳ���Ʒ���
	 * @param xml
	 * @return
	 */
	@Override
	public String PostGoodsReceiveNote(String xml) {
		IProductOrder s = NCLocator.getInstance().lookup(IProductOrder.class);
		return s.saveProductInbound_requireNew(xml);
	}

	/**
	 * ��ȡ���ⵥ�����Ͱ������۳��⣬��������͵�������
	 * @param transationType
	 * @param orderNo
	 * @return
	 */
	@Override
	public String GetDeliveryNoteByNo(String transationType, String orderNo) {
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.getOutboundOrder(transationType, orderNo);
	}

	/**
	 * ��д���ⵥɨ������
	 * @param xml
	 * @return
	 */
	@Override
	public String PostDeliveryNoteDetailScanQty(String xml) {
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.saveOutboundBarcodeNum_requireNew(xml);
	}

	/**
	 * ��д���ⵥʵ������
	 * @param xml
	 * @return
	 */
	@Override
	public String PostOtherDeliveryNoteDetailActualQty(String xml) {
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.saveOuntboundOutNum_requireNew(xml);
	}

	/**
	 * д��������
	 * @param xml
	 * @return
	 */
	@Override
	public String PostTransferReceiveNote(String xml) {
		ITOInOrder s = NCLocator.getInstance().lookup(ITOInOrder.class);
		return s.saveTransferIn_requireNew(xml);
	}

	/**
	 * д��ת�����������
	 * @param xml
	 * @return
	 */
	@Override
	public String PostTransferOutNote(String xml) {
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferOut_requireNew(xml);
	}

	/**
	 * д��ת����������
	 * @param xml
	 * @return
	 */
	@Override
	public String PostTransferInNote(String xml) {
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferIn_requireNew(xml);
	}

}
