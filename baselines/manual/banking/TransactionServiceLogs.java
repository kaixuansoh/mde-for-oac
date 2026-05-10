package com.acme.banking.transactions;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class TransactionServiceLogs {

    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.banking.transactions");

    public void transferFailedInsufficientFunds() {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Transfer failed insufficient funds")
                .setAttribute(ERROR_TYPE, "insufficient_funds")
                .emit();
    }

    public void transferCompleted() {
        logger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setSeverityText("INFO")
                .setBody("Transfer completed successfully")
                .emit();
    }
}
