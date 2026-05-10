package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static my.edu.um.dissertation.observability.generator.EmfHelpers.*;

/**
 * Mirrors {@code templates/acceleo/span-instrumentation.mtl}:
 * one Java class per Service exposing a {@code Span start<Name>(...)}
 * factory method per Span declared in the model.
 */
public final class SpanInstrumentationEmitter implements Emitter {

    @Override
    public void emit(EObject model, Path outDir) throws IOException {
        for (EObject service : children(model, "services")) {
            String cls = str(service, "name") + "Instrumentation";
            StringBuilder b = new StringBuilder();
            b.append("package ").append(packageName(service)).append(";\n\n");
            b.append("import io.opentelemetry.api.OpenTelemetry;\n");
            b.append("import io.opentelemetry.api.common.Attributes;\n");
            b.append("import io.opentelemetry.api.trace.Span;\n");
            b.append("import io.opentelemetry.api.trace.SpanBuilder;\n");
            b.append("import io.opentelemetry.api.trace.SpanKind;\n");
            b.append("import io.opentelemetry.api.trace.StatusCode;\n");
            b.append("import io.opentelemetry.api.trace.Tracer;\n");
            b.append("import io.opentelemetry.context.Context;\n\n");
            b.append("/**\n * Auto-generated OpenTelemetry instrumentation for ")
             .append(str(service, "name")).append(".\n")
             .append(" * Generated from observability model — do not edit by hand.\n */\n");
            b.append("public final class ").append(cls).append(" {\n\n");

            for (EObject scope : children(service, "instrumentations")) {
                String sn = str(scope, "name");
                String sv = str(scope, "version");
                b.append("    private static final Tracer ")
                 .append(tracerField(sn)).append(" =\n")
                 .append("        OpenTelemetry.getGlobalOpenTelemetry()\n")
                 .append("            .getTracer(\"").append(sn).append("\"");
                if (sv != null) b.append(", \"").append(sv).append("\"");
                b.append(");\n");
            }
            b.append("\n");

            for (EObject scope : children(service, "instrumentations")) {
                String tracer = tracerField(str(scope, "name"));
                for (EObject span : children(scope, "spans")) {
                    emitSpanMethod(b, span, tracer);
                }
            }
            b.append("}\n");
            Files.writeString(outDir.resolve(cls + ".java"), b.toString());
        }
    }

    private void emitSpanMethod(StringBuilder b, EObject span, String tracerField) {
        String name = str(span, "name");
        String kind = str(span, "kind");
        String status = str(span, "statusCode");
        boolean hasParent = single(span, "parentSpan") != null;
        String params = hasParent ? "Context parentContext" : "";
        b.append("    public Span start").append(name).append("(").append(params).append(") {\n");
        b.append("        SpanBuilder builder = ").append(tracerField).append("\n");
        b.append("            .spanBuilder(\"").append(name).append("\")\n");
        if (hasParent) {
            b.append("            .setSpanKind(SpanKind.").append(kind).append(")\n");
            b.append("            .setParent(parentContext);\n");
        } else {
            b.append("            .setSpanKind(SpanKind.").append(kind).append(");\n");
        }
        for (EObject attr : children(span, "attributes")) {
            b.append("        builder.setAttribute(\"")
             .append(str(attr, "key")).append("\", ")
             .append(attrJavaLiteral(attr)).append(");\n");
        }
        b.append("        Span span = builder.startSpan();\n");
        for (EObject event : children(span, "events")) {
            List<EObject> eAttrs = children(event, "attributes");
            if (eAttrs.isEmpty()) {
                b.append("        span.addEvent(\"").append(str(event, "name")).append("\");\n");
            } else {
                b.append("        span.addEvent(\"").append(str(event, "name"))
                 .append("\", Attributes.builder()\n");
                for (EObject a : eAttrs) {
                    b.append("            .put(\"").append(str(a, "key")).append("\", ")
                     .append(attrJavaLiteral(a)).append(")\n");
                }
                b.append("            .build());\n");
            }
        }
        if (status != null && !"UNSET".equals(status)) {
            b.append("        span.setStatus(StatusCode.").append(status).append(");\n");
        }
        b.append("        return span;\n");
        b.append("    }\n\n");
    }

    private static String tracerField(String scopeName) {
        return identifier(scopeName).toUpperCase() + "_TRACER";
    }

    static String attrJavaLiteral(EObject attribute) {
        String vt = str(attribute, "valueType");
        String v  = str(attribute, "value");
        if (vt == null) vt = "STRING";
        return switch (vt) {
            case "BOOLEAN" -> v;
            case "LONG"    -> v + "L";
            case "DOUBLE"  -> v + "d";
            default        -> "\"" + v + "\"";
        };
    }
}
