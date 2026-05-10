package my.edu.um.dissertation.observability.model;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates an in-memory {@link EObject} model against the structural
 * rules of {@code observability.ecore} plus every OCL invariant
 * declared on the metamodel.
 *
 * <p>The OCL invariants live as {@code EAnnotation} entries on the
 * metamodel — see {@code framework/metamodel/model/observability.ecore}.
 * This class re-implements each invariant in Java so the JVM runtime
 * can execute them without an OCL evaluator on the classpath. The
 * Python prototype at {@code framework/scripts/validate_instance.py}
 * performs the same checks; both must stay in sync with the metamodel.
 */
public final class Validator {

    private static final Set<String> UCUM_UNITS = Set.of(
            "1", "ms", "s", "By", "KBy", "MBy", "GBy",
            "{requests}", "{errors}", "%");
    private static final Set<String> SEMCONV_PREFIXES = Set.of(
            "http.", "db.", "rpc.", "messaging.", "net.",
            "service.", "k8s.", "cloud.");
    private static final Pattern PROM_DURATION = Pattern.compile("^[0-9]+(ms|s|m|h|d)$");
    private static final Pattern PROM_SHORT_DURATION = Pattern.compile("^[0-9]+(ms|s)$");

    private final List<Diagnostic> errors = new ArrayList<>();
    private final List<Diagnostic> warnings = new ArrayList<>();
    private EObject root;

    public ValidationResult validate(EObject model) {
        errors.clear();
        warnings.clear();
        this.root = model;

        // E1 — service names unique
        List<EObject> services = childrenOf(model, "services");
        if (!isUnique(services, s -> stringAttr(s, "name"))) {
            errors.add(Diagnostic.error("E1",
                    "Service names must be unique within the model",
                    pathOf(model)));
        }

        // E12 — model must contain at least one service
        if (services.isEmpty()) {
            errors.add(Diagnostic.error("E12",
                    "ObservabilityModel must contain at least one Service",
                    pathOf(model)));
        }

        Set<EObject> allMetrics = new HashSet<>();
        for (EObject service : services) {
            // W3 — services should target Java per scope
            if (!"JAVA".equals(stringAttr(service, "language"))) {
                warnings.add(Diagnostic.warning("W3",
                        "Service language is " + stringAttr(service, "language")
                                + " (scope is JAVA)",
                        pathOf(service)));
            }
            // W7 — service should emit at least some telemetry
            boolean emits = false;
            for (EObject scope : childrenOf(service, "instrumentations")) {
                if (!childrenOf(scope, "spans").isEmpty()
                        || !childrenOf(scope, "metrics").isEmpty()
                        || !childrenOf(scope, "logs").isEmpty()) {
                    emits = true;
                    break;
                }
            }
            if (!emits) {
                warnings.add(Diagnostic.warning("W7",
                        "Service emits no telemetry",
                        pathOf(service)));
            }

            for (EObject scope : childrenOf(service, "instrumentations")) {
                List<EObject> metrics = childrenOf(scope, "metrics");
                allMetrics.addAll(metrics);
                if (!isUnique(metrics, m -> stringAttr(m, "name"))) {
                    errors.add(Diagnostic.error("E2",
                            "Metric names must be unique within an InstrumentationScope",
                            pathOf(scope)));
                }
                for (EObject metric : metrics) validateMetric(metric);
                for (EObject span : childrenOf(scope, "spans")) validateSpan(span, scope);
                for (EObject log : childrenOf(scope, "logs"))   validateLog(log);
            }

            EObject sampler = childOf(service, "sampler");
            if (sampler != null) validateSampler(sampler);
        }

        // Validate every Attribute reachable from the model (W4)
        for (EObject obj : reachable(model)) {
            if ("Attribute".equals(obj.eClass().getName())) {
                validateAttribute(obj);
            } else if ("AttributeAction".equals(obj.eClass().getName())) {
                validateAttributeAction(obj);
            }
        }

        for (EObject pipeline : childrenOf(model, "pipelines")) {
            validatePipeline(pipeline);
        }

        for (EObject alert : childrenOf(model, "alertRules")) {
            validateAlertRule(alert, allMetrics);
        }

        return new ValidationResult(List.copyOf(errors), List.copyOf(warnings));
    }

    // -------- Per-element validations --------

