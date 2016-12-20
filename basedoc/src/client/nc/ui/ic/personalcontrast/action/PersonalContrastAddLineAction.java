package nc.ui.ic.personalcontrast.action;

import nc.ui.pubapp.uif2app.actions.batch.BatchAddLineAction;
import nc.vo.ic.personalcontrast.personalContrastVO;

public class PersonalContrastAddLineAction extends BatchAddLineAction {

  private static final long serialVersionUID = 1L;
  
  @Override
  protected void setDefaultData(Object obj) {
    super.setDefaultData(obj);
    personalContrastVO baseDocVO = (personalContrastVO) obj;
    baseDocVO.setPk_group(this.getModel().getContext().getPk_group());
    baseDocVO.setPk_org(this.getModel().getContext().getPk_org());
  }


}
