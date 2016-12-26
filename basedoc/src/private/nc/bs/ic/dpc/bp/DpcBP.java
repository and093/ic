package nc.bs.ic.dpc.bp;

import nc.impl.pubapp.pattern.data.vo.SchemeVOQuery;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pubapp.query2.sql.process.QuerySchemeProcessor;
import nc.vo.ic.dpc.dpContrastVO;

public class DpcBP {
  
  public dpContrastVO[] queryByQueryScheme(IQueryScheme querySheme) {
    QuerySchemeProcessor p = new QuerySchemeProcessor(querySheme);
    p.appendFuncPermissionOrgSql();
    return new SchemeVOQuery<dpContrastVO>(dpContrastVO.class).query(
        querySheme, null);
  }

}
