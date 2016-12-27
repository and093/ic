create table ic_personal_contrast (
pk_pc CHAR(20) NOT NULL,
p_name VARCHAR(50) NULL,
p_code VARCHAR(50) NULL,
pk_group VARCHAR(20) default '~' NULL,
pk_org VARCHAR(20) default '~' NULL,
pk_psndon VARCHAR(20) default '~' NULL,
CONSTRAINT PK_RSONAL_CONTRAST PRIMARY KEY (pk_pc),
ts char(19) default to_char(sysdate,'yyyy-mm-dd hh24:mi:ss') NULL,
 dr smallint default 0 NULL 
)


