package my.edu.um.dissertation.observability.model;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JVM counterpart of {@code tests/test_negative_corpus.py}: drives every
 * directory under {@code evaluation/corpus/negative/} through the JVM
 * validator and asserts the seeded invariant code appears in the produced
 * diagnostics. This is the test-shaped twin of the ICR metric on the JVM
 * side and keeps the two runtimes honest with each other.
 */
class NegativeCorpusTest {

    private static final Pattern EXPECTED_CODE =
            Pattern.compile("\"code\"\\s*:\\s*\"([^\"]+)\"");

    static Stream<Arguments> cases() throws IOException {
        Path corpus = TestSupport.repoRoot().resolve("evaluation/corpus/negative");
        if (!Files.exists(corpus)) return Stream.empty();
        List<Arguments> out = new ArrayList<>();
        try (var s = Files.list(corpus)) {
            for (Path dir : s.sorted().toList()) {
                Path inst = dir.resolve("instance.observability");
                Path exp  = dir.resolve("expected.json");
                if (Files.exists(inst) && Files.exists(exp)) {
                    out.add(Arguments.of(dir.getFileName().toString(), inst, exp));
                }
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void seededInvariantCaught(String name, Path instance, Path expectedJson) throws Exception {
        String json = Files.readString(expectedJson);
        Matcher m = EXPECTED_CODE.matcher(json);
        assertTrue(m.find(), "expected.json missing 'code' field at " + expectedJson);
        String code = m.group(1);

        EObject model;
        try {
            model = TestSupport.runtime().loadInstance(instance);
        } catch (RuntimeException ex) {
            // The EMF XMI loader is stricter than the Python validator: it
            // throws UnresolvedReferenceException when an alertRule.referencedMetric
            // (E3) points at a non-existent metric, instead of returning a proxy.
            // For E3 that means the pipeline still rejects the bad model — just
            // one layer earlier than the Validator. We accept that as the seeded
            // invariant being caught; for any other code, re-throw because the
            // test fixture itself is broken.
            String cause = rootCauseMessage(ex);
            if ("E3".equals(code) && cause.contains("Unresolved reference")) return;
            throw ex;
        }

        ValidationResult r = new Validator().validate(model);
        List<Diagnostic> bucket = code.startsWith("W") ? r.warnings() : r.errors();
        assertTrue(bucket.stream().anyMatch(d -> d.code().equals(code)),
                () -> "expected code " + code + " in "
                        + (code.startsWith("W") ? "warnings" : "errors")
                        + "; got errors=" + r.errors() + " warnings=" + r.warnings());
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() == null ? "" : cur.getMessage();
    }
}
