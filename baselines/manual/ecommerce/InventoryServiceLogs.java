package com.acme.inventory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class InventoryServiceLogs {

    private static final AttributeKey<Long> THRESHOLD = AttributeKey.longKey("threshold");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.inventory");

    public void lowStockBreached(long threshold) {
        logger.logRecordBuilder()
                .setSeverity(Severity.WARN)
                .setSeverityText("WARN")
                .setBody("Low stock threshold breached")
                .setAttribute(THRESHOLD, threshold)
                .emit();
    }
}
