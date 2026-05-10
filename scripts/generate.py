#!/usr/bin/env python3
"""
Prototype model-to-text generator that mirrors the four Acceleo templates
under framework/templates/acceleo/. Produces the same artefacts the .mtl
templates are intended to emit, so the generation logic can be exercised
without an Eclipse + Acceleo runtime.

Generation pipeline:
    .observability  ->  parse + validate  ->  4 emitters  ->  artefacts/

Run:
    python3 generate.py <instance.observability> <output-dir>
"""
import pathlib, re, sys
import xml.etree.ElementTree as ET

XSI = '{http://www.w3.org/2001/XMLSchema-instance}'
HERE = pathlib.Path(__file__).resolve().parent
DEFAULT_ECORE = HERE.parent / 'metamodel' / 'model' / 'observability.ecore'

# Avoid re-importing the validator; replicate the small bits we need here so
# this script stays standalone.

def _local(tag): return tag.split('}')[-1]
def _kids(elem, name): return [c for c in list(elem) if _local(c.tag) == name]
def _xsi_type(elem): return (elem.get(XSI + 'type') or '').split(':')[-1]


# --------------------------------------------------------------------------
# 9a — span-instrumentation.mtl
# --------------------------------------------------------------------------
def _ident(s): return re.sub(r'[^A-Za-z0-9]', '_', s)
def _tracer_field(scope_name): return _ident(scope_name).upper() + '_TRACER'
def _meter_field(scope_name):  return _ident(scope_name).upper() + '_METER'
def _metric_field(name):       return _ident(name)


def _attr_java_literal(attr):
    vt = attr.get('valueType')
    v  = attr.get('value')
    if vt == 'STRING':  return f'"{v}"'
    if vt == 'BOOLEAN': return v
    if vt == 'LONG':    return f'{v}L'
    if vt == 'DOUBLE':  return f'{v}d'
    return f'"{v}"'


def _package_name(service):
    ns = service.get('namespace')
    return ns if ns else 'com.acme.observability'


def gen_span_instrumentation(model, outdir):
    for service in _kids(model, 'services'):
        cls = f"{service.get('name')}Instrumentation"
        out = outdir / f'{cls}.java'
        L = []
        L.append(f'package {_package_name(service)};')
        L.append('')
        L.append('import io.opentelemetry.api.OpenTelemetry;')
        L.append('import io.opentelemetry.api.common.Attributes;')
        L.append('import io.opentelemetry.api.trace.Span;')
        L.append('import io.opentelemetry.api.trace.SpanBuilder;')
        L.append('import io.opentelemetry.api.trace.SpanKind;')
        L.append('import io.opentelemetry.api.trace.StatusCode;')
        L.append('import io.opentelemetry.api.trace.Tracer;')
        L.append('import io.opentelemetry.context.Context;')
        L.append('')
        L.append('/**')
        L.append(f" * Auto-generated OpenTelemetry instrumentation for {service.get('name')}.")
        L.append(' * Generated from observability model — do not edit by hand.')
        L.append(' */')
        L.append(f'public final class {cls} {{')
        L.append('')
        for scope in _kids(service, 'instrumentations'):
            sn = scope.get('name'); sv = scope.get('version')
            ver = f', "{sv}"' if sv else ''
            L.append(f'    private static final Tracer {_tracer_field(sn)} =')
            L.append('        OpenTelemetry.getGlobalOpenTelemetry()')
            L.append(f'            .getTracer("{sn}"{ver});')
        L.append('')
        for scope in _kids(service, 'instrumentations'):
            for sp in _kids(scope, 'spans'):
                name = sp.get('name'); kind = sp.get('kind'); status = sp.get('statusCode')
                has_parent = sp.get('parentSpan') is not None
                params = 'Context parentContext' if has_parent else ''
                L.append(f'    public Span start{name}({params}) {{')
                L.append(f'        SpanBuilder builder = {_tracer_field(scope.get("name"))}')
                L.append(f'            .spanBuilder("{name}")')
                if has_parent:
                    L.append(f'            .setSpanKind(SpanKind.{kind})')
                    L.append('            .setParent(parentContext);')
                else:
                    L.append(f'            .setSpanKind(SpanKind.{kind});')
                for a in _kids(sp, 'attributes'):
                    L.append(f'        builder.setAttribute("{a.get("key")}", {_attr_java_literal(a)});')
                L.append('        Span span = builder.startSpan();')
                for ev in _kids(sp, 'events'):
                    eattrs = _kids(ev, 'attributes')
                    if eattrs:
                        L.append(f'        span.addEvent("{ev.get("name")}", Attributes.builder()')
                        for a in eattrs:
                            L.append(f'            .put("{a.get("key")}", {_attr_java_literal(a)})')
                        L.append('            .build());')
                    else:
                        L.append(f'        span.addEvent("{ev.get("name")}");')
                if status and status != 'UNSET':
                    L.append(f'        span.setStatus(StatusCode.{status});')
                L.append('        return span;')
                L.append('    }')
                L.append('')
        L.append('}')
        out.write_text('\n'.join(L))


