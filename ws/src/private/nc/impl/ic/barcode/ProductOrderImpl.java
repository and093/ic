package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.WsQueryBS;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.IProductOrder;
import nc.itf.mmpac.wr.IWrBusinessService;
import nc.itf.mmpac.wr.pwr.IPwrMaintainService;
import nc.itf.uap.pf.IPFBusiAction;
import nc.itf.uap.pf.IPfExchangeService;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.pub.ic.barcode.LoggerUtil;
import nc.vo.am.common.util.CloneUtil;
import nc.vo.ic.m46.entity.FinProdInBodyVO;
import nc.vo.ic.m46.entity.FinProdInHeadVO;
import nc.vo.ic.m46.entity.FinProdInVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOHeadVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;
import nc.vo.mmpac.wr.entity.AggWrVO;
import nc.vo.mmpac.wr.entity.WrItemVO;
import nc.vo.mmpac.wr.entity.WrQualityVO;
import nc.vo.mmpac.wr.entity.WrVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class ProductOrderImpl implements IProductOrder {

	@Override
	public String getProductOrder(String batchcode) {
		LoggerUtil.debug("entry ProductOrderImpl getProductOrder " + batchcode);
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
					if(item.getFitemstatus() != 1 && item.getFitemstatus() != 2){
						throw new BusinessException("生产订单行状态不是投放或者完工");
					}
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
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("ProductOrderImpl getProductOrder error ", e);
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/productionOrderl.fl");
		LoggerUtil.debug("leave ProductOrderImpl getProductOrder " + rst);
		return rst;
	}

	@Override
	public String saveProductInbound_requireNew(String xml) {
		LoggerUtil.debug("entry ProductOrderImpl saveProductInbound_requireNew " + xml);
		HashMap<String, Object> para = new HashMap<String, Object>();
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		String receiverLocationCode = obj.getString("ReceiverLocationCode"); //入库仓库
		String senderLocationCode = obj.getString("SenderLocationCode"); //发货方 车间
		String sender = obj.getString("Sender"); //操作人
		String receiver = obj.getString("Receiver"); //收货人
		String date = obj.getString("Date"); //单据日期
		String sourceOrderNo = obj.getString("SourceOrderNo"); //源单号（生产订单表体生产批次号）
		JSONArray arrays = obj.getJSONArray("items");
		
		InvocationInfoProxy.getInstance().setUserId(WsQueryBS.getUserid(sender));
		BaseDAO dao = new BaseDAO();
		//根据生产批次号查询生产订单明细行
		String where = " nvl(dr,0) = 0 and vbatchcode = '"+sourceOrderNo+"'";
		try {
			Collection<PMOItemVO> col = dao.retrieveByClause(PMOItemVO.class, where);
			if(col == null || col.size() == 0){
				CommonUtil.putFailResult(para, sourceOrderNo + "查询不到对应的生产订单数据");
			} else {
				PMOItemVO pmoitem = col.iterator().next();
				if(pmoitem.getFitemstatus() != 1 && pmoitem.getFitemstatus() != 2){
					throw new BusinessException("生产订单行状态不是投放或者完工,不能入库");
				}
				PMOHeadVO pmohead = (PMOHeadVO)dao.retrieveByPK(PMOHeadVO.class, pmoitem.getCpmohid());
				PMOAggVO pmoagg = new PMOAggVO();
				pmoagg.setParentVO(pmohead);
				pmoagg.setChildrenVO(new PMOItemVO[]{pmoitem});
				
				InvocationInfoProxy.getInstance().setGroupId(pmohead.getPk_group());
				IPfExchangeService exchangeService = NCLocator.getInstance().lookup(IPfExchangeService.class);
				AggWrVO wragg = (AggWrVO)exchangeService.runChangeData("55A2", "55A4", pmoagg, null);
				
				HashMap<String, Object> deptmap = WsQueryBS.queryDeptidByCode(senderLocationCode, pmohead.getPk_org());
				String pk_dept = (String)deptmap.get("pk_dept");
				String pk_vid = (String)deptmap.get("pk_vid");
				
				HashMap<String, String> stormap = WsQueryBS.queryStordocByCode(receiverLocationCode);
				if(stormap == null || stormap.size() == 0){
					throw new BusinessException("收货方" + receiverLocationCode + "在仓库对照找不到对应的仓库档案");
				}
				
				WrVO headvo = wragg.getParentVO();
				headvo.setCdeptid(pk_dept);
				headvo.setCdeptvid(pk_vid);
				headvo.setDbilldate(new UFDate(date));
				headvo.setVtrantypecode("55A4-01");
				headvo.setVtrantypeid(PfDataCache.getBillType("55A4-01").getPk_billtypeid());
				headvo.setFbillstatus(1);  //自由态
				WrItemVO[] writems = (WrItemVO[])wragg.getChildren(WrItemVO.class);
				WrItemVO old = writems[0];
				writems = new WrItemVO[arrays.size()];
				for(int i = 0; i < arrays.size(); i++){
					JSONObject jsitem = arrays.getJSONObject(i);
					WrItemVO newItem = CloneUtil.clone(old);
					//根据条码返回的箱数，计算主数量
					int scanQty = jsitem.getInt("ScanQty");
					newItem.setNbwrastnum(new UFDouble(scanQty));
					newItem.setNbwrnum(calcMainNum(scanQty, newItem.getVbchangerate()));
					//判断物料是否启用了批次
					if(WsQueryBS.getWholemanaflag(newItem.getCbmaterialid(), headvo.getPk_org())){
						//入库批次号
						newItem.setVbinbatchcode(jsitem.getString("BatchNo"));
						newItem.setVbinbatchid(WsQueryBS.getPk_BatchCode(newItem.getCbmaterialid(), jsitem.getString("BatchNo")));
					}
					newItem.setCbdeptid(pk_dept);
					newItem.setCbdeptvid(pk_vid);
					newItem.setFbproducttype(1); //产出类型 主产品
					newItem.setTbstarttime(new UFDateTime(date + " 00:00:00"));
					newItem.setTbendtime(new UFDateTime(date + " 23:59:59"));
					
					writems[i] = newItem;
				}
				//按物料+批次号汇总表体行
				HashMap<String, WrItemVO> sumMap = new HashMap<String, WrItemVO>();
				for(WrItemVO writem : writems){
					String key = writem.getCbmaterialid() + writem.getVbinbatchcode();
					WrItemVO olditem = sumMap.get(key);
					if(olditem != null){
						olditem.setNbwrastnum(olditem.getNbwrastnum().add(writem.getNbwrastnum()));
						olditem.setNbwrnum(olditem.getNbwrnum().add(writem.getNbwrnum()));
					} else {
						sumMap.put(key, writem);
					}
				}
				
				wragg.setChildren(WrItemVO.class, sumMap.values().toArray(new WrItemVO[0]));
				//生产报告保存并签字
				IPwrMaintainService service = NCLocator.getInstance().lookup(IPwrMaintainService.class);
				AggWrVO[] rstagg = service.insert(new AggWrVO[] {wragg});
				LoggerUtil.debug("ProductOrderImpl saveProductInbound_requireNew  完工报告保存");
				rstagg = service.audit(rstagg);
				LoggerUtil.debug("ProductOrderImpl saveProductInbound_requireNew  完工报告签字");
				//设置仓库
				for(AggWrVO agg : rstagg){
					WrItemVO[] items = (WrItemVO[])agg.getChildren(WrItemVO.class);
					for(WrItemVO item : items){
						WrQualityVO[] qvos = item.getQualityvos();
						for(WrQualityVO qvo : qvos){
							qvo.setNginastnum(item.getNbwrastnum());
							qvo.setNginnum(item.getNbwrnum());
							qvo.setNgtoinastnum(item.getNbwrastnum());
							qvo.setNgtoinnum(item.getNbwrnum());
							qvo.setCgdepositorgid(stormap.get("pk_org"));
							qvo.setCgwarehouseid(stormap.get("pk_stordoc"));
						}
					}
				}
				
				//生产报告生成产成品入库
				IWrBusinessService businessService = NCLocator.getInstance().lookup(IWrBusinessService.class);
				rstagg = businessService.prodIn(rstagg);
				
				LoggerUtil.debug("ProductOrderImpl saveProductInbound_requireNew  生成产成品入库");
				
				//对应的产成品入库单填写实收数量， 
				//实收数量改为在交换规则配置，只要交换规则配置了，入库就有实收数量
				//cgeneralbid.nnum,cgeneralbid.nassistnum, cgeneralbid.dvalidate
				//需要配置以上3个字段
				IPFBusiAction pf = NCLocator.getInstance().lookup(IPFBusiAction.class);
				for(AggWrVO agg : rstagg){
					WrVO wrheadvo = agg.getParentVO();
					String cgeneralhid = queryFinprodinpkByWrpk(wrheadvo.getPk_wr());
					List<FinProdInVO> list = (List<FinProdInVO>) MDPersistenceService
							.lookupPersistenceQueryService().queryBillOfVOByCond(
									FinProdInVO.class, "cgeneralhid = '"+cgeneralhid+"'", true, false);
					for (FinProdInVO finprodvo : list) {
						FinProdInHeadVO finheadvo = finprodvo.getHead();
						FinProdInBodyVO[] finProdInBodyVOs = (FinProdInBodyVO[]) finprodvo.getChildren(FinProdInBodyVO.class);
						for (FinProdInBodyVO bvo : finProdInBodyVOs) {
							bvo.setNassistnum(bvo.getNshouldassistnum());
							bvo.setNnum(bvo.getNshouldnum());
							bvo.setDvalidate(new UFDate()); // 失效日期
							bvo.setStatus(VOStatus.UPDATED);
						}
						pf.processAction("WRITE", "46", null, finprodvo, null, null);
						LoggerUtil.debug("ProductOrderImpl saveProductInbound_requireNew  产成品入库实发数量更新");
						para.put("OrderNo", finheadvo.getVbillcode());
					}
					//String vbillcode = queryFinprodinpkByWrpk(wrheadvo.getPk_wr());
				}
				
				CommonUtil.putSuccessResult(para);
			}
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "发生异常：" + e.getMessage());
			LoggerUtil.error("ProductOrderImpl saveProductInbound_requireNew error ", e);
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/PostProductionOrderl.fl");
		LoggerUtil.debug("leave ProductOrderImpl saveProductInbound_requireNew " + rst);
		return rst;
	}

	private UFDouble calcMainNum(int scanQty, String vbchangerate){
		String numerator = vbchangerate.split("/")[0]; 
		String denominator = vbchangerate.split("/")[1];
		return new UFDouble(numerator).div(new UFDouble(denominator)).multiply(scanQty);
	}
	
	/**
	 * 根据生产报告表头pk查找对应的产成品入库单表头pk
	 * @param pk_wr
	 * @return
	 */
	public  String queryFinprodinpkByWrpk(String pk_wr){
		BaseDAO dao = new BaseDAO();
		try {
			StringBuffer sql = new StringBuffer();
			sql.append(" select h.cgeneralhid from ic_finprodin_b b, ic_finprodin_h h ")
			.append("  where nvl(h.dr,0) = 0 and nvl(b.dr,0) = 0  ")
			.append("  and b.cgeneralhid = h.cgeneralhid  ")
			.append("  and b.csourcebillhid = '"+pk_wr+"' ");
			Object rst = dao.executeQuery(sql.toString(),  new ColumnProcessor());
			if(rst != null){
				return (String)rst;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
