package com.acme.orders;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

public final class OrderServiceMetrics {

    private static final List<Double> ORDER_BUCKETS = List.of(
            5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0,
            1000.0, 2500.0, 5000.0, 10000.0);

    public final LongCounter ordersCreated;
    public final LongCounter ordersFailed;
    public final DoubleHistogram orderDuration;
    public final LongUpDownCounter ordersPending;

    public OrderServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry().getMeter("com.acme.orders");
        ordersCreated = m.counterBuilder("orders.created.total")
                .setUnit("1")
                .setDescription("Successfully created orders")
                .build();
        ordersFailed = m.counterBuilder("orders.failed.total")
                .setUnit("1")
                .setDescription("Failed order attempts")
                .build();
        orderDuration = m.histogramBuilder("order.duration")
                .setUnit("ms")
                .setDescription("End-to-end order creation latency")
                .setExplicitBucketBoundariesAdvice(ORDER_BUCKETS)
                .build();
        ordersPending = m.upDownCounterBuilder("orders.pending")
                .setUnit("1")
                .setDescription("In-flight orders awaiting processing")
                .build();
    }
}
