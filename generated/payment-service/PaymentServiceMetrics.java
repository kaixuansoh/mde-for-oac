package com.acme.payments;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for PaymentService.
 * Generated from observability model — do not edit by hand.
 */
public final class PaymentServiceMetrics {

    private static final Meter COM_ACME_PAYMENTS_METER =
        GlobalOpenTelemetry.get()
            .getMeter("com.acme.payments");

    public final LongCounter payment_requests_total = COM_ACME_PAYMENTS_METER
        .counterBuilder("payment.requests.total").setUnit("1").setDescription("Total payment requests received").build();

    public final DoubleHistogram payment_duration = COM_ACME_PAYMENTS_METER
        .histogramBuilder("payment.duration").setUnit("ms").setDescription("End-to-end payment processing latency").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

    public final LongCounter payment_errors = COM_ACME_PAYMENTS_METER
        .counterBuilder("payment.errors").setUnit("1").setDescription("Total payment errors").build();

    public final LongUpDownCounter payment_active_connections = COM_ACME_PAYMENTS_METER
        .upDownCounterBuilder("payment.active_connections").setUnit("1").setDescription("In-flight payment connections").build();

}