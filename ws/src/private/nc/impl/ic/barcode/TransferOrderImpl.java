package nc.impl.ic.barcode;

import java.util.HashMap;
import java.util.List;

import nc.ift.ic.barcode.ITransferOrder;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.ic.m4a.entity.GeneralInHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutHeadVO;
import nc.vo.ic.m4i.entity.GeneralOutVO;
import nc.vo.pub.lang.UFDate;
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
		
		HashMap<String,Object> para = new HashMap<String,Object>();

		XMLSerializer xmls = new XMLSerializer();
		JSON json = xmls.read(xml);
		JSONObject obj = JSONObject.fromObject(json);

		UFDate Date = new UFDate(obj.getString("Date"));
		String OrderNo = obj.getString("OrderNo");
		JSONArray item = obj.getJSONArray("item");

		String ProductCode = item.getJSONObject(0).getString("ProductCode");
		String BatchNo = item.getJSONObject(0).getString("BatchNo");
		int ScanQty = item.getJSONObject(0).getInt("ScanQty");

		// 根据OrderNo 查询NC 转库出库单
		GeneralOutVO gvo = getGeneralOutVO(OrderNo);
		if(gvo == null) {
			CommonUtil.putFailResult(para, "查询失败");
		} else {
			//获取转库出库单表头
			GeneralOutHeadVO goHeadVO = gvo.getHead();
			
		}
		
		
		return FreeMarkerUtil.process(para, "nc/config/ic/barcode/TransferInOrder.fl");
	}
	
	private GeneralInHeadVO setGeneralInHeadVO(GeneralOutHeadVO goHeadVO){
		
		GeneralInHeadVO giHeadVO = new GeneralInHeadVO();
		
		return null;
	}
	
	private GeneralOutVO getGeneralOutVO(String OrderNo){
		
		String sqlWhere = "nvl(dr,0) = 0 and vbillcode='" + OrderNo + "'";
		try {
			List<GeneralOutVO> list = (List<GeneralOutVO>) MDPersistenceService
					.lookupPersistenceQueryService().queryBillOfVOByCond(
							GeneralOutVO.class, sqlWhere, true, false);
			if(list != null && list.size() != 0){
				return list.get(0);
			} 
		} catch (MetaDataException e) {
			e.printStackTrace();
		}
		return null;
	}

}
