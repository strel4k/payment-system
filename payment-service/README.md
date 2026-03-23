# payment-service

Микросервис управления методами оплаты и проведения платежей через интеграцию с Fake Payment Provider. Предоставляет REST API с Basic Auth, сохраняет историю платежей в PostgreSQL.

---

## Стек технологий

| Компонент       | Технология                        |
|-----------------|-----------------------------------|
| Runtime         | Java 17, Spring Boot 3.5.0        |
| API             | Spring Web MVC, OpenAPI 3.0       |
| Безопасность    | Spring Security (HTTP Basic Auth) |
| БД              | PostgreSQL 16, Spring Data JPA    |
| Миграции        | Flyway                            |
| HTTP-клиент     | RestTemplate                      |
| Метрики         | Micrometer + Prometheus           |
| Трассировка     | OpenTelemetry Java Agent + Tempo  |
| Логирование     | Logback + Logstash JSON encoder   |
| Тесты           | JUnit 5, Mockito, TestContainers, WireMock |
| Сборка          | Gradle 8, OpenAPI Generator       |

---

## Архитектура

Диаграммы в формате PlantUML (C4) находятся в `docs/diagrams/`:

- `c4-container.puml` — контейнерная диаграмма системы
- `c4-component.puml` — компонентная диаграмма сервиса
- `sequence-get-payment-methods.puml` — flow получения методов оплаты
- `sequence-payment-happy-path.puml` — полный цикл платежа (успех)
- `sequence-payment-error-compensation.puml` — flow ошибки и компенсации

---

## Запуск

### Через Docker Compose (рекомендуется)

```bash
# Из корня payment-system
./gradlew :payment-service:clean :payment-service:build -x test

docker-compose up -d payment-postgres fake-payment-provider payment-service
```

Сервис будет доступен на `http://localhost:8083`.

### Локальный запуск (без Docker)

1. Поднять PostgreSQL на порту `5438`, создать базу `payment`, пользователя `payment`
2. Задать переменные окружения (см. таблицу ниже)
3. Запустить:

```bash
./gradlew :payment-service:bootRun
```

### Переменные окружения

| Переменная                   | По умолчанию                               | Описание                          |
|------------------------------|--------------------------------------------|-----------------------------------|
| `SERVER_PORT`                | `8083`                                     | Порт сервиса                      |
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5438/payment` | JDBC URL базы данных              |
| `SPRING_DATASOURCE_USERNAME` | `payment`                                  | Пользователь БД                   |
| `SPRING_DATASOURCE_PASSWORD` | `payment`                                  | Пароль БД                         |
| `PAYMENT_SERVICE_USERNAME`   | —                                          | Basic Auth — логин (обязательно)  |
| `PAYMENT_SERVICE_PASSWORD`   | —                                          | Basic Auth — пароль (обязательно) |
| `FAKE_PROVIDER_URL`          | `http://fake-payment-provider:8090`        | URL Fake Payment Provider         |
| `FAKE_PROVIDER_USERNAME`     | —                                          | FPP Basic Auth — логин            |
| `FAKE_PROVIDER_PASSWORD`     | —                                          | FPP Basic Auth — пароль           |

---

## Аутентификация

Все эндпоинты `/api/v1/**` защищены HTTP Basic Auth. Credentials задаются через env-переменные `PAYMENT_SERVICE_USERNAME` / `PAYMENT_SERVICE_PASSWORD`.

Из `.env` по умолчанию:

| Поле     | Значение          |
|----------|-------------------|
| username | `payment-service` |
| password | `payment-secret`  |

Эндпоинт `/actuator/**` открыт без аутентификации.

---

## API — примеры запросов

### Получить методы оплаты

```bash
curl -s -u payment-service:payment-secret \
  http://localhost:8083/api/v1/payment-methods/USD/USA | jq
```

Ответ `200 OK`:
```json
[
  {
    "id": 1,
    "name": "Bank Card (VISA/MC)",
    "provider_method_type": "CARD",
    "image_url": null,
    "required_fields": [
      {
        "uid": "64cbc46a-659f-4e7a-926e-7e03fd509ae4",
        "name": "card_number",
        "description": "Номер карты",
        "placeholder": "0000 0000 0000 0000",
        "data_type": "STRING",
        "validation_type": "REGEXP",
        "validation_rule": "^\\d{16}$",
        "is_active": true
      }
    ]
  }
]
```

### Провести платёж

```bash
curl -s -u payment-service:payment-secret \
  -X POST http://localhost:8083/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "internalTransactionId": "550e8400-e29b-41d4-a716-446655440000",
    "methodId": 1,
    "amount": 100.00,
    "currency": "USD",
    "userFields": {
      "card_number": "1234567812345678",
      "card_holder": "JOHN DOE",
      "expiry_date": "12/26",
      "cvv": "123"
    }
  }' | jq
```

Ответ `200 OK` (успех):
```json
{
  "providerTransactionId": "42",
  "status": "COMPLETED"
}
```

Ответ `422 Unprocessable Entity` (ошибка провайдера):
```json
{
  "error": "PAYMENT_FAILED",
  "message": "Payment provider rejected the request: ..."
}
```

Ответ `404 Not Found` (метод не найден):
```json
{
  "error": "METHOD_NOT_FOUND",
  "message": "Payment method not found or inactive: 999"
}
```

---

## Мониторинг

| Эндпоинт                   | Описание               |
|----------------------------|------------------------|
| `GET /actuator/health`     | Статус приложения      |
| `GET /actuator/prometheus` | Метрики для Prometheus |

Grafana дашборд **Payment Service** доступен на `http://localhost:3000` (admin / admin).

Панели дашборда:
- 🟢 Service Health — UP/DOWN, RPS, ошибки, latency p99, uptime
- 💳 GET /payment-methods — RPS по статусам + latency p50/p95/p99
- 💸 POST /payments — RPS, COMPLETED/FAILED счётчики, Success Rate, JVM Heap
- 🗄️ Database — HikariCP connection pool + acquire time

---

## Тесты

```bash
# Все тесты (unit + интеграционные)
./gradlew :payment-service:test

# Только unit-тесты (без Docker)
./gradlew :payment-service:test --tests "com.example.paymentservice.service.*"
./gradlew :payment-service:test --tests "com.example.paymentservice.mapper.*"

# Только интеграционные тесты (требуется Docker)
./gradlew :payment-service:test --tests "com.example.paymentservice.it.*"

# С отчётом JaCoCo
./gradlew :payment-service:jacocoTestReport
# Отчёт: payment-service/build/reports/jacoco/test/html/index.html
```

---

## Схема базы данных

```
payment_providers              — провайдеры (FPP и др.)
payment_methods                — методы оплаты (CARD, BANK_TRANSFER)
payment_method_definitions     — доступность метода по валюте и стране
payment_method_required_fields — поля, обязательные для заполнения пользователем
payments                       — история платежей (PENDING → COMPLETED | FAILED)
```

Миграции: `src/main/resources/db/migration/V1–V6`.

---

## payment-service-api-client

Отдельный Gradle submodule для публикации DTO в Nexus. Используется `individuals-api` для типизированного взаимодействия.

```bash
# Публикация в Nexus
./gradlew :payment-service:payment-service-api-client:publish

# Публикация в mavenLocal (для локальной разработки)
./gradlew :payment-service:payment-service-api-client:publishToMavenLocal
```