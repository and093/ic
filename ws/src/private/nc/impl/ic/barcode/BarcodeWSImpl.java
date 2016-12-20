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
	 * 根据物料编码读取物料信息，返回xml格式数据
	 * @param code
	 * @return
	 */
	@Override
	public String GetProductInfoByCode(String productCode) {
		IMaterialInfo s = NCLocator.getInstance().lookup(IMaterialInfo.class);
		return s.getMaterialInfo(productCode);
	}
	
	/**
	 * 读取生产订单
	 * @param orderNo
	 * @return
	 */
	@Override
	public String GetProductionOrderByNo(String orderNo) {
		IProductOrder s = NCLocator.getInstance().lookup(IProductOrder.class);
		return s.getProductOrder(orderNo);
	}

	/**
	 * 写入成品入库，根据来源的生产订单，生成完工报告和产成品入库
	 * @param xml
	 * @return
	 */
	@Override
	public String PostGoodsReceiveNote(String xml) {
		IProductOrder s = NCLocator.getInstance().lookup(IProductOrder.class);
		return s.saveProductInbound_requireNew(xml);
	}

	/**
	 * 读取出库单，类型包括销售出库，其他出库和调拨出库
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
	 * 回写出库单扫码数量
	 * @param xml
	 * @return
	 */
	@Override
	public String PostDeliveryNoteDetailScanQty(String xml) {
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.saveOutboundBarcodeNum_requireNew(xml);
	}

	/**
	 * 回写出库单实发数量
	 * @param xml
	 * @return
	 */
	@Override
	public String PostOtherDeliveryNoteDetailActualQty(String xml) {
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.saveOuntboundOutNum_requireNew(xml);
	}

	/**
	 * 写入调拨入库
	 * @param xml
	 * @return
	 */
	@Override
	public String PostTransferReceiveNote(String xml) {
		ITOInOrder s = NCLocator.getInstance().lookup(ITOInOrder.class);
		return s.saveTransferIn_requireNew(xml);
	}

	/**
	 * 写入转库和其他出库
	 * @param xml
	 * @return
	 */
	@Override
	public String PostTransferOutNote(String xml) {
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferOut_requireNew(xml);
	}

	/**
	 * 写入转库的其他入库
	 * @param xml
	 * @return
	 */
	@Override
	public String PostTransferInNote(String xml) {
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferIn_requireNew(xml);
	}

}
