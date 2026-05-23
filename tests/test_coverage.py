"""Additional focused unit tests for the validator and each of the five
emitters in scripts/generate.py.

The negative corpus + test_validator.py cover error-tier OCL invariants
broadly; this file targets behaviours and code paths that aren't otherwise
exercised — secondary metric types, secondary collector component types,
sub-tier log severity literals, alert labels/annotations, and the explicit
positive cases for warning-tier invariants where the corpus only seeds the
negative side."""
from __future__ import annotations
import pathlib
import pytest

from validate_instance import validate
from generate import (
    _ident, _severity_enum, _log_method_name,
    gen_span_instrumentation, gen_metric_registration,
    gen_collector_yaml, gen_prometheus_alerts, gen_log_instrumentation,
)
from _helpers import build_model, write_instance


# =======================================================================
# Validator — invariants not already covered by test_validator.py
# =======================================================================
def test_e1_duplicate_service_names_rejected(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="Dup" namespace="com.example.a" language="JAVA">
      <instrumentations name="com.example.a"/>
    </services>
    <services name="Dup" namespace="com.example.b" language="JAVA">
      <instrumentations name="com.example.b"/>
    </services>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert any('E1' in e for e in errs)


def test_e2_duplicate_metric_names_in_scope_rejected(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <metrics name="dup" type="COUNTER" unit="1" valueType="LONG"/>
        <metrics name="dup" type="COUNTER" unit="1" valueType="LONG"/>
      </instrumentations>
    </services>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert any('E2' in e for e in errs)


def test_e2_same_metric_name_in_different_scopes_allowed(tmp_path: pathlib.Path) -> None:
    """E2 is scoped per InstrumentationScope, not per model."""
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example.a">
        <metrics name="requests" type="COUNTER" unit="1" valueType="LONG"/>
      </instrumentations>
      <instrumentations name="com.example.b">
        <metrics name="requests" type="COUNTER" unit="1" valueType="LONG"/>
      </instrumentations>
    </services>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert not any('E2' in e for e in errs)


def test_e3_alert_referencing_existing_metric_clean(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
      </instrumentations>
    </services>
    <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                severity="WARNING" forDuration="5m"
                referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert not any('E3' in e for e in errs)


def test_e10_insert_action_requires_value(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example"/>
    </services>
    <pipelines name="p" signal="METRICS">
      <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
      <processors xsi:type="obs:AttributesProcessor" name="enrich">
        <actions action="INSERT" key="cluster"/>
      </processors>
      <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
    </pipelines>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert any('E10' in e for e in errs)


def test_e10_delete_action_does_not_require_value(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example"/>
    </services>
    <pipelines name="p" signal="METRICS">
      <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
      <processors xsi:type="obs:AttributesProcessor" name="enrich">
        <actions action="DELETE" key="legacy.key"/>
      </processors>
      <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
    </pipelines>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert not any('E10' in e for e in errs)


def test_e11_histogram_default_aggregation_accepted(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <metrics name="m" type="HISTOGRAM" unit="ms" valueType="DOUBLE" aggregation="DEFAULT"/>
      </instrumentations>
    </services>'''
    _, errs, _ = validate(write_instance(tmp_path, body))
    assert not any('E11' in e for e in errs)


def test_w3_non_java_language_warns(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="GO">
      <instrumentations name="com.example">
        <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
      </instrumentations>
    </services>'''
    _, _, warns = validate(write_instance(tmp_path, body))
    assert any('W3' in w for w in warns)


def test_w5_pipeline_without_batch_processor_warns(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example"/>
    </services>
    <pipelines name="p" signal="METRICS">
      <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
      <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
    </pipelines>'''
    _, _, warns = validate(write_instance(tmp_path, body))
    assert any('W5' in w for w in warns)


def test_w7_service_emitting_nothing_warns(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="Empty" namespace="com.example" language="JAVA">
      <instrumentations name="com.example"/>
    </services>'''
    _, _, warns = validate(write_instance(tmp_path, body))
    assert any('W7' in w for w in warns)


# =======================================================================
# Span emitter
# =======================================================================
def test_span_emitter_sets_explicit_status(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <spans name="Op" kind="SERVER" statusCode="OK"/>
        </instrumentations>
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    out = (tmp_path / 'SInstrumentation.java').read_text()
    assert 'span.setStatus(StatusCode.OK);' in out


def test_span_emitter_omits_status_when_unset(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <spans name="Op" kind="INTERNAL" statusCode="UNSET"/>
        </instrumentations>
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    out = (tmp_path / 'SInstrumentation.java').read_text()
    assert 'setStatus' not in out


def test_span_emitter_emits_attributes_and_events(tmp_path: pathlib.Path) -> None:
    model = build_model('''
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
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    out = (tmp_path / 'SInstrumentation.java').read_text()
    assert 'builder.setAttribute("http.method", "GET");' in out
    assert 'span.addEvent("cache.hit", Attributes.builder()' in out
    assert '.put("cache.size", 128L)' in out


def test_span_emitter_uses_default_package_when_namespace_missing(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" language="JAVA">
        <instrumentations name="scope">
          <spans name="Op" kind="INTERNAL" statusCode="UNSET"/>
        </instrumentations>
      </services>''')
    gen_span_instrumentation(model, tmp_path)
    out = (tmp_path / 'SInstrumentation.java').read_text()
    assert 'package com.acme.observability;' in out


# =======================================================================
# Metric emitter — secondary types and description
# =======================================================================
def test_metric_emitter_up_down_counter_double(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="queue.depth" type="UP_DOWN_COUNTER" unit="1" valueType="DOUBLE"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert 'DoubleUpDownCounter' in out
    assert '.upDownCounterBuilder("queue.depth").ofDoubles()' in out


def test_metric_emitter_gauge_long(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="temperature" type="GAUGE" unit="1" valueType="LONG"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert 'LongGauge' in out
    assert '.gaugeBuilder("temperature").ofLongs()' in out


def test_metric_emitter_includes_description(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="m" type="COUNTER" unit="1" valueType="LONG"
                   description="total widgets minted"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert '.setDescription("total widgets minted")' in out


def test_metric_emitter_imports_are_sorted_and_deduplicated(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="a" type="COUNTER" unit="1" valueType="LONG"/>
          <metrics name="b" type="COUNTER" unit="1" valueType="LONG"/>
          <metrics name="c" type="HISTOGRAM" unit="ms" valueType="DOUBLE"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    # LongCounter imported once even with two counters; sorted before LongHistogram (D < L lexicographically).
    assert out.count('import io.opentelemetry.api.metrics.LongCounter;') == 1
    assert out.index('DoubleHistogram') < out.index('LongCounter')


def test_metric_emitter_observable_double_gauge(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="memory" type="OBSERVABLE_GAUGE" unit="By" valueType="DOUBLE"/>
        </instrumentations>
      </services>''')
    gen_metric_registration(model, tmp_path)
    out = (tmp_path / 'SMetrics.java').read_text()
    assert 'ObservableDoubleGauge' in out


# =======================================================================
# Collector YAML — secondary component types
# =======================================================================
def _pipeline_with(component_xml: str, signal: str = 'METRICS') -> str:
    return f'''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example"/>
    </services>
    <pipelines name="p" signal="{signal}">
      {component_xml}
    </pipelines>'''


def test_collector_yaml_memory_limiter_processor(tmp_path: pathlib.Path) -> None:
    model = build_model(_pipeline_with('''
      <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
      <processors xsi:type="obs:MemoryLimiterProcessor" name="mem"
                  limitMiB="512" spikeLimitMiB="128"/>
      <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>'''))
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'check_interval: 1s' in out
    assert 'limit_mib: 512' in out
    assert 'spike_limit_mib: 128' in out


def test_collector_yaml_prometheus_receiver_scrape_config(tmp_path: pathlib.Path) -> None:
    model = build_model(_pipeline_with('''
      <receivers xsi:type="obs:PrometheusReceiver" name="prom" endpoint="localhost:9090"/>
      <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>'''))
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'scrape_configs:' in out
    assert "- job_name: 'otel'" in out
    assert "- targets: ['localhost:9090']" in out


def test_collector_yaml_logging_exporter_loglevel(tmp_path: pathlib.Path) -> None:
    model = build_model(_pipeline_with('''
      <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
      <exporters xsi:type="obs:LoggingExporter" name="log" endpoint="ignored"/>'''))
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'loglevel: info' in out


def test_collector_yaml_otlp_exporter_with_compression_and_tls(tmp_path: pathlib.Path) -> None:
    model = build_model(_pipeline_with('''
      <receivers xsi:type="obs:OtlpReceiver" name="r" protocol="GRPC" endpoint="0.0.0.0:4317"/>
      <exporters xsi:type="obs:OtlpExporter" name="otlp" endpoint="collector:4317" compression="gzip"/>'''))
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'compression: gzip' in out
    assert 'insecure: true' in out


def test_collector_yaml_jaeger_exporter_in_traces_pipeline(tmp_path: pathlib.Path) -> None:
    model = build_model(_pipeline_with('''
      <receivers xsi:type="obs:JaegerReceiver" name="jr" protocol="GRPC" endpoint="0.0.0.0:14250"/>
      <exporters xsi:type="obs:JaegerExporter" name="jx" endpoint="jaeger:14250"/>''',
        signal='TRACES'))
    gen_collector_yaml(model, tmp_path)
    out = (tmp_path / 'otel-collector.yaml').read_text()
    assert 'jr/traces:' in out
    assert 'jx/traces:' in out
    assert 'endpoint: jaeger:14250' in out


# =======================================================================
# Prometheus alerts emitter
# =======================================================================
def test_prometheus_alerts_includes_labels(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
        </instrumentations>
      </services>
      <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                  severity="WARNING" forDuration="5m"
                  referencedMetric="//@services.0/@instrumentations.0/@metrics.0">
        <labels key="team" value="platform"/>
      </alertRules>''')
    gen_prometheus_alerts(model, tmp_path)
    out = (tmp_path / 'prometheus-alerts.yaml').read_text()
    assert 'severity: warning' in out
    assert 'team: "platform"' in out


def test_prometheus_alerts_emits_annotations(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
        </instrumentations>
      </services>
      <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                  severity="CRITICAL" forDuration="10m"
                  referencedMetric="//@services.0/@instrumentations.0/@metrics.0">
        <annotations key="runbook" value="https://runbooks/errors"/>
      </alertRules>''')
    gen_prometheus_alerts(model, tmp_path)
    out = (tmp_path / 'prometheus-alerts.yaml').read_text()
    assert 'annotations:' in out
    assert 'runbook: "https://runbooks/errors"' in out
    assert 'severity: critical' in out


def test_prometheus_alerts_group_uses_model_name(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <metrics name="errors" type="COUNTER" unit="1" valueType="LONG"/>
        </instrumentations>
      </services>
      <alertRules name="A" expression="rate(errors[1m]) &gt; 0"
                  severity="WARNING" forDuration="5m"
                  referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>''',
                        name='banking')
    gen_prometheus_alerts(model, tmp_path)
    out = (tmp_path / 'prometheus-alerts.yaml').read_text()
    assert '- name: banking' in out


# =======================================================================
# Log emitter
# =======================================================================
def test_log_emitter_attributes_emitted_with_typed_keys(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <logs body="user signed in" severityNumber="9" severityText="INFO">
            <attributes key="user.id" valueType="LONG" value="42"/>
            <attributes key="user.region" valueType="STRING" value="apac"/>
          </logs>
        </instrumentations>
      </services>''')
    gen_log_instrumentation(model, tmp_path)
    out = (tmp_path / 'SLogs.java').read_text()
    assert '.setAttribute(AttributeKey.longKey("user.id"), 42L)' in out
    assert '.setAttribute(AttributeKey.stringKey("user.region"), "apac")' in out


def test_log_emitter_emits_logger_per_scope(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example.a">
          <logs body="a" severityNumber="9"/>
        </instrumentations>
        <instrumentations name="com.example.b">
          <logs body="b" severityNumber="9"/>
        </instrumentations>
      </services>''')
    gen_log_instrumentation(model, tmp_path)
    out = (tmp_path / 'SLogs.java').read_text()
    assert '.get("com.example.a")' in out
    assert '.get("com.example.b")' in out


def test_log_emitter_omits_severity_text_when_absent(tmp_path: pathlib.Path) -> None:
    model = build_model('''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example">
          <logs body="x" severityNumber="9"/>
        </instrumentations>
      </services>''')
    gen_log_instrumentation(model, tmp_path)
    out = (tmp_path / 'SLogs.java').read_text()
    assert '.setSeverityText(' not in out


# =======================================================================
# Pure helpers in generate.py
# =======================================================================
@pytest.mark.parametrize('raw, expected', [
    ('plain',         'plain'),
    ('with space',    'with_space'),
    ('dot.separated', 'dot_separated'),
    ('a/b-c.d',       'a_b_c_d'),
])
def test_ident_replaces_non_alnum_with_underscore(raw: str, expected: str) -> None:
    assert _ident(raw) == expected


@pytest.mark.parametrize('sev, expected', [
    (2,  'TRACE2'), (3,  'TRACE3'),
    (6,  'DEBUG2'), (8,  'DEBUG4'),
    (10, 'INFO2'),  (14, 'WARN2'),
    (18, 'ERROR2'), (22, 'FATAL2'),
])
def test_severity_enum_subtiers(sev: int, expected: str) -> None:
    assert _severity_enum(sev) == expected


def test_log_method_name_empty_body_is_just_prefix() -> None:
    assert _log_method_name('') == 'log'
    assert _log_method_name(None) == 'log'
