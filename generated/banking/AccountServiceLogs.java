package com.acme.banking.accounts;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for AccountService.
 * Generated from observability model — do not edit by hand.
 */
public final class AccountServiceLogs {

    private static final Logger COM_ACME_BANKING_ACCOUNTS_LOGGER =
        GlobalOpenTelemetry.get()
            .getLogsBridge()
            .get("com.acme.banking.accounts");

    public void logAccountDatabaseUnreachable() {
        COM_ACME_BANKING_ACCOUNTS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Account database unreachable")
            .setAttribute(AttributeKey.stringKey("error.type"), "db_timeout")
            .emit();
    }

}