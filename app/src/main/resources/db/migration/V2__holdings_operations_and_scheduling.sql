alter table sip_plans add column user_id varchar(36);
alter table sip_plans add column status varchar(16) default '生效' not null;
alter table sip_plans add column fee_rate decimal(10,6) default 0 not null;
alter table sip_plans add column created_at timestamp default current_timestamp not null;
alter table sip_plans add column updated_at timestamp default current_timestamp not null;

create index idx_sip_plans_user_id on sip_plans (user_id);

create table watchlist_groups (
  id varchar(36) primary key,
  watchlist_id varchar(36) not null,
  group_code varchar(32) not null,
  created_at timestamp not null,
  constraint fk_watchlist_group_watchlist foreign key (watchlist_id) references watchlists(id),
  constraint uk_watchlist_group unique (watchlist_id, group_code)
);

create table user_fund_holdings (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  fund_code varchar(12) not null,
  fund_name varchar(120) not null,
  shares decimal(19,4) not null,
  average_cost decimal(19,4) not null,
  market_value decimal(19,2) not null,
  holding_pnl decimal(19,2) not null,
  holding_pnl_rate decimal(10,4) not null,
  updated_at timestamp not null,
  constraint fk_user_fund_holding_user foreign key (user_id) references users(id),
  constraint fk_user_fund_holding_fund foreign key (fund_code) references funds(code),
  constraint uk_user_fund_holding unique (user_id, fund_code)
);

create index idx_user_fund_holding_user_code on user_fund_holdings (user_id, fund_code);

create table user_fund_daily_profit_snapshots (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  fund_code varchar(12) not null,
  trade_date date not null,
  shares decimal(19,4) not null,
  average_cost decimal(19,4) not null,
  nav decimal(19,4) not null,
  market_value decimal(19,2) not null,
  daily_pnl decimal(19,2) not null,
  total_pnl decimal(19,2) not null,
  total_pnl_rate decimal(10,4) not null,
  updated_at timestamp not null,
  constraint fk_daily_profit_user foreign key (user_id) references users(id),
  constraint fk_daily_profit_fund foreign key (fund_code) references funds(code),
  constraint uk_daily_profit unique (user_id, fund_code, trade_date)
);

create index idx_daily_profit_user_code_date on user_fund_daily_profit_snapshots (user_id, fund_code, trade_date desc);

create table user_fund_operation_records (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  fund_code varchar(12) not null,
  operation varchar(16) not null,
  source varchar(16) not null,
  status varchar(16) not null,
  trade_date date not null,
  amount decimal(19,2) not null,
  shares_delta decimal(19,4) not null,
  nav decimal(19,4) not null,
  fee_rate decimal(10,6) not null,
  fee_amount decimal(19,2) not null,
  sip_plan_id varchar(36),
  note varchar(255),
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_operation_user foreign key (user_id) references users(id),
  constraint fk_operation_fund foreign key (fund_code) references funds(code)
);

create index idx_operation_user_code_date on user_fund_operation_records (user_id, fund_code, trade_date asc, created_at asc);

create table shedlock (
  name varchar(64) not null primary key,
  lock_until timestamp not null,
  locked_at timestamp not null,
  locked_by varchar(255) not null
);
