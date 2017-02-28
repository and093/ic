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
		LoggerUtil.debug("读取出库单 getOutboundOrder " + transationType + " - "
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
		LoggerUtil.debug("读取出库单结果  getOutboundOrder " + rst);
		return rst;
	}

	/**
	 * 2.5 回写出库单的扫码数量
	 */
	@Override
	public String saveOutboundBarcodeNum_requireNew(String xml) {
		LoggerUtil.debug("写入出库扫码数量 saveOutboundBarcodeNum_requireNew " + xml);
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String TransationType = obj.getString("TransationType"); // 交易类型
		String OrderNo = obj.getString("OrderNo"); // 单据号
		String LineNo = obj.getString("LineNo"); // 行号
		int UpdateType = obj.getInt("UpdateType"); // 更新类型
		int ScanQty = obj.getInt("ScanQty"); // 扫码箱数

		String rst = "";
		if ("4C".equals(TransationType)) {
			rst = writeSaleOrderBound(OrderNo, LineNo, UpdateType, ScanQty);
		} else if ("4A".equals(TransationType)) {
			rst = writeGeneralOut(OrderNo, LineNo, UpdateType, ScanQty);
		} else if ("4Y".equals(TransationType)) {
			rst = writeTransOut(OrderNo, LineNo, UpdateType, ScanQty);
		} else {
			HashMap<String, Object> para = new HashMap<String, Object>();
			CommonUtil.putFailResult(para, "没有与交易类型" + TransationType
					+ "对应的出库业务");
			LoggerUtil.error("没有与交易类型" + TransationType + "对应的出库业务");
			rst = FreeMarkerUtil.process(para,
					"nc/config/ic/barcode/WriteOutBoundOrder.fl");
		}
		LoggerUtil.debug("写入出库扫码数量结果 saveOutboundBarcodeNum_requireNew " + rst);
		return rst;
	}

	/**
	 * 调拨出库
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

			// 未查询到单号为 OrderNo 的出库单
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "调拨出库单号" + OrderNo + "找不到对应的单据");
				LoggerUtil.error("调拨出库单号" + OrderNo + "找不到对应的单据");
			} else {
				TransOutHeadVO headVO = list.get(0).getHead();

				// 获取单据状态 1-删除 2-自由 3-签字
				if (headVO.getFbillflag() == 2) {
					int len = list.get(0).getBodys().length;
					for (int i = 0; i < len; i++) {
						body = list.get(0).getBodys()[i];

						if (LineNo.equals(body.getCrowno())) {
							// 实发数量为空 提示出错
							if (body.getNassistnum() == null) {
								CommonUtil.putFailResult(para, "行号 " + LineNo
										+ " 的实发数量为空，不能填写扫描数量！");
								LoggerUtil.error("行号" + LineNo + " 的实发数量为空，不能填写扫描数量！");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}
							
							// 如果扫码箱数大于实发数量 不允许写入扫码数量 直接结束函数
							if ((ScanQty > body.getNassistnum().intValue() && UpdateType == 2)
									|| (UpdateType == 1 && (ScanQty + Integer
											.parseInt(body.getVbdef20() == null ? "0"
													: body.getVbdef20())) > body
													.getNassistnum().intValue())) {
								CommonUtil.putFailResult(para, "行号" + LineNo
										+ "的物料扫码箱数大于实发数量");
								LoggerUtil.error("行号" + LineNo + "的物料扫码箱数大于实发数量");
								
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
								CommonUtil.putFailResult(para, "更新类型有误！");
								LoggerUtil.error("更新类型有误！");
							}
							break;
						}
					}
					// 没有找到对应行号的其他出库单子表
					if (!flag && (UpdateType == 1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "调拨出库单号 " + OrderNo
								+ " 找不到对应行号为：" + LineNo + "的子表");
						LoggerUtil.error("调拨出库单号 " + OrderNo + " 找不到对应行号为："
								+ LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该单据为非自由状态，不可修改");
					LoggerUtil.error("该单据号" + OrderNo + "为非自由状态，不可修改");
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
						LoggerUtil.error("写入调拨出库扫码数量异常 ", e);
					}
				}
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("写入调拨出库扫码数量异常 ", e);
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	/**
	 * 其它出库
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
			// false-未找到单号对应行号的数据
			boolean flag = false;

			List<GeneralOutVO> list = (List<GeneralOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutVO.class, sqlWhere, true, false);
			// 未查询到单号为 OrderNo 的出库单
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "其他出库单号" + OrderNo + "找不到对应的单据");
				LoggerUtil.error("其他出库单号" + OrderNo + "找不到对应的单据");
			} else {
				GeneralOutHeadVO headVO = list.get(0).getHead();
				// 获取单据状态 1-删除 2-自由 3-签字
				if (headVO.getFbillflag() == 2) {
					for (GeneralOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {

							// 实发数量为空 提示出错
							if (body.getNassistnum() == null) {
								CommonUtil.putFailResult(para, "行号 " + LineNo
										+ " 的实发数量为空，不能填写扫描数量！");
								LoggerUtil.error("行号" + LineNo + " 的实发数量为空，不能填写扫描数量！");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}
							
							// 根据更新类型判断 如果扫码箱数大于应发数量 不允许写入扫码数量 直接结束函数
							if ((ScanQty > body.getNassistnum()
									.intValue() && UpdateType == 2)
									|| (UpdateType == 1 && (ScanQty + Integer
											.parseInt(body.getVbdef20() == null ? "0"
													: body.getVbdef20())) > body
											.getNassistnum().intValue())) {
								CommonUtil.putFailResult(para, "行号" + LineNo
										+ "的物料扫码箱数大于实发数量");
								LoggerUtil.error("行号" + LineNo
										+ "的物料扫码箱数大于实发数量");
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
								CommonUtil.putFailResult(para, "更新类型有误！");
								LoggerUtil.error("更新类型有误！");
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// 没有找到对应行号的其他出库单子表
					if (!flag && (UpdateType == 1 || UpdateType == 2)) {
						CommonUtil.putFailResult(para, "其他出库单号 " + OrderNo
								+ " 找不到对应行号为：" + LineNo + "的子表");
						LoggerUtil.error("其他出库单号 " + OrderNo + " 找不到对应行号为："
								+ LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该单据为非自由状态，不可修改");
					LoggerUtil.error("该单据为非自由状态，不可修改");
				} // end if 单据状态
			} // end if list.size()
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("写入其他出库扫码数量异常 ", e);
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "写入数据库失败" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("写入其他出库扫码数量异常 ", e);
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	/**
	 * 销售出库
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
			// false-未找到单号对应行号的数据
			boolean flag = false;
			List<SaleOutVO> list = (List<SaleOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							SaleOutVO.class, sqlWhere, true, false);
			// 未查询到单号为 OrderNo 的销售出库单
			if (list.size() == 0 || list == null) {
				CommonUtil.putFailResult(para, "销售出库单号" + OrderNo
						+ "找不到对应的销售出库单");
				LoggerUtil.error("销售出库单号" + OrderNo + "找不到对应的销售出库单");

			} else {
				SaleOutHeadVO headVO = (SaleOutHeadVO) list.get(0).getHead();
				// 获取单据状态 1-删除 2-自由 3-签字
				if (headVO.getFbillflag() == 2) {
					for (SaleOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {

							// 实发数量为空 提示出错
							if (body.getNassistnum() == null) {
								CommonUtil.putFailResult(para, "行号 " + LineNo
										+ " 的实发数量为空，不能填写扫描数量！");
								LoggerUtil.error("行号" + LineNo + " 的实发数量为空，不能填写扫描数量！");
								return FreeMarkerUtil
										.process(para,
												"nc/config/ic/barcode/WriteOutBoundOrder.fl");
							}
							
							// 如果扫码箱数大于实发数量 不允许写入扫码数量 直接结束函数
							if ((ScanQty > body.getNassistnum()
									.intValue() && UpdateType == 2)
									|| (UpdateType == 1 && (ScanQty + Integer
											.parseInt(body.getVbdef20() == null ? "0"
													: body.getVbdef20())) > body
											.getNassistnum().intValue())) {
								CommonUtil.putFailResult(para, "行号" + LineNo
										+ "的物料扫码箱数大于实发数量");
								LoggerUtil.error("行号" + LineNo
										+ "的物料扫码箱数大于实发数量");
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
						LoggerUtil.error("销售出库单号 " + OrderNo + " 找不到对应行号为："
								+ LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该销售单为非自由状态，不可修改");
					LoggerUtil.error("该销售单为非自由状态，不可修改");
				}
			}
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("写入销售出库扫码数量异常 ", e);
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "写入数据库失败" + e.getMessage());
			e.printStackTrace();
			LoggerUtil.error("写入销售出库扫码数量异常 ", e);
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
	}

	/**
	 * 2.6 回写出库单实发数量
	 */
	@Override
	public String saveOuntboundOutNum_requireNew(String xml) {
		LoggerUtil.debug("写入出库实发数量 saveOuntboundOutNum_requireNew " + xml);
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
				LoggerUtil.debug("写入出库实发数量错误，找不到对应单据 ");
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
							UFDouble vc1 = new UFDouble(vchangeate[0]);
							UFDouble vc2 = new UFDouble(vchangeate[1]);
							// if (vc2 == 0) {
							// CommonUtil.putFailResult(para, "换算率除数不能为0！");
							// LoggerUtil.error("换算率除数不能为0！");
							// break;
							// }
							// 更新类型 1-追加 2-覆写
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
								CommonUtil.putFailResult(para, "单据更新类型有误！");
								LoggerUtil.error("单据更新类型有误！");
								break;
							} // end if updateType 更新类型
						}// end if LineNo 行号
					}// end for
				} else {
					CommonUtil.putFailResult(para, "该单据为非自由状态，不可修改");
					LoggerUtil.error("该单据为非自由状态，不可修改");
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
							System.currentTimeMillis());
					pf.processAction("WRITE", "4I", null, list.get(0), null,
							null);
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "其他出库单号 " + OrderNo
							+ " 找不到对应行号为：" + LineNo + "的子表");
					LoggerUtil.error("其他出库单号 " + OrderNo + " 找不到对应行号为："
							+ LineNo + "的子表");
				}
			}

		} catch (MetaDataException e) {
			LoggerUtil.error("写入出库实发数量异常 ", e);
			CommonUtil.putFailResult(para, "查询数据库失败！" + e.getMessage());
			e.printStackTrace();
		} catch (BusinessException e) {
			LoggerUtil.error("写入出库实发数量异常 ", e);
			CommonUtil.putFailResult(para, "写入数据库失败！" + e.getMessage());
			e.printStackTrace();
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/WriteOutBoundOrder.fl");
		LoggerUtil.debug("写入出库实发数量结果  saveOuntboundOutNum_requireNew " + rst);
		return rst;
	}

}
