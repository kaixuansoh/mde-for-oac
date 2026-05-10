package com.acme.payments;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for PaymentService.
 * Generated from observability model — do not edit by hand.
 */
public final class PaymentServiceInstrumentation {

    private static final Tracer COM_ACME_PAYMENTS_TRACER =
        OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.payments", "1.4.0");

    public Span startProcessPayment() {
        SpanBuilder builder = COM_ACME_PAYMENTS_TRACER
            .spanBuilder("ProcessPayment")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("http.request.method", "POST");
        builder.setAttribute("http.route", "/payments");
        builder.setAttribute("component", "payment");
        Span span = builder.startSpan();
        return span;
    }

    public Span startValidatePayment(Context parentContext) {
        SpanBuilder builder = COM_ACME_PAYMENTS_TRACER
            .spanBuilder("ValidatePayment")
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(parentContext);
        builder.setAttribute("component", "validation");
        builder.setAttribute("status", "success");
        Span span = builder.startSpan();
        span.setStatus(StatusCode.OK);
        return span;
    }

    public Span startChargeCard(Context parentContext) {
        SpanBuilder builder = COM_ACME_PAYMENTS_TRACER
            .spanBuilder("ChargeCard")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("db.system", "postgresql");
        builder.setAttribute("net.peer.name", "card-gateway");
        Span span = builder.startSpan();
        span.addEvent("card.authorized", Attributes.builder()
            .put("auth.code", "A100")
            .build());
        return span;
    }

    public Span startPublishPaymentEvent(Context parentContext) {
        SpanBuilder builder = COM_ACME_PAYMENTS_TRACER
            .spanBuilder("PublishPaymentEvent")
            .setSpanKind(SpanKind.PRODUCER)
            .setParent(parentContext);
        builder.setAttribute("messaging.system", "kafka");
        builder.setAttribute("messaging.destination", "payment.completed");
        Span span = builder.startSpan();
        return span;
    }

}