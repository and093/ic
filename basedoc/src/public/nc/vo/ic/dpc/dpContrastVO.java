package nc.vo.ic.dpc;

import nc.vo.pub.IVOMeta;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pattern.model.meta.entity.vo.VOMetaFactory;

public class dpContrastVO extends SuperVO {
/**
*条码部门编码
*/
public String bc_code;
/**
*条码部门名称
*/
public String bc_name;
/**
*部门pk
*/
public String pk_dept;
/**
*主键
*/
public String pk_dpc;
/**
*所属集团
*/
public String pk_group;
/**
*库存组织
*/
public String pk_org;
/**
*时间戳
*/
public UFDateTime ts;
/** 
* 获取条码部门编码
*
* @return 条码部门编码
*/
public String getBc_code () {
return this.bc_code;
 } 

/** 
* 设置条码部门编码
*
* @param bc_code 条码部门编码
*/
public void setBc_code ( String bc_code) {
this.bc_code=bc_code;
 } 

/** 
* 获取条码部门名称
*
* @return 条码部门名称
*/
public String getBc_name () {
return this.bc_name;
 } 

/** 
* 设置条码部门名称
*
* @param bc_name 条码部门名称
*/
public void setBc_name ( String bc_name) {
this.bc_name=bc_name;
 } 

/** 
* 获取部门pk
*
* @return 部门pk
*/
public String getPk_dept () {
return this.pk_dept;
 } 

/** 
* 设置部门pk
*
* @param pk_dept 部门pk
*/
public void setPk_dept ( String pk_dept) {
this.pk_dept=pk_dept;
 } 

/** 
* 获取主键
*
* @return 主键
*/
public String getPk_dpc () {
return this.pk_dpc;
 } 

/** 
* 设置主键
*
* @param pk_dpc 主键
*/
public void setPk_dpc ( String pk_dpc) {
this.pk_dpc=pk_dpc;
 } 

/** 
* 获取所属集团
*
* @return 所属集团
*/
public String getPk_group () {
return this.pk_group;
 } 

/** 
* 设置所属集团
*
* @param pk_group 所属集团
*/
public void setPk_group ( String pk_group) {
this.pk_group=pk_group;
 } 

/** 
* 获取库存组织
*
* @return 库存组织
*/
public String getPk_org () {
return this.pk_org;
 } 

/** 
* 设置库存组织
*
* @param pk_org 库存组织
*/
public void setPk_org ( String pk_org) {
this.pk_org=pk_org;
 } 

/** 
* 获取时间戳
*
* @return 时间戳
*/
public UFDateTime getTs () {
return this.ts;
 } 

/** 
* 设置时间戳
*
* @param ts 时间戳
*/
public void setTs ( UFDateTime ts) {
this.ts=ts;
 } 


  @Override
  public IVOMeta getMetaData() {
    return VOMetaFactory.getInstance().getVOMeta("ic.dpc");
  }
 public static String getDefaultTableName(){
	  return "ic_dpc";
  }
}