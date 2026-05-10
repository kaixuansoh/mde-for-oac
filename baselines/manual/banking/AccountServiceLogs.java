package com.acme.banking.accounts;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class AccountServiceLogs {

    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.banking.accounts");

    public void accountDatabaseUnreachable(String errorType) {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Account database unreachable")
                .setAttribute(ERROR_TYPE, errorType)
                .emit();
    }
}
