package com.acme.banking.notifications;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;

public final class NotificationServiceLogs {

    private static final AttributeKey<String> CHANNEL = AttributeKey.stringKey("channel");

    private final Logger logger = OpenTelemetry.getGlobalOpenTelemetry()
            .getLogsBridge().get("com.acme.banking.notifications");

    public void notificationDispatchFailed(String channel) {
        logger.logRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setSeverityText("ERROR")
                .setBody("Notification dispatch failed")
                .setAttribute(CHANNEL, channel)
                .emit();
    }
}
