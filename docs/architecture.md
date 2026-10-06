# Planned Architecture

Airport Route Planner uses independently owned microservices. `airport-service`, `fleet-service`, and `flight-service` currently exist with their initial domains and read-only APIs. The other services and routing functionality described here are planned for later work.

## Planned service responsibilities

### `airport-service`

Airports, coordinates, runway counts, and fixed UTC offsets.

### `fleet-service`

Flight companies and aircraft types. A flight company's base airport is represented by an IATA code, not by an entity or database relationship owned by `fleet-service`.

### `flight-service`

Owns 616 persisted CompanyRoute records and derives non-persisted FlightLeg values. Every CompanyRoute produces exactly one outbound leg and one return leg, yielding 1,232 daily scheduled legs before runway adjustment. The return reverses the airports and departs 12 hours after the outbound time, with schedules repeating every 24 hours. Runway-adjusted departure scheduling is not yet implemented.

### `route-service`

Graph construction, Haversine distance calculation, and routing algorithms.

### `web-app`

Spring MVC, Thymeleaf, and the interactive map interface.

## Planned default ports

| Application | Default port | Current status |
| --- | ---: | --- |
| `web-app` | 8080 | Planned |
| `airport-service` | 8081 | Implemented |
| `fleet-service` | 8082 | Implemented |
| `flight-service` | 8083 | Implemented |
| `route-service` | 8084 | Planned |

## Architectural rules

- Each microservice owns its own data.
- `airport-service`, `fleet-service`, and `flight-service` use separate embedded H2 databases for development and testing. This is a current-stage implementation choice, not a requirement for future deployment databases.
- Flyway owns database schema creation, while Hibernate validates the migrated schema.
- Microservices must not share database entities.
- References to entities owned by another service use identifiers such as IATA codes, not JPA relationships.
- The source Excel workbooks are design-time material only. They are not runtime application inputs and must not be required, read, imported, parsed, uploaded, or committed to the repository.
- Airport, fleet, and CompanyRoute reference data are committed as version-controlled Flyway seed migrations. These provide 161 airports, 23 aircraft types, 77 flight companies, and 616 company routes.
- CompanyRoute stores company, airport, and aircraft-type identifiers only. It does not store or relate to entities owned by other services.
- FlightLeg is derived and never persisted. Outbound departure is the stored CompanyRoute schedule; return departure is 12 hours later using daily UTC time wrapping.
- Runway-adjusted departure times remain future `flight-service` functionality.
- Distances will not be stored as imported route data. They will eventually be calculated in Java from airport coordinates using the Haversine formula.
- Schedules will internally use UTC.
- Airport local times will use fixed UTC offsets. Daylight-saving changes are intentionally outside the project scope.
