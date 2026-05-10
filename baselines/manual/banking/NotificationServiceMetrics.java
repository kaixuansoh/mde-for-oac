package com.acme.banking.notifications;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.List;

public final class NotificationServiceMetrics {

    private static final List<Double> DISPATCH_BUCKETS = List.of(
            5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0,
            1000.0, 2500.0, 5000.0, 10000.0);

    public final LongCounter sentTotal;
    public final LongCounter failedTotal;
    public final DoubleHistogram dispatchDuration;

    public NotificationServiceMetrics() {
        Meter m = OpenTelemetry.getGlobalOpenTelemetry()
                .getMeter("com.acme.banking.notifications");
        sentTotal = m.counterBuilder("notifications.sent.total")
                .setUnit("1")
                .setDescription("Notifications successfully sent")
                .build();
        failedTotal = m.counterBuilder("notifications.failed.total")
                .setUnit("1")
                .setDescription("Notification dispatch failures")
                .build();
        dispatchDuration = m.histogramBuilder("notification.dispatch.duration")
                .setUnit("ms")
                .setDescription("Time to dispatch a notification")
                .setExplicitBucketBoundariesAdvice(DISPATCH_BUCKETS)
                .build();
    }
}
