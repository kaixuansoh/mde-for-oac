package com.acme.banking.auth;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Auto-generated OpenTelemetry instrumentation for AuthService.
 * Generated from observability model — do not edit by hand.
 */
public final class AuthServiceInstrumentation {

    private static final Tracer COM_ACME_BANKING_AUTH_TRACER =
        GlobalOpenTelemetry.get()
            .getTracer("com.acme.banking.auth", "3.2.0");

    public Span startLogin() {
        SpanBuilder builder = COM_ACME_BANKING_AUTH_TRACER
            .spanBuilder("Login")
            .setSpanKind(SpanKind.SERVER);
        builder.setAttribute("http.request.method", "POST");
        builder.setAttribute("http.route", "/auth/login");
        Span span = builder.startSpan();
        return span;
    }

    public Span startVerifyMFA(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_AUTH_TRACER
            .spanBuilder("VerifyMFA")
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(parentContext);
        builder.setAttribute("component", "mfa");
        Span span = builder.startSpan();
        return span;
    }

    public Span startIssueToken(Context parentContext) {
        SpanBuilder builder = COM_ACME_BANKING_AUTH_TRACER
            .spanBuilder("IssueToken")
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(parentContext);
        builder.setAttribute("component", "token");
        Span span = builder.startSpan();
        span.setStatus(StatusCode.OK);
        return span;
    }

}