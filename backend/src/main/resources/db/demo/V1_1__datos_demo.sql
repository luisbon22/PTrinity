-- =====================================================================
-- PTrinity - DML de demostracion (solo ambientes dev, qa y local)
-- Saldos coherentes con los movimientos registrados.
-- =====================================================================

INSERT INTO clientes (tipo_identificacion, numero_identificacion, nombres, apellido, correo, fecha_nacimiento,
                      fecha_creacion, fecha_modificacion)
VALUES ('CC', '1020304050', 'Ana Maria', 'Gomez', 'ana.gomez@correo.com', DATE '1990-05-14',
        TIMESTAMP '2026-01-05 09:00:00', TIMESTAMP '2026-01-05 09:00:00'),
       ('CE', '987654', 'Carlos', 'Perez', 'carlos.perez@correo.com', DATE '1985-11-02',
        TIMESTAMP '2026-01-06 10:30:00', TIMESTAMP '2026-01-06 10:30:00'),
       ('CC', '1098765432', 'Laura', 'Rodriguez', 'laura.rodriguez@correo.com', DATE '1998-02-20',
        TIMESTAMP '2026-01-07 14:15:00', TIMESTAMP '2026-01-07 14:15:00');

INSERT INTO cuentas (tipo_cuenta, numero_cuenta, estado, saldo, exenta_gmf, fecha_creacion, fecha_modificacion,
                     cliente_id)
VALUES ('AHORROS', '5310000001', 'ACTIVA', 1500000.00, TRUE,
        TIMESTAMP '2026-01-05 09:10:00', TIMESTAMP '2026-01-10 11:00:00',
        (SELECT id FROM clientes WHERE numero_identificacion = '1020304050')),
       ('CORRIENTE', '3310000001', 'ACTIVA', 800000.00, FALSE,
        TIMESTAMP '2026-01-06 10:40:00', TIMESTAMP '2026-01-10 11:00:00',
        (SELECT id FROM clientes WHERE numero_identificacion = '987654')),
       ('AHORROS', '5310000002', 'ACTIVA', 799200.00, FALSE,
        TIMESTAMP '2026-01-07 14:20:00', TIMESTAMP '2026-01-12 16:00:00',
        (SELECT id FROM clientes WHERE numero_identificacion = '1098765432'));

INSERT INTO movimientos (cuenta_id, tipo, naturaleza, monto, gmf, saldo_resultante, referencia, cuenta_contraparte,
                         descripcion, fecha)
VALUES ((SELECT id FROM cuentas WHERE numero_cuenta = '5310000001'), 'CONSIGNACION', 'CREDITO', 2000000.00, 0, 2000000.00,
        'b2f3c9a0-0001-4c1e-9a10-000000000001', NULL, 'Consignacion inicial', TIMESTAMP '2026-01-05 09:15:00'),
       ((SELECT id FROM cuentas WHERE numero_cuenta = '3310000001'), 'CONSIGNACION', 'CREDITO', 300000.00, 0, 300000.00,
        'b2f3c9a0-0002-4c1e-9a10-000000000002', NULL, 'Consignacion inicial', TIMESTAMP '2026-01-06 10:45:00'),
       ((SELECT id FROM cuentas WHERE numero_cuenta = '5310000001'), 'TRANSFERENCIA', 'DEBITO', 500000.00, 0, 1500000.00,
        'b2f3c9a0-0003-4c1e-9a10-000000000003', '3310000001', 'Pago arriendo', TIMESTAMP '2026-01-10 11:00:00'),
       ((SELECT id FROM cuentas WHERE numero_cuenta = '3310000001'), 'TRANSFERENCIA', 'CREDITO', 500000.00, 0, 800000.00,
        'b2f3c9a0-0003-4c1e-9a10-000000000003', '5310000001', 'Pago arriendo', TIMESTAMP '2026-01-10 11:00:00'),
       ((SELECT id FROM cuentas WHERE numero_cuenta = '5310000002'), 'CONSIGNACION', 'CREDITO', 1000000.00, 0, 1000000.00,
        'b2f3c9a0-0004-4c1e-9a10-000000000004', NULL, 'Consignacion inicial', TIMESTAMP '2026-01-07 14:30:00'),
       ((SELECT id FROM cuentas WHERE numero_cuenta = '5310000002'), 'RETIRO', 'DEBITO', 200000.00, 800.00, 799200.00,
        'b2f3c9a0-0005-4c1e-9a10-000000000005', NULL, 'Retiro en cajero', TIMESTAMP '2026-01-12 16:00:00');
