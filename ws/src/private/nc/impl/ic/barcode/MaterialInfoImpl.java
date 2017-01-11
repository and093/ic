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
		LoggerUtil.debug("��ȡ������Ϣ getMaterialInfo - " + bccode);
		//ͨ����������϶��룬��ѯ������Ϣ
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
				CommonUtil.putFailResult(para, "�����" + bccode + "�Ҳ�����Ӧ��������Ϣ");
			}
		} catch (DAOException e) {
			LoggerUtil.error("��ȡ�����쳣 ", e);
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
		}
		String rst = FreeMarkerUtil.process(para,"nc/config/ic/barcode/material.fl");
		LoggerUtil.debug("��ȡ���Ͻ�� getMaterialInfo : " + rst);
		return rst;
	}

}
