# Architecture Overview — Payment System

Архитектурная документация микросервисной системы управления пользователями.

---

## 📊 Архитектурные диаграммы

### C4 Model Diagrams

Диаграммы созданы по методологии [C4 Model](https://c4model.com) для визуализации архитектуры на разных уровнях абстракции.

#### 1. Context Diagram (Уровень 1)
**Файл**: [docs/architecture/diagrams/context-diagram.svg](docs/architecture/diagrams/context-diagram.svg)

Показывает общую картину системы и взаимодействие с внешними компонентами.

![Context Diagram](./docs/architecture/diagrams/context-diagram.svg)

**Ключевые компоненты**:
- 👤 **User** — конечный пользователь
- 🌐 **Individuals API** — оркестратор (аутентификация, регистрация)
- 💾 **Person Service** — управление данными пользователей
- 🔐 **Keycloak** — OAuth2/JWT сервер
- 📊 **Observability Stack** — Prometheus, Grafana, Loki, Tempo
- 📦 **Nexus OSS** — репозиторий Maven артефактов

**Подробнее**: [docs/architecture/c4-context.md](docs/architecture/c4-context.md)

---

#### 2. Container Diagram (Уровень 2)
**Файл**: [docs/architecture/diagrams/container-diagram.svg](docs/architecture/diagrams/container-diagram.svg)

Детализация внутренних контейнеров, технологий и баз данных.

![Container Diagram](docs/architecture/diagrams/container-diagram.svg)

**Технологический стек**:

| Компонент | Технология | Порт |
|-----------|-----------|------|
| **Individuals API** | Spring Boot WebFlux (Reactive) | 8081 |
| **Person Service** | Spring Boot Web + JPA | 8082 |
| **Person DB** | PostgreSQL 16 | 5434 |
| **Keycloak** | Keycloak 26.2 | 8080 |
| **Keycloak DB** | PostgreSQL 17 | 5433 |
| **Nexus OSS** | Nexus 3.75.1 | 8091 |
| **Prometheus** | Prometheus | 9090 |
| **Grafana** | Grafana 10.3 | 3000 |
| **Loki** | Loki 2.9 | 3100 |
| **Tempo** | Tempo 2.6 | 3200 |

**Подробнее**: [docs/architecture/c4-container.md](docs/architecture/c4-container.md)

---

### Sequence Diagrams

#### User Registration Flow
**Файл**: [docs/architecture/diagrams/sequence-registration.svg](docs/architecture/diagrams/sequence-registration.svg)

Полная последовательность шагов при регистрации пользователя с distributed tracing.

![Sequence Diagram](docs/architecture/diagrams/sequence-registration.svg)

**Основные шаги**:
1. User → Individuals API: `POST /v1/auth/registration`
2. API генерирует `trace_id` (OpenTelemetry)
3. API → Person Service: создание Person (транзакционно)
4. Person Service → PostgreSQL: `INSERT users`, `INSERT individuals`
5. API → Keycloak: регистрация пользователя с `user_uid` attribute
6. API → Keycloak: установка пароля
7. API → Keycloak: генерация JWT токенов (access + refresh)
8. API → Tempo: завершение span с полной трассой
9. API → User: возврат JWT tokens

**Подробнее**: [docs/architecture/sequence-registration.md](docs/architecture/sequence-registration.md)

---

## 🏗️ Архитектурные решения

### Микросервисная архитектура

**Разделение ответственности**:
- **individuals-api** — оркестратор, обрабатывает пользовательские запросы
- **person-service** — data service, управляет персональными данными
- **Keycloak** — централизованная аутентификация

**Преимущества**:
- ✅ Независимое масштабирование сервисов
- ✅ Изоляция отказов (failure isolation)
- ✅ Разные технологические стеки (WebFlux vs Web)
- ✅ Независимые циклы разработки

### Reactive vs Blocking

**individuals-api (WebFlux)**:
- Reactive, non-blocking I/O
- Высокая пропускная способность
- Подходит для I/O-intensive операций (HTTP calls к Person Service и Keycloak)

**person-service (Web)**:
- Blocking, традиционный Spring MVC
- Проще в разработке и отладке
- Достаточно для database-heavy операций

### Database per Service

Каждый сервис имеет свою БД:
- **person-service** → `person_db` (PostgreSQL)
- **keycloak** → `keycloak_db` (PostgreSQL)

**Преимущества**:
- ✅ Независимое управление схемой
- ✅ Изоляция данных
- ✅ Возможность выбора разных СУБД

### Distributed Tracing

**OpenTelemetry Java Agent**:
- Автоматическая инструментация HTTP calls
- Минимальные изменения в коде
- Trace ID в логах для корреляции

**Tempo**:
- Хранение и индексация traces
- Интеграция с Grafana
- Поиск по service name, trace ID, duration

**Визуализация**:
```
User Request (trace_id: abc123)
├─ individuals-api [POST /registration] (200ms)
│  ├─ person-service [POST /persons] (80ms)
│  │  └─ PostgreSQL [INSERT users] (20ms)
│  ├─ keycloak [POST /users] (50ms)
│  └─ keycloak [POST /token] (40ms)
```

### Observability Stack

**Три столпа observability**:

1. **Metrics** (Prometheus + Grafana)
    - JVM metrics (heap, threads, GC)
    - HTTP metrics (request rate, latency, errors)
    - Database connection pool metrics

2. **Logs** (Loki + Promtail + Grafana)
    - Structured JSON logging
    - Correlation via trace_id
    - Centralized aggregation

3. **Traces** (Tempo + OpenTelemetry + Grafana)
    - Distributed request tracing
    - Service dependency mapping
    - Performance bottleneck identification

### Artifact Management

**Nexus OSS**:
- Maven repository для `person-service-client`
- Автогенерированный клиент через OpenAPI
- Централизованное управление зависимостями
- Caching для ускорения сборки

**Workflow**:
```
1. OpenAPI spec (person-service.yml)
2. openapi-generator → Java client
3. Gradle publish → Nexus
4. individuals-api → fetch from Nexus
```

### Security

**OAuth2 + JWT**:
- Centralized authentication (Keycloak)
- Stateless JWT tokens (RS256)
- Token rotation (refresh tokens)
- Role-based access control (RBAC)

**Data linking**:
- Keycloak хранит `user_uid` как custom attribute
- person-service использует `user_uid` как primary key
- Связь через UUID, не через email

### Database Audit

**Hibernate Envers**:
- Автоматическое отслеживание изменений
- Таблицы `*_aud` для каждой entity
- Полная история изменений (who, when, what)
- Compliance-ready (GDPR, audit trails)

---

## 📐 Архитектурные паттерны

### 1. API Gateway Pattern
**individuals-api** выступает как API Gateway:
- Единая точка входа для клиентов
- Роутинг к внутренним сервисам
- Аутентификация и авторизация

### 2. Backend for Frontend (BFF)
**individuals-api** адаптирует ответы:
- Агрегация данных из Person Service и Keycloak
- Трансформация DTO для клиентов
- Упрощение клиентской логики

### 3. Database per Service
Изолированные базы данных:
- Независимое управление схемой
- Избежание tight coupling
- Возможность выбора разных СУБД

### 4. Saga Pattern (Choreography)
Регистрация пользователя как распределённая транзакция:
1. Create Person в person-service
2. Create User в Keycloak
3. Set Password в Keycloak
4. Generate Tokens в Keycloak

**Обработка ошибок**: компенсирующие транзакции при сбое на любом шаге.


---

## 🔄 Data Flow

### Registration Flow
```
┌──────┐                                                      
│ User │                                                      
└──┬───┘                                                      
   │ 1. POST /v1/auth/registration                           
   ▼                                                          
┌─────────────────┐                                          
│ Individuals API │                                          
└────┬───┬───┬────┘                                          
     │   │   │                                               
     │   │   │ 2. POST /v1/persons                          
     │   │   ▼                                               
     │   │ ┌──────────────┐    3. INSERT                    
     │   │ │Person Service├────────────► ┌──────────┐        
     │   │ └──────────────┘              │Person DB │        
     │   │                               └──────────┘        
     │   │                                                    
     │   │ 4. POST /admin/users                             
     │   ▼                                                    
     │ ┌─────────┐    5. INSERT                             
     │ │Keycloak ├────────────► ┌──────────┐                 
     │ └────┬────┘              │Keycloak  │                 
     │      │                   │   DB     │                 
     │      │ 6. /reset-password└──────────┘                 
     │      │                                                 
     │      │ 7. /token                                      
     │      ▼                                                 
     │   JWT tokens                                          
     │                                                        
     │ 8. trace → Tempo                                      
     ▼                                                        
  200 OK                                                      
  {access_token, refresh_token}                              
```

---

## Error Handling & Compensating Transactions

### Проблема распределённых транзакций

В микросервисной архитектуре невозможно использовать классические ACID транзакции между сервисами:
- **Person Service** имеет свою PostgreSQL БД
- **Keycloak** имеет свою PostgreSQL БД
- Нет distributed transaction coordinator (2PC не используется)

### Решение: Compensating Transactions (Saga Pattern)

При ошибке на одном из шагов выполняется **компенсирующая транзакция** для отката изменений.

**Визуализация:** См. [Sequence Diagram: Registration Flow](./docs/architecture/sequence-registration.md#error-path-компенсирующая-транзакция)

#### Пример: User Registration Flow

**Happy Path:**
1. ✅ Создать Person в `person-service`
2. ✅ Создать User в Keycloak
3. ✅ Установить пароль
4. ✅ Получить JWT токены

**Error Path (Keycloak fails):**
1. ✅ Создать Person в `person-service`
2. ❌ Создать User в Keycloak → **409 Conflict**
3. 🔄 **ROLLBACK**: Удалить Person из БД через `DELETE /v1/persons/{id}`
4. ❌ Вернуть ошибку пользователю

### Реализация в коде
```java
// UserService.java
public Mono<TokenResponse> register(UserRegistrationRequest request) {
    return personServiceClient.createPerson(request)
        .flatMap(personResponse -> {
            String userId = personResponse.getUserId().toString();
            
            return keycloakClient.createUserWithAttribute(email, password, userId)
                .onErrorResume(keycloakError -> {
                    // Компенсирующая транзакция
                    RuntimeException registrationError = new RuntimeException(
                        "Registration failed: " + keycloakError.getMessage(),
                        keycloakError  // Сохраняем оригинальную ошибку
                    );
                    
                    return personServiceClient.deletePerson(personResponse.getUserId())
                        .doOnSuccess(v -> log.info("✅ Rollback successful"))
                        .doOnError(deleteError -> 
                            log.error("🚨 CRITICAL: Rollback failed! Manual cleanup required")
                        )
                        .thenReturn(true)
                        .onErrorReturn(false)
                        .<Void>flatMap(deleteSucceeded -> Mono.error(registrationError));
                })
                .then(keycloakClient.login(email, password));
        });
}
```

### Границы компенсации

**Откат выполняется только до создания Keycloak user:**
- ✅ Если Keycloak user creation fails → удаляем Person
- ❌ Если password set fails → НЕ удаляем ни Person, ни Keycloak user
- ❌ Если login fails → НЕ удаляем (пользователь может залогиниться позже)

**Причина:** После создания Keycloak user начинается audit trail, который нельзя просто удалить.

### Observability

Все компенсирующие транзакции трассируются в **Grafana Tempo**:
```
registration (ERROR, 850ms)
  ├─ create_person (OK, 120ms)
  ├─ create_keycloak_user (ERROR, 200ms)
  └─ rollback_person (OK, 80ms) ← compensating transaction
```

**Логи:**
```json
{
  "level": "ERROR",
  "message": "Registration failed for email: user@example.com",
  "trace_id": "fb47b1deb3b6e4134167048b1ad49eda",
  "compensating_transaction": "DELETE /v1/persons/{user_uid}",
  "rollback_status": "SUCCESS"
}
```

### Тестирование

Компенсирующие транзакции покрыты unit-тестами:

| Test | Scenario | Expected Result |
|------|----------|----------------|
| `register_keycloakCreateFails_deletesPersonAndPropagatesError` | Keycloak creation fails → DELETE succeeds | ✅ Person deleted, error propagated |
| `register_keycloakCreateFails_deletePersonAlsoFails_stillPropagatesOriginalError` | Keycloak fails → DELETE also fails | ❌ CRITICAL log, original error propagated |

**Покрытие:** `UserService` business logic ~80-85% (исключая auto-generated код)


---




