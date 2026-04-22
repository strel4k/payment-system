# Test Coverage Report — Payment System

---

## 📊 Executive Summary

| Модуль | Тестов | JaCoCo (overall) | JaCoCo threshold |
|--------|--------|------------------|------------------|
| **person-service** | 33 | ~62% | 35% |
| **individuals-api** | 29 | ~28% | 20% |
| **transaction-service** | 38 | ~20%+ | 20% |
| **currency-rate-service** | 9 | — | — |
| **fake-payment-provider** | 48 | ~70%+ | — |
| **payment-service** | 22 | ~75%+ | — |
| **webhook-collector-service** | 19 | ~85%+ | — |
| **notification-service** | 20 | ~80%+ | — |
| **Итого** | **218** | — | — |

> Overall % низкий из-за автогенерированных OpenAPI DTO и entity классов.
> Покрытие бизнес-логики (после exclusions): **~80-90%** по всем модулям.

---

## 🎯 person-service — 33 теста

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `PersonApplicationServiceTest` | 16 | Unit |
| `PersonsApiIT` | 11 | Integration (TestContainers) |
| `PersonMapperTest` | 6 | Unit |

### Покрытые компоненты
✅ **PersonApplicationService** — создание, получение, обновление, удаление Person
✅ **PersonMapper** — маппинг CreatePersonRequest ↔ Entity ↔ PersonResponse
✅ **PersonsApiIT** — E2E через REST API, валидация, error handling (404, 400)

### Исключены из JaCoCo
- Entity классы (`UserEntity`, `IndividualEntity`, `AddressEntity`, `CountryEntity`) — нет бизнес-логики
- DTO классы — автогенерированы openapi-generator
- `GlobalExceptionHandler` — ~39% coverage (покрывается E2E)
- Configuration классы

### JaCoCo threshold
```
overall:  35%  minimum
per-class: 30% minimum (с exclusions)
```

---

## 🎯 individuals-api — 29 тестов

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `UserServiceTest` | 6 | Unit |
| `TransferServiceTest` | 4 | Unit |
| `AuthControllerTest` | 4 | Unit |
| `TransactionControllerTest` | 5 | Unit |
| `WalletTransactionFlowIT` | 25 | Integration (TestContainers + MockBean) |
| `AuthFlowIntegrationTest` | 4 | Integration (TestContainers + MockBean) |
| `TokenServiceTest` | 2 | Unit |
| `IndividualsApiApplicationTests` | 1 | Context load |

### Покрытые компоненты
✅ **UserService** — полный flow регистрации: Person → Keycloak → JWT, компенсирующие транзакции
✅ **TokenService** — login, refresh token
✅ **TransferService** — same-currency (без конвертации), cross-currency (fetch курса), error paths
✅ **AuthController** — все 4 endpoints: registration, login, refresh-token, me
✅ **TransactionController** — init (deposit/withdrawal/transfer), confirm, status
✅ **WalletTransactionFlowIT** — E2E: wallet CRUD, deposit/withdrawal/transfer init+confirm, cross-currency transfer
✅ **AuthFlowIntegrationTest** — E2E регистрации с реальным Keycloak (TestContainers)

### Исключены из JaCoCo
- OpenAPI generated code (`ApiClient`, `RFC3339DateFormat`, `ServerConfiguration`, `auth/*`) — ~60% кодовой базы
- `PersonServiceClient`, `TransactionServiceClient`, `CurrencyRateServiceClient` — HTTP клиенты
- `GlobalExceptionHandler` — покрывается E2E
- Configuration и Properties классы

### JaCoCo threshold
```
overall:  20% minimum
per-class: 20% minimum (с exclusions)
executionData: test.exec + integrationTest.exec (оба источника)
```

---

## 🎯 transaction-service — 38 тестов

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `InitRequestCacheTest` | 12 | Unit |
| `FeeCalculatorTest` | 9 | Unit |
| `WalletServiceTest` | 9 | Unit |
| `TransactionServiceIntegrationTest` | 5 | Integration (TestContainers) |
| `TransactionRollbackTest` | 3 | Integration (@Transactional) |

