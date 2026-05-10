package com.acme.gateway;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Tracer for the WebGateway. Hand-written counterpart to the
 * framework-generated WebGatewayInstrumentation. Functionally equivalent.
 */
public final class WebGatewayInstrumentation {

    private static final String SCOPE = "com.acme.gateway";
    private static final String SCOPE_VERSION = "2.1.0";

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer(SCOPE, SCOPE_VERSION);

    public Span startHandleRequest() {
        return tracer.spanBuilder("HandleRequest")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.request.method", "GET")
                .setAttribute("http.route", "/api/{path}")
                .startSpan();
    }

    public Span startAuthCheck(Context parent) {
        Span span = builder("AuthCheck", SpanKind.INTERNAL, parent)
                .setAttribute("component", "auth")
                .startSpan();
        span.setStatus(StatusCode.OK);
        return span;
    }

    public Span startRouteToOrders(Context parent) {
        return builder("RouteToOrders", SpanKind.CLIENT, parent)
                .setAttribute("net.peer.name", "order-service")
                .setAttribute("rpc.system", "grpc")
                .startSpan();
    }

    public Span startRouteToCatalog(Context parent) {
        return builder("RouteToCatalog", SpanKind.CLIENT, parent)
                .setAttribute("net.peer.name", "catalog-service")
                .startSpan();
    }

    private SpanBuilder builder(String name, SpanKind kind, Context parent) {
        return tracer.spanBuilder(name).setSpanKind(kind).setParent(parent);
    }
}
