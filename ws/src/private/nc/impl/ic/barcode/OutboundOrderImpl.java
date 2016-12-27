package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.ReadProductOrder;
import nc.bs.ic.barcode.WsQueryBS;
import nc.bs.ic.pub.util.ICBillVOQuery;
import nc.bs.scmpub.pf.PfParameterUtil;

import java.util.List;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.itf.ic.m4i.IGeneralOutMaintain;
import nc.itf.uap.pf.IPFBusiAction;
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
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDouble;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class OutboundOrderImpl implements IOutboundOrder {

	public String getOutboundOrder(String transationType, String orderNo) {
		ReadProductOrder readproductorder = new ReadProductOrder();
		if ("4C".equals(transationType)) {
			return readproductorder.RaadSaleOrder(orderNo);
		}
		if ("4A".equals(transationType)) {
			return readproductorder.RaadGeneralOutOrder(orderNo);
		}
		if ("4Y".equals(transationType)) {
			return readproductorder.RaadTransOutOrder(orderNo);
		}
		return readproductorder.Error(orderNo);
	}

	/**
	 * 回写出库单的扫描数量
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
			CommonUtil.putFailResult(para, "没有与交易类型" + TransationType
					+ "对应的出库业务");
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
			// false-未找到单号对应行号的数据
			boolean flag = false;

			List<TransOutVO> list = (List<TransOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutVO.class, sqlWhere, true, false);

			// 未查询到单号为 OrderNo 的出库单
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "调拨出库单号" + OrderNo + "找不到对应的单据");
			} else {
				TransOutHeadVO headVO = list.get(0).getHead();

				// 获取单据状态 1-删除 2-自由 3-签字
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
								CommonUtil.putFailResult(para, "更新类型有误！");
							}
						}
					}
					// 没有找到对应行号的其他出库单子表
					if (!flag && (UpdateType == 1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "调拨出库单号 " + OrderNo
								+ " 找不到对应行号为：" + LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该单据为非自由状态，不可修改");
				} // end if 单据状态

				// 查找到对应单据并且有数据修改
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
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
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
			// false-未找到单号对应行号的数据
			boolean flag = false;

			List<GeneralOutVO> list = (List<GeneralOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutVO.class, sqlWhere, true, false);
			// 未查询到单号为 OrderNo 的出库单
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "其他出库单号" + OrderNo + "找不到对应的单据");
			} else {
				GeneralOutHeadVO headVO = list.get(0).getHead();
				// 获取单据状态 1-删除 2-自由 3-签字
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
								CommonUtil.putFailResult(para, "更新类型有误！");
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// 没有找到对应行号的其他出库单子表
					if (!flag && (UpdateType == 1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "其他出库单号 " + OrderNo
								+ " 找不到对应行号为：" + LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该单据为非自由状态，不可修改");
				} // end if 单据状态
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
			e.printStackTrace();
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "写入数据库失败" + e.getMessage());
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
			// false-未找到单号对应行号的数据
			boolean flag = false;

			List<SaleOutVO> list = (List<SaleOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							SaleOutVO.class, sqlWhere, true, false);
			// 未查询到单号为 OrderNo 的销售出库单
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "销售出库单号" + OrderNo
						+ "找不到对应的销售出库单");
			} else {
				SaleOutHeadVO headVO = (SaleOutHeadVO) list.get(0).getHead();
				// 获取单据状态 1-删除 2-自由 3-签字
				if (headVO.getFbillflag() == 2) {
					for (SaleOutBodyVO body : list.get(0).getBodys()) {
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
								CommonUtil.putFailResult(para, "更新类型有误！");
								break; // 此次应该直接跳出循环，不执行接下来的 update 操作
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// 没有找到对应行号的销售出库单子表
					if (!flag && (UpdateType == 1 && UpdateType == 2)) {
						CommonUtil.putFailResult(para, "销售出库单号 " + OrderNo
								+ " 找不到对应行号为：" + LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该销售单为非自由状态，不可修改");
				}
			}
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
			e.printStackTrace();
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "写入数据库失败" + e.getMessage());
			e.printStackTrace();
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	@Override
	public String saveOuntboundOutNum_requireNew(String xml) {

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
				CommonUtil.putFailResult(para, "其它出库单号" + OrderNo + "找不到对应的单据");
				return FreeMarkerUtil.process(para,
						"nc/config/ic/barcode/WriteOutBoundOrder.fl");
			} else {
				GeneralOutHeadVO head = list.get(0).getHead();
				// 获取单据状态 2-自由态，可修改状态，其他状态不允许修改
				if (head.getFbillflag() == 2) {
					for (GeneralOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {
							// nassistnum 实发数量 nnum 实发主数量 实发数量*换算率=实发主数量
							// vchangerate 换算率

							String[] vchangeate = body.getVchangerate().split(
									"/");
							int vc1 = Integer.parseInt(vchangeate[0].substring(
									0, vchangeate[0].indexOf(".")));
							int vc2 = Integer.parseInt(vchangeate[1].substring(
									0, vchangeate[1].indexOf(".")));
							if (vc2 == 0) {
								CommonUtil.putFailResult(para, "换算率除数不能为0！");
								break;
							}
							// 更新类型 1-追加 2-覆写
							if (UpdateType == 1) {
								body.setNassistnum(new UFDouble(ScanQty
										+ body.getNassistnum().doubleValue()));
								body.setNnum(new UFDouble(ScanQty * vc1 / vc2
										+ body.getNnum().doubleValue()));
								body.setStatus(VOStatus.UPDATED);
								flag = true;
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setNassistnum(new UFDouble(ScanQty));
								body.setNnum(new UFDouble(ScanQty * vc1 / vc2));
								body.setStatus(VOStatus.UPDATED);
								flag = true;
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "单据更新类型有误！");
								break;
							} // end if updateType 更新类型
						}// end if LineNo 行号
					}// end for
				} else {
					CommonUtil.putFailResult(para, "该单据为非自由状态，不可修改");
				}
				// flag==true 表示找到对应行号单据，并进行更新
				if (flag == true) {
					 IPFBusiAction pf = NCLocator.getInstance().lookup(
					 IPFBusiAction.class);
					 InvocationInfoProxy.getInstance().setUserId(
					 head.getBillmaker());
					 InvocationInfoProxy.getInstance().setGroupId(
					 head.getPk_group());
					 InvocationInfoProxy.getInstance().setBizDateTime(
					 System.currentTimeMillis()); pf.processAction("WRITE",
					 "4I", null, list.get(0), null, null);
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "其他出库单号 " + OrderNo
							+ " 找不到对应行号为：" + LineNo + "的子表");
				}
			}

		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败！" + e.getMessage());
			e.printStackTrace();
		} catch (BusinessException e) {
			CommonUtil.putFailResult(para, "写入数据库失败！" + e.getMessage());
			e.printStackTrace();
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

}
