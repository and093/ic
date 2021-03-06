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
		LoggerUtil.debug("写入转库出库 saveTransferOut_requireNew " + xml);
		HashMap<String, Object> para = new HashMap<String, Object>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String senderLocationCode = obj.getString("SenderLocationCode");
		String receiverLocationCode = obj.getString("ReceiverLocationCode");
		JSONArray items = obj.getJSONArray("items");

		try {
			if (senderLocationCode == null || receiverLocationCode == null) {
				throw new BusinessException(
						"SenderLocationCode和ReceiverLocationCode不能为空");
			}
			HashMap<String, String> outStoreMap = WsQueryBS
					.queryStordocByCode(senderLocationCode);
			HashMap<String, String> inStoreMap = WsQueryBS
					.queryStordocByCode(receiverLocationCode);
			if (outStoreMap.size() == 0) {
				throw new BusinessException("仓库对照找不到仓库编码" + senderLocationCode);
			}
			if (inStoreMap.size() == 0) {
				throw new BusinessException("仓库对照找不到仓库编码"
						+ receiverLocationCode);
			}
			if (!outStoreMap.get("pk_org").equals(inStoreMap.get("pk_org"))) {
				throw new BusinessException("转出跟转入仓库所属组织不一致");
			}

			WhsTransBillVO wtbillvo = new WhsTransBillVO();
			WhsTransBillHeaderVO wthvo = getWhsTransBillHeaderVO(obj,
					outStoreMap, inStoreMap);
			wtbillvo.setParent(wthvo);
			wtbillvo.setChildrenVO(getWhsTransBillBodysVO(wthvo, items));
			// 保存转库单
			IPFBusiAction pf = NCLocator.getInstance().lookup(
					IPFBusiAction.class);
			pf.processAction("WRITE", "4K", null, wtbillvo, null, null);
			LoggerUtil.error("转库保存 ");
			pf.processAction("APPROVE", "4K", null, wtbillvo, null, null);
			LoggerUtil.error("转库审核 ");
			// 转库单推其他出库
			IPfExchangeService exchangeService = NCLocator.getInstance()
					.lookup(IPfExchangeService.class);
			GeneralOutVO outvo = (GeneralOutVO) exchangeService.runChangeData(
					"4K", "4I", wtbillvo, null);
			GeneralOutHeadVO outheadvo = outvo.getHead();
			outheadvo.setVtrantypecode("4I-02");
			outheadvo.setCtrantypeid(PfDataCache.getBillType("4I-02")
					.getPk_billtypeid());
			GeneralOutBodyVO[] outbodys = outvo.getBodys();
			for (int i = 0; i < outbodys.length; i++) {
				GeneralOutBodyVO outbody = outbodys[i];
				outbody.setCrowno(String.valueOf((i + 1) * 10));
				outbody.setNassistnum(outbody.getNshouldassistnum());
				outbody.setNnum(outbody.getNshouldnum());
				outbody.setVtransfercode("4I-02");
				// 批次号
				if(outbody.getVbatchcode() != null){
					HashMap<String, String> batchMap = WsQueryBS.getBatchCode(outbody.getCmaterialoid(), outbody.getVbatchcode());
					outbody.setPk_batchcode(batchMap.get("pk_batchcode"));
					outbody.setDproducedate(batchMap.get("dproducedate") == null ? null : new UFDate(batchMap.get("dproducedate")));
					outbody.setDvalidate(batchMap.get("dvalidate") == null ? null : new UFDate(batchMap.get("dvalidate")));
					outbody.setDinbounddate(batchMap.get("dinbounddate") == null ? null : new UFDate(batchMap.get("dinbounddate")));
				}
			}
			GeneralOutVO[] outrst = (GeneralOutVO[]) pf.processAction("WRITE",
					"4I", null, outvo, null, null);
			LoggerUtil.error("其他出库保存 ");
			para.put("OrderNo", outrst[0].getHead().getVbillcode());
			CommonUtil.putSuccessResult(para);
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "发生异常：" + e.getMessage());
			LoggerUtil.error("写入转库出库异常", e);
		} catch (Exception e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "发生异常：" + e.getMessage());
			LoggerUtil.error("写入转库出库异常", e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/PostProductionOrderl.fl");
		LoggerUtil.debug("写入转库出库结果 saveTransferOut_requireNew " + rst);
		return rst;
	}

	private WhsTransBillHeaderVO getWhsTransBillHeaderVO(JSONObject obj,
			HashMap<String, String> outStoreMap,
			HashMap<String, String> inStoreMap) {
		WhsTransBillHeaderVO wthvo = new WhsTransBillHeaderVO();
		UFDate Date = new UFDate(obj.getString("Date"));
		String sender = obj.getString("Sender");
		String receiver = obj.getString("Receiver");
		InvocationInfoProxy.getInstance().setGroupId(
				outStoreMap.get("pk_group"));

		String pk_org = outStoreMap.get("pk_org");
		String pk_org_v = outStoreMap.get("pk_vid");
		wthvo.setCorpoid(pk_org);
		wthvo.setCorpvid(pk_org_v);
		wthvo.setCotherwhid(inStoreMap.get("pk_stordoc")); // 入库仓库
		wthvo.setCtrantypeid(PfDataCache.getBillType("4K-01")
				.getPk_billtypeid());
		wthvo.setCwarehouseid(outStoreMap.get("pk_stordoc")); // 出库仓库
		wthvo.setDbilldate(Date);
		wthvo.setFbillflag(1); // 单据状态
		wthvo.setPk_group(outStoreMap.get("pk_group"));
		wthvo.setPk_org(pk_org);
		wthvo.setPk_org_v(pk_org_v);
		wthvo.setVtrantypecode("4K-01");

		return wthvo;
	}

	private WhsTransBillBodyVO[] getWhsTransBillBodysVO(
			WhsTransBillHeaderVO wthvo, JSONArray items)
			throws BusinessException {
		WhsTransBillBodyVO[] wtbodys = new WhsTransBillBodyVO[items.size()];
		for (int i = 0; i < items.size(); i++) {
			WhsTransBillBodyVO bvo = new WhsTransBillBodyVO();
			JSONObject jsitem = items.getJSONObject(i);
			String ProductCode = jsitem.getString("ProductCode");
			String BatchNo = jsitem.getString("BatchNo");
			UFDouble ScanQty = new UFDouble(jsitem.getInt("ScanQty"));
			HashMap<String, String> materialMap = WsQueryBS
					.queryMaterialInfoByCode(ProductCode);
			if (materialMap.size() == 0) {
				throw new BusinessException("根据条码短号" + ProductCode
						+ "找不到对应的物料或者物料单位不是箱");
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
			
			LoggerUtil.error("-----------------  ");
			LoggerUtil.error("数量：" + bvo.getNassistnum());
			LoggerUtil.error("主数量：" + bvo.getNnum());
			LoggerUtil.error("换算率：" + bvo.getVchangerate());
			LoggerUtil.error("单位：" + bvo.getCastunitid());
			LoggerUtil.error("主单位：" + bvo.getCunitid());
			LoggerUtil.error("-----------------  ");
			
			
			// 判断物料是否启用了批次
			if (WsQueryBS.getWholemanaflag(bvo.getCmaterialoid(), bvo.getPk_org())) {
				// 批次号
				bvo.setVbatchcode(BatchNo);
				HashMap<String, String> batchMap = WsQueryBS.getBatchCode(bvo.getCmaterialoid(), BatchNo);
				if(batchMap == null){
					throw new BusinessException("批次号" + BatchNo + "对应的批次档案不存在");
				}
				bvo.setPk_batchcode(batchMap.get("pk_batchcode"));
				bvo.setDproducedate(batchMap.get("dproducedate") == null ? null : new UFDate(batchMap.get("dproducedate")));
				bvo.setDvalidate(batchMap.get("dvalidate") == null ? null : new UFDate(batchMap.get("dvalidate")));
			}
			wtbodys[i] = bvo;
		}
		return wtbodys;
	}

	/**
	 * 2.9 写入转库入库单
	 */
	@Override
	public String saveTransferIn_requireNew(String xml) {
		LoggerUtil.debug("写入转库入库 saveTransferIn_requireNew  " + xml);
		HashMap<String, Object> para = new HashMap<String, Object>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		UFDate Date = new UFDate(obj.getString("Date"));
		String OrderNo = obj.getString("SourceOrderNo");
		JSONArray item = obj.getJSONArray("items");

		GeneralInVO gvi = new GeneralInVO();

		// 根据OrderNo 查询NC 转库出库单
		GeneralOutVO gvo = getGeneralOutVO(OrderNo);

		if (gvo == null) {
			CommonUtil.putFailResult(para, "转库出库单号" + OrderNo + "查询失败");
			LoggerUtil.error("转库出库单号" + OrderNo + "查询失败");
		} else {
			try {
				// 检查转库出库单对应的 转库单 的累计出库主数量 是否为空，不为空则 表示该出库单已生成过转库入库单 不允许再次生成
	
				if (IsTurntoGenerIn(gvo)) {
					CommonUtil.putFailResult(para, "该转库出库单已经生成过转库入库单，不能再次生成！");
					LoggerUtil.debug("写入转库入库错误，  该转库出库单已经生成过转库入库单，不能再次生成！");
					return FreeMarkerUtil.process(para,
							"nc/config/ic/barcode/TransferInOrder.fl");
				}
	
				// 获取转库出库单表头
				GeneralOutHeadVO goHeadVO = gvo.getHead();
				if (goHeadVO != null) {
					InvocationInfoProxy.getInstance().setGroupId(
							goHeadVO.getPk_group());
					// 通过转库出库单表头生成转库入库单表头
					gvi.setParent(this.setGeneralInHeadVO(goHeadVO, Date));
				} else {
					CommonUtil.putFailResult(para, "转库出库单号" + OrderNo+ "找不到对应的单据！");
					LoggerUtil.error("转库出库单号" + OrderNo + "找不到对应的单据！");
					return FreeMarkerUtil.process(para,
							"nc/config/ic/barcode/TransferInOrder.fl");
				}
				
				List<GeneralInBodyVO> list = getGeneralInBodyVO(gvo, item, para);
				// 通过转库出库单获取表体
				if (list != null && list.size() != 0) {
					gvi.setChildrenVO(list.toArray(new GeneralInBodyVO[0]));
					IPFBusiAction pf = NCLocator.getInstance().lookup(
							IPFBusiAction.class);
					// InvocationInfoProxy.getInstance().setUserId(
					// gvi.getHead().getBillmaker());
					InvocationInfoProxy.getInstance().setGroupId(
							gvi.getHead().getPk_group());
					InvocationInfoProxy.getInstance().setBizDateTime(
							System.currentTimeMillis());
					
						GeneralInVO[] gvis = (GeneralInVO[]) pf.processAction(
								"WRITE", "4A", null, gvi, null, null);
						if (gvis.length != 0) {
							para.put("OrderNo", gvis[0].getHead().getVbillcode());
							CommonUtil.putSuccessResult(para);
						}
					
				} else {
					CommonUtil.putFailResult(para, "物料短号不能全部与转库出库单表体匹配  或者  "
							+ "转库出库单号" + OrderNo + "对应的表体单据为空");
				}
				
			} catch (BusinessException e) {
				CommonUtil.putFailResult(para, e.getMessage());
				e.printStackTrace();
				LoggerUtil.error("写入转库入库异常", e);
			} catch (Exception e) {
				CommonUtil.putFailResult(para, e.getMessage());
				e.printStackTrace();
				LoggerUtil.error("写入转库入库异常", e);
			}
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
		LoggerUtil.debug("写入转库入库结果 saveTransferIn_requireNew " + rst);
		return rst;
	}

	/**
	 * 判断转库出库单是否已经生成过转库入库单
	 * 
	 * @param gvo
	 *            转库出库单aggvo
	 * @return true 已生成 false 未生成
	 */
	private boolean IsTurntoGenerIn(GeneralOutVO gvo) {

		boolean flag = false;

		for (GeneralOutBodyVO body : gvo.getBodys()) {
			String id = body.getCsourcebillbid(); // 转库出库单来源表体id 即
			String sqlWhere = "nvl(dr,0) = 0 and csourcebillbid='" + id + "'";
			try {
				Object ntransinnum = new BaseDAO().executeQuery(
						"select ntransinnum from ic_whstrans_b where cspecialbid='"
								+ id + "' and dr = 0", new ColumnProcessor());
				Object nnum = new BaseDAO().executeQuery(
						"select nnum from ic_whstrans_b where cspecialbid='"
								+ id + "' and dr = 0", new ColumnProcessor());
				if (ntransinnum != null) {
					double ntnum = Double.parseDouble(ntransinnum.toString()); // 累计入库主数量
					if (ntnum == Double.parseDouble(nnum.toString())) {
						flag = true;
						break;
					}
				}
			} catch (DAOException e) {
				e.printStackTrace();
				LoggerUtil.error(e);
			}
		}
		return flag;
	}

	/**
	 * 通过转库出库单获取转库入库单表体
	 * 
	 * @param gvo
	 *            转库出库单 aggVO
	 * @return
	 */
	private List<GeneralInBodyVO> getGeneralInBodyVO(GeneralOutVO gvo,
			JSONArray item, HashMap<String, Object> para) throws BusinessException {

//		String errorCode = new String();

		GeneralOutBodyVO[] goBodys = gvo.getBodys();
		GeneralOutHeadVO gohead = gvo.getHead();
		List<GeneralInBodyVO> list = new ArrayList<GeneralInBodyVO>();
		
		
		for (int index = 0; index < item.size(); index++) {

			JSONObject bodyjson = item.getJSONObject(index);
			
			String SourceOrderLineNo = bodyjson.getString("SourceOrderLineNo");
			if(SourceOrderLineNo == null || SourceOrderLineNo.length() == 0){
				throw new BusinessException("行号为空");
			}
			
			boolean flag = false;
			for (GeneralOutBodyVO go : goBodys) {
				
				String crowno = go.getCrowno();
				
				if (SourceOrderLineNo.equals(crowno)) {
					flag = true;
					GeneralInBodyVO gi = new GeneralInBodyVO();
					gi.setPk_group(gohead.getPk_group()); // 集团

					gi.setCrowno(go.getCrowno() == null ? (index + 1) * 10 + ""
							: go.getCrowno()); // 行号
					gi.setCmaterialoid(go.getCmaterialoid()); // 物料
					gi.setCmaterialvid(go.getCmaterialvid()); // 物料编码
					// gi.setVbdef8(item.getJSONObject(index).getString(
					// "ProductCode")); // 物料短号
					gi.setCunitid(go.getCunitid()); // 主单位
					gi.setCastunitid(go.getCastunitid()); // 单位
					gi.setVchangerate(go.getVchangerate()); // 换算率
					gi.setNshouldassistnum(go.getNshouldassistnum()); // 应收数量
					gi.setNshouldnum(go.getNshouldnum()); // 应收主数量
					gi.setNassistnum(new UFDouble(item.getJSONObject(index)
							.getInt("ScanQty"))); // 实收数量
					gi.setNnum(gi.getNassistnum().multiply(getVchangerate(go.getVchangerate()))); // 实收主数量 与 应收主数量一致
					gi.setCbodywarehouseid(go.getCbodywarehouseid()); // 库存仓库
					gi.setNcostprice(go.getNcostprice()); // 单价
					gi.setNcostmny(go.getNcostmny()); // 金额
					gi.setDbizdate(new UFDate()); // 入库日期

					gi.setCbodytranstypecode("4A-02"); // 出入库类型pk
//					if (WsQueryBS.getWholemanaflag(go.getCmaterialoid(), go.getPk_org())) {
//						gi.setVbatchcode(item.getJSONObject(index).getString(
//								"BatchNo")); // 批次号
//						gi.setPk_batchcode(WsQueryBS.getPk_BatchCode(
//								go.getCmaterialoid(), item.getJSONObject(index)
//										.getString("BatchNo")));
//					} 
					gi.setVbatchcode(go.getVbatchcode());
					gi.setPk_batchcode(go.getPk_batchcode());
					gi.setDproducedate(go.getDproducedate()); // 生产日期
					gi.setDvalidate(go.getDvalidate()); // 失效日期
					// 批次号
//					if(gi.getVbatchcode() != null){
//						HashMap<String, String> batchMap = WsQueryBS.getBatchCode(gi.getCmaterialoid(), gi.getVbatchcode());
//						gi.setPk_batchcode(batchMap.get("pk_batchcode"));
//						gi.setDproducedate(batchMap.get("dproducedate") == null ? null : new UFDate(batchMap.get("dproducedate")));
//						gi.setDvalidate(batchMap.get("dvalidate") == null ? null : new UFDate(batchMap.get("dvalidate")));
//						gi.setDinbounddate(batchMap.get("dinbounddate") == null ? null : new UFDate(batchMap.get("dinbounddate")));
//					}
					gi.setVvendbatchcode(go.getVvendbatchcode()); // 供应商批次号

					gi.setCprojectid(go.getCprojectid()); // 项目
					gi.setCasscustid(go.getCasscustid()); // 客户

					// 来源信息
					gi.setVsourcerowno(go.getVsourcerowno());
					gi.setVsourcebillcode(go.getVsourcebillcode()); // 来源单据号
					gi.setCsourcebillhid(go.getCsourcebillhid()); // 来源单据主键
					gi.setCsourcebillbid(go.getCsourcebillbid()); // 来源单据表体主键
					gi.setCsourcetranstype(go.getCsourcetranstype()); // 来源单据出入库类型
					gi.setCsourcetype(go.getCsourcetype());

					gi.setNweight(go.getNweight());
					gi.setNvolume(go.getNvolume());
					gi.setStatus(VOStatus.NEW);

					list.add(gi);
					break;
				} // end if pk_material.equals(go.getCmaterialoid())
			} // end for go
			if (!flag) {
				throw new BusinessException("行号" + SourceOrderLineNo + "匹配不到对应的出库表体行");
			}
		}
//		// 有些物料短号匹配到的物料pk 在其他出库单的子表中不存在，即：条码系统传入参数有误！
//		if (count != item.size()) {
//			CommonUtil.putFailResult(para, "以下物料短号" + errorCode
//					+ "对应的物料pk匹配不到出库单子表对应物料信息！");
//			LoggerUtil.error("以下物料短号" + errorCode + "对应的物料pk匹配不到出库单子表对应物料信息！");
//			return null;
//		} 
		return list;
	}

	/**
	 * 计算换算率
	 * 
	 * @param vchangerate
	 *            换算率
	 * @return
	 */
	private double getVchangerate(String vchangerate) {

		String[] vcs = vchangerate.split("/");
		double vc = Double.parseDouble(vcs[0]) / Double.parseDouble(vcs[1]);
		return vc;
	}

	/**
	 * 通过转库出库单表头生成转库入库单表头
	 * 
	 * @param goHeadVO
	 *            转库出库单表头
	 * @return
	 */
	private GeneralInHeadVO setGeneralInHeadVO(GeneralOutHeadVO goHeadVO,
			UFDate date) {

		GeneralInHeadVO giHeadVO = new GeneralInHeadVO();

		giHeadVO.setPk_group(goHeadVO.getPk_group());
		giHeadVO.setVtrantypecode("4A-02");
		giHeadVO.setCtrantypeid(PfDataCache.getBillType("4A-02")
				.getPk_billtypeid()); // 单据类型pk (出入库类型)
		giHeadVO.setCdptid(null); // 部门
		giHeadVO.setCdptvid(null); // 部门信息

		giHeadVO.setNtotalnum(goHeadVO.getNtotalnum()); // 总数量

		// giHeadVO.setCreator("NC_USER0000000000000"); // 创建人
		giHeadVO.setCreationtime(new UFDateTime(System.currentTimeMillis())); // 创建日期
		// giHeadVO.setBillmaker("NC_USER0000000000000"); // 制单人
		// giHeadVO.setModifier("NC_USER0000000000000");
		// giHeadVO.setModifiedtime(new UFDateTime());
		giHeadVO.setDbilldate(date); // 单据日期
		giHeadVO.setDmakedate(new UFDate());
		giHeadVO.setVnote(goHeadVO.getVnote()); // 备注
		giHeadVO.setFbillflag(2); // 设置单据状态 2-自由

		giHeadVO.setPk_org(goHeadVO.getPk_org()); // 库存组织
		giHeadVO.setPk_org_v(goHeadVO.getPk_org_v()); // 库存组织版本
		giHeadVO.setCwarehouseid(goHeadVO.getCotherwhid()); // 仓库

		// 设置出库仓库
		giHeadVO.setCothercalbodyoid(goHeadVO.getCothercalbodyoid()); // 库存组织
		giHeadVO.setCothercalbodyvid(goHeadVO.getCothercalbodyvid()); // 出库库存组织版本
		giHeadVO.setStatus(VOStatus.NEW);
		return giHeadVO;
	}

	/**
	 * 通过转库出库单号 获取转库出库单aggVO
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

	
	/**
	 * 3.0 保存转库、其他出库和其他入库
	 * @param xml
	 * @return
	 */
	@Override
	public String saveTransferOutAndIn_requireNew(String xml) {
		LoggerUtil.debug("写入转库出库和入库 saveTransferOutAndIn_requireNew " + xml);
		HashMap<String, Object> para = new HashMap<String, Object>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String senderLocationCode = obj.getString("SenderLocationCode");
		String receiverLocationCode = obj.getString("ReceiverLocationCode");
		JSONArray items = obj.getJSONArray("items");

		try {
			if (senderLocationCode == null || receiverLocationCode == null) {
				throw new BusinessException(
						"SenderLocationCode和ReceiverLocationCode不能为空");
			}
			HashMap<String, String> outStoreMap = WsQueryBS
					.queryStordocByCode(senderLocationCode);
			HashMap<String, String> inStoreMap = WsQueryBS
					.queryStordocByCode(receiverLocationCode);
			if (outStoreMap.size() == 0) {
				throw new BusinessException("仓库对照找不到仓库编码" + senderLocationCode);
			}
			if (inStoreMap.size() == 0) {
				throw new BusinessException("仓库对照找不到仓库编码"
						+ receiverLocationCode);
			}
			if (!outStoreMap.get("pk_org").equals(inStoreMap.get("pk_org"))) {
				throw new BusinessException("转出跟转入仓库所属组织不一致");
			}

			WhsTransBillVO wtbillvo = new WhsTransBillVO();
			WhsTransBillHeaderVO wthvo = getWhsTransBillHeaderVO(obj,
					outStoreMap, inStoreMap);
			wtbillvo.setParent(wthvo);
			wtbillvo.setChildrenVO(getWhsTransBillBodysVO(wthvo, items));
			
			// 保存转库单
			IPFBusiAction pf = NCLocator.getInstance().lookup(
					IPFBusiAction.class);
			pf.processAction("WRITE", "4K", null, wtbillvo, null, null);
			LoggerUtil.error("转库保存 ");
			Object object = pf.processAction("APPROVE", "4K", null, wtbillvo, null, null);
			LoggerUtil.error("转库审核 ");
			
			//WhsTransBillVO wtrst = (WhsTransBillVO)ArrayUtil.getFirstInArrays((Object[])object);
			WhsTransBillVO wtrst = wtbillvo;
			
			// 转库单推其他出库
			IPfExchangeService exchangeService = NCLocator.getInstance()
					.lookup(IPfExchangeService.class);
			GeneralOutVO outvo = (GeneralOutVO) exchangeService.runChangeData(
					"4K", "4I", wtrst, null);
			LoggerUtil.error("转换为其他出库 ");
			GeneralOutHeadVO outheadvo = outvo.getHead();
			outheadvo.setVtrantypecode("4I-02");
			outheadvo.setCtrantypeid(PfDataCache.getBillType("4I-02")
					.getPk_billtypeid());
			GeneralOutBodyVO[] outbodys = outvo.getBodys();
			for (int i = 0; i < outbodys.length; i++) {
				GeneralOutBodyVO outbody = outbodys[i];
				outbody.setCrowno(String.valueOf((i + 1) * 10));
				outbody.setNassistnum(outbody.getNshouldassistnum());
				outbody.setNnum(outbody.getNshouldnum());
				outbody.setVtransfercode("4I-02");
				// 批次号
				if(outbody.getVbatchcode() != null){
					HashMap<String, String> batchMap = WsQueryBS.getBatchCode(outbody.getCmaterialoid(), outbody.getVbatchcode());
					outbody.setPk_batchcode(batchMap.get("pk_batchcode"));
					outbody.setDproducedate(batchMap.get("dproducedate") == null ? null : new UFDate(batchMap.get("dproducedate")));
					outbody.setDvalidate(batchMap.get("dvalidate") == null ? null : new UFDate(batchMap.get("dvalidate")));
					outbody.setDinbounddate(batchMap.get("dinbounddate") == null ? null : new UFDate(batchMap.get("dinbounddate")));
				}
			}
			pf.processAction("WRITE", "4I", null, outvo, null, null);
			LoggerUtil.error("其他出库保存 ");
			
			// 转库单推其他入库
			GeneralInVO invo = (GeneralInVO) exchangeService.runChangeData("4K", "4A", wtrst, null);
			LoggerUtil.error("转换为其他入库 ");
			GeneralInHeadVO inheadvo = invo.getHead();
			inheadvo.setVtrantypecode("4A-02");
			inheadvo.setCtrantypeid(PfDataCache.getBillType("4A-02")
					.getPk_billtypeid());
			GeneralInBodyVO[] inbodys = invo.getBodys();
			for (int i = 0; i < inbodys.length; i++) {
				GeneralInBodyVO inbody = inbodys[i];
				inbody.setCrowno(String.valueOf((i + 1) * 10));
//				inbody.setNassistnum(inbody.getNshouldassistnum());
//				inbody.setNnum(inbody.getNshouldnum());
				//实收数量改为不填，手工修改 20170722
				inbody.setNassistnum(null);
				inbody.setNnum(null);
				inbody.setVtransfercode("4A-02");
				// 批次号
				if(inbody.getVbatchcode() != null){
					HashMap<String, String> batchMap = WsQueryBS.getBatchCode(inbody.getCmaterialoid(), inbody.getVbatchcode());
					inbody.setPk_batchcode(batchMap.get("pk_batchcode"));
					inbody.setDproducedate(batchMap.get("dproducedate") == null ? null : new UFDate(batchMap.get("dproducedate")));
					inbody.setDvalidate(batchMap.get("dvalidate") == null ? null : new UFDate(batchMap.get("dvalidate")));
					inbody.setDinbounddate(batchMap.get("dinbounddate") == null ? null : new UFDate(batchMap.get("dinbounddate")));
				}
			}
			pf.processAction("WRITE", "4A", null, invo, null, null);
			LoggerUtil.error("其他入库保存 ");
			
			para.put("OrderNo", wtrst.getHead().getVbillcode());
			CommonUtil.putSuccessResult(para);
		} catch (BusinessException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "发生异常：" + e.getMessage());
			LoggerUtil.error("写入转库出库和入库异常", e);
		} catch (Exception e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "发生异常：" + e.getMessage());
			LoggerUtil.error("写入转库出库和入库异常", e);
		}
		String rst = FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/PostTransferOutAndInl.fl");
		LoggerUtil.debug("写入转库出库和入库结果 saveTransferOutAndIn_requireNew " + rst);
		return rst;
	} 

}
