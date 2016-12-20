package nc.impl.ic;

import nc.impl.pub.ace.AceWarehouseContrastPubServiceImpl;
import nc.bs.ic.warehousecontrast.ace.rule.DataUniqueCheckRule;
import nc.impl.pubapp.pub.smart.BatchSaveAction;
import nc.vo.bd.meta.BatchOperateVO;
import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.pub.BusinessException;
import nc.vo.ic.warehousecontrast.warehouse_contrastVO;

public class WarehouseContrastMaintainImpl extends AceWarehouseContrastPubServiceImpl implements nc.itf.ic.IWarehouseContrastMaintain {

  @Override
  public warehouse_contrastVO[] query(IQueryScheme queryScheme)
      throws BusinessException {
      return super.pubquerybasedoc(queryScheme);
  }


  @Override
  public BatchOperateVO batchSave(BatchOperateVO batchVO) throws BusinessException {
    BatchSaveAction<warehouse_contrastVO> saveAction = new BatchSaveAction<warehouse_contrastVO>();
    BatchOperateVO retData = saveAction.batchSave(batchVO);
    //调用编码、名称唯一性校验规则
    new DataUniqueCheckRule().process(new BatchOperateVO[] {
      batchVO    });
    return retData;
  }
}
