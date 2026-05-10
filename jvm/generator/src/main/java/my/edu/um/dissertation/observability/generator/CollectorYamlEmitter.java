package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static my.edu.um.dissertation.observability.generator.EmfHelpers.*;

/**
 * Mirrors {@code templates/acceleo/collector-yaml.mtl}: emits a single
 * {@code otel-collector.yaml} where each TelemetryPipeline becomes a
 * {@code service.pipelines.<signal>} block. Components are aliased per
 * pipeline (e.g. {@code otlp/traces}) so names never collide.
 */
public final class CollectorYamlEmitter implements Emitter {

    private static final Map<String, String> SIGNAL_SLUG = Map.of(
            "TRACES", "traces", "METRICS", "metrics", "LOGS", "logs");

    @Override
    public void emit(EObject model, Path outDir) throws IOException {
        StringBuilder L = new StringBuilder();
        L.append("# Auto-generated OpenTelemetry Collector configuration\n");
        L.append("# Generated from: ").append(str(model, "name"))
         .append(" v").append(str(model, "version")).append("\n\n");

        L.append("receivers:\n");
        for (EObject p : children(model, "pipelines")) {
            String slug = SIGNAL_SLUG.get(str(p, "signal"));
            for (EObject r : children(p, "receivers")) {
                L.append("  ").append(str(r, "name")).append("/").append(slug).append(":\n");
                L.append(receiverBody(r));
            }
        }
        L.append("\n");

        L.append("processors:\n");
        for (EObject p : children(model, "pipelines")) {
            String slug = SIGNAL_SLUG.get(str(p, "signal"));
            for (EObject pr : children(p, "processors")) {
                L.append("  ").append(str(pr, "name")).append("/").append(slug).append(":\n");
                L.append(processorBody(pr));
            }
        }
        L.append("\n");

        L.append("exporters:\n");
        for (EObject p : children(model, "pipelines")) {
            String slug = SIGNAL_SLUG.get(str(p, "signal"));
            for (EObject e : children(p, "exporters")) {
                L.append("  ").append(str(e, "name")).append("/").append(slug).append(":\n");
                L.append(exporterBody(e));
            }
        }
        L.append("\n");

        L.append("service:\n  pipelines:\n");
        for (EObject p : children(model, "pipelines")) {
            String slug = SIGNAL_SLUG.get(str(p, "signal"));
            String rs = children(p, "receivers").stream()
                    .map(r -> str(r, "name") + "/" + slug)
                    .collect(Collectors.joining(", "));
            String ps = children(p, "processors").stream()
                    .map(pr -> str(pr, "name") + "/" + slug)
                    .collect(Collectors.joining(", "));
            String es = children(p, "exporters").stream()
                    .map(e -> str(e, "name") + "/" + slug)
                    .collect(Collectors.joining(", "));
            L.append("    ").append(slug).append(":\n");
            L.append("      receivers: [").append(rs).append("]\n");
            L.append("      processors: [").append(ps).append("]\n");
            L.append("      exporters: [").append(es).append("]\n");
        }
        Files.writeString(outDir.resolve("otel-collector.yaml"), L.toString());
    }

    private String receiverBody(EObject r) {
        String typ = typeName(r);
        String ep = str(r, "endpoint");
        String proto = str(r, "protocol");
        if ("OtlpReceiver".equals(typ) || "JaegerReceiver".equals(typ)) {
            String key = "GRPC".equals(proto) ? "grpc" : "http";
            return "    protocols:\n      " + key + ":\n        endpoint: " + ep + "\n";
        }
        if ("PrometheusReceiver".equals(typ)) {
            return  "    config:\n"
                  + "      scrape_configs:\n"
                  + "        - job_name: 'otel'\n"
                  + "          static_configs:\n"
                  + "            - targets: ['" + ep + "']\n";
        }
        return "    endpoint: " + ep + "\n";
    }

    private String processorBody(EObject p) {
        String typ = typeName(p);
        return switch (typ) {
            case "BatchProcessor" -> "    timeout: " + str(p, "timeout")
                    + "\n    send_batch_size: " + str(p, "maxBatchSize") + "\n";
            case "MemoryLimiterProcessor" -> "    check_interval: 1s\n"
                    + "    limit_mib: " + str(p, "limitMiB") + "\n"
                    + "    spike_limit_mib: " + str(p, "spikeLimitMiB") + "\n";
            case "AttributesProcessor" -> attributesProcessorBody(p);
            case "TailSamplingProcessor" -> tailSamplingBody(p);
            default -> "    {}\n";
        };
    }

    private String attributesProcessorBody(EObject ap) {
        StringBuilder b = new StringBuilder("    actions:\n");
        for (EObject a : children(ap, "actions")) {
            b.append("      - key: ").append(str(a, "key")).append("\n");
            b.append("        action: ").append(str(a, "action").toLowerCase()).append("\n");
            String v = str(a, "value");
            if (v != null) b.append("        value: ").append(v).append("\n");
        }
        return b.toString();
    }

    private String tailSamplingBody(EObject tsp) {
        StringBuilder b = new StringBuilder();
        b.append("    decision_wait: ").append(str(tsp, "decisionWait")).append("\n");
        b.append("    policies:\n");
        for (EObject pol : children(tsp, "policies")) {
            String t = str(pol, "type");
            b.append("      - name: ").append(str(pol, "name")).append("\n");
            b.append("        type: ").append(t.toLowerCase()).append("\n");
            String v = str(pol, "value");
            if (v == null) continue;
            switch (t) {
                case "PROBABILISTIC" ->
                    b.append("        probabilistic:\n          sampling_percentage: ")
                     .append(Double.parseDouble(v) * 100.0).append("\n");
                case "STATUS_CODE" ->
                    b.append("        status_code:\n          status_codes: [")
                     .append(v).append("]\n");
                case "STRING_ATTRIBUTE" ->
                    b.append("        string_attribute:\n          values: [")
                     .append(v).append("]\n");
                default -> { /* no extra body */ }
            }
        }
        return b.toString();
    }

    private String exporterBody(EObject e) {
        String typ = typeName(e);
        String ep = str(e, "endpoint");
        String comp = str(e, "compression");
        return switch (typ) {
            case "OtlpExporter" -> {
                StringBuilder b = new StringBuilder("    endpoint: " + ep + "\n");
                if (comp != null) b.append("    compression: ").append(comp).append("\n");
                b.append("    tls:\n      insecure: true\n");
                yield b.toString();
            }
            case "PrometheusExporter" -> "    endpoint: " + ep + "\n";
            case "JaegerExporter" -> "    endpoint: " + ep + "\n    tls:\n      insecure: true\n";
            case "LoggingExporter" -> "    loglevel: info\n";
            default -> "    endpoint: " + ep + "\n";
        };
    }
}
