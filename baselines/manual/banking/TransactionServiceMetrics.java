package com.acme.banking.transactions;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

public final class TransactionServiceMetrics {

    private static final List<Double> TRANSACTION_BUCKETS = List.of(
            5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0,
            1000.0, 2500.0, 5000.0, 10000.0);

    public final LongCounter initiated;
    public final LongCounter completed;
    public final LongCounter failed;
    public final DoubleHistogram duration;
    public final LongUpDownCounter inFlight;

    public TransactionServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry()
                .getMeter("com.acme.banking.transactions");
        initiated = m.counterBuilder("transactions.initiated.total")
                .setUnit("1")
                .setDescription("Transfers initiated")
                .build();
        completed = m.counterBuilder("transactions.completed.total")
                .setUnit("1")
                .setDescription("Transfers completed successfully")
                .build();
        failed = m.counterBuilder("transactions.failed.total")
                .setUnit("1")
                .setDescription("Failed transfer attempts")
                .build();
        duration = m.histogramBuilder("transaction.duration")
                .setUnit("ms")
                .setDescription("End-to-end transfer latency")
                .setExplicitBucketBoundariesAdvice(TRANSACTION_BUCKETS)
                .build();
        inFlight = m.upDownCounterBuilder("transactions.in_flight")
                .setUnit("1")
                .setDescription("In-flight transfers")
                .build();
    }
}
