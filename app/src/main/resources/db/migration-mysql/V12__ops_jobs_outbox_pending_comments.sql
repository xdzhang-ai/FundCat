alter table outbox_events comment = '跨语言数据事件 outbox 表';
alter table outbox_events
  modify id varchar(64) comment '事件 ID',
  modify event_type varchar(64) comment '事件类型',
  modify aggregate_key varchar(128) comment '聚合键',
  modify payload text comment '事件载荷 JSON',
  modify status varchar(16) comment '发送状态',
  modify created_at timestamp comment '创建时间',
  modify published_at timestamp null comment '发布时间';

alter table ops_job_runs comment = '统一任务运行状态表';
alter table ops_job_runs
  modify id varchar(64) comment '任务运行 ID',
  modify job_code varchar(64) comment '任务编码',
  modify job_source varchar(32) comment '任务来源',
  modify job_type varchar(32) comment '任务类型',
  modify run_key varchar(128) comment '幂等运行键',
  modify status varchar(16) comment '任务状态',
  modify payload_summary varchar(1024) null comment '任务摘要',
  modify stats_total int comment '总数',
  modify stats_success int comment '成功数',
  modify stats_failed int comment '失败数',
  modify stats_skipped int comment '跳过数',
  modify started_at timestamp null comment '开始时间',
  modify finished_at timestamp null comment '结束时间',
  modify duration_ms bigint null comment '耗时毫秒',
  modify attempt_count int comment '尝试次数',
  modify error_message varchar(1024) null comment '错误信息',
  modify created_at timestamp comment '创建时间',
  modify updated_at timestamp comment '更新时间';

alter table fund_nav_pending_keys comment = '基金净值待抓取中间表';
alter table fund_nav_pending_keys
  modify id varchar(64) comment '记录 ID',
  modify fund_code varchar(16) comment '基金代码',
  modify nav_date date comment '净值日期',
  modify status varchar(16) comment '待抓取状态',
  modify created_at timestamp comment '创建时间',
  modify updated_at timestamp comment '更新时间';
