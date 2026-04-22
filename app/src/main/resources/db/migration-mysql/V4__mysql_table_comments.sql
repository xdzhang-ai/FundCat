alter table auth_users comment = '认证模块用户表';
alter table auth_users
  modify column id varchar(36) not null comment '用户ID',
  modify column display_name varchar(120) not null comment '展示名称',
  modify column username varchar(64) not null comment '登录用户名',
  modify column password_hash varchar(255) not null comment '密码哈希',
  modify column risk_mode varchar(32) not null comment '风险模式',
  modify column created_at timestamp not null comment '创建时间',
  modify column updated_at timestamp not null comment '更新时间';

alter table ops_feature_flags comment = '运维模块功能开关表';
alter table ops_feature_flags
  modify column id varchar(36) not null comment '功能开关ID',
  modify column code varchar(80) not null comment '开关编码',
  modify column name varchar(120) not null comment '开关名称',
  modify column enabled boolean not null comment '是否启用',
  modify column environment varchar(32) not null comment '生效环境',
  modify column description varchar(500) not null comment '开关说明',
  modify column risk_level varchar(16) not null comment '风险等级',
  modify column created_at timestamp not null comment '创建时间';

alter table funds comment = '基金模块基础信息表';
alter table funds
  modify column code varchar(12) not null comment '基金代码',
  modify column name varchar(120) not null comment '基金名称',
  modify column category varchar(64) not null comment '基金分类',
  modify column risk_level varchar(32) not null comment '风险等级',
  modify column benchmark varchar(200) not null comment '业绩比较基准',
  modify column tag_line varchar(255) not null comment '产品短标语',
  modify column tags varchar(255) not null comment '产品标签',
  modify column management_fee decimal(8,4) not null comment '管理费率',
  modify column custody_fee decimal(8,4) not null comment '托管费率',
  modify column purchase_fee decimal(8,4) not null comment '申购费率',
  modify column status varchar(32) not null comment '基金状态',
  modify column created_at timestamp not null comment '创建时间';

alter table fund_nav_snapshots comment = '基金模块确认净值快照表';
alter table fund_nav_snapshots
  modify column id varchar(36) not null comment '快照ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column nav_date date not null comment '净值日期',
  modify column unit_nav decimal(19,4) not null comment '单位净值',
  modify column accumulated_nav decimal(19,4) not null comment '累计净值',
  modify column day_growth decimal(10,4) not null comment '日涨跌幅',
  modify column week_growth decimal(10,4) not null comment '近一周涨跌幅',
  modify column month_growth decimal(10,4) not null comment '近一月涨跌幅',
  modify column year_growth decimal(10,4) not null comment '近一年涨跌幅',
  modify column asset_size decimal(19,2) not null comment '资产规模',
  modify column stock_ratio decimal(10,4) not null comment '股票仓位占比',
  modify column bond_ratio decimal(10,4) not null comment '债券仓位占比',
  modify column top_holdings text not null comment '前序持仓摘要',
  modify column updated_at timestamp not null comment '更新时间';

alter table fund_intraday_estimates comment = '基金模块盘中估值参考表';
alter table fund_intraday_estimates
  modify column id varchar(36) not null comment '估值记录ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column estimated_at timestamp not null comment '估值时间',
  modify column estimated_nav decimal(19,4) not null comment '估算净值',
  modify column estimated_growth decimal(10,4) not null comment '估算涨跌幅',
  modify column reference_only boolean not null comment '是否仅供参考',
  modify column sentiment varchar(64) not null comment '情绪标签';

alter table fund_nav_history comment = '基金模块历史净值表';
alter table fund_nav_history
  modify column id varchar(36) not null comment '历史记录ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column trade_date date not null comment '交易日期',
  modify column unit_nav decimal(19,4) not null comment '单位净值',
  modify column accumulated_nav decimal(19,4) not null comment '累计净值';

alter table watchlist_items comment = '自选模块基金条目表';
alter table watchlist_items
  modify column id varchar(36) not null comment '自选条目ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column note varchar(255) not null comment '自选备注',
  modify column created_at timestamp not null comment '创建时间';

