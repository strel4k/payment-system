# Payment System — Microservices Architecture

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Test Coverage](https://img.shields.io/badge/coverage-80%25-green)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Полнофункциональная микросервисная система управления пользователями с **distributed tracing**, **observability stack**, и **artifact management**.

---

## 🎯 Возможности

- ✅ **Микросервисная архитектура** — individuals-api (orchestrator) + person-service (data service)
- ✅ **OAuth2/JWT аутентификация** — интеграция с Keycloak
- ✅ **Distributed Tracing** — OpenTelemetry + Tempo
- ✅ **Full Observability** — Prometheus (метрики) + Loki (логи) + Grafana (визуализация)
- ✅ **Artifact Management** — Nexus OSS для Maven артефактов
- ✅ **Database Audit** — Hibernate Envers для отслеживания изменений
- ✅ **OpenAPI Specification** — автогенерация моделей и клиентов
- ✅ **Comprehensive Testing** — 64 unit + integration теста, 80%+ покрытие бизнес-логики
- ✅ **Production Ready** — Docker Compose для быстрого деплоя

---

## 📚 Документация

| Документ | Описание |
|----------|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Архитектурные диаграммы (C4, Sequence) |
| [docs/TEST_COVERAGE_REPORT.md](docs/TEST_COVERAGE_REPORT.md) | Отчёт о покрытии тестами |

---

## 🏗️ Архитектура

```
┌─────────────┐
│    User     │
└──────┬──────┘
       │ HTTPS/REST
       ▼
┌─────────────────────────────────────────┐
│         Individuals API                 │
│   (Orchestrator, WebFlux, Port 8081)    │
│  ┌───────────────────────────────────┐  │
│  │ • Authentication & Registration   │  │
│  │ • JWT Token Management            │  │
│  │ • Person Service Integration      │  │
│  └───────────────────────────────────┘  │
└────┬─────────────┬──────────────────┬───┘
     │             │                  │
     ▼             ▼                  ▼
┌──────────┐  ┌──────────┐    ┌──────────┐
│ Person   │  │ Keycloak │    │  Nexus   │
│ Service  │  │ (OAuth2) │    │   OSS    │
│ (8082)   │  │  (8080)  │    │  (8091)  │
└────┬─────┘  └────┬─────┘    └──────────┘
     │             │
     ▼             ▼
┌──────────┐  ┌──────────┐
│ Person   │  │ Keycloak │
│   DB     │  │    DB    │
│(Postgres)│  │(Postgres)│
└──────────┘  └──────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│      Observability Stack                │
│  Prometheus │ Grafana │ Loki │ Tempo   │
└─────────────────────────────────────────┘
```

**Полные диаграммы**: [docs/architecture/](docs/architecture/)

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
- ✅ individuals-keycloak (8080)
- ✅ nexus (8091)
- ✅ prometheus (9090)
- ✅ grafana (3000)
- ✅ loki (3100)
- ✅ tempo (3200)
- ✅ promtail
- ✅ person-postgres (5434)
- ✅ keycloak-postgres (5433)

### 4. Первый запрос — регистрация пользователя
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

Ответ:
```json
{
  "access_token": "eyJhbGc...",
  "refresh_token": "eyJhbGc...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

---

## 🌐 Порты и доступы

| Сервис | URL | Credentials | Назначение |
|--------|-----|-------------|------------|
| **Individuals API** | http://localhost:8081 | — | REST API (регистрация, логин) |
| **Person Service** | http://localhost:8082 | — | REST API (CRUD persons) |
| **Keycloak** | http://localhost:8080 | admin/admin | Identity Provider |
| **Nexus OSS** | http://localhost:8091 | admin/admin123 | Maven Repository |
| **Grafana** | http://localhost:3000 | admin/admin | Dashboards & Tracing |
| **Prometheus** | http://localhost:9090 | — | Metrics |
| **Loki** | http://localhost:3100 | — | Logs |
| **Tempo** | http://localhost:3200 | — | Distributed Tracing |

---

## 📊 Observability

### Grafana Dashboards
1. Открой http://localhost:3000 (admin/admin)
2. Доступные дашборды:
   - **Payment System Overview** — общая картина
   - **Individuals API Overview** — метрики API
   - **Keycloak Status** — статус Keycloak

### Distributed Tracing (Tempo)
1. **Grafana → Explore → Tempo**
2. Поиск по trace_id (из логов):
   ```bash
   docker logs individuals-api | grep trace_id | tail -1
   ```
3. Или поиск по service name: `individuals-api`

### Logs (Loki)
1. **Grafana → Explore → Loki**
2. Запрос:
   ```logql
   {job="docker", service="individuals-api"} |= "registration"
   ```

### Metrics (Prometheus)
1. **Grafana → Explore → Prometheus**
2. Примеры запросов:
   ```promql
   rate(http_server_requests_seconds_count[5m])
   jvm_memory_used_bytes{application="individuals-api"}
   ```

---

## 🧪 Тестирование

### Запуск всех тестов
```bash
./gradlew test
```

### Генерация отчёта о покрытии
```bash
./gradlew jacocoTestReport

# Открыть HTML отчёты
open person-service/build/reports/jacoco/test/html/index.html
open individuals-api/build/reports/jacoco/test/html/index.html
```

### Статистика тестов
- **64 теста** (51 unit + 13 integration)
- **Покрытие бизнес-логики**: 80-85%
- **TestContainers** для PostgreSQL

Подробнее: [docs/TEST_COVERAGE_REPORT.md](docs/TEST_COVERAGE_REPORT.md)

---

## 🔧 Локальная разработка

### Сборка проектов
```bash
# Все модули
./gradlew build

# Только person-service
./gradlew :person-service:build

# Только individuals-api
./gradlew :individuals-api:build
```

### Публикация в Nexus
```bash
# Публикация person-service-client
./gradlew :common:publish -PnexusUsername=admin -PnexusPassword=admin123

# Проверка в Nexus
curl -u admin:admin123 'http://localhost:8091/service/rest/v1/components?repository=maven-releases' | jq
```

### Запуск локально (без Docker)
1. Подними инфраструктуру:
   ```bash
   docker-compose up -d person-postgres keycloak-postgres individuals-keycloak nexus
   ```

2. Запуск person-service:
   ```bash
   cd person-service
   ./gradlew bootRun
   ```

3. Запуск individuals-api:
   ```bash
   cd individuals-api
   SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
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

---

## 📄 License

This project is licensed under the MIT License.