# --------------------------------------------------------------------------
# 9d — metric-registration.mtl
# --------------------------------------------------------------------------
JAVA_TYPE = {
    ('COUNTER',                    'LONG'):   'LongCounter',
    ('COUNTER',                    'DOUBLE'): 'DoubleCounter',
    ('UP_DOWN_COUNTER',            'LONG'):   'LongUpDownCounter',
    ('UP_DOWN_COUNTER',            'DOUBLE'): 'DoubleUpDownCounter',
    ('HISTOGRAM',                  'LONG'):   'LongHistogram',
    ('HISTOGRAM',                  'DOUBLE'): 'DoubleHistogram',
    ('GAUGE',                      'LONG'):   'LongGauge',
    ('GAUGE',                      'DOUBLE'): 'DoubleGauge',
    ('OBSERVABLE_COUNTER',         'LONG'):   'ObservableLongCounter',
    ('OBSERVABLE_COUNTER',         'DOUBLE'): 'ObservableDoubleCounter',
    ('OBSERVABLE_UP_DOWN_COUNTER', 'LONG'):   'ObservableLongUpDownCounter',
    ('OBSERVABLE_UP_DOWN_COUNTER', 'DOUBLE'): 'ObservableDoubleUpDownCounter',
    ('OBSERVABLE_GAUGE',           'LONG'):   'ObservableLongGauge',
    ('OBSERVABLE_GAUGE',           'DOUBLE'): 'ObservableDoubleGauge',
}


def _builder_call(metric):
    name = metric.get('name'); t = metric.get('type'); vt = metric.get('valueType')
    if t == 'COUNTER':
        return f'counterBuilder("{name}")' + ('.ofDoubles()' if vt == 'DOUBLE' else '')
    if t == 'UP_DOWN_COUNTER':
        return f'upDownCounterBuilder("{name}")' + ('.ofDoubles()' if vt == 'DOUBLE' else '')
    if t == 'HISTOGRAM':
        return f'histogramBuilder("{name}")' + ('.ofLongs()' if vt == 'LONG' else '')
    if t == 'GAUGE':
        return f'gaugeBuilder("{name}")' + ('.ofLongs()' if vt == 'LONG' else '')
    return f'counterBuilder("{name}")'


def gen_metric_registration(model, outdir):
    for service in _kids(model, 'services'):
        cls = f"{service.get('name')}Metrics"
        out = outdir / f'{cls}.java'
        L = []
        L.append(f'package {_package_name(service)};')
        L.append('')
        for cn in sorted({JAVA_TYPE[(m.get('type'), m.get('valueType'))]
                          for scope in _kids(service, 'instrumentations')
                          for m in _kids(scope, 'metrics')}):
            L.append(f'import io.opentelemetry.api.metrics.{cn};')
        L.append('import io.opentelemetry.api.metrics.Meter;')
        L.append('import io.opentelemetry.api.OpenTelemetry;')
        L.append('')
        L.append('/**')
        L.append(f" * Auto-generated OpenTelemetry metric registry for {service.get('name')}.")
        L.append(' * Generated from observability model — do not edit by hand.')
        L.append(' */')
        L.append(f'public final class {cls} {{')
        L.append('')
        for scope in _kids(service, 'instrumentations'):
            L.append(f'    private static final Meter {_meter_field(scope.get("name"))} =')
            L.append('        OpenTelemetry.getGlobalOpenTelemetry()')
            L.append(f'            .getMeter("{scope.get("name")}");')
        L.append('')
        for scope in _kids(service, 'instrumentations'):
            for m in _kids(scope, 'metrics'):
                jtype = JAVA_TYPE[(m.get('type'), m.get('valueType'))]
                desc = m.get('description'); agg = m.get('aggregation')
                L.append(f'    public final {jtype} {_metric_field(m.get("name"))} = {_meter_field(scope.get("name"))}')
                line = f'        .{_builder_call(m)}'
                line += f'.setUnit("{m.get("unit")}")'
                if desc: line += f'.setDescription("{desc}")'
                if m.get('type') == 'HISTOGRAM' and agg == 'EXPLICIT_BUCKET_HISTOGRAM':
                    line += ('.setExplicitBucketBoundariesAdvice('
                             'java.util.List.of(5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, '
                             '1000.0, 2500.0, 5000.0, 10000.0))')
                line += '.build();'
                L.append(line)
                L.append('')
        L.append('}')
        out.write_text('\n'.join(L))


