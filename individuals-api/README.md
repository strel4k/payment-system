# Payment System — Microservices Architecture

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
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
- ✅ **Distributed Tracing** — OpenTelemetry + Grafana Tempo
- ✅ **Full Observability** — Prometheus (метрики) + Loki (логи) + Grafana (визуализация)
- ✅ **Artifact Management** — Nexus OSS для Maven артефактов (person-service-api-client, transaction-service-api-client)
- ✅ **Database Audit** — Hibernate Envers для отслеживания изменений
- ✅ **OpenAPI Specification** — автогенерация DTO из YAML
- ✅ **Database Sharding** — Apache ShardingSphere JDBC (optional profile)
- ✅ **Comprehensive Testing** — unit & integration тесты, 80%+ покрытие бизнес-логики

---

## 📚 Документация

| Документ | Описание |
|----------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Архитектурные решения, паттерны, стек |
| [transaction-service/README.md](transaction-service/README.md) | Transaction Service API и архитектура |
| [docs/TEST_COVERAGE_REPORT.md](docs/TEST_COVERAGE_REPORT.md) | Отчёт о покрытии тестами |

### Диаграммы (PlantUML)

| Диаграмма | Описание |
|-----------|----------|
| [docs/architecture/diagrams/context.puml](docs/architecture/diagrams/context.puml) | C4 Context Diagram |
| [docs/architecture/diagrams/container.puml](docs/architecture/diagrams/container.puml) | C4 Container Diagram |
| [docs/architecture/diagrams/sequence-registration.puml](docs/architecture/diagrams/sequence-registration.puml) | User Registration Flow |
| [docs/architecture/diagrams/sequence-deposit.puml](docs/architecture/diagrams/sequence-deposit.puml) | Deposit Flow (async Kafka) |
| [docs/architecture/diagrams/sequence-withdrawal.puml](docs/architecture/diagrams/sequence-withdrawal.puml) | Withdrawal Flow (semi-sync + compensating) |
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
│                    Individuals API (8081)                   │
│              Orchestrator, WebFlux, Stateless               │
│ • Authentication & Registration (Keycloak)                  │
│ • Proxy to Person Service & Transaction Service             │
│ • person-service-api-client + transaction-service-api-client│
└────┬──────────────────┬──────────────────┬──────────────────┘
     │                  │                  │
     ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Person     │  │ Transaction  │  │   Keycloak   │
│   Service    │  │   Service    │  │   (8080)     │
│   (8082)     │  │   (8083)     │  └──────┬───────┘
└──────┬───────┘  └──────┬───────┘         │
       │                 │          ┌──────┴───────┐
       ▼                 ▼          │ Keycloak DB  │
┌──────────────┐  ┌──────────────┐  │ Postgres:5433│
│  Person DB   │  │Transaction DB│  └──────────────┘
│ Postgres:5434│  │ Postgres:5435│
└──────────────┘  └──────┬───────┘
                         │
                  ┌──────▼───────┐
                  │    Kafka     │
                  │ 9092/29092   │
                  └──────┬───────┘
                         │
               ┌─────────▼──────────┐
               │   Kafka Exporter   │
               │      :9308         │
               └─────────┬──────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│                 Observability Stack                         │
│  Prometheus:9090 │ Grafana:3000 │ Loki:3100 │ Tempo:3200    │
│                  Promtail (log shipper)                     │
└─────────────────────────────────────────────────────────────┘

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

### 1. Запуск всех сервисов

```bash
docker-compose up -d
```

Первый запуск занимает ~10-12 минут (JVM + OTel агент + Kafka).

### 2. Проверка статуса

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Все сервисы должны быть `(healthy)`.

### 3. Smoke test

```bash
curl http://localhost:8081/actuator/health   # individuals-api
curl http://localhost:8082/actuator/health   # person-service
curl http://localhost:8083/actuator/health   # transaction-service
```

---

## 🌐 Порты и доступы

| Сервис | URL | Credentials | Назначение |
|--------|-----|-------------|------------|
| **Individuals API** | http://localhost:8081 | — | Orchestrator (auth, wallets, transactions) |
| **Person Service** | http://localhost:8082 | — | User Data Management (internal) |
| **Transaction Service** | http://localhost:8083 | — | Wallets & Transactions (internal) |
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

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `deposit-requested` | transaction-service | Payment Gateway | Initiate deposit |
| `deposit-completed` | Payment Gateway | transaction-service | Credit wallet |
| `withdrawal-requested` | transaction-service | Payment Gateway | Initiate withdrawal |
| `withdrawal-completed` | Payment Gateway | transaction-service | Confirm withdrawal |
| `withdrawal-failed` | Payment Gateway | transaction-service | Refund on failure |

---

## 📦 API Client Artifacts

Каждый сервис публикует собственный API-клиент в Nexus:

```bash
# Публикация
./gradlew :person-service:person-service-api-client:publishToMavenLocal
./gradlew :transaction-service:transaction-service-api-client:publishToMavenLocal

# Или в Nexus (при запущенном Docker)
./gradlew :person-service:person-service-api-client:publish
./gradlew :transaction-service:transaction-service-api-client:publish
```

---

## 🧪 Тестирование

### Unit-тесты
```bash
./gradlew test

# По модулям
./gradlew :person-service:test
./gradlew :individuals-api:test
./gradlew :transaction-service:test
```

### Integration-тесты
```bash
# individuals-api (Keycloak + WebFlux)
./gradlew :individuals-api:integrationTest -x test

# transaction-service (PostgreSQL + ShardingSphere)
./gradlew :transaction-service:integrationTest -x test
```

### Покрытие
- **Покрытие бизнес-логики**: 80%+
- **Testcontainers**: PostgreSQL, Keycloak (quay.io/keycloak/keycloak:26.2)

### Integration-тесты — individuals-api

| Класс | Описание |
|-------|----------|
| `AuthFlowIntegrationTest` | Registration → Me, Login, Duplicate 409 |
| `WalletTransactionFlowIT` | E2E deposit flow, Wallets CRUD, Transactions lifecycle |

Тесты поднимают реальный Keycloak-контейнер (~1 мин) и проверяют полный auth-flow через JWT.

---

## 🔧 Локальная разработка

### Пересборка JAR и Docker образа одного сервиса

```bash
./gradlew :individuals-api:bootJar
docker-compose up -d --build individuals-api
```

### Flyway и volumes

При изменении уже применённых миграций необходимо пересоздать volumes:
```bash
docker-compose down -v
docker-compose up -d
```

> ⚠️ Никогда не редактируй уже применённые файлы миграций V1, V2 и т.д. — только добавляй новые.

---

## 🐛 Troubleshooting

### Сервис не стартует
```bash
docker logs individuals-api --tail 50
docker logs transaction-service --tail 50
```

### Kafka healthcheck
```bash
# Kafka использует внутренний listener для healthcheck
docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list
```

### Flyway checksum mismatch
```bash
# Пересоздать базы (удаляет все данные!)
docker-compose down -v && docker-compose up -d
```

### Prometheus targets
Открой http://localhost:9090/targets — все должны быть UP.

### Kafka consumer lag
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --group transaction-service --describe
```

### Grafana Kafka Dashboard
Импортируй dashboard ID **7589** для kafka-exporter метрик.

---

## 📄 License

This project is licensed under the MIT License.
