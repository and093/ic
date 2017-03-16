package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.DAOException;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.ic.barcode.WsQueryBS;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.ITOInOrder;
import nc.itf.uap.pf.IPFBusiAction;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.pub.ic.barcode.LoggerUtil;
import nc.vo.ic.m4e.entity.TransInBodyVO;
import nc.vo.ic.m4e.entity.TransInHeadVO;
import nc.vo.ic.m4e.entity.TransInVO;
import nc.vo.ic.m4y.entity.TransOutBodyVO;
import nc.vo.ic.m4y.entity.TransOutHeadVO;
import nc.vo.ic.m4y.entity.TransOutVO;
import nc.vo.pub.AggregatedValueObject;
import nc.vo.pub.BusinessException;
import nc.vo.pub.VOStatus;
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
		LoggerUtil.debug("д�������� - " + xml);
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		HashMap<String, Object> para = new HashMap<String, Object>();
		try {
			// ȡxml��ͷ����
			String ReceiverLocationCode = obj.getString("ReceiverLocationCode"); // ���ֿ�
			String SenderLocationCode = obj.getString("SenderLocationCode");// ����ֿ�
			String Sender = obj.getString("Sender");// ������
			String Receiver = obj.getString("Receiver");// �ջ���
			String Date = obj.getString("Date");// ��������
			String SourceOrderNo = obj.getString("SourceOrderNo");
			
			//InvocationInfoProxy.getInstance().setUserId(WsQueryBS.getUserid(Sender));
			
			
			// ȡxml��������
			JSONArray item = obj.getJSONArray("items");

			String where = "nvl(dr,0) = 0 and vbillcode = '" + SourceOrderNo
					+ "'";
			String error = "";
		
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutHeadVO.class, where, true, false);
			if (list != null && list.size() != 0) {
			
				// �ж����ϱ����Ƿ�����Ӧ������
				for (int i = 0; i < item.size(); i++) {
					if (WsQueryBS.queryPK_materialByProductCode(item
							.getJSONObject(i).getString("ProductCode")) == null) {
						if (item.getJSONObject(i).getString("ProductCode")
								.equals("[]")) {
							error = error + "��ȡ�ĵ�" + (i + 1) + "�����϶̺�Ϊ��ֵ,  ";
							continue;
						}
						error = error
								+ "���϶̺�"
								+ item.getJSONObject(i)
										.getString("ProductCode")
								+ "�����ϱ���û������,  ";
					}
				}
				if (error.equals("")) {
					
					TransInVO transInVO = new TransInVO();
					TransOutVO agg = (TransOutVO) list.get(0);
					TransOutHeadVO ohvo = agg.getHead();
					InvocationInfoProxy.getInstance().setGroupId(ohvo.getPk_group());
					// ���ɵ�������ͷ����
					TransInHeadVO hvo = InsertTransOutHeadVO(ohvo,
							SenderLocationCode, ReceiverLocationCode, Date,
							Receiver, Sender,para);
					TransOutBodyVO[] obodys = agg.getBodys();
					// ���ɵ�������������
					List<TransInBodyVO> bvo = getTransBodyVOTransout(hvo, ohvo,
							obodys, SenderLocationCode, item, para);
					if (hvo != null) {
						
						transInVO.setParentVO(hvo);
						if (bvo != null && bvo.size() > 0) {
							
							transInVO.setChildrenVO(bvo
									.toArray(new TransInBodyVO[0]));
							IPFBusiAction pf = NCLocator.getInstance().lookup(
									IPFBusiAction.class);
							InvocationInfoProxy.getInstance().setUserId(
									transInVO.getHead().getBillmaker());
							InvocationInfoProxy.getInstance().setGroupId(
									transInVO.getHead().getPk_group());
							InvocationInfoProxy.getInstance().setBizDateTime(
									System.currentTimeMillis());

							TransInVO[] transInVOs = (TransInVO[]) pf
									.processAction("WRITE", "4Y", null,
											transInVO, null, null);
							if (transInVOs.length != 0) {
								
								para.put("OrderNo", transInVOs[0].getHead()
										.getVbillcode());
								
								CommonUtil.putSuccessResult(para);
							}

						} 
					} else {
						CommonUtil.putFailResult(para, "����" + SourceOrderNo
								+ "��������ͷ����û������");
						LoggerUtil.error("����" + SourceOrderNo
								+ "��������ͷ����û������");
					}
				} else {
					CommonUtil.putFailResult(para, error);
					LoggerUtil.error(error);

				}
			} else {
				CommonUtil.putFailResult(para, "�ڵ������ⵥ���ݿ��в����ڵ���"
						+ SourceOrderNo);
				LoggerUtil.error("�ڵ������ⵥ���ݿ��в����ڵ���"
						+ SourceOrderNo);
			}

		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
			LoggerUtil.error("д���������쳣" , e);
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "���ɵ�����ⵥʧ�ܣ�" + e.getMessage());
			LoggerUtil.error("д���������쳣" , e);
		} catch (Exception e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "���ɵ�����ⵥʧ�ܣ�" + e.getMessage());
			LoggerUtil.error("д���������쳣" , e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
		LoggerUtil.debug("�뿪�ӿ� TOInOrderImpl" + rst);
		return rst;
		
	}

	// ��ֵ��������ͷ����
	public static TransInHeadVO InsertTransOutHeadVO(TransOutHeadVO ohvo,
			String SenderLocationCode, String ReceiverLocationCode,
			String Date, String Receiver, String Sender,HashMap<String, Object> para) {
//		IPFConfig ipf = NCLocator.getInstance().lookup(IPFConfig.class);
//		String pk_busitype = null;
//		try {
//			pk_busitype = ipf.retBusitypeCanStart("4E-01", "4E-01",
//					ohvo.getPk_org(), Receiver);
//		} catch (BusinessException e) {
//			// TODO �Զ����ɵ� catch ��
//			e.printStackTrace();
//		}
//		if (pk_busitype == null) {
//		}// �ж�pk_busitype��ֵ 
		TransInHeadVO hvo = new TransInHeadVO();
		hvo.setPk_group(ohvo.getPk_group());// ����
		hvo.setVtrantypecode("4E-01");// ��������
		hvo.setCbiztype(ohvo.getCbiztype());// ҵ������ 
		
		hvo.setCtrantypeid(PfDataCache.getBillType("4E-01").getPk_billtypeid());// ��������pk
		 
		//hvo.setCtrantypeid("0001A510000000002QF8");// ���Ե�������pk
		hvo.setCdptid(null);// ����
		hvo.setCdptvid(null);// ������Ϣ
		hvo.setFmodetype(0);// 0-��ͨ 1-ֱ�� 2-�Ĵ����
		hvo.setCreator(InvocationInfoProxy.getInstance().getUserId());// �����ˣ�����xml������
		hvo.setCreationtime(new UFDateTime(System.currentTimeMillis()));// ����ʱ��
		hvo.setBillmaker(InvocationInfoProxy.getInstance().getUserId());// �Ƶ��ˣ�����xml������
		hvo.setDbilldate(new UFDate(Date));// ��������
		hvo.setDmakedate(new UFDate());// �Ƶ�����
		hvo.setVnote(ohvo.getVnote());// ��ע
		hvo.setFbillflag(2);// ����״̬ 2-����
		hvo.setPk_org(ohvo.getCothercalbodyoid());// �����֯
		hvo.setPk_org_v(ohvo.getCothercalbodyvid());// �����֯�汾
		hvo.setCotherwhid(ohvo.getCwarehouseid());// ���ó���ֿ�
		hvo.setCothercalbodyoid(ohvo.getPk_org());// �����֯
		hvo.setCothercalbodyvid(ohvo.getPk_org_v());// ��������֯�汾
		hvo.setBdirecttranflag(UFBoolean.FALSE);
		hvo.setCsendtypeid(ohvo.getCdilivertypeid());// ���䷽ʽ
		//hvo.setCwarehouseid(ohvo.getCotherwhid()); //���ֿ� ����ȡ�������ⵥ�ϵ����ֿ��ֶΣ���Ϊ����������ʱ�����ֿ��ǿյ�
		try {
			hvo.setCwarehouseid(WsQueryBS.queryStordocByCode(ReceiverLocationCode).get("pk_stordoc"));
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, e.getMessage());
			LoggerUtil.error("��ȡ���ֿ��쳣��" , e);
			e.printStackTrace();
		}// �ֿ�-xml��ȡ�����ֿ�
		hvo.setStatus(VOStatus.NEW);
		return hvo;
	}

	// ��ֵ����������������
	private static List<TransInBodyVO> getTransBodyVOTransout(TransInHeadVO hvo,
			TransOutHeadVO ohvo, TransOutBodyVO[] obodys,
			String SenderLocationCode, JSONArray item,
			HashMap<String, Object> para) {
		List<TransInBodyVO> list = new ArrayList<TransInBodyVO>();
		String error = "";
		for (int i = 0; i < item.size(); i++) {
			boolean flag = false;
		
			for (TransOutBodyVO dbvo : obodys) {
				if (item.getJSONObject(i).getString("SourceOrderLineNo").equals(dbvo.getCrowno())) {
					TransInBodyVO bvo = new TransInBodyVO();
					flag = true;
					bvo.setCmaterialoid(dbvo.getCmaterialoid());
					bvo.setCmaterialvid(dbvo.getCmaterialvid());
					bvo.setNshouldassistnum(new UFDouble(item.getJSONObject(i)
							.getInt("ScanQty"))); // Ӧ������
					bvo.setNshouldnum(new UFDouble(item.getJSONObject(i)
							.getInt("ScanQty")
							* getVchangerate(dbvo.getVchangerate())));// Ӧ��������
					bvo.setNassistnum(new UFDouble(item.getJSONObject(i)
							.getInt("ScanQty"))); // ʵ������
					bvo.setNnum(new UFDouble(item.getJSONObject(i).getInt(
							"ScanQty")
							* getVchangerate(dbvo.getVchangerate())));// ʵ��������
					bvo.setCrowno(item.getJSONObject(i).getString("SourceOrderLineNo"));// �к�
					bvo.setPk_group(hvo.getPk_group());// ����
					bvo.setPk_org(hvo.getPk_org());// �����֯
					bvo.setPk_org_v(hvo.getPk_org_v());// �����֯�汾
					bvo.setCunitid(dbvo.getCunitid());// ����λ
					bvo.setCastunitid(dbvo.getCastunitid());// ����λ
					bvo.setVchangerate(dbvo.getVchangerate());// ������
					bvo.setCproductorid(dbvo.getCproductorid());// ��������
					bvo.setCprojectid(dbvo.getCprojectid());// ��Ŀ
					bvo.setCasscustid(dbvo.getCasscustid());// �ͻ�
					bvo.setCliabilityoid(dbvo.getCliabilityoid());// ��������
					bvo.setCliabilityvid(dbvo.getCliabilityvid());// �������İ汾
					bvo.setCbodywarehouseid(hvo.getCwarehouseid());// ���ֿ�
					bvo.setVnotebody(dbvo.getCrowno());// �б�ע
					bvo.setCvendorid(dbvo.getCvendorid());// ��Ӧ��
					bvo.setCvmivenderid(dbvo.getCvmivenderid());
					bvo.setNcostprice(dbvo.getNcostprice());//����
					bvo.setCbodytranstypecode("4E-01");
					bvo.setFlargess(dbvo.getFlargess());// ��Ʒ
					bvo.setBsourcelargess(dbvo.getBsourcelargess());// ������Ʒ��
					
						bvo.setVbatchcode(dbvo.getVbatchcode()); // ���κ�
						bvo.setPk_batchcode(dbvo.getPk_batchcode());
						bvo.setDproducedate(dbvo.getDproducedate()); //��Ч����
						bvo.setDvalidate(new UFDate()); //ʧЧ����
					
					bvo.setDbizdate(hvo.getDbilldate());
					bvo.setCoutcalbodyoid(ohvo.getPk_org());
					bvo.setCoutcalbodyvid(ohvo.getPk_org_v());
					// ��Դ��Ϣ
					bvo.setCsourcebillhid(dbvo.getCgeneralhid());
					bvo.setCsourcebillbid(dbvo.getCgeneralbid());
					bvo.setVsourcebillcode(ohvo.getVbillcode());
					bvo.setVsourcerowno(dbvo.getCrowno());
					bvo.setCsourcetype("4Y");
					bvo.setCsourcetranstype(ohvo.getCtrantypeid());
					// ������Դ
//					bvo.setCsrc2billhid(dbvo.getCsourcebillhid());
//					bvo.setCsrc2billbid(dbvo.getCsourcebillbid());
//					bvo.setVsrc2billcode(dbvo.getVsourcebillcode());
//					bvo.setVsrc2billrowno(dbvo.getVsourcerowno());
//					bvo.setCsrc2billtype("4331");
//					bvo.setCsrc2transtype(dbvo.getCsourcetranstype());
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
					list.add(bvo);
				}
			}
			if (!flag) {
				 error = error + "��������кţ�"
						+ item.getJSONObject(i).getString("SourceOrderLineNo")
						+ "  �ڵ������������û��ƥ����кţ�  ";
			}
		}
		if (!error.equals("")) {
			CommonUtil.putFailResult(para, error);
			LoggerUtil.error(error);
			list.clear(); 
			return list;
		}

		return list;
	}

	private static double getVchangerate(String vchangerate) {
		String[] vcs = vchangerate.split("/");
		double vc = Double.parseDouble(vcs[0]) / Double.parseDouble(vcs[1]);
		return vc;
	}

}
