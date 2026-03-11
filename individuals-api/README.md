# Individuals API

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Оркестратор платёжной системы — единая точка входа для всех клиентских запросов. Проксирует вызовы к person-service, transaction-service и currency-rate-service.

---

## 🎯 Возможности

- ✅ **Единая точка входа** — все внешние запросы только через individuals-api
- ✅ **Authentication & Authorization** — регистрация, логин, refresh через Keycloak
- ✅ **Wallet Management** — создание и получение кошельков (proxy → transaction-service)
- ✅ **Transaction Processing** — deposit, withdrawal, transfer с двухфазным подтверждением
- ✅ **Cross-Currency Transfer** — автоматический fetch курса при разных валютах кошельков
- ✅ **OpenFeign** — декларативный HTTP-клиент к currency-rate-service
- ✅ **OAuth2/JWT** — интеграция с Keycloak (Resource Server)
- ✅ **Distributed Tracing** — OpenTelemetry + Grafana Tempo
- ✅ **Full Observability** — Prometheus метрики, JSON логи в Loki
- ✅ **OpenAPI Specification** — автогенерация DTO из YAML

---

## 📚 Документация

| Документ | Описание |
|----------|----------|
| [../README.md](../README.md) | Корневая документация проекта |
| [../transaction-service/README.md](../transaction-service/README.md) | Transaction Service |
| [../currency-rate-service/README.md](../currency-rate-service/README.md) | Currency Rate Service |
| [../docs/TEST_COVERAGE_REPORT.md](../docs/TEST_COVERAGE_REPORT.md) | Отчёт о покрытии тестами |

### Диаграммы (PlantUML)

| Диаграмма | Описание |
|-----------|----------|
| [docs/architecture/diagrams/context.puml](../docs/architecture/diagrams/context.puml) | C4 Context Diagram |
| [docs/architecture/diagrams/container.puml](../docs/architecture/diagrams/container.puml) | C4 Container Diagram |
| [docs/architecture/diagrams/sequence-registration.puml](../docs/architecture/diagrams/sequence-registration.puml) | User Registration Flow |
| [docs/architecture/diagrams/sequence-deposit.puml](../docs/architecture/diagrams/sequence-deposit.puml) | Deposit Flow (async Kafka) |
| [docs/architecture/diagrams/sequence-withdrawal.puml](../docs/architecture/diagrams/sequence-withdrawal.puml) | Withdrawal Flow (semi-sync + compensating) |
| [docs/architecture/diagrams/sequence-transfer.puml](../docs/architecture/diagrams/sequence-transfer.puml) | Transfer Flow (sync atomic) |

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
│ • Currency rate fetch via OpenFeign (currency-rate-service) │
│ • person-service-api-client + transaction-service-api-client│
└────┬──────────────┬──────────────┬──────────────┬───────────┘
     │              │              │              │
     ▼              ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌──────────┐ ┌──────────────┐
│   Person     │ │ Transaction  │ │ Keycloak │ │Currency Rate │
│   Service    │ │   Service    │ │  (8080)  │ │   Service    │
│   (8082)     │ │   (8083)     │ └──────────┘ │   (8085)     │
└──────┬───────┘ └──────┬───────┘              └──────┬───────┘
       │                │                             │
       ▼                ▼                             ▼
┌──────────────┐ ┌──────────────┐             ┌──────────────┐
│  Person DB   │ │Transaction DB│             │ Currency DB  │
│ Postgres:5434│ │ Postgres:5435│             │ Postgres:5436│
└──────────────┘ └──────┬───────┘             └──────────────┘
                        │
                 ┌──────▼───────┐
                 │    Kafka     │
                 │  9092/29092  │
                 └──────────────┘
```

---

## 🚀 Быстрый старт

### Требования
- Docker & Docker Compose
- JDK 17+

### 1. Запуск всех сервисов

```bash
docker compose up -d
```

Первый запуск занимает ~10-15 минут (JVM + OTel агент + Kafka).

### 2. Проверка статуса

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### 3. Smoke test

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8085/actuator/health
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

## 💳 API

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

## 🔗 OpenFeign — Currency Rate Client

При cross-currency переводе individuals-api автоматически получает курс через **Spring Cloud OpenFeign**:

```java
@FeignClient(name = "currency-rate-service", url = "${currency-rate-service.base-url}")
public interface CurrencyRateServiceFeignClient {

    @GetMapping("/api/v1/rates")
    RateResponse getRate(@RequestParam("from") String from,
                         @RequestParam("to") String to);
}
```

`CurrencyRateServiceClient` оборачивает синхронный Feign в реактивный `Mono.fromCallable()` на `boundedElastic` scheduler — это позволяет использовать блокирующий Feign в реактивном WebFlux контексте.

### Transfer Flow с конвертацией валют

```
POST /v1/transactions/transfer/init
  → TransferService.initTransfer()
    → getWallet(sourceUid)           // USD wallet
    → getWallet(targetUid)           // EUR wallet
    → sourceCurrency != targetCurrency?
        → CurrencyRateServiceFeignClient.getRate("USD", "EUR")  // 0.8542
        → enrichedRequest с курсом
    → TransactionServiceClient.initTransaction("transfer", request)
```

### Конфигурация
```yaml
currency-rate-service:
  base-url: ${CURRENCY_RATE_SERVICE_BASE_URL:http://localhost:8085}
```

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

```bash
# Публикация в локальный Maven
./gradlew :person-service:person-service-api-client:publishToMavenLocal
./gradlew :transaction-service:transaction-service-api-client:publishToMavenLocal

# Публикация в Nexus (при запущенном Docker)
./gradlew :person-service:person-service-api-client:publish
./gradlew :transaction-service:transaction-service-api-client:publish
```

---

## 🧪 Тестирование

```bash
# Все тесты
./gradlew :individuals-api:test

# Интеграционные тесты
./gradlew :individuals-api:integrationTest
```

---

## 🔧 Локальная разработка

### Пересборка без Docker build (быстро)

```bash
./gradlew :individuals-api:bootJar -x test
docker cp individuals-api/build/libs/individuals-api-0.0.1-SNAPSHOT.jar individuals-api:/app/app.jar
docker restart individuals-api
```

### Flyway и volumes

```bash
# При изменении миграций — пересоздать volumes
docker compose down -v
docker compose up -d
```

> ⚠️ Никогда не редактируй уже применённые файлы миграций V1, V2 и т.д. — только добавляй новые.

---

## 🐛 Troubleshooting

### Сервис не стартует
```bash
docker logs individuals-api --tail 50
```

### Spring Cloud версия несовместима
```yaml
# В application.yml
spring:
  cloud:
    compatibility-verifier:
      enabled: false
```

### Kafka healthcheck
```bash
docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list
```

### Kafka consumer lag
```bash
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --group transaction-service --describe
```

### Prometheus targets
Открой http://localhost:9090/targets — все должны быть UP.

### Grafana Kafka Dashboard
Импортируй dashboard ID **7589** для kafka-exporter метрик.

---

## 📄 License

This project is licensed under the MIT License.