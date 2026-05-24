"""Unit tests for the five emitters in scripts/generate.py.

The emitters take a parsed model root and an output directory; tests build
minimal models in memory, run the emitter into a tmp_path, and assert on the
artifact text. Assertions focus on observable output shape (method names,
YAML keys, severity enum names) rather than literal byte equality, so they
survive cosmetic refactors."""
from __future__ import annotations
import pathlib

import pytest

from generate import (
    _severity_enum, _log_method_name, _attr_java_literal,
    gen_span_instrumentation, gen_metric_registration,
    gen_collector_yaml, gen_prometheus_alerts, gen_log_instrumentation,
)
from _helpers import build_model


# -----------------------------------------------------------------------
# Pure helpers
# -----------------------------------------------------------------------
@pytest.mark.parametrize('sev, expected', [
    (1, 'TRACE'), (4, 'TRACE4'),
    (5, 'DEBUG'), (9, 'INFO'),
    (13, 'WARN'), (17, 'ERROR'),
    (21, 'FATAL'), (24, 'FATAL4'),
])
def test_severity_enum_mapping(sev: int, expected: str) -> None:
    assert _severity_enum(sev) == expected


def test_log_method_name_camel_cases_body() -> None:
    assert _log_method_name('user logged in successfully') == 'logUserLoggedInSuccessfully'


def test_log_method_name_handles_punctuation() -> None:
    assert _log_method_name('order #42: completed!') == 'logOrder42Completed'


@pytest.mark.parametrize('vt, value, expected', [
    ('STRING',  'hello', '"hello"'),
    ('BOOLEAN', 'true',  'true'),
    ('LONG',    '42',    '42L'),
    ('DOUBLE',  '3.14',  '3.14d'),
])
def test_attr_java_literal(vt: str, value: str, expected: str) -> None:
    class A:
        def get(self, k):
            return {'valueType': vt, 'value': value}.get(k)
    assert _attr_java_literal(A()) == expected


