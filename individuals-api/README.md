# Individuals API

Микросервис авторизации/аутентификации для платёжной системы.  
Отвечает за регистрацию пользователя, выдачу access/refresh токенов и получение профиля текущего пользователя по JWT.

---

## Содержание
- [Технологический стек](#Технологический-стек)
- [Запуск](#запуск)
- [Архитектура и эндпоинты](#Архитектура-и-эндпоинты)
- [Запуск через Docker Compose](#Запуск-через-Docker-Compose)
- [Postman](#postman)
- [Метрика](#Метрика)
- [Логи](#логи)
- [Grafana](#grafana)
- [Тесты](#тесты)
- [OpenAPI и генерация DTO](#OpenAPI-и-генерация-DTO)

## Технологический стек

- Java 17
- Spring Boot 3.5, WebFlux
- Spring Security, OAuth2 Resource Server (JWT)
- Keycloak 26 (OIDC provider)
- OpenAPI 3 + `org.openapi.generator` (генерация DTO)
- Docker, Docker Compose
- Prometheus (метрики)
- Loki (+ агент сбора логов, настроенный через Docker)
- Grafana (дашборды по метрикам и логам)
- JUnit 5, Mockito, Reactor Test

---

## Запуск
Собрать только individuals-api:
```text
cd individuals-api
./gradlew clean build
```
Запустить всю инфраструктуру (Keycloak, БД, метрики, логи, Grafana):
```text
cd ..
docker compose up -d
```

## Архитектура и эндпоинты

Сервис поднимается на порту **8081** и предоставляет API:

### `POST /v1/auth/registration`

Регистрация нового пользователя в Keycloak и одновременный логин.

- **Request body:** `UserRegistrationRequest`
  - `email`
  - `password`
  - `confirmPassword`
- **Response:** `TokenResponse`
  - `access_token`
  - `refresh_token`
  - `expires_in`
  - `token_type`

### `POST /v1/auth/login`

Логин существующего пользователя через Keycloak.

- **Request body:** `UserLoginRequest`
  - `email`
  - `password`
- **Response:** `TokenResponse`

### `POST /v1/auth/refresh-token`

Обновление access-токена по refresh-токену.

- **Request body:** `TokenRefreshRequest`
  - `refresh_token`
- **Response:** `TokenResponse`

### `GET /v1/auth/me`

Возвращает информацию о текущем пользователе.  
JWT валидируется Spring Security (Resource Server), далее клеймы читаются из `Jwt`.

- **Headers:** `Authorization: Bearer <access_token>`
- **Response:** `UserInfoResponse`
  - `id`
  - `email`
  - `roles` (список ролей из Keycloak)
  - `created_at` (OffsetDateTime)

---

## Запуск через Docker Compose
```text
cd payment-system
docker compose build individuals-api
docker compose up -d
```
- **Поднимаются контейнеры:**
  - `individuals-keycloak – Keycloak (порт 8080)`
  - `keycloak-postgres – Postgres 17 для Keycloak (порт 5433 наружу)`
  - `individuals-api – микросервис (порт 8081)`
  - `prometheus – Prometheus (порт 9090)`
  - `loki – Loki (порт 3100)`
  - `grafana – Grafana (порт 3000)`
  - `(агент логов, если определён в docker-compose.yml)`
  ```text
  docker ps
  ```

## Postman
Импортировать файл коллекции:
```text
postman/individuals-api.postman_collection.json
```
- Переменные коллекции:
  - baseUrl – по умолчанию http://localhost:8081
  - email – test3@example.com
  - password – Qwe12345!
  - accessToken – заполняется вручную после логина (или через скрипты)
  - refreshToken – аналогично

#### 1. Запрос Auth-Registration:
- Запрос Auth-Registration:
```text
POST {{baseUrl}}/v1/auth/registration
{
    "email": "{{email}}",
    "password": "{{password}}",
    "confirmPassword": "{{password}}"
}
```
#### 2. Логин:
- Запрос Auth-Login:
```text
POST {{baseUrl}}/v1/auth/login
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```
#### 3. Refresh
- Запрос Auth-Refresh Token:
```text
  POST {{baseUrl}}/v1/auth/refresh-token
  {
    "refresh_token": "{{refreshToken}}"
  }
```
#### 4. Текущий пользователь
- Запрос Auth-Me:
```text
GET {{baseUrl}}/v1/auth/me
Authorization: Bearer {{accessToken}}
```

## Метрика
- Spring Boot экспонирует метрики в формате Prometheus:
```text
  http://localhost:8081/actuator/prometheus
```
## Логи
- Логи individuals-api пишутся в JSON через Logback encoder и забираются Promtail из Docker:
  - конфиг promtail: promtail-config.yml
  - логи отправляются в Loki по адресу http://loki:3100
  
В Grafana (datasource Loki) достаточно выполнить запрос:
```text
  {job="individuals-api"}
```
## Grafana
- URL: http://localhost:3000
- Логин/пароль: admin / admin

Датасорсы и базовые дашборды провиженятся из:
```text
grafana/provisioning/datasources
grafana/provisioning/dashboards
grafana/dashboards/*.json
```
На дашбордах можно посмотреть:
- графики метрик из Prometheus (http_server_requests_* для individuals-api);
- логи из Loki по job individuals-api.

## Тесты
- Локально из дериктории individuals-api:
```text
  ./gradlew clean test
  ./gradlew clean build
```
Тесты покрывают:
- маппинг ответов Keycloak в TokenResponse (TokenResponseTest);
- регистрацию и маппинг JWT в UserInfoResponse (UserServiceTest);
- работу REST-контроллера (AuthControllerTest);
- интеграционный сценарий регистрации/логин/refresh/me через WebTestClient (AuthFlowIntegrationTest).

## OpenAPI и генерация DTO

- Спецификация API:

```text
individuals-api/openapi/individuals-api.yaml
```
