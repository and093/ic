package nc.rule.ic.barcode;

import nc.bs.ic.pub.base.ICRule;
import nc.vo.ic.general.define.ICBillBodyVO;
import nc.vo.ic.general.define.ICBillVO;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;

public class BeforeDeleteRule<E extends ICBillVO> extends ICRule<E>{

	@Override
	public void process(ICBillVO[] e) {
		for(ICBillVO bill : e){
			 ICBillBodyVO[] bodys = bill.getBodys();
			 for(ICBillBodyVO body : bodys){
				 String scan = body.getVbdef20();
				 if(scan != null && !"~".equals(scan) && scan.length() > 0){
					 ExceptionUtils.wrappBusinessException("�����к�" + body.getCrowno() + "��ɨ�����������ݲ�����ɾ��");
				 }
			 }
		}
	}

}
