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
import nc.itf.uap.pf.busiflow.PfButtonClickContext;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.pub.ic.barcode.LoggerUtil;
import nc.util.mmf.busi.service.PFPubService;
import nc.util.mmpac.wr.WrTransTypeUtil;
import nc.util.mmpac.wr.vochange.WrBusiVOToChangeVO;
import nc.vo.ic.m46.entity.FinProdInBodyVO;
import nc.vo.ic.m46.entity.FinProdInHeadVO;
import nc.vo.ic.m46.entity.FinProdInVO;
import nc.vo.mmpac.pacpub.consts.MMPacBillTypeConstant;
import nc.vo.mmpac.pmo.pac0002.entity.PMOAggVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOHeadVO;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;
import nc.vo.mmpac.wr.entity.AggWrChangeVO;
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
		LoggerUtil.debug("��ȡ��������  getProductOrder - " + batchcode);
		HashMap<String, Object> para = new HashMap<String, Object>();
		BaseDAO dao = new BaseDAO();
		//�����������κŲ�ѯ����������ϸ��
		String where = " nvl(dr,0) = 0 and vbatchcode = '"+batchcode+"'";
		try {
			Collection<PMOItemVO> col = dao.retrieveByClause(PMOItemVO.class, where);
			if(col == null || col.size() == 0){
				CommonUtil.putFailResult(para, batchcode + "��ѯ������Ӧ��������������");
			} else {
				CommonUtil.putSuccessResult(para);
				ArrayList<HashMap<String, Object>> details = new ArrayList<HashMap<String, Object>>();
				for(PMOItemVO item : col){
					if(item.getFitemstatus() != 1 && item.getFitemstatus() != 2){
						throw new BusinessException("����������״̬����Ͷ�Ż����깤");
					}
					HashMap<String, Object> detail = new HashMap<String, Object>();
					detail.put("PlanProductionDate", item.getTplanstarttime().getDate().toString());
					detail.put("OrderStatus", item.getFitemstatus());
					detail.put("PlanPackQty", item.getNmmastnum());
					//���복���Ӧnc�Ĳ��ţ����ݲ��Ŷ��ձ�ת��
					detail.putAll(WsQueryBS.queryWorkShop(item.getCdeptid())); 
					//������Ŀǰֱ��ȡnc�ģ�Ӧ�û���Ҫ�����ա�
					detail.putAll(WsQueryBS.queryWorkLine(item.getCwkid()));
					//��ȡ������Ϣ
					detail.putAll(WsQueryBS.queryMaterialInfoByPk(item.getCmaterialid()));
					//��ȡ���ϵ�λ������ȡ��������λ
					detail.put("ProductUM", WsQueryBS.queryUnitName(item.getCastunitid()));
					//detail.put("IsNeedToCheckProduct", "T"); //�����Ƿ���Ҫ���� 
					
					details.add(detail);
				}
				para.put("Details", details);
			}
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
			LoggerUtil.error("��ȡ���������쳣 ", e);
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/productionOrderl.fl");
		LoggerUtil.debug("��ȡ�����������  getProductOrder " + rst);
		return rst;
	}

	@Override
	public String saveProductInbound_requireNew(String xml) {
		
		LoggerUtil.debug("д���깤��� saveProductInbound_requireNew " + xml);
		
		HashMap<String, Object> para = new HashMap<String, Object>();
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		String receiverLocationCode = obj.getString("ReceiverLocationCode"); //���ֿ�
		String senderLocationCode = obj.getString("SenderLocationCode"); //������ ����
		String sender = obj.getString("Sender"); //������
		String receiver = obj.getString("Receiver"); //�ջ���
		String date = obj.getString("Date"); //��������
		JSONArray arrays = obj.getJSONArray("items");
		
		IPFBusiAction pf = NCLocator.getInstance().lookup(IPFBusiAction.class);
		
		try{
			HashMap<String, String> stormap = WsQueryBS.queryStordocByCode(receiverLocationCode);
			if(stormap == null || stormap.size() == 0){
				throw new BusinessException("�ջ���" + receiverLocationCode + "�ڲֿ�����Ҳ�����Ӧ�Ĳֿ⵵��");
			}
			
			BaseDAO dao = new BaseDAO();
			//�Ƚ��������ݰ��������κţ����飬���ϣ����κţ�������ʽ�ϲ�
			HashMap<String, JSONObject> calMap = new HashMap<String, JSONObject>();
			for(int i = 0; i < arrays.size(); i++){
				JSONObject jsitem = arrays.getJSONObject(i);
				String sourceOrderNo = jsitem.getString("SourceOrderNo"); //Դ���ţ��������������������κţ�
				String productCode = jsitem.getString("ProductCode");
				String teamCode = jsitem.getString("TeamCode");
				String batchno = jsitem.getString("BatchNo");
				int scanQty = jsitem.getInt("ScanQty");
				String productionMode = jsitem.getString("ProductionMode"); //������ʽ
				String key = sourceOrderNo + productCode + teamCode + batchno + productionMode;
				JSONObject jsonv = calMap.get(key);
				if(jsonv != null){
					jsonv.put("ScanQty", jsonv.getInt("ScanQty") + scanQty);
				} else {
					calMap.put(key, jsitem);
				}
			}
			//������������ţ��������ţ����齫�������ݷ���
			Collection<JSONObject> colMap = calMap.values();
			//HashMap<String, Object> deptmap = null;
			//�ֱ��¼����������ͷ�ͱ��壬���ڸ�����������ƥ���Ӧ����������
			HashMap<String, PMOHeadVO> pmoHeadMap = new HashMap<String, PMOHeadVO>();
			HashMap<String, ArrayList<PMOItemVO>> pmoBodyListMap = new HashMap<String, ArrayList<PMOItemVO>>();
			for(JSONObject jsonobj : colMap){
				
				String sourceOrderNo = jsonobj.getString("SourceOrderNo"); //Դ���ţ��������������������κţ�
				String productCode = jsonobj.getString("ProductCode");
				String teamCode = jsonobj.getString("TeamCode");
				String batchno = jsonobj.getString("BatchNo");
				int scanQty = jsonobj.getInt("ScanQty");
				String productionMode = jsonobj.getString("ProductionMode");
				String bcvalue = String.format("%s,%s,%s,%s", teamCode, batchno, scanQty, productionMode);
				
				String where = " nvl(dr,0) = 0 and vbatchcode = '"+sourceOrderNo+"'";
				Collection<PMOItemVO> col = dao.retrieveByClause(PMOItemVO.class, where); 
				if(col == null || col.size() == 0){ 
					throw new BusinessException(sourceOrderNo + "��ѯ������Ӧ��������������");
				}
				PMOItemVO pmoitem = col.iterator().next();
				if(pmoitem.getFitemstatus() != 1 && pmoitem.getFitemstatus() != 2){
					throw new BusinessException("����������"+sourceOrderNo+"״̬����Ͷ�Ż����깤,�������");
				}
				//��ʱ��¼������顢���κź�����
				pmoitem.setVdef20(bcvalue);
				
				PMOHeadVO pmohead = pmoHeadMap.get(pmoitem.getCpmohid());
				if(pmohead == null){
					pmohead = (PMOHeadVO)dao.retrieveByPK(PMOHeadVO.class, pmoitem.getCpmohid());
					pmoHeadMap.put(pmoitem.getCpmohid(), pmohead);
				}
//				if(deptmap == null){
//					deptmap = WsQueryBS.queryDeptidByCode(senderLocationCode, pmohead.getPk_org());
//				}
				//��֯ + ��ͷpk + �������� + ������(��������) + ���� 
				String key = pmoitem.getPk_org() + pmoitem.getCpmohid() + pmoitem.getCdeptid() + pmoitem.getCwkid() + teamCode;
				ArrayList<PMOItemVO> itemlist = pmoBodyListMap.get(key);
				if(itemlist == null){
					itemlist = new ArrayList<PMOItemVO>();
					pmoBodyListMap.put(key, itemlist);
				}
				itemlist.add(pmoitem);
			}
			//����ֺ������������ϸ�����깤����
			IPfExchangeService exchangeService = NCLocator.getInstance().lookup(IPfExchangeService.class);
			ArrayList<String> billno = new ArrayList<String>();
			Collection<ArrayList<PMOItemVO>> colbodyListMap = pmoBodyListMap.values();
			for(ArrayList<PMOItemVO> pmoItems : colbodyListMap){
				PMOHeadVO pmohead = pmoHeadMap.get(pmoItems.get(0).getCpmohid());
				
				PMOAggVO pmoagg = new PMOAggVO();
				pmoagg.setParentVO(pmohead);
				pmoagg.setChildrenVO(pmoItems.toArray(new PMOItemVO[0]));
				
				InvocationInfoProxy.getInstance().setGroupId(pmohead.getPk_group());
				AggWrVO wragg = (AggWrVO)exchangeService.runChangeData("55A2", "55A4", pmoagg, null);
				
				//���봫�����Ĳ���Ŀǰû��ʹ�ã�ȡ�Ķ������������Ĳ���
//				String pk_dept = (String)deptmap.get("pk_dept");
//				String pk_vid = (String)deptmap.get("pk_vid");
				
				WrVO headvo = wragg.getParentVO();
//				headvo.setCdeptid(pk_dept);
//				headvo.setCdeptvid(pk_vid);
				headvo.setDbilldate(new UFDate(date));
				headvo.setVtrantypecode("55A4-01");
				headvo.setVtrantypeid(PfDataCache.getBillType("55A4-01").getPk_billtypeid());
				headvo.setFbillstatus(1);  //����̬
				WrItemVO[] writems = (WrItemVO[])wragg.getChildren(WrItemVO.class);
				for(int i = 0; i < writems.length; i++){
					WrItemVO writem = writems[i];
					String[] bcvalue = writem.getVbdef20().split(",");
					String teamCode = bcvalue[0];
					String batchno = bcvalue[1];
					String scanQty = bcvalue[2];
					String productionMode = bcvalue[3];
					
					//�������뷵�ص�����������������
					writem.setNbwrastnum(new UFDouble(scanQty));
					writem.setNbwrnum(calcMainNum(scanQty, writem.getVbchangerate()));
					//�ж������Ƿ�����������
					if(WsQueryBS.getWholemanaflag(writem.getCbmaterialid(), headvo.getPk_org())){
						//������κ�
						writem.setVbinbatchcode(batchno);
						writem.setVbinbatchid(WsQueryBS.getPk_BatchCode(writem.getCbmaterialid(), batchno));
					}
					writem.setVbrowno(String.valueOf((i + 1) * 10));
//					writem.setCbdeptid(pk_dept);
//					writem.setCbdeptvid(pk_vid);
					writem.setFbproducttype(1); //�������� ����Ʒ
					writem.setTbstarttime(new UFDateTime(date + " 00:00:00"));
					writem.setTbendtime(new UFDateTime(date + " 23:59:59"));
					writem.setVbdef20(teamCode);
					headvo.setVdef20(teamCode);
					writem.setVbdef19(WsQueryBS.getProductModelByCode(productionMode));
					//��������
					//writem.setVbdef1(date);  
				}
				//�������汣�沢ǩ��
				IPwrMaintainService service = NCLocator.getInstance().lookup(IPwrMaintainService.class);
				AggWrVO[] rstagg = service.insert(new AggWrVO[] {wragg});
				LoggerUtil.debug("�깤���汣��");
				rstagg = service.audit(rstagg);
				LoggerUtil.debug("�깤����ǩ��");
				//���òֿ�
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
				
				//���ж��Ƿ��п��Ժϲ��Ĳ���Ʒ��⣬����ϲ���û��������
				String cgeneralhidMer =  queryFinprodinpkByMerge(pmohead.getCpmohid(), date, stormap.get("pk_stordoc"),
						pmoItems.get(0).getCdeptid(), pmoItems.get(0).getCclassid(), wragg.getParentVO().getVdef20());
				if(cgeneralhidMer != null){
					List<FinProdInVO> list = (List<FinProdInVO>) MDPersistenceService
							.lookupPersistenceQueryService().queryBillOfVOByCond(
									FinProdInVO.class, "cgeneralhid = '"+cgeneralhidMer+"'", true, false);
					FinProdInVO finprodvo = list.get(0);
					FinProdInHeadVO finheadvo = finprodvo.getHead();
					LoggerUtil.debug("���ڿ��Ժϲ�����Ʒ��⣺" + finheadvo.getVbillcode());
					
					FinProdInBodyVO[] finProdInBodyVOs = (FinProdInBodyVO[]) finprodvo.getChildren(FinProdInBodyVO.class);
					ArrayList<FinProdInBodyVO> newBodyList = new ArrayList<FinProdInBodyVO>();
					for(FinProdInBodyVO fb : finProdInBodyVOs){
						newBodyList.add(fb);
					}
					//�깤����ת��Ϊ����Ʒ���
					FinProdInVO[] megerInVO = change(rstagg);
					int rowno = newBodyList.size();
					for(FinProdInVO finvo : megerInVO){
						FinProdInBodyVO[] fbs = (FinProdInBodyVO[]) finvo.getChildren(FinProdInBodyVO.class);
						for(FinProdInBodyVO fbb : fbs){
							fbb.setNassistnum(fbb.getNshouldassistnum());
							fbb.setNnum(fbb.getNshouldnum());
							String pk_material = fbb.getCmaterialoid();
							Integer qualitynum = WsQueryBS.queryQualitynum(pmohead.getPk_org(), pk_material);
							fbb.setDvalidate(new UFDate(date).getDateAfter(qualitynum)); // ʧЧ����
							fbb.setStatus(VOStatus.NEW);
							fbb.setCrowno(String.valueOf((++rowno) * 10));
							fbb.setCgeneralhid(finprodvo.getPrimaryKey()); 
							newBodyList.add(fbb);
						}
					}
					finprodvo.setChildren(FinProdInBodyVO.class, newBodyList.toArray(new FinProdInBodyVO[0]));
					pf.processAction("WRITE", "46", null, finprodvo, null, null);
					LoggerUtil.debug("�ϲ�����Ʒ��⣬�޸ĳɹ�");
					billno.add(finheadvo.getVbillcode());
				} else {
				
					//�����������ɲ���Ʒ���
					IWrBusinessService businessService = NCLocator.getInstance().lookup(IWrBusinessService.class);
					rstagg = businessService.prodIn(rstagg);
					
					LoggerUtil.debug("���ɲ���Ʒ���");
					
					//��Ӧ�Ĳ���Ʒ��ⵥ��дʵ�������� 
					//ʵ��������Ϊ�ڽ����������ã�ֻҪ�������������ˣ�������ʵ������
					//cgeneralbid.nnum,cgeneralbid.nassistnum, cgeneralbid.dvalidate
					//��Ҫ��������3���ֶ�
					for(AggWrVO agg : rstagg){
						WrVO wrheadvo = agg.getParentVO();
						String cgeneralhid = queryFinprodinpkByWrpk(wrheadvo.getPk_wr());
						List<FinProdInVO> list = (List<FinProdInVO>) MDPersistenceService
								.lookupPersistenceQueryService().queryBillOfVOByCond(
										FinProdInVO.class, "cgeneralhid = '"+cgeneralhid+"'", true, false);
						for (FinProdInVO finprodvo : list) {
							FinProdInHeadVO finheadvo = finprodvo.getHead();
	//						finheadvo.setCdptid(pk_dept);
	//						finheadvo.setCdptvid(pk_vid);
	//						finheadvo.setStatus(VOStatus.UPDATED);
							FinProdInBodyVO[] finProdInBodyVOs = (FinProdInBodyVO[]) finprodvo.getChildren(FinProdInBodyVO.class);
							String pk_org = finheadvo.getPk_org();
							for (FinProdInBodyVO bvo : finProdInBodyVOs) {
								bvo.setNassistnum(bvo.getNshouldassistnum());
								bvo.setNnum(bvo.getNshouldnum());
								String pk_material = bvo.getCmaterialoid();
								Integer qualitynum = WsQueryBS.queryQualitynum(pk_org, pk_material);
								bvo.setDvalidate(new UFDate(date).getDateAfter(qualitynum)); // ʧЧ����
								bvo.setStatus(VOStatus.UPDATED);
							}
							pf.processAction("WRITE", "46", null, finprodvo, null, null);
							LoggerUtil.debug("����Ʒ���ʵ����������");
							billno.add(finheadvo.getVbillcode());
						}
					}
				}
			}
			para.put("OrderNo", join(billno));
			CommonUtil.putSuccessResult(para);
			
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "�����쳣��" + e.getMessage());
			LoggerUtil.error("д���깤����쳣", e);
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/PostProductionOrderl.fl");
		LoggerUtil.debug("д���깤����� saveProductInbound_requireNew " + rst);
		return rst;
	}

	private UFDouble calcMainNum(String scanQty, String vbchangerate){
		String numerator = vbchangerate.split("/")[0]; 
		String denominator = vbchangerate.split("/")[1];
		return new UFDouble(numerator).div(new UFDouble(denominator)).multiply(new UFDouble(scanQty));
	}
	
	/**
	 * �������������ͷpk���Ҷ�Ӧ�Ĳ���Ʒ��ⵥ��ͷpk
	 * @param pk_wr
	 * @return
	 */
	public String queryFinprodinpkByWrpk(String pk_wr){
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
	
	/**
	 * ��ѯ��ͬγ�ȵĲ���Ʒ��ⵥ
	 * @param pk_wr
	 * @return
	 */
	public String queryFinprodinpkByMerge(String cpmohid, String dbilldate, String cwarehouseid, String cdptid, String cshiftid,
			String teamCode){
		BaseDAO dao = new BaseDAO();
		try {
			StringBuffer sql = new StringBuffer();
			sql.append("  select distinct h.cgeneralhid   ")
			.append("     from ic_finprodin_b b, ic_finprodin_h h  ")
			.append("    where  ")
			.append("      b.cgeneralhid = h.cgeneralhid  ")
			.append("      and h.fbillflag = 2  ")
			.append("      and nvl(h.dr, 0) = 0  ")
			.append("      and nvl(b.dr, 0) = 0  ")
			.append("      and b.cfirstbillhid = '"+cpmohid+"'  ")
			.append("      and substr(h.dbilldate, 1, 10) = '"+dbilldate+"'  ")
			.append("      and h.cwarehouseid = '"+cwarehouseid+"'  ")
			.append("      and h.cdptid = '"+cdptid+"' ")
			.append("      and h.vdef4 = '"+cshiftid+"' ")
			.append("      and b.vbdef20 = '"+teamCode+"' ")
			.append("      and nvl(h.vdef2,'~') <> 'Y' "); 
			//.append("      and b.vbdef19 = '"+productionMode+"' ");
			Object rst = dao.executeQuery(sql.toString(),  new ColumnProcessor());
			if(rst != null){
				return (String)rst;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public String join(ArrayList<String> list){
		StringBuffer str = new StringBuffer();
		for(int i = 0; i < list.size(); i++){
			str.append(list.get(i));
			if(i != list.size() - 1){
				str.append(",");
			}
		}
		return str.toString();
	}
	
	
	public static FinProdInVO[] change(AggWrVO[] aggVOs) throws BusinessException {

		// �Ƿ��������=false ʱ�����ܰѽ���������Ϣ������棬 ���˵Ļ��ᰴ����������⴦��
		WrItemVO[] items = null;

		for (AggWrVO wr : aggVOs) {
			WrTransTypeUtil.changeTransTypeCodeDefault(wr.getParentVO());
			items = (WrItemVO[]) wr.getChildren(WrItemVO.class);
			for (WrItemVO item : items) {
				if (null == item.getBbisempass() || item.getBbisempass().booleanValue() == false) {
					item.setNbempassastnum(null);
					item.setNbempassnum(null);
					item.setCbempass_bid(null);
					item.setCbempass_brow(null);
					item.setCbempasscode(null);
					item.setCbempassid(null);
				}
			}
		}
		// ����������ҵ��VOת��Ϊ���ݽ���VO(��ΪUAP����֧�������ӣ�����VO����)
		AggWrChangeVO[] aggChangeVOs = WrBusiVOToChangeVO.changeOnlyQualityVO(aggVOs);

		FinProdInVO[] depositAggVOs = (FinProdInVO[]) PFPubService.runChangeData(MMPacBillTypeConstant.WRCHANGE_BILLTYPE,
				MMPacBillTypeConstant.DEPOSITE_BILLTYPE, aggChangeVOs, null, PfButtonClickContext.ClassifyByItfdef);
		return depositAggVOs;
	}
	
}
