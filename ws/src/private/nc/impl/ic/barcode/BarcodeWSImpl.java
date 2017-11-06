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
	 * �������ϱ����ȡ������Ϣ������xml��ʽ����
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
	 * ��ȡ��������
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
	 * д���Ʒ��⣬������Դ�����������������깤����Ͳ���Ʒ���
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
	 * ��ȡ���ⵥ�����Ͱ������۳��⣬��������͵�������
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
	 * ��д���ⵥɨ������
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
	 * ��д���ⵥʵ������
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
	 * д��������
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
	 * д��ת�����������
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
	 * д��ת����������
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
	 * д��ת�⣬���Զ���������������������
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
	 * �ع�����Ʒ���
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
