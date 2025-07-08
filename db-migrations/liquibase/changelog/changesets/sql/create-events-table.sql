create table public.events
(
    aggregate_id uuid   not null,
    revision     bigint not null,
    event_date   timestamp with time zone not null,
    event_type   varchar(255) not null,
    data         jsonb not null,
    primary key (aggregate_id, revision)
);
