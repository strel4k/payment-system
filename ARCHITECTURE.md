# Architecture Overview — Payment System

Архитектурная документация микросервисной платёжной системы.

---

## 📊 Архитектурные диаграммы

### C4 Model Diagrams

#### 1. Context Diagram (Уровень 1)
**Файл**: [docs/architecture/diagrams/context.puml](docs/architecture/diagrams/context.puml)

Показывает общую картину системы и взаимодействие с внешними компонентами.

**Ключевые компоненты**:
- 👤 **User** — конечный пользователь
- 🌐 **Individuals API** — оркестратор (аутентификация, кошельки, транзакции)
- 👤 **Person Service** — управление данными пользователей
- 💳 **Transaction Service** — обработка платежей и транзакций
- 🔐 **Keycloak** — OAuth2/JWT сервер
- 📬 **Apache Kafka** — асинхронные события
- 📦 **Nexus OSS** — репозиторий Maven артефактов
- 📊 **Observability Stack** — Prometheus, Grafana, Loki, Tempo

---

#### 2. Container Diagram (Уровень 2)
**Файл**: [docs/architecture/diagrams/container.puml](docs/architecture/diagrams/container.puml)

Детализация внутренних контейнеров, технологий и баз данных.

**Технологический стек**:

| Компонент | Технология | Порт |
|-----------|-----------|------|
| **Individuals API** | Spring Boot WebFlux (Reactive) | 8081 |
| **Person Service** | Spring Boot MVC + JPA + Envers | 8082 |
| **Transaction Service** | Spring Boot MVC + JPA + Kafka | 8083 |
| **Currency Rate Service** | Spring Boot MVC + JPA + OpenFeign | 8084 |
| **Fake Payment Provider** | Spring Boot MVC + JPA + Basic Auth | 8090 |
| **Person DB** | PostgreSQL 16 | 5434 |
| **Transaction DB** | PostgreSQL 16 | 5435 |
| **Currency Rate DB** | PostgreSQL 16 | 5436 |
| **FPP DB** | PostgreSQL 16 | 5437 |
| **Keycloak** | Keycloak 26.2 | 8080 |
| **Keycloak DB** | PostgreSQL 16 | 5433 |
| **Kafka** | Apache Kafka | 9092 (host), 29092 (internal) |
| **Zookeeper** | Apache Zookeeper | 2181 |
| **Nexus OSS** | Nexus 3.x | 8091 |
| **Prometheus** | Prometheus | 9090 |
| **Grafana** | Grafana 10.3.1 | 3000 |
| **Loki** | Loki 2.9.2 | 3100 |
| **Tempo** | Tempo 2.6 | 3200 |
| **Promtail** | Promtail | — |
| **Kafka Exporter** | danielqsj/kafka-exporter | 9308 |

**API Clients (Maven Artifacts)**:

| Артефакт | GroupId | Version |
|----------|---------|---------|
| `person-service-api-client` | `com.example` | `1.0.0` |
| `transaction-service-api-client` | `com.example` | `1.0.0` |

---

### Sequence Diagrams

#### User Registration Flow
**Файл**: [docs/architecture/diagrams/sequence-registration.puml](docs/architecture/diagrams/sequence-registration.puml)

Полная последовательность при регистрации с distributed tracing и compensating transactions.

**Основные шаги**:
1. User → Individuals API: `POST /v1/auth/registration`
2. API → Person Service: создание Person (транзакционно)
3. API → Keycloak: регистрация пользователя + пароль
4. API → Keycloak: генерация JWT токенов
5. При ошибке Keycloak: compensating transaction → удаление Person

---

#### Deposit Flow (Asynchronous via Kafka)
**Файл**: [docs/architecture/diagrams/sequence-deposit.puml](docs/architecture/diagrams/sequence-deposit.puml)

Двухфазное пополнение через Kafka. Fee: 0%.

**Основные шаги**:
1. `POST /transactions/deposit/init` → расчёт условий, requestUid (TTL 15 мин)
2. `POST /transactions/deposit/confirm` → создание PENDING транзакции
3. Kafka: `deposit-requested` → внешний Payment Gateway
4. Kafka: `deposit-completed` → зачисление на баланс, статус COMPLETED

---

#### Withdrawal Flow (Semi-synchronous)
**Файл**: [docs/architecture/diagrams/sequence-withdrawal.puml](docs/architecture/diagrams/sequence-withdrawal.puml)

Вывод средств с немедленным списанием и Kafka подтверждением. Fee: 1%.

**Основные шаги**:
1. `POST /transactions/withdrawal/init` → проверка баланса, requestUid
2. `POST /transactions/withdrawal/confirm` → pessimistic lock, списание, PENDING
3. Kafka: `withdrawal-requested` → Payment Gateway
4. Kafka: `withdrawal-completed` → статус COMPLETED
5. Kafka: `withdrawal-failed` → compensating transaction (refund), статус FAILED

