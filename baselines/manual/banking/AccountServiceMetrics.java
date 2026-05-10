package com.acme.banking.accounts;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

public final class AccountServiceMetrics {

    public final LongCounter queriesTotal;
    public final DoubleGauge balance;
    public final LongCounter errorsTotal;

    public AccountServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.banking.accounts");
        queriesTotal = m.counterBuilder("account.queries.total")
                .setUnit("1")
                .setDescription("Total account queries")
                .build();
        balance = m.gaugeBuilder("account.balance")
                .setUnit("1")
                .setDescription("Current account balance (sampled)")
                .build();
        errorsTotal = m.counterBuilder("account.errors.total")
                .setUnit("1")
                .setDescription("Account service errors")
                .build();
    }
}
