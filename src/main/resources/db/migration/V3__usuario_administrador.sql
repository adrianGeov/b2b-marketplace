-- =====================================================================
-- V3: Usuario administrador inicial
-- Correo: admin@marketplace.com  |  Contraseña: Admin123!
-- =====================================================================
INSERT INTO usuarios (correo, contrasena_hash, nombre_completo, razon_social, rfc, rol)
VALUES ('admin@marketplace.com',
        '$2a$10$mqyz2NW/2LJUnKLHBivcQu0Ll2oa/8PTALcjTiixbNh10LZlJdgA6',
        'Administrador del Marketplace',
        'FS Marketplace SA de CV',
        'FSM010101AB1',
        'ADMIN');