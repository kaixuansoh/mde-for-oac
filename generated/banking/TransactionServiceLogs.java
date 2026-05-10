package com.acme.banking.transactions;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for TransactionService.
 * Generated from observability model — do not edit by hand.
 */
public final class TransactionServiceLogs {

    private static final Logger COM_ACME_BANKING_TRANSACTIONS_LOGGER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge()
            .get("com.acme.banking.transactions");

    public void logTransferFailedInsufficientFunds() {
        COM_ACME_BANKING_TRANSACTIONS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Transfer failed insufficient funds")
            .setAttribute(AttributeKey.stringKey("error.type"), "insufficient_funds")
            .emit();
    }

    public void logTransferCompletedSuccessfully() {
        COM_ACME_BANKING_TRANSACTIONS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setSeverityText("INFO")
            .setBody("Transfer completed successfully")
            .emit();
    }

}