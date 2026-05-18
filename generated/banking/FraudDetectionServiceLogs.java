package com.acme.banking.fraud;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for FraudDetectionService.
 * Generated from observability model — do not edit by hand.
 */
public final class FraudDetectionServiceLogs {

    private static final Logger COM_ACME_BANKING_FRAUD_LOGGER =
        GlobalOpenTelemetry.get()
            .getLogsBridge()
            .get("com.acme.banking.fraud");

    public void logHighRiskTransactionFlagged() {
        COM_ACME_BANKING_FRAUD_LOGGER.logRecordBuilder()
            .setSeverity(Severity.WARN)
            .setSeverityText("WARN")
            .setBody("High risk transaction flagged")
            .setAttribute(AttributeKey.doubleKey("risk.score"), 0.92d)
            .emit();
    }

}