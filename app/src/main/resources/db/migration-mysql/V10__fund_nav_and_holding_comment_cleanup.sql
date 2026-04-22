alter table funds comment = '基金模块主数据表';
alter table funds
  modify column code varchar(32) not null comment '基金代码',
  modify column name varchar(128) not null comment '基金名称',
  modify column tags varchar(512) not null comment '基金标签列表',
  modify column top_holdings varchar(4096) not null comment '前十大持仓 JSON 字符串',
  modify column created_at timestamp not null comment '创建时间';

alter table fund_nav_history comment = '基金净值历史事实表';
alter table fund_nav_history
  modify column id varchar(64) not null comment '主键',
  modify column fund_code varchar(32) not null comment '基金代码',
  modify column nav_date date not null comment '净值日期',
  modify column unit_nav double not null comment '单位净值',
  modify column accumulated_nav double not null comment '累计净值',
  modify column day_growth double not null comment '当日涨跌幅';

alter table fund_nav_growth comment = '基金区间涨幅摘要表';
alter table fund_nav_growth
  modify column id varchar(64) not null comment '主键',
  modify column fund_code varchar(32) not null comment '基金代码',
  modify column nav_date date not null comment '净值日期',
  modify column week_growth double not null comment '近一周涨幅',
  modify column month_growth double not null comment '近一月涨幅',
  modify column year_growth double not null comment '近一年涨幅',
  modify column updated_at timestamp not null comment '更新时间';

alter table holding_daily_snapshots comment = '用户基金日终持仓收益快照表';
alter table holding_daily_snapshots
  modify column id varchar(64) not null comment '主键',
  modify column trade_date date not null comment '交易日期',
  modify column shares double not null comment '持有份额',
  modify column average_cost double not null comment '平均成本',
  modify column nav double not null comment '当日净值',
  modify column market_value double not null comment '持仓市值',
  modify column daily_pnl double not null comment '当日盈亏',
  modify column total_pnl double not null comment '累计盈亏',
  modify column total_pnl_rate double not null comment '累计盈亏率',
  modify column updated_at timestamp not null comment '更新时间';
