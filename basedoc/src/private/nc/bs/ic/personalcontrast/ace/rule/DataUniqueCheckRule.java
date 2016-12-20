package nc.bs.ic.personalcontrast.ace.rule;

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
import nc.vo.ic.personalcontrast.personalContrastVO;

public class DataUniqueCheckRule implements IRule<BatchOperateVO>{

  @Override
  public void process(BatchOperateVO[] vos) {
    if (vos == null || vos.length == 0) {
      return;
    }
    Object[] oadd = vos[0].getAddObjs();
    Object[] oupd = vos[0].getUpdObjs();
    // 如果没有新增和修改的数据，则不需要校验
    personalContrastVO[] vosadd = null;
    if (oadd != null && oadd.length > 0) {
      vosadd = this.convertArrayType(oadd);
      this.checkDBUnique(vosadd);
      // return;
    }
    personalContrastVO[] vosupd = null;
    if (oupd != null && oupd.length > 0) {
      vosupd = this.convertArrayType(oupd);
      this.checkDBUnique(vosupd);
      // return;
    }
  }
  
  public void checkDBUnique(personalContrastVO[] bills) {
    if (bills == null || bills.length == 0) {
      return;
    }
    for (int j = 0; j < bills.length; j++) {
      personalContrastVO vo = bills[j];
      // String[][] voCheckedColumnsArray = vo.getCheckedColumnsArray();
      personalContrastVO[] dbvo =
          new VOQuery<personalContrastVO>(personalContrastVO.class).query(new String[] {
            vo.getPrimaryKey()
          });
      this.doLock(dbvo);
      IRowSet rowSet = new DataAccessUtils().query(this.getCheckSql(dbvo[0]));
      if (rowSet.size() > 1) {
        ExceptionUtils.wrappBusinessException("保存失败，当前所新增或修改的信息在该集团已经存在编码或名称相同的记录。");
      }
    }
  }



  private personalContrastVO[] convertArrayType(Object[] vos) {
    personalContrastVO[] smartVOs =
        (personalContrastVO[]) Array.newInstance(personalContrastVO.class, vos.length);
    System.arraycopy(vos, 0, smartVOs, 0, vos.length);
    return smartVOs;
  }

  private void doLock(personalContrastVO[] bills) {
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
  private String getCheckSql(personalContrastVO vo) {
    StringBuffer sql = new StringBuffer();
    sql.append("select p_code,p_name ");
    sql.append("  from ");
    sql.append(vo.getTableName());

    sql.append(" where dr=0 ");
    sql.append(" and ");
    sql.append(" (p_code ='");
    sql.append(vo.getP_code());
    sql.append("' ");
    sql.append(" or ");
    sql.append(" p_name='");
    sql.append(vo.getP_name());
    sql.append("' ");
    sql.append(");");
    //sql.append(" group by code ");
    //sql.append(" having count(1) > 1;");
    return sql.toString();
  }

}
