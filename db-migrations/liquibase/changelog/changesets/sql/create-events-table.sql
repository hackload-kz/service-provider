create table public.events
(
    aggregate_id uuid   not null,
    revision     bigint not null,
    event_type   varchar(255),
    data         jsonb,
    primary key (aggregate_id, revision)
);
