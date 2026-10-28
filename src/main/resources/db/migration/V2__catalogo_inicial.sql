-- =====================================================================
-- V2: Catálogo inicial de productos (datos de prueba del MVP)
-- =====================================================================
INSERT INTO productos (sku, nombre, descripcion, categoria, precio, existencias) VALUES
('PAP-001', 'Caja de hojas carta (5,000 hojas)',  'Papel bond blanco 75 g, caja con 10 paquetes de 500 hojas', 'Papelería',   899.00, 500),
('PAP-002', 'Bolígrafo azul (caja 50 pzs)',       'Bolígrafo punto mediano 1.0 mm, tinta azul',                  'Papelería',   245.50, 800),
('PAP-003', 'Folder tamaño carta (paquete 100)',  'Folder de cartulina color crema',                             'Papelería',   189.90, 600),
('LIM-001', 'Gel antibacterial 4 L',              'Gel antibacterial con 70% de alcohol, galón',                 'Limpieza',    320.00, 300),
('LIM-002', 'Toalla interdoblada (caja 20 pqts)', 'Toalla de papel interdoblada para despachador',               'Limpieza',    540.00, 250),
('LIM-003', 'Detergente multiusos 20 L',          'Detergente líquido biodegradable, porrón de 20 litros',       'Limpieza',    780.00, 150),
('CAF-001', 'Café molido 1 kg',                   'Café de altura 100% arábica, molido para cafetera',           'Cafetería',   289.00, 400),
('CAF-002', 'Vaso térmico 12 oz (paquete 50)',    'Vaso desechable de unicel 12 oz',                             'Cafetería',   135.00, 700),
('TEC-001', 'Tóner láser negro compatible',       'Tóner compatible, rendimiento aproximado de 3,000 páginas',   'Tecnología', 1150.00, 120),
('TEC-002', 'Mouse inalámbrico',                  'Mouse óptico inalámbrico 2.4 GHz con receptor USB',           'Tecnología',  210.00, 350),
('TEC-003', 'Teclado USB en español',             'Teclado alámbrico USB distribución latinoamericana',          'Tecnología',  275.00, 300),
('MOB-001', 'Silla ergonómica de oficina',        'Silla con soporte lumbar, malla transpirable y brazos fijos', 'Mobiliario', 2899.00,  40);