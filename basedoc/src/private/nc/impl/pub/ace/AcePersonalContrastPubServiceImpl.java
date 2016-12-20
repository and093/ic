package nc.impl.pub.ace;

import nc.bs.ic.personalcontrast.bp.PersonalContrastBP;
import nc.impl.pubapp.pub.smart.SmartServiceImpl;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pub.ISuperVO;
import nc.vo.ic.personalcontrast.personalContrastVO;
import nc.vo.uif2.LoginContext;
import nc.vo.pub.BusinessException;

public abstract class AcePersonalContrastPubServiceImpl extends SmartServiceImpl {
 public personalContrastVO[] pubquerybasedoc(IQueryScheme querySheme)throws nc.vo.pub.BusinessException {
   return new PersonalContrastBP().queryByQueryScheme(querySheme);
 } 
 
 @Override
  public ISuperVO[] queryByDataVisibilitySetting(LoginContext context,
      Class<? extends ISuperVO> clz) throws BusinessException {
    return super.queryByDataVisibilitySetting(context, clz);
  }
  @Override
  public ISuperVO[] selectByWhereSql(String whereSql,
      Class<? extends ISuperVO> clz) throws BusinessException {
    return super.selectByWhereSql(whereSql, clz);
  }
 
}