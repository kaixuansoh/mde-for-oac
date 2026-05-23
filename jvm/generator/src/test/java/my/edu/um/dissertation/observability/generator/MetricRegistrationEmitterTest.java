package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mirrors the metric-emitter section of {@code tests/test_emitters.py}. */
class MetricRegistrationEmitterTest {

    private final MetricRegistrationEmitter emitter = new MetricRegistrationEmitter();

    @Test
    void counterBuilder(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="requests" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertTrue(out.contains("import io.opentelemetry.api.metrics.LongCounter;"));
        assertTrue(out.contains(".counterBuilder(\"requests\")"));
        assertTrue(out.contains(".setUnit(\"1\")"));
    }

    @Test
    void histogramWithExplicitBuckets(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="latency" type="HISTOGRAM" unit="ms"
                             valueType="DOUBLE" aggregation="EXPLICIT_BUCKET_HISTOGRAM"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertTrue(out.contains("DoubleHistogram"));
        assertTrue(out.contains(".setExplicitBucketBoundariesAdvice("));
    }

    @Test
    void defaultHistogramOmitsExplicitBuckets(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="latency" type="HISTOGRAM" unit="ms" valueType="DOUBLE"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertFalse(out.contains(".setExplicitBucketBoundariesAdvice("));
    }

    @Test
    void upDownCounterDouble(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="queue.depth" type="UP_DOWN_COUNTER" unit="1" valueType="DOUBLE"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertTrue(out.contains("DoubleUpDownCounter"));
        assertTrue(out.contains(".upDownCounterBuilder(\"queue.depth\").ofDoubles()"));
    }

    @Test
    void gaugeLong(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="temperature" type="GAUGE" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertTrue(out.contains("LongGauge"));
        assertTrue(out.contains(".gaugeBuilder(\"temperature\").ofLongs()"));
    }

    @Test
    void includesDescription(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="m" type="COUNTER" unit="1" valueType="LONG"
                             description="total widgets minted"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertTrue(out.contains(".setDescription(\"total widgets minted\")"));
    }

    @Test
    void importsAreSortedAndDeduplicated(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="a" type="COUNTER" unit="1" valueType="LONG"/>
                    <metrics name="b" type="COUNTER" unit="1" valueType="LONG"/>
                    <metrics name="c" type="HISTOGRAM" unit="ms" valueType="DOUBLE"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        long longCounterImports = out.lines()
                .filter(l -> l.equals("import io.opentelemetry.api.metrics.LongCounter;"))
                .count();
        assertEquals(1, longCounterImports);
        assertTrue(out.indexOf("DoubleHistogram") < out.indexOf("LongCounter"),
                "imports should be alphabetical (DoubleHistogram before LongCounter)");
    }

    @Test
    void observableDoubleGauge(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="memory" type="OBSERVABLE_GAUGE" unit="By" valueType="DOUBLE"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SMetrics.java"));
        assertTrue(out.contains("ObservableDoubleGauge"));
    }
}
