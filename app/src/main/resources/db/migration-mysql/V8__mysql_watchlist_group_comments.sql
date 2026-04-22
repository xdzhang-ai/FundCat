alter table watchlist_groups comment = '自选模块用户分组表';
alter table watchlist_groups
  modify column id varchar(128) not null comment '分组ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column group_name varchar(64) not null comment '分组名称',
  modify column created_at timestamp not null comment '创建时间';

alter table watchlist_items comment = '自选模块基金条目表';
alter table watchlist_items
  modify column id varchar(36) not null comment '自选条目ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column note varchar(255) not null comment '自选备注',
  modify column created_at timestamp not null comment '创建时间',
  modify column group_id varchar(128) null comment '关联分组ID';
