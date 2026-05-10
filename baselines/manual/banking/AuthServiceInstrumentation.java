package com.acme.banking.auth;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

/**
 * Tracer for the AuthService.
 *
 * Hand-written counterpart to the framework-generated
 * AuthServiceInstrumentation. Functionally equivalent — same span names,
 * kinds, attributes, and parent-child links.
 */
public final class AuthServiceInstrumentation {

    private static final String SCOPE = "com.acme.banking.auth";
    private static final String SCOPE_VERSION = "3.2.0";

    private final Tracer tracer = OpenTelemetry.getGlobalOpenTelemetry()
            .getTracer(SCOPE, SCOPE_VERSION);

    public Span startLogin() {
        return tracer.spanBuilder("Login")
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.request.method", "POST")
                .setAttribute("http.route", "/auth/login")
                .startSpan();
    }

    public Span startVerifyMFA(Context parent) {
        return tracer.spanBuilder("VerifyMFA")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parent)
                .setAttribute("component", "mfa")
                .startSpan();
    }

    public Span startIssueToken(Context parent) {
        Span span = tracer.spanBuilder("IssueToken")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parent)
                .setAttribute("component", "token")
                .startSpan();
        span.setStatus(StatusCode.OK);
        return span;
    }
}
