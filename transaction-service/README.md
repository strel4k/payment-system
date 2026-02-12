# Transaction Service

Микросервис для управления кошельками и транзакциями в Payment System.

## 🎯 Функциональность

- **Wallet Management** — создание и управление кошельками пользователей
- **Deposit** — пополнение кошелька (асинхронный двухфазный процесс через Kafka)
- **Withdrawal** — вывод средств (полу-синхронный с Kafka)
- **Transfer** — перевод между кошельками (синхронный)
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
| GET | `/api/v1/transactions/{uid}` | Get transaction status |
| GET | `/api/v1/transactions` | List transactions |

## 💰 Fee Structure

| Operation | Fee | Example |
|-----------|-----|---------|
| Deposit | 0% | 100 → 100 credited |
| Withdrawal | 1% | 100 → 99 received |
| Transfer | 0.5% | 100 → 99.50 transferred |

## 🔄 Transaction Flows

### Deposit (Asynchronous)
```
1. init → returns requestUid + fee info (TTL 15 min)
2. confirm → creates PENDING transaction → sends Kafka event
3. Payment Gateway → sends deposit-completed event
4. Kafka Consumer → credits wallet → status COMPLETED
```

### Withdrawal (Semi-synchronous)
```
1. init → validates balance → returns fee info
2. confirm → debits balance immediately → PENDING → Kafka event
3. Payment Gateway → sends completion/failure event
4. Consumer → updates status (or refunds on failure)
```

### Transfer (Synchronous)
```
1. init → validates source balance → returns fee info
2. confirm → atomic: debit source + credit target → COMPLETED
```

## 🔧 Configuration
```yaml
app:
  transaction:
    deposit-fee-percent: 0.00
    withdrawal-fee-percent: 0.01
    transfer-fee-percent: 0.005
    init-request-ttl-minutes: 15
```

## 🚀 Running

```bash
# Start dependencies
docker-compose up -d zookeeper kafka transaction-postgres keycloak

# Run service
./gradlew :transaction-service:bootRun

# Or via Docker
docker-compose up -d transaction-service
```

## 🧪 Testing
```bash
./gradlew :transaction-service:test
# 35 tests: unit + integration
```

## 📊 Database Schema
```sql
wallet_types (uid, name, currency_code)
wallets (uid, user_uid, wallet_type_uid, name, status, balance)
transactions (uid, user_uid, wallet_uid, type, status, amount, fee, target_wallet_uid)
```
