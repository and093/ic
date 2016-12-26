package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.ReadProductOrder;
import nc.bs.ic.barcode.WsQueryBS;
import java.util.List;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.ic.m4c.entity.SaleOutBodyVO;
import nc.vo.ic.m4c.entity.SaleOutHeadVO;
import nc.vo.ic.m4c.entity.SaleOutVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4y.entity.TransOutBodyVO;
import nc.vo.ic.m4y.entity.TransOutHeadVO;
import nc.vo.ic.m4y.entity.TransOutVO;
import nc.vo.pub.AggregatedValueObject;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class OutboundOrderImpl implements IOutboundOrder {

	public String getOutboundOrder(String transationType, String orderNo) {
		ReadProductOrder readproductorder =new ReadProductOrder();
		if("4C".equals(transationType)){
			return readproductorder.RaadSaleOrder(orderNo);
		}if("4A".equals(transationType)) {
			return readproductorder.RaadGeneralOutOrder(orderNo);
		}if("4Y".equals(transationType)) {
			return readproductorder.RaadTransOutOrder(orderNo);
		} 
		return readproductorder.Error(orderNo);
	}

	/**
	 * ��д���ⵥ��ɨ������
	 */
	@Override
	public String saveOutboundBarcodeNum_requireNew(String xml) {
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String TransationType = obj.getString("TransationType");
		String OrderNo = obj.getString("OrderNo");
		String LineNo = obj.getString("LineNo");
		int UpdateType = obj.getInt("UpdateType");
		int ScanQty = obj.getInt("ScanQty");

		if ("4C".equals(TransationType)) {
			return writeSaleOrderBound(OrderNo, LineNo, UpdateType, ScanQty);
		} else if ("4A".equals(TransationType)) {
			return writeGeneralOut(OrderNo, LineNo, UpdateType, ScanQty);
		} else if ("4Y".equals(TransationType)) {
			return writeTransOut(OrderNo, LineNo, UpdateType, ScanQty);
		} else {
			HashMap<String, Object> para = new HashMap<String, Object>();
			CommonUtil.putFailResult(para, "û���뽻������" + TransationType
					+ "��Ӧ�ĳ���ҵ��");
			return FreeMarkerUtil.process(para,
					"nc/config/ic/barcode/WriteOutBoundOrder.fl");
		}
	}
	
	public String writeTransOut(String OrderNo, String LineNo, int UpdateType,
			int ScanQty) {

		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";

		TransOutBodyVO body = null;

		try {
			// false-δ�ҵ����Ŷ�Ӧ�кŵ�����
			boolean flag = false;

			List<TransOutVO> list = (List<TransOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutVO.class, sqlWhere, true, false);

			// δ��ѯ������Ϊ OrderNo �ĳ��ⵥ
			if (list.size() == 0) {
				CommonUtil.putFailResult(para, "�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
			} else {
				TransOutHeadVO headVO = list.get(0).getHead();

				// ��ȡ����״̬ 1-ɾ�� 2-���� 3-ǩ��
				if (headVO.getFbillflag() == 2) {
					int len = list.get(0).getBodys().length;
					for (int i = 0; i < len; i++) {
						body = list.get(0).getBodys()[i];

						if (LineNo.equals(body.getCrowno())) {

							if (UpdateType == 1) {
								int num = Integer.parseInt(body.getVbdef20())
										+ ScanQty;
								body.setVbdef20(new String(num + ""));
								flag = true;
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setVbdef20(new String(ScanQty + ""));
								flag = true;
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "������������");
							}
						}
					}
					// û���ҵ���Ӧ�кŵ��������ⵥ�ӱ�
					if (!flag && (UpdateType==1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "�������ⵥ�� " + OrderNo
								+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
					}
				} else {
					CommonUtil.putFailResult(para, "�õ���Ϊ������״̬�������޸�");
				} // end if ����״̬

				// ���ҵ���Ӧ���ݲ����������޸�
				if (flag == true) {
					System.out.print("save");
					try {
						dao.executeUpdate("update ic_transout_b set vbdef20='"
								+ body.getVbdef20() + "' where cgeneralbid='"
								+ body.getCgeneralbid() + "'");
					} catch (DAOException e) {
						CommonUtil.putSuccessResult(para);
						e.printStackTrace();
					}
				}
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	private String writeGeneralOut(String OrderNo, String LineNo,
			int UpdateType, int ScanQty) {

		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";
		try {
			// false-δ�ҵ����Ŷ�Ӧ�кŵ�����
			boolean flag = false;

			List<GeneralOutVO> list = (List<GeneralOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutVO.class, sqlWhere, true, false);
			// δ��ѯ������Ϊ OrderNo �ĳ��ⵥ
			if (list.size() == 0) {
				CommonUtil.putFailResult(para, "�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
			} else {
				GeneralOutHeadVO headVO = list.get(0).getHead();
				// ��ȡ����״̬ 1-ɾ�� 2-���� 3-ǩ��
				if (headVO.getFbillflag() == 2) {
					for (GeneralOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {
							if (UpdateType == 1) {
								int num = Integer.parseInt(body.getVbdef20())
										+ ScanQty;
								body.setVbdef20(new String(num + ""));
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setVbdef20(new String(ScanQty + ""));
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "������������");
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// û���ҵ���Ӧ�кŵ��������ⵥ�ӱ�
					if (!flag && (UpdateType==1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "�������ⵥ�� " + OrderNo
								+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
					}
				} else {
					CommonUtil.putFailResult(para, "�õ���Ϊ������״̬�������޸�");
				} // end if ����״̬
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "д�����ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}
	
	private String writeSaleOrderBound(String OrderNo, String LineNo,
			int UpdateType, int ScanQty) {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";

		try {
			// false-δ�ҵ����Ŷ�Ӧ�кŵ�����
			boolean flag = false;

			List<SaleOutVO> list = (List<SaleOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							SaleOutVO.class, sqlWhere, true, false);
			// δ��ѯ������Ϊ OrderNo �����۳��ⵥ
			if (list.size() == 0) {
				CommonUtil.putFailResult(para, "���۳��ⵥ��" + OrderNo
						+ "�Ҳ�����Ӧ�����۳��ⵥ");
			} else {
				SaleOutHeadVO headVO = (SaleOutHeadVO) list.get(0).getHead();
				// ��ȡ����״̬ 1-ɾ�� 2-���� 3-ǩ��
				if (headVO.getFbillflag() == 2) {
					for (SaleOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {
							if (UpdateType == 1) {
								int num = Integer.parseInt(body.getVbdef20())+ScanQty;
								body.setVbdef20(new String(num+""));
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setVbdef20(new String(ScanQty+""));
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "������������");
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// û���ҵ���Ӧ�кŵ����۳��ⵥ�ӱ�
					if (!flag && (UpdateType == 1 && UpdateType == 2)) {
						CommonUtil.putFailResult(para, "���۳��ⵥ�� " + OrderNo
								+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
					}
				} else {
					CommonUtil.putFailResult(para, "�����۵�Ϊ������״̬�������޸�");
				}
			}
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "д�����ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
		}
		return FreeMarkerUtil.process(para, "nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	@Override
	public String saveOuntboundOutNum_requireNew(String xml) {
		// TODO Auto-generated method stub
		return null;
	}

}

