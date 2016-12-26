package nc.impl.ic.barcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.ic.barcode.WsQueryBS;
import nc.ift.ic.barcode.IProductOrder;
import nc.pub.ic.barcode.CommonUtil;
import nc.pub.ic.barcode.FreeMarkerUtil;
import nc.vo.mmpac.pmo.pac0002.entity.PMOItemVO;

public class ProductOrderImpl implements IProductOrder {

	@Override
	public String getProductOrder(String batchcode) {
		HashMap<String, Object> para = new HashMap<String, Object>();
		BaseDAO dao = new BaseDAO();
		//�����������κŲ�ѯ����������ϸ��
		String where = " nvl(dr,0) = 0 and vbatchcode = '"+batchcode+"'";
		try {
			Collection<PMOItemVO> col = dao.retrieveByClause(PMOItemVO.class, where);
			if(col == null || col.size() == 0){
				CommonUtil.putFailResult(para, batchcode + "��ѯ������Ӧ��������������");
			} else {
				CommonUtil.putSuccessResult(para);
				ArrayList<HashMap<String, Object>> details = new ArrayList<HashMap<String, Object>>();
				for(PMOItemVO item : col){
					HashMap<String, Object> detail = new HashMap<String, Object>();
					detail.put("PlanProductionDate", item.getTplanstarttime().getDate().toString());
					detail.put("OrderStatus", item.getFitemstatus());
					detail.put("PlanPackQty", item.getNmmastnum());
					//���복���Ӧnc�Ĳ��ţ����ݲ��Ŷ��ձ�ת��
					detail.putAll(WsQueryBS.queryWorkShop(item.getCdeptid()));
					//������Ŀǰֱ��ȡnc�ģ�Ӧ�û���Ҫ������
					detail.putAll(WsQueryBS.queryWorkLine(item.getCwkid()));
					//��ȡ������Ϣ
					detail.putAll(WsQueryBS.queryMaterialInfoByPk(item.getCmaterialid()));
					//��ȡ���ϵ�λ������ȡ��������λ
					detail.put("ProductUM", WsQueryBS.queryUnitName(item.getCunitid()));
					detail.put("IsNeedToCheckProduct", "T"); //�����Ƿ���Ҫ����
					
					details.add(detail);
				}
				para.put("Details", details);
			}
		} catch (DAOException e) {
			e.printStackTrace();
			CommonUtil.putFailResult(para, "��ѯ���ݿ�ʧ�ܣ�" + e.getMessage());
		}
		return FreeMarkerUtil.process(para,"nc/config/ic/barcode/productionOrderl.fl");
	}

	@Override
	public String saveProductInbound_requireNew(String xml) {
		
		return null;
	}

}
