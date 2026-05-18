package com.acme.banking.accounts;

import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for AccountService.
 * Generated from observability model — do not edit by hand.
 */
public final class AccountServiceMetrics {

    private static final Meter COM_ACME_BANKING_ACCOUNTS_METER =
        GlobalOpenTelemetry.get()
            .getMeter("com.acme.banking.accounts");

    public final LongCounter account_queries_total = COM_ACME_BANKING_ACCOUNTS_METER
        .counterBuilder("account.queries.total").setUnit("1").setDescription("Total account queries").build();

    public final DoubleGauge account_balance = COM_ACME_BANKING_ACCOUNTS_METER
        .gaugeBuilder("account.balance").setUnit("1").setDescription("Current account balance (sampled)").build();

    public final LongCounter account_errors_total = COM_ACME_BANKING_ACCOUNTS_METER
        .counterBuilder("account.errors.total").setUnit("1").setDescription("Account service errors").build();

}