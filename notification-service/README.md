# Notification Service

Микросервис для приёма, хранения и доставки уведомлений пользователям.

## Функциональность

- Приём уведомлений через Kafka (топик `notification.created`, Avro + Schema Registry)
- Сохранение уведомлений в PostgreSQL
- Отправка email при регистрации пользователя (`subject = REGISTRATION`)
- REST API: получение уведомлений пользователя и обновление статуса

## Технологии

- Java 17, Spring Boot 3.5.0
- Spring Data JPA, Flyway, PostgreSQL 16
- Apache Kafka, Avro, Confluent Schema Registry
- Spring Mail (JavaMailSender)
- Spring Security
- TestContainers (PostgreSQL), JUnit 5, Mockito, AssertJ
- JaCoCo, Logback (JSON), Prometheus, OTEL Java Agent
- Docker, Docker Compose

## Структура проекта

```
notification-service/
├── src/main/avro/
│   └── notification-created.avsc       # Avro-схема входящих событий
├── src/main/java/com/example/notificationservice/
│   ├── config/          # AppConfig, KafkaConsumerConfig (@Profile("!test")), SecurityConfig
│   ├── controller/      # NotificationController
│   │   └── dto/         # NotificationResponse (record), NotificationStatusRequest (record)
│   ├── email/           # EmailProperties (@ConfigurationProperties), EmailService
│   ├── entity/          # BaseEntity, Notification, NotificationStatus
│   ├── exception/       # GlobalExceptionHandler, NotificationNotFoundException
│   ├── kafka/           # NotificationKafkaConsumer (@Profile("!test"))
│   ├── mapper/          # NotificationMapper
│   ├── repository/      # NotificationRepository
│   └── service/         # NotificationService (оркестратор), NotificationPersistenceService (@Transactional)
└── src/main/resources/
    ├── application.yml
    ├── logback-spring.xml
    └── db/migration/
        └── V1__create_notifications_table.sql
```

## Переменные окружения

| Переменная | Описание | Дефолт |
|---|---|---|
| `SERVER_PORT` | Порт сервиса | `8087` |
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://localhost:5440/notification` |
| `SPRING_DATASOURCE_USERNAME` | Пользователь БД | `notification` |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД | — |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `SCHEMA_REGISTRY_URL` | URL Schema Registry | `http://localhost:8081` |
| `KAFKA_TOPIC_NOTIFICATION_CREATED` | Имя Kafka топика | `notification.created` |
| `MAIL_HOST` | SMTP хост | `localhost` |
| `MAIL_PORT` | SMTP порт | `1025` |
| `MAIL_USERNAME` | SMTP логин | — |
| `MAIL_PASSWORD` | SMTP пароль | — |
| `NOTIFICATION_EMAIL_FROM` | Адрес отправителя | `noreply@payment-system.com` |

## API

### GET /api/v1/notifications/{userUid}

Получение всех уведомлений пользователя, отсортированных по дате (новые первые).

**Ответ `200 OK`:**
```json
[
  {
    "uid": "550e8400-e29b-41d4-a716-446655440000",
    "userUid": "660e8400-e29b-41d4-a716-446655440001",
    "message": "Welcome to Payment System!",
    "subject": "REGISTRATION",
    "createdBy": "individuals-api",
    "recipientEmail": "user@example.com",
    "status": "NEW",
    "createdAt": "2024-01-15T10:30:00",
    "modifiedAt": null
  }
]
```

### PATCH /api/v1/notifications/{id}/status

Обновление статуса уведомления (`NEW → COMPLETED`).

**Тело запроса:**
```json
{ "status": "COMPLETED" }
```

**Ответы:**

| Код | Описание |
|---|---|
| `200 OK` | Статус обновлён |
| `400 Bad Request` | Невалидный статус |
| `404 Not Found` | Уведомление не найдено |

### GET /actuator/health

```bash
curl http://localhost:8087/actuator/health
```

## Kafka

Топик: `notification.created`
Формат: Avro + Schema Registry
Consumer group: `notification-service`

**Avro-схема (`notification-created.avsc`):**
```json
{
  "namespace": "com.example.kafka",
  "name": "NotificationCreated",
  "fields": [
    {"name": "message",        "type": "string"},
    {"name": "userUid",        "type": "string"},
    {"name": "subject",        "type": "string"},
    {"name": "createdBy",      "type": "string"},
    {"name": "recipientEmail", "type": ["null", "string"], "default": null}
  ]
}
```

**Логика обработки:**
- Событие сохраняется в БД со статусом `NEW`
- Если `subject = REGISTRATION` и `recipientEmail` не пустой → отправляется email

## Локальный запуск

### Предварительные требования

- Docker Desktop
- Java 17+

### Зависимости из docker-compose

```bash
# Schema Registry и Kafka должны быть запущены
docker compose up kafka zookeeper schema-registry notification-db -d
```

### Запуск сервиса

```bash
./gradlew :notification-service:bootRun
```

## Тестирование

```bash
# Генерация Avro Java-классов (обязательно перед первым запуском тестов)
./gradlew :notification-service:generateAvroJava

# Все тесты (требуется Docker для IT-тестов)
./gradlew :notification-service:test

# Только unit-тесты (без Docker)
./gradlew :notification-service:test --tests "com.example.notificationservice.service.*"
./gradlew :notification-service:test --tests "com.example.notificationservice.email.*"
./gradlew :notification-service:test --tests "com.example.notificationservice.mapper.*"

# Только IT-тесты (требуется Docker)
./gradlew :notification-service:test --tests "com.example.notificationservice.it.*"

# JaCoCo отчёт
./gradlew :notification-service:jacocoTestReport
open notification-service/build/reports/jacoco/test/html/index.html
```

## Архитектурные решения

**Оркестратор без `@Transactional`** — `NotificationService` не держит транзакцию во время SMTP-вызова. Транзакция открывается и закрывается внутри `NotificationPersistenceService`, email отправляется уже после коммита.

**`@Profile("!test")`** на `KafkaConsumerConfig` и `NotificationKafkaConsumer` — Kafka-инфраструктура не инициализируется в тестовом профиле, что позволяет IT-тестам стартовать без реального Kafka-брокера.

**Avro + Schema Registry** — события `NotificationCreated` десериализуются через `KafkaAvroDeserializer` с `specific.avro.reader=true`. Java-классы генерируются из `.avsc` схемы плагином `com.github.davidmc24.gradle.plugin.avro`.

## Схема БД

```sql
notifications (
  uid             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at      TIMESTAMP NOT NULL,
  modified_at     TIMESTAMP,
  user_uid        UUID NOT NULL,
  message         TEXT NOT NULL,
  subject         VARCHAR(255) NOT NULL,
  created_by      VARCHAR(255) NOT NULL,
  recipient_email VARCHAR(255),
  status          VARCHAR(20) NOT NULL DEFAULT 'NEW'
)
```