alter table watchlist_item_groups comment = '自选模块分组映射表';
alter table watchlist_item_groups
  modify column id varchar(36) not null comment '分组映射ID',
  modify column watchlist_id varchar(36) not null comment '自选条目ID',
  modify column group_code varchar(32) not null comment '分组编码',
  modify column created_at timestamp not null comment '创建时间';

alter table sip_plans comment = '定投模块计划表';
alter table sip_plans
  modify column id varchar(36) not null comment '定投计划ID',
  modify column portfolio_id varchar(36) not null comment '兼容保留的组合标识',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column fund_name varchar(120) not null comment '基金名称',
  modify column amount decimal(19,2) not null comment '每期定投金额',
  modify column cadence varchar(32) not null comment '定投周期',
  modify column next_run_at timestamp not null comment '下一次执行时间',
  modify column active boolean not null comment '是否激活',
  modify column user_id varchar(36) null comment '用户ID',
  modify column status varchar(16) not null comment '计划状态',
  modify column fee_rate decimal(10,6) not null comment '费率',
  modify column created_at timestamp not null comment '创建时间',
  modify column updated_at timestamp not null comment '更新时间';

alter table holding_positions comment = '持仓模块当前持仓表';
alter table holding_positions
  modify column id varchar(36) not null comment '持仓ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column fund_name varchar(120) not null comment '基金名称',
  modify column shares decimal(19,4) not null comment '持有份额',
  modify column average_cost decimal(19,4) not null comment '平均成本',
  modify column market_value decimal(19,2) not null comment '当前市值',
  modify column holding_pnl decimal(19,2) not null comment '持有收益',
  modify column holding_pnl_rate decimal(10,4) not null comment '持有收益率',
  modify column updated_at timestamp not null comment '更新时间';

alter table holding_daily_snapshots comment = '持仓模块日收益快照表';
alter table holding_daily_snapshots
  modify column id varchar(36) not null comment '日快照ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column trade_date date not null comment '交易日期',
  modify column shares decimal(19,4) not null comment '持有份额',
  modify column average_cost decimal(19,4) not null comment '平均成本',
  modify column nav decimal(19,4) not null comment '当日净值',
  modify column market_value decimal(19,2) not null comment '当日市值',
  modify column daily_pnl decimal(19,2) not null comment '当日盈亏',
  modify column total_pnl decimal(19,2) not null comment '累计盈亏',
  modify column total_pnl_rate decimal(10,4) not null comment '累计收益率',
  modify column updated_at timestamp not null comment '更新时间';

alter table holding_operation_records comment = '持仓模块操作记录表';
alter table holding_operation_records
  modify column id varchar(36) not null comment '操作记录ID',
  modify column user_id varchar(36) not null comment '用户ID',
  modify column fund_code varchar(12) not null comment '基金代码',
  modify column operation varchar(16) not null comment '操作类型',
  modify column source varchar(16) not null comment '操作来源',
  modify column status varchar(16) not null comment '执行状态',
  modify column trade_date date not null comment '交易日期',
  modify column amount decimal(19,2) not null comment '交易金额',
  modify column shares_delta decimal(19,4) not null comment '份额变化',
  modify column nav decimal(19,4) not null comment '确认净值',
  modify column fee_rate decimal(10,6) not null comment '费率',
  modify column fee_amount decimal(19,2) not null comment '手续费金额',
  modify column sip_plan_id varchar(36) null comment '关联定投计划ID',
  modify column note varchar(255) null comment '备注',
  modify column created_at timestamp not null comment '创建时间',
  modify column updated_at timestamp not null comment '更新时间';

alter table shedlock comment = '分布式调度锁表';
alter table shedlock
  modify column name varchar(64) not null comment '锁名称',
  modify column lock_until timestamp not null comment '锁过期时间',
  modify column locked_at timestamp not null comment '加锁时间',
  modify column locked_by varchar(255) not null comment '持锁节点';
