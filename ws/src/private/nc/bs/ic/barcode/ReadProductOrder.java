package nc.bs.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.ReadProductOrder;
import nc.bs.ic.barcode.WsQueryBS;
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

public class ReadProductOrder {
	/**
	 * 销售出库单读取
	 * @param orderNo
	 * @return
	 */
	public String RaadSaleOrder(String orderNo){
	HashMap<String, Object> para = new HashMap<String, Object>();
	String where = "nvl(dr,0) = 0 and vbillcode = '"+orderNo+"'";
	try {
		List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
		        .lookupPersistenceQueryService().queryBillOfVOByCond(
		        		SaleOutHeadVO.class, where, true, false);
		if(list != null && list.size() == 0){
			SaleOutVO agg = (SaleOutVO)list.get(0);
			SaleOutHeadVO hvo = agg.getHead();
			SaleOutBodyVO[] bodys = agg.getBodys();
			//将nc的仓库pk通过查询对照表转换为条码的仓库编码和名称
			//
			String pk_stordoc = hvo.getCwarehouseid();
			HashMap<String, Object> stordocMap = WsQueryBS.queryLocationInfoByPk(pk_stordoc);
			para.put("detail", stordocMap);
			
			para.put("Date", hvo.getDbilldate().toString());
			para.put("Remark", hvo.getVnote());
			
			ArrayList<HashMap<String, Object>> bodylist = new ArrayList<HashMap<String, Object>>();
			for(SaleOutBodyVO body : bodys){
				HashMap<String, Object> bodypara = new HashMap<String, Object>();
				bodypara.put("BatchNo", body.getVbatchcode());
				bodypara.put("LineNo", body.getCrowno());
				bodypara.put("PlanPackQty", body.getNshouldassistnum());
				bodypara.put("ActualPackQty", body.getNassistnum());
				bodypara.put("ScanQty", CommonUtil.getUFDouble(body.getVbdef20()));
				
				// 转换主辅单位
				bodypara.put("ProductUMName", WsQueryBS.queryUnitName(body.getCunitid()));
				bodypara.put("ActualPackQty", WsQueryBS.queryUnitName(body.getCastunitid()));
				
				String pk_material = body.getCmaterialoid();
				HashMap<String, Object> materailMap = WsQueryBS.queryMaterialInfoByPk(pk_material);
				bodypara.putAll(materailMap);
				
				bodylist.add(bodypara);
			}
			
			para.put("detail", bodylist);
			
			return FreeMarkerUtil.process(para,"nc/config/ic/barcode/material.fl");
			
		} else {
			CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的出库单");
		}
	} catch (MetaDataException e) {
		e.printStackTrace();
		CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
	} catch (DAOException e) {
		e.printStackTrace();
		CommonUtil.putFailResult(para, "仓库对照表没有相应的数据");
	}

return null;
}
	/**
	 * 调拨出库读取
	 * @param orderNo
	 * @return
	 */
	public String RaadTransOutOrder(String orderNo){
		HashMap<String, Object> para = new HashMap<String, Object>();
		String where = "nvl(dr,0) = 0 and vbillcode = '"+orderNo+"'";
		try {
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
			        .lookupPersistenceQueryService().queryBillOfVOByCond(
			        		SaleOutHeadVO.class, where, true, false);
			if(list != null && list.size() == 0){
				TransOutVO agg = (TransOutVO)list.get(0);
				TransOutHeadVO hvo = agg.getHead();
				TransOutBodyVO[] bodys = agg.getBodys();
				//将nc的仓库pk通过查询对照表转换为条码的仓库编码和名称
				String pk_stordoc = hvo.getCwarehouseid();
				HashMap<String, Object> stordocMap = WsQueryBS.queryLocationInfoByPk(pk_stordoc);
				para.put("detail", stordocMap);
				
				para.put("Date", hvo.getDbilldate().toString());
				para.put("Remark", hvo.getVnote());
				
				ArrayList<HashMap<String, Object>> bodylist = new ArrayList<HashMap<String, Object>>();
				for(TransOutBodyVO body : bodys){
					HashMap<String, Object> bodypara = new HashMap<String, Object>();
					bodypara.put("BatchNo", body.getVbatchcode());
					bodypara.put("LineNo", body.getCrowno());
					bodypara.put("PlanPackQty", body.getNshouldassistnum());
					bodypara.put("ActualPackQty", body.getNassistnum());
					bodypara.put("ScanQty", CommonUtil.getUFDouble(body.getVbdef20()));
					
					// 转换主辅单位
					bodypara.put("ProductUMName", WsQueryBS.queryUnitName(body.getCunitid()));
					bodypara.put("ActualPackQty", WsQueryBS.queryUnitName(body.getCastunitid()));
					
					String pk_material = body.getCmaterialoid();
					HashMap<String, Object> materailMap = WsQueryBS.queryMaterialInfoByPk(pk_material);
					bodypara.putAll(materailMap);
					
					bodylist.add(bodypara);
				}
				
				para.put("detail", bodylist);
				
				return FreeMarkerUtil.process(para,"nc/config/ic/barcode/material.fl");
				
			} else {
				CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的出库单");
			}
		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "仓库对照表没有相应的数据");
		}
	return null;
	}
	/**
	 * 其他出库单读取
	 * @param orderNo
	 * @return
	 */
	public String RaadGeneralOutOrder(String orderNo){
		HashMap<String, Object> para = new HashMap<String, Object>();
		String where = "nvl(dr,0) = 0 and vbillcode = '"+orderNo+"'";
		try {
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
			        .lookupPersistenceQueryService().queryBillOfVOByCond(
			        		SaleOutHeadVO.class, where, true, false);
			if(list != null && list.size() == 0){
				GeneralOutVO agg = (GeneralOutVO)list.get(0);
				GeneralOutHeadVO hvo = agg.getHead();
				GeneralOutBodyVO[] bodys = agg.getBodys();
				//将nc的仓库pk通过查询对照表转换为条码的仓库编码和名称
				String pk_stordoc = hvo.getCwarehouseid();
				HashMap<String, Object> stordocMap = WsQueryBS.queryLocationInfoByPk(pk_stordoc);
				para.put("detail", stordocMap);
				
				para.put("Date", hvo.getDbilldate().toString());
				para.put("Remark", hvo.getVnote());
				
				ArrayList<HashMap<String, Object>> bodylist = new ArrayList<HashMap<String, Object>>();
				for(GeneralOutBodyVO body : bodys){
					HashMap<String, Object> bodypara = new HashMap<String, Object>();
					bodypara.put("BatchNo", body.getVbatchcode());
					bodypara.put("LineNo", body.getCrowno());
					bodypara.put("PlanPackQty", body.getNshouldassistnum());
					bodypara.put("ActualPackQty", body.getNassistnum());
					bodypara.put("ScanQty", CommonUtil.getUFDouble(body.getVbdef20()));
					
					// 转换主辅单位
					bodypara.put("ProductUMName", WsQueryBS.queryUnitName(body.getCunitid()));
					bodypara.put("ActualPackQty", WsQueryBS.queryUnitName(body.getCastunitid()));
					
					String pk_material = body.getCmaterialoid();
					HashMap<String, Object> materailMap = WsQueryBS.queryMaterialInfoByPk(pk_material);
					bodypara.putAll(materailMap);
					
					bodylist.add(bodypara);
				}
				
				para.put("detail", bodylist);
				
				return FreeMarkerUtil.process(para,"nc/config/ic/barcode/material.fl");
				
			} else {
				CommonUtil.putFailResult(para, "单号" + orderNo + "找不到对应的出库单");
			}
		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "仓库对照表没有相应的数据");
		}

	return null;
	}


}
