create table product (
    sku      varchar(64) primary key,
    name     varchar(200)   not null,
    price    numeric(12, 2) not null check (price > 0),
    currency varchar(3)     not null,
    stock    integer        not null check (stock >= 0),
    version  bigint         not null default 0
);

create table reservation (
    id          uuid primary key,
    customer_id varchar(64) not null,
    status      varchar(16) not null,
    currency    varchar(3)  not null,
    created_at  timestamp with time zone not null,
    expires_at  timestamp with time zone not null,
    version     bigint      not null
);

-- Lo apartado de un producto se calcula con las reservas que siguen vigentes.
create index reservation_held on reservation (status, expires_at);

create table reservation_line (
    id             bigint generated always as identity primary key,
    reservation_id uuid           not null references reservation (id) on delete cascade,
    sku            varchar(64)    not null references product (sku),
    quantity       integer        not null check (quantity > 0),
    unit_price     numeric(12, 2) not null check (unit_price > 0)
);

create index reservation_line_sku on reservation_line (sku);
