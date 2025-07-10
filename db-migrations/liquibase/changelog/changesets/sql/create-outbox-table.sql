create table public.outbox
(
    id                 uuid primary key,
    topic              varchar(255) not null,
    aggregate_id       varchar(255) not null,
    aggregate_revision bigint       not null,
    aggregate_type     varchar(255) not null,
    event_type         varchar(255) not null,
    payload            jsonb        not null
);
