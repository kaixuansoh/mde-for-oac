package com.acme.payments;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Structured-logging helpers for PaymentService.
 *
 * Hand-written counterpart to the framework-generated
 * PaymentServiceLogs. Same severities, bodies, and attributes.
 */
public final class PaymentServiceLogs {

    private static final String SCOPE = "com.acme.payments";

    private static final AttributeKey<String> ERROR_TYPE =
            AttributeKey.stringKey("error.type");

    private final Logger logger;

    public PaymentServiceLogs() {
        this.logger = OpenTelemetry.getGlobalOpenTelemetry()
                .getLogsBridge()
                .get(SCOPE);
    }

    public void paymentProcessingFailed(String errorType) {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Payment processing failed")
                .setAttribute(ERROR_TYPE, errorType)
                .emit();
    }

    public void paymentProcessedSuccessfully() {
        logger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setSeverityText("INFO")
                .setBody("Payment processed successfully")
                .emit();
    }
}
