package nc.bs.ic.barcode;

import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.jdbc.framework.processor.MapProcessor;

/**
 * 提供给ws接口的公共查询类
 * 
 * @author Administrator
 * 
 */
public class WsQueryBS {

	/**
	 * 通过物料pk查询物料信息
	 * 
	 * @param pk_material
	 * @return
	 * @throws DAOException
	 */

	public static HashMap<String, Object> queryMaterialInfoByPk(
			String pk_material) throws DAOException {
		// 通过条码的物料短码，查询物料信息
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
	 * 根据单位id得到单位名称
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
	 * 根据pk获得仓库对照表的编号和名称
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
	 * 根据条码系统的仓库编码，查找nc仓库pk
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
						"select pk_org, pk_stordoc from ic_warehouse_contrast where nvl(dr,0) = 0 and wc_code = '"
								+ code + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * 根据部门pk，查找对照表定义的条码车间编码和车间名称
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
	 * 根据条码系统传过来的车间编码SenderLocationCode，查找nc部门主键
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryDeptidByCode(String code, String pk_org) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		SQLParameter sqlpa = new SQLParameter();
		sqlpa.addParam(pk_org);
		sqlpa.addParam(code);
		Object rst = dao
				.executeQuery(
						"select b.pk_dept, b.pk_vid from ic_dpc a, org_dept b" +
						" where nvl(a.dr,0) = 0 and a.pk_dept = b.pk_dept and a.pk_org = ? and a.bc_code = ?", sqlpa, new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * 查询生产线编码和名称
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
	 * 根据订单查找客户档案名称与编码
	 * 
	 * @param cwarehouseid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryCustomer(String ccustomerid)
			throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao
				.executeQuery(
						"select code, name from  bd_customer where pk_customer = '"
								+ ccustomerid + "'", new MapProcessor());

		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * 根据物料短号获取物料pk
	 * @param ProductCode 物料短号
	 * @return 返回物料pk string
	 */
	public static String queryPK_materialByProductCode(String ProductCode){
		
		BaseDAO dao = new BaseDAO();
		try {
			Object rst = dao.executeQuery("select pk_material from bd_material where def8='"+ProductCode+"'",  new ColumnProcessor());
			return (String)rst;  // 查询成功 返回物料pk
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 根据名称或者编码，得到用户id
	 * @param sender
	 * @return
	 */
	public static String getUserid(String sender){
		BaseDAO dao = new BaseDAO();
		try {
			Object rst = dao.executeQuery("select cuserid from sm_user where nvl(dr,0) = 0 and user_name = '"+sender+"'",  new ColumnProcessor());
			if(rst != null){
				return (String)rst;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
