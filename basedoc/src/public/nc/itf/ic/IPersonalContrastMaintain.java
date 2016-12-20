package nc.itf.ic;

import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.ic.personalcontrast.personalContrastVO;
import nc.vo.pub.BusinessException;
import nc.itf.pubapp.pub.smart.ISmartService;

public interface IPersonalContrastMaintain extends ISmartService{
    public personalContrastVO[] query(IQueryScheme queryScheme)
      throws BusinessException, Exception;
}
