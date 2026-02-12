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
- 📊 **Observability Stack** — Prometheus, Grafana, Loki, Tempo

---

#### 2. Container Diagram (Уровень 2)
**Файл**: [docs/architecture/diagrams/container.puml](docs/architecture/diagrams/container.puml)

Детализация внутренних контейнеров, технологий и баз данных.

**Технологический стек**:

| Компонент | Технология | Порт |
|-----------|-----------|------|
| **Individuals API** | Spring Boot WebFlux (Reactive) | 8081 |
| **Person Service** | Spring Boot Web + JPA | 8082 |
| **Transaction Service** | Spring Boot Web + JPA + Kafka | 8083 |
| **Person DB** | PostgreSQL 16 | 5434 |
| **Transaction DB** | PostgreSQL 16 | 5435 |
| **Keycloak** | Keycloak 26.2 | 8080 |
| **Keycloak DB** | PostgreSQL 17 | 5433 |
| **Kafka** | Apache Kafka | 9092 |
| **Zookeeper** | Apache Zookeeper | 2181 |
| **Nexus OSS** | Nexus 3.75.1 | 8091 |
| **Prometheus** | Prometheus | 9090 |
| **Grafana** | Grafana 10.3 | 3000 |
| **Loki** | Loki 2.9 | 3100 |
| **Tempo** | Tempo 2.6 | 3200 |

---

### Sequence Diagrams

#### User Registration Flow
**Файл**: [docs/architecture/diagrams/sequence-registration.puml](docs/architecture/diagrams/sequence-registration.puml)

Полная последовательность при регистрации с distributed tracing и compensating transactions.

**Основные шаги**:
1. User → Individuals API: `POST /v1/auth/registration`
2. API → Person Service: создание Person
3. API → Keycloak: регистрация пользователя + пароль
4. API → Keycloak: генерация JWT токенов
5. При ошибке: compensating transaction (удаление Person)

---

#### Deposit Flow (Asynchronous)
**Файл**: [docs/architecture/diagrams/sequence-deposit.puml](docs/architecture/diagrams/sequence-deposit.puml)

Двухфазное пополнение через Kafka.

**Основные шаги**:
1. `POST /transactions/deposit/init` → расчёт условий (TTL 15 мин)
2. `POST /transactions/deposit/confirm` → создание PENDING транзакции
3. Kafka: `deposit-requested` → Payment Gateway
4. Kafka: `deposit-completed` → зачисление на баланс

---

#### Withdrawal Flow (Semi-synchronous)
**Файл**: [docs/architecture/diagrams/sequence-withdrawal.puml](docs/architecture/diagrams/sequence-withdrawal.puml)

Вывод средств с немедленным списанием и Kafka подтверждением.

**Основные шаги**:
1. `POST /transactions/withdrawal/init` → проверка баланса
2. `POST /transactions/withdrawal/confirm` → списание, PENDING
3. Kafka: `withdrawal-requested` → Payment Gateway
4. Kafka: `withdrawal-completed` или `withdrawal-failed` (с refund)

---

#### Transfer Flow (Synchronous)
**Файл**: [docs/architecture/diagrams/sequence-transfer.puml](docs/architecture/diagrams/sequence-transfer.puml)

Атомарный перевод между кошельками.

**Основные шаги**:
1. `POST /transactions/transfer/init` → валидация обоих кошельков
2. `POST /transactions/transfer/confirm` → atomic debit + credit
3. Статус сразу COMPLETED (без Kafka)

---

## 🏗️ Архитектурные решения

### Микросервисная архитектура

**Разделение ответственности**:
- **individuals-api** — оркестратор, единая точка входа
- **person-service** — персональные данные пользователей
- **transaction-service** — кошельки, транзакции, платежи
- **Keycloak** — централизованная аутентификация

**Преимущества**:
- ✅ Независимое масштабирование сервисов
- ✅ Изоляция отказов (failure isolation)
- ✅ Разные технологические стеки
- ✅ Независимые циклы разработки

### Reactive vs Blocking

