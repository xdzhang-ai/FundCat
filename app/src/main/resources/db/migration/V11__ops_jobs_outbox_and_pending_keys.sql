create table outbox_events (
  id varchar(64) primary key,
  event_type varchar(64) not null,
  aggregate_key varchar(128) not null,
  payload text not null,
  status varchar(16) not null,
  created_at timestamp not null,
  published_at timestamp null
);

create index idx_outbox_status_created_at on outbox_events (status, created_at);
create index idx_outbox_event_type_created_at on outbox_events (event_type, created_at);

create table ops_job_runs (
  id varchar(64) primary key,
  job_code varchar(64) not null,
  job_source varchar(32) not null,
  job_type varchar(32) not null,
  run_key varchar(128) not null,
  status varchar(16) not null,
  payload_summary varchar(1024) null,
  stats_total int not null default 0,
  stats_success int not null default 0,
  stats_failed int not null default 0,
  stats_skipped int not null default 0,
  started_at timestamp null,
  finished_at timestamp null,
  duration_ms bigint null,
  attempt_count int not null default 0,
  error_message varchar(1024) null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_ops_job_runs unique (job_code, run_key)
);

create index idx_ops_job_runs_source_started_at on ops_job_runs (job_source, started_at desc);
create index idx_ops_job_runs_status_started_at on ops_job_runs (status, started_at desc);

create table fund_nav_pending_keys (
  id varchar(64) primary key,
  fund_code varchar(16) not null,
  nav_date date not null,
  status varchar(16) not null,
  created_at timestamp not null,
  updated_at timestamp not null,
  constraint uk_fund_nav_pending_keys unique (fund_code, nav_date)
);

create index idx_fund_nav_pending_keys_date_status on fund_nav_pending_keys (nav_date, status);
