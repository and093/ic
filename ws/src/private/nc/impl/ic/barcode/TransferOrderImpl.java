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
	 * 2.9 写入转库入库单
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

		// 根据OrderNo 查询NC 转库出库单
		GeneralOutVO gvo = getGeneralOutVO(OrderNo);
		if (gvo == null) {
			CommonUtil.putFailResult(para, "转库出库单号" + OrderNo + "查询失败");
		} else {
			// 获取转库出库单表头
			GeneralOutHeadVO goHeadVO = gvo.getHead();
			if (goHeadVO != null) {
				// 通过转库出库单表头生成转库入库单表头
				gvi.setParent(this.setGeneralInHeadVO(goHeadVO, Date));
			} else {
				CommonUtil.putFailResult(para, "转库出库单号" + OrderNo
						+ "对应的表头单据为空！");
			}
			List<GeneralInBodyVO> list = setGeneralInBodyVO(gvo,item,para);
			// 通过转库出库单获取表体
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
				CommonUtil.putFailResult(para, "转库出库单号" + OrderNo
						+ "对应的表体单据为空！");
			}
		}
		return FreeMarkerUtil.process(para,
				"nc/config/ic/barcode/TransferInOrder.fl");
	}

	/**
	 * 通过转库出库单获取转库入库单表体
	 * 
	 * @param gvo
	 *            转库出库单 aggVO
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
					.getJSONObject(index).getString("ProductCode"));  //根据物料短号获取物料pk
			
			if(pk_material == null) {
				CommonUtil.putFailResult(para, "料号"+item.getJSONObject(index).getString("ProductCode")+"找不到对应的物料");
				return null;  //获取物料pk失败
			}
			
			boolean flag = false;
			for (GeneralOutBodyVO go : goBodys) {
				if(pk_material.equals(go.getCmaterialoid())){
					flag = true;
					GeneralInBodyVO gi = new GeneralInBodyVO();
					gi.setPk_group(gohead.getPk_group()); // 集团
	
					gi.setCrowno(go.getCrowno()); // 行号
					gi.setCmaterialoid(go.getCmaterialoid()); // 物料
					gi.setCmaterialvid(go.getCmaterialvid()); // 物料编码
					gi.setVbdef8(item.getJSONObject(index).getString("ProductCode")); // 物料短号
					gi.setCunitid(go.getCunitid()); // 主单位
					gi.setCastunitid(go.getCastunitid()); // 单位
					gi.setVchangerate(go.getVchangerate()); // 换算率
					gi.setNshouldassistnum(go.getNshouldassistnum()); // 应收数量
					gi.setNshouldnum(new UFDouble(go.getNshouldassistnum()
							.doubleValue() * getVchangerate(go.getVchangerate()))); // 应收主数量
																					// =
																					// 应收数量*换算率
					gi.setNassistnum(new UFDouble(item.getJSONObject(index).getInt(
							"ScanQty"))); // 实收数量
					gi.setNnum(gi.getNshouldnum()); // 实收主数量 与 应收主数量一直
					gi.setCbodywarehouseid(go.getCbodywarehouseid()); // 库存仓库
	
					gi.setNcostprice(go.getNcostprice()); // 单价
					gi.setNcostmny(go.getNcostmny()); // 金额
					gi.setDbizdate(new UFDate()); // 入库日期
					gi.setVbatchcode(go.getVbatchcode()); // 批次号
					gi.setDproducedate(go.getDproducedate()); // 生产日期
					gi.setVvendbatchcode(go.getVvendbatchcode()); // 供应商批次号
	
					gi.setCprojectid(go.getCprojectid()); // 项目
					gi.setCasscustid(go.getCasscustid()); // 客户
	
					// 来源信息
					gi.setCsourcebillhid(go.getCgeneralhid());
					gi.setCsourcebillbid(go.getCgeneralbid());
					gi.setVsourcebillcode(gohead.getVbillcode());
					gi.setVsourcerowno(go.getCrowno());
					gi.setCsourcetype("4A");
					gi.setCsourcetranstype(gohead.getCtrantypeid());
	
					// 其他来源
					gi.setCsrc2billhid(go.getCsrc2billhid());
					gi.setCsrc2billbid(go.getCsrc2billbid());
					gi.setVsrc2billcode(go.getVsrc2billcode());
					gi.setVsrc2billrowno(go.getVsrc2billrowno());
					// gi.setCsrc2billtype(go.get) 其他来源单据类型编码
					gi.setCsrc2transtype(go.getCsrc2transtype());
	
					// 源头信息
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
			CommonUtil.putFailResult(para, "以下物料短号找不到对应物料："+errorCode);
			return null;
		}
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
		BilltypeVO billTypeVO = PfDataCache.getBillTypeInfo("4A-02");
		giHeadVO.setCtrantypeid(billTypeVO.getPk_billtypeid()); // 单据类型pk
																// (出入库类型)
		giHeadVO.setCdptid(null); // 部门
		giHeadVO.setCdptvid(null); // 部门信息

		giHeadVO.setNtotalnum(goHeadVO.getNtotalnum()); // 总数量

		giHeadVO.setCreator("NC_USER0000000000000"); // 创建人
		giHeadVO.setCreationtime(new UFDateTime(System.currentTimeMillis())); // 创建日期
		giHeadVO.setBillmaker("NC_USER0000000000000"); // 制单人
		giHeadVO.setModifier("NC_USER0000000000000");
		giHeadVO.setModifiedtime(new UFDateTime());
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

}
