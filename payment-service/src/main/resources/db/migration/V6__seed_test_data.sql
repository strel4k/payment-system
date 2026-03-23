-- ============================================================
-- Тестовый провайдер
-- ============================================================
INSERT INTO payment_providers (id, name, description)
VALUES (1, 'Fake Payment Provider', 'Тестовый провайдер для разработки и интеграционных тестов');

-- ============================================================
-- Методы оплаты
-- ============================================================
INSERT INTO payment_methods (id, provider_id, type, name, is_active,
                             provider_unique_id, provider_method_type, profile_type)
VALUES (1, 1, 'CARD', 'Bank Card (VISA/MC)', TRUE,
        'fpp-card-001', 'CARD', 'INDIVIDUAL'),
       (2, 1, 'BANK_TRANSFER', 'Bank Transfer', TRUE,
        'fpp-bank-001', 'BANK_TRANSFER', 'INDIVIDUAL');

-- ============================================================
-- Определения доступности: метод 1 (CARD) — все валюты, все страны
-- ============================================================
INSERT INTO payment_method_definitions (payment_method_id, is_all_currencies, is_all_countries,
                                        is_priority, is_active)
VALUES (1, TRUE, TRUE, TRUE, TRUE);

-- Определения доступности: метод 2 (BANK_TRANSFER) — только USD, только USA
INSERT INTO payment_method_definitions (payment_method_id, currency_code, country_alpha3_code,
                                        is_all_currencies, is_all_countries, is_priority, is_active)
VALUES (2, 'USD', 'USA', FALSE, FALSE, FALSE, TRUE);

-- ============================================================
-- Required fields для метода CARD
-- ============================================================
INSERT INTO payment_method_required_fields (payment_method_id, payment_type, name, data_type,
                                            validation_type, validation_rule,
                                            description, placeholder, representation_name,
                                            language, is_active)
VALUES (1, 'DEPOSIT', 'card_number', 'STRING',
        'REGEXP', '^\d{16}$',
        'Номер карты', '0000 0000 0000 0000', 'Card Number',
        'en', TRUE),
       (1, 'DEPOSIT', 'card_holder', 'STRING',
        'REGEXP', '^[A-Z\s]{2,50}$',
        'Имя держателя карты', 'JOHN DOE', 'Card Holder',
        'en', TRUE),
       (1, 'DEPOSIT', 'expiry_date', 'STRING',
        'REGEXP', '^(0[1-9]|1[0-2])\/\d{2}$',
        'Срок действия карты', 'MM/YY', 'Expiry Date',
        'en', TRUE),
       (1, 'DEPOSIT', 'cvv', 'STRING',
        'REGEXP', '^\d{3,4}$',
        'CVV/CVC код', '***', 'CVV',
        'en', TRUE);

-- ============================================================
-- Required fields для метода BANK_TRANSFER
-- ============================================================
INSERT INTO payment_method_required_fields (payment_method_id, payment_type, name, data_type,
                                            validation_type, validation_rule,
                                            description, placeholder, representation_name,
                                            language, is_active)
VALUES (2, 'DEPOSIT', 'account_number', 'STRING',
        'REGEXP', '^\d{8,20}$',
        'Номер банковского счёта', '12345678', 'Account Number',
        'en', TRUE),
       (2, 'DEPOSIT', 'routing_number', 'STRING',
        'REGEXP', '^\d{9}$',
        'Routing number (ABA)', '021000021', 'Routing Number',
        'en', TRUE);