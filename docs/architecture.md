# Planned Architecture

Airport Route Planner will use independently owned microservices. Only `airport-service` exists today; the other services and all domain functionality described here are planned for later work.

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
- Microservices must not share database entities.
- References to entities owned by another service use identifiers such as IATA codes, not JPA relationships.
- The source Excel workbooks are design-time material only. They are not runtime application inputs and must not be required, read, imported, parsed, uploaded, or committed to the repository.
- Application datasets will later be committed as version-controlled database seed data.
- Distances will not be stored as imported route data. They will eventually be calculated in Java from airport coordinates using the Haversine formula.
- Schedules will internally use UTC.
- Airport local times will use fixed UTC offsets. Daylight-saving changes are intentionally outside the project scope.
