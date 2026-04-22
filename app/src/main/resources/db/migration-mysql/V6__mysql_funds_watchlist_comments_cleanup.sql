alter table funds comment = '基金模块基础信息表';
alter table funds
  modify column code varchar(12) not null comment '基金代码',
  modify column name varchar(120) not null comment '基金名称',
  modify column tags varchar(255) not null comment '产品标签',
  modify column created_at timestamp not null comment '创建时间';

alter table watchlist_items comment = '自选模块基金条目表';
alter table watchlist_items
  modify column id varchar(36) not null comment '自选条目ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column note varchar(255) not null comment '自选备注',
  modify column group_name varchar(32) not null comment '用户自选分组',
  modify column created_at timestamp not null comment '创建时间';
