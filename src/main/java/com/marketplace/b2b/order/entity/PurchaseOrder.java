package com.marketplace.b2b.order.entity;

import com.marketplace.b2b.common.entity.AuditableEntity;
import com.marketplace.b2b.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de compra. La clase se llama PurchaseOrder porque ORDER es palabra reservada en JPQL.
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Folio público del pedido (lo que ve el cliente). */
    @Column(name = "folio", nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /** Llave enviada por el cliente en el header Idempotency-Key. */
    @Column(name = "llave_idempotencia", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "id_pedido_erp", length = 50)
    private String erpOrderId;

    @Column(name = "fecha_entrega_estimada")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "fecha_entrega")
    private Instant deliveredAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}