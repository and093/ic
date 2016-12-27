create table ic_dpc (
pk_dpc CHAR(20) NOT NULL,
pk_org VARCHAR(20) default '~' NULL,
pk_group VARCHAR(20) default '~' NULL,
pk_dept CHAR(20) NULL,
bc_name VARCHAR(50) NULL,
bc_code VARCHAR(50) NULL,
CONSTRAINT PK_IC_DPC PRIMARY KEY (pk_dpc),
ts char(19) default to_char(sysdate,'yyyy-mm-dd hh24:mi:ss') NULL,
 dr smallint default 0 NULL 
)