# --------------------------------------------------------------------------
# 9b — collector-yaml.mtl
# --------------------------------------------------------------------------
SIGNAL_SLUG = {'TRACES': 'traces', 'METRICS': 'metrics', 'LOGS': 'logs'}


def _receiver_body(r, indent='    '):
    typ = _xsi_type(r); ep = r.get('endpoint'); proto = r.get('protocol')
    if typ in ('OtlpReceiver', 'JaegerReceiver'):
        proto_key = 'grpc' if proto == 'GRPC' else 'http'
        return [f'{indent}protocols:',
                f'{indent}  {proto_key}:',
                f'{indent}    endpoint: {ep}']
    if typ == 'PrometheusReceiver':
        return [f'{indent}config:',
                f'{indent}  scrape_configs:',
                f"{indent}    - job_name: 'otel'",
                f'{indent}      static_configs:',
                f"{indent}        - targets: ['{ep}']"]
    return [f'{indent}endpoint: {ep}']


def _processor_body(pr, indent='    '):
    typ = _xsi_type(pr)
    if typ == 'BatchProcessor':
        return [f'{indent}timeout: {pr.get("timeout")}',
                f'{indent}send_batch_size: {pr.get("maxBatchSize")}']
    if typ == 'MemoryLimiterProcessor':
        return [f'{indent}check_interval: 1s',
                f'{indent}limit_mib: {pr.get("limitMiB")}',
                f'{indent}spike_limit_mib: {pr.get("spikeLimitMiB")}']
    if typ == 'AttributesProcessor':
        out = [f'{indent}actions:']
        for a in _kids(pr, 'actions'):
            out.append(f'{indent}  - key: {a.get("key")}')
            out.append(f'{indent}    action: {a.get("action").lower()}')
            if a.get('value') is not None:
                out.append(f'{indent}    value: {a.get("value")}')
        return out
    if typ == 'TailSamplingProcessor':
        out = [f'{indent}decision_wait: {pr.get("decisionWait")}',
               f'{indent}policies:']
        for p in _kids(pr, 'policies'):
            out.append(f'{indent}  - name: {p.get("name")}')
            out.append(f'{indent}    type: {p.get("type").lower()}')
            v = p.get('value')
            t = p.get('type')
            if v is not None:
                if t == 'PROBABILISTIC':
                    out.append(f'{indent}    probabilistic:')
                    out.append(f'{indent}      sampling_percentage: {float(v) * 100.0}')
                elif t == 'STATUS_CODE':
                    out.append(f'{indent}    status_code:')
                    out.append(f'{indent}      status_codes: [{v}]')
                elif t == 'STRING_ATTRIBUTE':
                    out.append(f'{indent}    string_attribute:')
                    out.append(f'{indent}      values: [{v}]')
        return out
    return [f'{indent}{{}}']


def _exporter_body(e, indent='    '):
    typ = _xsi_type(e); ep = e.get('endpoint'); comp = e.get('compression')
    if typ == 'OtlpExporter':
        out = [f'{indent}endpoint: {ep}']
        if comp: out.append(f'{indent}compression: {comp}')
        out.append(f'{indent}tls:')
        out.append(f'{indent}  insecure: true')
        return out
    if typ == 'PrometheusExporter':
        return [f'{indent}endpoint: {ep}']
    if typ == 'JaegerExporter':
        return [f'{indent}endpoint: {ep}', f'{indent}tls:', f'{indent}  insecure: true']
    if typ == 'LoggingExporter':
        return [f'{indent}loglevel: info']
    return [f'{indent}endpoint: {ep}']