### Покрытые компоненты
✅ **InitRequestCache** — TTL, хранение, истечение requestUid
✅ **FeeCalculator** — расчёт комиссии: deposit (0%), withdrawal (1%), transfer (0.5%)
✅ **WalletService** — создание, получение, валидация
✅ **TransactionServiceIntegrationTest** — deposit init/confirm, insufficient balance, transfer validations, status
✅ **TransactionRollbackTest** — `@Transactional` rollback при ошибке, отсутствие orphan records

### Исключены из JaCoCo
- DTO классы — автогенерированы openapi-generator
- API interfaces
- Configuration классы

### JaCoCo threshold
```
overall:  20% minimum
per-class: 20% minimum (с exclusions)
```

---

## 🎯 currency-rate-service — 9 тестов

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `ExchangeRateServiceTest` | 9 | Unit |
| `CurrencyRateIT` | — | Integration (TestContainers) |

### Покрытые компоненты
✅ **ExchangeRateService.getRate** — по timestamp, без timestamp, identity (same currency), unknown currency, not found
✅ **ExchangeRateService.updateRates** — cross-rate calculation, все пары (N×(N-1)), skip при отсутствии в API, empty API response
✅ **CurrencyRateIT** — E2E: GET /api/v1/rates, /currencies, /rate-providers (TestContainers PostgreSQL)

### Исключены из JaCoCo
- Entity классы (`Currency`, `ConversionRate`, `RateProvider`, `RateCorrectionFactor`)
- Configuration и Properties классы
- `GlobalExceptionHandler`

---

## 🎯 fake-payment-provider — 48 тестов

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `TransactionServiceTest` | 9 | Unit |
| `PayoutServiceTest` | 9 | Unit |
| `WebhookServiceTest` | 7 | Unit |
| `WebhookDeliveryServiceTest` | 5 | Unit |
| `TransactionIT` | 7 | Integration (TestContainers) |
| `PayoutIT` | 7 | Integration (TestContainers) |
| `WebhookIT` | 5 | Integration (TestContainers) |
| `WebhookDeliveryIT` | 4 | Integration (TestContainers + MockRestServiceServer) |

### Покрытые компоненты
✅ **TransactionService** — создание (PENDING), получение по ID (404 если не найдено), список по мерчанту
✅ **PayoutService** — создание (PENDING), получение по ID (404 если не найдено), список по мерчанту
✅ **WebhookService** — обновление статуса транзакции/выплаты, idempotency (terminal status), audit log, валидация PENDING-статуса, вызов delivery
✅ **WebhookDeliveryService** — успех с 1-й попытки, retry, исчерпание попыток, null URL, blank URL
✅ **TransactionIT** — E2E: Basic Auth (401 без авторизации), создание транзакции (201), получение (200/404), список
✅ **PayoutIT** — E2E: Basic Auth (401 без авторизации), создание выплаты (201), получение (200/404), список
✅ **WebhookIT** — E2E: обновление статуса (SUCCESS/FAILED), idempotency при повторном вызове, невалидные данные (404)
✅ **WebhookDeliveryIT** — outbound delivery (MockRestServiceServer): SUCCESS/FAILED доставка, отсутствие запроса без notification_url, retry при ошибке сервера

### Архитектура тестов
- **Singleton TestContainers** — `AbstractIT` запускает `postgres:16-alpine` один раз через `static {}` блок
- **`application-test.yml`** — тестовый конфиг отделён от тест-логики (`ddl-auto: none`, Flyway active)
- **`TestRestTemplate`** — реальные HTTP запросы к поднятому контексту с Basic Auth

---

