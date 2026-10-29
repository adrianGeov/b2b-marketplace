package com.marketplace.b2b.integration.admin;

import com.marketplace.b2b.common.config.OpenApiConfig;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/integrations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_ADMIN_INTEGRATIONS)
public class IntegrationAdminController {

    private final IntegrationAdminService adminService;

    @GetMapping("/outbox")
    @Operation(summary = "Estado del outbox",
            description = "Eventos pendientes, procesados y fallidos, y disponibilidad del ERP/CRM simulados.")
    public OutboxStatusResponse status() {
        return adminService.status();
    }

    @GetMapping("/outbox/events")
    @Operation(summary = "Eventos por estatus", description = "Últimos 50 eventos con intentos y último error.")
    public List<OutboxEventResponse> events(@RequestParam(defaultValue = "PENDING") OutboxStatus status) {
        return adminService.events(status);
    }

    @PostMapping("/outbox/events/{id}/retry")
    @Operation(summary = "Reintentar evento", description = "Reencola un evento PENDING o FAILED.")
    public OutboxEventResponse retry(@PathVariable Long id) {
        return adminService.retry(id);
    }

    @PutMapping("/{system}/availability")
    @Operation(summary = "Simular caída o recuperación (demo)",
            description = "Solo para la demostración del MVP: enciende o apaga el ERP/CRM simulado.")
    public OutboxStatusResponse setAvailability(
            @Parameter(description = "erp o crm", example = "erp") @PathVariable String system,
            @Parameter(description = "false = caído, true = disponible", example = "false")
            @RequestParam boolean available) {
        return adminService.setAvailability(system, available);
    }
}