package com.acme.payments;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for PaymentService.
 * Generated from observability model — do not edit by hand.
 */
public final class PaymentServiceLogs {

    private static final Logger COM_ACME_PAYMENTS_LOGGER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge()
            .get("com.acme.payments");

    public void logPaymentProcessingFailed() {
        COM_ACME_PAYMENTS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Payment processing failed")
            .setAttribute(AttributeKey.stringKey("error.type"), "card_declined")
            .emit();
    }

    public void logPaymentProcessedSuccessfully() {
        COM_ACME_PAYMENTS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setSeverityText("INFO")
            .setBody("Payment processed successfully")
            .emit();
    }

}