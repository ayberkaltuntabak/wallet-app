INSERT INTO customers (name, surname, tckn, password, role, created_at, updated_at)
VALUES
    ('Elif', 'Yildiz', '10000000001', '$2b$12$unNZ1kOHF3UHufSdF0PEA.tuIzFYN3rImq1joClGNf0GpbeSpXwpm', 'EMPLOYEE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Mert', 'Demir', '10000000012', '$2b$12$9f.OxNRlayR8UH.vRfW5lOGPv6JYs25nyYO7pDziVkHN5lCmkzmgC', 'CUSTOMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO wallets (customer_id, wallet_name, currency, active_for_shopping, active_for_withdraw, balance, usable_balance, created_at, updated_at, version)
VALUES
    ((SELECT id FROM customers WHERE tckn = '10000000012'), 'TRY Daily Wallet', 'TRY', TRUE, TRUE, 5000, 5000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ((SELECT id FROM customers WHERE tckn = '10000000012'), 'USD Savings', 'USD', TRUE, FALSE, 1200, 1200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
