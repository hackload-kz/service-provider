create table public.outbox
(
    id                 uuid primary key,
    topic              varchar(255) not null,
    aggregate_id       varchar(255) not null,
    aggregate_type     varchar(255) not null,
    aggregate_revision int          not null,
    event_type         varchar(255) not null,
    payload            jsonb        not null
);

create index idx_aggregate_type_revision on public.outbox (aggregate_type, aggregate_revision);
