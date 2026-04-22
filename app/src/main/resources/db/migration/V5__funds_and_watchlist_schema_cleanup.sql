alter table funds drop column category;
alter table funds drop column risk_level;
alter table funds drop column benchmark;
alter table funds drop column tag_line;
alter table funds drop column management_fee;
alter table funds drop column custody_fee;
alter table funds drop column purchase_fee;
alter table funds drop column status;

alter table watchlist_items add column group_name varchar(32) not null default '全部';

update watchlist_items
set group_name = coalesce(
  (
    select min(watchlist_item_groups.group_code)
    from watchlist_item_groups
    where watchlist_item_groups.watchlist_id = watchlist_items.id
  ),
  '全部'
);

drop table if exists watchlist_item_groups;