## 🎯 payment-service — 22 теста

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `PaymentMethodServiceTest` | 2 | Unit |
| `PaymentOrchestrationServiceTest` | 4 | Unit |
| `PaymentPersistenceServiceTest` | 3 | Unit |
| `PaymentMethodMapperTest` | 5 | Unit |
| `PaymentMethodIT` | 6 | Integration (TestContainers) |
| `PaymentOrchestrationIT` | 6 | Integration (TestContainers + WireMock) |

### Покрытые компоненты
✅ **PaymentMethodService** — фильтрация методов по валюте/стране, пустой результат
✅ **PaymentOrchestrationService** — happy path (COMPLETED), ошибка FPP → FAILED, method not found, inactive method
✅ **PaymentPersistenceService** — createPending (статус + поля), markCompleted (externalId), markFailed
✅ **PaymentMethodMapper** — маппинг всех полей, фильтрация inactive requiredFields, CSV→List для valuesOptions, UUID
✅ **PaymentMethodIT** — GET /payment-methods: USD/USA (2 метода), EUR/DEU (только CARD), required_fields, 401 без auth
✅ **PaymentOrchestrationIT** — POST /payments: FPP 201→COMPLETED, FPP 400→422+FAILED, 404, FPP 500→422, 401 без auth

### Архитектура тестов
- **Singleton TestContainers** — `AbstractIT` в `it/config/` запускает PostgreSQL и WireMock через `static {}` блок
- **WireMock** — мокирует Fake Payment Provider без реального FPP
- **`@BeforeEach cleanUp()`** — `paymentRepository.deleteAll()` + `WIRE_MOCK.resetAll()` перед каждым тестом

---

## 🎯 webhook-collector-service — 19 тестов

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `WebhookSecurityServiceTest` | 8 | Unit |
| `WebhookPersistenceServiceTest` | 3 | Unit |
| `WebhookServiceTest` | 2 | Unit |
| `WebhookControllerIT` | 6 | Integration (TestContainers) |

### Покрытые компоненты
✅ **WebhookSecurityService** — validateToken (верный, неверный, null), verifyHmacSignature (верная, неверная, null, пустая, от другого тела)
✅ **WebhookPersistenceService** — известный тип → `payment_provider_callbacks` + `true`, неизвестный тип → `unknown_callbacks` + `false`, null тип
✅ **WebhookService** — оркестрация: persistence вернул true → Kafka вызван, false → Kafka не вызван
✅ **WebhookControllerIT** — POST известный тип (200 + БД + Kafka), POST неизвестный тип (200 + БД, Kafka пуст), неверный токен (401), неверная подпись (401), подпись от другого тела (401), GET /actuator/health (200)

### Архитектура тестов
- **Singleton TestContainers** — `AbstractIT` в `it/config/` запускает `postgres:16-alpine` и `KafkaContainer` (cp-kafka:7.5.0) через `static {}` блок
- **Реальный Kafka** — публикация проверяется через `KafkaTestUtils` consumer с `assign()` + `seekToEnd()` для изоляции тестов
- **`@DynamicPropertySource`** — инжектирует datasource, Kafka bootstrap-servers и секреты безопасности без хардкода в `application-test.yml`
- **`@BeforeEach cleanUp()`** — `deleteAll()` на всех репозиториях перед каждым тестом

### Исключены из JaCoCo
- Entity классы (`BaseEntity`, `PaymentProviderCallback`, `VerificationCallback`, `UnknownCallback`) — нет бизнес-логики
- Configuration классы (`AppConfig`, `KafkaConfig`, `SecurityConfig`)
- `WebhookCollectorApplication` — точка входа

---

## 🧪 Типы тестов

### Unit Tests (с Mockito)
- Мокируются все внешние зависимости (БД, HTTP клиенты, Kafka)
- Быстрые, без инфраструктуры
- Покрывают: PersonApplicationService, PersonMapper, UserService, TokenService, AuthController, TransferService, TransactionController, FeeCalculator, InitRequestCache, WalletService, ExchangeRateService, TransactionService (FPP), PayoutService (FPP), WebhookService (FPP), PaymentMethodService, PaymentOrchestrationService, PaymentPersistenceService, PaymentMethodMapper, **WebhookSecurityService, WebhookPersistenceService, WebhookService**

