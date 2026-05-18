package com.acme.banking.fraud;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.GlobalOpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for FraudDetectionService.
 * Generated from observability model — do not edit by hand.
 */
public final class FraudDetectionServiceMetrics {

    private static final Meter COM_ACME_BANKING_FRAUD_METER =
        GlobalOpenTelemetry.get()
            .getMeter("com.acme.banking.fraud");

    public final LongCounter fraud_scores_computed = COM_ACME_BANKING_FRAUD_METER
        .counterBuilder("fraud.scores.computed").setUnit("1").setDescription("Risk scores computed").build();

    public final DoubleHistogram fraud_score = COM_ACME_BANKING_FRAUD_METER
        .histogramBuilder("fraud.score").setUnit("1").setDescription("Distribution of risk scores").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

    public final LongCounter fraud_flagged_total = COM_ACME_BANKING_FRAUD_METER
        .counterBuilder("fraud.flagged.total").setUnit("1").setDescription("Transactions flagged as high-risk").build();

}