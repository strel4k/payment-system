# Payment System — Microservices Architecture

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Test Coverage](https://img.shields.io/badge/coverage-80%25-green)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Полнофункциональная микросервисная платёжная система с **wallet management**, **transaction processing**, **event-driven architecture**, **distributed tracing** и **observability stack**.

---

## 🎯 Возможности

- ✅ **Микросервисная архитектура** — individuals-api (orchestrator) + person-service + transaction-service + currency-rate-service + fake-payment-provider + **payment-service**
- ✅ **Wallet Management** — создание и управление кошельками пользователей
- ✅ **Transaction Processing** — deposit, withdrawal, transfer с двухфазным подтверждением
- ✅ **Payment Service** — управление методами оплаты, проведение платежей через FPP, компенсация при ошибках
- ✅ **Currency Rate Service** — актуальные курсы валют, cross-rate расчёт через USD, корректирующие коэффициенты, ShedLock
- ✅ **OpenFeign** — декларативные HTTP-клиенты (individuals-api → currency-rate-service)
- ✅ **Event-Driven Architecture** — Apache Kafka для асинхронных операций
- ✅ **OAuth2/JWT аутентификация** — интеграция с Keycloak
- ✅ **Distributed Tracing** — OpenTelemetry + Tempo
- ✅ **Full Observability** — Prometheus (метрики) + Loki (логи) + Grafana (визуализация)
- ✅ **Artifact Management** — Nexus OSS для Maven артефактов
- ✅ **Database Audit** — Hibernate Envers для отслеживания изменений
- ✅ **OpenAPI Specification** — автогенерация DTO из YAML
- ✅ **Database Sharding** — Apache ShardingSphere JDBC (optional profile)
- ✅ **Comprehensive Testing** — unit & integration тесты, 80%+ покрытие бизнес-логики

---

## 📚 Документация

| Документ | Описание |
|----------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Архитектурные диаграммы (C4, Sequence) |
| [individuals-api/README.md](individuals-api/README.md) | Individuals API (оркестратор) |
| [transaction-service/README.md](transaction-service/README.md) | Transaction Service API и архитектура |
| [currency-rate-service/README.md](currency-rate-service/README.md) | Currency Rate Service API и архитектура |
| [fake-payment-provider/README.md](fake-payment-provider/README.md) | Fake Payment Provider — эмулятор платёжного шлюза |
| [payment-service/README.md](payment-service/README.md) | Payment Service — управление методами оплаты и платежами |
| [docs/TEST_COVERAGE_REPORT.md](docs/TEST_COVERAGE_REPORT.md) | Отчёт о покрытии тестами |

### Диаграммы (PlantUML)

| Диаграмма | Описание |
|-----------|----------|
| [docs/architecture/diagrams/context.puml](docs/architecture/diagrams/context.puml) | C4 Context Diagram |
| [docs/architecture/diagrams/container.puml](docs/architecture/diagrams/container.puml) | C4 Container Diagram |
| [docs/architecture/diagrams/sequence-registration.puml](docs/architecture/diagrams/sequence-registration.puml) | User Registration Flow |
| [docs/architecture/diagrams/sequence-deposit.puml](docs/architecture/diagrams/sequence-deposit.puml) | Deposit Flow (async Kafka) |
| [docs/architecture/diagrams/sequence-withdrawal.puml](docs/architecture/diagrams/sequence-withdrawal.puml) | Withdrawal Flow (semi-sync) |
| [docs/architecture/diagrams/sequence-transfer.puml](docs/architecture/diagrams/sequence-transfer.puml) | Transfer Flow (sync atomic) |

---

## 🏗️ Архитектура

```
                       ┌─────────────┐
                       │    User     │
                       └──────┬──────┘
                              │ HTTPS/REST
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Individuals API                          │
│              (Orchestrator, WebFlux, Port 8081)             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ • Authentication & Registration                      │   │
│  │ • JWT Token Management                               │   │
│  │ • Proxy to Person Service & Transaction Service      │   │
│  │ • Currency rate fetch via OpenFeign                  │   │
│  └──────────────────────────────────────────────────────┘   │
└────┬──────────────┬──────────────┬──────────────┬───────────┘
     │              │              │              │
     ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────────┐
│   Person     │ │ Transaction  │ │ Keycloak │ │Currency Rate │
│   Service    │ │   Service    │ │ (OAuth2) │ │   Service    │
│   (8082)     │ │   (8083)     │ │  (8080)  │ │   (8085)     │
└──────┬───────┘ └──────┬───────┘ └────┬─────┘ └──────┬───────┘
       │                │              │              │
       ▼                ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  Person DB   │ │Transaction DB│ │Keycloak  │ │ Currency DB  │
│  (Postgres)  │ │  (Postgres)  │ │   DB     │ │  (Postgres)  │
│    :5434     │ │    :5435     │ │  :5433   │ │    :5436     │
└──────────────┘ └──────┬───────┘ └──────────┘ └──────────────┘
                        │
                        ▼
                 ┌──────────────┐        ┌───────────────────────┐
                 │    Kafka     │        │ exchangerate-api.com  │
                 │   (Events)   │        │   (External Rates)    │
                 │    :9092     │        └───────────────────────┘
                 └──────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                 Observability Stack                         │
│  Prometheus:9090 │ Grafana:3000 │ Loki:3100 │ Tempo:3200    │
│                  Promtail (log shipper)                     │
└─────────────────────────────────────────────────────────────┘

                 ┌──────────────┐
                 │  Nexus OSS   │
                 │    :8091     │
                 │ Maven repo   │
                 └──────────────┘

┌──────────────────────────────────────────────┐
│          Fake Payment Provider               │
│    (Payment Gateway Emulator, Port 8090)     │
│  • Basic Auth (merchantId / secretKey)       │
│  • POST /api/v1/transactions                 │
│  • POST /api/v1/payouts                      │
│  • POST /webhook/transaction|payout          │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
            ┌──────────────┐
            │   FPP DB     │
            │  (Postgres)  │
            │    :5437     │
            └──────────────┘
```

---

## 🚀 Быстрый старт

### Требования
- Docker & Docker Compose
- JDK 17+ (для локальной разработки)
- Git

### 1. Клонирование репозитория
```bash
git clone 
cd payment-system
```

### 2. Запуск всех сервисов
```bash
docker compose up -d
```

### 3. Проверка статуса
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Должны быть запущены:
- ✅ individuals-api (8081)
- ✅ person-service (8082)
- ✅ transaction-service (8083)
- ✅ currency-rate-service (8085)
- ✅ fake-payment-provider (8090)
- ✅ keycloak (8080)
- ✅ kafka (9092)
- ✅ nexus (8091)
- ✅ prometheus (9090)
- ✅ grafana (3000)
- ✅ loki (3100)
- ✅ tempo (3200)

### 4. Smoke test
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8085/actuator/health
curl "http://localhost:8085/api/v1/rates?from=USD&to=EUR" | jq
```

### 5. Регистрация пользователя
```bash
curl -X POST http://localhost:8081/v1/auth/registration \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "confirm_password": "SecurePass123!",
    "first_name": "John",
    "last_name": "Doe"
  }' | jq