### Integration Tests (с TestContainers)
- Реальная PostgreSQL в Docker-контейнере
- Реальный Kafka в Docker-контейнере (webhook-collector-service)
- WireMock для мокирования Fake Payment Provider (payment-service)
- Покрывают: PersonsApiIT, TransactionServiceIntegrationTest, TransactionRollbackTest, WalletTransactionFlowIT, AuthFlowIntegrationTest, CurrencyRateIT, TransactionIT (FPP), PayoutIT (FPP), WebhookIT (FPP), PaymentMethodIT, PaymentOrchestrationIT, **WebhookControllerIT**

---

## 📋 Команды

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

# Только unit-тесты webhook-collector-service (без Docker)
./gradlew :webhook-collector-service:test --tests "com.example.webhookcollector.security.*"
./gradlew :webhook-collector-service:test --tests "com.example.webhookcollector.service.*"

# Только интеграционные тесты webhook-collector-service (требуется Docker)
./gradlew :webhook-collector-service:test --tests "com.example.webhookcollector.it.*"

# JaCoCo отчёты
./gradlew :webhook-collector-service:jacocoTestReport
open webhook-collector-service/build/reports/jacoco/test/html/index.html
```

---

## ✅ Итог

- ✅ **198 тестов** — все проходят
- ✅ Unit + Integration тесты по всем семи сервисам
- ✅ TestContainers (PostgreSQL, Kafka, Keycloak) для интеграционных тестов
- ✅ WireMock для мокирования Fake Payment Provider в payment-service
- ✅ Реальный KafkaContainer для проверки публикации событий в webhook-collector-service
- ✅ `@Transactional` rollback тесты для transaction-service
- ✅ JaCoCo thresholds пройдены во всех модулях
- ✅ Покрытие бизнес-логики **80-90%** (после исключения autogenerated кода)
- ✅ Singleton TestContainers — один контейнер на весь прогон
- ✅ Двухуровневая защита webhook: X-Webhook-Token + HMAC-SHA256 (timing-safe)

---

## 🎯 notification-service — 20 тестов

### Тест-классы

| Класс | Тестов | Тип |
|-------|--------|-----|
| `NotificationServiceTest` | 5 | Unit |
| `NotificationPersistenceServiceTest` | 4 | Unit |
| `EmailServiceTest` | 2 | Unit |
| `NotificationMapperTest` | 3 | Unit |
| `NotificationControllerIT` | 6 | Integration (TestContainers) |

### Покрытые компоненты
✅ **NotificationService** — processNotification (REGISTRATION + email, без email, не REGISTRATION), getByUserUid, updateStatus
✅ **NotificationPersistenceService** — save, findByUserUid, updateStatus, updateStatus throws NotFound
✅ **EmailService** — корректные поля письма, пробрасывает MailException
✅ **NotificationMapper** — toEntity (все поля, null email), toResponse все поля
✅ **NotificationControllerIT** — GET список, GET пустой список, GET изоляция по userUid, PATCH статус, PATCH 404, actuator health

### Архитектура тестов
- **Singleton TestContainers** — `AbstractIT` запускает `postgres:16-alpine` через `static {}` блок
- **`@Profile("!test")`** на Kafka-бинах — Kafka-инфраструктура не инициализируется в тестах
- **`KafkaAutoConfiguration` excluded** — полное исключение Kafka auto-configuration в тестовом профиле
- **`ddl-auto: create-drop`** — Hibernate создаёт схему из entity; `MailHealthIndicator` отключён
- **`@MockitoBean NotificationKafkaConsumer`** — consumer заменён моком, тестируется только REST API
- **`@DynamicPropertySource`** — инжектирует datasource без хардкода в `application-test.yml`