-- =====================================================================
-- PTrinity - DML
-- Datos de demostración. Solo se cargan en los perfiles de ejecución
-- (no en las pruebas automatizadas).
-- =====================================================================

INSERT INTO customers (identification_type, identification_number, first_name, last_name, email, birth_date, created_at)
VALUES ('CC', '1020304050', 'Valentina', 'Restrepo', 'valentina.restrepo@correo.com', DATE '1992-03-14', TIMESTAMP '2026-10-01 09:00:00'),
       ('CC', '79865432', 'Andrés', 'Gómez', 'andres.gomez@correo.com', DATE '1985-11-02', TIMESTAMP '2026-10-02 10:30:00'),
       ('CE', '456789', 'Camila', 'Rojas', 'camila.rojas@correo.com', DATE '1998-07-21', TIMESTAMP '2026-10-03 14:15:00');

INSERT INTO accounts (account_type, account_number, status, balance, overdraft_limit, gmf_exempt, customer_id, created_at)
VALUES ('SAVINGS', '5300000001', 'ACTIVE', 1500000.00, 0, TRUE,
        (SELECT id FROM customers WHERE identification_number = '1020304050'), TIMESTAMP '2026-10-01 09:05:00'),
       ('CHECKING', '3300000001', 'ACTIVE', 2500000.00, 1000000.00, FALSE,
        (SELECT id FROM customers WHERE identification_number = '79865432'), TIMESTAMP '2026-10-02 10:40:00'),
       ('SAVINGS', '5300000002', 'ACTIVE', 800000.00, 0, FALSE,
        (SELECT id FROM customers WHERE identification_number = '79865432'), TIMESTAMP '2026-10-02 10:45:00');

INSERT INTO transactions (reference, transaction_type, movement_type, account_id, amount, gmf_amount, balance_after,
                          counterpart_account_number, description, created_at)
VALUES ('seed-0001', 'DEPOSIT', 'CREDIT', (SELECT id FROM accounts WHERE account_number = '5300000001'),
        1500000.00, 0, 1500000.00, NULL, 'Consignación inicial', TIMESTAMP '2026-10-01 09:10:00'),
       ('seed-0002', 'DEPOSIT', 'CREDIT', (SELECT id FROM accounts WHERE account_number = '3300000001'),
        2500000.00, 0, 2500000.00, NULL, 'Consignación inicial', TIMESTAMP '2026-10-02 11:00:00'),
       ('seed-0003', 'DEPOSIT', 'CREDIT', (SELECT id FROM accounts WHERE account_number = '5300000002'),
        800000.00, 0, 800000.00, NULL, 'Consignación inicial', TIMESTAMP '2026-10-02 11:05:00');

-- Se ajustan los consecutivos para no repetir los números de cuenta cargados
ALTER SEQUENCE savings_account_number_seq RESTART WITH 3;
ALTER SEQUENCE checking_account_number_seq RESTART WITH 2;
