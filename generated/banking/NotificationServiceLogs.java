package com.acme.banking.notifications;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

/**
 * Auto-generated OpenTelemetry log emitters for NotificationService.
 * Generated from observability model — do not edit by hand.
 */
public final class NotificationServiceLogs {

    private static final Logger COM_ACME_BANKING_NOTIFICATIONS_LOGGER =
        GlobalOpenTelemetry.get()
            .getLogsBridge()
            .get("com.acme.banking.notifications");

    public void logNotificationDispatchFailed() {
        COM_ACME_BANKING_NOTIFICATIONS_LOGGER.logRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setSeverityText("ERROR")
            .setBody("Notification dispatch failed")
            .setAttribute(AttributeKey.stringKey("channel"), "email")
            .emit();
    }

}