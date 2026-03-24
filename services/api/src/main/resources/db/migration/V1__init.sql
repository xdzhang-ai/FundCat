create table users (
  id varchar(36) primary key,
  display_name varchar(120) not null,
  phone varchar(32) not null unique,
  password_hash varchar(255) not null,
  risk_mode varchar(32) not null,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table feature_flags (
  id varchar(36) primary key,
  code varchar(80) not null unique,
  name varchar(120) not null,
  enabled boolean not null,
  environment varchar(32) not null,
  description varchar(500) not null,
  risk_level varchar(16) not null,
  created_at timestamp not null
);

create table funds (
  code varchar(12) primary key,
  name varchar(120) not null,
  category varchar(64) not null,
  risk_level varchar(32) not null,
  benchmark varchar(200) not null,
  tag_line varchar(255) not null,
  tags varchar(255) not null,
  management_fee decimal(8,4) not null,
  custody_fee decimal(8,4) not null,
  purchase_fee decimal(8,4) not null,
  status varchar(32) not null,
  created_at timestamp not null
);

create table fund_snapshots (
  id varchar(36) primary key,
  fund_code varchar(12) not null,
  nav_date date not null,
  unit_nav decimal(19,4) not null,
  accumulated_nav decimal(19,4) not null,
  day_growth decimal(10,4) not null,
  week_growth decimal(10,4) not null,
  month_growth decimal(10,4) not null,
  year_growth decimal(10,4) not null,
  asset_size decimal(19,2) not null,
  stock_ratio decimal(10,4) not null,
  bond_ratio decimal(10,4) not null,
  top_holdings text not null,
  updated_at timestamp not null,
  constraint fk_snapshot_fund foreign key (fund_code) references funds(code)
);

create table fund_estimates (
  id varchar(36) primary key,
  fund_code varchar(12) not null,
  estimated_at timestamp not null,
  estimated_nav decimal(19,4) not null,
  estimated_growth decimal(10,4) not null,
  reference_only boolean not null,
  sentiment varchar(64) not null,
  constraint fk_estimate_fund foreign key (fund_code) references funds(code)
);

create table nav_history (
  id varchar(36) primary key,
  fund_code varchar(12) not null,
  trade_date date not null,
  unit_nav decimal(19,4) not null,
  accumulated_nav decimal(19,4) not null,
  constraint fk_nav_history_fund foreign key (fund_code) references funds(code)
);

create index idx_nav_history_fund_code_trade_date on nav_history (fund_code, trade_date desc);

create table watchlists (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  fund_code varchar(12) not null,
  note varchar(255) not null,
  created_at timestamp not null,
  constraint fk_watchlist_user foreign key (user_id) references users(id),
  constraint fk_watchlist_fund foreign key (fund_code) references funds(code)
);

create table portfolios (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  name varchar(120) not null,
  broker varchar(120) not null,
  currency varchar(12) not null,
  initial_cash decimal(19,2) not null,
  created_at timestamp not null,
  constraint fk_portfolio_user foreign key (user_id) references users(id)
);

create table holding_lots (
  id varchar(36) primary key,
  portfolio_id varchar(36) not null,
  fund_code varchar(12) not null,
  fund_name varchar(120) not null,
  shares decimal(19,4) not null,
  average_cost decimal(19,4) not null,
  current_value decimal(19,2) not null,
  pnl decimal(19,2) not null,
  allocation decimal(10,4) not null,
  source varchar(64) not null,
  imported boolean not null,
  updated_at timestamp not null,
  constraint fk_holding_portfolio foreign key (portfolio_id) references portfolios(id),
  constraint fk_holding_fund foreign key (fund_code) references funds(code)
);

create table paper_orders (
  id varchar(36) primary key,
  portfolio_id varchar(36) not null,
  fund_code varchar(12) not null,
  fund_name varchar(120) not null,
  order_type varchar(16) not null,
  amount decimal(19,2) not null,
  shares decimal(19,4) not null,
  fee decimal(19,2) not null,
  status varchar(32) not null,
  executed_at timestamp not null,
  note varchar(255) not null,
  constraint fk_order_portfolio foreign key (portfolio_id) references portfolios(id),
  constraint fk_order_fund foreign key (fund_code) references funds(code)
);

create table sip_plans (
  id varchar(36) primary key,
  portfolio_id varchar(36) not null,
  fund_code varchar(12) not null,
  fund_name varchar(120) not null,
  amount decimal(19,2) not null,
  cadence varchar(32) not null,
  next_run_at timestamp not null,
  active boolean not null,
  constraint fk_sip_portfolio foreign key (portfolio_id) references portfolios(id),
  constraint fk_sip_fund foreign key (fund_code) references funds(code)
);

create table import_jobs (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  source_platform varchar(80) not null,
  status varchar(32) not null,
  file_name varchar(180) not null,
  recognized_holdings int not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint fk_import_job_user foreign key (user_id) references users(id)
);

create table weekly_reports (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  week_label varchar(40) not null,
  summary text not null,
  return_rate decimal(10,4) not null,
  best_fund_code varchar(12) not null,
  risk_note varchar(255) not null,
  created_at timestamp not null,
  constraint fk_weekly_report_user foreign key (user_id) references users(id),
  constraint fk_weekly_report_fund foreign key (best_fund_code) references funds(code)
);

create table alert_rules (
  id varchar(36) primary key,
  user_id varchar(36) not null,
  fund_code varchar(12) not null,
  rule_type varchar(64) not null,
  threshold_value decimal(19,4) not null,
  enabled boolean not null,
  channel varchar(32) not null,
  constraint fk_alert_rule_user foreign key (user_id) references users(id),
  constraint fk_alert_rule_fund foreign key (fund_code) references funds(code)
);
