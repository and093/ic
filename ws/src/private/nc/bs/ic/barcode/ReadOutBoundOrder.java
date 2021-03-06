package nc.bs.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.WsQueryBS;
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
import nc.vo.pub.AggregatedValueObject;

public class ReadOutBoundOrder {
	/**
	 * 销售出库单读取，，
	 * 
	 * @param orderNo
	 * @return
	 */
	public String RaadSaleOrder(String orderNo) {
		
		HashMap<String, Object> para = new HashMap<String, Object>();
		para.put("detail", "null");
		String where = "nvl(dr,0) = 0 and vbillcode = '" + orderNo + "'";
		try {
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							SaleOutHeadVO.class, where, true, false);
			if (list != null && list.size() != 0) {
				SaleOutVO agg = (SaleOutVO) list.get(0);
				SaleOutHeadVO hvo = agg.getHead();
				SaleOutBodyVO[] bodys = agg.getBodys();
				// 将nc的仓库pk通过查询对照表转换为条码的仓库编码和名称，
				String pk_stordoc = hvo.getCwarehouseid();
				HashMap<String, Object> stordocMap = WsQueryBS
						.queryLocationInfoByPk(pk_stordoc);
				if (stordocMap != null && stordocMap.size() != 0) {
					para.put("SenderLocationCode",
							stordocMap.get("senderlocationcode"));
					para.put("SenderLocationName",
							stordocMap.get("senderlocationname"));
					para.put("Date", hvo.getDbilldate().toString());
					para.put("Remark", hvo.getVnote());
					para.put("Ccustomerid", hvo.getCcustomerid());
					HashMap<String, Object>  customer =WsQueryBS.queryCustomer( (String) para.get("Ccustomerid"));
					para.put("ReceiverLocationCode", customer.get("code"));
					para.put("ReceiverLocationName", customer.get("name"));
					ArrayList<HashMap<String, Object>> bodylist = new ArrayList<HashMap<String, Object>>();
					for (SaleOutBodyVO body : bodys) {
						HashMap<String, Object> bodypara = new HashMap<String, Object>();
						HashMap<String, Object> pk = WsQueryBS
								.queryMaterialInfoByPk(body.getCmaterialvid());
						bodypara.put("ProductCode", pk.get("productcode"));
						bodypara.put("ProductNo", pk.get("productno"));
						bodypara.put("ProductName", pk.get("productname"));
						bodypara.put("PackSize", pk.get("packsize"));
						bodypara.put("BatchNo", body.getVbatchcode());
						bodypara.put("LineNo", body.getCrowno());
						bodypara.put("PlanPackQty", body.getNshouldassistnum());
						bodypara.put("ActualPackQty", body.getNassistnum());
						bodypara.put("ScanQty",
								CommonUtil.getUFDouble(body.getVbdef20()));//
						// 转换主辅单位
						bodypara.put("PackUMName",
								WsQueryBS.queryUnitName(body.getCastunitid()));
						bodypara.put("ProductUMName",
								WsQueryBS.queryUnitName(body.getCunitid()));// 主单位
						String pk_material = body.getCmaterialoid();
						HashMap<String, Object> materailMap = WsQueryBS
								.queryMaterialInfoByPk(pk_material);
						bodypara.putAll(materailMap);
						bodylist.add(bodypara);
					}
					para.put("detail", bodylist);
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "单号" + orderNo
							+ "在仓库对照表没有相应的数据");
					LoggerUtil.error("单号" + orderNo + "在仓库对照表没有相应的数据");
				}
			} else {
				CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的销售出库单");
				LoggerUtil.error("单号" + orderNo + "找不到对应的销售出库单");
			}
		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("查询数据库失败：", e);
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("查询数据库失败：", e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/ReadOutBoundOrder.fl");
		//LoggerUtil.debug("销售订单读取结束：" + rst);
		return rst;
	}

