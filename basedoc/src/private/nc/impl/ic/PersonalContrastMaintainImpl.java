package nc.impl.ic;

import nc.impl.pub.ace.AcePersonalContrastPubServiceImpl;
import nc.bs.ic.personalcontrast.ace.rule.DataUniqueCheckRule;
import nc.impl.pubapp.pub.smart.BatchSaveAction;
import nc.vo.bd.meta.BatchOperateVO;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pub.BusinessException;
import nc.vo.ic.personalcontrast.personalContrastVO;

public class PersonalContrastMaintainImpl extends AcePersonalContrastPubServiceImpl implements nc.itf.ic.IPersonalContrastMaintain {

  @Override
  public personalContrastVO[] query(IQueryScheme queryScheme)
      throws BusinessException {
      return super.pubquerybasedoc(queryScheme);
  }


  @Override
  public BatchOperateVO batchSave(BatchOperateVO batchVO) throws BusinessException {
    BatchSaveAction<personalContrastVO> saveAction = new BatchSaveAction<personalContrastVO>();
    BatchOperateVO retData = saveAction.batchSave(batchVO);
    //调用编码、名称唯一性校验规则
    new DataUniqueCheckRule().process(new BatchOperateVO[] {
      batchVO    });
    return retData;
  }
}
