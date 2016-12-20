package nc.impl.ic.barcode;

import java.util.HashMap;

import nc.ift.ic.barcode.IBarcodeWS;
import nc.pub.ic.barcode.FreeMarkerUtil;

public class BarcodeWSImpl implements IBarcodeWS {

	@Override
	public String GetProductInfoByCode(String productCode) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		para.put("code", productCode);
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/material.fl");
	}
	@Override
	public String GetProductionOrderByNo(String orderNo) {
		return null;
	}

	@Override
	public String PostGoodsReceiveNote(String xml) {
		return null;
	}

	@Override
	public String GetDeliveryNoteByNo(String transationType, String orderNo) {
		return null;
	}

	@Override
	public String PostDeliveryNoteDetailScanQty(String xml) {
		return null;
	}

	@Override
	public String PostOtherDeliveryNoteDetailActualQty(String xml) {
		return null;
	}

	@Override
	public String PostTransferReceiveNote(String xml) {
		return null;
	}

	@Override
	public String PostTransferOutNote(String xml) {
		return null;
	}

	@Override
	public String PostTransferInNote(String xml) {
		return null;
	}

}
