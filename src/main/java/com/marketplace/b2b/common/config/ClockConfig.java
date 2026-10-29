package com.marketplace.b2b.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj inyectable: en producción es el del sistema (UTC); en pruebas se usa un reloj fijo
 * para que las fechas (folio, entrega estimada) sean predecibles.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}