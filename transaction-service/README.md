# Transaction Service

Микросервис для управления кошельками и транзакциями в Payment System.

## 🎯 Функциональность

- **Wallet Management** — создание и управление кошельками пользователей
- **Deposit** — пополнение кошелька (асинхронный двухфазный процесс через Kafka), fee: 0%
- **Withdrawal** — вывод средств (полу-синхронный с Kafka + compensating transaction), fee: 1%
- **Transfer** — синхронный атомарный перевод между кошельками, fee: 0.5%
- **Fee Calculation** — автоматический расчёт комиссий

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                   Transaction Service                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Wallet API  │  │Transaction  │  │   Kafka Consumer    │  │
│  │ Controller  │  │ Controller  │  │  (Event Processing) │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
│         │                │                    │             │
│         ▼                ▼                    ▼             │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Service Layer                              ││
│  │  WalletService │ TransactionService │ FeeCalculator     ││
│  └─────────────────────────────────────────────────────────┘│
│         │                │                     │            │
│         ▼                ▼                     ▼            │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐│
│  │ PostgreSQL   │  │    Kafka     │  │   InitRequestCache  ││
│  │ (Wallets,    │  │  (Events)    │  │   (Two-phase TTL)   ││
│  │ Transactions)│  │              │  │                     ││
│  └──────────────┘  └──────────────┘  └─────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## 📡 API Endpoints

### Wallets

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/wallets` | Create new wallet |
| GET | `/api/v1/wallets/{uid}` | Get wallet by UID |
| GET | `/api/v1/wallets` | List user's wallets |

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transactions/deposit/init` | Initialize deposit |
| POST | `/api/v1/transactions/deposit/confirm` | Confirm deposit |
| POST | `/api/v1/transactions/withdrawal/init` | Initialize withdrawal |
| POST | `/api/v1/transactions/withdrawal/confirm` | Confirm withdrawal |
| POST | `/api/v1/transactions/transfer/init` | Initialize transfer |
| POST | `/api/v1/transactions/transfer/confirm` | Confirm transfer |
| GET | `/api/v1/transactions/{uid}/status` | Get transaction status |
| GET | `/api/v1/transactions` | List transactions (paginated) |

## 💰 Fee Structure

| Operation | Fee | Example |
|-----------|-----|---------|
| Deposit | 0% | 100.00 → 100.00 credited |
| Withdrawal | 1% | 100.00 → 99.00 received, 1.00 fee |
| Transfer | 0.5% | 100.00 → 99.50 received, 0.50 fee |

## 🔄 Transaction Flows

### Deposit (Asynchronous)
```
1. init → validates wallet, generates requestUid (TTL 15 min), fee=0
2. confirm → creates PENDING transaction → publishes deposit-requested to Kafka
3. [Payment Gateway] → processes payment → publishes deposit-completed
4. Kafka Consumer → credits wallet balance → status COMPLETED
```

### Withdrawal (Semi-synchronous + Compensating Transaction)
```
1. init → validates balance, generates requestUid, fee=1%
2. confirm → pessimistic lock → deducts balance → creates PENDING → publishes withdrawal-requested
3. [Payment Gateway] processes:
   - success → publishes withdrawal-completed → status COMPLETED
   - failure → publishes withdrawal-failed → REFUND balance → status FAILED
```

### Transfer (Synchronous Atomic)
```
1. init → validates both wallets + source balance, fee=0.5%
2. confirm → pessimistic lock on both wallets → atomic debit + credit → status COMPLETED
(no Kafka, no async step)
```

## 📊 Kafka Topics

| Topic | Role | Description |
|-------|------|-------------|
| `deposit-requested` | Publisher | Initiate deposit with payment gateway |
| `deposit-completed` | Consumer | Credit wallet after successful payment |
| `withdrawal-requested` | Publisher | Initiate withdrawal with payment gateway |
| `withdrawal-completed` | Consumer | Mark withdrawal as completed |
| `withdrawal-failed` | Consumer | Refund balance, mark as failed |

## 🗄️ Database Schema

### wallet_types
Справочник типов кошельков (USD Wallet, EUR Wallet, RUB Wallet).

### wallets
| Column | Type | Description |
|--------|------|-------------|
| uid | UUID | Primary key |
| user_uid | UUID | Owner (from Keycloak JWT) |
| wallet_type_uid | UUID | FK to wallet_types |
| name | VARCHAR(32) | Wallet name |
| status | VARCHAR(30) | ACTIVE / BLOCKED / CLOSED |
| balance | DECIMAL(19,4) | Current balance (≥ 0) |

### transactions
| Column | Type | Description |
|--------|------|-------------|
| uid | UUID | Primary key |
| wallet_uid | UUID | Source wallet |
| target_wallet_uid | UUID | Target wallet (transfer only) |
| type | VARCHAR(20) | DEPOSIT / WITHDRAWAL / TRANSFER |
| status | VARCHAR(32) | PENDING / COMPLETED / FAILED |
| amount | DECIMAL(19,4) | Transaction amount |
| fee | DECIMAL(19,4) | Calculated fee |
| failure_reason | VARCHAR(256) | Error description if FAILED |

## 🔧 Конфигурация

### Профиль docker
```yaml
spring:
  datasource:
    url: jdbc:postgresql://transaction-postgres:5432/transaction
  kafka:
    bootstrap-servers: kafka:29092
```

### Профиль sharding (optional)
```bash
SPRING_PROFILES_ACTIVE=sharding
```
Активирует Apache ShardingSphere JDBC с шардированием по `user_uid`.

## 📦 API Client

Артефакт `transaction-service-api-client` публикуется в Nexus:

```bash
./gradlew :transaction-service:transaction-service-api-client:publishToMavenLocal
# или
./gradlew :transaction-service:transaction-service-api-client:publish
```

Содержит auto-generated DTOs из `openapi/transaction-service.yaml`:
- `CreateWalletRequest`, `WalletResponse`
- `TransactionInitRequest`, `TransactionInitResponse`
- `TransactionConfirmRequest`, `TransactionConfirmResponse`
- `TransactionStatusResponse`, `TransactionPageResponse`