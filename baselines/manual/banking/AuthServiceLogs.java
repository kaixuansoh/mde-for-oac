package com.acme.banking.auth;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class AuthServiceLogs {

    private static final AttributeKey<String> REASON = AttributeKey.stringKey("reason");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.banking.auth");

    public void mfaVerificationFailed(String reason) {
        logger.logRecordBuilder()
                .setSeverity(Severity.WARN)
                .setSeverityText("WARN")
                .setBody("MFA verification failed")
                .setAttribute(REASON, reason)
                .emit();
    }

    public void userAuthenticated() {
        logger.logRecordBuilder()
                .setSeverity(Severity.INFO)
                .setSeverityText("INFO")
                .setBody("User authenticated successfully")
                .emit();
    }
}
