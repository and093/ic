package nc.bs.ic.personalcontrast.bp;

import nc.impl.pubapp.pattern.data.vo.SchemeVOQuery;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pubapp.query2.sql.process.QuerySchemeProcessor;
import nc.vo.ic.personalcontrast.personalContrastVO;

public class PersonalContrastBP {
  
  public personalContrastVO[] queryByQueryScheme(IQueryScheme querySheme) {
    QuerySchemeProcessor p = new QuerySchemeProcessor(querySheme);
    p.appendFuncPermissionOrgSql();
    return new SchemeVOQuery<personalContrastVO>(personalContrastVO.class).query(
        querySheme, null);
  }

}
