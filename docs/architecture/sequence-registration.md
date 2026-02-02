# Sequence Diagram — User Registration Flow

Полный процесс регистрации пользователя в системе.

```mermaid
sequenceDiagram
    autonumber
    
    actor User
    participant API as Individuals API
    participant PS as Person Service
    participant DB as Person Database
    participant KC as Keycloak
    participant KDB as Keycloak Database
    participant Tempo
    
    Note over User,Tempo: Registration Flow with Distributed Tracing
    
    User->>+API: POST /v1/auth/registration<br/>{email, password, firstName, lastName}
    
    Note over API: Generate trace_id<br/>(OpenTelemetry)
    API->>Tempo: Start span: registration
    
    Note over API: Validate request:<br/>- email format<br/>- password strength<br/>- confirm_password match
    
    API->>+PS: POST /v1/persons<br/>{firstName, lastName, email}<br/>header: traceparent
    
    Note over PS: Start child span:<br/>create_person
    
    PS->>PS: Generate user_uid (UUID)
    
    PS->>+DB: BEGIN TRANSACTION
    
    PS->>DB: INSERT INTO users<br/>(id=user_uid, email,<br/>first_name, last_name)
    DB-->>PS: OK
    
    PS->>DB: INSERT INTO individuals<br/>(user_id=user_uid,<br/>status=ACTIVE)
    DB-->>PS: OK
    
    PS->>DB: COMMIT
    DB-->>-PS: Transaction committed
    
    Note over PS: Envers audit:<br/>user_aud, individuals_aud<br/>tables updated
    
    PS-->>-API: 201 Created<br/>{userId: user_uid,<br/>email, firstName, lastName}
    
    Note over API: Person created successfully<br/>with user_uid
    
    API->>+KC: POST /admin/realms/individuals/users<br/>{username=email,<br/>email, firstName, lastName,<br/>enabled=true,<br/>attributes: {user_uid}}
    
    KC->>+KDB: INSERT INTO user_entity<br/>(username, email, realm_id)
    KDB-->>-KC: OK
    
    KC->>KDB: INSERT INTO user_attribute<br/>(user_id, name="user_uid",<br/>value=user_uid)
    
    KC-->>-API: 201 Created<br/>Location: .../users/{keycloak_id}
    
    Note over API: Keycloak user created<br/>with user_uid attribute
    
    API->>+KC: POST /admin/realms/individuals/users/{keycloak_id}/reset-password<br/>{value=password, temporary=false}
    KC->>KDB: UPDATE credential<br/>SET password_hash
    KC-->>-API: 204 No Content
    
    Note over API: Password set successfully
    
    API->>+KC: POST /realms/individuals/protocol/openid-connect/token<br/>{grant_type=password,<br/>username=email, password}
    
    KC->>KDB: SELECT * FROM user_entity<br/>WHERE username=email
    KDB-->>KC: User found
    
    KC->>KC: Validate password hash
    
    KC->>KC: Generate JWT tokens:<br/>- access_token (5 min)<br/>- refresh_token (30 min)
    
    KC-->>-API: 200 OK<br/>{access_token,<br/>refresh_token,<br/>expires_in=300}
    
    API->>Tempo: End span: registration<br/>(duration, status=OK)
    
    API-->>-User: 200 OK<br/>{access_token,<br/>refresh_token,<br/>expires_in, token_type}
    
    Note over User: Registration complete!<br/>User receives JWT tokens
```

## Описание шагов

### 1-3. Инициация запроса
User отправляет POST запрос на `/v1/auth/registration` с данными:
- email
- password
- confirm_password
- first_name
- last_name

Individuals API создаёт **trace_id** через OpenTelemetry Java Agent.

### 4-5. Валидация
API валидирует:
- Email format (RFC 5322)
- Password strength (мин. 8 символов, заглавные, цифры, спец. символы)
- confirm_password совпадает с password

### 6-14. Создание Person в Person Service
**Транзакционная операция**:
1. Генерируется `user_uid` (UUID)
2. Создаётся запись в `users` таблице
3. Создаётся запись в `individuals` таблице (ссылка на user_id)
4. COMMIT транзакции
5. Hibernate Envers создаёт audit записи в `users_aud` и `individuals_aud`

**Trace**: создаётся child span `create_person` с `traceparent` header

### 15-18. Регистрация в Keycloak
API вызывает Keycloak Admin API:
- Создаёт пользователя с `username=email`
- Добавляет **custom attribute** `user_uid` для связи с Person Service
- Keycloak возвращает Location header с ID созданного пользователя

### 19-21. Установка пароля
API устанавливает password через `/reset-password` endpoint:
- `temporary=false` (пароль постоянный)
- Keycloak хеширует пароль (bcrypt) и сохраняет в БД

### 22-28. Генерация JWT токенов
API вызывает Token endpoint Keycloak:
- `grant_type=password` (Resource Owner Password Credentials)
- Keycloak валидирует credentials
- Генерирует **access_token** (JWT, exp=5 min)
- Генерирует **refresh_token** (JWT, exp=30 min)

### 29-30. Завершение
- API завершает span в Tempo
- Возвращает tokens пользователю
- **Полная трасса доступна в Grafana Tempo**

## Error Handling

### Person Service недоступен (шаг 6)
→ API возвращает `503 Service Unavailable`  
→ Rollback не нужен (транзакция не начата)

### Ошибка создания Person (шаг 13)
→ Person Service делает ROLLBACK  
→ API возвращает `500 Internal Server Error`  
→ Keycloak user **не создаётся**

### Keycloak недоступен (шаг 15)
→ Person уже создан в БД  
→ API возвращает `503 Service Unavailable`  
→ **Inconsistency**: person есть, Keycloak user нет  
→ Требуется компенсирующая транзакция или retry logic

### Пароль не установлен (шаг 19)
→ Keycloak user создан, но без password  
→ API возвращает `500 Internal Server Error`  
→ User не может залогиниться (нужен manual reset)

### Token generation failed (шаг 22)
→ Всё создано, но токены не выданы  
→ API возвращает `500 Internal Server Error`  
→ User может залогиниться через `/v1/auth/login`

## Tracing Information

Все компоненты экспортируют spans в **Tempo** через OpenTelemetry:

| Service | Span Name | Attributes |
|---------|-----------|------------|
| individuals-api | `POST /v1/auth/registration` | http.method, http.url, http.status_code |
| person-service | `POST /v1/persons` | http.method, http.url, user_uid |
| individuals-api | `POST /admin/.../users` | http.method, keycloak.realm |
| individuals-api | `POST /.../token` | grant_type, username |

**Trace ID** доступен в логах:
```json
{"trace_id":"fb47b1deb3b6e4134167048b1ad49eda", "message":"Registration completed"}
```

Полная трасса видна в **Grafana → Explore → Tempo**.