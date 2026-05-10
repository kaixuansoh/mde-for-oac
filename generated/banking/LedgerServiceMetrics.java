package com.acme.banking.ledger;

import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for LedgerService.
 * Generated from observability model — do not edit by hand.
 */
public final class LedgerServiceMetrics {

    private static final Meter COM_ACME_BANKING_LEDGER_METER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getMeter("com.acme.banking.ledger");

    public final LongCounter ledger_entries_total = COM_ACME_BANKING_LEDGER_METER
        .counterBuilder("ledger.entries.total").setUnit("1").setDescription("Ledger entries written").build();

    public final DoubleGauge ledger_balance_discrepancy = COM_ACME_BANKING_LEDGER_METER
        .gaugeBuilder("ledger.balance.discrepancy").setUnit("1").setDescription("Aggregate ledger imbalance (zero in steady state)").build();

}