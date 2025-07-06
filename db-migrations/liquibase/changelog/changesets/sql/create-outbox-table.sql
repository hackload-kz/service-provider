create table public.outbox
(
    id             uuid primary key,
    topic          varchar(255),
    aggregate_id   varchar(255),
    aggregate_type varchar(255),
    payload        jsonb
);