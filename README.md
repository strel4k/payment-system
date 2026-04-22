# Payment System — Microservices Architecture

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Test Coverage](https://img.shields.io/badge/coverage-80%25-green)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Полнофункциональная микросервисная платёжная система с **wallet management**, **transaction processing**, **event-driven architecture**, **distributed tracing** и **observability stack**.

---

## 🎯 Возможности

- ✅ **Микросервисная архитектура** — individuals-api (orchestrator) + person-service + transaction-service + currency-rate-service + fake-payment-provider + payment-service + webhook-collector-service
- ✅ **Wallet Management** — создание и управление кошельками пользователей
- ✅ **Transaction Processing** — deposit, withdrawal, transfer с двухфазным подтверждением
- ✅ **Payment Service** — управление методами оплаты, проведение платежей через FPP, компенсация при ошибках
- ✅ **Webhook Collector Service** — приём и обработка webhook-уведомлений от FPP, двухуровневая защита (токен + HMAC-SHA256), публикация в Kafka
- ✅ **Notification Service** — приём уведомлений из Kafka (Avro + Schema Registry), сохранение в БД, отправка email при регистрации, REST API
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
| [webhook-collector-service/README.md](webhook-collector-service/README.md) | Webhook Collector Service — приём webhook, безопасность, Kafka |
| [notification-service/README.md](notification-service/README.md) | Notification Service — Kafka Avro consumer, email, REST API |
| [docs/TEST_COVERAGE_REPORT.md](docs/TEST_COVERAGE_REPORT.md) | Отчёт о покрытии тестами |

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
└────┬──────────────┬──────────────┬──────────────┬───────────┘
     │              │              │              │
     ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────────┐
│   Person     │ │ Transaction  │ │ Keycloak │ │Currency Rate │
│   Service    │ │   Service    │ │ (OAuth2) │ │   Service    │
│   (8082)     │ │   (8083)     │ │  (8080)  │ │   (8085)     │
└──────┬───────┘ └──────┬───────┘ └──────────┘ └──────────────┘
       │                │
       ▼                ▼
┌──────────────┐ ┌──────────────┐
│  Person DB   │ │Transaction DB│
│  (Postgres)  │ │  (Postgres)  │
│    :5434     │ │    :5435     │
└──────────────┘ └──────┬───────┘
                        │
                        ▼
                 ┌──────────────┐
                 │    Kafka     │
                 │   (Events)   │
                 │    :9092     │
                 └──────┬───────┘
                        │
         ┌──────────────┴──────────────┐
         ▼                             ▼
┌──────────────────┐         ┌──────────────────────┐
│  Payment Service │         │ Webhook Collector    │
│     (8083)       │         │ Service  (8086)      │
│  • Basic Auth    │         │ • X-Webhook-Token    │
│  • Outbox        │         │ • HMAC-SHA256        │
└────────┬─────────┘         └──────────┬───────────┘
         │                              │
         ▼                              ▼
┌──────────────────┐         ┌──────────────────────┐
│  Payment DB      │         │  Webhook DB          │
│  (Postgres :5438)│         │  (Postgres :5439)    │
└──────────────────┘         └──────────────────────┘

┌──────────────────────────────────────────────┐
│          Fake Payment Provider               │
│    (Payment Gateway Emulator, Port 8090)     │
└──────────────────┬───────────────────────────┘
                   │
                   ▼
            ┌──────────────┐
            │   FPP DB     │
            │  (Postgres)  │
            │    :5437     │
            └──────────────┘

┌───────────────────────────────────────────────────────────────┐
│                 Observability Stack                           │
│  Prometheus:9090 │ Grafana:3000 │ Loki:3100 │ Tempo:3200      │
└───────────────────────────────────────────────────────────────┘

┌──────────────┐
│  Nexus OSS   │
│    :8091     │
│ Maven repo   │
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
git clone <repository-url>
cd payment-system
```

### 2. Запуск всех сервисов
```bash
make up
# или
docker compose up -d
```

### 3. Проверка статуса
```bash
make health
```

Должны быть запущены:
- ✅ individuals-api (8081)
- ✅ person-service (8082)
- ✅ transaction-service (8083)
- ✅ currency-rate-service (8085)
- ✅ payment-service (8083)
- ✅ fake-payment-provider (8090)
- ✅ webhook-collector-service (8086)
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
curl http://localhost:8086/actuator/health
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
| **Payment Service** | http://localhost:8083 | — | Payment processing (internal) |
| **Webhook Collector** | http://localhost:8086 | — | Webhook processing (internal) |
| **Notification Service** | http://localhost:8087 | — | Notifications (internal) |
| **Fake Payment Provider** | http://localhost:8090 | merchant-1/secret123 | Payment Gateway Emulator |
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

## 📊 Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `deposit-requested` | transaction-service | Payment Gateway | Initiate deposit |
| `deposit-completed` | Payment Gateway | transaction-service | Credit wallet |
| `withdrawal-requested` | transaction-service | Payment Gateway | Initiate withdrawal |
| `withdrawal-completed` | Payment Gateway | transaction-service | Confirm withdrawal |
| `withdrawal-failed` | Payment Gateway | transaction-service | Refund on failure |
| `payment.status.updated` | webhook-collector-service | transaction-service | Webhook status update |
| `notification.created` | transaction-service | notification-service | User notification (Avro) |

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
./gradlew :fake-payment-provider:test
./gradlew :payment-service:test
./gradlew :webhook-collector-service:test
./gradlew :notification-service:test

# Makefile shortcuts
make test
make test-webhook
```

---

## 🔧 Локальная разработка

### Сборка всех модулей
```bash
./gradlew build
```

### Публикация common-library в Nexus
```bash
make nexus-publish-common
# или
./gradlew :common-library:publish
```

### Запуск отдельных сервисов локально
```bash
# Инфраструктура
docker compose up -d zookeeper kafka transaction-postgres keycloak-postgres keycloak person-postgres webhook-db

# Сервисы
./gradlew :webhook-collector-service:bootRun
./gradlew :transaction-service:bootRun
./gradlew :individuals-api:bootRun
```

---

## 🐛 Troubleshooting

### Сервис не стартует
```bash
docker logs webhook-collector-service
docker logs payment-service
docker-compose ps
```

### База данных не подключается
```bash
make db-webhook
make db-payment
```

### Kafka consumer не обрабатывает события
```bash
make kafka-consumer-groups
make kafka-topics
```

### Nexus недоступен
```bash
make nexus-password
```

---

## 📄 License

This project is licensed under the MIT License.