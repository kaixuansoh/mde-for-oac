package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static my.edu.um.dissertation.observability.generator.EmfHelpers.*;

/**
 * Mirrors {@code templates/acceleo/log-instrumentation.mtl}: emits a
 * {@code <Service>Logs.java} class exposing a structured-logging method
 * per {@code Log} entity declared in any of the service's
 * {@code InstrumentationScope}s. Severity numbers (1..24) are mapped to
 * the OTel Java {@code Severity} enum literals.
 */
public final class LogInstrumentationEmitter implements Emitter {

    private static final List<String> SEVERITY_BASES = List.of(
            "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL");

    private static final Map<String, String> ATTR_KEY_TYPE = Map.of(
            "STRING",  "string",
            "BOOLEAN", "boolean",
            "LONG",    "long",
            "DOUBLE",  "double");

    @Override
    public void emit(EObject model, Path outDir) throws IOException {
        for (EObject service : children(model, "services")) {
            // Skip services that declare no logs
            boolean anyLog = children(service, "instrumentations").stream()
                    .anyMatch(s -> !children(s, "logs").isEmpty());
            if (!anyLog) continue;

            String cls = str(service, "name") + "Logs";
            StringBuilder b = new StringBuilder();
            b.append("package ").append(packageName(service)).append(";\n\n");
            b.append("import io.opentelemetry.api.GlobalOpenTelemetry;\n");
            b.append("import io.opentelemetry.api.common.AttributeKey;\n");
            b.append("import io.opentelemetry.api.logs.Logger;\n");
            b.append("import io.opentelemetry.api.logs.Severity;\n\n");
            b.append("/**\n * Auto-generated OpenTelemetry log emitters for ")
             .append(str(service, "name")).append(".\n")
             .append(" * Generated from observability model — do not edit by hand.\n */\n");
            b.append("public final class ").append(cls).append(" {\n\n");

            for (EObject scope : children(service, "instrumentations")) {
                b.append("    private static final Logger ")
                 .append(loggerField(str(scope, "name"))).append(" =\n")
                 .append("        GlobalOpenTelemetry.get()\n")
                 .append("            .getLogsBridge()\n")
                 .append("            .get(\"").append(str(scope, "name")).append("\");\n");
            }
            b.append("\n");

            for (EObject scope : children(service, "instrumentations")) {
                String loggerField = loggerField(str(scope, "name"));
                for (EObject log : children(scope, "logs")) emitLogMethod(b, log, loggerField);
            }
            b.append("}\n");
            Files.writeString(outDir.resolve(cls + ".java"), b.toString());
        }
    }

    private void emitLogMethod(StringBuilder b, EObject log, String loggerField) {
        String body = str(log, "body");
        String method = methodName(body);
        String sev = severityEnum(intValue(log, "severityNumber"));
        String sevText = str(log, "severityText");
        b.append("    public void ").append(method).append("() {\n");
        b.append("        ").append(loggerField).append(".logRecordBuilder()\n");
        b.append("            .setSeverity(Severity.").append(sev).append(")\n");
        if (sevText != null) {
            b.append("            .setSeverityText(\"").append(sevText).append("\")\n");
        }
        String escapedBody = body.replace("\"", "\\\"");
        b.append("            .setBody(\"").append(escapedBody).append("\")\n");
        for (EObject a : children(log, "attributes")) {
            String kt = ATTR_KEY_TYPE.getOrDefault(str(a, "valueType"), "string");
            b.append("            .setAttribute(AttributeKey.").append(kt).append("Key(\"")
             .append(str(a, "key")).append("\"), ")
             .append(SpanInstrumentationEmitter.attrJavaLiteral(a)).append(")\n");
        }
        b.append("            .emit();\n");
        b.append("    }\n\n");
    }

    private static String loggerField(String scopeName) {
        return identifier(scopeName).toUpperCase() + "_LOGGER";
    }

    private static String severityEnum(int n) {
        String base = SEVERITY_BASES.get((n - 1) / 4);
        int sub = (n - 1) % 4;
        return sub == 0 ? base : base + (sub + 1);
    }

    private static String methodName(String body) {
        if (body == null) return "log";
        StringBuilder out = new StringBuilder("log");
        for (String word : body.split("[^A-Za-z0-9]+")) {
            if (word.isEmpty()) continue;
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase());
        }
        return out.toString();
    }
}
