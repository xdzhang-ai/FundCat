alter table users rename to auth_users;
alter table feature_flags rename to ops_feature_flags;
alter table fund_snapshots rename to fund_nav_snapshots;
alter table fund_estimates rename to fund_intraday_estimates;
alter table nav_history rename to fund_nav_history;
alter table watchlists rename to watchlist_items;
alter table watchlist_groups rename to watchlist_item_groups;
alter table user_fund_holdings rename to holding_positions;
alter table user_fund_daily_profit_snapshots rename to holding_daily_snapshots;
alter table user_fund_operation_records rename to holding_operation_records;

drop table if exists holding_lots;
drop table if exists paper_orders;
alter table sip_plans drop foreign key fk_sip_portfolio;
drop table if exists portfolios;
drop table if exists import_jobs;
drop table if exists weekly_reports;
drop table if exists alert_rules;
