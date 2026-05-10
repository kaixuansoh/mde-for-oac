package com.acme.banking.fraud;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class FraudDetectionServiceLogs {

    private static final AttributeKey<Double> RISK_SCORE = AttributeKey.doubleKey("risk.score");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.banking.fraud");

    public void highRiskTransactionFlagged(double riskScore) {
        logger.logRecordBuilder()
                .setSeverity(Severity.WARN)
                .setSeverityText("WARN")
                .setBody("High risk transaction flagged")
                .setAttribute(RISK_SCORE, riskScore)
                .emit();
    }
}
