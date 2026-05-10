package com.acme.banking.auth;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for AuthService.
 * Generated from observability model — do not edit by hand.
 */
public final class AuthServiceLogs {

    private static final Logger COM_ACME_BANKING_AUTH_LOGGER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge()
            .get("com.acme.banking.auth");

    public void logMfaVerificationFailed() {
        COM_ACME_BANKING_AUTH_LOGGER.logRecordBuilder()
            .setSeverity(Severity.WARN)
            .setSeverityText("WARN")
            .setBody("MFA verification failed")
            .setAttribute(AttributeKey.stringKey("reason"), "invalid_code")
            .emit();
    }

    public void logUserAuthenticatedSuccessfully() {
        COM_ACME_BANKING_AUTH_LOGGER.logRecordBuilder()
            .setSeverity(Severity.INFO)
            .setSeverityText("INFO")
            .setBody("User authenticated successfully")
            .emit();
    }

}