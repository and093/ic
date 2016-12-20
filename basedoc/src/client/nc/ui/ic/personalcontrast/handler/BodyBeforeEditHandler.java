package nc.ui.ic.personalcontrast.handler;

import nc.ui.pub.beans.UIRefPane;
import nc.ui.pubapp.uif2app.event.IAppEventHandler;
import nc.ui.pubapp.uif2app.event.card.CardBodyBeforeEditEvent;

public class BodyBeforeEditHandler implements IAppEventHandler<CardBodyBeforeEditEvent> {

  @Override
  public void handleAppEvent(CardBodyBeforeEditEvent e) {
	  String key = e.getKey();
		if ("pk_psndon".equals(key)) {
			Object org = e.getBillCardPanel().getBillModel()
					.getValueAt(e.getRow(), "pk_org_ID");
			UIRefPane refpane = (UIRefPane) e.getBillCardPanel()
					.getBodyItem("pk_psndon").getComponent();
			if (org != null) {
				refpane.getRefModel().setPk_org((String) org);
				refpane.setMultiOrgSelected(true);
			} else {
				refpane.getRefModel().setPk_org(null);
			}
		}

  e.setReturnValue(Boolean.TRUE);
}
  }

