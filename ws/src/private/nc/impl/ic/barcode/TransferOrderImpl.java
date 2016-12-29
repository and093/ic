package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.WsQueryBS;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.ITransferOrder;
import nc.itf.uap.pf.IPFBusiAction;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.ic.m4a.entity.GeneralInBodyVO;
import nc.vo.ic.m4a.entity.GeneralInHeadVO;
import nc.vo.ic.m4a.entity.GeneralInVO;
import nc.vo.ic.m4i.entity.GeneralOutBodyVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.pub.BusinessException;
import nc.vo.pub.CircularlyAccessibleValueObject;
import nc.vo.pub.ISuperVO;
import nc.vo.pub.VOStatus;
import nc.vo.pub.billtype.BilltypeVO;
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
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 2.9 д��ת����ⵥ
	 */
	@Override
	public String saveTransferIn_requireNew(String xml) {

		HashMap<String, Object> para = new HashMap<String, Object>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		UFDate Date = new UFDate(obj.getString("Date"));
		String OrderNo = obj.getString("OrderNo");
		JSONArray item = obj.getJSONArray("item");

		GeneralInVO gvi = new GeneralInVO();

		// ����OrderNo ��ѯNC ת����ⵥ
		GeneralOutVO gvo = getGeneralOutVO(OrderNo);
		if (gvo == null) {
			CommonUtil.putFailResult(para, "ת����ⵥ��" + OrderNo + "��ѯʧ��");
		} else {
			// ��ȡת����ⵥ��ͷ
			GeneralOutHeadVO goHeadVO = gvo.getHead();
			if (goHeadVO != null) {
				// ͨ��ת����ⵥ��ͷ����ת����ⵥ��ͷ
				gvi.setParent(this.setGeneralInHeadVO(goHeadVO, Date));
			} else {
				CommonUtil.putFailResult(para, "ת����ⵥ��" + OrderNo
						+ "��Ӧ�ı�ͷ����Ϊ�գ�");
			}
			List<GeneralInBodyVO> list = setGeneralInBodyVO(gvo,item,para);
			// ͨ��ת����ⵥ��ȡ����
			if (list != null && list.size() != 0) {
				gvi.setChildren(GeneralInBodyVO.class,
						(ISuperVO[]) list.toArray());

				IPFBusiAction pf = NCLocator.getInstance().lookup(
						IPFBusiAction.class);
				InvocationInfoProxy.getInstance().setUserId(
						gvi.getHead().getBillmaker());
				InvocationInfoProxy.getInstance().setGroupId(
						gvi.getHead().getPk_group());
				InvocationInfoProxy.getInstance().setBizDateTime(
						System.currentTimeMillis());
				try {
					pf.processAction("WRITE", "4A", null, gvi, null, null);
					CommonUtil.putSuccessResult(para);
				} catch (BusinessException e) {
					CommonUtil.putFailResult(para, e.getMessage());
					e.printStackTrace();
				}
			} else {
				CommonUtil.putFailResult(para, "ת����ⵥ��" + OrderNo
						+ "��Ӧ�ı��嵥��Ϊ�գ�");
			}
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
	}

	/**
	 * ͨ��ת����ⵥ��ȡת����ⵥ����
	 * 
	 * @param gvo
	 *            ת����ⵥ aggVO
	 * @return
	 */
	private List<GeneralInBodyVO> setGeneralInBodyVO(GeneralOutVO gvo,
			JSONArray item,HashMap<String, Object> para) {
		
		int count = 0;
		String errorCode = new String();
		
		GeneralOutBodyVO[] goBodys = gvo.getBodys();
		GeneralOutHeadVO gohead = gvo.getHead();
		List<GeneralInBodyVO> list = new ArrayList<GeneralInBodyVO>();
		int index = 0;
		for (; index < goBodys.length; index++) {
			
			String pk_material = WsQueryBS.queryPK_materialByProductCode(item
					.getJSONObject(index).getString("ProductCode"));  //�������϶̺Ż�ȡ����pk
			
			if(pk_material == null) {
				CommonUtil.putFailResult(para, "�Ϻ�"+item.getJSONObject(index).getString("ProductCode")+"�Ҳ�����Ӧ������");
				return null;  //��ȡ����pkʧ��
			}
			
			boolean flag = false;
			for (GeneralOutBodyVO go : goBodys) {
				if(pk_material.equals(go.getCmaterialoid())){
					flag = true;
					GeneralInBodyVO gi = new GeneralInBodyVO();
					gi.setPk_group(gohead.getPk_group()); // ����
	
					gi.setCrowno(go.getCrowno()); // �к�
					gi.setCmaterialoid(go.getCmaterialoid()); // ����
					gi.setCmaterialvid(go.getCmaterialvid()); // ���ϱ���
					gi.setVbdef8(item.getJSONObject(index).getString("ProductCode")); // ���϶̺�
					gi.setCunitid(go.getCunitid()); // ����λ
					gi.setCastunitid(go.getCastunitid()); // ��λ
					gi.setVchangerate(go.getVchangerate()); // ������
					gi.setNshouldassistnum(go.getNshouldassistnum()); // Ӧ������
					gi.setNshouldnum(new UFDouble(go.getNshouldassistnum()
							.doubleValue() * getVchangerate(go.getVchangerate()))); // Ӧ��������
																					// =
																					// Ӧ������*������
					gi.setNassistnum(new UFDouble(item.getJSONObject(index).getInt(
							"ScanQty"))); // ʵ������
					gi.setNnum(gi.getNshouldnum()); // ʵ�������� �� Ӧ��������һֱ
					gi.setCbodywarehouseid(go.getCbodywarehouseid()); // ���ֿ�
	
					gi.setNcostprice(go.getNcostprice()); // ����
					gi.setNcostmny(go.getNcostmny()); // ���
					gi.setDbizdate(new UFDate()); // �������
					gi.setVbatchcode(go.getVbatchcode()); // ���κ�
					gi.setDproducedate(go.getDproducedate()); // ��������
					gi.setVvendbatchcode(go.getVvendbatchcode()); // ��Ӧ�����κ�
	
					gi.setCprojectid(go.getCprojectid()); // ��Ŀ
					gi.setCasscustid(go.getCasscustid()); // �ͻ�
	
					// ��Դ��Ϣ
					gi.setCsourcebillhid(go.getCgeneralhid());
					gi.setCsourcebillbid(go.getCgeneralbid());
					gi.setVsourcebillcode(gohead.getVbillcode());
					gi.setVsourcerowno(go.getCrowno());
					gi.setCsourcetype("4A");
					gi.setCsourcetranstype(gohead.getCtrantypeid());
	
					// ������Դ
					gi.setCsrc2billhid(go.getCsrc2billhid());
					gi.setCsrc2billbid(go.getCsrc2billbid());
					gi.setVsrc2billcode(go.getVsrc2billcode());
					gi.setVsrc2billrowno(go.getVsrc2billrowno());
					// gi.setCsrc2billtype(go.get) ������Դ�������ͱ���
					gi.setCsrc2transtype(go.getCsrc2transtype());
	
					// Դͷ��Ϣ
					gi.setVfirstbillcode(go.getVfirstbillcode());
					gi.setVfirstrowno(go.getVfirstrowno());
					gi.setCfirstbillbid(go.getCfirstbillbid());
					gi.setCfirstbillhid(go.getCfirstbillhid());
					gi.setCfirsttranstype(go.getCfirsttranstype());
					gi.setCfirsttype(go.getCfirsttype());
	
					gi.setNweight(go.getNweight());
					gi.setNvolume(go.getNvolume());
					gi.setStatus(VOStatus.NEW);
	
					list.add(gi);
					count++;
				} //end if pk_material.equals(go.getCmaterialoid())
			} //end for go
			if(!flag){
				errorCode += item.getJSONObject(index).getString("ProductCode")+" ";
			}
		}
		if(count != goBodys.length){
			CommonUtil.putFailResult(para, "�������϶̺��Ҳ�����Ӧ���ϣ�"+errorCode);
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
		BilltypeVO billTypeVO = PfDataCache.getBillTypeInfo("4A-02");
		giHeadVO.setCtrantypeid(billTypeVO.getPk_billtypeid()); // ��������pk
																// (���������)
		giHeadVO.setCdptid(null); // ����
		giHeadVO.setCdptvid(null); // ������Ϣ

		giHeadVO.setNtotalnum(goHeadVO.getNtotalnum()); // ������

		giHeadVO.setCreator("NC_USER0000000000000"); // ������
		giHeadVO.setCreationtime(new UFDateTime(System.currentTimeMillis())); // ��������
		giHeadVO.setBillmaker("NC_USER0000000000000"); // �Ƶ���
		giHeadVO.setModifier("NC_USER0000000000000");
		giHeadVO.setModifiedtime(new UFDateTime());
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
