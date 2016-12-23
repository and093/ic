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
	public static HashMap<String, Object> queryMaterialInfoByPk(String pk_material)
			throws DAOException {
		// 通过条码的物料短码，查询物料信息
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		StringBuffer sql = new StringBuffer();
		sql.append(" select c.name ProductCategoryName, ")
				.append("        a.code ProductNo, ")
				.append("        a.name ProductName, ")
//				.append("        b.name ProductUM, ")
				.append("        a.def16 PackSize,   ")
				.append("        a.def8 ProductCode   ")
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
	 * @param unitid
	 * @return
	 * @throws DAOException 
	 */
	public static String queryUnitName(String unitid) throws DAOException{
		BaseDAO dao = new BaseDAO();
		Object rst =dao.executeQuery("select name from bd_measdoc where pk_measdoc = '"+unitid+"'",
				new ColumnProcessor());
		if(rst != null){
			return (String)rst;
		}
		return null;
	}
	/**
	 * 根据pk获得编号和名称
	 * @param cwarehouseid
	 * @return
	 * @throws DAOException 
	 */
	public static HashMap<String, Object> queryLocationInfoByPk(String pk_stordoc) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, Object> para = new HashMap<String, Object>();
		Object rst =dao.executeQuery("select wc_name SenderLocationName ,wc_code SenderLocationCode from ic_warehouse_contrast where pk_stordoc = '"+pk_stordoc+"'",
				new ColumnProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	
	
	}

