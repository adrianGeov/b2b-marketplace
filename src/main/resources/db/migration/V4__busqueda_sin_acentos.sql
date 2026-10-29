-- =====================================================================
-- V4: Búsqueda sin acentos ("cafe" encuentra "Café", "toner" encuentra "Tóner")
-- La extensión unaccent viene incluida en la imagen oficial de PostgreSQL.
-- En un servidor administrado sin permisos de superusuario, un DBA debe crearla.
-- =====================================================================
CREATE EXTENSION IF NOT EXISTS unaccent;