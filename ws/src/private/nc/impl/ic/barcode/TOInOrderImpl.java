package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.ic.barcode.WsQueryBS;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.bs.framework.common.NCLocator;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.ITOInOrder;
import nc.itf.ic.m4e.ITransInMaintain;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.ic.m4e.entity.TransInBodyVO;
import nc.vo.ic.m4e.entity.TransInHeadVO;
import nc.vo.ic.m4e.entity.TransInVO;
import nc.vo.ic.m4y.entity.TransOutBodyVO;
import nc.vo.ic.m4y.entity.TransOutHeadVO;
import nc.vo.ic.m4y.entity.TransOutVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.ISuperVO;
import nc.vo.pub.VOStatus;
import nc.vo.pub.billtype.BilltypeVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class TOInOrderImpl implements ITOInOrder {

	@Override
	public String saveTransferIn_requireNew(String xml) {
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		HashMap<String, Object> para = new HashMap<String, Object>();
		try {
			//ȡxml��ͷ����
			// String ReceiverLocationCode =
			// obj.getString("ReceiverLocationCode");// ���ֿ�
			String SenderLocationCode = obj.getString("SenderLocationCode");// ����ֿ�
			// String Sender = obj.getString("Sender");// ������
			String Receiver = obj.getString("Receiver");// �ջ���
			String Date = obj.getString("Date");// ��������
			String SourceOrderNo = obj.getString("SourceOrderNo");
			//ȡxml��������
			JSONArray item = obj.getJSONArray("item");
		
			String where = "nvl(dr,0) = 0 and vbillcode = '" + SourceOrderNo
					+ "'";
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutHeadVO.class, where, true, false);
			if (list != null && list.size() != 0) {
				List<TransInVO> transInVOlist = new ArrayList<TransInVO>();
				TransInVO transInVO = new TransInVO();
				TransOutVO agg = (TransOutVO) list.get(0);
				TransOutHeadVO ohvo = agg.getHead();
				// ���ɵ�������ͷ����
				TransInHeadVO hvo = InsertTransOutHeadVO(ohvo,
						SenderLocationCode, Date, Receiver);
				TransOutBodyVO[] obodys = agg.getBodys();
				//�ж����ϱ����Ƿ�����Ӧ������
				for (int i = 0; i <obodys.length ; i++) {
					if (WsQueryBS.queryPK_materialByProductCode(item.getJSONObject(i).getString("ProductCode")) == null) {
						CommonUtil.putFailResult(para, "���϶̺�" + item.getJSONObject(i).getString("ProductCode")
								+ "�����ϱ���û������");
					}
				}
				// ���ɵ�������������
				List<Object[]> bvo = getTransBodyVOTransout(ohvo, obodys,
						SenderLocationCode, item);
				/*
				 * if(hvo == null ){ CommonUtil.putFailResult(para, "����" +
				 * SourceOrderNo + "��������ͷ����û������"); }
				 */
				if (bvo == null || bvo.size() <= 0) {
					CommonUtil.putFailResult(para, "����" + SourceOrderNo
							+ "��������������û������");
				}
				transInVO.setParentVO(hvo);
				transInVO.setChildren(TransInBodyVO.class,
						(ISuperVO[]) list.toArray());
				transInVOlist.add(transInVO);
				if (transInVOlist != null && transInVOlist.size() > 0) {
					ITransInMaintain maintain = NCLocator.getInstance().lookup(
							ITransInMaintain.class);
					maintain.insert(transInVOlist.toArray(new TransInVO[0]));
					para.put("OrderNo", hvo.getVbillcode());
					CommonUtil.putSuccessResult(para);
				} else {
					CommonUtil.putFailResult(para, "����" + SourceOrderNo
							+ "���ɵĵ�����������Ϊ��");
				}

			} else {
				CommonUtil.putFailResult(para, "����" + SourceOrderNo
						+ "�ڳ��ⵥ���ݿ���û������");
			}

		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "���ɵ�����ⵥʧ�ܣ�" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/RroductTransInOrder.fl");
	}

	// ��ֵ��������ͷ����
	public static TransInHeadVO InsertTransOutHeadVO(TransOutHeadVO ohvo,
			String SenderLocationCode, String Date, String Receiver) {
		TransInHeadVO hvo = new TransInHeadVO();
		hvo.setPk_group(ohvo.getPk_group());// ����
		hvo.setVtrantypecode("4E-01");// ��������
		BilltypeVO billTypeVO = PfDataCache.getBillTypeInfo("4E-01");
		hvo.setCtrantypeid(billTypeVO.getPk_billtypeid());// ��������pk
		hvo.setCcustomerid(ohvo.getCcustomerid());// �ջ��ͻ�
		hvo.setCdptid(null);// ����???
		hvo.setCdptvid(null);// ������Ϣ???
		hvo.setFmodetype(0);// 0-��ͨ 1-ֱ�� 2-�Ĵ����
		hvo.setCreator(Receiver);// ������????
		hvo.setCreationtime(new UFDateTime(System.currentTimeMillis()));// ����ʱ��
		hvo.setBillmaker(Receiver);// �Ƶ���????
		hvo.setDbilldate(new UFDate(Date));// ��������???
		hvo.setDmakedate(new UFDate());// �Ƶ�����???
		hvo.setVnote(ohvo.getVnote());// ��ע
		hvo.setFbillflag(2);// ����״̬ 2-����
		hvo.setPk_org(ohvo.getPk_org());// �����֯
		hvo.setPk_org_v(ohvo.getPk_org_v());// �����֯�汾
		hvo.setCotherwhid(SenderLocationCode);// ���ó���ֿ�-xml��ȡ�ĳ���ֿ�
		hvo.setCothercalbodyoid(ohvo.getCothercalbodyoid());// �����֯
		hvo.setCothercalbodyvid(ohvo.getCothercalbodyvid());// ��������֯�汾
		hvo.setBdirecttranflag(UFBoolean.FALSE);
		hvo.setCsendtypeid(ohvo.getCdilivertypeid());// ���䷽ʽ
		hvo.setCwarehouseid(SenderLocationCode);// �ֿ�
		hvo.setStatus(VOStatus.NEW);//
		return hvo;

	}

	// ��ֵ����������������
	private static List<Object[]> getTransBodyVOTransout(TransOutHeadVO ohvo,
			TransOutBodyVO[] obodys, String SenderLocationCode, JSONArray item) {
		List<Object[]> list = new ArrayList<Object[]>();

		// boolean isallout = true; // �Ƿ�ȫ������
		/*
		 * for (TransOutBodyVO vo : obodys) { TransOutBodyVO[] bvos =
		 * vo.getBodys();
		 */
		for (int i = 0; i < obodys.length; i++) {
			TransOutBodyVO dbvo = obodys[i];
			TransInBodyVO bvo = new TransInBodyVO();
			String key1 = dbvo.getCmaterialoid() + dbvo.getVfree1()
					+ dbvo.getVfree2() + dbvo.getVfree3() + dbvo.getVfree4()
					+ dbvo.getVfree5() + dbvo.getVfree6() + dbvo.getVfree7()
					+ dbvo.getVfree8() + dbvo.getVfree9() + dbvo.getVfree10();
			String key2 = dbvo.getCmaterialoid() + dbvo.getVfree1()
					+ dbvo.getVfree3() + dbvo.getVfree4() + dbvo.getVfree5()
					+ dbvo.getVfree6() + dbvo.getVfree7() + dbvo.getVfree8()
					+ dbvo.getVfree9() + dbvo.getVfree10();
			// ƥ���������������ֿ�
			/*
			 * UFDouble[] ff = mp.get(dbvo.getCgeneralbid()); String whouse =
			 * whousemap.get(dbvo.getCgeneralbid());
			 */

			/*
			 * if (whouse == null) { continue; }
			 */
			/*
			 * UFDouble kfcount = new UFDouble(0);// ��������� UFDouble outnum = new
			 * UFDouble(0);// �ۼ�������� UFDouble onum = new UFDouble(0);// �ۼƳ�������
			 *//*
				 * if (ff != null) { kfcount = ff[0]; // ��������� outnum = ff[1];
				 * // �ۼ�������� onum = ff[2]; // �ۼƳ������� }
				 */
			/*
			 * if (kfcount.doubleValue() != 0) { isallout = false; }
			 */
			bvo.setCmaterialoid(dbvo.getCmaterialoid());
			bvo.setCmaterialvid(dbvo.getCmaterialvid());
			bvo.setVfree1(dbvo.getVfree1());
			bvo.setVfree2(dbvo.getVfree2());
			bvo.setVfree3(dbvo.getVfree3());
			bvo.setVfree4(dbvo.getVfree4());
			bvo.setVfree5(dbvo.getVfree5());
			bvo.setVfree6(dbvo.getVfree6());
			bvo.setVfree7(dbvo.getVfree7());
			bvo.setVfree8(dbvo.getVfree8());
			bvo.setVfree9(dbvo.getVfree9());
			bvo.setVfree10(dbvo.getVfree10());
			bvo.setNnum(new UFDouble(item.getJSONObject(i).getInt("ScanQty")
					* getVchangerate(dbvo.getVchangerate())));// ������
			bvo.setNshouldnum(new UFDouble(item.getJSONObject(i).getInt(
					"ScanQty")
					* getVchangerate(dbvo.getVchangerate())));// Ӧ��������
			bvo.setCrowno(((i + 1) * 10) + "");// �к�
			bvo.setPk_group(ohvo.getPk_group());// ����
			bvo.setPk_org(dbvo.getPk_org());// �����֯
			bvo.setPk_org_v(dbvo.getPk_org_v());// �����֯�汾
			bvo.setCunitid(dbvo.getCunitid());// ����λ
			bvo.setCastunitid(dbvo.getCastunitid());// ����λ
			bvo.setVchangerate(dbvo.getVchangerate());// ������
			bvo.setCproductorid(dbvo.getCproductorid());// ��������
			bvo.setCprojectid(dbvo.getCprojectid());// ��Ŀ
			bvo.setCasscustid(dbvo.getCasscustid());// �ͻ�
			bvo.setCliabilityoid(dbvo.getCliabilityoid());// ��������
			bvo.setCliabilityvid(dbvo.getCliabilityvid());// �������İ汾
			bvo.setCbodywarehouseid(SenderLocationCode);// ���ֿ�
			bvo.setVnotebody(dbvo.getCrowno());// �б�ע
			bvo.setCvendorid(dbvo.getCvendorid());// ��Ӧ��
			bvo.setCvmivenderid(dbvo.getCvmivenderid());
			bvo.setNcostprice(dbvo.getNcostprice());// ����
			bvo.setCbodytranstypecode("4E-01");
			bvo.setFlargess(dbvo.getFlargess());// ��Ʒ
			bvo.setBsourcelargess(dbvo.getBsourcelargess());// ������Ʒ��
			bvo.setVbatchcode(item.getJSONObject(i).getString("BatchNo"));// ���κ�
			// ��Դ��Ϣ
			bvo.setCsourcebillhid(dbvo.getCgeneralhid());
			bvo.setCsourcebillbid(dbvo.getCgeneralbid());
			bvo.setVsourcebillcode(ohvo.getVbillcode());
			bvo.setVsourcerowno(dbvo.getCrowno());
			bvo.setCsourcetype("4Y");
			bvo.setCsourcetranstype(ohvo.getCtrantypeid());

			// ������Դ
			bvo.setCsrc2billhid(dbvo.getCsourcebillhid());
			bvo.setCsrc2billbid(dbvo.getCsourcebillbid());
			bvo.setVsrc2billcode(dbvo.getVsourcebillcode());
			bvo.setVsrc2billrowno(dbvo.getVsourcerowno());
			bvo.setCsrc2billtype("4331");
			bvo.setCsrc2transtype(dbvo.getCsourcetranstype());

			// Դͷ��Ϣ
			bvo.setVfirstbillcode(dbvo.getVfirstbillcode());
			bvo.setVfirstrowno(dbvo.getVfirstrowno());
			bvo.setCfirstbillbid(dbvo.getCfirstbillbid());
			bvo.setCfirstbillhid(dbvo.getCfirstbillhid());
			bvo.setCfirsttranstype(dbvo.getCfirsttranstype());
			bvo.setCfirsttype(dbvo.getCfirsttype());

			bvo.setNweight(dbvo.getNweight());
			bvo.setNvolume(dbvo.getNvolume());
			bvo.setDplanoutdate(dbvo.getDplanoutdate());
			bvo.setDplanarrivedate(dbvo.getDplanarrivedate());
			bvo.setStatus(VOStatus.NEW);
			Object[] objs = new Object[] { bvo, key1, key2 };
			list.add(objs);

		}
		return list;
	}

	// ��鷢�����Ƿ�ȫ�����
	/*
	 * String sql =
	 * "select sum(nvl(b.nnum,0)-nvl(b.ntotaloutnum,0)) soutnum from so_delivery_b b,so_delivery d where d.dr=0 and b.dr=0 and"
	 * + " b.cdeliveryid = d.cdeliveryid and d.vbillcode = '" + delbillno + "'";
	 *//*
		 * UFDouble soutnum = (UFDouble) ((Object) getDAO()).executeQuery(sql,
		 * new ResultSetProcessor() {
		 */
	/*
	 * public Object handleResultSet(ResultSet rs) throws SQLException { if (rs
	 * == null) return null; UFDouble soutnum = null; while (rs.next()) {
	 * soutnum = new UFDouble(rs.getString("soutnum")); } return soutnum; } })
	 */

	/*
	 * if (isallout) { if (soutnum.doubleValue() == 0) { throw new
	 * BusinessException("��������" + delbillno + ",��ȫ����������)"); } else { throw new
	 * BusinessException("��������" + delbillno + ",�õ���Ӧ�ĵ�������������ȫ��������⣬���ȳ��������"); }
	 * }
	 */
	private static double getVchangerate(String vchangerate) {
		String[] vcs = vchangerate.split("/");
		double vc = Double.parseDouble(vcs[0]) / Double.parseDouble(vcs[1]);
		return vc;
	}

}
