package com.acme.orders;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class OrderServiceLogs {

    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.orders");

    public void inventoryValidationFailed() {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Failed to validate inventory")
                .setAttribute(ERROR_TYPE, "upstream_failure")
                .emit();
    }
}
