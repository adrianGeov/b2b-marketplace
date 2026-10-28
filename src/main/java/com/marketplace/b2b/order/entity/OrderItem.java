package com.marketplace.b2b.order.entity;

import com.marketplace.b2b.catalog.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Detalle del pedido. Guarda sku, nombre y precio del momento de la compra
 * para que cambios posteriores al catálogo no alteren pedidos históricos.
 */
@Entity
@Table(name = "pedido_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PurchaseOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Product product;

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    @Column(name = "nombre_producto", nullable = false, length = 150)
    private String productName;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "cantidad", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}