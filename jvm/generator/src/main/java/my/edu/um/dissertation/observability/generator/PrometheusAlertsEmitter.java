package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static my.edu.um.dissertation.observability.generator.EmfHelpers.*;

/**
 * Mirrors {@code templates/acceleo/prometheus-alerts.mtl}: emits a
 * single Prometheus rule-group YAML where each {@code AlertRule}
 * becomes one rule.
 */
public final class PrometheusAlertsEmitter implements Emitter {

    @Override
    public void emit(EObject model, Path outDir) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("# Auto-generated Prometheus alerting rules\n");
        b.append("# Generated from: ").append(str(model, "name"))
         .append(" v").append(str(model, "version")).append("\n\n");
        b.append("groups:\n");
        b.append("  - name: ").append(str(model, "name")).append("\n");
        b.append("    rules:\n");

        for (EObject ar : children(model, "alertRules")) {
            String expr = str(ar, "expression").replace("\"", "\\\"");
            b.append("      - alert: ").append(str(ar, "name")).append("\n");
            b.append("        expr: \"").append(expr).append("\"\n");
            b.append("        for: ").append(str(ar, "forDuration")).append("\n");
            b.append("        labels:\n");
            b.append("          severity: ").append(str(ar, "severity").toLowerCase()).append("\n");
            for (EObject l : children(ar, "labels")) {
                b.append("          ").append(str(l, "key"))
                 .append(": \"").append(str(l, "value")).append("\"\n");
            }
            List<EObject> annotations = children(ar, "annotations");
            String desc = str(ar, "description");
            if (!annotations.isEmpty() || desc != null) {
                b.append("        annotations:\n");
                if (desc != null) b.append("          description: \"").append(desc).append("\"\n");
                for (EObject a : annotations) {
                    b.append("          ").append(str(a, "key"))
                     .append(": \"").append(str(a, "value")).append("\"\n");
                }
            }
        }
        Files.writeString(outDir.resolve("prometheus-alerts.yaml"), b.toString());
    }
}
