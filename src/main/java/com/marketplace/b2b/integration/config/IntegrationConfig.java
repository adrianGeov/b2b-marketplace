package com.marketplace.b2b.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas (procesador del outbox) y las propiedades de integración.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationConfig {
}