package nc.bs.ic.barcode;

import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
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
	public static HashMap<String, Object> queryMaterialInfoByPk(String pk_material) throws DAOException {
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
				.append(" and a.pk_measdoc = '" + pk_material + "' ");
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
		Object rst = dao.executeQuery("select name from bd_measdoc where pk_measdoc = '" + unitid + "'", new ColumnProcessor());
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
	public static HashMap<String, Object> queryLocationInfoByPk(String pk_stordoc) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao.executeQuery(
				"select wc_name SenderLocationName ,wc_code SenderLocationCode from ic_warehouse_contrast where pk_stordoc = '"
						+ pk_stordoc + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * 根据部门pk，查找对照表定义的条码车间编码和车间名称
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryWorkShop(String pk_deptid) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao.executeQuery(
				"select wc_name WorkshopName ,wc_code WorkshopCode from 部门对照表  where pk_deptid = '"
						+ pk_deptid + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}
	
	/**
	 * 查询生产线编码和名称
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, Object> queryWorkLine(String pk_deptid) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst = dao.executeQuery(
				"select vwkcode ProductionLineCode ,vwkname ProductionLineName from bd_wk where cwkid = '"
						+ pk_deptid + "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}
}
