package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirror of {@code tests/test_emitters.py} + {@code test_coverage.py} for
 * {@link SpanInstrumentationEmitter}. The emitter writes one Java class
 * per Service; tests build minimal in-memory models and assert on the
 * observable shape of the output (method names, status calls, parent
 * context, package) rather than byte-exact equality so they survive
 * cosmetic refactors.
 */
class SpanInstrumentationEmitterTest {

    private final SpanInstrumentationEmitter emitter = new SpanInstrumentationEmitter();

    @Test
    void writesPerServiceClassWithMethods(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="Alpha" namespace="com.acme.alpha" language="JAVA">
                  <instrumentations name="com.acme.alpha">
                    <spans name="DoWork" kind="SERVER" statusCode="OK"/>
                  </instrumentations>
                </services>
                <services name="Beta" namespace="com.acme.beta" language="JAVA">
                  <instrumentations name="com.acme.beta">
                    <spans name="Run" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String alpha = Files.readString(tmp.resolve("AlphaInstrumentation.java"));
        String beta = Files.readString(tmp.resolve("BetaInstrumentation.java"));
        assertTrue(alpha.contains("public Span startDoWork()"));
        assertTrue(alpha.contains("package com.acme.alpha;"));
        assertTrue(beta.contains("public Span startRun()"));
        assertTrue(beta.contains("package com.acme.beta;"));
    }

    @Test
    void parentSpanGetsContextParameter(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="Root" kind="SERVER" statusCode="UNSET"/>
                    <spans name="Child" kind="INTERNAL" statusCode="UNSET"
                           parentSpan="//@services.0/@instrumentations.0/@spans.0"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SInstrumentation.java"));
        assertTrue(out.contains("public Span startRoot()"));
        assertTrue(out.contains("public Span startChild(Context parentContext)"));
        assertTrue(out.contains(".setParent(parentContext)"));
    }

    @Test
    void emitsGlobalOpenTelemetryTracerLookup(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="X" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SInstrumentation.java"));
        assertTrue(out.contains("GlobalOpenTelemetry.get()"));
        assertTrue(out.contains(".getTracer(\"com.example\""));
    }

    @Test
    void setsExplicitStatusWhenNotUnset(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="Op" kind="SERVER" statusCode="OK"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SInstrumentation.java"));
        assertTrue(out.contains("span.setStatus(StatusCode.OK);"));
    }

    @Test
    void omitsStatusWhenUnset(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="Op" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SInstrumentation.java"));
        assertFalse(out.contains("setStatus"));
    }

    @Test
    void emitsAttributesAndEvents(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example">
                    <spans name="Op" kind="SERVER" statusCode="UNSET">
                      <attributes key="http.method" valueType="STRING" value="GET"
                                  semanticConvention="http.request.method"/>
                      <events name="cache.hit">
                        <attributes key="cache.size" valueType="LONG" value="128"/>
                      </events>
                    </spans>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SInstrumentation.java"));
        assertTrue(out.contains("builder.setAttribute(\"http.method\", \"GET\");"));
        assertTrue(out.contains("span.addEvent(\"cache.hit\", Attributes.builder()"));
        assertTrue(out.contains(".put(\"cache.size\", 128L)"));
    }

    @Test
    void defaultPackageWhenNamespaceMissing(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" language="JAVA">
                  <instrumentations name="scope">
                    <spans name="Op" kind="INTERNAL" statusCode="UNSET"/>
                  </instrumentations>
                </services>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("SInstrumentation.java"));
        assertTrue(out.contains("package com.acme.observability;"));
    }
}