---

#### Transfer Flow (Synchronous Atomic)
**Файл**: [docs/architecture/diagrams/sequence-transfer.puml](docs/architecture/diagrams/sequence-transfer.puml)

Атомарный перевод между кошельками. Fee: 0.5%. Без Kafka.

**Основные шаги**:
1. `POST /transactions/transfer/init` → валидация обоих кошельков
2. `POST /transactions/transfer/confirm` → pessimistic lock обоих кошельков, atomic debit + credit
3. Статус сразу COMPLETED (no Kafka, no async)

---

## 🏗️ Архитектурные решения

### Микросервисная архитектура

**Разделение ответственности**:
- **individuals-api** — оркестратор, единая точка входа, без собственной БД
- **person-service** — персональные данные пользователей, Hibernate Envers аудит
- **transaction-service** — кошельки, транзакции, Kafka-события
- **currency-rate-service** — обменные курсы, OpenFeign к внешнему API
- **fake-payment-provider** — эмулятор внешнего платёжного шлюза, изолированный микросервис
- **Keycloak** — централизованная аутентификация

### Reactive vs Blocking

| Сервис | Стек | Причина |
|--------|------|---------|
| **individuals-api** | WebFlux | I/O-intensive (HTTP calls к 3 сервисам) |
| **person-service** | Spring MVC | Database-heavy, Envers, проще |
| **transaction-service** | Spring MVC | Database + Kafka + pessimistic locking |
| **currency-rate-service** | Spring MVC | Scheduled jobs, OpenFeign, кеширование |
| **fake-payment-provider** | Spring MVC | Синхронный REST, эмуляция внешнего шлюза |

### API Client Architecture

Каждый сервис публикует собственный API-клиент как Maven артефакт:

```
person-service/
└── person-service-api-client/   ← генерирует DTOs из openapi.yml → публикует в Nexus
    └── com.example.dto.person.*

transaction-service/
└── transaction-service-api-client/  ← генерирует DTOs из transaction-service.yaml → публикует в Nexus
    └── com.example.dto.transaction.*

individuals-api/
└── build.gradle.kts  ← зависит от обоих артефактов из Nexus
```

### Database per Service

| Сервис | БД | Порт |
|--------|-----|------|
| person-service | person_db | 5434 |
| transaction-service | transaction_db | 5435 |
| currency-rate-service | currency_rate_db | 5436 |
| fake-payment-provider | fpp | 5437 |
| keycloak | keycloak_db | 5433 |

**individuals-api** не имеет собственной БД — stateless оркестратор.

### Database Sharding (Optional Profile)

**Apache ShardingSphere JDBC** для transaction-service:
- Шардирование по `user_uid`
- 2 шарда (ds_0, ds_1)
- Broadcast tables: `wallet_types`
- Активация: `SPRING_PROFILES_ACTIVE=sharding`

---

## 🔄 Transaction Flows

### Two-Phase Pattern (init → confirm)

```
┌──────────────────────────────────────────────────────────┐
│                    Init Phase                            │
│  • Валидация входных данных                              │
│  • Расчёт комиссии                                       │
│  • Проверка баланса (withdrawal/transfer)                │
│  • Генерация requestUid (TTL 15 мин, in-memory cache)    │
│  • БД не изменяется!                                     │
└──────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────┐
│                   Confirm Phase                          │
│  • Получение данных из кеша по requestUid                │
│  • Pessimistic locking (withdrawal/transfer)             │
│  • Создание транзакции в БД                              │
│  • Изменение баланса                                     │
│  • Отправка в Kafka (deposit/withdrawal)                 │
└──────────────────────────────────────────────────────────┘
```

### Fee Structure

| Operation | Fee | Sync/Async | Kafka Topics |
|-----------|-----|------------|--------------|
| Deposit | 0% | Async | deposit-requested, deposit-completed |
| Withdrawal | 1% | Semi-sync | withdrawal-requested, withdrawal-completed, withdrawal-failed |
| Transfer | 0.5% | Sync | — |

---

## 📐 Архитектурные паттерны

### 1. API Gateway / BFF Pattern
**individuals-api** как единая точка входа:
- Роутинг к internal services
- JWT валидация (Spring Security OAuth2 Resource Server)
- Request/response transformation

### 2. Saga Pattern (Choreography)

**Registration Saga**:
```
1. Create Person ──► OK
2. Create Keycloak User ──► FAIL
3. [Compensate] Delete Person ◄──
```

