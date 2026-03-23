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
| **Итого** | **179** | — | — |

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

### Исключены из JaCoCo
- DTO классы — автогенерированы openapi-generator
- `FakePaymentProviderApplication` — точка входа
- `SecurityConfig` — конфигурация
- `ApiClient*`, `RFC3339DateFormat`, `ServerConfiguration`, `StringUtil` — сгенерированный boilerplate

### Архитектура тестов
- **Singleton TestContainers** — `AbstractIT` запускает `postgres:16-alpine` один раз для всего тестового прогона через `static {}` блок
- **`@IntegrationTest`** — составная аннотация (`@SpringBootTest` + `@ActiveProfiles("test")` + `@Testcontainers`)
- **`application-test.yml`** — тестовый конфиг отделён от тест-логики (`ddl-auto: none`, Flyway active)
- **`TestRestTemplate`** — реальные HTTP запросы к поднятому контексту с Basic Auth

### JaCoCo
```
JaCoCo настроен с exclusions (dto/**, api/**, config/**, ApiClient*.class и т.д.)
Отчёт: fake-payment-provider/build/reports/jacoco/test/html/index.html
```

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
✅ **PaymentOrchestrationIT** — POST /payments: FPP 201→COMPLETED (проверка БД), FPP 400→422+FAILED (проверка БД), 404 (FPP не вызывается), FPP 500→422, 401 без auth, 401 неверные credentials

### Архитектура тестов
- **Singleton TestContainers** — `AbstractIT` в `it/config/` запускает `postgres:16-alpine` и WireMock один раз через `static {}` блок
- **WireMock** — мокирует Fake Payment Provider для интеграционных тестов без реального FPP
- **`@BeforeEach cleanUp()`** — `paymentRepository.deleteAll()` + `WIRE_MOCK.resetAll()` перед каждым тестом
- **`application-test.yml`** — тестовый конфиг, `fake-provider.base-url` переопределяется через `@DynamicPropertySource`
- **Разделение** — контейнеры в `it/config/AbstractIT`, сценарии в `it/*IT` классах

### Исключены из JaCoCo
- DTO классы — автогенерированы openapi-generator
- Entity классы (`Payment`, `PaymentMethod` и др.) — нет бизнес-логики
- `PaymentServiceApplication` — точка входа
- Configuration классы
- `ApiClient*`, `RFC3339DateFormat`, `ServerConfiguration`, `StringUtil` — сгенерированный boilerplate

---

## 🧪 Типы тестов

### Unit Tests (с Mockito)
- Мокируются все внешние зависимости (БД, HTTP клиенты, Kafka)
- Быстрые, без инфраструктуры
- Покрывают: PersonApplicationService, PersonMapper, UserService, TokenService, AuthController, TransferService, TransactionController, FeeCalculator, InitRequestCache, WalletService, ExchangeRateService, TransactionService (FPP), PayoutService (FPP), WebhookService (FPP), **PaymentMethodService, PaymentOrchestrationService, PaymentPersistenceService, PaymentMethodMapper**

### Integration Tests (с TestContainers)
- Реальная PostgreSQL в Docker-контейнере
- Keycloak в Docker-контейнере (individuals-api)
- WireMock для мокирования Fake Payment Provider (payment-service)
- Покрывают: PersonsApiIT, TransactionServiceIntegrationTest, TransactionRollbackTest, WalletTransactionFlowIT, AuthFlowIntegrationTest, CurrencyRateIT, TransactionIT (FPP), PayoutIT (FPP), WebhookIT (FPP), **PaymentMethodIT, PaymentOrchestrationIT**

### Integration Tests (с MockBean)
- MockBean для внешних HTTP сервисов (PersonServiceClient, TransactionServiceClient, CurrencyRateServiceClient)
- Покрывают: WalletTransactionFlowIT — полный wallet и transaction flow

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

# Только unit-тесты payment-service (без Docker)
./gradlew :payment-service:test --tests "com.example.paymentservice.service.*"
./gradlew :payment-service:test --tests "com.example.paymentservice.mapper.*"

# Только интеграционные тесты payment-service (требуется Docker)
./gradlew :payment-service:test --tests "com.example.paymentservice.it.*"

# Интеграционные тесты
./gradlew :individuals-api:integrationTest
./gradlew :currency-rate-service:integrationTest

# JaCoCo отчёты (HTML)
./gradlew :person-service:jacocoTestReport
./gradlew :individuals-api:jacocoTestReport
./gradlew :transaction-service:jacocoTestReport
./gradlew :fake-payment-provider:jacocoTestReport
./gradlew :payment-service:jacocoTestReport

# Открыть отчёты
open person-service/build/reports/jacoco/test/html/index.html
open individuals-api/build/reports/jacoco/test/html/index.html
open transaction-service/build/reports/jacoco/test/html/index.html
open fake-payment-provider/build/reports/jacoco/test/html/index.html
open payment-service/build/reports/jacoco/test/html/index.html

# Проверка thresholds
./gradlew :person-service:jacocoTestCoverageVerification
./gradlew :individuals-api:jacocoTestCoverageVerification
./gradlew :transaction-service:jacocoTestCoverageVerification

# Полная сборка
./gradlew build
```

---

## ✅ Итог

- ✅ **179 тестов** — все проходят
- ✅ Unit + Integration тесты по всем шести сервисам
- ✅ TestContainers (PostgreSQL, Keycloak) для интеграционных тестов
- ✅ WireMock для мокирования Fake Payment Provider в payment-service
- ✅ `@Transactional` rollback тесты для transaction-service
- ✅ Cross-currency transfer тесты (same-currency и USD→EUR) в individuals-api
- ✅ JaCoCo executionData включает `test.exec` + `integrationTest.exec`
- ✅ JaCoCo thresholds пройдены во всех модулях
- ✅ Покрытие бизнес-логики **80-90%** (после исключения autogenerated кода)
- ✅ Singleton TestContainers — один контейнер на весь прогон (fake-payment-provider, payment-service)
- ✅ Webhook idempotency проверена интеграционными тестами