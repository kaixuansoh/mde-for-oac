package com.acme.banking.notifications;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for NotificationService.
 * Generated from observability model — do not edit by hand.
 */
public final class NotificationServiceInstrumentation {

    private static final Tracer COM_ACME_BANKING_NOTIFICATIONS_TRACER =
        GlobalOpenTelemetry.get()
            .getTracer("com.acme.banking.notifications", "1.4.0");

    public Span startSendNotification() {
        SpanBuilder builder = COM_ACME_BANKING_NOTIFICATIONS_TRACER
            .spanBuilder("SendNotification")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("messaging.system", "kafka");
        Span span = builder.startSpan();
        return span;
    }

    public Span startDispatchEmail(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_NOTIFICATIONS_TRACER
            .spanBuilder("DispatchEmail")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "smtp-relay");
        Span span = builder.startSpan();
        return span;
    }

    public Span startDispatchPush(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_NOTIFICATIONS_TRACER
            .spanBuilder("DispatchPush")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "fcm-gateway");
        Span span = builder.startSpan();
        return span;
    }

}