**Withdrawal Saga**:
```
1. Debit Balance ──► OK (synchronous)
2. Process Payment ──► FAIL (via Kafka)
3. [Compensate] Refund Balance ◄── (Kafka consumer)
```

### 3. Two-Phase Commit (Application Level)
Init + Confirm разделение:
- Atomicity через in-memory cache с TTL
- Idempotency через requestUid

### 4. Pessimistic Locking
Используется в withdrawal и transfer:
- `SELECT ... FOR UPDATE` предотвращает race conditions
- Гарантирует консистентность баланса при конкурентных операциях

---

## 💳 Fake Payment Provider

Изолированный микросервис — эмулятор внешнего платёжного шлюза. Не зависит от других сервисов и не публикует API-клиент в Nexus.

### API Endpoints

| Endpoint | Method | Auth | Описание |
|----------|--------|------|----------|
| `/api/v1/transactions` | POST | Basic Auth | Создать транзакцию (PENDING) |
| `/api/v1/transactions/{id}` | GET | Basic Auth | Получить статус транзакции |
| `/api/v1/transactions` | GET | Basic Auth | Список транзакций мерчанта |
| `/api/v1/payouts` | POST | Basic Auth | Создать выплату (PENDING) |
| `/api/v1/payouts/{id}` | GET | Basic Auth | Получить статус выплаты |
| `/api/v1/payouts` | GET | Basic Auth | Список выплат мерчанта |
| `/webhook/transaction` | POST | Нет | Обновить статус транзакции |
| `/webhook/payout` | POST | Нет | Обновить статус выплаты |
| `/actuator/prometheus` | GET | Нет | Prometheus метрики |

### Архитектурные решения

- **Basic Auth** — merchantId:secretKey (BCrypt hash в БД)
- **OpenAPI-first** — DTO генерируются из `openapi/fake-payment-provider.yaml`
- **Webhook idempotency** — статус обновляется только из PENDING; при конечном статусе (SUCCESS/FAILED) записывается только audit log
- **Audit log** — все входящие webhook-запросы сохраняются в таблицу `webhooks` вне зависимости от результата
- **Database isolation** — собственная PostgreSQL на порту 5437
- **No Nexus publishing** — не публикует артефакты, не зависит от других сервисов

### Схема БД

```
merchants          — аутентификация (merchant_id, secret_key BCrypt)
transactions       — пополнения (PENDING → SUCCESS/FAILED)
payouts            — выплаты (PENDING → SUCCESS/FAILED)
webhooks           — audit log входящих webhook-запросов
```

### Диаграммы

| Файл | Описание |
|------|----------|
| `fake-payment-provider/docs/diagrams/c4-container.puml` | C4 Container: место FPP в экосистеме |
| `fake-payment-provider/docs/diagrams/c4-component.puml` | C4 Component: внутренние компоненты |
| `fake-payment-provider/docs/diagrams/sequence-transaction.puml` | Sequence: создание транзакции + webhook |
| `fake-payment-provider/docs/diagrams/sequence-payout.puml` | Sequence: создание выплаты + webhook |
| `fake-payment-provider/docs/diagrams/sequence-webhook.puml` | Sequence: детальный webhook flow |

---

## 🔒 Security

### OAuth2 + JWT
- **Keycloak** — centralized IdP, realm `individuals`
- **RS256** — JWT signature
- **user_uid** — custom attribute для связи Keycloak user → Person entity
- **Spring Security OAuth2 Resource Server** — JWT validation в всех сервисах

---

## 📊 Observability

### Three Pillars

| Pillar | Stack | Purpose |
|--------|-------|---------|
| **Metrics** | Prometheus + Grafana | JVM, HTTP, DB, Kafka metrics |
| **Logs** | Loki + Promtail | Centralized JSON logs (Logstash encoder) |
| **Traces** | Tempo + OpenTelemetry | Distributed tracing (OTLP/HTTP) |

### Корреляция через trace_id
```json
{
  "@timestamp": "2026-02-18T15:28:01Z",
  "level": "INFO",
  "message": "Deposit completed",
  "service": "transaction-service",
  "traceId": "abc123def456",
  "spanId": "789xyz"
}
```

### Дашборды Grafana
- **Kafka** — импорт ID `7589` (kafka-exporter dashboard)
- **JVM** — Spring Boot Actuator метрики
- **PostgreSQL** — postgres-exporter метрики

---

## 🧪 Testing Strategy

| Layer | Tools | Scope |
|-------|-------|-------|
| Unit | JUnit 5, Mockito | Services, Mappers |
| Integration | TestContainers (PostgreSQL) | Repositories, Controllers |
| Rollback | @Transactional tests | Transaction integrity |

**Покрытие**: 80%+ бизнес-логика (после исключения автогенерированных DTO и entity классов)