    private void validateSpan(EObject span, EObject scope) {
        // E4 — parentSpan must belong to the same scope
        Object parent = span.eGet(featureOf(span, "parentSpan"));
        if (parent instanceof EObject parentSpan) {
            if (!Objects.equals(parentSpan.eContainer(), scope)) {
                errors.add(Diagnostic.error("E4",
                        "Span.parentSpan must belong to the same InstrumentationScope",
                        pathOf(span)));
            }
        }
    }

    private void validateMetric(EObject metric) {
        String type = stringAttr(metric, "type");
        String agg  = stringAttr(metric, "aggregation");
        String unit = stringAttr(metric, "unit");

        // E11 — HISTOGRAM aggregation must be DEFAULT or EXPLICIT_BUCKET_HISTOGRAM
        if ("HISTOGRAM".equals(type) && agg != null
                && !"DEFAULT".equals(agg)
                && !"EXPLICIT_BUCKET_HISTOGRAM".equals(agg)) {
            errors.add(Diagnostic.error("E11",
                    "Histogram metric uses aggregation=" + agg
                            + " (must be DEFAULT or EXPLICIT_BUCKET_HISTOGRAM)",
                    pathOf(metric)));
        }

        // W1 — UCUM-compliant common unit
        if (unit != null && !UCUM_UNITS.contains(unit)) {
            warnings.add(Diagnostic.warning("W1",
                    "Metric unit '" + unit + "' is not in the common-UCUM allow-list",
                    pathOf(metric)));
        }

        // W2 — cardinality risk
        if (childrenOf(metric, "attributes").size() > 10) {
            warnings.add(Diagnostic.warning("W2",
                    "Metric has more than 10 attributes (cardinality risk)",
                    pathOf(metric)));
        }
    }

    private void validateLog(EObject log) {
        Integer sn = (Integer) log.eGet(featureOf(log, "severityNumber"));
        if (sn != null && (sn < 1 || sn > 24)) {
            errors.add(Diagnostic.error("E8",
                    "Log severityNumber must be in [1,24] (got " + sn + ")",
                    pathOf(log)));
        }
    }

    private void validateSampler(EObject sampler) {
        String type = stringAttr(sampler, "type");
        Object ratioObj = sampler.eGet(featureOf(sampler, "ratio"));
        if ("TRACE_ID_RATIO_BASED".equals(type) || "PARENT_BASED".equals(type)) {
            if (!(ratioObj instanceof Number n)
                    || n.doubleValue() < 0.0 || n.doubleValue() > 1.0) {
                errors.add(Diagnostic.error("E7",
                        "Sampler.ratio is required and must lie in [0.0, 1.0] for type " + type,
                        pathOf(sampler)));
            }
        }
    }

    private void validatePipeline(EObject pipeline) {
        List<EObject> receivers = childrenOf(pipeline, "receivers");
        List<EObject> exporters = childrenOf(pipeline, "exporters");
        List<EObject> processors = childrenOf(pipeline, "processors");
        String signal = stringAttr(pipeline, "signal");

        // E5 — non-empty endpoints (also enforced structurally by [1..*])
        if (receivers.isEmpty() || exporters.isEmpty()) {
            errors.add(Diagnostic.error("E5",
                    "TelemetryPipeline must have at least one receiver and one exporter",
                    pathOf(pipeline)));
        }

        // E6a / E6b — signal/component compatibility
        if ("TRACES".equals(signal)) {
            for (EObject r : receivers) {
                if ("PrometheusReceiver".equals(r.eClass().getName())) {
                    errors.add(Diagnostic.error("E6a",
                            "TRACES pipeline cannot use a PrometheusReceiver",
                            pathOf(r)));
                }
            }
            for (EObject e : exporters) {
                if ("PrometheusExporter".equals(e.eClass().getName())) {
                    errors.add(Diagnostic.error("E6a",
                            "TRACES pipeline cannot use a PrometheusExporter",
                            pathOf(e)));
                }
            }
        } else if ("METRICS".equals(signal)) {
            for (EObject r : receivers) {
                if ("JaegerReceiver".equals(r.eClass().getName())) {
                    errors.add(Diagnostic.error("E6b",
                            "METRICS pipeline cannot use a JaegerReceiver",
                            pathOf(r)));
                }
            }
            for (EObject e : exporters) {
                if ("JaegerExporter".equals(e.eClass().getName())) {
                    errors.add(Diagnostic.error("E6b",
                            "METRICS pipeline cannot use a JaegerExporter",
                            pathOf(e)));
                }
            }
        }

        // W5 — BatchProcessor recommended
        boolean hasBatch = processors.stream()
                .anyMatch(p -> "BatchProcessor".equals(p.eClass().getName()));
        if (!hasBatch) {
            warnings.add(Diagnostic.warning("W5",
                    "Pipeline has no BatchProcessor (recommended for efficiency)",
                    pathOf(pipeline)));
        }
    }

