package com.marketplace.b2b.integration.crm;

import com.marketplace.b2b.integration.IntegrationException;
import com.marketplace.b2b.integration.config.IntegrationProperties;
import com.marketplace.b2b.integration.event.CustomerRegisteredPayload;
import com.marketplace.b2b.integration.event.OrderPaidPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class SimulatedCrmClient implements CrmClient {

    private final AtomicBoolean available;
    private final Duration latency;
    private final Map<Long, String> customers = new ConcurrentHashMap<>();

    public SimulatedCrmClient(IntegrationProperties properties) {
        this.available = new AtomicBoolean(properties.crm().available());
        this.latency = properties.crm().latency();
    }

    @Override
    public String upsertCustomer(CustomerRegisteredPayload payload) {
        checkAvailability();
        simulateLatency();
        String crmId = customers.computeIfAbsent(payload.userId(), id -> "CRM-CUST-" + String.format("%06d", id));
        log.info("CRM: cliente {} registrado como {}", payload.userId(), crmId);
        return crmId;
    }

    @Override
    public void registerSale(OrderPaidPayload payload) {
        checkAvailability();
        simulateLatency();
        log.info("CRM: venta registrada pedido={} total={}", payload.orderNumber(), payload.total());
    }

    public boolean isAvailable() {
        return available.get();
    }

    public void setAvailable(boolean value) {
        available.set(value);
        log.warn("CRM simulado ahora está {}", value ? "DISPONIBLE" : "CAÍDO");
    }

    private void checkAvailability() {
        if (!available.get()) {
            throw new IntegrationException("CRM no disponible (caída simulada)");
        }
    }

    private void simulateLatency() {
        try {
            Thread.sleep(latency.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("Llamada al CRM interrumpida");
        }
    }
}