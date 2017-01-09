package nc.bs.ic.barcode;

import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.jdbc.framework.processor.MapProcessor;

/**
 * �ṩ��ws�ӿڵĹ�����ѯ��
 * 
 * @author Administrator
 * 
 */
public class WsQueryBS {

	/**
	 * ͨ������pk��ѯ������Ϣ
	 * 
	 * @param pk_material
	 * @return
	 * @throws DAOException
	 */

	public static HashMap<String, Object> queryMaterialInfoByPk(
			String pk_material) throws DAOException {
		// ͨ����������϶��룬��ѯ������Ϣ
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		StringBuffer sql = new StringBuffer();
		sql.append(" select c.name ProductCategoryName, ")
				.append("        a.code ProductNo, ")
				.append("        a.name ProductName, ")
				// .append("        b.name ProductUM, ")
				.append("        a.def16 PackSize,   ")
				.append("        a.def8 ProductCode,  ")
				.append("        a.def14 PalletPackSize,  ")
				.append("        case a.def11 when 'Y' then 1 else 0 end ProductionOrderType   ")
				.append("  from bd_material a, bd_measdoc b, bd_marbasclass c ")
				.append(" where a.pk_measdoc = b.pk_measdoc ")
				.append(" and a.pk_marbasclass = c.pk_marbasclass ")
				.append(" and nvl(a.dr,0) = 0 ")
				.append(" and a.pk_material = '" + pk_material + "' ");
		Object rst = dao.executeQuery(sql.toString(), new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * ���ݵ�λid�õ���λ����
	 * 
	 * @param unitid
	 * @return
	 * @throws DAOException
	 */
	public static String queryUnitName(String unitid) throws DAOException {
		BaseDAO dao = new BaseDAO();
		Object rst = dao.executeQuery(
				"select name from bd_measdoc where pk_measdoc = '" + unitid
						+ "'", new ColumnProcessor());
		if (rst != null) {
			return (String) rst;
		}
		return null;
	}

	/**
	 * ����pk��òֿ���ձ�ı�ź�����
	 * 
	 * @param cwarehouseid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryLocationInfoByPk(
			String pk_stordoc) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao
				.executeQuery(
						"select wc_name SenderLocationName ,wc_code SenderLocationCode from ic_warehouse_contrast where nvl(dr,0) = 0 and pk_stordoc = '"
								+ pk_stordoc + "'", new MapProcessor());

		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * ��������ϵͳ�Ĳֿ���룬����nc�ֿ�pk
	 * 
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, String> queryStordocByCode(String code)
			throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, String> para = new HashMap<String, String>();
		Object rst = dao
				.executeQuery(
						"select a.pk_org, b.pk_vid, pk_stordoc, b.pk_group from ic_warehouse_contrast a , org_orgs b where a.pk_org = b.pk_org and nvl(a.dr,0) = 0 and wc_code = '"
								+ code + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * ���ݲ���pk�����Ҷ��ձ�������복�����ͳ�������
	 * 
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryWorkShop(String pk_deptid)
			throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao
				.executeQuery(
						"select bc_name WorkshopName ,bc_code WorkshopCode from ic_dpc where nvl(dr,0) = 0 and pk_dept = '"
								+ pk_deptid + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * ��������ϵͳ�������ĳ������SenderLocationCode������nc��������
	 * 
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryDeptidByCode(String code,
			String pk_org) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		SQLParameter sqlpa = new SQLParameter();
		sqlpa.addParam(pk_org);
		sqlpa.addParam(code);
		Object rst = dao
				.executeQuery(
						"select b.pk_dept, b.pk_vid from ic_dpc a, org_dept b"
								+ " where nvl(a.dr,0) = 0 and a.pk_dept = b.pk_dept and a.pk_org = ? and a.bc_code = ?",
						sqlpa, new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * ��ѯ�����߱��������
	 * 
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryWorkLine(String pk_deptid)
			throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao
				.executeQuery(
						"select vwkcode ProductionLineCode ,vwkname ProductionLineName from bd_wk where cwkid = '"
								+ pk_deptid + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * ���ݶ������ҿͻ��������������
	 * 
	 * @param cwarehouseid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryCustomer(String ccustomerid)
			throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao.executeQuery(
				"select code, name from  bd_customer where pk_customer = '"
						+ ccustomerid + "'", new MapProcessor());

		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * �������϶̺Ż�ȡ����pk
	 * 
	 * @param ProductCode
	 *            ���϶̺�
	 * @return ��������pk string
	 * @throws DAOException
	 */
	public static String queryPK_materialByProductCode(String ProductCode)
			throws DAOException {

		BaseDAO dao = new BaseDAO();
		Object rst = dao.executeQuery(
				"select pk_material from bd_material where def8='"
						+ ProductCode + "' and nvl(dr,0)=0",
				new ColumnProcessor());
		return (String) rst; // ��ѯ�ɹ� ��������pk
	}

	/**
	 * �������ƻ��߱��룬�õ��û�id
	 * 
	 * @param sender
	 * @return
	 */
	public static String getUserid(String sender) {
		BaseDAO dao = new BaseDAO();
		try {
			Object rst = dao.executeQuery(
					"select cuserid from sm_user where nvl(dr,0) = 0 and user_name = '"
							+ sender + "'", new ColumnProcessor());
			if (rst != null) {
				return (String) rst;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ��������pk �� ���κŻ�ȡ ���ε�������
	 * 
	 * @param pk_material
	 * @param batchCode
	 * @return
	 */
	public static String getPk_BatchCode(String pk_material, String batchCode) {

		Object obj = null;

		try {
			obj = new BaseDAO().executeQuery(
					"select pk_batchcode from scm_batchcode where cmaterialoid ='"
							+ pk_material + "' and vbatchcode='" + batchCode
							+ "'", new ColumnProcessor());
			if(obj != null) return obj.toString();
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * ��������pk �� �����֯ �жϸ������Ƿ��������κ�
	 * 
	 * @param pk_material
	 * @param pk_org
	 * @return true ����
	 */
	public static boolean getWholemanaflag(String pk_material, String pk_org) {

		try {
			Object wholemanaflag = new BaseDAO().executeQuery(
					"select wholemanaflag from bd_materialstock where pk_material='"
							+ pk_material + "' and pk_org='" + pk_org
							+ "' and nvl(dr,0)=0", new ColumnProcessor());
			if (wholemanaflag != null) {
				return "Y".equals(wholemanaflag.toString()) ? true : false;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * ��������̺ţ���ѯ����pk������������λ
	 * @param productCode
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, String> queryMaterialInfoByCode(String productCode) throws DAOException {
		// ͨ����������϶��룬��ѯ������Ϣ����Ҫ������pk������������λ
		BaseDAO dao = new BaseDAO();
		HashMap<String, String> para = new HashMap<String, String>();
		StringBuffer sql = new StringBuffer();
		sql.append(" select a.pk_material, a.pk_measdoc cunitid, b.pk_measdoc castunitid, b.measrate  ")
			.append("  from bd_material a, bd_materialconvert b, bd_measdoc c ")
			.append(" where a.pk_material = b.pk_material  ")
			.append(" and b.pk_measdoc = c.pk_measdoc ")
			.append(" and a.def8 = '"+productCode+"' ")
			.append(" and c.name = '��' ")
			.append(" and nvl(a.dr,0) = 0 ");
		Object rst = dao.executeQuery(sql.toString(), new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}
}