	/**
	 * 调拨出库读取
	 * 
	 * @param orderNo
	 * @return
	 */
	public String RaadTransOutOrder(String orderNo) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		para.put("detail", "null");
		String where = "nvl(dr,0) = 0 and vbillcode = '" + orderNo + "'";
		try {
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutHeadVO.class, where, true, false);
			if (list != null && list.size() != 0) {
				TransOutVO agg = (TransOutVO) list.get(0);
				TransOutHeadVO hvo = agg.getHead();
				TransOutBodyVO[] bodys = agg.getBodys();
				// 将nc的仓库pk通过查询对照表转换为条码的仓库编码和名称
				String pk_stordoc = hvo.getCwarehouseid();
				HashMap<String, Object> stordocMap = WsQueryBS
						.queryLocationInfoByPk(pk_stordoc);
				
				HashMap<String, Object> instordocMap = WsQueryBS
						.queryLocationInfoByPk(hvo.getCotherwhid());
				
				
				if (stordocMap != null && stordocMap.size() != 0) {
					para.put("SenderLocationCode",
							stordocMap.get("senderlocationcode"));
					para.put("SenderLocationName",
							stordocMap.get("senderlocationname"));
					para.put("ReceiverLocationCode", instordocMap.get("senderlocationcode"));
					para.put("ReceiverLocationName", instordocMap.get("senderlocationname"));
					para.put("Date", hvo.getDbilldate().toString());
					para.put("Remark", hvo.getVnote());
					ArrayList<HashMap<String, Object>> bodylist = new ArrayList<HashMap<String, Object>>();
					for (TransOutBodyVO body : bodys) {
						HashMap<String, Object> bodypara = new HashMap<String, Object>();
						HashMap<String, Object> pk = WsQueryBS
								.queryMaterialInfoByPk(body.getCmaterialvid());
						bodypara.put("ProductCode", pk.get("productcode"));
						bodypara.put("ProductNo", pk.get("productno"));
						bodypara.put("ProductName", pk.get("productname"));
						bodypara.put("PackSize", pk.get("packsize"));
						bodypara.put("BatchNo", body.getVbatchcode());
						bodypara.put("LineNo", body.getCrowno());
						bodypara.put("PlanPackQty", body.getNshouldassistnum());
						bodypara.put("ActualPackQty", body.getNassistnum());
						bodypara.put("ScanQty",
								CommonUtil.getUFDouble(body.getVbdef20()));
						// 转换主辅单位
						bodypara.put("PackUMName",
								WsQueryBS.queryUnitName(body.getCastunitid()));
						bodypara.put("ProductUMName",
								WsQueryBS.queryUnitName(body.getCunitid()));// 主单位
						String pk_material = body.getCmaterialoid();
						HashMap<String, Object> materailMap = WsQueryBS
								.queryMaterialInfoByPk(pk_material);
						bodypara.putAll(materailMap);
						bodylist.add(bodypara);
					}
					para.put("detail", bodylist);
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "单号" + orderNo
							+ "在仓库对照表没有相应的数据");
					LoggerUtil.error("单号" + orderNo + "在仓库对照表没有相应的数据");
				}
			} else {
				CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的调拨出库单");
				LoggerUtil.error("单号" + orderNo + "找不到对应的调拨出库单");
			}
		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("查询数据库失败：", e);
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("查询数据库失败：", e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/ReadOutBoundOrder.fl");
		//LoggerUtil.debug("调拨出库单读取结束：" + rst);
		return rst;
	}

	/**
	 * 其他出库单读取
	 * 
	 * @param orderNo
	 * @return
	 */
	public String RaadGeneralOutOrder(String orderNo) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		para.put("detail", "null");
		String where = "nvl(dr,0) = 0 and vbillcode = '" + orderNo + "'";
		try {
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutHeadVO.class, where, true, false);
			if (list != null && list.size() != 0) {
				GeneralOutVO agg = (GeneralOutVO) list.get(0);
				GeneralOutHeadVO hvo = agg.getHead();
				GeneralOutBodyVO[] bodys = agg.getBodys();
				HashMap<String, Object>  warehouseOutMap = WsQueryBS.queryLocationInfoByPk(hvo.getCwarehouseid());
				HashMap<String, Object> warehouseInMap = WsQueryBS.queryLocationInfoByPk(hvo.getCotherwhid());
				 
				if (warehouseOutMap != null && warehouseOutMap.size() != 0) {
					para.put("SenderLocationCode",
							warehouseOutMap.get("senderlocationcode"));
					para.put("SenderLocationName",
							warehouseOutMap.get("senderlocationname"));
					para.put("ReceiverLocationCode", warehouseInMap.get("senderlocationcode"));
					para.put("ReceiverLocationName", warehouseInMap.get("senderlocationname"));
					para.put("Date", hvo.getDbilldate().toString());
					para.put("Remark", hvo.getVnote());
					ArrayList<HashMap<String, Object>> bodylist = new ArrayList<HashMap<String, Object>>();
					for (GeneralOutBodyVO body : bodys) {
						//如果实发数量为0，则不返回给条码 20170722
						if(body.getNassistnum() == null || body.getNassistnum().doubleValue() == 0D){
							continue;
						} 
						HashMap<String, Object> bodypara = new HashMap<String, Object>();
						HashMap<String, Object> pk = WsQueryBS
								.queryMaterialInfoByPk(body.getCmaterialvid());
						bodypara.put("ProductCode", pk.get("productcode"));
						bodypara.put("ProductNo", pk.get("productno"));
						bodypara.put("ProductName", pk.get("productname"));
						bodypara.put("PackSize", pk.get("packsize"));
						bodypara.put("BatchNo", body.getVbatchcode());//
						bodypara.put("LineNo", body.getCrowno());
						bodypara.put("PlanPackQty", body.getNshouldassistnum());
						bodypara.put("ActualPackQty", body.getNassistnum());
						bodypara.put("ScanQty",
								CommonUtil.getUFDouble(body.getVbdef20()));
						// 转换主辅单位
						bodypara.put("PackUMName",
								WsQueryBS.queryUnitName(body.getCastunitid()));
						bodypara.put("ProductUMName",
								WsQueryBS.queryUnitName(body.getCunitid()));// 主单位
						String pk_material = body.getCmaterialoid();
						HashMap<String, Object> materailMap = WsQueryBS
								.queryMaterialInfoByPk(pk_material);
						bodypara.putAll(materailMap);
						bodylist.add(bodypara);
					}
					para.put("detail", bodylist);
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "单号" + orderNo
							+ "在仓库对照表没有相应的数据");
					LoggerUtil.error("单号" + orderNo + "在仓库对照表没有相应的数据");
				}
			} else {
				CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的其他出库单");
				LoggerUtil.error("单号" + orderNo + "找不到对应的其他出库单");
			}
		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("查询数据库失败：", e);
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("查询数据库失败：", e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/ReadOutBoundOrder.fl");
		//LoggerUtil.debug("调拨出库单读取结束：" + rst);
		return rst;
	}

	/**
	 * 没有找到单号对应的出库类型
	 * 
	 * @param orderNo
	 * @return
	 */
	public String Error(String orderNo) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的出库类型");
		LoggerUtil.error("单号" + orderNo + "找不到对应的出库类型或者出入库类型输入有误");
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/ReadOutBoundOrder.fl");
		return rst;
	}

}
