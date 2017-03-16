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
		LoggerUtil.debug("写入调拨入库 - " + xml);
		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);
		HashMap<String, Object> para = new HashMap<String, Object>();
		try {
			// 取xml表头数据
			String ReceiverLocationCode = obj.getString("ReceiverLocationCode"); // 入库仓库
			String SenderLocationCode = obj.getString("SenderLocationCode");// 出库仓库
			String Sender = obj.getString("Sender");// 发货人
			String Receiver = obj.getString("Receiver");// 收货人
			String Date = obj.getString("Date");// 单据日期
			String SourceOrderNo = obj.getString("SourceOrderNo");
			
			//InvocationInfoProxy.getInstance().setUserId(WsQueryBS.getUserid(Sender));
			
			
			// 取xml表体数据
			JSONArray item = obj.getJSONArray("items");

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
					
					TransInVO transInVO = new TransInVO();
					TransOutVO agg = (TransOutVO) list.get(0);
					TransOutHeadVO ohvo = agg.getHead();
					InvocationInfoProxy.getInstance().setGroupId(ohvo.getPk_group());
					// 生成调拨入库表头数据
					TransInHeadVO hvo = InsertTransOutHeadVO(ohvo,
							SenderLocationCode, ReceiverLocationCode, Date,
							Receiver, Sender,para);
					TransOutBodyVO[] obodys = agg.getBodys();
					// 生成调拨入库表体数据
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
						CommonUtil.putFailResult(para, "单号" + SourceOrderNo
								+ "调拨入库表头数据没有生成");
						LoggerUtil.error("单号" + SourceOrderNo
								+ "调拨入库表头数据没有生成");
					}
				} else {
					CommonUtil.putFailResult(para, error);
					LoggerUtil.error(error);

				}
			} else {
				CommonUtil.putFailResult(para, "在调拨出库单数据库中不存在单号"
						+ SourceOrderNo);
				LoggerUtil.error("在调拨出库单数据库中不存在单号"
						+ SourceOrderNo);
			}

		} catch (MetaDataException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
			LoggerUtil.error("写入调拨入库异常" , e);
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "生成调拨入库单失败：" + e.getMessage());
			LoggerUtil.error("写入调拨入库异常" , e);
		} catch (Exception e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "生成调拨入库单失败：" + e.getMessage());
			LoggerUtil.error("写入调拨入库异常" , e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
		LoggerUtil.debug("离开接口 TOInOrderImpl" + rst);
		return rst;
		
	}

	// 赋值调拨入库表头数据
	public static TransInHeadVO InsertTransOutHeadVO(TransOutHeadVO ohvo,
			String SenderLocationCode, String ReceiverLocationCode,
			String Date, String Receiver, String Sender,HashMap<String, Object> para) {
//		IPFConfig ipf = NCLocator.getInstance().lookup(IPFConfig.class);
//		String pk_busitype = null;
//		try {
//			pk_busitype = ipf.retBusitypeCanStart("4E-01", "4E-01",
//					ohvo.getPk_org(), Receiver);
//		} catch (BusinessException e) {
//			// TODO 自动生成的 catch 块
//			e.printStackTrace();
//		}
//		if (pk_busitype == null) {
//		}// 判断pk_busitype的值 
		TransInHeadVO hvo = new TransInHeadVO();
		hvo.setPk_group(ohvo.getPk_group());// 集团
		hvo.setVtrantypecode("4E-01");// 单据类型
		hvo.setCbiztype(ohvo.getCbiztype());// 业务流程 
		
		hvo.setCtrantypeid(PfDataCache.getBillType("4E-01").getPk_billtypeid());// 单据类型pk
		 
		//hvo.setCtrantypeid("0001A510000000002QF8");// 测试单据类型pk
		hvo.setCdptid(null);// 部门
		hvo.setCdptvid(null);// 部门信息
		hvo.setFmodetype(0);// 0-普通 1-直运 2-寄存调拨
		hvo.setCreator(InvocationInfoProxy.getInstance().getUserId());// 创建人，，，xml发货人
		hvo.setCreationtime(new UFDateTime(System.currentTimeMillis()));// 创建时间
		hvo.setBillmaker(InvocationInfoProxy.getInstance().getUserId());// 制单人，，，xml发货人
		hvo.setDbilldate(new UFDate(Date));// 单据日期
		hvo.setDmakedate(new UFDate());// 制单日期
		hvo.setVnote(ohvo.getVnote());// 备注
		hvo.setFbillflag(2);// 单据状态 2-自由
		hvo.setPk_org(ohvo.getCothercalbodyoid());// 库存组织
		hvo.setPk_org_v(ohvo.getCothercalbodyvid());// 库存组织版本
		hvo.setCotherwhid(ohvo.getCwarehouseid());// 设置出库仓库
		hvo.setCothercalbodyoid(ohvo.getPk_org());// 库存组织
		hvo.setCothercalbodyvid(ohvo.getPk_org_v());// 出库库存组织版本
		hvo.setBdirecttranflag(UFBoolean.FALSE);
		hvo.setCsendtypeid(ohvo.getCdilivertypeid());// 运输方式
		//hvo.setCwarehouseid(ohvo.getCotherwhid()); //入库仓库 不能取调拨出库单上的入库仓库字段，因为做调拨订单时，入库仓库是空的
		try {
			hvo.setCwarehouseid(WsQueryBS.queryStordocByCode(ReceiverLocationCode).get("pk_stordoc"));
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, e.getMessage());
			LoggerUtil.error("获取入库仓库异常：" , e);
			e.printStackTrace();
		}// 仓库-xml获取的入库仓库
		hvo.setStatus(VOStatus.NEW);
		return hvo;
	}

	// 赋值给调拨入库表体数据
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
							.getInt("ScanQty"))); // 应收数量
					bvo.setNshouldnum(new UFDouble(item.getJSONObject(i)
							.getInt("ScanQty")
							* getVchangerate(dbvo.getVchangerate())));// 应收主数量
					bvo.setNassistnum(new UFDouble(item.getJSONObject(i)
							.getInt("ScanQty"))); // 实收数量
					bvo.setNnum(new UFDouble(item.getJSONObject(i).getInt(
							"ScanQty")
							* getVchangerate(dbvo.getVchangerate())));// 实收主数量
					bvo.setCrowno(item.getJSONObject(i).getString("SourceOrderLineNo"));// 行号
					bvo.setPk_group(hvo.getPk_group());// 集团
					bvo.setPk_org(hvo.getPk_org());// 库存组织
					bvo.setPk_org_v(hvo.getPk_org_v());// 库存组织版本
					bvo.setCunitid(dbvo.getCunitid());// 主单位
					bvo.setCastunitid(dbvo.getCastunitid());// 辅单位
					bvo.setVchangerate(dbvo.getVchangerate());// 换算率
					bvo.setCproductorid(dbvo.getCproductorid());// 生产厂商
					bvo.setCprojectid(dbvo.getCprojectid());// 项目
					bvo.setCasscustid(dbvo.getCasscustid());// 客户
					bvo.setCliabilityoid(dbvo.getCliabilityoid());// 利润中心
					bvo.setCliabilityvid(dbvo.getCliabilityvid());// 利润中心版本
					bvo.setCbodywarehouseid(hvo.getCwarehouseid());// 库存仓库
					bvo.setVnotebody(dbvo.getCrowno());// 行备注
					bvo.setCvendorid(dbvo.getCvendorid());// 供应商
					bvo.setCvmivenderid(dbvo.getCvmivenderid());
					bvo.setNcostprice(dbvo.getNcostprice());//单价
					bvo.setCbodytranstypecode("4E-01");
					bvo.setFlargess(dbvo.getFlargess());// 赠品
					bvo.setBsourcelargess(dbvo.getBsourcelargess());// 上游赠品行
					
						bvo.setVbatchcode(dbvo.getVbatchcode()); // 批次号
						bvo.setPk_batchcode(dbvo.getPk_batchcode());
						bvo.setDproducedate(dbvo.getDproducedate()); //生效日期
						bvo.setDvalidate(new UFDate()); //失效日期
					
					bvo.setDbizdate(hvo.getDbilldate());
					bvo.setCoutcalbodyoid(ohvo.getPk_org());
					bvo.setCoutcalbodyvid(ohvo.getPk_org_v());
					// 来源信息
					bvo.setCsourcebillhid(dbvo.getCgeneralhid());
					bvo.setCsourcebillbid(dbvo.getCgeneralbid());
					bvo.setVsourcebillcode(ohvo.getVbillcode());
					bvo.setVsourcerowno(dbvo.getCrowno());
					bvo.setCsourcetype("4Y");
					bvo.setCsourcetranstype(ohvo.getCtrantypeid());
					// 其他来源
//					bvo.setCsrc2billhid(dbvo.getCsourcebillhid());
//					bvo.setCsrc2billbid(dbvo.getCsourcebillbid());
//					bvo.setVsrc2billcode(dbvo.getVsourcebillcode());
//					bvo.setVsrc2billrowno(dbvo.getVsourcerowno());
//					bvo.setCsrc2billtype("4331");
//					bvo.setCsrc2transtype(dbvo.getCsourcetranstype());
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
				 error = error + "条形码的行号："
						+ item.getJSONObject(i).getString("SourceOrderLineNo")
						+ "  在调拨出库表体中没有匹配的行号；  ";
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
