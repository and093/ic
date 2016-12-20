package nc.bs.ic.warehousecontrast.bp;

import nc.impl.pubapp.pattern.data.vo.SchemeVOQuery;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pubapp.query2.sql.process.QuerySchemeProcessor;
import nc.vo.ic.warehousecontrast.warehouse_contrastVO;

public class WarehouseContrastBP {
  
  public warehouse_contrastVO[] queryByQueryScheme(IQueryScheme querySheme) {
    QuerySchemeProcessor p = new QuerySchemeProcessor(querySheme);
    p.appendFuncPermissionOrgSql();
    return new SchemeVOQuery<warehouse_contrastVO>(warehouse_contrastVO.class).query(
        querySheme, null);
  }

}
