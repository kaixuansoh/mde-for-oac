package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static my.edu.um.dissertation.observability.generator.EmfHelpers.*;

/**
 * Mirrors {@code templates/acceleo/metric-registration.mtl}: emits one
 * Java class per Service that constructs every Metric using the OTel
 * Meter API. The OTel instrument type is selected based on the model's
 * {@code MetricType} × {@code ValueType} combination.
 */
public final class MetricRegistrationEmitter implements Emitter {

    private static final Map<String, String> JAVA_TYPE = Map.ofEntries(
            Map.entry("COUNTER|LONG",                       "LongCounter"),
            Map.entry("COUNTER|DOUBLE",                     "DoubleCounter"),
            Map.entry("UP_DOWN_COUNTER|LONG",               "LongUpDownCounter"),
            Map.entry("UP_DOWN_COUNTER|DOUBLE",             "DoubleUpDownCounter"),
            Map.entry("HISTOGRAM|LONG",                     "LongHistogram"),
            Map.entry("HISTOGRAM|DOUBLE",                   "DoubleHistogram"),
            Map.entry("GAUGE|LONG",                         "LongGauge"),
            Map.entry("GAUGE|DOUBLE",                       "DoubleGauge"),
            Map.entry("OBSERVABLE_COUNTER|LONG",            "ObservableLongCounter"),
            Map.entry("OBSERVABLE_COUNTER|DOUBLE",          "ObservableDoubleCounter"),
            Map.entry("OBSERVABLE_UP_DOWN_COUNTER|LONG",    "ObservableLongUpDownCounter"),
            Map.entry("OBSERVABLE_UP_DOWN_COUNTER|DOUBLE",  "ObservableDoubleUpDownCounter"),
            Map.entry("OBSERVABLE_GAUGE|LONG",              "ObservableLongGauge"),
            Map.entry("OBSERVABLE_GAUGE|DOUBLE",            "ObservableDoubleGauge"));

    @Override
    public void emit(EObject model, Path outDir) throws IOException {
        for (EObject service : children(model, "services")) {
            String cls = str(service, "name") + "Metrics";
            Set<String> imports = new TreeSet<>();
            for (EObject scope : children(service, "instrumentations")) {
                for (EObject m : children(scope, "metrics")) {
                    imports.add("io.opentelemetry.api.metrics."
                            + JAVA_TYPE.get(str(m, "type") + "|" + str(m, "valueType")));
                }
            }
            StringBuilder b = new StringBuilder();
            b.append("package ").append(packageName(service)).append(";\n\n");
            for (String imp : imports) b.append("import ").append(imp).append(";\n");
            b.append("import io.opentelemetry.api.metrics.Meter;\n");
            b.append("import io.opentelemetry.api.GlobalOpenTelemetry;\n\n");
            b.append("/**\n * Auto-generated OpenTelemetry metric registry for ")
             .append(str(service, "name")).append(".\n")
             .append(" * Generated from observability model — do not edit by hand.\n */\n");
            b.append("public final class ").append(cls).append(" {\n\n");

            for (EObject scope : children(service, "instrumentations")) {
                b.append("    private static final Meter ")
                 .append(meterField(str(scope, "name"))).append(" =\n")
                 .append("        GlobalOpenTelemetry.get()\n")
                 .append("            .getMeter(\"").append(str(scope, "name")).append("\");\n");
            }
            b.append("\n");

            for (EObject scope : children(service, "instrumentations")) {
                String meter = meterField(str(scope, "name"));
                for (EObject m : children(scope, "metrics")) {
                    String jtype = JAVA_TYPE.get(str(m, "type") + "|" + str(m, "valueType"));
                    String desc = str(m, "description");
                    String agg  = str(m, "aggregation");
                    b.append("    public final ").append(jtype).append(" ")
                     .append(identifier(str(m, "name"))).append(" = ").append(meter).append("\n");
                    StringBuilder line = new StringBuilder("        .");
                    line.append(builderCall(m));
                    line.append(".setUnit(\"").append(str(m, "unit")).append("\")");
                    if (desc != null) line.append(".setDescription(\"").append(desc).append("\")");
                    if ("HISTOGRAM".equals(str(m, "type")) && "EXPLICIT_BUCKET_HISTOGRAM".equals(agg)) {
                        line.append(".setExplicitBucketBoundariesAdvice(java.util.List.of(")
                            .append("5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, ")
                            .append("1000.0, 2500.0, 5000.0, 10000.0))");
                    }
                    line.append(".build();");
                    b.append(line).append("\n\n");
                }
            }
            b.append("}\n");
            Files.writeString(outDir.resolve(cls + ".java"), b.toString());
        }
    }

    private static String meterField(String scopeName) {
        return identifier(scopeName).toUpperCase() + "_METER";
    }

    private static String builderCall(EObject metric) {
        String name = str(metric, "name");
        String t  = str(metric, "type");
        String vt = str(metric, "valueType");
        return switch (t) {
            case "COUNTER"           -> "counterBuilder(\""        + name + "\")"
                    + ("DOUBLE".equals(vt) ? ".ofDoubles()" : "");
            case "UP_DOWN_COUNTER"   -> "upDownCounterBuilder(\""  + name + "\")"
                    + ("DOUBLE".equals(vt) ? ".ofDoubles()" : "");
            case "HISTOGRAM"         -> "histogramBuilder(\""      + name + "\")"
                    + ("LONG".equals(vt)   ? ".ofLongs()"   : "");
            case "GAUGE"             -> "gaugeBuilder(\""          + name + "\")"
                    + ("LONG".equals(vt)   ? ".ofLongs()"   : "");
            default                  -> "counterBuilder(\""        + name + "\")";
        };
    }
}
