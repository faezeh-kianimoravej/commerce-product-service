# commerce-product-service

Product management microservice for the Commerce microservices project.

This service owns product catalog data and exposes APIs for creating, reading, updating, and deleting products. The initial implementation is intentionally focused on the product backend only; platform integrations will be added incrementally.

## Responsibilities

- Store product records
- Enforce basic product validation
- Prevent duplicate product SKUs
- Soft delete products by deactivating them
- Expose product CRUD REST endpoints
- Provide an actuator health endpoint
- Store timestamps using `Instant`

## Technology stack

- Java 21
- Maven
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Spring Boot Actuator
- Bean Validation
- Lombok
- Spring Boot Test
- H2 for repository tests only
- Springdoc OpenAPI

## Package structure

```text
com.faezeh.commerce.product
|-- controller
|-- dto
|-- entity
|-- exception
|-- repository
`-- service
```

## Run locally

The default configuration points to a local PostgreSQL database:

```bash
mvn spring-boot:run
```

Configuration can be supplied with environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Example defaults:

```bash
DB_URL=jdbc:postgresql://localhost:5432/product_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## Docker Compose

Start the service and its PostgreSQL database:

```bash
docker compose up --build
```

Run in the background:

```bash
docker compose up -d --build
```

Stop the containers:

```bash
docker compose down
```

Stop the containers and delete persisted database data:

```bash
docker compose down -v
```

Follow Product Service logs:

```bash
docker compose logs -f product-service
```

Useful URLs after startup:

- Product API: http://localhost:8080/api/products
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health

`product-db` is the PostgreSQL database dedicated to Product Service. Inside Docker Compose, Product Service connects to the database hostname `product-db`, not `localhost`.

PostgreSQL data is persisted in the named Docker volume `product-db-data`. The data survives `docker compose down` and is removed only with `docker compose down -v`.

## API endpoints

- `POST /api/products` - create a product
- `GET /api/products` - get all products
- `GET /api/products/{id}` - get a product by id
- `GET /api/products/sku/{sku}` - get a product by SKU
- `PUT /api/products/{id}` - update a product
- `DELETE /api/products/{id}` - deactivate a product with soft delete
- `GET /actuator/health` - health check
- `GET /v3/api-docs` - OpenAPI JSON
- `GET /swagger-ui.html` - Swagger UI

## Verify

```bash
mvn test
mvn package
```

Tests use mocks and H2 where appropriate, so PostgreSQL does not need to be running for test execution.

Product deletion is implemented as soft delete: `DELETE /api/products/{id}` sets `active` to `false` instead of permanently removing the row. Normal read and update operations only target active products.

Product timestamps are stored using `Instant` for UTC-friendly persistence across cloud and microservices environments.

## Planned later

- Spring Security
- Keycloak
- Kafka
- Azure
- Terraform
- Monitoring and tracing
