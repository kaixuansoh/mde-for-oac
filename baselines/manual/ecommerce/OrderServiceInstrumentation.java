package com.acme.orders;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Tracer for OrderService.
 */
public final class OrderServiceInstrumentation {

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer("com.acme.orders", "1.3.0");

    public Span startCreateOrder() {
        return tracer.spanBuilder("CreateOrder")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.request.method", "POST")
                .setAttribute("http.route", "/orders")
                .startSpan();
    }

    public Span startValidateInventory(Context parent) {
        return tracer.spanBuilder("ValidateInventory")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("net.peer.name", "inventory-service")
                .setAttribute("rpc.system", "grpc")
                .startSpan();
    }

    public Span startChargePayment(Context parent) {
        return tracer.spanBuilder("ChargePayment")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(parent)
                .setAttribute("net.peer.name", "payment-gateway")
                .startSpan();
    }

    public Span startPersistOrder(Context parent) {
        return tracer.spanBuilder("PersistOrder")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parent)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation", "INSERT")
                .startSpan();
    }
}
