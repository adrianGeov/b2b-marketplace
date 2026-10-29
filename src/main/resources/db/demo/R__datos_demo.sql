-- =====================================================================
-- Datos de DEMOSTRACIÓN para las métricas de negocio (Paso 11).
-- Migración repetible (R__): Flyway la vuelve a ejecutar solo si el archivo cambia.
-- Es idempotente (ON CONFLICT / NOT EXISTS): ejecutarla varias veces no duplica datos.
-- Solo se carga cuando FLYWAY_LOCATIONS incluye classpath:db/demo (desarrollo / demo).
-- Contraseña de todos los clientes demo: Demo1234
-- =====================================================================

-- ---------------------------------------------------------------------
-- Clientes demo (6 leads: 4 compran, 1 solo tiene un pago rechazado, 1 nunca compra)
-- ---------------------------------------------------------------------
INSERT INTO usuarios (correo, contrasena_hash, nombre_completo, razon_social, rfc, rol, fecha_creacion, fecha_actualizacion)
VALUES
 ('norte@demo.mx',    '$2a$10$mWOwJ39VAX/ujGBd67izme5LXsMGaVoEyQjB2kDihNy6D2IuVcEfK', 'Carlos Treviño',  'Distribuidora del Norte SA de CV', 'DNO010101AB1', 'DISTRIBUTOR', '2026-08-20 16:00+00', '2026-08-20 16:00+00'),
 ('centro@demo.mx',   '$2a$10$mWOwJ39VAX/ujGBd67izme5LXsMGaVoEyQjB2kDihNy6D2IuVcEfK', 'Lucía Hernández', 'Papelería del Centro SA',         'PCE020202AB2', 'CUSTOMER',    '2026-08-25 16:00+00', '2026-08-25 16:00+00'),
 ('bajio@demo.mx',    '$2a$10$mWOwJ39VAX/ujGBd67izme5LXsMGaVoEyQjB2kDihNy6D2IuVcEfK', 'Jorge Ramírez',   'Oficinas del Bajío SC',           'OBA030303AB3', 'CUSTOMER',    '2026-08-28 16:00+00', '2026-08-28 16:00+00'),
 ('sur@demo.mx',      '$2a$10$mWOwJ39VAX/ujGBd67izme5LXsMGaVoEyQjB2kDihNy6D2IuVcEfK', 'Mariana López',   'Corporativo del Sur SA de CV',    'CSU040404AB4', 'DISTRIBUTOR', '2026-09-01 16:00+00', '2026-09-01 16:00+00'),
 ('cafe@demo.mx',     '$2a$10$mWOwJ39VAX/ujGBd67izme5LXsMGaVoEyQjB2kDihNy6D2IuVcEfK', 'Pedro Sánchez',   'Cafetería Altura SA',             'CAL050505AB5', 'CUSTOMER',    '2026-09-15 16:00+00', '2026-09-15 16:00+00'),
 ('limpieza@demo.mx', '$2a$10$mWOwJ39VAX/ujGBd67izme5LXsMGaVoEyQjB2kDihNy6D2IuVcEfK', 'Sofía Castro',    'Limpieza Express SA',             'LEX060606AB6', 'CUSTOMER',    '2026-10-01 16:00+00', '2026-10-01 16:00+00')
ON CONFLICT (correo) DO NOTHING;

-- ---------------------------------------------------------------------
-- Pedidos históricos (el total se calcula al final a partir del detalle)
-- Entrega estimada = creación + 3 días. A tiempo: entregado al día 2. Tarde: al día 5.
-- ---------------------------------------------------------------------
INSERT INTO pedidos (folio, usuario_id, estatus, total, llave_idempotencia, id_pedido_erp,
                     fecha_entrega_estimada, fecha_entrega, fecha_creacion, fecha_actualizacion)
SELECT v.folio, u.id, v.estatus, 0, 'demo-' || v.folio,
       CASE WHEN v.estatus IN ('CONFIRMED', 'SHIPPED', 'DELIVERED') THEN 'ERP-' || v.folio END,
       (v.creado::date + 3),
       CASE v.entrega WHEN 'A_TIEMPO' THEN v.creado + INTERVAL '2 days'
                      WHEN 'TARDE'    THEN v.creado + INTERVAL '5 days' END,
       v.creado, v.creado