    private void validateAttribute(EObject attribute) {
        String key = stringAttr(attribute, "key");
        String semconv = stringAttr(attribute, "semanticConvention");
        if (key == null) return;
        for (String prefix : SEMCONV_PREFIXES) {
            if (key.startsWith(prefix) && (semconv == null || semconv.isBlank())) {
                warnings.add(Diagnostic.warning("W4",
                        "Attribute key '" + key + "' looks like a semantic-convention "
                                + "key but no semanticConvention is set",
                        pathOf(attribute)));
                break;
            }
        }
    }

    private void validateAttributeAction(EObject action) {
        String act = stringAttr(action, "action");
        String value = stringAttr(action, "value");
        if (("INSERT".equals(act) || "UPDATE".equals(act) || "UPSERT".equals(act))
                && (value == null || value.isBlank())) {
            errors.add(Diagnostic.error("E10",
                    "AttributeAction with action=" + act + " requires a value",
                    pathOf(action)));
        }
    }

    private void validateAlertRule(EObject alert, Set<EObject> allMetrics) {
        // E3 — referenced metric must be in the model
        Object refObj = alert.eGet(featureOf(alert, "referencedMetric"));
        if (refObj instanceof EObject ref && !allMetrics.contains(ref)) {
            errors.add(Diagnostic.error("E3",
                    "AlertRule references a metric not contained in this model",
                    pathOf(alert)));
        }

        String forDuration = stringAttr(alert, "forDuration");
        if (forDuration != null && !PROM_DURATION.matcher(forDuration).matches()) {
            errors.add(Diagnostic.error("E9",
                    "AlertRule.forDuration must match Prometheus duration syntax (got '"
                            + forDuration + "')",
                    pathOf(alert)));
        }

        // W6 — CRITICAL alerts should not use sub-minute durations
        String severity = stringAttr(alert, "severity");
        if ("CRITICAL".equals(severity) && forDuration != null
                && PROM_SHORT_DURATION.matcher(forDuration).matches()) {
            warnings.add(Diagnostic.warning("W6",
                    "CRITICAL alert uses a sub-minute forDuration (" + forDuration
                            + ") — risks flapping",
                    pathOf(alert)));
        }
    }

    // -------- EMF helpers --------

    private static EReference featureRef(EObject parent, String name) {
        Object f = parent.eClass().getEStructuralFeature(name);
        if (f instanceof EReference r) return r;
        return null;
    }

    private static org.eclipse.emf.ecore.EStructuralFeature featureOf(EObject parent, String name) {
        return parent.eClass().getEStructuralFeature(name);
    }

    @SuppressWarnings("unchecked")
    private static List<EObject> childrenOf(EObject parent, String featureName) {
        EReference ref = featureRef(parent, featureName);
        if (ref == null) return List.of();
        Object value = parent.eGet(ref);
        if (value instanceof EList<?> list) return (List<EObject>) list;
        if (value instanceof EObject eo) return List.of(eo);
        return List.of();
    }

    private static EObject childOf(EObject parent, String featureName) {
        EReference ref = featureRef(parent, featureName);
        if (ref == null) return null;
        Object value = parent.eGet(ref);
        return value instanceof EObject eo ? eo : null;
    }

    private static String stringAttr(EObject parent, String name) {
        Object f = parent.eClass().getEStructuralFeature(name);
        if (f == null) return null;
        Object v = parent.eGet((org.eclipse.emf.ecore.EStructuralFeature) f);
        return v == null ? null : v.toString();
    }

    private static <T> boolean isUnique(List<T> values, java.util.function.Function<T, ?> key) {
        Set<Object> seen = new HashSet<>();
        for (T v : values) {
            Object k = key.apply(v);
            if (k != null && !seen.add(k)) return false;
        }
        return true;
    }

    private static String pathOf(EObject obj) {
        return EcoreUtil.getURI(obj).fragment();
    }

    private static List<EObject> reachable(EObject root) {
        List<EObject> out = new ArrayList<>();
        out.add(root);
        for (var it = root.eAllContents(); it.hasNext(); ) out.add(it.next());
        return out;
    }
}
