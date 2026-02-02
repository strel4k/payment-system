# C4 Context Diagram — Payment System

Диаграмма уровня 1: общий контекст системы и взаимодействие с внешними акторами.

```mermaid
C4Context
    title System Context diagram for Payment System

    Person(user, "User", "End user registering<br/>and using the system")
    
    System_Boundary(payment_system, "Payment System") {
        System(individuals_api, "Individuals API", "Orchestrator service:<br/>handles authentication,<br/>user registration,<br/>and coordinates person data")
        System(person_service, "Person Service", "Manages person entities,<br/>addresses, and individuals<br/>(CRUD + audit)")
    }
    
    System_Ext(keycloak, "Keycloak", "Authentication &<br/>Authorization server<br/>(OAuth2/JWT)")
    
    System_Ext(observability, "Observability Stack", "Prometheus, Grafana,<br/>Loki, Tempo<br/>(metrics, logs, traces)")
    
    System_Ext(nexus, "Nexus OSS", "Artifact repository<br/>for person-service-client")

    Rel(user, individuals_api, "Uses", "HTTPS/REST")
    Rel(individuals_api, person_service, "Creates/updates person data", "HTTP/REST")
    Rel(individuals_api, keycloak, "Registers users,<br/>authenticates", "HTTP/Admin API")
    
    Rel(individuals_api, observability, "Exports metrics,<br/>logs, traces", "OTLP/HTTP")
    Rel(person_service, observability, "Exports metrics,<br/>logs, traces", "OTLP/HTTP")
    
    Rel(individuals_api, nexus, "Fetches person-service-client", "Maven HTTP")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="2")
```

## Описание компонентов

### User
Конечный пользователь, который:
- Регистрируется через `/v1/auth/registration`
- Логинится через `/v1/auth/login`
- Получает JWT токены от Keycloak

### Individuals API
**Роль**: Оркестратор  
**Технологии**: Spring Boot WebFlux, Spring Security OAuth2  
**Функции**:
- Принимает запросы от пользователей
- Создаёт person через person-service
- Регистрирует пользователя в Keycloak
- Возвращает JWT токены

### Person Service
**Роль**: Data service  
**Технологии**: Spring Boot Web, Spring Data JPA, Hibernate Envers  
**Функции**:
- CRUD для Person, Address, Individual
- Транзакционное управление данными
- Аудит всех изменений через Envers

### Keycloak
**Роль**: Identity Provider  
**Функции**:
- Хранит credentials пользователей
- Выдаёт JWT токены
- Управляет realm "individuals"

### Observability Stack
**Компоненты**:
- **Prometheus**: сбор метрик через Actuator
- **Grafana**: визуализация метрик и traces
- **Loki**: хранение логов (через Promtail)
- **Tempo**: distributed tracing (OpenTelemetry)

### Nexus OSS
**Роль**: Artifact Repository  
**Функции**:
- Хранит `person-service-client` JAR (автогенерированный клиент)
- Предоставляет Maven repository для зависимостей