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
*��Ա����
*/
public String p_code;
/**
*��Ա����
*/
public String p_name;
/**
*����
*/
public String pk_group;
/**
*��֯
*/
public String pk_org;
/**
*����
*/
public String pk_pc;
/**
*��Աpk
*/
public String pk_psndon;
/**
*ʱ���
*/
public UFDateTime ts;
/** 
* ��ȡ��Ա����
*
* @return ��Ա����
*/
public String getP_code () {
return this.p_code;
 } 

/** 
* ������Ա����
*
* @param p_code ��Ա����
*/
public void setP_code ( String p_code) {
this.p_code=p_code;
 } 

/** 
* ��ȡ��Ա����
*
* @return ��Ա����
*/
public String getP_name () {
return this.p_name;
 } 

/** 
* ������Ա����
*
* @param p_name ��Ա����
*/
public void setP_name ( String p_name) {
this.p_name=p_name;
 } 

/** 
* ��ȡ����
*
* @return ����
*/
public String getPk_group () {
return this.pk_group;
 } 

/** 
* ���ü���
*
* @param pk_group ����
*/
public void setPk_group ( String pk_group) {
this.pk_group=pk_group;
 } 

/** 
* ��ȡ��֯
*
* @return ��֯
*/
public String getPk_org () {
return this.pk_org;
 } 

/** 
* ������֯
*
* @param pk_org ��֯
*/
public void setPk_org ( String pk_org) {
this.pk_org=pk_org;
 } 

/** 
* ��ȡ����
*
* @return ����
*/
public String getPk_pc () {
return this.pk_pc;
 } 

/** 
* ��������
*
* @param pk_pc ����
*/
public void setPk_pc ( String pk_pc) {
this.pk_pc=pk_pc;
 } 

/** 
* ��ȡ��Աpk
*
* @return ��Աpk
*/
public String getPk_psndon () {
return this.pk_psndon;
 } 

/** 
* ������Աpk
*
* @param pk_psndon ��Աpk
*/
public void setPk_psndon ( String pk_psndon) {
this.pk_psndon=pk_psndon;
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


  @Override
  public IVOMeta getMetaData() {
    return VOMetaFactory.getInstance().getVOMeta("ic.personal_contrast");
  }
 public static String getDefaultTableName(){
	  return "ic_personal_contrast";
  }
}