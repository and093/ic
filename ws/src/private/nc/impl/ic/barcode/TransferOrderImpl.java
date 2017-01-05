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
import nc.itf.uap.IUAPQueryBS;
import nc.itf.uap.pf.IPFBusiAction;
import nc.jdbc.framework.processor.ColumnProcessor;
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
import nc.vo.ic.m4k.entity.WhsTransBillBodyVO;
import nc.vo.ic.m4k.entity.WhsTransBillVO;
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

		List<GeneralInVO> list_gi = new ArrayList<GeneralInVO>();

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

			// ���ת����ⵥ��Ӧ�� ת�ⵥ ���ۼƳ��������� �Ƿ�Ϊ�գ���Ϊ���� ��ʾ�ó��ⵥ�����ɹ�ת����ⵥ �������ٴ�����

			if (IsTurntoGenerIn(gvo)) {
				CommonUtil.putFailResult(para, "��ת����ⵥ�Ѿ����ɹ�ת����ⵥ�������ٴ����ɣ�");
				return FreeMarkerUtil.process(para,
						"nc/config/ic/barcode/TransferInOrder.fl");
			}

			// ��ȡת����ⵥ��ͷ
			GeneralOutHeadVO goHeadVO = gvo.getHead();
			if (goHeadVO != null) {
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
		giHeadVO.setCtrantypeid("0001A510000000002QE6"); // ��������pk (���������)
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
