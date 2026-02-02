# C4 Container Diagram — Payment System

Диаграмма уровня 2: детализация внутренних контейнеров системы.

```mermaid
C4Container
    title Container diagram for Payment System

    Person(user, "User", "End user")

    Container_Boundary(individuals_api_boundary, "Individuals API") {
        Container(individuals_api_app, "Individuals API Application", "Spring Boot WebFlux", "Handles REST requests,<br/>orchestrates registration flow")
        ContainerDb(individuals_api_config, "Configuration", "application.yml", "Keycloak settings,<br/>person-service URL")
    }

    Container_Boundary(person_service_boundary, "Person Service") {
        Container(person_service_app, "Person Service Application", "Spring Boot Web", "CRUD for Person,<br/>Address, Individual")
        ContainerDb(person_db, "Person Database", "PostgreSQL", "person schema:<br/>users, individuals,<br/>addresses, countries")
    }

    Container_Boundary(keycloak_boundary, "Keycloak") {
        Container(keycloak_app, "Keycloak Server", "Keycloak 26.2", "OAuth2/OIDC provider")
        ContainerDb(keycloak_db, "Keycloak Database", "PostgreSQL", "realms, users,<br/>credentials")
    }

    Container_Boundary(observability_boundary, "Observability") {
        Container(prometheus, "Prometheus", "Prometheus", "Metrics storage")
        Container(grafana, "Grafana", "Grafana 10.3", "Dashboards & visualization")
        Container(loki, "Loki", "Loki 2.9", "Log aggregation")
        Container(tempo, "Tempo", "Tempo 2.6", "Distributed tracing")
        Container(promtail, "Promtail", "Promtail 2.9", "Log shipper")
    }

    Container(nexus, "Nexus OSS", "Nexus 3.75", "Maven repository")

    Rel(user, individuals_api_app, "POST /v1/auth/registration", "HTTPS/JSON")
    
    Rel(individuals_api_app, person_service_app, "POST /v1/persons", "HTTP/JSON")
    Rel(individuals_api_app, keycloak_app, "POST /admin/realms/.../users", "HTTP/JSON")
    Rel(individuals_api_app, keycloak_app, "POST /realms/.../protocol/openid-connect/token", "HTTP/Form")
    
    Rel(person_service_app, person_db, "JPA queries", "JDBC/SQL")
    Rel(keycloak_app, keycloak_db, "Stores users", "JDBC/SQL")
    
    Rel(individuals_api_app, prometheus, "Exposes /actuator/prometheus", "HTTP")
    Rel(person_service_app, prometheus, "Exposes /actuator/prometheus", "HTTP")
    
    Rel(individuals_api_app, tempo, "Exports traces", "OTLP/HTTP:4318")
    Rel(person_service_app, tempo, "Exports traces", "OTLP/HTTP:4318")
    
    Rel(promtail, individuals_api_app, "Reads logs", "Docker Socket")
    Rel(promtail, person_service_app, "Reads logs", "Docker Socket")
    Rel(promtail, loki, "Ships logs", "HTTP")
    
    Rel(grafana, prometheus, "Queries metrics", "HTTP")
    Rel(grafana, loki, "Queries logs", "HTTP")
    Rel(grafana, tempo, "Queries traces", "HTTP")
    
    Rel(individuals_api_app, nexus, "Fetches person-service-client JAR", "Maven/HTTP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="2")
```

## Технологический стек

### Individuals API
- **Framework**: Spring Boot 3.5.0 WebFlux (Reactive)
- **Security**: Spring Security OAuth2 Resource Server
- **Client**: WebClient для вызовов person-service
- **Observability**: Micrometer, OpenTelemetry Java Agent
- **Port**: 8081

### Person Service
- **Framework**: Spring Boot 3.5.0 Web (Blocking)
- **ORM**: Spring Data JPA + Hibernate
- **Audit**: Hibernate Envers
- **Migrations**: Flyway
- **Observability**: Micrometer, OpenTelemetry Java Agent
- **Port**: 8082

### Databases
- **person-postgres**: PostgreSQL 16 (port 5434)
- **keycloak-postgres**: PostgreSQL 17 (port 5433)

### Observability
- **Prometheus**: Port 9090
- **Grafana**: Port 3000
- **Loki**: Port 3100
- **Tempo**: Port 3200, OTLP HTTP 4318, OTLP gRPC 4317

### Artifact Repository
- **Nexus OSS**: Port 8091 (UI), 8092 (Docker registry)