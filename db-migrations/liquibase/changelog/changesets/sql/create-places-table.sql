create table places
(
    id      uuid primary key not null,
    row     int              not null,
    seat    int              not null,
    is_free bool             not null
);