```

---

## 🌐 Порты и доступы

| Сервис | URL | Credentials | Назначение |
|--------|-----|-------------|------------|
| **Individuals API** | http://localhost:8081 | — | Orchestrator (auth, wallets, transactions) |
| **Person Service** | http://localhost:8082 | — | User Data Management (internal) |
| **Transaction Service** | http://localhost:8083 | — | Wallets & Transactions (internal) |
| **Currency Rate Service** | http://localhost:8085 | — | Exchange rates (internal) |
| **Keycloak** | http://localhost:8080 | admin/admin | Identity Provider |
| **Nexus OSS** | http://localhost:8091 | admin/admin123 | Maven Repository |
| **Grafana** | http://localhost:3000 | admin/admin | Dashboards |
| **Prometheus** | http://localhost:9090 | — | Metrics |
| **Kafka UI** | http://localhost:8084 | — | Kafka Browser |

---

## 💳 API — Individuals API (Orchestrator)

### Authentication
```bash
POST /v1/auth/registration    # Register new user
POST /v1/auth/login           # Login
POST /v1/auth/refresh-token   # Refresh JWT
GET  /v1/auth/me              # Get current user info
```

### Wallets
```bash
POST /v1/wallets              # Create wallet
GET  /v1/wallets/{uid}        # Get wallet
GET  /v1/wallets              # List user wallets
```

### Transactions
```bash
POST /v1/transactions/{type}/init      # Init (deposit/withdrawal/transfer)
POST /v1/transactions/{type}/confirm   # Confirm
GET  /v1/transactions/{uid}/status     # Get status
```

### Fee Structure

| Operation | Fee | Flow |
|-----------|-----|------|
| Deposit | 0% | Async (Kafka) |
| Withdrawal | 1% | Semi-sync (Kafka) |
| Transfer | 0.5% | Sync (atomic) |

---

## 💱 Currency Rate Service

### API
```bash
GET /api/v1/rates?from=USD&to=EUR             # Актуальный курс
GET /api/v1/rates?from=USD&to=EUR&timestamp=  # Курс на дату
GET /api/v1/currencies                         # Список валют
GET /api/v1/rate-providers                     # Провайдеры
```

### Поддерживаемые валюты
USD, EUR, RUB, GBP, CNY, JPY, CHF, CAD, AUD, TRY (10 валют, 90 пар)

### Корректирующие коэффициенты
```
rate_final = rate_raw * factor
```
Коэффициенты хранятся в таблице `rate_correction_factors`. Примеры: USD→EUR `0.9980`, USD→RUB `1.0020`.

---

## 📊 Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `deposit-requested` | transaction-service | Payment Gateway | Initiate deposit |
| `deposit-completed` | Payment Gateway | transaction-service | Credit wallet |
| `withdrawal-requested` | transaction-service | Payment Gateway | Initiate withdrawal |
| `withdrawal-completed` | Payment Gateway | transaction-service | Confirm withdrawal |
| `withdrawal-failed` | Payment Gateway | transaction-service | Refund on failure |

---

## 🧪 Тестирование

```bash
# Все тесты
./gradlew test

