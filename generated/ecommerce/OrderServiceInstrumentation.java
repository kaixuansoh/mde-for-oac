package com.acme.orders;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for OrderService.
 * Generated from observability model — do not edit by hand.
 */
public final class OrderServiceInstrumentation {

    private static final Tracer COM_ACME_ORDERS_TRACER =
        GlobalOpenTelemetry.get()
            .getTracer("com.acme.orders", "1.3.0");

    public Span startCreateOrder() {
        SpanBuilder builder = COM_ACME_ORDERS_TRACER
            .spanBuilder("CreateOrder")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("http.request.method", "POST");
        builder.setAttribute("http.route", "/orders");
        Span span = builder.startSpan();
        return span;
    }

    public Span startValidateInventory(Context parentContext) {
        SpanBuilder builder = COM_ACME_ORDERS_TRACER
            .spanBuilder("ValidateInventory")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "inventory-service");
        builder.setAttribute("rpc.system", "grpc");
        Span span = builder.startSpan();
        return span;
    }

    public Span startChargePayment(Context parentContext) {
        SpanBuilder builder = COM_ACME_ORDERS_TRACER
            .spanBuilder("ChargePayment")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "payment-gateway");
        Span span = builder.startSpan();
        return span;
    }

    public Span startPersistOrder(Context parentContext) {
        SpanBuilder builder = COM_ACME_ORDERS_TRACER
            .spanBuilder("PersistOrder")
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(parentContext);
        builder.setAttribute("db.system", "postgresql");
        builder.setAttribute("db.operation", "INSERT");
        Span span = builder.startSpan();
        return span;
    }

}