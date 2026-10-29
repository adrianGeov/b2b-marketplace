package com.marketplace.b2b.integration.admin;

import com.marketplace.b2b.common.exception.BusinessException;
import com.marketplace.b2b.common.exception.ErrorCode;
import com.marketplace.b2b.common.exception.ResourceNotFoundException;
import com.marketplace.b2b.integration.crm.SimulatedCrmClient;
import com.marketplace.b2b.integration.erp.SimulatedErpClient;
import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import com.marketplace.b2b.integration.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationAdminService {

    private final OutboxEventRepository repository;
    private final SimulatedErpClient erpClient;
    private final SimulatedCrmClient crmClient;
    private final Clock clock;

    @Transactional(readOnly = true)
    public OutboxStatusResponse status() {
        return new OutboxStatusResponse(
                repository.countByStatus(OutboxStatus.PENDING),
                repository.countByStatus(OutboxStatus.PROCESSED),
                repository.countByStatus(OutboxStatus.FAILED),
                erpClient.isAvailable(),
                crmClient.isAvailable());
    }

    @Transactional(readOnly = true)
    public List<OutboxEventResponse> events(OutboxStatus status) {
        return repository.findTop50ByStatusOrderByCreatedAtDesc(status).stream()
                .map(OutboxEventResponse::from)
                .toList();
    }

    /** Reintento manual de un evento (por ejemplo, uno FAILED después de corregir el problema). */
    @Transactional
    public OutboxEventResponse retry(Long eventId) {
        OutboxEvent event = repository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", eventId));
        if (event.getStatus() == OutboxStatus.PROCESSED) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "El evento " + eventId + " ya fue procesado");
        }
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setNextAttemptAt(Instant.now(clock));
        log.info("Evento {} reencolado manualmente", eventId);
        return OutboxEventResponse.from(event);
    }

    /** Solo para la demo del MVP: simula la caída o recuperación del ERP / CRM. */
    public OutboxStatusResponse setAvailability(String system, boolean available) {
        switch (system.toLowerCase(Locale.ROOT)) {
            case "erp" -> erpClient.setAvailable(available);
            case "crm" -> crmClient.setAvailable(available);
            default -> throw new ResourceNotFoundException("Sistema externo", system);
        }
        return status();
    }
}