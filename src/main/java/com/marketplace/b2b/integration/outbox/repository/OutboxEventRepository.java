package com.marketplace.b2b.integration.outbox.repository;

import com.marketplace.b2b.integration.outbox.entity.OutboxEvent;
import com.marketplace.b2b.integration.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Toma un lote de eventos pendientes y los bloquea.
     * SKIP LOCKED permite correr varias instancias de la app sin procesar el mismo evento dos veces.
     */
    @Query(value = """
            SELECT * FROM eventos_outbox
            WHERE estatus = 'PENDING' AND fecha_siguiente_intento <= now()
            ORDER BY fecha_creacion
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("limit") int limit);

    long countByStatus(OutboxStatus status);
}