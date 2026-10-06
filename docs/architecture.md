# Planned Architecture

Airport Route Planner uses independently owned microservices with a dedicated presentation/BFF application. All five planned applications now exist. `route-service` implements the validated graph foundation, three static optimization modes, and time-dependent FASTEST routing. `web-app` implements the airport-map frontend foundation; route calculation wiring and route drawing remain future work.

## Planned service responsibilities

### `airport-service`

Airports, coordinates, runway counts, and fixed UTC offsets.

### `fleet-service`

Flight companies and aircraft types. A flight company's base airport is represented by an IATA code, not by an entity or database relationship owned by `fleet-service`.

### `flight-service`

Owns 616 persisted CompanyRoute records and derives non-persisted FlightLeg values. Every CompanyRoute produces exactly one outbound leg and one return leg, yielding 1,232 raw daily scheduled legs. The return reverses the airports and departs 12 hours after the outbound time, with schedules repeating every 24 hours.

For the complete adjusted schedule, `flight-service` retrieves runway counts from `airport-service` with one `GET /api/airports` request. Scheduling models departures only. Requests are processed deterministically by scheduled UTC time, company code, route number, then direction, with OUTBOUND before RETURN. Each runway supplies one departure-capacity unit per slot; excess departures move forward in five-minute increments until capacity is available. Adjusted times, day offsets, and delays are derived and never persisted. The current reference dataset yields 14 delayed departures across 11 origin airports.

The read-only flight APIs are:

- `GET /api/company-routes`
- `GET /api/company-routes/{id}`
- `GET /api/company-routes/{id}/flight-legs` for the two raw legs of one route
- `GET /api/flight-legs` for the complete runway-adjusted daily schedule

### `route-service`

Owns no database. When `GET /api/route-graph/summary` is requested, it retrieves airports, aircraft types, flight companies, and the adjusted global FlightLeg schedule through the other services' HTTP APIs. It then validates all cross-service identifiers and aircraft ranges and builds an immutable in-memory directed multigraph.

Airports are vertices and adjusted FlightLegs are directed edges. Parallel flights between the same airports remain separate edges. Aircraft performance data is retained once in a catalogue keyed by aircraft-type ID instead of being duplicated into every edge. Great-circle distance is calculated in Java with the Haversine formula and the 6371.0088 km mean Earth radius; imported or spreadsheet route distances are never used.

One reusable ordinary Dijkstra implementation provides three static optimization modes through separate edge-weight strategies:

- SHORTEST minimizes Haversine kilometres.
- CHEAPEST minimizes estimated whole-aircraft fuel litres (`distanceKm * fuelConsumptionLitresPerKm`). It is not a monetary-price calculation.
- ECOLOGICAL minimizes estimated fuel litres per passenger (`distanceKm / 100 * fuelConsumptionPerPassenger`).

Static optimization ignores schedule timing, but selected route details retain the adjusted departure metadata for explanation. FASTEST uses a separate time-dependent earliest-arrival Dijkstra implementation: each vertex label is the earliest known absolute UTC arrival, and each relaxation uses the next recurring adjusted departure at or after that arrival. Schedules repeat every 24 hours, and the adjusted runway departure—not the original scheduled clock—controls flight availability.

FASTEST requires an origin-local departure date and time. `route-service` converts it to an absolute UTC instant with the origin airport's fixed UTC offset; no IANA time zones or daylight-saving rules are used. Flight duration is theoretical cruise time (`Haversine distance / aircraft cruise speed`) at nanosecond precision. Total journey time includes initial waiting, connection waiting, and cruise duration. Version 1 permits exact-time connections with a zero-minute minimum connection time and does not model taxiing, climb/descent, arrival runway capacity, or aircraft turnaround. All route calculations then remain in UTC; local-time presentation is future `web-app` responsibility.

The read-only diagnostic and calculation APIs are:

- `GET /api/route-graph/summary`
- `POST /api/routes/calculate` for SHORTEST, CHEAPEST, ECOLOGICAL, and FASTEST

### `web-app`

Runs on port 8080 and owns no database. It uses Spring MVC and Thymeleaf for server-rendered pages, with framework-free browser JavaScript and CSS. Leaflet and OpenStreetMap provide the interactive world map.

The browser communicates only with `web-app`. For airport display data, the browser requests `GET /api/ui/airports`; web-app then retrieves `GET /api/airports` from `airport-service` through an externally configurable server-side client. This Backend-for-Frontend boundary avoids exposing backend service URLs or requiring browser-to-service CORS configuration. The upstream call is lazy, so web-app starts and continues serving its main page when airport-service is unavailable.

The current map renders all 161 runtime airport records, marker popups, and origin/destination selectors. Route-type and FASTEST date/time controls are present as the future interaction shell, but web-app does not yet call `route-service`, submit route calculations, draw routes, or display route-result cards.

## Planned default ports

| Application | Default port | Current status |
| --- | ---: | --- |
| `web-app` | 8080 | Foundation implemented |
| `airport-service` | 8081 | Implemented |
| `fleet-service` | 8082 | Implemented |
| `flight-service` | 8083 | Implemented |
| `route-service` | 8084 | Foundation implemented |

## Architectural rules

- Each microservice owns its own data.
- `airport-service`, `fleet-service`, and `flight-service` use separate embedded H2 databases for development and testing. This is a current-stage implementation choice, not a requirement for future deployment databases. `route-service` has no database and constructs its graph in memory from service APIs.
- Flyway owns database schema creation, while Hibernate validates the migrated schema.
- Microservices must not share database entities.
- References to entities owned by another service use identifiers such as IATA codes, not JPA relationships.
- The source Excel workbooks are design-time material only. They are not runtime application inputs and must not be required, read, imported, parsed, uploaded, or committed to the repository.
- Airport, fleet, and CompanyRoute reference data are committed as version-controlled Flyway seed migrations. These provide 161 airports, 23 aircraft types, 77 flight companies, and 616 company routes.
- CompanyRoute stores company, airport, and aircraft-type identifiers only. It does not store or relate to entities owned by other services.
- FlightLeg and AdjustedFlightLeg are derived and never persisted. Outbound departure is the stored CompanyRoute schedule; return departure is 12 hours later using daily UTC time wrapping.
- Runway counts remain owned by `airport-service`; `flight-service` obtains them through the airport HTTP API when an adjusted schedule is requested.
- Runway scheduling covers departures only, uses deterministic five-minute allocation slots, and never updates CompanyRoute records.
- Distances are not stored as imported route data. `route-service` calculates them in Java from airport coordinates using the Haversine formula.
- `route-service` validates that every graph edge references known airports, aircraft, and companies, and that its distance does not exceed the referenced aircraft's maximum range.
- Schedules will internally use UTC.
- Airport local times will use fixed UTC offsets. Daylight-saving changes are intentionally outside the project scope.
- Static routes use ordinary Dijkstra with metric-specific weights. FASTEST uses time-dependent earliest-arrival Dijkstra and never persists route results.
- `web-app` is the browser-facing BFF. Browsers do not call backend microservices directly, and web-app does not duplicate backend-owned reference datasets.
