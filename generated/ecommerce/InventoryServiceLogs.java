package com.acme.inventory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for InventoryService.
 * Generated from observability model — do not edit by hand.
 */
public final class InventoryServiceLogs {

    private static final Logger COM_ACME_INVENTORY_LOGGER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge()
            .get("com.acme.inventory");

    public void logLowStockThresholdBreached() {
        COM_ACME_INVENTORY_LOGGER.logRecordBuilder()
            .setSeverity(Severity.WARN)
            .setSeverityText("WARN")
            .setBody("Low stock threshold breached")
            .setAttribute(AttributeKey.longKey("threshold"), 10L)
            .emit();
    }

}