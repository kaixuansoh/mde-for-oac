package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mirrors the log-emitter section of {@code tests/test_emitters.py}. */
class LogInstrumentationEmitterTest {

    private final LogInstrumentationEmitter emitter = new LogInstrumentationEmitter();

    @Test
    void skipsServicesWithoutLogs(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="Silent" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        assertFalse(Files.exists(tmp.resolve("SilentLogs.java")));
    }

    @Test
    void usesSeverityEnumAndBody(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="oops" severityNumber="17" severityText="ERROR"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertTrue(out.contains(".setSeverity(Severity.ERROR)"));
        assertTrue(out.contains(".setSeverityText(\"ERROR\")"));
        assertTrue(out.contains(".setBody(\"oops\")"));
    }

    @Test
    void escapesBodyQuotes(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="said &quot;hi&quot;" severityNumber="9"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertTrue(out.contains(".setBody(\"said \\\"hi\\\"\")"));
    }

    @Test
    void attributesEmittedWithTypedKeys(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="user signed in" severityNumber="9" severityText="INFO">
                      <attributes key="user.id" valueType="LONG" value="42"/>
                      <attributes key="user.region" valueType="STRING" value="apac"/>
                    </logs>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertTrue(out.contains(".setAttribute(AttributeKey.longKey(\"user.id\"), 42L)"));
        assertTrue(out.contains(".setAttribute(AttributeKey.stringKey(\"user.region\"), \"apac\")"));
    }

    @Test
    void emitsLoggerPerScope(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example.a">
                    <logs body="a" severityNumber="9"/>
                  </instrumentations>
                  <instrumentations name="com.example.b">
                    <logs body="b" severityNumber="9"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertTrue(out.contains(".get(\"com.example.a\")"));
        assertTrue(out.contains(".get(\"com.example.b\")"));
    }

    @Test
    void omitsSeverityTextWhenAbsent(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="x" severityNumber="9"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertFalse(out.contains(".setSeverityText("));
    }

    @Test
    void subTierSeverityEnumLiteral(@TempDir Path tmp) throws Exception {
        // severityNumber=10 should map to INFO2 (INFO+1 sub-tier).
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="x" severityNumber="10"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertTrue(out.contains(".setSeverity(Severity.INFO2)"));
    }

    @Test
    void methodNameCamelCasesBody(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <logs body="order #42: completed!" severityNumber="9"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SLogs.java"));
        assertTrue(out.contains("public void logOrder42Completed()"));
    }
}
