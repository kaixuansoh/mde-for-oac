package com.acme.banking.transactions;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for TransactionService.
 * Generated from observability model — do not edit by hand.
 */
public final class TransactionServiceMetrics {

    private static final Meter COM_ACME_BANKING_TRANSACTIONS_METER =
        GlobalOpenTelemetry.get()
            .getMeter("com.acme.banking.transactions");

    public final LongCounter transactions_initiated_total = COM_ACME_BANKING_TRANSACTIONS_METER
        .counterBuilder("transactions.initiated.total").setUnit("1").setDescription("Transfers initiated").build();

    public final LongCounter transactions_completed_total = COM_ACME_BANKING_TRANSACTIONS_METER
        .counterBuilder("transactions.completed.total").setUnit("1").setDescription("Transfers completed successfully").build();

    public final LongCounter transactions_failed_total = COM_ACME_BANKING_TRANSACTIONS_METER
        .counterBuilder("transactions.failed.total").setUnit("1").setDescription("Failed transfer attempts").build();

    public final DoubleHistogram transaction_duration = COM_ACME_BANKING_TRANSACTIONS_METER
        .histogramBuilder("transaction.duration").setUnit("ms").setDescription("End-to-end transfer latency").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

    public final LongUpDownCounter transactions_in_flight = COM_ACME_BANKING_TRANSACTIONS_METER
        .upDownCounterBuilder("transactions.in_flight").setUnit("1").setDescription("In-flight transfers").build();

}