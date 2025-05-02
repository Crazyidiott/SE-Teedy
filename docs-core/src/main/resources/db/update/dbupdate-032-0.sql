-- DBUPDATE-032-0.SQL

create cached table T_USER_REGISTRATION (
    REG_ID_C varchar(36) not null, 
    REG_USERNAME_C varchar(50) not null,
    REG_PASSWORD_C varchar(60) not null,
    REG_EMAIL_C varchar(100) not null,
    REG_CREATEDATE_D datetime not null,
    REG_APPROVALDATE_D datetime,
    REG_APPROVEDBY_C varchar(36),
    REG_STATUS_C varchar(20) not null, -- PENDING, APPROVED, REJECTED
    REG_MESSAGE_C varchar(500),
    primary key (REG_ID_C)
);

-- 添加外键关系
alter table T_USER_REGISTRATION add constraint FK_REG_APPROVEDBY_C foreign key (REG_APPROVEDBY_C) references T_USER (USE_ID_C) on delete restrict on update restrict;

-- 更新数据库版本
update T_CONFIG set CFG_VALUE_C = '32' where CFG_ID_C = 'DB_VERSION';
