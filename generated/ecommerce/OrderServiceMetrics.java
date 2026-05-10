package com.acme.orders;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for OrderService.
 * Generated from observability model — do not edit by hand.
 */
public final class OrderServiceMetrics {

    private static final Meter COM_ACME_ORDERS_METER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getMeter("com.acme.orders");

    public final LongCounter orders_created_total = COM_ACME_ORDERS_METER
        .counterBuilder("orders.created.total").setUnit("1").setDescription("Successfully created orders").build();

    public final LongCounter orders_failed_total = COM_ACME_ORDERS_METER
        .counterBuilder("orders.failed.total").setUnit("1").setDescription("Failed order attempts").build();

    public final DoubleHistogram order_duration = COM_ACME_ORDERS_METER
        .histogramBuilder("order.duration").setUnit("ms").setDescription("End-to-end order creation latency").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

    public final LongUpDownCounter orders_pending = COM_ACME_ORDERS_METER
        .upDownCounterBuilder("orders.pending").setUnit("1").setDescription("In-flight orders awaiting processing").build();

}