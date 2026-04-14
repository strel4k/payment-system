# Webhook Collector Service

Микросервис для приёма и обработки webhook-уведомлений от внешних платёжных провайдеров.

## Функциональность

- Приём `POST /api/v1/webhooks/payment-provider` от Fake Payment Provider
- Двухуровневая валидация безопасности: токен источника (X-Webhook-Token) + HMAC-SHA256 подпись тела (X-Webhook-Signature)
- Сохранение известных уведомлений в `payment_provider_callbacks`
- Сохранение неизвестных уведомлений в `unknown_callbacks`
- Публикация события `payment.status.updated` в Kafka для Transaction Service

## Технологии

- Java 17, Spring Boot 3.5.0
- Spring Data JPA, Flyway, PostgreSQL 16
- Spring Kafka, Apache Kafka
- Spring Security
- TestContainers (PostgreSQL + Kafka), JUnit 5, Mockito
- JaCoCo, Logback (JSON)
- Docker, Docker Compose

## Структура проекта

```
webhook-collector-service/
├── src/main/java/com/example/webhookcollector/
│   ├── config/          # AppConfig, KafkaConfig, SecurityConfig
│   ├── controller/      # WebhookController
│   │   └── dto/         # WebhookPayload (record)
│   ├── entity/          # BaseEntity, PaymentProviderCallback,
│   │                    # VerificationCallback, UnknownCallback, CallbackType
│   ├── exception/       # GlobalExceptionHandler, WebhookAuthenticationException
│   ├── kafka/           # KafkaProducer
│   ├── mapper/          # WebhookMapper
│   ├── repository/      # Spring Data JPA репозитории (3 шт.)
│   ├── security/        # WebhookSecurityService, WebhookSecurityProperties
│   └── service/         # WebhookService, WebhookPersistenceService
└── src/main/resources/
    ├── application.yml
    ├── logback-spring.xml
    └── db/migration/
        └── V1__create_webhook_tables.sql
```

## Переменные окружения

| Переменная | Описание | Дефолт |
|---|---|---|
| `SERVER_PORT` | Порт сервиса | `8086` |
| `SPRING_DATASOURCE_URL` | JDBC URL базы данных | `jdbc:postgresql://localhost:5439/webhook` |
| `SPRING_DATASOURCE_USERNAME` | Пользователь БД | `webhook` |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД | — |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `WEBHOOK_SECRET_TOKEN` | Токен идентификации источника (X-Webhook-Token) | — |
| `WEBHOOK_HMAC_SECRET` | Секрет для HMAC-SHA256 подписи (X-Webhook-Signature) | — |
| `KAFKA_TOPIC_PAYMENT_STATUS_UPDATED` | Имя Kafka топика | `payment.status.updated` |
| `KAFKA_TOPIC_PARTITIONS` | Количество партиций топика | `3` |
| `KAFKA_TOPIC_REPLICAS` | Количество реплик топика | `1` |

## Локальный запуск

### Предварительные требования
- Docker Desktop
- Java 17+

### Запуск через Docker Compose
```bash
# Из корня монорепо payment-system
docker compose up webhook-collector-service
```

### Запуск для разработки
```bash
# Зависимости (PostgreSQL + Kafka)
docker compose up webhook-db kafka -d

# Сервис локально
./gradlew :webhook-collector-service:bootRun
```

## API

### POST /api/v1/webhooks/payment-provider

Приём webhook-уведомления от платёжного провайдера.

**Заголовки:**

| Заголовок | Описание |
|---|---|
| `X-Webhook-Token` | Токен идентификации источника |
| `X-Webhook-Signature` | HMAC-SHA256 подпись тела запроса |
| `Content-Type` | `application/json` |

**Тело запроса:**
```json
{
  "providerTransactionUid": "550e8400-e29b-41d4-a716-446655440000",
  "type": "PAYMENT_STATUS_UPDATED",
  "provider": "FPP",
  "status": "COMPLETED"
}
```

**Ответы:**

| Код | Описание |
|---|---|
| `200 OK` | Уведомление успешно принято и обработано |
| `401 Unauthorized` | Неверный токен или подпись |
| `400 Bad Request` | Невалидный JSON |
| `500 Internal Server Error` | Внутренняя ошибка |

### Пример запроса (curl)

```bash
BODY='{"providerTransactionUid":"550e8400-e29b-41d4-a716-446655440000","type":"PAYMENT_STATUS_UPDATED","provider":"FPP","status":"COMPLETED"}'
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "webhook-hmac-secret" | awk '{print $2}')

curl -X POST http://localhost:8086/api/v1/webhooks/payment-provider \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Token: webhook-secret-token" \
  -H "X-Webhook-Signature: $SIGNATURE" \
  -d "$BODY"
```

### GET /actuator/health

```bash
curl http://localhost:8086/actuator/health
```

## Тестирование

```bash
# Все тесты (unit + integration)
./gradlew :webhook-collector-service:test

# Только unit-тесты (без Docker)
./gradlew :webhook-collector-service:test --tests "com.example.webhookcollector.security.*"
./gradlew :webhook-collector-service:test --tests "com.example.webhookcollector.service.*"

# Только интеграционные (требуется Docker)
./gradlew :webhook-collector-service:test --tests "com.example.webhookcollector.it.*"

# JaCoCo отчёт
./gradlew :webhook-collector-service:jacocoTestReport
open webhook-collector-service/build/reports/jacoco/test/html/index.html
```

## Безопасность

Двухуровневая защита входящих запросов:

1. **X-Webhook-Token** — идентифицирует источник запроса как доверенный
2. **X-Webhook-Signature** — HMAC-SHA256 подпись тела, гарантирует целостность

Сравнение подписи через `MessageDigest.isEqual()` — защита от timing-атак.
Все строки обрабатываются в `UTF-8` — платформонезависимо.

## Схема БД

```sql
-- Известные callback'и от платёжного провайдера
payment_provider_callbacks (uid, created_at, updated_at, body, provider_transaction_uid, type, provider)

-- Неизвестные / нераспознанные callback'и (для анализа)
unknown_callbacks (uid, created_at, updated_at, body)

-- Verification callback'и (задел на будущее)
verification_callbacks (uid, created_at, modified_at, body, transaction_uid, profile_uid, status, type)
```