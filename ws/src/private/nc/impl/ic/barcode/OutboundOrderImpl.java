package nc.impl.ic.barcode;

import java.util.HashMap;
import java.util.List;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.ReadOutBoundOrder;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.itf.uap.pf.IPFBusiAction;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.pub.ic.barcode.LoggerUtil;
import nc.vo.ic.m4c.entity.SaleOutBodyVO;
import nc.vo.ic.m4c.entity.SaleOutHeadVO;
import nc.vo.ic.m4c.entity.SaleOutVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4y.entity.TransOutBodyVO;
import nc.vo.ic.m4y.entity.TransOutHeadVO;
import nc.vo.ic.m4y.entity.TransOutVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDouble;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class OutboundOrderImpl implements IOutboundOrder {

	public String getOutboundOrder(String transationType, String orderNo) {
		LoggerUtil.debug("��ȡ���ⵥ getOutboundOrder " + transationType + " - "
				+ orderNo);
		ReadOutBoundOrder readoutboundorder = new ReadOutBoundOrder();
		String rst = "";
		if ("4C".equals(transationType)) {
			rst = readoutboundorder.RaadSaleOrder(orderNo);
		} else if ("4A".equals(transationType)) {
			rst = readoutboundorder.RaadGeneralOutOrder(orderNo);
		} else if ("4Y".equals(transationType)) {
			rst = readoutboundorder.RaadTransOutOrder(orderNo);
		} else {
			rst = readoutboundorder.Error(orderNo);
		}
		LoggerUtil.debug("��ȡ���ⵥ���  getOutboundOrder " + rst);
		return rst;
	}

	/**
	 * 2.5 ��д���ⵥ��ɨ������
	 */
	@Override
	public String saveOutboundBarcodeNum_requireNew(String xml) {
		LoggerUtil.debug("д�����ɨ������ saveOutboundBarcodeNum_requireNew " + xml);
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String TransationType = obj.getString("TransationType"); // ��������
		String OrderNo = obj.getString("OrderNo"); // ���ݺ�
		String LineNo = obj.getString("LineNo"); // �к�
		int UpdateType = obj.getInt("UpdateType"); // ��������
		int ScanQty = obj.getInt("ScanQty"); // ɨ������

		String rst = "";
		if ("4C".equals(TransationType)) {
			rst = writeSaleOrderBound(OrderNo, LineNo, UpdateType, ScanQty);
		} else if ("4A".equals(TransationType)) {
			rst = writeGeneralOut(OrderNo, LineNo, UpdateType, ScanQty);
		} else if ("4Y".equals(TransationType)) {
			rst = writeTransOut(OrderNo, LineNo, UpdateType, ScanQty);
		} else {
			HashMap<String, Object> para = new HashMap<String, Object>();
			CommonUtil.putFailResult(para, "û���뽻������" + TransationType
					+ "��Ӧ�ĳ���ҵ��");
			LoggerUtil.error("û���뽻������" + TransationType + "��Ӧ�ĳ���ҵ��");
			rst = FreeMarkerUtil.process(para,
					"nc/config/ic/barcode/WriteOutBoundOrder.fl");
		}
		LoggerUtil.debug("д�����ɨ��������� saveOutboundBarcodeNum_requireNew " + rst);
		return rst;
	}

	/**
	 * ��������
	 * 
	 * @param OrderNo
	 * @param LineNo
	 * @param UpdateType
	 * @param ScanQty
	 * @return
	 */
	public String writeTransOut(String OrderNo, String LineNo, int UpdateType,
			int ScanQty) {

		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";

		TransOutBodyVO body = null;

		try {
			boolean flag = false;

			List<TransOutVO> list = (List<TransOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutVO.class, sqlWhere, true, false);

			// δ��ѯ������Ϊ OrderNo �ĳ��ⵥ
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
				LoggerUtil.error("�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
			} else {
				TransOutHeadVO headVO = list.get(0).getHead();

				// ��ȡ����״̬ 1-ɾ�� 2-���� 3-ǩ��
				if (headVO.getFbillflag() == 2) {
					int len = list.get(0).getBodys().length;
					for (int i = 0; i < len; i++) {
						body = list.get(0).getBodys()[i];

						if (LineNo.equals(body.getCrowno())) {
							// ʵ������Ϊ�� ��ʾ����
							if (body.getNassistnum() == null) {
								CommonUtil.putFailResult(para, "�к� " + LineNo
										+ " ��ʵ������Ϊ�գ�������дɨ��������");
								LoggerUtil.error("�к�" + LineNo + " ��ʵ������Ϊ�գ�������дɨ��������");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}
							
							// ���ɨ����������ʵ������ ������д��ɨ������ ֱ�ӽ�������
							if ((ScanQty > body.getNassistnum().intValue() && UpdateType == 2)
									|| (UpdateType == 1 && (ScanQty + Integer
											.parseInt(body.getVbdef20() == null ? "0"
													: body.getVbdef20())) > body
													.getNassistnum().intValue())) {
								CommonUtil.putFailResult(para, "�к�" + LineNo
										+ "������ɨ����������ʵ������");
								LoggerUtil.error("�к�" + LineNo + "������ɨ����������ʵ������");
								
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}

							if (UpdateType == 1) {
								int num = Integer
										.parseInt(body.getVbdef20() == null ? "0"
												: body.getVbdef20())
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
								LoggerUtil.error("������������");
							}
							break;
						}
					}
					// û���ҵ���Ӧ�кŵ��������ⵥ�ӱ�
					if (!flag && (UpdateType == 1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "�������ⵥ�� " + OrderNo
								+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
						LoggerUtil.error("�������ⵥ�� " + OrderNo + " �Ҳ�����Ӧ�к�Ϊ��"
								+ LineNo + "���ӱ�");
					}
				} else {
					CommonUtil.putFailResult(para, "�õ���Ϊ������״̬�������޸�");
					LoggerUtil.error("�õ��ݺ�" + OrderNo + "Ϊ������״̬�������޸�");
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
						LoggerUtil.error("д���������ɨ�������쳣 ", e);
					}
				}
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("д���������ɨ�������쳣 ", e);
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	/**
	 * ��������
	 * 
	 * @param OrderNo
	 * @param LineNo
	 * @param UpdateType
	 * @param ScanQty
	 * @return
	 */
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
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
				LoggerUtil.error("�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
			} else {
				GeneralOutHeadVO headVO = list.get(0).getHead();
				// ��ȡ����״̬ 1-ɾ�� 2-���� 3-ǩ��
				if (headVO.getFbillflag() == 2) {
					for (GeneralOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {

							// ʵ������Ϊ�� ��ʾ����
							if (body.getNassistnum() == null) {
								CommonUtil.putFailResult(para, "�к� " + LineNo
										+ " ��ʵ������Ϊ�գ�������дɨ��������");
								LoggerUtil.error("�к�" + LineNo + " ��ʵ������Ϊ�գ�������дɨ��������");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}
							
							// ���ݸ��������ж� ���ɨ����������Ӧ������ ������д��ɨ������ ֱ�ӽ�������
							if ((ScanQty > body.getNassistnum()
									.intValue() && UpdateType == 2)
									|| (UpdateType == 1 && (ScanQty + Integer
											.parseInt(body.getVbdef20() == null ? "0"
													: body.getVbdef20())) > body
											.getNassistnum().intValue())) {
								CommonUtil.putFailResult(para, "�к�" + LineNo
										+ "������ɨ����������ʵ������");
								LoggerUtil.error("�к�" + LineNo
										+ "������ɨ����������ʵ������");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}

							if (UpdateType == 1) {
								int num = Integer
										.parseInt(body.getVbdef20() == null ? "0"
												: body.getVbdef20())
										+ ScanQty;
								body.setVbdef20(new String(num + ""));
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setVbdef20(new String(ScanQty + ""));
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "������������");
								LoggerUtil.error("������������");
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// û���ҵ���Ӧ�кŵ��������ⵥ�ӱ�
					if (!flag && (UpdateType == 1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "�������ⵥ�� " + OrderNo
								+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
						LoggerUtil.error("�������ⵥ�� " + OrderNo + " �Ҳ�����Ӧ�к�Ϊ��"
								+ LineNo + "���ӱ�");
					}
				} else {
					CommonUtil.putFailResult(para, "�õ���Ϊ������״̬�������޸�");
					LoggerUtil.error("�õ���Ϊ������״̬�������޸�");
				} // end if ����״̬
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("д����������ɨ�������쳣 ", e);
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "д�����ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("д����������ɨ�������쳣 ", e);
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	/**
	 * ���۳���
	 * 
	 * @param OrderNo
	 * @param LineNo
	 * @param UpdateType
	 * @param ScanQty
	 * @return
	 */
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
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "���۳��ⵥ��" + OrderNo
						+ "�Ҳ�����Ӧ�����۳��ⵥ");
				LoggerUtil.error("���۳��ⵥ��" + OrderNo + "�Ҳ�����Ӧ�����۳��ⵥ");

			} else {
				SaleOutHeadVO headVO = (SaleOutHeadVO) list.get(0).getHead();
				// ��ȡ����״̬ 1-ɾ�� 2-���� 3-ǩ��
				if (headVO.getFbillflag() == 2) {
					for (SaleOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {

							// ʵ������Ϊ�� ��ʾ����
							if (body.getNassistnum() == null) {
								CommonUtil.putFailResult(para, "�к� " + LineNo
										+ " ��ʵ������Ϊ�գ�������дɨ��������");
								LoggerUtil.error("�к�" + LineNo + " ��ʵ������Ϊ�գ�������дɨ��������");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}
							
							// ���ɨ����������ʵ������ ������д��ɨ������ ֱ�ӽ�������
							if ((ScanQty > body.getNassistnum()
									.intValue() && UpdateType == 2)
									|| (UpdateType == 1 && (ScanQty + Integer
											.parseInt(body.getVbdef20() == null ? "0"
													: body.getVbdef20())) > body
											.getNassistnum().intValue())) {
								CommonUtil.putFailResult(para, "�к�" + LineNo
										+ "������ɨ����������ʵ������");
								LoggerUtil.error("�к�" + LineNo
										+ "������ɨ����������ʵ������");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}

							if (UpdateType == 1) {
								int num = Integer
										.parseInt(body.getVbdef20() == null ? "0"
												: body.getVbdef20())
										+ ScanQty;
								body.setVbdef20(new String(num + ""));
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setVbdef20(new String(ScanQty + ""));
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "������������");
								break; // �˴�Ӧ��ֱ������ѭ������ִ�н������� update ����
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// û���ҵ���Ӧ�кŵ����۳��ⵥ�ӱ�
					if (!flag && (UpdateType == 1 && UpdateType == 2)) {
						CommonUtil.putFailResult(para, "���۳��ⵥ�� " + OrderNo
								+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
						LoggerUtil.error("���۳��ⵥ�� " + OrderNo + " �Ҳ�����Ӧ�к�Ϊ��"
								+ LineNo + "���ӱ�");
					}
				} else {
					CommonUtil.putFailResult(para, "�����۵�Ϊ������״̬�������޸�");
					LoggerUtil.error("�����۵�Ϊ������״̬�������޸�");
				}
			}
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("д�����۳���ɨ�������쳣 ", e);
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "д�����ݿ�ʧ��" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("д�����۳���ɨ�������쳣 ", e);
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	/**
	 * 2.6 ��д���ⵥʵ������
	 */
	@Override
	public String saveOuntboundOutNum_requireNew(String xml) {
		LoggerUtil.debug("д�����ʵ������ saveOuntboundOutNum_requireNew " + xml);
		HashMap<String, Object> para = new HashMap<String, Object>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String OrderNo = obj.getString("OrderNo");
		String LineNo = obj.getString("LineNo");
		int UpdateType = obj.getInt("UpdateType");
		double ScanQty = obj.getDouble("ScanQty");

		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";
		boolean flag = false;

		try {
			List<GeneralOutVO> list = (List<GeneralOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutVO.class, sqlWhere, true, false);

			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "�������ⵥ��" + OrderNo + "�Ҳ�����Ӧ�ĵ���");
				LoggerUtil.debug("д�����ʵ�����������Ҳ�����Ӧ���� ");
				return FreeMarkerUtil.process(para,
						"nc/config/ic/barcode/WriteOutBoundOrder.fl");
			} else {
				GeneralOutHeadVO head = list.get(0).getHead();
				// ��ȡ����״̬ 2-����̬�����޸�״̬������״̬�������޸�
				if (head.getFbillflag() == 2) {
					for (GeneralOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {
							// nassistnum ʵ������ nnum ʵ�������� ʵ������*������=ʵ��������
							// vchangerate ������
							String[] vchangeate = body.getVchangerate().split(
									"/");
							UFDouble vc1 = new UFDouble(vchangeate[0]);
							UFDouble vc2 = new UFDouble(vchangeate[1]);
							// if (vc2 == 0) {
							// CommonUtil.putFailResult(para, "�����ʳ�������Ϊ0��");
							// LoggerUtil.error("�����ʳ�������Ϊ0��");
							// break;
							// }
							// �������� 1-׷�� 2-��д
							if (UpdateType == 1) {
								body.setNassistnum(new UFDouble(ScanQty
										+ Double.parseDouble(body
												.getNassistnum() == null ? "0"
												: body.getNassistnum()
														.toString())));
								body.setNnum(new UFDouble(ScanQty)
										.multiply(vc1.div(vc2))
										.add(body.getNnum() == null ? UFDouble.ZERO_DBL
												: body.getNnum()));
								body.setStatus(VOStatus.UPDATED);
								flag = true;
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setNassistnum(new UFDouble(ScanQty));
								body.setNnum(new UFDouble(ScanQty).multiply(vc1
										.div(vc2)));
								body.setStatus(VOStatus.UPDATED);
								flag = true;
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "���ݸ�����������");
								LoggerUtil.error("���ݸ�����������");
								break;
							} // end if updateType ��������
						}// end if LineNo �к�
					}// end for
				} else {
					CommonUtil.putFailResult(para, "�õ���Ϊ������״̬�������޸�");
					LoggerUtil.error("�õ���Ϊ������״̬�������޸�");
				}
				// flag==true ��ʾ�ҵ���Ӧ�кŵ��ݣ������и���
				if (flag == true) {
					IPFBusiAction pf = NCLocator.getInstance().lookup(
							IPFBusiAction.class);
					InvocationInfoProxy.getInstance().setUserId(
							head.getBillmaker());
					InvocationInfoProxy.getInstance().setGroupId(
							head.getPk_group());
					InvocationInfoProxy.getInstance().setBizDateTime(
							System.currentTimeMillis());
					pf.processAction("WRITE", "4I", null, list.get(0), null,
							null);
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "�������ⵥ�� " + OrderNo
							+ " �Ҳ�����Ӧ�к�Ϊ��" + LineNo + "���ӱ�");
					LoggerUtil.error("�������ⵥ�� " + OrderNo + " �Ҳ�����Ӧ�к�Ϊ��"
							+ LineNo + "���ӱ�");
				}
			}

		} catch (MetaDataException e) {
			LoggerUtil.error("д�����ʵ�������쳣 ", e);
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
			e.printStackTrace();
		} catch (BusinessException e) {
			LoggerUtil.error("д�����ʵ�������쳣 ", e);
			CommonUtil.putFailResult(para, "д�����ݿ�ʧ�ܣ�" + e.getMessage());
			e.printStackTrace();
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
		LoggerUtil.debug("д�����ʵ���������  saveOuntboundOutNum_requireNew " + rst);
		return rst;
	}

}
