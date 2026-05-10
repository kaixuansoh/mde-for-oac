package com.acme.payments;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

/**
 * Metric registry for PaymentService.
 *
 * Hand-written counterpart to the framework-generated
 * PaymentServiceMetrics. Functionally equivalent — same metric names,
 * types, units, and descriptions.
 */
public final class PaymentServiceMetrics {

    private static final String SCOPE = "com.acme.payments";

    // Histogram bucket boundaries for the duration histogram (ms)
    private static final List<Double> DURATION_BUCKETS = List.of(
            5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0,
            1000.0, 2500.0, 5000.0, 10000.0);

    public final LongCounter requestsTotal;
    public final DoubleHistogram duration;
    public final LongCounter errorsTotal;
    public final LongUpDownCounter activeConnections;

    public PaymentServiceMetrics() {
        Meter meter = OpenTelemetry.getGlobalOpenTelemetry().getMeter(SCOPE);

        this.requestsTotal = meter.counterBuilder("payment.requests.total")
                .setUnit("1")
                .setDescription("Total payment requests received")
                .build();

        this.duration = meter.histogramBuilder("payment.duration")
                .setUnit("ms")
                .setDescription("End-to-end payment processing latency")
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS)
                .build();

        this.errorsTotal = meter.counterBuilder("payment.errors")
                .setUnit("1")
                .setDescription("Total payment errors")
                .build();

        this.activeConnections = meter.upDownCounterBuilder("payment.active_connections")
                .setUnit("1")
                .setDescription("In-flight payment connections")
                .build();
    }
}
