package com.marketplace.b2b.integration.crm;

import com.marketplace.b2b.integration.event.CustomerRegisteredPayload;
import com.marketplace.b2b.integration.event.OrderPaidPayload;

public interface CrmClient {

    /**
     * Crea o actualiza el cliente (lead) en el CRM.
     *
     * @return identificador del cliente en el CRM
     */
    String upsertCustomer(CustomerRegisteredPayload payload);

    /** Registra una venta ganada (alimenta la tasa de conversión). */
    void registerSale(OrderPaidPayload payload);
}