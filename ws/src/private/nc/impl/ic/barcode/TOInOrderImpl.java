package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.ic.barcode.WsQueryBS;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.bs.framework.common.InvocationInfoProxy;
import nc.bs.framework.common.NCLocator;
import nc.bs.pf.pub.PfDataCache;
import nc.ift.ic.barcode.ITOInOrder;

import nc.itf.uap.pf.IPFBusiAction;
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
			// 取xml表头数据
			 String ReceiverLocationCode =
			 obj.getString("ReceiverLocationCode"); //入库仓库
			String SenderLocationCode = obj.getString("SenderLocationCode");// 出库仓库
			// String Sender = obj.getString("Sender");// 发货人
			String Receiver = obj.getString("Receiver");// 收货人
			String Date = obj.getString("Date");// 单据日期
			String SourceOrderNo = obj.getString("SourceOrderNo");
			// 取xml表体数据
			JSONArray item = obj.getJSONArray("item");

			String where = "nvl(dr,0) = 0 and vbillcode = '" + SourceOrderNo
					+ "'";
			String error = "";
			List<AggregatedValueObject> list = (List<AggregatedValueObject>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							TransOutHeadVO.class, where, true, false);
			if (list != null && list.size() != 0) {
				// 判断物料表中是否有相应的数据
				for (int i = 0; i < item.size(); i++) {
					if (WsQueryBS.queryPK_materialByProductCode(item
							.getJSONObject(i).getString("ProductCode")) == null) {
						if (item.getJSONObject(i).getString("ProductCode")
								.equals("[]")) {
							error = error + "获取的第" + (i + 1) + "条物料短号为空值,  ";
							continue;
						}
						error = error
								+ "物料短号"
								+ item.getJSONObject(i)
										.getString("ProductCode")
								+ "在物料表中没有数据,  ";
					}
				}
				if (error.equals("")) {
					List<TransInVO> transInVOlist = new ArrayList<TransInVO>();
					TransInVO transInVO = new TransInVO();
					TransOutVO agg = (TransOutVO) list.get(0);
					TransOutHeadVO ohvo = agg.getHead();
					// 生成调拨入库表头数据
					TransInHeadVO hvo = InsertTransOutHeadVO(ohvo,
							SenderLocationCode, ReceiverLocationCode, Date, Receiver);
					TransOutBodyVO[] obodys = agg.getBodys();
					// 生成调拨入库表体数据
					List<TransInBodyVO> bvo = getTransBodyVOTransout(ohvo,
							obodys, SenderLocationCode, item);
					if (hvo != null) {
						transInVO.setParentVO(hvo);
						if (bvo != null && bvo.size() > 0) {
							// transInVO.setChildren(TransInBodyVO.class,(ISuperVO[])
							// list.toArray());
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
							pf.processAction("WRITE", "4Y", null, transInVO,
									null, null);
							para.put("OrderNo", hvo.getVbillcode());
							CommonUtil.putSuccessResult(para);
						} else {
							CommonUtil.putFailResult(para, "单号" + SourceOrderNo
									+ "调拨入库表体数据没有生成");

						}
					} else {
						CommonUtil.putFailResult(para, "单号" + SourceOrderNo
								+ "调拨入库表头数据没有生成");
					}
				} else {
					CommonUtil.putFailResult(para, error);

				}
			} else {
				CommonUtil.putFailResult(para, "在调拨出库单数据库中不存在单号"
						+ SourceOrderNo);
			}

		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "生成调拨入库单失败：" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
	}

	// 赋值调拨入库表头数据
	public static TransInHeadVO InsertTransOutHeadVO(TransOutHeadVO ohvo,
			String SenderLocationCode,String ReceiverLocationCode, String Date, String Receiver) {
		TransInHeadVO hvo = new TransInHeadVO();
		hvo.setPk_group(ohvo.getPk_group());// 集团
		hvo.setVtrantypecode("4E-01");// 单据类型
		BilltypeVO billTypeVO = PfDataCache.getBillTypeInfo("4E-01");
		hvo.setCtrantypeid(billTypeVO.getPk_billtypeid());// 单据类型pk
		//hvo.setCtrantypeid("0001A510000000002QF8");// 单据类型pk
		hvo.setCcustomerid(ohvo.getCcustomerid());// 收货客户
		hvo.setCdptid(null);// 部门???
		hvo.setCdptvid(null);// 部门信息???
		hvo.setFmodetype(0);// 0-普通 1-直运 2-寄存调拨
		hvo.setCreator(Receiver);// 创建人????
		hvo.setCreationtime(new UFDateTime(System.currentTimeMillis()));// 创建时间
		hvo.setBillmaker(Receiver);// 制单人????
		hvo.setDbilldate(new UFDate(Date));// 单据日期???
		hvo.setDmakedate(new UFDate());// 制单日期???
		hvo.setVnote(ohvo.getVnote());// 备注
		hvo.setFbillflag(2);// 单据状态 2-自由
		hvo.setPk_org(ohvo.getPk_org());// 库存组织
		hvo.setPk_org_v(ohvo.getPk_org_v());// 库存组织版本
		hvo.setCotherwhid(ohvo.getCotherwhid() );// 设置出库仓库-xml获取的出库仓库
		hvo.setCothercalbodyoid(ohvo.getCothercalbodyoid());// 库存组织0001A510000000003BS3
		//hvo.setCothercalbodyoid("0001A510000000003BS3");
		hvo.setCothercalbodyvid(ohvo.getCothercalbodyvid());// 出库库存组织版本0001A510000000003BS2
		//hvo.setCothercalbodyvid("0001A510000000003BS2");
		hvo.setBdirecttranflag(UFBoolean.FALSE);
		hvo.setCsendtypeid(ohvo.getCdilivertypeid());// 运输方式
		hvo.setCwarehouseid(ohvo.getCwarehouseid());// 仓库
		hvo.setStatus(VOStatus.NEW);//
		return hvo;

	}

	// 赋值给调拨入库表体数据
	private static List<TransInBodyVO> getTransBodyVOTransout(TransOutHeadVO ohvo,
			TransOutBodyVO[] obodys, String SenderLocationCode, JSONArray item) {
		List<TransInBodyVO> list = new ArrayList<TransInBodyVO>();
		for (int i = 0; i < obodys.length; i++) {
			TransOutBodyVO dbvo = obodys[i];
			TransInBodyVO bvo = new TransInBodyVO();
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
			bvo.setNshouldassistnum(new UFDouble(item.getJSONObject(i).getInt("ScanQty"))); //应收数量
			bvo.setNshouldnum(new UFDouble(item.getJSONObject(i).getInt(
					"ScanQty")
					* getVchangerate(dbvo.getVchangerate())));// 应收主数量
			bvo.setNassistnum(new UFDouble(item.getJSONObject(i).getInt(
					"ScanQty")));          //shishou shuliang
			bvo.setNnum(new UFDouble(item.getJSONObject(i).getInt("ScanQty")
					* getVchangerate(dbvo.getVchangerate())));// shishou主数量
			bvo.setCrowno(dbvo.getCrowno());// 行号
			bvo.setPk_group(ohvo.getPk_group());// 集团
			bvo.setPk_org(dbvo.getPk_org());// 库存组织
			bvo.setPk_org_v(dbvo.getPk_org_v());// 库存组织版本
			bvo.setCunitid(dbvo.getCunitid());// 主单位
			bvo.setCastunitid(dbvo.getCastunitid());// 辅单位
			bvo.setVchangerate(dbvo.getVchangerate());// 换算率
			bvo.setCproductorid(dbvo.getCproductorid());// 生产厂商
			bvo.setCprojectid(dbvo.getCprojectid());// 项目
			bvo.setCasscustid(dbvo.getCasscustid());// 客户
			bvo.setCliabilityoid(dbvo.getCliabilityoid());// 利润中心
			bvo.setCliabilityvid(dbvo.getCliabilityvid());// 利润中心版本
			bvo.setCbodywarehouseid(dbvo.getCbodywarehouseid());// 库存仓库
			bvo.setVnotebody(dbvo.getCrowno());// 行备注
			bvo.setCvendorid(dbvo.getCvendorid());// 供应商
			bvo.setCvmivenderid(dbvo.getCvmivenderid());
			bvo.setNcostprice(dbvo.getNcostprice());// 单价
			bvo.setCbodytranstypecode("4E-01");
			bvo.setFlargess(dbvo.getFlargess());// 赠品
			bvo.setBsourcelargess(dbvo.getBsourcelargess());// 上游赠品行
			bvo.setVbatchcode(item.getJSONObject(i).getString("BatchNo"));// 批次号
			bvo.setCoutcalbodyoid(SenderLocationCode);
			bvo.setCoutcalbodyvid(SenderLocationCode);
			// 来源信息
			bvo.setCsourcebillhid(dbvo.getCgeneralhid());
			bvo.setCsourcebillbid(dbvo.getCgeneralbid());
			bvo.setVsourcebillcode(ohvo.getVbillcode());
			bvo.setVsourcerowno(dbvo.getCrowno());
			bvo.setCsourcetype("4Y");
			bvo.setCsourcetranstype(ohvo.getCtrantypeid());

			// 其他来源
			bvo.setCsrc2billhid(dbvo.getCsourcebillhid());
			bvo.setCsrc2billbid(dbvo.getCsourcebillbid());
			bvo.setVsrc2billcode(dbvo.getVsourcebillcode());
			bvo.setVsrc2billrowno(dbvo.getVsourcerowno());
			bvo.setCsrc2billtype("4331");
			bvo.setCsrc2transtype(dbvo.getCsourcetranstype());

			// 源头信息
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
			/*TransOutBodyVO objs = new TransOutBodyVO { bvo, key1, key2 };*/
			list.add(bvo);

		}
		return list;
	}

	// 检查发货单是否全部完成
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
	 * BusinessException("发货单：" + delbillno + ",已全部出入库完成)"); } else { throw new
	 * BusinessException("发货单：" + delbillno + ",该单对应的调拨出库数量已全部调拨入库，请先出库再入库"); }
	 * }
	 */
	private static double getVchangerate(String vchangerate) {
		String[] vcs = vchangerate.split("/");
		double vc = Double.parseDouble(vcs[0]) / Double.parseDouble(vcs[1]);
		return vc;
	}

}
