package com.acme.banking.ledger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class LedgerServiceLogs {

    private static final AttributeKey<Double> DISCREPANCY = AttributeKey.doubleKey("discrepancy");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.banking.ledger");

    public void ledgerImbalanceDetected(double discrepancy) {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Ledger imbalance detected")
                .setAttribute(DISCREPANCY, discrepancy)
                .emit();
    }
}
