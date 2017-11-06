package nc.impl.ic.barcode;

import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.WsQueryBS;
import nc.ift.ic.barcode.IBarcodeWS;
import nc.ift.ic.barcode.IMaterialInfo;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.ift.ic.barcode.IProductOrder;
import nc.ift.ic.barcode.ITOInOrder;
import nc.ift.ic.barcode.ITransferOrder;
import nc.pub.ic.barcode.ResourceUtil;

public class BarcodeWSImpl implements IBarcodeWS {

	/**
	 * 根据物料编码读取物料信息，返回xml格式数据
	 * @param code
	 * @return
	 */
	@Override
	public String GetProductInfoByCode(String productCode) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
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
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		IProductOrder s = NCLocator.getInstance().lookup(IProductOrder.class);
		return s.getProductOrder(orderNo);
	}

	/**
	 * 写入成品入库，根据来源的生产订单，生成完工报告和产成品入库
	 * @param xml
	 * @return
	 */
	@Override
	public synchronized String PostGoodsReceiveNote(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
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
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.getOutboundOrder(transationType, orderNo);
	}

	/**
	 * 回写出库单扫码数量
	 * @param xml
	 * @return
	 */
	@Override
	public synchronized String PostDeliveryNoteDetailScanQty(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.saveOutboundBarcodeNum_requireNew(xml);
	}

	/**
	 * 回写出库单实发数量
	 * @param xml
	 * @return
	 */
	@Override
	public synchronized String PostOtherDeliveryNoteDetailActualQty(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		IOutboundOrder s = NCLocator.getInstance().lookup(IOutboundOrder.class);
		return s.saveOuntboundOutNum_requireNew(xml);
	}

	/**
	 * 写入调拨入库
	 * @param xml
	 * @return
	 */
	@Override
	public synchronized String PostTransferReceiveNote(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		ITOInOrder s = NCLocator.getInstance().lookup(ITOInOrder.class);
		return s.saveTransferIn_requireNew(xml);
	}

	/**
	 * 写入转库和其他出库
	 * @param xml
	 * @return
	 */
	@Override
	public synchronized String PostTransferOutNote(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferOut_requireNew(xml);
	}

	/**
	 * 写入转库的其他入库
	 * @param xml
	 * @return
	 */
	@Override
	public synchronized String PostTransferInNote(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferIn_requireNew(xml);
	}

	/**
	 * 写入转库，并自动生成其他出库和其他入库
	 */
	@Override
	public String PostTransferOutAndInNote(String xml) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		ITransferOrder s = NCLocator.getInstance().lookup(ITransferOrder.class);
		return s.saveTransferOutAndIn_requireNew(xml);
	}

	/**
	 * 回滚产成品入库
	 * @param guid
	 * @return
	 */
	@Override
	public String RollbackGoodsReceiveNote(String guid) {
		InvocationInfoProxy.getInstance().setUserDataSource(ResourceUtil.getPro().getProperty("system.dataSource"));
		String userCode = ResourceUtil.getPro().getProperty("system.user");
		String cuserid = WsQueryBS.getUseridByCode(userCode);
		InvocationInfoProxy.getInstance().setUserId(cuserid);
		IProductOrder s = NCLocator.getInstance().lookup(IProductOrder.class);
		return s.rollbackProductInbound_requireNew(guid);
	}

}
