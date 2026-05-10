package com.acme.payments;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Tracing helpers for PaymentService.
 *
 * Hand-written counterpart to the framework-generated
 * PaymentServiceInstrumentation; functionally equivalent (same span
 * names, kinds, attributes, parent-child links, and span events) but
 * written in a typical first-pass style with extracted constants and
 * helper methods.
 */
public final class PaymentServiceInstrumentation {

    private static final String SCOPE_NAME = "com.acme.payments";
    private static final String SCOPE_VERSION = "1.4.0";

    // Reused attribute keys
    private static final String ATTR_HTTP_METHOD = "http.request.method";
    private static final String ATTR_HTTP_ROUTE  = "http.route";
    private static final String ATTR_COMPONENT   = "component";
    private static final String ATTR_STATUS      = "status";

    private final Tracer tracer;

    public PaymentServiceInstrumentation() {
        this.tracer = OpenTelemetry.getGlobalOpenTelemetry()
                .getTracer(SCOPE_NAME, SCOPE_VERSION);
    }

    public Span startProcessPayment() {
        return tracer.spanBuilder("ProcessPayment")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute(ATTR_HTTP_METHOD, "POST")
                .setAttribute(ATTR_HTTP_ROUTE, "/payments")
                .setAttribute(ATTR_COMPONENT, "payment")
                .startSpan();
    }

    public Span startValidatePayment(Context parent) {
        Span span = tracer.spanBuilder("ValidatePayment")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parent)
                .setAttribute(ATTR_COMPONENT, "validation")
                .setAttribute(ATTR_STATUS, "success")
                .startSpan();
        span.setStatus(StatusCode.OK);
        return span;
    }

    public Span startChargeCard(Context parent) {
        Span span = tracer.spanBuilder("ChargeCard")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("db.system", "postgresql")
                .setAttribute("net.peer.name", "card-gateway")
                .startSpan();
        // Card-authorisation event with auth code attached
        span.addEvent("card.authorized", Attributes.builder()
                .put("auth.code", "A100")
                .build());
        return span;
    }

    public Span startPublishPaymentEvent(Context parent) {
        return tracer.spanBuilder("PublishPaymentEvent")
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(parent)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", "payment.completed")
                .startSpan();
    }
}
