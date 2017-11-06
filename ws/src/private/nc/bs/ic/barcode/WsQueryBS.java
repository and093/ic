package nc.bs.ic.barcode;

import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.jdbc.framework.SQLParameter;
import nc.jdbc.framework.processor.ColumnProcessor;
import nc.jdbc.framework.processor.MapProcessor;
import nc.pub.ic.barcode.LoggerUtil;

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
				.append("        a.name ProductName, ")//
				// .append("        b.name ProductUM, ")
				.append("        case a.def10 when 'Y' then 'T' else 'F' end ischeck,  ")   //是否需要校对料号
				.append("        substr(d.measrate,0,INSTR(d.measrate, '/') - 1) PackSize,   ")
				.append("        a.def8 ProductCode,  ")
				.append("        a.def14 PalletPackSize,  ")
				.append("        case a.def11 when 'Y' then 1 else 0 end ProductionOrderType   ")
				.append("  from bd_material a, bd_measdoc b, bd_marbasclass c , bd_materialconvert d ")
				.append(" where a.pk_measdoc = b.pk_measdoc ")
				.append(" and a.pk_marbasclass = c.pk_marbasclass ")
				.append(" and a.pk_material = d.pk_material ")
				.append(" and nvl(a.dr,0) = 0 ")
				.append(" and nvl(d.isprodmeasdoc,'N') = 'Y' and nvl(d.dr,0) = 0 ")
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
						"select a.pk_org, b.pk_vid, pk_stordoc, b.pk_group from ic_warehouse_contrast a , org_orgs b where a.pk_org = b.pk_org and nvl(a.dr,0) = 0 and wc_code = '"
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
		Object rst = dao.executeQuery(
				"select code, name from  bd_customer where pk_customer = '"
						+ ccustomerid + "'", new MapProcessor());

		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}

	/**
	 * 根据物料短号获取物料pk
	 * 
	 * @param ProductCode
	 *            物料短号
	 * @return 返回物料pk string
	 * @throws DAOException
	 */
	public static String queryPK_materialByProductCode(String ProductCode)
			throws DAOException {

		BaseDAO dao = new BaseDAO();
		Object rst = dao.executeQuery(
				"select pk_material from bd_material where def8='"
						+ ProductCode + "' and nvl(dr,0)=0",
				new ColumnProcessor());
		return (String) rst; // 查询成功 返回物料pk
	}

	/**
	 * 根据名称或者编码，得到用户id
	 * 
	 * @param sender
	 * @return
	 */
	public static String getUseridByCode(String code) {
		BaseDAO dao = new BaseDAO();
		try {
			Object rst = dao.executeQuery(
					"select cuserid from sm_user where nvl(dr,0) = 0 and user_code = '"
							+ code + "'", new ColumnProcessor());
			if (rst != null) {
				return (String) rst;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据物料pk 和 批次号获取 批次档案主键
	 * 
	 * @param pk_material
	 * @param batchCode
	 * @return
	 */
	public static String getPk_BatchCode(String pk_material, String batchCode) {

		Object obj = null;

		try {
			obj = new BaseDAO().executeQuery(
					"select pk_batchcode from scm_batchcode where nvl(dr,0) = 0 and cmaterialoid ='"
							+ pk_material + "' and vbatchcode='" + batchCode
							+ "'", new ColumnProcessor());
			if(obj != null) return obj.toString();
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 根据物料pk 和 批次号获取 批次档案主键、生产日期、失效日期
	 * @param pk_deptid
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, String> getBatchCode(String pk_material, String batchCode) throws DAOException {
		BaseDAO dao = new BaseDAO();
		HashMap<String, String> para = new HashMap<String, String>();
		Object rst = dao
				.executeQuery(
						"select pk_batchcode,dproducedate, dvalidate, dinbounddate from scm_batchcode where nvl(dr,0) = 0 and cmaterialoid ='"
								+ pk_material + "' and vbatchcode='" + batchCode
								+ "'", new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);
		}
		return para;
	}
	
	/**
	 * 根据物料pk 和 库存组织 判断该物料是否启用批次号
	 * 
	 * @param pk_material
	 * @param pk_org
	 * @return true 启用
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
	 * 根据条码短号，查询物料pk和物料主辅单位
	 * @param productCode
	 * @return
	 * @throws DAOException
	 */
	public static HashMap<String, String> queryMaterialInfoByCode(String productCode) throws DAOException {
		// 通过条码的物料短码，查询物料信息，主要是物料pk和物料主辅单位
		BaseDAO dao = new BaseDAO();
		HashMap<String, String> para = new HashMap<String, String>();
		StringBuffer sql = new StringBuffer();
		sql.append(" select a.pk_material, a.pk_measdoc cunitid, b.pk_measdoc castunitid, b.measrate  ")
			.append("  from bd_material a, bd_materialconvert b, bd_measdoc c ")
			.append(" where a.pk_material = b.pk_material  ")
			.append(" and b.pk_measdoc = c.pk_measdoc ")
			.append(" and a.def8 = '"+productCode+"' ")
			.append(" and nvl(b.isstockmeasdoc,'N') = 'Y' ")
			.append(" and nvl(a.dr,0) = 0 ");
		LoggerUtil.error("queryMaterialInfoByCode：" + sql.toString());
		Object rst = dao.executeQuery(sql.toString(), new MapProcessor());
		if (rst != null) {
			para.putAll((HashMap) rst);  
		}
		return para; 
	}
	
	/**
	 * 查询物料所在组织的保质期
	 * @param pk_org
	 * @param pk_material
	 * @return
	 */ 
	public static Integer queryQualitynum(String pk_org, String pk_material){
		
		try {
			Object obj = new BaseDAO().executeQuery(
					"select qualitynum  from bd_materialstock where nvl(dr,0) = 0 and pk_material = '"+pk_material+"' and pk_org = '"+pk_org+"'", new ColumnProcessor());
			if(obj != null) {
				return (Integer)obj;
			}
		} catch (DAOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private static HashMap<String, String> PM_CACHE = new HashMap<String, String>();
	/**
	 * 根据生产方式编码得到生产方式pk
	 */
	public static String getProductModelByCode(String code){
		String pk_defdoc = PM_CACHE.get(code);
		if(pk_defdoc == null){
			Object obj = null;
			try {
				StringBuffer sql = new StringBuffer();
				sql.append(" select pk_defdoc ")
				.append("   from bd_defdoc ")
				.append("  where code = '"+code+"' ")
				.append("    and nvl(dr,0) = 0 and pk_defdoclist = (select pk_defdoclist ")
				.append("                           from bd_defdoclist ")
				.append("                          where code = 'hm017' ")
				.append("                            and nvl(dr, 0) = 0 ) ");
				obj = new BaseDAO().executeQuery(sql.toString(), new ColumnProcessor());
				if(obj != null) {
					pk_defdoc = obj.toString();
				}
				if(pk_defdoc != null){
					PM_CACHE.put(code, pk_defdoc);
				}
			} catch (DAOException e) {
				e.printStackTrace();
			}
		}
		return pk_defdoc;
	}
}
