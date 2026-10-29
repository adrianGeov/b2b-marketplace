package com.marketplace.b2b.payment.mapper;

import com.marketplace.b2b.payment.dto.PaymentResponse;
import com.marketplace.b2b.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    @Mapping(target = "orderStatus", source = "order.status")
    PaymentResponse toResponse(Payment payment);
}