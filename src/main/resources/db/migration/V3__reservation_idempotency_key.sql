-- La reserva guarda la clave de la compra que la pidió (ADR-043): si el cliente repite la compra con la misma
-- Idempotency-Key, el catálogo devuelve la misma reserva en lugar de apartar stock otra vez. La clave vale por
-- cliente, como en pedidos; una reserva sin clave no choca con ninguna.
alter table reservation add column idempotency_key varchar(255);

create unique index reservation_customer_key on reservation (customer_id, idempotency_key)
    where idempotency_key is not null;
