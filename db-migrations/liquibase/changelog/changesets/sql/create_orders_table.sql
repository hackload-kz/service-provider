create table public.orders
(
    id           uuid                     not null,
    status       varchar(255)             not null,
    places_count int                      not null,
    started_at   timestamp with time zone not null,
    update_at    timestamp with time zone not null,
    submitted_at timestamp with time zone,
    revision     bigint                   not null
);
