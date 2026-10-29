package com.marketplace.b2b.integration.admin;

import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
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
public class IntegrationAdminController {

    private final IntegrationAdminService adminService;

    @GetMapping("/outbox")
    public OutboxStatusResponse status() {
        return adminService.status();
    }

    @GetMapping("/outbox/events")
    public List<OutboxEventResponse> events(@RequestParam(defaultValue = "PENDING") OutboxStatus status) {
        return adminService.events(status);
    }

    @PostMapping("/outbox/events/{id}/retry")
    public OutboxEventResponse retry(@PathVariable Long id) {
        return adminService.retry(id);
    }

    /** Ejemplo: PUT /api/v1/admin/integrations/erp/availability?available=false */
    @PutMapping("/{system}/availability")
    public OutboxStatusResponse setAvailability(@PathVariable String system, @RequestParam boolean available) {
        return adminService.setAvailability(system, available);
    }
}