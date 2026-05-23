package my.edu.um.dissertation.observability.model;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JVM counterpart of {@code tests/test_validator.py} +
 * {@code tests/test_coverage.py} validator section. Exercises each OCL
 * invariant on a minimal fixture and asserts the diagnostic code surfaces
 * (or doesn't) on the {@link ValidationResult}.
 */
class ValidatorTest {

    private final Validator validator = new Validator();

    // ----- Shipped positive examples: must validate clean -----
    static Stream<Path> shippedExamples() throws IOException {
        Path examples = TestSupport.repoRoot().resolve("examples");
        try (var s = Files.list(examples)) {
            return s.filter(p -> p.toString().endsWith(".observability")).sorted().toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("shippedExamples")
    @DisplayName("shipped examples validate with no errors and no warnings")
    void shippedExamplesValidateClean(Path example) {
        EObject model = TestSupport.runtime().loadInstance(example);
        ValidationResult r = validator.validate(model);
        assertFalse(r.hasErrors(), () -> "unexpected errors in " + example.getFileName() + ": " + r.errors());
        assertTrue(r.warnings().isEmpty(),
                () -> "unexpected warnings in " + example.getFileName() + ": " + r.warnings());
    }

    // ----- E1: duplicate service names -----
    @Test
    void e1_duplicateServiceNames_rejected(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="Dup" namespace="com.example.a" language="JAVA">
                  <instrumentations name="com.example.a"/>
                </services>
                <services name="Dup" namespace="com.example.b" language="JAVA">
                  <instrumentations name="com.example.b"/>
                </services>""");
        assertHasCode(validator.validate(m).errors(), "E1");
    }

    // ----- E2: duplicate metric names within scope -----
    @Test
    void e2_duplicateMetricNamesInScope_rejected(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="dup" type="COUNTER" unit="1" valueType="LONG"/>
                    <metrics name="dup" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>""");
        assertHasCode(validator.validate(m).errors(), "E2");
    }

    @Test
    void e2_sameMetricNameAcrossScopes_allowed(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example.a">
                    <metrics name="requests" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                  <instrumentations name="com.example.b">
                    <metrics name="requests" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>""");
        assertLacksCode(validator.validate(m).errors(), "E2");
    }

    // ----- E3: alert references metric not in model -----
    @Test
    void e3_alertReferencingExistingMetric_clean(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                            severity="WARNING" forDuration="5m"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>""");
        assertLacksCode(validator.validate(m).errors(), "E3");
    }

    // ----- E7: sampler ratio boundaries -----
    @ParameterizedTest
    @ValueSource(strings = {"0.0", "0.5", "1.0"})
    void e7_sampler_validRatios(String ratio, @TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, samplerModel(ratio));
        assertLacksCode(validator.validate(m).errors(), "E7");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "1.001"})
    void e7_sampler_invalidRatios(String ratio, @TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, samplerModel(ratio));
        assertHasCode(validator.validate(m).errors(), "E7");
    }

    private static String samplerModel(String ratio) {
        return """
                <services name="S" namespace="com.example" language="JAVA">
                  <sampler type="TRACE_ID_RATIO_BASED" ratio="%s"/>
                  <instrumentations name="com.example">
                    <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""".formatted(ratio);
    }

    // ----- E8: log severity range -----
    @ParameterizedTest
    @ValueSource(ints = {1, 9, 17, 24})
    void e8_logSeverityInRange(int sev, @TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, logModel(sev));
        assertLacksCode(validator.validate(m).errors(), "E8");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 25})
    void e8_logSeverityOutOfRange(int sev, @TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, logModel(sev));
        assertHasCode(validator.validate(m).errors(), "E8");
    }

    private static String logModel(int sev) {
        return """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="hello" severityNumber="%d" severityText="INFO"/>
                  </instrumentations>
                </services>""".formatted(sev);
    }

    // ----- E9: forDuration grammar -----
    @ParameterizedTest
    @ValueSource(strings = {"100ms", "30s", "5m", "2h", "1d"})
    void e9_forDuration_validUnits(String dur, @TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, alertModel(dur, "WARNING"));
        assertLacksCode(validator.validate(m).errors(), "E9");
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "5x", "5min", "forever"})
    void e9_forDuration_invalid(String dur, @TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, alertModel(dur, "WARNING"));
        assertHasCode(validator.validate(m).errors(), "E9");
    }

    @Test
    void w6_onlyFiresForCriticalShortDuration(@TempDir Path tmp) throws Exception {
        EObject ok = TestSupport.load(tmp, alertModel("30s", "WARNING"));
        assertLacksCode(validator.validate(ok).warnings(), "W6");
        EObject bad = TestSupport.load(tmp, alertModel("30s", "CRITICAL"));
        assertHasCode(validator.validate(bad).warnings(), "W6");
    }

    private static String alertModel(String forDuration, String severity) {
        return """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="m" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="A" expression="rate(m[1m]) &gt; 0"
                            severity="%s" forDuration="%s"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>"""
                .formatted(severity, forDuration);
    }

    // ----- E10: AttributeAction value required for INSERT/UPDATE/UPSERT -----
    @Test
    void e10_insertActionRequiresValue(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, attrProcessorModel("INSERT", null));
        assertHasCode(validator.validate(m).errors(), "E10");
    }

    @Test
    void e10_deleteActionDoesNotRequireValue(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, attrProcessorModel("DELETE", null));
        assertLacksCode(validator.validate(m).errors(), "E10");
    }

    private static String attrProcessorModel(String action, String value) {
        String valueAttr = value == null ? "" : " value=\"" + value + "\"";
        return """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="p" signal="METRICS">
                  <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <processors xsi:type="obs:AttributesProcessor" name="enrich">
                    <actions action="%s" key="cluster"%s/>
                  </processors>
                  <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
                </pipelines>""".formatted(action, valueAttr);
    }

    // ----- E11: histogram aggregation -----
    @Test
    void e11_histogramDefaultAggregationAccepted(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="m" type="HISTOGRAM" unit="ms" valueType="DOUBLE" aggregation="DEFAULT"/>
                  </instrumentations>
                </services>""");
        assertLacksCode(validator.validate(m).errors(), "E11");
    }

    // ----- E12: model must contain at least one service -----
    @Test
    void e12_modelWithoutServicesRejected(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, "");
        assertHasCode(validator.validate(m).errors(), "E12");
    }

    @Test
    void e12_modelWithServicesAccepted(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        assertLacksCode(validator.validate(m).errors(), "E12");
    }

    // ----- Warning-tier invariants -----
    @Test
    void w1_silentForUcumUnit(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="m" type="COUNTER" unit="ms" valueType="LONG"/>
                  </instrumentations>
                </services>""");
        assertLacksCode(validator.validate(m).warnings(), "W1");
    }

    @Test
    void w3_nonJavaLanguageWarns(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="GO">
                  <instrumentations name="com.example">
                    <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        assertHasCode(validator.validate(m).warnings(), "W3");
    }

    @Test
    void w4_silentWhenSemanticConventionPresent(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="op" kind="INTERNAL" statusCode="UNSET">
                      <attributes key="http.request.method"
                                  valueType="STRING" value="GET"
                                  semanticConvention="http.request.method"/>
                    </spans>
                  </instrumentations>
                </services>""");
        assertLacksCode(validator.validate(m).warnings(), "W4");
    }

    @Test
    void w5_pipelineWithoutBatchProcessorWarns(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="p" signal="METRICS">
                  <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
                </pipelines>""");
        assertHasCode(validator.validate(m).warnings(), "W5");
    }

    @Test
    void w7_serviceEmittingNothingWarns(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="Empty" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>""");
        assertHasCode(validator.validate(m).warnings(), "W7");
    }

    // -------- Helpers --------
    private static void assertHasCode(List<Diagnostic> diags, String code) {
        assertTrue(diags.stream().anyMatch(d -> d.code().equals(code)),
                () -> "expected diagnostic " + code + " in " + diags);
    }

    private static void assertLacksCode(List<Diagnostic> diags, String code) {
        assertFalse(diags.stream().anyMatch(d -> d.code().equals(code)),
                () -> "unexpected diagnostic " + code + " in " + diags);
    }
}