def gen_collector_yaml(model, outdir):
    L = []
    L.append('# Auto-generated OpenTelemetry Collector configuration')
    L.append(f'# Generated from: {model.get("name")} v{model.get("version")}')
    L.append('')
    L.append('receivers:')
    for p in _kids(model, 'pipelines'):
        slug = SIGNAL_SLUG[p.get('signal')]
        for r in _kids(p, 'receivers'):
            L.append(f'  {r.get("name")}/{slug}:')
            L.extend(_receiver_body(r))
    L.append('')
    L.append('processors:')
    for p in _kids(model, 'pipelines'):
        slug = SIGNAL_SLUG[p.get('signal')]
        for pr in _kids(p, 'processors'):
            L.append(f'  {pr.get("name")}/{slug}:')
            L.extend(_processor_body(pr))
    L.append('')
    L.append('exporters:')
    for p in _kids(model, 'pipelines'):
        slug = SIGNAL_SLUG[p.get('signal')]
        for e in _kids(p, 'exporters'):
            L.append(f'  {e.get("name")}/{slug}:')
            L.extend(_exporter_body(e))
    L.append('')
    L.append('service:')
    L.append('  pipelines:')
    for p in _kids(model, 'pipelines'):
        slug = SIGNAL_SLUG[p.get('signal')]
        rs = ', '.join(f'{r.get("name")}/{slug}' for r in _kids(p, 'receivers'))
        ps = ', '.join(f'{pr.get("name")}/{slug}' for pr in _kids(p, 'processors'))
        es = ', '.join(f'{e.get("name")}/{slug}' for e in _kids(p, 'exporters'))
        L.append(f'    {slug}:')
        L.append(f'      receivers: [{rs}]')
        L.append(f'      processors: [{ps}]')
        L.append(f'      exporters: [{es}]')
    (outdir / 'otel-collector.yaml').write_text('\n'.join(L) + '\n')


# --------------------------------------------------------------------------
# 9c — prometheus-alerts.mtl
# --------------------------------------------------------------------------
def gen_prometheus_alerts(model, outdir):
    L = []
    L.append('# Auto-generated Prometheus alerting rules')
    L.append(f'# Generated from: {model.get("name")} v{model.get("version")}')
    L.append('')
    L.append('groups:')
    L.append(f'  - name: {model.get("name")}')
    L.append('    rules:')
    for ar in _kids(model, 'alertRules'):
        L.append(f'      - alert: {ar.get("name")}')
        # Quote expression to keep YAML safe regardless of operators inside.
        expr = ar.get("expression").replace('"', '\\"')
        L.append(f'        expr: "{expr}"')
        L.append(f'        for: {ar.get("forDuration")}')
        L.append('        labels:')
        L.append(f'          severity: {ar.get("severity").lower()}')
        for l in _kids(ar, 'labels'):
            L.append(f'          {l.get("key")}: "{l.get("value")}"')
        anns = _kids(ar, 'annotations')
        desc = ar.get('description')
        if anns or desc:
            L.append('        annotations:')
            if desc: L.append(f'          description: "{desc}"')
            for a in anns:
                L.append(f'          {a.get("key")}: "{a.get("value")}"')
    (outdir / 'prometheus-alerts.yaml').write_text('\n'.join(L) + '\n')


# --------------------------------------------------------------------------
# Driver
# --------------------------------------------------------------------------
def main():
    if len(sys.argv) < 3:
        print(__doc__); sys.exit(2)
    instance, output_dir = sys.argv[1], pathlib.Path(sys.argv[2])
    output_dir.mkdir(parents=True, exist_ok=True)

    # Validate first
    sys.path.insert(0, str(HERE))
    from validate_instance import validate
    nodes, errors, warnings = validate(instance)
    if errors:
        print(f'Refusing to generate: {len(errors)} validation errors')
        for e in errors: print(f'  X {e}')
        sys.exit(1)
    for w in warnings: print(f'  ! {w}')

    model = ET.parse(instance).getroot()
    gen_span_instrumentation(model, output_dir)
    gen_metric_registration(model, output_dir)
    gen_collector_yaml(model, output_dir)
    gen_prometheus_alerts(model, output_dir)
    print(f'Generated {len(list(output_dir.iterdir()))} artefact(s) in {output_dir}')


if __name__ == '__main__':
    main()
