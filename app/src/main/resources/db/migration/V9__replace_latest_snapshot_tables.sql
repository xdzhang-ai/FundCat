alter table funds add column top_holdings varchar(4096) not null default '[]';

alter table fund_nav_history add column day_growth double not null default 0;
alter table fund_nav_history rename column trade_date to nav_date;

create table fund_nav_growth (
  id varchar(64) primary key,
  fund_code varchar(16) not null,
  nav_date date not null,
  week_growth double not null,
  month_growth double not null,
  year_growth double not null,
  updated_at timestamp not null,
  constraint uk_fund_nav_growth unique (fund_code, nav_date)
);

create index idx_fund_nav_growth_code_date on fund_nav_growth (fund_code, nav_date desc);

update fund_nav_history history
set day_growth = (
  select snapshot.day_growth
  from fund_nav_snapshots snapshot
  where snapshot.fund_code = history.fund_code
    and snapshot.nav_date = history.nav_date
)
where exists (
  select 1
  from fund_nav_snapshots snapshot
  where snapshot.fund_code = history.fund_code
    and snapshot.nav_date = history.nav_date
);

update funds fund
set top_holdings = (
  select snapshot.top_holdings
  from fund_nav_snapshots snapshot
  where snapshot.fund_code = fund.code
  order by snapshot.nav_date desc, snapshot.updated_at desc
  limit 1
)
where exists (
  select 1
  from fund_nav_snapshots snapshot
  where snapshot.fund_code = fund.code
);

insert into fund_nav_growth (id, fund_code, nav_date, week_growth, month_growth, year_growth, updated_at)
select
  snapshot.id,
  snapshot.fund_code,
  snapshot.nav_date,
  snapshot.week_growth,
  snapshot.month_growth,
  snapshot.year_growth,
  snapshot.updated_at
from fund_nav_snapshots snapshot
where not exists (
  select 1
  from fund_nav_growth growth
  where growth.fund_code = snapshot.fund_code
    and growth.nav_date = snapshot.nav_date
);

drop table if exists holding_positions;
drop table if exists fund_nav_snapshots;
