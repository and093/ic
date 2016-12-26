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
*���벿�ű���
*/
public String bc_code;
/**
*���벿������
*/
public String bc_name;
/**
*����pk
*/
public String pk_dept;
/**
*����
*/
public String pk_dpc;
/**
*��������
*/
public String pk_group;
/**
*�����֯
*/
public String pk_org;
/**
*ʱ���
*/
public UFDateTime ts;
/** 
* ��ȡ���벿�ű���
*
* @return ���벿�ű���
*/
public String getBc_code () {
return this.bc_code;
 } 

/** 
* �������벿�ű���
*
* @param bc_code ���벿�ű���
*/
public void setBc_code ( String bc_code) {
this.bc_code=bc_code;
 } 

/** 
* ��ȡ���벿������
*
* @return ���벿������
*/
public String getBc_name () {
return this.bc_name;
 } 

/** 
* �������벿������
*
* @param bc_name ���벿������
*/
public void setBc_name ( String bc_name) {
this.bc_name=bc_name;
 } 

/** 
* ��ȡ����pk
*
* @return ����pk
*/
public String getPk_dept () {
return this.pk_dept;
 } 

/** 
* ���ò���pk
*
* @param pk_dept ����pk
*/
public void setPk_dept ( String pk_dept) {
this.pk_dept=pk_dept;
 } 

/** 
* ��ȡ����
*
* @return ����
*/
public String getPk_dpc () {
return this.pk_dpc;
 } 

/** 
* ��������
*
* @param pk_dpc ����
*/
public void setPk_dpc ( String pk_dpc) {
this.pk_dpc=pk_dpc;
 } 

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
    return VOMetaFactory.getInstance().getVOMeta("ic.dpc");
  }
 public static String getDefaultTableName(){
	  return "ic_dpc";
  }
}