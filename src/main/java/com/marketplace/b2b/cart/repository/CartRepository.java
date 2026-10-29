package com.marketplace.b2b.cart.repository;

import com.marketplace.b2b.cart.entity.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /** Trae el carrito con sus detalles y productos en una sola consulta (evita N+1). */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(Long userId);

    /**
     * Solo los ids de los productos del carrito, SIN cargar los productos.
     * Se usa para bloquearlos antes de leer sus existencias al crear el pedido.
     */
    @Query("select ci.product.id from CartItem ci where ci.cart.user.id = :userId order by ci.product.id")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);
}