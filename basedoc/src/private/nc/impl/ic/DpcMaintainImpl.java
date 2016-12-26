package nc.impl.ic;

import nc.impl.pub.ace.AceDpcPubServiceImpl;
import nc.bs.ic.dpc.ace.rule.DataUniqueCheckRule;
import nc.impl.pubapp.pub.smart.BatchSaveAction;
import nc.vo.bd.meta.BatchOperateVO;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pub.BusinessException;
import nc.vo.ic.dpc.dpContrastVO;

public class DpcMaintainImpl extends AceDpcPubServiceImpl implements nc.itf.ic.IDpcMaintain {

  @Override
  public dpContrastVO[] query(IQueryScheme queryScheme)
      throws BusinessException {
      return super.pubquerybasedoc(queryScheme);
  }


  @Override
  public BatchOperateVO batchSave(BatchOperateVO batchVO) throws BusinessException {
    BatchSaveAction<dpContrastVO> saveAction = new BatchSaveAction<dpContrastVO>();
    BatchOperateVO retData = saveAction.batchSave(batchVO);
    //调用编码、名称唯一性校验规则
    new DataUniqueCheckRule().process(new BatchOperateVO[] {
      batchVO    });
    return retData;
  }
}
