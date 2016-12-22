package nc.impl.ic.barcode;

import java.util.HashMap;
import java.util.List;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.ic.m4c.entity.SaleOutBodyVO;
import nc.vo.ic.m4c.entity.SaleOutHeadVO;
import nc.vo.ic.m4c.entity.SaleOutVO;
import nc.vo.pub.AggregatedValueObject;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

public class OutboundOrderImpl implements IOutboundOrder {

	@Override
	public String getOutboundOrder(String transationType, String orderNo) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 回写出库单的扫描数量
	 */
	@Override
	public String saveOutboundBarcodeNum_requireNew(String xml) {


		XMLSerializer xmlS = new XMLSerializer();
		JSON json = xmlS.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		String TransationType = obj.getString("TransationType");
		String OrderNo = obj.getString("OrderNo");
		String LineNo = obj.getString("LineNo");
		int UpdateType = obj.getInt("UpdateType");
		int ScanQty = obj.getInt("ScanQty");

		if("4C".equals(TransationType)){
			return writeSaleOrderBound(OrderNo,LineNo,UpdateType,ScanQty);
		} else if("4A".equals(TransationType)) {
			return null;
		} else if("4Y".equals(TransationType)) {
			return null;
		} else {
			HashMap<String,Object> para = new HashMap<String,Object>();
			CommonUtil.putFailResult(para, "没有与交易类型"+TransationType+"对应的出库业务");
			return FreeMarkerUtil.process(para, "nc/config/ic/barcode/SaleOut.fl");
		}
	}
	
	private String writeSaleOrderBound(String OrderNo, String LineNo,
			int UpdateType, int ScanQty) {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";

		try {
			// false-未找到单号对应行号的数据
			boolean flag = false;

			List<SaleOutVO> list = (List<SaleOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							SaleOutVO.class, sqlWhere, true, false);
			// 未查询到单号为 OrderNo 的销售出库单
			if (list.size() == 0) {
				CommonUtil.putFailResult(para, "销售出库单号" + OrderNo
						+ "找不到对应的销售出库单");
			} else {
				SaleOutHeadVO headVO = (SaleOutHeadVO) list.get(0).getHead();
				// 获取单据状态 1-删除 2-自由 3-签字
				if (headVO.getFbillflag() == 2) {
					for (SaleOutBodyVO body : list.get(0).getBodys()) {
						if (LineNo.equals(body.getCrowno())) {
							if (UpdateType == 1) {
								int num = Integer.parseInt(body.getVbdef20())+ScanQty;
								body.setVbdef20(new String(num+""));
								CommonUtil.putSuccessResult(para);
							} else if (UpdateType == 2) {
								body.setVbdef20(new String(ScanQty+""));
								CommonUtil.putSuccessResult(para);
							} else {
								CommonUtil.putFailResult(para, "更新类型有误！");
							}
							flag = true;
							dao.updateVO(body);
						}
					}
					// 没有找到对应行号的销售出库单子表
					if (!flag) {
						CommonUtil.putFailResult(para, "销售出库单号 " + OrderNo
								+ " 找不到对应行号为：" + LineNo + "的子表");
					}
				} else {
					CommonUtil.putFailResult(para, "该销售单为非自由状态，不可修改");
				}
			}
		} catch (MetaDataException e) {
			CommonUtil.putFailResult(para, "查询数据库失败" + e.getMessage());
			e.printStackTrace();
		} catch (DAOException e) {
			CommonUtil.putFailResult(para, "写入数据库失败" + e.getMessage());
			e.printStackTrace();
		}
		return FreeMarkerUtil.process(para, "nc/config/ic/barcode/SaleOut.fl");
	}

	@Override
	public String saveOuntboundOutNum_requireNew(String xml) {
		// TODO Auto-generated method stub
		return null;
	}

}
