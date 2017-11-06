package nc.rule.ic.barcode;

import nc.impl.pubapp.pattern.rule.ICompareRule;
import nc.vo.ic.general.define.ICBillBodyVO;
import nc.vo.ic.general.define.ICBillVO;
import nc.vo.pub.VOStatus;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;

public class BeforeUpdateRule<E extends ICBillVO> implements ICompareRule<E> {

	@Override
	public void process(E[] vos, E[] originVOs) {
		for (ICBillVO bill : vos) {
			ICBillBodyVO[] bodys = bill.getBodys();
			for (ICBillBodyVO body : bodys) {
				String scan = body.getVbdef20();
				UFDouble nscanNum = null;
				if (scan != null && !"~".equals(scan) && scan.length() > 0 && !"0".equals(scan)) {
					nscanNum = new UFDouble(scan);
				}
				int status = body.getStatus();
				if (status == VOStatus.NEW || status == VOStatus.UPDATED) {
					UFDouble outNum = body.getNassistnum();
					if (outNum != null && nscanNum != null && outNum.compareTo(nscanNum) < 0) {
						ExceptionUtils.wrappBusinessException("表体行号" + body.getCrowno() + "实发数量不能少于扫码数量");
					}
				} else if (status == VOStatus.DELETED) {
					if (nscanNum != null) {
						ExceptionUtils.wrappBusinessException("表体行号" + body.getCrowno() + "有扫码数量，不允许删除");
					}
				}
			}
		}
	}

}
