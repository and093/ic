package nc.itf.ic;

import nc.ui.querytemplate.querytree.IQueryScheme;
import nc.vo.ic.warehousecontrast.warehouse_contrastVO;
import nc.vo.pub.BusinessException;
import nc.itf.pubapp.pub.smart.ISmartService;

public interface IWarehouseContrastMaintain extends ISmartService{
    public warehouse_contrastVO[] query(IQueryScheme queryScheme)
      throws BusinessException, Exception;
}
