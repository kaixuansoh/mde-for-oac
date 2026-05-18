package com.acme.banking.ledger;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for LedgerService.
 * Generated from observability model — do not edit by hand.
 */
public final class LedgerServiceLogs {

    private static final Logger COM_ACME_BANKING_LEDGER_LOGGER =
        GlobalOpenTelemetry.get()
            .getLogsBridge()
            .get("com.acme.banking.ledger");

    public void logLedgerImbalanceDetected() {
        COM_ACME_BANKING_LEDGER_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Ledger imbalance detected")
            .setAttribute(AttributeKey.doubleKey("discrepancy"), 0.01d)
            .emit();
    }

}