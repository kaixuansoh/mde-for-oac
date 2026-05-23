package my.edu.um.dissertation.observability.generator;

import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mirrors the collector-yaml section of {@code tests/test_emitters.py}. */
class CollectorYamlEmitterTest {

    private final CollectorYamlEmitter emitter = new CollectorYamlEmitter();

    @Test
    void pipelinesGetSignalSlugDisambiguation(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="traces" signal="TRACES">
                  <receivers xsi:type="obs:OtlpReceiver" name="otlp" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <processors xsi:type="obs:BatchProcessor" name="batch" timeout="200ms" maxBatchSize="512"/>
                  <exporters xsi:type="obs:OtlpExporter" name="otlp" endpoint="collector:4317"/>
                </pipelines>
                <pipelines name="metrics" signal="METRICS">
                  <receivers xsi:type="obs:OtlpReceiver" name="otlp" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <processors xsi:type="obs:BatchProcessor" name="batch" timeout="200ms" maxBatchSize="512"/>
                  <exporters xsi:type="obs:PrometheusExporter" name="prom" endpoint="0.0.0.0:9464"/>
                </pipelines>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("otlp/traces:"));
        assertTrue(out.contains("otlp/metrics:"));
        assertTrue(out.contains("batch/traces:"));
        assertTrue(out.contains("batch/metrics:"));
        assertTrue(out.contains("prom/metrics:"));
        assertTrue(out.contains("receivers: [otlp/traces]"));
        assertTrue(out.contains("exporters: [prom/metrics]"));
    }

    @Test
    void otlpGrpcReceiverProtocolBlock(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="p" signal="TRACES">
                  <receivers xsi:type="obs:OtlpReceiver" name="otlp" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <exporters xsi:type="obs:OtlpExporter" name="otlp" endpoint="collector:4317"/>
                </pipelines>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("protocols:"));
        assertTrue(out.contains("  grpc:"));
        assertTrue(out.contains("endpoint: 0.0.0.0:4317"));
    }

    @Test
    void attributesProcessorActions(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="p" signal="METRICS">
                  <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <processors xsi:type="obs:AttributesProcessor" name="enrich">
                    <actions action="INSERT" key="cluster" value="prod"/>
                    <actions action="DELETE" key="legacy.key"/>
                  </processors>
                  <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
                </pipelines>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("action: insert"));
        assertTrue(out.contains("value: prod"));
        assertTrue(out.contains("action: delete"));
    }

    @Test
    void tailSamplingProbabilityPercent(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="p" signal="TRACES">
                  <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                  <processors xsi:type="obs:TailSamplingProcessor" name="ts" decisionWait="10s">
                    <policies name="p1" type="PROBABILISTIC" value="0.05"/>
                  </processors>
                  <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
                </pipelines>""");
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("sampling_percentage: 5.0"));
    }

    @Test
    void memoryLimiterProcessor(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, pipelineWith("""
                <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                <processors xsi:type="obs:MemoryLimiterProcessor" name="mem"
                            limitMiB="512" spikeLimitMiB="128"/>
                <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>""", "METRICS"));
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("check_interval: 1s"));
        assertTrue(out.contains("limit_mib: 512"));
        assertTrue(out.contains("spike_limit_mib: 128"));
    }

    @Test
    void prometheusReceiverScrapeConfig(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, pipelineWith("""
                <receivers xsi:type="obs:PrometheusReceiver" name="prom" endpoint="localhost:9090"/>
                <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>""", "METRICS"));
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("scrape_configs:"));
        assertTrue(out.contains("- job_name: 'otel'"));
        assertTrue(out.contains("- targets: ['localhost:9090']"));
    }

    @Test
    void loggingExporterLoglevel(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, pipelineWith("""
                <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                <exporters xsi:type="obs:LoggingExporter" name="log" endpoint="ignored"/>""", "METRICS"));
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("loglevel: info"));
    }

    @Test
    void otlpExporterWithCompressionAndTls(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, pipelineWith("""
                <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
                <exporters xsi:type="obs:OtlpExporter" name="otlp" endpoint="collector:4317" compression="gzip"/>""",
                "METRICS"));
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("compression: gzip"));
        assertTrue(out.contains("insecure: true"));
    }

    @Test
    void jaegerExporterInTracesPipeline(@TempDir Path tmp) throws Exception {
        EObject m = TestSupport.load(tmp, pipelineWith("""
                <receivers xsi:type="obs:JaegerReceiver" name="jr" protocol="GRPC" endpoint="0.0.0.0:14250"/>
                <exporters xsi:type="obs:JaegerExporter" name="jx" endpoint="jaeger:14250"/>""", "TRACES"));
        emitter.emit(m, tmp);
        String out = Files.readString(tmp.resolve("otel-collector.yaml"));
        assertTrue(out.contains("jr/traces:"));
        assertTrue(out.contains("jx/traces:"));
        assertTrue(out.contains("endpoint: jaeger:14250"));
    }

    private static String pipelineWith(String componentXml, String signal) {
        return """
                <services name="S" namespace="com.example" language="JAVA">
                  <instrumentations name="com.example"/>
                </services>
                <pipelines name="p" signal="%s">
                %s
                </pipelines>""".formatted(signal, componentXml);
    }
}
