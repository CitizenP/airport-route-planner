# Planned Architecture

Airport Route Planner will use independently owned microservices. Only `airport-service` exists today; its Airport domain and read-only REST API are implemented. The other services and routing functionality described here are planned for later work.

## Planned service responsibilities

### `airport-service`

Airports, coordinates, runway counts, and fixed UTC offsets.

### `fleet-service`

Flight companies and aircraft types.

### `flight-service`

Company routes, schedules, generated return flights, and runway scheduling.

### `route-service`

Graph construction, Haversine distance calculation, and routing algorithms.

### `web-app`

Spring MVC, Thymeleaf, and the interactive map interface.

## Architectural rules

- Each microservice owns its own data.
- `airport-service` currently uses an embedded H2 database for development and testing. This is a current-stage implementation choice, not a requirement for future deployment databases.
- Flyway owns database schema creation, while Hibernate validates the migrated schema.
- Microservices must not share database entities.
- References to entities owned by another service use identifiers such as IATA codes, not JPA relationships.
- The source Excel workbooks are design-time material only. They are not runtime application inputs and must not be required, read, imported, parsed, uploaded, or committed to the repository.
- Airport reference data is committed as a version-controlled Flyway seed migration. Datasets for future services will follow the same ownership rule when those services are implemented.
- Distances will not be stored as imported route data. They will eventually be calculated in Java from airport coordinates using the Haversine formula.
- Schedules will internally use UTC.
- Airport local times will use fixed UTC offsets. Daylight-saving changes are intentionally outside the project scope.
