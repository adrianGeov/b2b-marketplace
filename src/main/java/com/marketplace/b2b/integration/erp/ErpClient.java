package com.marketplace.b2b.integration.erp;

import com.marketplace.b2b.integration.event.OrderPaidPayload;

/**
 * Contrato con el ERP. En producción sería un cliente HTTP/SOAP con timeouts.
 */
public interface ErpClient {

    /**
     * Crea la orden de venta. Debe ser idempotente por número de pedido.
     *
     * @return identificador de la orden de venta en el ERP
     */
    String createSalesOrder(OrderPaidPayload payload);
}