package com.acme.gateway;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for WebGateway.
 * Generated from observability model — do not edit by hand.
 */
public final class WebGatewayInstrumentation {

    private static final Tracer COM_ACME_GATEWAY_TRACER =
        GlobalOpenTelemetry.get()
            .getTracer("com.acme.gateway", "2.1.0");

    public Span startHandleRequest() {
        SpanBuilder builder = COM_ACME_GATEWAY_TRACER
            .spanBuilder("HandleRequest")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("http.request.method", "GET");
        builder.setAttribute("http.route", "/api/{path}");
        Span span = builder.startSpan();
        return span;
    }

    public Span startAuthCheck(Context parentContext) {
        SpanBuilder builder = COM_ACME_GATEWAY_TRACER
            .spanBuilder("AuthCheck")
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(parentContext);
        builder.setAttribute("component", "auth");
        Span span = builder.startSpan();
        span.setStatus(StatusCode.OK);
        return span;
    }

    public Span startRouteToOrders(Context parentContext) {
        SpanBuilder builder = COM_ACME_GATEWAY_TRACER
            .spanBuilder("RouteToOrders")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "order-service");
        builder.setAttribute("rpc.system", "grpc");
        Span span = builder.startSpan();
        return span;
    }

    public Span startRouteToCatalog(Context parentContext) {
        SpanBuilder builder = COM_ACME_GATEWAY_TRACER
            .spanBuilder("RouteToCatalog")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(parentContext);
        builder.setAttribute("net.peer.name", "catalog-service");
        Span span = builder.startSpan();
        return span;
    }

}