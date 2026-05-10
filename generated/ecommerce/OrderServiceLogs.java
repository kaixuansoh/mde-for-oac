package com.acme.orders;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for OrderService.
 * Generated from observability model — do not edit by hand.
 */
public final class OrderServiceLogs {

    private static final Logger COM_ACME_ORDERS_LOGGER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge()
            .get("com.acme.orders");

    public void logFailedToValidateInventory() {
        COM_ACME_ORDERS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Failed to validate inventory")
            .setAttribute(AttributeKey.stringKey("error.type"), "upstream_failure")
            .emit();
    }

}