| Сервис | Стек | Причина |
|--------|------|---------|
| **individuals-api** | WebFlux | I/O-intensive (HTTP calls) |
| **person-service** | Spring MVC | Database-heavy, проще |
| **transaction-service** | Spring MVC | Database + Kafka |

### Database per Service

| Сервис | БД | Порт |
|--------|-----|------|
| person-service | person_db | 5434 |
| transaction-service | transaction_db | 5435 |
| keycloak | keycloak_db | 5433 |

**Преимущества**:
- ✅ Независимое управление схемой
- ✅ Изоляция данных
- ✅ Возможность шардирования (transaction-service)

### Database Sharding (Optional)

**Apache ShardingSphere JDBC** для transaction-service:
- Шардирование по `user_uid`
- 2 шарда (ds_0, ds_1)
- Broadcast tables: `wallet_types`
- Активация: `SPRING_PROFILES_ACTIVE=sharding`
```yaml
# shardingsphere-config.yaml
rules:
  - !SHARDING
    tables:
      transactions:
        shardingColumn: user_uid
        shardingAlgorithmName: user_uid_hash
```

---

## 🔄 Transaction Flows

### Two-Phase Pattern (init → confirm)
```
┌──────────────────────────────────────────────────────────┐
│                    Init Phase                            │
│  • Валидация входных данных                              │
│  • Расчёт комиссии                                       │
│  • Проверка баланса (withdrawal/transfer)                │
│  • Генерация requestUid (TTL 15 мин)                     │
│  • БД не изменяется!                                     │
└──────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────┐
│                   Confirm Phase                          │
│  • Получение данных из кеша по requestUid                │
│  • Создание транзакции в БД                              │
│  • Изменение баланса (withdrawal/transfer)               │
│  • Отправка в Kafka (deposit/withdrawal)                 │
└──────────────────────────────────────────────────────────┘
```

### Fee Structure

| Operation | Fee | Debit from | Credit to |
|-----------|-----|------------|-----------|
| Deposit | 0% | — | wallet |
| Withdrawal | 1% | wallet | external |
| Transfer | 0.5% | source wallet | target wallet |

---

## 📐 Архитектурные паттерны

### 1. API Gateway Pattern
**individuals-api** как единая точка входа:
- Роутинг к internal services
- JWT валидация
- Request/response transformation

### 2. Backend for Frontend (BFF)
Агрегация данных из нескольких сервисов:
- Person Service + Keycloak → User Info
- Transaction Service → Wallets, Transactions

### 3. Saga Pattern (Choreography)

**Registration Saga**:
```
1. Create Person ──► OK
2. Create Keycloak User ──► FAIL
3. [Compensate] Delete Person ◄──
```

**Withdrawal Saga**:
```
1. Debit Balance ──► OK
2. Process Payment ──► FAIL
3. [Compensate] Refund Balance ◄──
```

### 4. Two-Phase Commit (Application Level)
Init + Confirm разделение:
- Atomicity через кеш с TTL
- Idempotency через requestUid

---

## 🔒 Security

### OAuth2 + JWT
- **Keycloak** — centralized IdP
- **RS256** — JWT signature
- **user_uid** — custom attribute для связи с Person

### API Security
- Все endpoints требуют JWT
- User может видеть только свои wallets/transactions
- Pessimistic locking для balance updates

---

## 📊 Observability

### Three Pillars

| Pillar | Stack | Purpose |
|--------|-------|---------|
| **Metrics** | Prometheus + Grafana | JVM, HTTP, DB metrics |
| **Logs** | Loki + Promtail | Centralized JSON logs |
| **Traces** | Tempo + OpenTelemetry | Distributed tracing |

### Correlation
```json
{
  "level": "INFO",
  "message": "Deposit completed",
  "trace_id": "abc123",
  "span_id": "def456",
  "transaction_uid": "..."
}
```

---

## 🧪 Testing Strategy

| Layer | Tools | Coverage |
|-------|-------|----------|
| Unit | JUnit 5, Mockito | Services, Utils |
| Integration | TestContainers, H2 | Repositories, Controllers |
| E2E | Docker Compose | Full flow (manual) |

**Total**: 100 tests, 80%+ business logic coverage
