package nc.ui.ic.dpc.action;

import nc.ui.pubapp.uif2app.actions.batch.BatchAddLineAction;
import nc.vo.ic.dpc.dpContrastVO;

public class DpcAddLineAction extends BatchAddLineAction {

  private static final long serialVersionUID = 1L;
  
  @Override
  protected void setDefaultData(Object obj) {
    super.setDefaultData(obj);
    dpContrastVO baseDocVO = (dpContrastVO) obj;
    baseDocVO.setPk_group(this.getModel().getContext().getPk_group());
    baseDocVO.setPk_org(this.getModel().getContext().getPk_org());
  }


}
