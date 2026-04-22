create table watchlist_groups (
  id varchar(128) primary key,
  user_id varchar(36) not null,
  group_name varchar(64) not null,
  created_at timestamp not null,
  constraint fk_watchlist_group_user foreign key (user_id) references auth_users(id),
  constraint uk_watchlist_group_user_name unique (user_id, group_name)
);

insert into watchlist_groups (id, user_id, group_name, created_at)
select distinct concat(user_id, ':', group_name), user_id, group_name, min(created_at)
from watchlist_items
group by user_id, group_name;

alter table watchlist_items add column group_id varchar(128);

update watchlist_items
set group_id = concat(user_id, ':', group_name);

alter table watchlist_items add constraint fk_watchlist_item_group foreign key (group_id) references watchlist_groups(id);

create unique index uk_watchlist_user_fund on watchlist_items (user_id, fund_code);
create index idx_watchlist_group_id on watchlist_items (group_id);

alter table watchlist_items drop column group_name;
