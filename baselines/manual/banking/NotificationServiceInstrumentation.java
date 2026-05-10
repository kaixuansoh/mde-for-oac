package com.acme.banking.notifications;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

public final class NotificationServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.banking.notifications", "1.4.0");

    public Span startSendNotification() {
        return tracer.spanBuilder("SendNotification")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("messaging.system", "kafka")
                .startSpan();
    }

    public Span startDispatchEmail(Context parent) {
        return tracer.spanBuilder("DispatchEmail")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("net.peer.name", "smtp-relay")
                .startSpan();
    }

    public Span startDispatchPush(Context parent) {
        return tracer.spanBuilder("DispatchPush")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("net.peer.name", "fcm-gateway")
                .startSpan();
    }
}
