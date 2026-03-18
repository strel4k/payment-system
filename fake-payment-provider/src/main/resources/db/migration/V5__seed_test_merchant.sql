-- Тестовый мерчант для локальной разработки и интеграционных тестов
-- merchantId: merchant-1
-- secretKey:  secret123  (BCrypt $2a$10$)
INSERT INTO merchants (merchant_id, secret_key, name)
VALUES ('merchant-1',
        '$2a$10$lGJ8m07.v8HIn3bZaViGg.j30R21EojrlF2KU2XPpY4PTbwX/P7Yy',
        'Test Merchant');