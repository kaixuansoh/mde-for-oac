package com.acme.banking.ledger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

public final class LedgerServiceMetrics {

    public final LongCounter entriesTotal;
    public final DoubleGauge balanceDiscrepancy;

    public LedgerServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.banking.ledger");
        entriesTotal = m.counterBuilder("ledger.entries.total")
                .setUnit("1")
                .setDescription("Ledger entries written")
                .build();
        balanceDiscrepancy = m.gaugeBuilder("ledger.balance.discrepancy")
                .setUnit("1")
                .setDescription("Aggregate ledger imbalance (zero in steady state)")
                .build();
    }
}