FROM (VALUES
 ('DEMO-20260902-001', 'norte@demo.mx',    'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-09-02 17:00+00'),
 ('DEMO-20260905-002', 'centro@demo.mx',   'DELIVERED',      'TARDE',    TIMESTAMPTZ '2026-09-05 17:00+00'),
 ('DEMO-20260909-003', 'bajio@demo.mx',    'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-09-09 17:00+00'),
 ('DEMO-20260912-004', 'sur@demo.mx',      'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-09-12 17:00+00'),
 ('DEMO-20260918-005', 'norte@demo.mx',    'DELIVERED',      'TARDE',    TIMESTAMPTZ '2026-09-18 17:00+00'),
 ('DEMO-20260922-006', 'centro@demo.mx',   'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-09-22 17:00+00'),
 ('DEMO-20260926-007', 'bajio@demo.mx',    'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-09-26 17:00+00'),
 ('DEMO-20261001-008', 'sur@demo.mx',      'DELIVERED',      'TARDE',    TIMESTAMPTZ '2026-10-01 17:00+00'),
 ('DEMO-20261006-009', 'norte@demo.mx',    'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-10-06 17:00+00'),
 ('DEMO-20261010-010', 'centro@demo.mx',   'DELIVERED',      'A_TIEMPO', TIMESTAMPTZ '2026-10-10 17:00+00'),
 ('DEMO-20261014-011', 'bajio@demo.mx',    'SHIPPED',        NULL,       TIMESTAMPTZ '2026-10-14 17:00+00'),
 ('DEMO-20261018-012', 'sur@demo.mx',      'CONFIRMED',      NULL,       TIMESTAMPTZ '2026-10-18 17:00+00'),
 ('DEMO-20261020-013', 'norte@demo.mx',    'CONFIRMED',      NULL,       TIMESTAMPTZ '2026-10-20 17:00+00'),
 ('DEMO-20261021-014', 'limpieza@demo.mx', 'PAYMENT_FAILED', NULL,       TIMESTAMPTZ '2026-10-21 17:00+00'),
 ('DEMO-20261022-015', 'centro@demo.mx',   'CANCELLED',      NULL,       TIMESTAMPTZ '2026-10-22 17:00+00')
) AS v(folio, correo, estatus, entrega, creado)
JOIN usuarios u ON u.correo = v.correo
ON CONFLICT (folio) DO NOTHING;

-- ---------------------------------------------------------------------
-- Detalle de los pedidos (precio y nombre tomados del catálogo)
-- ---------------------------------------------------------------------
INSERT INTO pedido_detalles (pedido_id, producto_id, sku, nombre_producto, precio_unitario, cantidad, subtotal)
SELECT p.id, pr.id, pr.sku, pr.nombre, pr.precio, v.cantidad, pr.precio * v.cantidad
FROM (VALUES
 ('DEMO-20260902-001', 'PAP-001', 10), ('DEMO-20260902-001', 'PAP-002', 5),
 ('DEMO-20260905-002', 'PAP-001', 4),  ('DEMO-20260905-002', 'PAP-003', 6),
 ('DEMO-20260909-003', 'CAF-001', 8),  ('DEMO-20260909-003', 'CAF-002', 10),
 ('DEMO-20260912-004', 'LIM-001', 6),  ('DEMO-20260912-004', 'LIM-002', 4),
 ('DEMO-20260918-005', 'TEC-001', 3),  ('DEMO-20260918-005', 'PAP-001', 5),
 ('DEMO-20260922-006', 'PAP-001', 3),  ('DEMO-20260922-006', 'PAP-002', 4),
 ('DEMO-20260926-007', 'CAF-001', 6),  ('DEMO-20260926-007', 'CAF-002', 8),
 ('DEMO-20261001-008', 'LIM-003', 3),  ('DEMO-20261001-008', 'LIM-001', 4),
 ('DEMO-20261006-009', 'PAP-001', 12), ('DEMO-20261006-009', 'PAP-002', 6), ('DEMO-20261006-009', 'PAP-003', 4),
 ('DEMO-20261010-010', 'TEC-002', 5),  ('DEMO-20261010-010', 'TEC-003', 5),
 ('DEMO-20261014-011', 'CAF-001', 10), ('DEMO-20261014-011', 'CAF-002', 12),
 ('DEMO-20261018-012', 'MOB-001', 2),  ('DEMO-20261018-012', 'TEC-002', 2),
 ('DEMO-20261020-013', 'PAP-001', 8),  ('DEMO-20261020-013', 'TEC-001', 2),
 ('DEMO-20261021-014', 'LIM-002', 5),
 ('DEMO-20261022-015', 'PAP-003', 10)
) AS v(folio, sku, cantidad)
JOIN pedidos p   ON p.folio = v.folio
JOIN productos pr ON pr.sku = v.sku
WHERE NOT EXISTS (
    SELECT 1 FROM pedido_detalles d WHERE d.pedido_id = p.id AND d.producto_id = pr.id
);

-- Total de cada pedido demo = suma de su detalle
UPDATE pedidos p
SET total = t.total
FROM (SELECT pedido_id, SUM(subtotal) AS total FROM pedido_detalles GROUP BY pedido_id) t
WHERE t.pedido_id = p.id
  AND p.folio LIKE 'DEMO-%';

-- ---------------------------------------------------------------------
-- Pagos: aprobados para los pedidos pagados; rechazado para el PAYMENT_FAILED
-- ---------------------------------------------------------------------
INSERT INTO pagos (pedido_id, monto, estatus, referencia_pasarela, motivo_rechazo, intentos, fecha_creacion)
SELECT p.id, p.total,
       CASE WHEN p.estatus = 'PAYMENT_FAILED' THEN 'DECLINED' ELSE 'APPROVED' END,
       CASE WHEN p.estatus = 'PAYMENT_FAILED' THEN NULL ELSE 'PAY-' || p.folio END,
       CASE WHEN p.estatus = 'PAYMENT_FAILED' THEN 'Fondos insuficientes' END,
       1,
       p.fecha_creacion + INTERVAL '5 minutes'
FROM pedidos p
WHERE p.folio LIKE 'DEMO-%'
  AND p.estatus IN ('PAID', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'PAYMENT_FAILED')
  AND NOT EXISTS (SELECT 1 FROM pagos pg WHERE pg.pedido_id = p.id);