# По модулям
./gradlew :person-service:test
./gradlew :individuals-api:test
./gradlew :transaction-service:test
./gradlew :currency-rate-service:test
./gradlew :currency-rate-service:integrationTest
./gradlew :fake-payment-provider:test
```

---

## 🔧 Локальная разработка

### Сборка всех модулей
```bash
./gradlew build
```

### Пересборка одного сервиса (быстро)
```bash
# Без пересборки Docker образа — копируем jar напрямую
./gradlew :individuals-api:bootJar -x test
docker cp individuals-api/build/libs/individuals-api-0.0.1-SNAPSHOT.jar individuals-api:/app/app.jar
docker restart individuals-api
```

### Запуск отдельных сервисов локально
```bash
# Инфраструктура
docker compose up -d zookeeper kafka transaction-postgres keycloak-postgres keycloak person-postgres currency-postgres

# Сервисы
./gradlew :currency-rate-service:bootRun
./gradlew :transaction-service:bootRun
./gradlew :individuals-api:bootRun
```

---

## 🐛 Troubleshooting

### Сервис не стартует
```bash
# Проверка логов
docker logs individuals-api
docker logs person-service

# Проверка зависимостей
docker-compose ps
```

### База данных не подключается
```bash
# Проверка доступности PostgreSQL
docker exec -it person-postgres psql -U person -d person_db -c "\dt person.*"
```

### Tempo не показывает трассы
```bash
# Проверка spans в Tempo
docker logs tempo | grep "Start span"

# Проверка OTel агента в контейнере
docker exec individuals-api ls -la /app/opentelemetry-javaagent.jar
```

### Nexus недоступен
```bash
# Получить admin пароль
docker exec nexus cat /nexus-data/admin.password

# Проверка repository
curl -u admin:<password> http://localhost:8091/service/rest/v1/repositories
```

### Kafka consumer не обрабатывает события
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group transaction-service --describe
```

### Проверка логов
```bash
docker logs transaction-service 2>&1 | grep -i "error\|exception" | tail -20
```

### База данных
```bash
docker exec transaction-postgres psql -U postgres -d transaction \
  -c "SELECT uid, type, status, amount FROM transactions ORDER BY created_at DESC LIMIT 10;"
```


---

## 📄 License

This project is licensed under the MIT License.