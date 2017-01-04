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
					//������Ŀǰֱ��ȡnc�ģ�Ӧ�û���Ҫ������
					detail.putAll(WsQueryBS.queryWorkLine(item.getCwkid()));
					//��ȡ������Ϣ
					detail.putAll(WsQueryBS.queryMaterialInfoByPk(item.getCmaterialid()));
					//��ȡ���ϵ�λ������ȡ��������λ
					detail.put("ProductUM", WsQueryBS.queryUnitName(item.getCunitid()));
					detail.put("IsNeedToCheckProduct", "T"); //�����Ƿ���Ҫ���� 
					
					details.add(detail);
				}
				para.put("Details", details);
			}
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/productionOrderl.fl");
	}

	@Override
	public String saveProductInbound_requireNew(String xml) {
		
		HashMap<String, Object> para = new HashMap<String, Object>();
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		String receiverLocationCode = obj.getString("ReceiverLocationCode"); //���ֿ�
		String senderLocationCode = obj.getString("SenderLocationCode"); //������ ����
		String sender = obj.getString("Sender"); //������
		String receiver = obj.getString("Receiver"); //�ջ���
		String date = obj.getString("Date"); //��������
		String sourceOrderNo = obj.getString("SourceOrderNo"); //Դ���ţ��������������������κţ�
		JSONArray arrays = obj.getJSONArray("items");
		
		InvocationInfoProxy.getInstance().setUserId(WsQueryBS.getUserid(sender));
		BaseDAO dao = new BaseDAO();
		//�����������κŲ�ѯ����������ϸ��
		String where = " nvl(dr,0) = 0 and vbatchcode = '"+sourceOrderNo+"'";
		try {
			Collection<PMOItemVO> col = dao.retrieveByClause(PMOItemVO.class, where);
			if(col == null || col.size() == 0){
				CommonUtil.putFailResult(para, sourceOrderNo + "��ѯ������Ӧ��������������");
			} else {
				PMOItemVO item = col.iterator().next();
				if(item.getFitemstatus() != 1 && item.getFitemstatus() != 2){
					//throw new BusinessException("����������״̬����Ͷ�Ż����깤,�������");
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
					//�������뷵�ص�����������������
					int scanQty = jsitem.getInt("ScanQty");
					newItem.setNbwrastnum(new UFDouble(scanQty));
					newItem.setNbwrnum(calcMainNum(scanQty, newItem.getVbchangerate()));
					//������κ�
					newItem.setVbinbatchcode(jsitem.getString("BatchNo"));
					newItem.setCbdeptid(pk_dept);
					newItem.setCbdeptvid(pk_vid);
					newItem.setFbproducttype(1); //�������� ����Ʒ
					newItem.setTbstarttime(new UFDateTime(date + " 00:00:00"));
					newItem.setTbendtime(new UFDateTime(date + " 23:59:59"));
					
//					//�����ȼ����
//					WrQualityVO qvo = new WrQualityVO();
//					qvo.setBghaveamend(UFBoolean.FALSE);
//					
//					newItem.setQualityvos(new WrQualityVO[]{qvo});
					writems[i] = newItem;
				}
				wragg.setChildren(WrItemVO.class, writems);
				//�������汣�沢ǩ��
				IPwrMaintainService service = NCLocator.getInstance().lookup(IPwrMaintainService.class);
				AggWrVO[] rstagg = service.insert(new AggWrVO[] {wragg});
				rstagg = service.audit(rstagg);
				//�����������ɲ���Ʒ���
//				IWrBusinessService businessService = NCLocator.getInstance().lookup(IWrBusinessService.class);
//				rstagg = businessService.prodIn(rstagg);
			}
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "�����쳣��" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/PostProductionOrderl.fl");
	}

	private UFDouble calcMainNum(int scanQty, String vbchangerate){
		String numerator = vbchangerate.split("/")[0]; 
		String denominator = vbchangerate.split("/")[1];
		return new UFDouble(numerator).div(new UFDouble(denominator)).multiply(scanQty);
	}
}
