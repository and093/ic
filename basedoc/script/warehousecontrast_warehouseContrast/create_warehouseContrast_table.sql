create table ic_warehouse_contrast (
pk_wc CHAR(20) NOT NULL,
wc_name VARCHAR(50) NULL,
wc_code VARCHAR(50) NULL,
pk_group VARCHAR(20) default '~' NULL,
pk_org VARCHAR(20) default '~' NULL,
pk_stordoc VARCHAR(20) default '~' NULL,
CONSTRAINT PK_EHOUSE_CONTRAST PRIMARY KEY (pk_wc),
ts char(19) default to_char(sysdate,'yyyy-mm-dd hh24:mi:ss') NULL,
 dr smallint default 0 NULL 
)


