package nc.itf.ic;

import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.ic.dpc.dpContrastVO;
import nc.vo.pub.BusinessException;
import nc.itf.pubapp.pub.smart.ISmartService;

public interface IDpcMaintain extends ISmartService{
    public dpContrastVO[] query(IQueryScheme queryScheme)
      throws BusinessException, Exception;
}
