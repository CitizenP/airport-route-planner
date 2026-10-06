# Airport Route Planner Constraints

- Use Java 25, Maven 3.10.x via the repository wrapper, and Spring Boot 4.1.1.
- Keep the root as a Maven multi-module project with group ID `com.airportrouteplanner`.
- Give each microservice ownership of its own data; never share database entities between services.
- Reference data owned by another service through identifiers such as IATA codes, never cross-service JPA relationships.
- Treat source Excel workbooks as design-time material only. Never commit them or add runtime Excel reading, importing, parsing, or upload functionality, including Apache POI.
- Add application datasets later as version-controlled database seed data.
- Calculate distances in Java from airport coordinates with the Haversine formula; do not import or store route distances as source data.
- Keep schedules in UTC. Represent airport local time with fixed UTC offsets; daylight-saving changes are out of scope.
- Do not add infrastructure or features before their task requests them, including databases, Docker, gateways, discovery, messaging, authentication, frontend code, domain entities, or routing algorithms.
