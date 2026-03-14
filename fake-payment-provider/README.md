# Fake-payment-provider

Микросервис, эмулирующий внешний платёжный шлюз. Предоставляет REST API для создания транзакций пополнения и выплат, а также принимает webhook-уведомления об обновлении статусов.

---

## Стек технологий

| Компонент       | Технология                        |
|-----------------|-----------------------------------|
| Runtime         | Java 17, Spring Boot 3.5.0        |
| API             | Spring Web MVC, OpenAPI 3.0       |
| Безопасность    | Spring Security (HTTP Basic Auth) |
| БД              | PostgreSQL 16, Spring Data JPA    |
| Миграции        | Flyway                            |
| Метрики         | Micrometer + Prometheus           |
| Логирование     | Logback + Logstash JSON encoder   |
| Тесты           | JUnit 5, Mockito, TestContainers  |
| Сборка          | Gradle 8, OpenAPI Generator       |

---

## Архитектура

Диаграммы в формате PlantUML (C4) находятся в `docs/diagrams/`:

- `c4-container.puml` — контейнерная диаграмма
- `c4-component.puml` — компонентная диаграмма
- `sequence-transaction.puml` — flow создания транзакции + webhook
- `sequence-payout.puml` — flow создания выплаты + webhook
- `sequence-webhook.puml` — детальный webhook flow с idempotency check

---

## Запуск локально

### Через Docker Compose (рекомендуется)

```bash
# Из корня payment-system
docker-compose up fake-payment-provider fpp-postgres
```

Сервис будет доступен на `http://localhost:8090`.

### Локальный запуск (без Docker)

1. Поднять PostgreSQL на порту `5437`, создать базу `fpp`, пользователя `fpp`
2. Запустить приложение:

```bash
./gradlew :fake-payment-provider:bootRun
```

### Переменные окружения

| Переменная               | По умолчанию                              | Описание              |
|--------------------------|-------------------------------------------|-----------------------|
| `SERVER_PORT`            | `8090`                                    | Порт сервиса          |
| `SPRING_DATASOURCE_URL`  | `jdbc:postgresql://localhost:5437/fpp`    | JDBC URL базы данных  |
| `SPRING_DATASOURCE_USERNAME` | `fpp`                                 | Пользователь БД       |
| `SPRING_DATASOURCE_PASSWORD` | `fpp`                                 | Пароль БД             |
| `DB_POOL_SIZE`           | `10`                                      | Размер HikariCP пула  |

---

## Аутентификация

Все эндпоинты `/api/v1/**` защищены HTTP Basic Auth.

Тестовый мерчант (добавляется Flyway миграцией `V5`):

| Поле        | Значение     |
|-------------|--------------|
| merchantId  | `merchant-1` |
| secretKey   | `secret123`  |

Эндпоинты `/webhook/**` и `/actuator/**` открыты, аутентификация не требуется.

---

## API — примеры запросов

### Транзакции

#### Создать транзакцию

```bash
curl -s -X POST http://localhost:8090/api/v1/transactions \
  -u merchant-1:secret123 \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150.00,
    "currency": "USD",
    "method": "CARD",
    "description": "Пополнение счёта",
    "notificationUrl": "http://your-service/webhook/callback"
  }' | jq
```

Ответ `201 Created`:
```json
{
  "id": 1,
  "merchantId": "merchant-1",
  "amount": 150.0,
  "currency": "USD",
  "method": "CARD",
  "status": "PENDING",
  "createdAt": "2025-01-01T12:00:00Z",
  "description": "Пополнение счёта"
}
```

#### Получить транзакцию по ID

```bash
curl -s http://localhost:8090/api/v1/transactions/1 \
  -u merchant-1:secret123 | jq
```

#### Список транзакций за период

```bash
curl -s "http://localhost:8090/api/v1/transactions?start_date=2025-01-01T00:00:00&end_date=2025-12-31T23:59:59" \
  -u merchant-1:secret123 | jq
```

---

### Выплаты

#### Создать выплату

```bash
curl -s -X POST http://localhost:8090/api/v1/payouts \
  -u merchant-1:secret123 \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "currency": "EUR",
    "notificationUrl": "http://your-service/webhook/callback"
  }' | jq
```

Ответ `201 Created`:
```json
{
  "id": 1,
  "merchantId": "merchant-1",
  "amount": 500.0,
  "currency": "EUR",
  "status": "PENDING",
  "createdAt": "2025-01-01T12:00:00Z"
}
```

#### Получить выплату по ID

```bash
curl -s http://localhost:8090/api/v1/payouts/1 \
  -u merchant-1:secret123 | jq
```

#### Список всех выплат

```bash
curl -s http://localhost:8090/api/v1/payouts \
  -u merchant-1:secret123 | jq
```

#### Список выплат за период

```bash
curl -s "http://localhost:8090/api/v1/payouts?start_date=2025-01-01T00:00:00&end_date=2025-12-31T23:59:59" \
  -u merchant-1:secret123 | jq
```

---

### Webhook-уведомления

Эндпоинты для получения статусов от внешней сети. **Аутентификация не требуется.**

#### Обновить статус транзакции

```bash
curl -s -X POST http://localhost:8090/webhook/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "status": "SUCCESS"
  }'
```

#### Обновить статус с причиной отказа

```bash
curl -s -X POST http://localhost:8090/webhook/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "status": "FAILED",
    "reason": "Insufficient funds"
  }'
```

#### Обновить статус выплаты

```bash
curl -s -X POST http://localhost:8090/webhook/payout \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "status": "SUCCESS"
  }'
```

> **Idempotency:** повторный webhook на уже завершённую транзакцию/выплату (статус `SUCCESS` или `FAILED`) не изменит данные — запрос будет только сохранён в audit log таблицу `webhooks`.

---

## Мониторинг

| Эндпоинт                         | Описание                  |
|----------------------------------|---------------------------|
| `GET /actuator/health`           | Статус приложения         |
| `GET /actuator/prometheus`       | Метрики для Prometheus    |

Grafana доступна на `http://localhost:3000` (admin / admin).

---

## Тесты

```bash
# Все тесты (unit + интеграционные)
./gradlew :fake-payment-provider:test

# Только unit-тесты (без Docker)
./gradlew :fake-payment-provider:test --tests "com.example.payment.service.*"

# Только интеграционные тесты (требуется Docker)
./gradlew :fake-payment-provider:test --tests "com.example.payment.it.*"

# С отчётом JaCoCo
./gradlew :fake-payment-provider:jacocoTestReport
# Отчёт: fake-payment-provider/build/reports/jacoco/test/html/index.html
```

---

## Схема базы данных

```
merchants        — учётные данные мерчантов (BCrypt secretKey)
transactions     — транзакции пополнения (PENDING → SUCCESS | FAILED)
payouts          — выплаты (PENDING → SUCCESS | FAILED)
webhooks         — audit log всех входящих webhook-уведомлений
```

Миграции: `src/main/resources/db/migration/V1–V5`.