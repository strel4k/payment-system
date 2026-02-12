# Payment System — Microservices Architecture

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Test Coverage](https://img.shields.io/badge/coverage-80%25-green)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Полнофункциональная микросервисная платёжная система с **wallet management**, **transaction processing**, **event-driven architecture**, **distributed tracing** и **observability stack**.

---

## 🎯 Возможности

- ✅ **Микросервисная архитектура** — individuals-api (orchestrator) + person-service + transaction-service
- ✅ **Wallet Management** — создание и управление кошельками пользователей
- ✅ **Transaction Processing** — deposit, withdrawal, transfer с двухфазным подтверждением
- ✅ **Event-Driven Architecture** — Apache Kafka для асинхронных операций
- ✅ **OAuth2/JWT аутентификация** — интеграция с Keycloak
- ✅ **Distributed Tracing** — OpenTelemetry + Tempo
- ✅ **Full Observability** — Prometheus (метрики) + Loki (логи) + Grafana (визуализация)
- ✅ **Artifact Management** — Nexus OSS для Maven артефактов
- ✅ **Database Audit** — Hibernate Envers для отслеживания изменений
- ✅ **OpenAPI Specification** — автогенерация DTO из YAML
- ✅ **Database Sharding** — Apache ShardingSphere JDBC (optional profile)
- ✅ **Comprehensive Testing** — 100 unit & integration тестов, 80%+ покрытие

---

## 📚 Документация

| Документ | Описание |
|----------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Архитектурные диаграммы (C4, Sequence) |
| [transaction-service/README.md](transaction-service/README.md) | Transaction Service API и архитектура |
| [docs/TEST_COVERAGE_REPORT.md](docs/TEST_COVERAGE_REPORT.md) | Отчёт о покрытии тестами |

### Диаграммы

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
│  └──────────────────────────────────────────────────────┘   │
└────┬──────────────────┬──────────────────┬──────────────────┘
     │                  │                  │
     ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Person     │  │ Transaction  │  │   Keycloak   │
│   Service    │  │   Service    │  │   (OAuth2)   │
│   (8082)     │  │   (8083)     │  │   (8080)     │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Person DB   │  │Transaction DB│  │ Keycloak DB  │
│  (Postgres)  │  │  (Postgres)  │  │  (Postgres)  │
│    :5434     │  │    :5435     │  │    :5433     │
└──────────────┘  └──────────────┘  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │    Kafka     │
                  │   (Events)   │
                  │    :9092     │
                  └──────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 Observability Stack                         │
│     Prometheus │ Grafana │ Loki │ Tempo │ Promtail          │
└─────────────────────────────────────────────────────────────┘
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
docker-compose up -d
```

### 3. Проверка статуса
```bash
docker-compose ps
```

Должны быть запущены:
- ✅ individuals-api (8081)
- ✅ person-service (8082)
- ✅ transaction-service (8083)
- ✅ keycloak (8080)
- ✅ kafka (9092)
- ✅ zookeeper (2181)
- ✅ nexus (8091)
- ✅ prometheus (9090)
- ✅ grafana (3000)
- ✅ loki (3100)
- ✅ tempo (3200)

### 4. Регистрация пользователя
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

### 5. Создание кошелька (через оркестратор)
```bash
TOKEN="<access_token from registration>"

curl -X POST http://localhost:8081/v1/wallets \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "walletTypeUid": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "name": "My USD Wallet"
  }' | jq
```

### 6. Инициализация депозита
```bash
curl -X POST http://localhost:8081/v1/transactions/deposit/init \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "walletUid": "<wallet_uid>",
    "amount": 100
  }' | jq
```

---

## 🌐 Порты и доступы

| Сервис | URL | Credentials | Назначение |
|--------|-----|-------------|------------|
| **Individuals API** | http://localhost:8081 | — | Orchestrator (auth, wallets, transactions) |
| **Person Service** | http://localhost:8082 | — | User Data Management |
| **Transaction Service** | http://localhost:8083 | — | Wallets & Transactions (internal) |
| **Keycloak** | http://localhost:8080 | admin/admin | Identity Provider |
| **Nexus OSS** | http://localhost:8091 | admin/admin123 | Maven Repository |
| **Grafana** | http://localhost:3000 | admin/admin | Dashboards |
| **Prometheus** | http://localhost:9090 | — | Metrics |

---

## 💳 API — Individuals API (Orchestrator)

### Authentication
```bash
POST /v1/auth/registration    # Register new user
POST /v1/auth/login           # Login
POST /v1/auth/refresh-token   # Refresh JWT
GET  /v1/auth/me              # Get current user info
```

### Wallets (proxied to Transaction Service)
```bash
POST /v1/wallets              # Create wallet
GET  /v1/wallets/{uid}        # Get wallet
GET  /v1/wallets              # List user wallets
```

### Transactions (proxied to Transaction Service)
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

| Topic | Direction | Purpose |
|-------|-----------|---------|
| `deposit-requested` | → Payment Gateway | Initiate deposit |
| `deposit-completed` | ← Payment Gateway | Credit wallet |
| `withdrawal-requested` | → Payment Gateway | Initiate withdrawal |
| `withdrawal-completed` | ← Payment Gateway | Confirm withdrawal |
| `withdrawal-failed` | ← Payment Gateway | Refund on failure |

---

## 🧪 Тестирование

### Запуск всех тестов
```bash
./gradlew test
```

### По модулям
```bash
./gradlew :person-service:test
./gradlew :individuals-api:test
./gradlew :transaction-service:test
```

### Статистика
- **100 тестов** (unit + integration)
- **Покрытие бизнес-логики**: 80-85%
- **TestContainers** для PostgreSQL
- **H2** для быстрых unit-тестов

---

## 🔧 Локальная разработка

### Сборка проектов
```bash
./gradlew build
```

### Запуск отдельных сервисов
```bash
# Инфраструктура
docker-compose up -d zookeeper kafka transaction-postgres keycloak-postgres keycloak person-postgres

# Person Service
./gradlew :person-service:bootRun

# Transaction Service  
./gradlew :transaction-service:bootRun

# Individuals API
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

