package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.WsQueryBS;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.IProductOrder;
import nc.itf.mmpac.wr.pwr.IPwrMaintainService;
import nc.itf.uap.pf.IPfExchangeService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.am.common.util.CloneUtil;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOHeadVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;
import nc.vo.mmpac.wr.entity.AggWrVO;
import nc.vo.mmpac.wr.entity.WrItemVO;
import nc.vo.mmpac.wr.entity.WrVO;
import nc.vo.pub.BusinessException;
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
		}
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/productionOrderl.fl");
	}

	@Override
	public String saveProductInbound_requireNew(String xml) {
		
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
				PMOItemVO item = col.iterator().next();
				if(item.getFitemstatus() != 1 && item.getFitemstatus() != 2){
					//throw new BusinessException("生产订单行状态不是投放或者完工,不能入库");
				}
				PMOHeadVO head = (PMOHeadVO)dao.retrieveByPK(PMOHeadVO.class, item.getCpmohid());
				//IPMOQueryService query = NCLocator.getInstance().lookup(IPMOQueryService.class);
				//PMOAggVO agg = query..queryByPk(item.getCpmohid());
				PMOAggVO agg = new PMOAggVO();
				agg.setParentVO(head);
				agg.setChildrenVO(new PMOItemVO[]{item});
				
				InvocationInfoProxy.getInstance().setGroupId(head.getPk_group());
				IPfExchangeService exchangeService = NCLocator.getInstance().lookup(IPfExchangeService.class);
				AggWrVO wragg = (AggWrVO)exchangeService.runChangeData("55A2", "55A4", agg, null);
				
				HashMap<String, Object> deptmap = WsQueryBS.queryDeptidByCode(senderLocationCode, head.getPk_org());
				String pk_dept = (String)deptmap.get("pk_dept");
				String pk_vid = (String)deptmap.get("pk_vid");
				
				WrVO headvo = wragg.getParentVO();
				headvo.setCdeptid(pk_dept);
				headvo.setCdeptvid(pk_vid);
				headvo.setDbilldate(new UFDate(date));
				headvo.setVtrantypecode("55A4-01");
				headvo.setVtrantypeid(PfDataCache.getBillType("55A4-01").getPk_billtypeid());
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
					//入库批次号
					newItem.setVbinbatchcode(jsitem.getString("BatchNo"));
					newItem.setCbdeptid(pk_dept);
					newItem.setCbdeptvid(pk_vid);
					newItem.setFbproducttype(1); //产出类型 主产品
					newItem.setTbstarttime(new UFDateTime(date + " 00:00:00"));
					newItem.setTbendtime(new UFDateTime(date + " 23:59:59"));
					
//					//质量等级孙表
//					WrQualityVO qvo = new WrQualityVO();
//					qvo.setBghaveamend(UFBoolean.FALSE);
//					
//					newItem.setQualityvos(new WrQualityVO[]{qvo});
					writems[i] = newItem;
				}
				wragg.setChildren(WrItemVO.class, writems);
				//生产报告保存并签字
				IPwrMaintainService service = NCLocator.getInstance().lookup(IPwrMaintainService.class);
				AggWrVO[] rstagg = service.insert(new AggWrVO[] {wragg});
				rstagg = service.audit(rstagg);
				//生产报告生成产成品入库
//				IWrBusinessService businessService = NCLocator.getInstance().lookup(IWrBusinessService.class);
//				rstagg = businessService.prodIn(rstagg);
			}
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "发生异常：" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/PostProductionOrderl.fl");
	}

	private UFDouble calcMainNum(int scanQty, String vbchangerate){
		String numerator = vbchangerate.split("/")[0]; 
		String denominator = vbchangerate.split("/")[1];
		return new UFDouble(numerator).div(new UFDouble(denominator)).multiply(scanQty);
	}
}
