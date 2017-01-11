package nc.impl.ic.barcode;

import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.ift.ic.barcode.IMaterialInfo;
import nc.jdbc.framework.processor.MapProcessor;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.pub.ic.barcode.LoggerUtil;

public class MaterialInfoImpl implements IMaterialInfo {

	@Override
	public String getMaterialInfo(String bccode) {
		LoggerUtil.debug("读取物料信息 getMaterialInfo - " + bccode);
		//通过条码的物料短码，查询物料信息
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		StringBuffer sql = new StringBuffer();
		sql.append(" select c.name marclassname, ")
			.append("        a.code, ")
			.append("        a.name, ")
			.append("        b.name measname, ")
			.append("        a.def16 materialspec   ")
			.append("  from bd_material a, bd_measdoc b, bd_marbasclass c ")
			.append(" where a.pk_measdoc = b.pk_measdoc ")
			.append(" and a.pk_marbasclass = c.pk_marbasclass ")
			.append(" and nvl(a.dr,0) = 0 ")
			.append(" and a.def8 = '"+bccode+"' ");
		try {
			Object rst = dao.executeQuery(sql.toString(), new MapProcessor());
			if(rst != null){
				para.putAll((HashMap)rst);
				CommonUtil.putSuccessResult(para); 
			} else {
				CommonUtil.putFailResult(para, "条码号" + bccode + "找不到对应的物料信息");
			}
		} catch (DAOException e) {
			LoggerUtil.error("读取物料异常 ", e);
			CommonUtil.putFailResult(para, "查询数据库失败：" + e.getMessage());
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/material.fl");
		LoggerUtil.debug("读取物料结果 getMaterialInfo : " + rst);
		return rst;
	}

}
