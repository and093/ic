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
*��������
*/
public String pk_group;
/**
*�����֯
*/
public String pk_org;
/**
*�ֿ�pk
*/
public String pk_stordoc;
/**
*����
*/
public String pk_wc;
/**
*ʱ���
*/
public UFDateTime ts;
/**
*������ֿ���
*/
public String wc_code;
/**
*������ֿ�����
*/
public String wc_name;
/** 
* ��ȡ��������
*
* @return ��������
*/
public String getPk_group () {
return this.pk_group;
 } 

/** 
* ������������
*
* @param pk_group ��������
*/
public void setPk_group ( String pk_group) {
this.pk_group=pk_group;
 } 

/** 
* ��ȡ�����֯
*
* @return �����֯
*/
public String getPk_org () {
return this.pk_org;
 } 

/** 
* ���ÿ����֯
*
* @param pk_org �����֯
*/
public void setPk_org ( String pk_org) {
this.pk_org=pk_org;
 } 

/** 
* ��ȡ�ֿ�pk
*
* @return �ֿ�pk
*/
public String getPk_stordoc () {
return this.pk_stordoc;
 } 

/** 
* ���òֿ�pk
*
* @param pk_stordoc �ֿ�pk
*/
public void setPk_stordoc ( String pk_stordoc) {
this.pk_stordoc=pk_stordoc;
 } 

/** 
* ��ȡ����
*
* @return ����
*/
public String getPk_wc () {
return this.pk_wc;
 } 

/** 
* ��������
*
* @param pk_wc ����
*/
public void setPk_wc ( String pk_wc) {
this.pk_wc=pk_wc;
 } 

/** 
* ��ȡʱ���
*
* @return ʱ���
*/
public UFDateTime getTs () {
return this.ts;
 } 

/** 
* ����ʱ���
*
* @param ts ʱ���
*/
public void setTs ( UFDateTime ts) {
this.ts=ts;
 } 

/** 
* ��ȡ������ֿ���
*
* @return ������ֿ���
*/
public String getWc_code () {
return this.wc_code;
 } 

/** 
* ����������ֿ���
*
* @param wc_code ������ֿ���
*/
public void setWc_code ( String wc_code) {
this.wc_code=wc_code;
 } 

/** 
* ��ȡ������ֿ�����
*
* @return ������ֿ�����
*/
public String getWc_name () {
return this.wc_name;
 } 

/** 
* ����������ֿ�����
*
* @param wc_name ������ֿ�����
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