# Currency Rate Service

[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

Микросервис для получения, хранения и предоставления актуальных курсов валют. Интегрируется с внешним API `exchangerate-api.com`, рассчитывает cross-rates через USD как базовую валюту и применяет корректирующие коэффициенты.

---

## 🎯 Функциональность

- ✅ **Автоматическое обновление курсов** — по расписанию через ShedLock (distributed lock)
- ✅ **Cross-rate расчёт** — все пары через USD: `rate(A→B) = USD→B / USD→A`
- ✅ **Корректирующие коэффициенты** — `rate_final = rate_raw * factor` из таблицы `rate_correction_factors`
- ✅ **REST API** — получение курса по паре валют, исторических курсов
- ✅ **OpenAPI** — автогенерация DTO из YAML спецификации
- ✅ **Prometheus метрики** — success/failure счётчики, длительность обновления
- ✅ **JSON логирование** — Logstash encoder для Loki
- ✅ **Distributed Tracing** — OpenTelemetry + Tempo
- ✅ **TestContainers** — интеграционные тесты с реальным PostgreSQL

---

## 🏗️ Архитектура

```
                    ┌─────────────────────────────┐
                    │    exchangerate-api.com     │
                    │    (External Rate Provider) │
                    └──────────────┬──────────────┘
                                   │ HTTP (base rates USD→X)
                                   ▼
┌──────────────────────────────────────────────────────────────┐
│                  Currency Rate Service (8085)                │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                  Scheduler (ShedLock)                   │ │
│  │  @Scheduled → acquireLock → fetchBaseRates →            │ │
│  │  calculateCrossRates → applyCorrectionFactors → save    │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌──────────────────┐    ┌───────────────────────────────┐   │
│  │  REST Controller │    │      ExchangeRateService      │   │
│  │                  │    │                               │   │
│  │ GET /rates       │───▶│ getRate(from, to, timestamp)  │   │
│  │ GET /currencies  │    │ updateRates()                 │   │
│  │ GET /providers   │    │ applyCorrectionFactor()       │   │
│  └──────────────────┘    └──────────────┬────────────────┘   │
│                                         │                    │
│                          ┌──────────────▼────────────────┐   │
│                          │         Repositories          │   │
│                          │ ConversionRateRepository      │   │
│                          │ CurrencyRepository            │   │
│                          │ RateProviderRepository        │   │
│                          │ RateCorrectionFactorRepo      │   │
│                          └───────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
                    ┌──────────────────────────┐
                    │    currency-postgres     │
                    │      (Port 5436)         │
                    │                          │
                    │  currencies              │
                    │  rate_providers          │
                    │  conversion_rates        │
                    │  rate_correction_factors │
                    │  shedlock                │
                    └──────────────────────────┘
```

---

## 🌐 API

### GET /api/v1/rates

Получить актуальный курс валютной пары.

```bash
# Актуальный курс
curl "http://localhost:8085/api/v1/rates?from=USD&to=EUR"

# Исторический курс
curl "http://localhost:8085/api/v1/rates?from=USD&to=EUR&timestamp=2026-03-01T10:00:00"
```

**Response:**
```json
{
  "fromCurrency": "USD",
  "toCurrency": "EUR",
  "rate": 0.85420000,
  "rateBeginTime": "2026-03-04T12:00:00",
  "rateEndTime": "2026-03-04T14:00:00",
  "providerCode": "EXCHANGERATE_API"
}
```

### GET /api/v1/currencies

Список активных валют.

```bash
curl "http://localhost:8085/api/v1/currencies"
```

### GET /api/v1/rate-providers

Список активных провайдеров курсов.

```bash
curl "http://localhost:8085/api/v1/rate-providers"
```

### GET /actuator/health

```bash
curl "http://localhost:8085/actuator/health"
```

---

## 💱 Поддерживаемые валюты

| Код | Валюта |
|-----|--------|
| USD | US Dollar |
| EUR | Euro |
| RUB | Russian Ruble |
| GBP | British Pound |
| CNY | Chinese Yuan |
| JPY | Japanese Yen |
| CHF | Swiss Franc |
| CAD | Canadian Dollar |
| AUD | Australian Dollar |
| TRY | Turkish Lira |

**Итого:** 10 валют × 9 пар = **90 валютных пар**

---

## 🔄 Логика обновления курсов

```
1. ShedLock acquires distributed lock (maxLockTime: PT10M, minLockTime: PT5M)
2. ExternalRateProviderClient.fetchBaseRates()
   → GET https://v6.exchangerate-api.com/v6/{apiKey}/latest/USD
   → returns { "USD": 1.0, "EUR": 0.854, "RUB": 89.5, ... }
3. Для каждой пары (source, destination):
   crossRate = USD→destination / USD→source
4. Применить корректирующий коэффициент:
   finalRate = crossRate * factor  (из rate_correction_factors, default = 1.0)
5. Инвалидировать текущий активный курс (rateEndTime = now)
6. Сохранить новый курс (rateBeginTime = now, rateEndTime = now + 2h)
```

---

## 🎛️ Корректирующие коэффициенты

Таблица `rate_correction_factors` позволяет применять поправочный коэффициент к рыночному курсу перед сохранением. Используется для моделирования спреда или наценки.

**Пример seeded-данных:**

| source | destination | factor | Описание |
|--------|-------------|--------|----------|
| USD | EUR | 0.9980 | USD→EUR spread |
| EUR | USD | 0.9980 | EUR→USD spread |
| USD | RUB | 1.0020 | USD→RUB spread |
| RUB | USD | 0.9970 | RUB→USD spread |

> Для пар без записи в таблице коэффициент = 1.0 (без изменений).

---

## 🗄️ Схема базы данных

```sql
-- Валюты
currencies (code PK, name, symbol, active, created_at)

-- Провайдеры курсов
rate_providers (id, provider_code UNIQUE, name, base_url, api_key, active, priority)

-- Курсы конвертации
conversion_rates (id, source_code FK, destination_code FK, rate NUMERIC(18,8),
                  rate_begin_time, rate_end_time, provider_code, created_at)

-- Корректирующие коэффициенты
rate_correction_factors (id, source_code FK, destination_code FK,
                         factor NUMERIC(10,6) DEFAULT 1.0,
                         description, active, created_at, modified_at)

-- ShedLock (distributed locking)
shedlock (name PK, lock_until, locked_at, locked_by)
```

---

## ⚙️ Конфигурация

| Параметр | Env Variable | Default | Описание |
|----------|-------------|---------|----------|
| Server port | `SERVER_PORT` | `8085` | HTTP порт |
| DB URL | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5436/currency` | PostgreSQL |
| DB User | `SPRING_DATASOURCE_USERNAME` | `currency` | |
| DB Password | `SPRING_DATASOURCE_PASSWORD` | `currency` | |
| External API Key | `EXCHANGE_RATE_API_KEY` | — | exchangerate-api.com ключ |
| External API URL | `EXCHANGE_RATE_API_BASE_URL` | `https://v6.exchangerate-api.com/v6` | |
| Update cron | `RATE_UPDATE_CRON` | `0 0 * * * *` | Каждый час |

---

## 📊 Метрики Prometheus

| Метрика | Тип | Описание |
|---------|-----|----------|
| `rate_update_success_total` | Counter | Успешные обновления курсов |
| `rate_update_failure_total` | Counter | Ошибки обновления |
| `rate_update_duration_seconds` | Timer | Длительность обновления |
| `http_server_requests_seconds` | Timer | HTTP latency (стандарт Spring) |
| `jvm_memory_used_bytes` | Gauge | Использование памяти JVM |

Доступны по адресу: `http://localhost:8085/actuator/prometheus`

---

## 🧪 Тестирование

```bash
# Unit тесты
./gradlew :currency-rate-service:test

# Интеграционные тесты (TestContainers + реальный PostgreSQL)
./gradlew :currency-rate-service:integrationTest
```

### Покрытие тестами

- **ExchangeRateServiceTest** — unit-тесты: cross-rate расчёт, применение коэффициентов, обработка ошибок внешнего API
- **CurrencyRateIT** — интеграционные тесты: полный цикл обновления курсов, REST API, ShedLock

---

## 🔧 Локальная разработка

### Запуск инфраструктуры

```bash
docker compose up -d currency-postgres
```

### Запуск сервиса

```bash
./gradlew :currency-rate-service:bootRun
```

### Пересборка Docker образа

```bash
./gradlew :currency-rate-service:bootJar -x test
docker cp currency-rate-service/build/libs/currency-rate-service-0.0.1-SNAPSHOT.jar \
  currency-rate-service:/app/app.jar
docker restart currency-rate-service
```

---

## 🐛 Troubleshooting

### Курсы не обновляются

```bash
# Проверяем ShedLock — нет ли застрявшего лока
docker exec currency-postgres psql -U currency -d currency \
  -c "SELECT * FROM shedlock;"

# Сбросить лок вручную (если завис)
docker exec currency-postgres psql -U currency -d currency \
  -c "DELETE FROM shedlock WHERE name = 'updateRates';"
```

### Нет курсов в БД

```bash
# Смотрим логи
docker logs currency-rate-service 2>&1 | grep -i "rate\|update\|error" | tail -20

# Проверяем таблицу курсов
docker exec currency-postgres psql -U currency -d currency \
  -c "SELECT source_code, destination_code, rate, rate_begin_time FROM conversion_rates LIMIT 10;"
```

### Внешний API недоступен

```bash
# Проверяем подключение из контейнера
docker exec currency-rate-service wget -qO- \
  "https://v6.exchangerate-api.com/v6/YOUR_KEY/latest/USD" | head -c 200
```

---

## 📄 License

This project is licensed under the MIT License.