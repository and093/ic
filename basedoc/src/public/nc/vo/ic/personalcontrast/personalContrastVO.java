package nc.vo.ic.personalcontrast;

import nc.vo.pub.IVOMeta;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pattern.model.meta.entity.vo.VOMetaFactory;

public class personalContrastVO extends SuperVO {
/**
*人员编码
*/
public String p_code;
/**
*人员名称
*/
public String p_name;
/**
*集团
*/
public String pk_group;
/**
*组织
*/
public String pk_org;
/**
*主键
*/
public String pk_pc;
/**
*人员pk
*/
public String pk_psndon;
/**
*时间戳
*/
public UFDateTime ts;
/** 
* 获取人员编码
*
* @return 人员编码
*/
public String getP_code () {
return this.p_code;
 } 

/** 
* 设置人员编码
*
* @param p_code 人员编码
*/
public void setP_code ( String p_code) {
this.p_code=p_code;
 } 

/** 
* 获取人员名称
*
* @return 人员名称
*/
public String getP_name () {
return this.p_name;
 } 

/** 
* 设置人员名称
*
* @param p_name 人员名称
*/
public void setP_name ( String p_name) {
this.p_name=p_name;
 } 

/** 
* 获取集团
*
* @return 集团
*/
public String getPk_group () {
return this.pk_group;
 } 

/** 
* 设置集团
*
* @param pk_group 集团
*/
public void setPk_group ( String pk_group) {
this.pk_group=pk_group;
 } 

/** 
* 获取组织
*
* @return 组织
*/
public String getPk_org () {
return this.pk_org;
 } 

/** 
* 设置组织
*
* @param pk_org 组织
*/
public void setPk_org ( String pk_org) {
this.pk_org=pk_org;
 } 

/** 
* 获取主键
*
* @return 主键
*/
public String getPk_pc () {
return this.pk_pc;
 } 

/** 
* 设置主键
*
* @param pk_pc 主键
*/
public void setPk_pc ( String pk_pc) {
this.pk_pc=pk_pc;
 } 

/** 
* 获取人员pk
*
* @return 人员pk
*/
public String getPk_psndon () {
return this.pk_psndon;
 } 

/** 
* 设置人员pk
*
* @param pk_psndon 人员pk
*/
public void setPk_psndon ( String pk_psndon) {
this.pk_psndon=pk_psndon;
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
    return VOMetaFactory.getInstance().getVOMeta("ic.personal_contrast");
  }
 public static String getDefaultTableName(){
	  return "ic_personal_contrast";
  }
}