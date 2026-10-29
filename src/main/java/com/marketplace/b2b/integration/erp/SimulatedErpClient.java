package com.marketplace.b2b.integration.erp;

import com.marketplace.b2b.integration.IntegrationException;
import com.marketplace.b2b.integration.config.IntegrationProperties;
import com.marketplace.b2b.integration.event.OrderPaidPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ERP simulado: latencia configurable y "interruptor" para simular caídas en la demo.
 */
@Slf4j
@Component
public class SimulatedErpClient implements ErpClient {

    private final AtomicBoolean available;
    private final Duration latency;

    /** Órdenes ya creadas por número de pedido (idempotencia del lado del ERP). */
    private final Map<String, String> salesOrders = new ConcurrentHashMap<>();

    public SimulatedErpClient(IntegrationProperties properties) {
        this.available = new AtomicBoolean(properties.erp().available());
        this.latency = properties.erp().latency();
    }

    @Override
    public String createSalesOrder(OrderPaidPayload payload) {
        if (!available.get()) {
            throw new IntegrationException("ERP no disponible (caída simulada)");
        }

        String existing = salesOrders.get(payload.orderNumber());
        if (existing != null) {
            return existing;
        }

        simulateLatency();
        String erpOrderId = salesOrders.computeIfAbsent(payload.orderNumber(),
                key -> "ERP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT));
        log.info("ERP: orden de venta {} creada para el pedido {} ({} ms)",
                erpOrderId, payload.orderNumber(), latency.toMillis());
        return erpOrderId;
    }

    public boolean isAvailable() {
        return available.get();
    }

    public void setAvailable(boolean value) {
        available.set(value);
        log.warn("ERP simulado ahora está {}", value ? "DISPONIBLE" : "CAÍDO");
    }

    private void simulateLatency() {
        try {
            Thread.sleep(latency.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Llamada al ERP interrumpida");
        }
    }
}