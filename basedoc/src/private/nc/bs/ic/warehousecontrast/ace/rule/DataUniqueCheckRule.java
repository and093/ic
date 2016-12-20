package nc.bs.ic.warehousecontrast.ace.rule;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import nc.impl.pubapp.pattern.data.vo.VOQuery;
import nc.impl.pubapp.pattern.database.DataAccessUtils;
import nc.impl.pubapp.pattern.pub.LockOperator;
import nc.impl.pubapp.pattern.rule.IRule;
import nc.vo.bd.meta.BatchOperateVO;
import nc.vo.pubapp.pattern.data.IRowSet;
import nc.vo.pubapp.pattern.exception.ExceptionUtils;
import nc.vo.ic.warehousecontrast.warehouse_contrastVO;

public class DataUniqueCheckRule implements IRule<BatchOperateVO>{

  @Override
  public void process(BatchOperateVO[] vos) {
    if (vos == null || vos.length == 0) {
      return;
    }
    Object[] oadd = vos[0].getAddObjs();
    Object[] oupd = vos[0].getUpdObjs();
    // 如果没有新增和修改的数据，则不需要校验
    warehouse_contrastVO[] vosadd = null;
    if (oadd != null && oadd.length > 0) {
      vosadd = this.convertArrayType(oadd);
      this.checkDBUnique(vosadd);
      // return;
    }
    warehouse_contrastVO[] vosupd = null;
    if (oupd != null && oupd.length > 0) {
      vosupd = this.convertArrayType(oupd);
      this.checkDBUnique(vosupd);
      // return;
    }
  }
  
  public void checkDBUnique(warehouse_contrastVO[] bills) {
    if (bills == null || bills.length == 0) {
      return;
    }
    for (int j = 0; j < bills.length; j++) {
      warehouse_contrastVO vo = bills[j];
      // String[][] voCheckedColumnsArray = vo.getCheckedColumnsArray();
      warehouse_contrastVO[] dbvo =
          new VOQuery<warehouse_contrastVO>(warehouse_contrastVO.class).query(new String[] {
            vo.getPrimaryKey()
          });
      this.doLock(dbvo);
      IRowSet rowSet = new DataAccessUtils().query(this.getCheckSql(dbvo[0]));
      if (rowSet.size() > 1) {
        ExceptionUtils.wrappBusinessException("保存失败，当前所新增或修改的信息在该集团已经存在编码或名称相同的记录。");
      }
    }
  }



  private warehouse_contrastVO[] convertArrayType(Object[] vos) {
    warehouse_contrastVO[] smartVOs =
        (warehouse_contrastVO[]) Array.newInstance(warehouse_contrastVO.class, vos.length);
    System.arraycopy(vos, 0, smartVOs, 0, vos.length);
    return smartVOs;
  }

  private void doLock(warehouse_contrastVO[] bills) {
    List<String> lockobj = new ArrayList<String>();
    for (int i = 0; i < bills.length; i++) {
      lockobj.add("#code_name#");
    }
    LockOperator lock = new LockOperator();
    lock.lock(lockobj.toArray(new String[lockobj.size()]),
        "当前单据记录有其他用户在操作，请稍候刷新后再操作");
  }

  /**
   * 拼接唯一性校验的sql
   * 
   * @param bill
   * @return
   */
  private String getCheckSql(warehouse_contrastVO vo) {
    StringBuffer sql = new StringBuffer();
    sql.append("select wc_code,wc_name ");
    sql.append("  from ");
    sql.append(vo.getTableName());

    sql.append(" where dr=0 ");
    sql.append(" and ");
    //sql.append(" where ");显示
    sql.append(" (wc_code ='");
    sql.append(vo.getWc_code());
    sql.append("' ");
    sql.append(" or ");
    sql.append(" wc_name='");
    sql.append(vo.getWc_name());
    sql.append("' ");
    sql.append(");");
    //sql.append(" group by code ");
    //sql.append(" having count(1) > 1;");
    return sql.toString();
  }

}
