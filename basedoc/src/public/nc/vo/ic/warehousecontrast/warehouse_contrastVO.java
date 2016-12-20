package nc.vo.ic.warehousecontrast;

import nc.vo.pub.IVOMeta;
import nc.vo.pub.SuperVO;
import nc.vo.pub.lang.UFBoolean;
import nc.vo.pub.lang.UFDate;
import nc.vo.pub.lang.UFDateTime;
import nc.vo.pub.lang.UFDouble;
import nc.vo.pubapp.pattern.model.meta.entity.vo.VOMetaFactory;

public class warehouse_contrastVO extends SuperVO {
/**
*所属集团
*/
public String pk_group;
/**
*库存组织
*/
public String pk_org;
/**
*仓库pk
*/
public String pk_stordoc;
/**
*主键
*/
public String pk_wc;
/**
*时间戳
*/
public UFDateTime ts;
/**
*条形码仓库编号
*/
public String wc_code;
/**
*条形码仓库名称
*/
public String wc_name;
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
* 获取仓库pk
*
* @return 仓库pk
*/
public String getPk_stordoc () {
return this.pk_stordoc;
 } 

/** 
* 设置仓库pk
*
* @param pk_stordoc 仓库pk
*/
public void setPk_stordoc ( String pk_stordoc) {
this.pk_stordoc=pk_stordoc;
 } 

/** 
* 获取主键
*
* @return 主键
*/
public String getPk_wc () {
return this.pk_wc;
 } 

/** 
* 设置主键
*
* @param pk_wc 主键
*/
public void setPk_wc ( String pk_wc) {
this.pk_wc=pk_wc;
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

/** 
* 获取条形码仓库编号
*
* @return 条形码仓库编号
*/
public String getWc_code () {
return this.wc_code;
 } 

/** 
* 设置条形码仓库编号
*
* @param wc_code 条形码仓库编号
*/
public void setWc_code ( String wc_code) {
this.wc_code=wc_code;
 } 

/** 
* 获取条形码仓库名称
*
* @return 条形码仓库名称
*/
public String getWc_name () {
return this.wc_name;
 } 

/** 
* 设置条形码仓库名称
*
* @param wc_name 条形码仓库名称
*/
public void setWc_name ( String wc_name) {
this.wc_name=wc_name;
 } 


  @Override
  public IVOMeta getMetaData() {
    return VOMetaFactory.getInstance().getVOMeta("ic.warehouse_contrast");
  }
 public static String getDefaultTableName(){
	  return "ic_warehouse_contrast";
  }
}