# -----------------------------------------------------------------------
# Span emitter
# -----------------------------------------------------------------------
def test_span_emitter_writes_per_service_class(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="Alpha" namespace="com.acme.alpha" language="JAVA">
        <instrumentations name="com.acme.alpha">
          <spans name="DoWork" kind="SERVER" statusCode="OK"/>
        </instrumentations>
      </services>
      <services name="Beta" namespace="com.acme.beta" language="JAVA">
        <instrumentations name="com.acme.beta">
          <spans name="Run" kind="INTERNAL" statusCode="UNSET"/>
        </instrumentations>
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    alpha = (tmp_path / 'AlphaInstrumentation.java').read_text()
    beta = (tmp_path / 'BetaInstrumentation.java').read_text()
    assert 'public Span startDoWork()' in alpha
    assert 'package com.acme.alpha;' in alpha
    assert 'public Span startRun()' in beta
    assert 'package com.acme.beta;' in beta


def test_span_emitter_parent_span_gets_context_parameter(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <spans name="Root" kind="SERVER" statusCode="UNSET"/>
          <spans name="Child" kind="INTERNAL" statusCode="UNSET"
                 parentSpan="//@services.0/@instrumentations.0/@spans.0"/>
        </instrumentations>
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    out = (tmp_path / 'SInstrumentation.java').read_text()
    assert 'public Span startRoot()' in out
    assert 'public Span startChild(Context parentContext)' in out
    assert '.setParent(parentContext)' in out


def test_span_emitter_emits_global_open_telemetry(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <spans name="X" kind="INTERNAL" statusCode="UNSET"/>
        </instrumentations>
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    out = (tmp_path / 'SInstrumentation.java').read_text()
    assert 'GlobalOpenTelemetry.get()' in out
    assert '.getTracer("com.example")' in out


# -----------------------------------------------------------------------
# Metric emitter
# -----------------------------------------------------------------------
def test_metric_emitter_counter_builder(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="requests" type="COUNTER" unit="1" valueType="LONG"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert 'import io.opentelemetry.api.metrics.LongCounter;' in out
    assert '.counterBuilder("requests")' in out
    assert '.setUnit("1")' in out


def test_metric_emitter_histogram_explicit_buckets(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="latency" type="HISTOGRAM" unit="ms"
                   valueType="DOUBLE" aggregation="EXPLICIT_BUCKET_HISTOGRAM"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert 'DoubleHistogram' in out
    assert '.setExplicitBucketBoundariesAdvice(' in out


def test_metric_emitter_default_histogram_omits_explicit_buckets(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="latency" type="HISTOGRAM" unit="ms" valueType="DOUBLE"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert '.setExplicitBucketBoundariesAdvice(' not in out


# -----------------------------------------------------------------------
# Collector YAML emitter
# -----------------------------------------------------------------------
def test_collector_yaml_pipelines_signal_slug(tmp_path: pathlib.Path) -> None:
    model = build_model('''
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
      </pipelines>''')
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    # Same logical receiver name in both pipelines must be disambiguated by the
    # signal slug — otherwise the Collector config would have duplicate keys.
    assert 'otlp/traces:' in out
    assert 'otlp/metrics:' in out
    assert 'batch/traces:' in out
    assert 'batch/metrics:' in out
    assert 'prom/metrics:' in out
    # Pipeline references must use the same slugs.
    assert 'receivers: [otlp/traces]' in out
    assert 'exporters: [prom/metrics]' in out


def test_collector_yaml_otlp_grpc_protocol_block(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example"/>
      </services>
      <pipelines name="p" signal="TRACES">
        <receivers xsi:type="obs:OtlpReceiver" name="otlp" protocol="GRPC" endpoint="0.0.0.0:4317"/>
        <exporters xsi:type="obs:OtlpExporter" name="otlp" endpoint="collector:4317"/>
      </pipelines>''')
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'protocols:' in out
    assert '  grpc:' in out
    assert 'endpoint: 0.0.0.0:4317' in out


def test_collector_yaml_attributes_processor_actions(tmp_path: pathlib.Path) -> None:
    model = build_model('''
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
      </pipelines>''')
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'action: insert' in out
    assert 'value: prod' in out
    assert 'action: delete' in out


def test_collector_yaml_tail_sampling_probability_percent(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example"/>
      </services>
      <pipelines name="p" signal="TRACES">
        <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
        <processors xsi:type="obs:TailSamplingProcessor" name="ts" decisionWait="10s">
          <policies name="p1" type="PROBABILISTIC" value="0.05"/>
        </processors>
        <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
      </pipelines>''')
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    # 0.05 → 5.0 % sampling
    assert 'sampling_percentage: 5.0' in out


# -----------------------------------------------------------------------
# Prometheus alerts emitter
# -----------------------------------------------------------------------
def test_prometheus_alerts_basic_shape(tmp_path: pathlib.Path) -> None:
    model = build_model('''
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
                  referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>''')
    gen_prometheus_alerts(model, tmp_path)
    out = (tmp_path / 'prometheus-alerts.yaml').read_text()
    assert '- alert: HighErrors' in out
    assert 'for: 5m' in out
    assert 'severity: warning' in out
    assert 'description: "Error rate elevated"' in out


def test_prometheus_alerts_expression_escapes_quotes(tmp_path: pathlib.Path) -> None:
    """A label-matched expression like
       absent(up{job="x"}) > 0
       must round-trip with the inner quotes escaped, not leak as unquoted YAML."""
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="up" type="GAUGE" unit="1" valueType="LONG"/>
        </instrumentations>
      </services>
      <alertRules name="JobMissing"
                  expression='absent(up{job="x"}) &gt; 0'
                  severity="WARNING"
                  forDuration="1m"
                  referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>''')
    gen_prometheus_alerts(model, tmp_path)
    out = (tmp_path / 'prometheus-alerts.yaml').read_text()
    assert r'expr: "absent(up{job=\"x\"}) > 0"' in out


# -----------------------------------------------------------------------
# Log emitter
# -----------------------------------------------------------------------
def test_log_emitter_skips_services_without_logs(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="Silent" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
        </instrumentations>
      </services>''')
    gen_log_instrumentation(model, tmp_path)
    assert not (tmp_path / 'SilentLogs.java').exists()


def test_log_emitter_uses_severity_enum(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <logs body="oops" severityNumber="17" severityText="ERROR"/>
        </instrumentations>
      </services>''')
    gen_log_instrumentation(model, tmp_path)
    out = (tmp_path / 'SLogs.java').read_text()
    assert '.setSeverity(Severity.ERROR)' in out
    assert '.setSeverityText("ERROR")' in out
    assert '.setBody("oops")' in out


def test_log_emitter_escapes_body_quotes(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <logs body='said &quot;hi&quot;' severityNumber="9"/>
        </instrumentations>
      </services>''')
    gen_log_instrumentation(model, tmp_path)
    out = (tmp_path / 'SLogs.java').read_text()
    assert r'.setBody("said \"hi\"")' in out
