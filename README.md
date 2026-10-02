# plaza-catalog

El catálogo de [Plaza](https://github.com/ahincho/nova-plaza-01-shared-platform), en Quarkus: los productos, sus
precios, su stock y las **reservas**, el stock apartado para una compra que todavía no terminó. Es el primer paso de
la compra que orquesta el BFF: reserva, y el precio de cada línea lo pone el catálogo, nunca el cliente.

Las decisiones están en [ADR-043](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-043-plaza-la-plataforma-de-compras.md)
y [ADR-056](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-056-plaza-fase-1-catalogo-y-pagos-en-nestjs.md).

## API

| Método | Ruta | Qué hace | Errores |
|---|---|---|---|
| `GET` | `/v1/products` | los productos, por código, en páginas por cursor | 400 si el cursor o el límite no sirven |
| `GET` | `/v1/products/{sku}` | un producto, con su precio y lo que se puede reservar ahora | 404 `PRODUCT_NOT_FOUND` |
| `POST` | `/v1/reservations` | aparta el stock de una lista de productos y devuelve los precios del momento | 409 `OUT_OF_STOCK`, 404 `PRODUCT_NOT_FOUND`, 422 `MIXED_CURRENCIES` |
| `POST` | `/v1/reservations/{id}/confirm` | el stock sale del almacén | 409 `RESERVATION_EXPIRED` o `RESERVATION_RELEASED`, 404 |
| `POST` | `/v1/reservations/{id}/release` | el stock vuelve a estar disponible | 409 `RESERVATION_CONFIRMED`, 404 |

Cada respuesta llega en el sobre de Nova, y cada error con su código y su capa
([ADR-031](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-031-modulo-de-errores-por-capas-con-trazabilidad.md)),
igual que en pedidos y en NestJS. La reserva lleva el cliente en `X-Customer-Id`, que pone el BFF.

- **Una reserva vence a los diez minutos.** Una vencida ya no aparta stock aunque nadie la libere, así que una
  compensación que se pierde no deja stock bloqueado.
- **Confirmar y liberar son idempotentes:** repetir la misma operación devuelve la misma reserva.
- **Dos compras del último stock no se pisan:** cada reserva toma sus productos en la base, ordenados por código, y
  la segunda recibe el 409.
- **El listado usa el mismo cursor que pedidos** ([ADR-054](https://github.com/ahincho/nova-shared-01-docs/blob/main/adrs/shared/ADR-054-persistencia-reutilizable-con-paginacion-por-cursor.md)):
  `?limit=`, 20 por defecto y 100 como máximo, y `?cursor=`, con `items`, `nextCursor` y `hasNext`. El cursor lo
  escribe el núcleo puro de `nova-persistence`, que no depende de ningún framework, así que es el mismo en Spring y
  en Quarkus.

## La arquitectura

Hexagonal, con un contexto acotado, `inventory`, como la plantilla de Quarkus de Nova y el estilo `acl` de NestJS:

| Paquete | Qué hay |
|---|---|
| `inventory/domain` | `Product`, `Reservation` y sus líneas, sin framework |
| `inventory/exception` | los errores del dominio, `CatalogErrors` |
| `inventory/port/in` | los casos de uso: `ProductCatalog` y `StockReservations` |
| `inventory/port/out` | lo que el núcleo necesita de afuera: `ProductStore` y `ReservationStore` |
| `inventory/service` | la implementación de los casos de uso, con su transacción |
| `inventory/adapter/in/web` | los recursos REST, con `request/` y `response/` |
| `inventory/adapter/out/persistence` | las entidades JPA y los puertos de salida sobre Panache |

`ArchitectureTest` comprueba las reglas, con los nombres de las de NestJS: el dominio no conoce nada, los bordes no
se conocen entre sí y los contratos web no salen del adaptador web.

## Lo que usa de Nova

| Pieza | Para qué |
|---|---|
| `nova-quarkus-bom` 4.2.1 | Quarkus 3.33.3.3 y las extensiones de Nova en versiones que se conocen |
| `nova-api-standard-quarkus-extension` | el sobre y los errores por capas |
| `nova-secrets-quarkus-extension` y `nova-secrets-vault` | las credenciales de la base salen de Vault, del secreto `plaza/catalog/db` |
| `nova-persistence` | el contrato de la paginación por cursor |

Las trazas, los logs y las métricas salen por OTLP con `quarkus-opentelemetry`.

## Correrlo en local

Levantar Postgres y Vault desde [`nova-plaza-01-shared-platform`](https://github.com/ahincho/nova-plaza-01-shared-platform)
con `docker compose up -d --wait`, y después:

```bash
export GITHUB_ACTOR=ahincho GITHUB_TOKEN=$(gh auth token) VAULT_ADDR=http://localhost:8200 VAULT_TOKEN=plaza-local-root
./gradlew quarkusDev
```

Escucha en el puerto 8082. Flyway crea las tablas y carga doce productos de ejemplo. Las dependencias de Nova están
en GitHub Packages, así que Gradle necesita `GITHUB_ACTOR` y un `GITHUB_TOKEN` con `read:packages`.

## Pruebas

```bash
./gradlew build
```

| Prueba | Qué cubre | Necesita |
|---|---|---|
| `DomainTest` | el stock disponible y el vencimiento de una reserva | nada |
| `StockReservationServiceTest` | los casos de uso con los puertos en memoria: reservar, confirmar, liberar, vencer y cada error | nada |
| `ArchitectureTest` | las reglas del hexágono | nada |
| `CatalogApiTest` | la API entera contra Postgres, con el sobre de Nova | Docker, por Dev Services |

## Licencia

[Eclipse Public License 2.0](LICENSE).
