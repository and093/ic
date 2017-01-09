package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.WsQueryBS;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.ITransferOrder;
import nc.itf.uap.pf.IPFBusiAction;
import nc.itf.uap.pf.IPfExchangeService;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.pub.ic.barcode.LoggerUtil;
import nc.vo.ic.m4a.entity.GeneralInBodyVO;
import nc.vo.ic.m4a.entity.GeneralInHeadVO;
import nc.vo.ic.m4a.entity.GeneralInVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillHeaderVO;
import nc.vo.ic.m4k.entity.WhsTransBillVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class TransferOrderImpl implements ITransferOrder {

	@Override
	public String saveTransferOut_requireNew(String xml) {
		LoggerUtil.debug("entry TransferOrderImpl saveTransferOut_requireNew " + xml);
		HashMap<String, Object> para = new HashMap<String, Object>();
		
		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		
		String senderLocationCode = obj.getString("SenderLocationCode");
		String receiverLocationCode = obj.getString("ReceiverLocationCode");
		JSONArray items = obj.getJSONArray("items");
		
		try{
			if(senderLocationCode == null || receiverLocationCode == null){
				throw new BusinessException("SenderLocationCode��ReceiverLocationCode����Ϊ��");
			}
			HashMap<String, String> outStoreMap = WsQueryBS.queryStordocByCode(senderLocationCode);
			HashMap<String, String> inStoreMap = WsQueryBS.queryStordocByCode(receiverLocationCode);
			if(outStoreMap.size() == 0 ){
				throw new BusinessException("�ֿ�����Ҳ����ֿ����" + senderLocationCode);
			}
			if(inStoreMap.size() == 0 ){ 
				throw new BusinessException("�ֿ�����Ҳ����ֿ����" + receiverLocationCode);
			}
			if(!outStoreMap.get("pk_org").equals(inStoreMap.get("pk_org"))){
				throw new BusinessException("ת����ת��ֿ�������֯��һ��");
			}
			
			WhsTransBillVO wtbillvo = new WhsTransBillVO();
			WhsTransBillHeaderVO wthvo = getWhsTransBillHeaderVO(obj, outStoreMap, inStoreMap);
			wtbillvo.setParent(wthvo);
			wtbillvo.setChildrenVO(getWhsTransBillBodysVO(wthvo, items));
			//����ת�ⵥ
			IPFBusiAction pf = NCLocator.getInstance().lookup(IPFBusiAction.class);
			pf.processAction("WRITE", "4K", null, wtbillvo, null, null);
			LoggerUtil.error("TransferOrderImpl saveTransferOut_requireNew ת�Ᵽ�� ");
			pf.processAction("APPROVE", "4K", null, wtbillvo, null, null);
			LoggerUtil.error("TransferOrderImpl saveTransferOut_requireNew ת����� ");
			//ת�ⵥ����������
			IPfExchangeService exchangeService = NCLocator.getInstance().lookup(IPfExchangeService.class);
			GeneralOutVO outvo = (GeneralOutVO )exchangeService.runChangeData("4K", "4I", wtbillvo, null);
			GeneralOutHeadVO outheadvo = outvo.getHead();
			outheadvo.setVtrantypecode("4I-02");
			outheadvo.setCtrantypeid(PfDataCache.getBillType("4I-02").getPk_billtypeid());
			GeneralOutBodyVO[] outbodys = outvo.getBodys();
			for(int i = 0; i < outbodys.length; i++){
				GeneralOutBodyVO outbody = outbodys[i];
				outbody.setCrowno(String.valueOf((i + 1) * 10));
				outbody.setNassistnum(outbody.getNshouldassistnum());
				outbody.setNnum(outbody.getNshouldnum());
				//outbody.setDproducedate(new UFDate());
				//outbody.setDvalidate(new UFDate()); // ʧЧ����
				outbody.setVtransfercode("4I-02");
				//outbody.setDinbounddate(new UFDate());
//				Logger.error("�������κ�   " + outbody.getVbatchcode());
//				Logger.error("��������pk   " + outbody.getPk_batchcode());
//				Logger.error("����ʧЧ����   " + outbody.getDvalidate());
//				Logger.error("������������   " + outbody.getDproducedate());
			}
			GeneralOutVO[] outrst = (GeneralOutVO[])pf.processAction("WRITE", "4I", null, outvo, null, null);
			LoggerUtil.error("TransferOrderImpl saveTransferOut_requireNew �������Ᵽ�� ");
			para.put("OrderNo", outrst[0].getHead().getVbillcode());
			CommonUtil.putSuccessResult(para);
		} catch(BusinessException e){
			e.printStackTrace();
			CommonUtil.putFailResult(para, "�����쳣��" + e.getMessage());
			LoggerUtil.error("TransferOrderImpl saveTransferOut_requireNew error ", e);
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/PostProductionOrderl.fl");
		LoggerUtil.debug("leave TransferOrderImpl saveTransferOut_requireNew " + rst);
		return rst;
	}

	private WhsTransBillHeaderVO getWhsTransBillHeaderVO(JSONObject obj, HashMap<String, String> outStoreMap, HashMap<String, String> inStoreMap){
		WhsTransBillHeaderVO wthvo = new WhsTransBillHeaderVO();
		UFDate Date = new UFDate(obj.getString("Date"));
		String sender = obj.getString("Sender");
		String receiver = obj.getString("Receiver");
		InvocationInfoProxy.getInstance().setGroupId(outStoreMap.get("pk_group"));
		
		String pk_org = outStoreMap.get("pk_org");
		String pk_org_v = outStoreMap.get("pk_vid");
		wthvo.setCorpoid(pk_org);
		wthvo.setCorpvid(pk_org_v);
		wthvo.setCotherwhid(inStoreMap.get("pk_stordoc")); //���ֿ�
		wthvo.setCtrantypeid(PfDataCache.getBillType("4K-01").getPk_billtypeid());
		wthvo.setCwarehouseid(outStoreMap.get("pk_stordoc")); //����ֿ�
		wthvo.setDbilldate(Date);
		wthvo.setFbillflag(1); //����״̬
		wthvo.setPk_group(outStoreMap.get("pk_group"));
		wthvo.setPk_org(pk_org);
		wthvo.setPk_org_v(pk_org_v);
		wthvo.setVtrantypecode("4K-01");
		
		return wthvo;
	}
	
	private WhsTransBillBodyVO[] getWhsTransBillBodysVO(WhsTransBillHeaderVO wthvo, JSONArray items) throws BusinessException{
		WhsTransBillBodyVO[] wtbodys = new WhsTransBillBodyVO[items.size()];
		for(int i = 0; i < items.size(); i++){
			WhsTransBillBodyVO bvo = new WhsTransBillBodyVO();
			JSONObject jsitem = items.getJSONObject(i);
			String ProductCode = jsitem.getString("ProductCode");
			String BatchNo = jsitem.getString("BatchNo");
			UFDouble ScanQty = new UFDouble(jsitem.getInt("ScanQty"));
			HashMap<String, String> materialMap = WsQueryBS.queryMaterialInfoByCode(ProductCode);
			if(materialMap.size() == 0){
				throw new BusinessException("��������̺�" + ProductCode + "�Ҳ�����Ӧ�����ϻ������ϵ�λ������");	
			}
			bvo.setPk_group(wthvo.getPk_group());
			bvo.setPk_org(wthvo.getPk_org());
			bvo.setPk_org_v(wthvo.getPk_org_v());
			bvo.setCastunitid(materialMap.get("castunitid"));
			bvo.setCunitid(materialMap.get("cunitid"));
			bvo.setCmaterialoid(materialMap.get("pk_material"));
			bvo.setCmaterialvid(materialMap.get("pk_material"));
			bvo.setCorpoid(wthvo.getCorpoid());
			bvo.setCorpvid(wthvo.getCorpvid());
			bvo.setCrowno(String.valueOf((i + 1) * 10));
			bvo.setVchangerate(materialMap.get("measrate"));
			bvo.setNassistnum(ScanQty);
			bvo.setNnum(ScanQty.multiply(getVchangerate(bvo.getVchangerate())));
			//�ж������Ƿ�����������
			if(WsQueryBS.getWholemanaflag(bvo.getCmaterialoid(), bvo.getPk_org())){
				//���κ�
				bvo.setVbatchcode(BatchNo);
				bvo.setPk_batchcode(WsQueryBS.getPk_BatchCode(bvo.getCmaterialoid(), BatchNo));
				bvo.setDproducedate(new UFDate());
				bvo.setDvalidate(new UFDate());
			}
			wtbodys[i] = bvo;
		}
		return wtbodys;
	}
	
	
	/**
	 * 2.9 д��ת����ⵥ
	 */
	@Override
	public String saveTransferIn_requireNew(String xml) {

		HashMap<String, Object> para = new HashMap<String, Object>();

		List<GeneralInVO> list_gi = new ArrayList<GeneralInVO>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		UFDate Date = new UFDate(obj.getString("Date"));
		String OrderNo = obj.getString("SourceOrderNo");
		JSONArray item = obj.getJSONArray("items");

		GeneralInVO gvi = new GeneralInVO();

		// ����OrderNo ��ѯNC ת����ⵥ
		GeneralOutVO gvo = getGeneralOutVO(OrderNo);

		if (gvo == null) {
			CommonUtil.putFailResult(para, "ת����ⵥ��" + OrderNo + "��ѯʧ��");
		} else {

			// ���ת����ⵥ��Ӧ�� ת�ⵥ ���ۼƳ��������� �Ƿ�Ϊ�գ���Ϊ���� ��ʾ�ó��ⵥ�����ɹ�ת����ⵥ �������ٴ�����

			if (IsTurntoGenerIn(gvo)) {
				CommonUtil.putFailResult(para, "��ת����ⵥ�Ѿ����ɹ�ת����ⵥ�������ٴ����ɣ�");
				return FreeMarkerUtil.process(para,
						"nc/config/ic/barcode/TransferInOrder.fl");
			}

			// ��ȡת����ⵥ��ͷ
			GeneralOutHeadVO goHeadVO = gvo.getHead();
			if (goHeadVO != null) {
				InvocationInfoProxy.getInstance().setGroupId(goHeadVO.getPk_group());
				// ͨ��ת����ⵥ��ͷ����ת����ⵥ��ͷ
				gvi.setParent(this.setGeneralInHeadVO(goHeadVO, Date));
			} else {
				CommonUtil.putFailResult(para, "ת����ⵥ��" + OrderNo
						+ "��Ӧ�ı�ͷ����Ϊ�գ�");
			}
			List<GeneralInBodyVO> list = getGeneralInBodyVO(gvo, item, para);
			// ͨ��ת����ⵥ��ȡ����
			if (list != null && list.size() != 0) {
				gvi.setChildrenVO(list.toArray(new GeneralInBodyVO[0]));
				IPFBusiAction pf = NCLocator.getInstance().lookup(
						IPFBusiAction.class);
				InvocationInfoProxy.getInstance().setUserId(
						gvi.getHead().getBillmaker());
				InvocationInfoProxy.getInstance().setGroupId(
						gvi.getHead().getPk_group());
				InvocationInfoProxy.getInstance().setBizDateTime(
						System.currentTimeMillis());
				try {
					GeneralInVO[] gvis = (GeneralInVO[]) pf.processAction(
							"WRITE", "4A", null, gvi, null, null);
					if (gvis.length != 0) {
						para.put("OrderNo", gvis[0].getHead().getVbillcode());
						CommonUtil.putSuccessResult(para);
					}
				} catch (BusinessException e) {
					CommonUtil.putFailResult(para, e.getMessage());
					e.printStackTrace();
				}
			} else {
				CommonUtil.putFailResult(para, "���϶̺Ų���ȫ����ת����ⵥ����ƥ��  ����  "
						+ "ת����ⵥ��" + OrderNo + "��Ӧ�ı��嵥��Ϊ��");
			}
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
	}

	/**
	 * �ж�ת����ⵥ�Ƿ��Ѿ����ɹ�ת����ⵥ
	 * 
	 * @param gvo
	 *            ת����ⵥaggvo
	 * @return true ������ false δ����
	 */
	private boolean IsTurntoGenerIn(GeneralOutVO gvo) {

		boolean flag = false;

		for (GeneralOutBodyVO body : gvo.getBodys()) {
			String id = body.getCsourcebillbid(); // ת����ⵥ��Դ����id ��
			String sqlWhere = "nvl(dr,0) = 0 and csourcebillbid='" + id + "'";
			try {
				Object ntransinnum = new BaseDAO().executeQuery(
						"select ntransinnum from ic_whstrans_b where cspecialbid='"
								+ id + "' and dr = 0", new ColumnProcessor());
				Object nnum = new BaseDAO().executeQuery(
						"select nnum from ic_whstrans_b where cspecialbid='"
								+ id + "' and dr = 0", new ColumnProcessor());
				if (ntransinnum != null) {
					double ntnum = Double.parseDouble(ntransinnum.toString()); // �ۼ����������
					if (ntnum == Double.parseDouble(nnum.toString())) {
						flag = true;
						break;
					}
				}
			} catch (DAOException e) {
				e.printStackTrace();
			}
		}
		return flag;
	}

	/**
	 * ͨ��ת����ⵥ��ȡת����ⵥ����
	 * 
	 * @param gvo
	 *            ת����ⵥ aggVO
	 * @return
	 */
	private List<GeneralInBodyVO> getGeneralInBodyVO(GeneralOutVO gvo,
			JSONArray item, HashMap<String, Object> para) {

		int count = 0;
		String errorCode = new String();

		GeneralOutBodyVO[] goBodys = gvo.getBodys();
		GeneralOutHeadVO gohead = gvo.getHead();
		List<GeneralInBodyVO> list = new ArrayList<GeneralInBodyVO>();
		int index = 0;
		for (; index < goBodys.length; index++) {

			String pk_material = null;
			try {
				pk_material = WsQueryBS.queryPK_materialByProductCode(item
						.getJSONObject(index).getString("ProductCode"));
			} catch (DAOException e) {
				CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ��" + e.getMessage());
				e.printStackTrace();
			} // �������϶̺Ż�ȡ����pk

			if (pk_material == null) {
				CommonUtil.putFailResult(para, "�Ϻ�"
						+ item.getJSONObject(index).getString("ProductCode")
						+ "�Ҳ�����Ӧ������");
				return null; // ��ȡ����pkʧ��
			}

			boolean flag = false;
			for (GeneralOutBodyVO go : goBodys) {
				if (pk_material.equals(go.getCmaterialoid())) {
					flag = true;
					GeneralInBodyVO gi = new GeneralInBodyVO();
					gi.setPk_group(gohead.getPk_group()); // ����

					gi.setCrowno(go.getCrowno() == null ? (index + 1) * 10 + ""
							: go.getCrowno()); // �к�
					gi.setCmaterialoid(go.getCmaterialoid()); // ����
					gi.setCmaterialvid(go.getCmaterialvid()); // ���ϱ���
					// gi.setVbdef8(item.getJSONObject(index).getString(
					// "ProductCode")); // ���϶̺�
					gi.setCunitid(go.getCunitid()); // ����λ
					gi.setCastunitid(go.getCastunitid()); // ��λ
					gi.setVchangerate(go.getVchangerate()); // ������
					gi.setNshouldassistnum(go.getNshouldassistnum()); // Ӧ������
					gi.setNshouldnum(new UFDouble(go.getNshouldassistnum()
							.doubleValue()
							* getVchangerate(go.getVchangerate()))); // Ӧ��������
																		// =
																		// Ӧ������*������
					gi.setNassistnum(new UFDouble(item.getJSONObject(index)
							.getInt("ScanQty"))); // ʵ������
					gi.setNnum(gi.getNshouldnum()); // ʵ�������� �� Ӧ��������һ��
					gi.setCbodywarehouseid(go.getCbodywarehouseid()); // ���ֿ�
					gi.setNcostprice(go.getNcostprice()); // ����
					gi.setNcostmny(go.getNcostmny()); // ���
					gi.setDbizdate(new UFDate()); // �������

					gi.setCbodytranstypecode("4A-02"); // ���������pk
					if (WsQueryBS.getWholemanaflag(pk_material, go.getPk_org())) {
						gi.setVbatchcode(item.getJSONObject(index).getString(
								"BatchNo")); // ���κ�
					}
					if (WsQueryBS.getPk_BatchCode(pk_material, item.getJSONObject(index)
							.getString("BatchNo")) != null) {
						gi.setPk_batchcode(WsQueryBS.getPk_BatchCode(pk_material, item
								.getJSONObject(index).getString("BatchNo")));
						gi.setDvalidate(new UFDate()); //ʧЧ����
					}
					gi.setDproducedate(go.getDproducedate()); // ��������
					gi.setVvendbatchcode(go.getVvendbatchcode()); // ��Ӧ�����κ�

					gi.setCprojectid(go.getCprojectid()); // ��Ŀ
					gi.setCasscustid(go.getCasscustid()); // �ͻ�

					// ��Դ��Ϣ
					gi.setVsourcerowno(go.getVsourcerowno());
					gi.setVsourcebillcode(go.getVsourcebillcode()); // ��Դ���ݺ�
					gi.setCsourcebillhid(go.getCsourcebillhid()); // ��Դ��������
					gi.setCsourcebillbid(go.getCsourcebillbid()); // ��Դ���ݱ�������
					gi.setCsourcetranstype(go.getCsourcetranstype()); // ��Դ���ݳ��������
					gi.setCsourcetype(go.getCsourcetype());

					gi.setNweight(go.getNweight());
					gi.setNvolume(go.getNvolume());
					gi.setStatus(VOStatus.NEW);

					list.add(gi);
					count++;
				} // end if pk_material.equals(go.getCmaterialoid())
			} // end for go
			if (!flag) {
				errorCode += item.getJSONObject(index).getString("ProductCode")
						+ " ";
			}
		}
		// ��Щ���϶̺�ƥ�䵽������pk ���������ⵥ���ӱ��в����ڣ���������ϵͳ�����������
		if (count != item.size()) {
			CommonUtil.putFailResult(para, "�������϶̺�" + errorCode
					+ "��Ӧ������pkƥ�䲻�����ⵥ�ӱ��Ӧ������Ϣ��");
			return null;
		}
		return list;
	}

	/**
	 * ���㻻����
	 * 
	 * @param vchangerate
	 *            ������
	 * @return
	 */
	private double getVchangerate(String vchangerate) {

		String[] vcs = vchangerate.split("/");
		double vc = Double.parseDouble(vcs[0]) / Double.parseDouble(vcs[1]);
		return vc;
	}

	/**
	 * ͨ��ת����ⵥ��ͷ����ת����ⵥ��ͷ
	 * 
	 * @param goHeadVO
	 *            ת����ⵥ��ͷ
	 * @return
	 */
	private GeneralInHeadVO setGeneralInHeadVO(GeneralOutHeadVO goHeadVO,
			UFDate date) {

		GeneralInHeadVO giHeadVO = new GeneralInHeadVO();

		giHeadVO.setPk_group(goHeadVO.getPk_group());
		giHeadVO.setVtrantypecode("4A-02");
		giHeadVO.setCtrantypeid(PfDataCache.getBillType("4A-02").getPk_billtypeid()); // ��������pk (���������)
		giHeadVO.setCdptid(null); // ����
		giHeadVO.setCdptvid(null); // ������Ϣ

		giHeadVO.setNtotalnum(goHeadVO.getNtotalnum()); // ������

		//giHeadVO.setCreator("NC_USER0000000000000"); // ������
		giHeadVO.setCreationtime(new UFDateTime(System.currentTimeMillis())); // ��������
		//giHeadVO.setBillmaker("NC_USER0000000000000"); // �Ƶ���
		//giHeadVO.setModifier("NC_USER0000000000000");
		//giHeadVO.setModifiedtime(new UFDateTime());
		giHeadVO.setDbilldate(date); // ��������
		giHeadVO.setDmakedate(new UFDate());
		giHeadVO.setVnote(goHeadVO.getVnote()); // ��ע
		giHeadVO.setFbillflag(2); // ���õ���״̬ 2-����

		giHeadVO.setPk_org(goHeadVO.getPk_org()); // �����֯
		giHeadVO.setPk_org_v(goHeadVO.getPk_org_v()); // �����֯�汾
		giHeadVO.setCwarehouseid(goHeadVO.getCotherwhid()); // �ֿ�

		// ���ó���ֿ�
		giHeadVO.setCothercalbodyoid(goHeadVO.getCothercalbodyoid()); // �����֯
		giHeadVO.setCothercalbodyvid(goHeadVO.getCothercalbodyvid()); // ��������֯�汾
		giHeadVO.setStatus(VOStatus.NEW);
		return giHeadVO;
	}

	/**
	 * ͨ��ת����ⵥ�� ��ȡת����ⵥaggVO
	 * 
	 * @param OrderNo
	 * @return
	 */
	private GeneralOutVO getGeneralOutVO(String OrderNo) {

		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";
		try {
			List<GeneralOutVO> list = (List<GeneralOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutVO.class, sqlWhere, true, false);
			if (list != null && list.size() != 0) {
				return list.get(0);
			}
		} catch (MetaDataException e) {
			e.printStackTrace();
		}
		return null;
	}

}
