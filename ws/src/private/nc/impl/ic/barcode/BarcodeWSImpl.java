package nc.impl.ic.barcode;

import java.util.HashMap;

import nc.ift.ic.barcode.IBarcodeWS;
import nc.pub.ic.barcode.FreeMarkerUtil;

public class BarcodeWSImpl implements IBarcodeWS {

	@Override
	public String getProductInfoByCode(String code) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		para.put("code", code);
		return FreeMarkerUtil.process(para, "nc/config/ic/barcode/material.fl");
	}

}
