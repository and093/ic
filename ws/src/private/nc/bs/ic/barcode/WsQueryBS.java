package nc.bs.ic.barcode;

import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
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
						"select wc_name SenderLocationName ,wc_code SenderLocationCode from ic_warehouse_contrast where pk_stordoc = '"
								+ pk_stordoc + "'", new MapProcessor());

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
						"select bc_name WorkshopName ,bc_code WorkshopCode from ic_dpc where pk_dept = '"
								+ pk_deptid + "'", new MapProcessor());
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
	public static String queryPK_materialByProductCode(String ProductCode) throws DAOException {

		BaseDAO dao = new BaseDAO();
		Object rst = dao.executeQuery(
				"select pk_material from bd_material where def8='"+ProductCode+"'",new ColumnProcessor());
		return (String) rst; // ��ѯ�ɹ� ��������pk
	}
}
