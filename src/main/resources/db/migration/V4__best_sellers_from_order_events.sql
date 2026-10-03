-- El ranking de lo más vendido se alimenta de los pedidos confirmados que publica pedidos (ADR-048).
create table best_seller (
    sku        varchar(64) primary key references product (sku),
    units_sold bigint      not null check (units_sold > 0)
);

create index best_seller_units_sold on best_seller (units_sold desc, sku);

-- El inbox de Nova, copiado de V1__create_inbox_message.sql de nova-outbox 0.1.0: cada evento procesado se
-- registra en la misma transacción que su suma, así que uno repetido no suma dos veces.
create table inbox_message (
    consumer     varchar(255) not null,
    source       varchar(255) not null,
    id           varchar(255) not null,
    processed_at timestamptz  not null,
    primary key (consumer, source, id)
);
