package com.marketplace.b2b.order.mapper;

import com.marketplace.b2b.order.dto.OrderItemResponse;
import com.marketplace.b2b.order.dto.OrderResponse;
import com.marketplace.b2b.order.dto.OrderSummaryResponse;
import com.marketplace.b2b.order.entity.OrderItem;
import com.marketplace.b2b.order.entity.PurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OrderMapper {

    @Mapping(target = "total", source = "totalAmount")
    OrderResponse toResponse(PurchaseOrder order);

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "total", source = "totalAmount")
    OrderSummaryResponse toSummary(PurchaseOrder order);
}