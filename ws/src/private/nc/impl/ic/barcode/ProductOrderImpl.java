package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.WsQueryBS;
import nc.ift.ic.barcode.IProductOrder;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;

public class ProductOrderImpl implements IProductOrder {

	@Override
	public String getProductOrder(String batchcode) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		BaseDAO dao = new BaseDAO();
		//根据生产批次号查询生产订单明细行
		String where = " nvl(dr,0) = 0 and vbatchcode = '"+batchcode+"'";
		try {
			Collection<PMOItemVO> col = dao.retrieveByClause(PMOItemVO.class, where);
			if(col == null || col.size() == 0){
				CommonUtil.putFailResult(para, batchcode + "查询不到对应的生产订单数据");
			} else {
				CommonUtil.putSuccessResult(para);
				ArrayList<HashMap<String, Object>> details = new ArrayList<HashMap<String, Object>>();
				for(PMOItemVO item : col){
					HashMap<String, Object> detail = new HashMap<String, Object>();
					detail.put("PlanProductionDate", item.getTplanstarttime().getDate().toString());
					detail.put("OrderStatus", item.getFitemstatus());
					detail.put("PlanPackQty", item.getNmmastnum());
					//条码车间对应nc的部门，根据部门对照表转换
					detail.putAll(WsQueryBS.queryWorkShop(item.getCdeptid()));
					//生产线目前直接取nc的，应该还需要做对照
					detail.putAll(WsQueryBS.queryWorkLine(item.getCwkid()));
					//读取物料信息
					detail.putAll(WsQueryBS.queryMaterialInfoByPk(item.getCmaterialid()));
					//读取物料单位，现在取的是主单位
					detail.put("ProductUM", WsQueryBS.queryUnitName(item.getCunitid()));
					detail.put("IsNeedToCheckProduct", "T"); //物料是否需要检验
					
					details.add(detail);
				}
				para.put("Details", details);
			}
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/productionOrderl.fl");
	}

	@Override
	public String saveProductInbound_requireNew(String xml) {
		
		return null;
	}

}
