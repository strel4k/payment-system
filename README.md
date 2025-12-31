# Payment System — учебный проект (Individuals API + Observability + Keycloak)

**Стек**: Java (JDK локально), Spring Boot (WebFlux), Spring Security (JWT/OAuth2 Resource Server), OpenAPI, Actuator + Micrometer (Prometheus), Docker Compose, Grafana, Prometheus, Loki, Promtail, Keycloak 26.2, Postgres.

> Репозиторий демонстрирует аутентификацию через Keycloak, работу Individuals API и базовую наблюдаемость: метрики (Prometheus) + логи (Loki) + дашборды (Grafana).

---

## Содержание
- Состав репозитория
- Быстрый старт (Makefile)
- Ручной запуск (без Makefile)
- Порты и сервисы
- Наблюдаемость (Grafana/Prometheus/Loki)
- API и OpenAPI
- Тестирование
- Типовые ошибки и устранение

---

## Состав репозитория

```
payment-system/
├─ individuals-api/                 # Spring Boot WebFlux сервис (порт 8081)
│  ├─ src/main/java/...            # бизнес-код
│  ├─ src/main/resources/...       # конфиги приложения
│  ├─ src/test/...                 # тесты
│  ├─ openapi/individuals-api.yaml # спецификация API
│  ├─ postman/                     # Postman коллекция
│  ├─ Dockerfile
│  └─ gradlew / build.gradle.kts
│
├─ infrastructure/
│  ├─ keycloak/realm-config.json
│  ├─ prometheus/prometheus.yml
│  ├─ loki/loki-config.yaml
│  ├─ promtail/promtail-config.yml
│  └─ grafana/
│     ├─ provisioning/...
│     └─ dashboards/
│        ├─ individuals-api-overview.json
│        ├─ payment-system-overview.json
│        └─ keycloak-status.json
│
├─ observability/
├─ docker-compose.yml
└─ Makefile
```

---

## Быстрый старт (Makefile)

Требуется: **Docker + Docker Compose**, `make`, (для тестов) **JDK**.

```bash
# в корне репозитория
make infra     # поднимает инфраструктуру: keycloak-postgres keycloak loki prometheus grafana promtail
make start     # поднимает все сервисы (включая individuals-api) + ожидание готовности
make health    # проверка health всех компонентов
make loki-test # smoke для логов (пишем запрос -> проверяем что Loki видит сервис)
make test      # тесты individuals-api (gradlew лежит внутри individuals-api)
```

---

## Ручной запуск (без Makefile)

```bash
# инфраструктура + приложение
docker compose up -d

# проверка статуса контейнеров
docker compose ps
```

---

## Порты и сервисы

| Сервис            | Порт (host) | Назначение |
|------------------|-------------|------------|
| Grafana          | 3000        | Дашборды и Explore (логин: admin/admin) |
| Loki             | 3100        | Хранилище логов |
| Prometheus       | 9090        | Метрики |
| Keycloak         | 8080        | UI/Realm |
| Keycloak metrics | 9000        | health/metrics (в зависимости от конфигурации Keycloak) |
| Individuals API  | 8081        | WebFlux API + Actuator |
| Postgres (KC)    | 5433        | БД Keycloak |

---

## Наблюдаемость

### Grafana
- URL: `http://localhost:3000` (обычно `admin/admin`)
- Дашборды лежат в: `infrastructure/grafana/dashboards/`

Рекомендуемые дашборды:
- `payment-system-overview.json`
- `individuals-api-overview.json`
- `keycloak-status.json`

### Prometheus
- URL: `http://localhost:9090`
- Targets:
  - `individuals-api`
  - `keycloak`

### Loki
- URL: `http://localhost:3100`
- Логи приходят через `promtail`
- Типичный запрос (пример): `{job="docker",service="individuals-api"}`

---

## API и OpenAPI

- OpenAPI-спека: `individuals-api/openapi/individuals-api.yaml`
- Postman коллекция: `individuals-api/postman/Individuals API.postman_collection.json`

Пример smoke-запроса:
```bash
curl -sS -X POST http://localhost:8081/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"nope@example.com","password":"bad"}'
```

---

## Тестирование

`gradlew` находится в модуле `individuals-api`, поэтому запускать так:

```bash
cd individuals-api
./gradlew test
```

---

## Типовые ошибки и устранение

### 1) `zsh: no such file or directory: ./gradlew`
`gradlew` лежит **не в корне**, а в `individuals-api/`:
```bash
cd individuals-api && ./gradlew test
```

### 2) `Makefile: *** missing separator`
Команды в Makefile должны начинаться **TAB**, а не пробелами.

### 3) Prometheus targets `down` из-за DNS/hostname
В конфиге Prometheus нужно использовать **имена сервисов docker compose** (например, `individuals-api`, `keycloak`) внутри сети compose.
