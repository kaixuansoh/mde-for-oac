package com.acme.banking.notifications;

import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Auto-generated OpenTelemetry metric registry for NotificationService.
 * Generated from observability model — do not edit by hand.
 */
public final class NotificationServiceMetrics {

    private static final Meter COM_ACME_BANKING_NOTIFICATIONS_METER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getMeter("com.acme.banking.notifications");

    public final LongCounter notifications_sent_total = COM_ACME_BANKING_NOTIFICATIONS_METER
        .counterBuilder("notifications.sent.total").setUnit("1").setDescription("Notifications successfully sent").build();

    public final LongCounter notifications_failed_total = COM_ACME_BANKING_NOTIFICATIONS_METER
        .counterBuilder("notifications.failed.total").setUnit("1").setDescription("Notification dispatch failures").build();

    public final DoubleHistogram notification_dispatch_duration = COM_ACME_BANKING_NOTIFICATIONS_METER
        .histogramBuilder("notification.dispatch.duration").setUnit("ms").setDescription("Time to dispatch a notification").setExplicitBucketBoundariesAdvice(java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0)).build();

}