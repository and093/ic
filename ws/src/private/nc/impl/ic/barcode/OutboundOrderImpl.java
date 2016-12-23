package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.ReadProductOrder;
import nc.bs.ic.barcode.WsQueryBS;
import nc.ift.ic.barcode.IOutboundOrder;
import nc.md.model.MetaDataException;
import nc.md.persist.framework.MDPersistenceService;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.ic.m4c.entity.SaleOutBodyVO;
import nc.vo.ic.m4c.entity.SaleOutHeadVO;
import nc.vo.ic.m4c.entity.SaleOutVO;
import nc.vo.pub.AggregatedValueObject;

public class OutboundOrderImpl implements IOutboundOrder {

	@Override
	public String getOutboundOrder(String transationType, String orderNo) {
		ReadProductOrder readproductorder = new ReadProductOrder();
		if ("4C".equals(transationType)) {
			readproductorder.RaadSaleOrder(orderNo);
		} else if ("4Y".equals(transationType)) {
			readproductorder.RaadTransOutOrder(orderNo);
		} else if ("4A".equals(transationType)) {
			readproductorder.RaadGeneralOutOrder(orderNo);
		}
		return null;
	}

	@Override
	public String saveOutboundBarcodeNum_requireNew(String xml) {
		return null;
	}

	@Override
	public String saveOuntboundOutNum_requireNew(String xml) {
		// TODO Auto-generated method stub
		return null;
	}

}
