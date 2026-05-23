package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mirrors the prometheus section of {@code tests/test_emitters.py}. */
class PrometheusAlertsEmitterTest {

    private final PrometheusAlertsEmitter emitter = new PrometheusAlertsEmitter();

    @Test
    void basicShape(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="HighErrors"
                            expression="rate(errors[1m]) &gt; 0"
                            severity="WARNING"
                            forDuration="5m"
                            description="Error rate elevated"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("prometheus-alerts.yaml"));
        assertTrue(out.contains("- alert: HighErrors"));
        assertTrue(out.contains("for: 5m"));
        assertTrue(out.contains("severity: warning"));
        assertTrue(out.contains("description: \"Error rate elevated\""));
    }

    @Test
    void expressionEscapesInnerQuotes(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="up" type="GAUGE" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="JobMissing"
                            expression='absent(up{job=&quot;x&quot;}) &gt; 0'
                            severity="WARNING"
                            forDuration="1m"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("prometheus-alerts.yaml"));
        assertTrue(out.contains("expr: \"absent(up{job=\\\"x\\\"}) > 0\""));
    }

    @Test
    void includesLabels(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                            severity="WARNING" forDuration="5m"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0">
                  <labels key="team" value="platform"/>
                </alertRules>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("prometheus-alerts.yaml"));
        assertTrue(out.contains("severity: warning"));
        assertTrue(out.contains("team: \"platform\""));
    }

    @Test
    void emitsAnnotations(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                            severity="CRITICAL" forDuration="10m"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0">
                  <annotations key="runbook" value="https://runbooks/errors"/>
                </alertRules>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("prometheus-alerts.yaml"));
        assertTrue(out.contains("annotations:"));
        assertTrue(out.contains("runbook: \"https://runbooks/errors\""));
        assertTrue(out.contains("severity: critical"));
    }

    @Test
    void groupUsesModelName(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
                  </instrumentations>
                </services>
                <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                            severity="WARNING" forDuration="5m"
                            referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>""",
                "banking");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("prometheus-alerts.yaml"));
        assertTrue(out.contains("- name: banking"));
    }
}
