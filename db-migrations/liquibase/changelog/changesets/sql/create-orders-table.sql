create table public.orders
(
    id           uuid primary key         not null,
    status       varchar(255)             not null,
    places_count int                      not null,
    started_at   timestamp with time zone not null,
    updated_at    timestamp with time zone not null,
    revision     bigint                   not null
);
