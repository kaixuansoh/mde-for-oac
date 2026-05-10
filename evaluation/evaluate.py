#!/usr/bin/env python3
"""
Phase III evaluation harness — computes CCR, TCR, CED, MER across all
scenarios under framework/examples/ and reports a JSON + tabular summary.

Definitions (matching framework-design.md §8.4):
  CCR = N_correct / N_total                   correctness rate, [0,1]
  TCR = N_present / N_required                coverage ratio,   [0,1]
  CED = N_errors  / N_entities                error density,    >=0
  MER = (T_manual - T_generated) / T_manual   effort reduction, (-inf, 1]

MER requires a manual baseline; if absent for a scenario it is reported as
None and excluded from aggregation.

Run:
    python3 evaluate.py [--baselines baselines.json] [--out results/]
"""
import argparse, json, pathlib, re, sys, time
import xml.etree.ElementTree as ET

XSI = '{http://www.w3.org/2001/XMLSchema-instance}'
HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
EXAMPLES_DIR  = ROOT / 'examples'
GENERATED_DIR = ROOT / 'generated'
RESULTS_DIR   = HERE / 'results'

sys.path.insert(0, str(ROOT / 'scripts'))
from validate_instance import validate           # noqa: E402
from generate import (                           # noqa: E402
    gen_span_instrumentation, gen_metric_registration,
    gen_collector_yaml, gen_prometheus_alerts,
    gen_log_instrumentation)


def _local(t): return t.split('}')[-1]
def _kids(e, n): return [c for c in list(e) if _local(c.tag) == n]


# --------------------------------------------------------------------------
# Source-model signal inventory
# --------------------------------------------------------------------------
def count_required(model):
    spans = metrics = logs = alerts = 0
    for s in _kids(model, 'services'):
        for sc in _kids(s, 'instrumentations'):
            spans   += len(_kids(sc, 'spans'))
            metrics += len(_kids(sc, 'metrics'))
            logs    += len(_kids(sc, 'logs'))
    alerts = len(_kids(model, 'alertRules'))
    return {'spans': spans, 'metrics': metrics, 'logs': logs, 'alerts': alerts}


# --------------------------------------------------------------------------
# Artefact-level inventories — mirror what the templates emit
# --------------------------------------------------------------------------
def count_java_methods(java_text, prefix='start'):
    return len(re.findall(rf'public\s+\w+\s+{prefix}\w+\s*\(', java_text))

def count_metric_fields(java_text):
    return len(re.findall(r'public\s+final\s+(?:[A-Z]\w*)\s+\w+\s*=', java_text))

def count_alerts(yaml_text):
    return len(re.findall(r'^\s*-\s+alert:\s', yaml_text, flags=re.M))


# --------------------------------------------------------------------------
# Artefact-level correctness
# --------------------------------------------------------------------------
def java_correct(text):
    return text.count('{') == text.count('}') and text.count('(') == text.count(')')

def yaml_correct(text):
    try:
        import yaml
        yaml.safe_load(text)
        return True
    except Exception:
        return False

def collector_yaml_consistent(text):
    """Verify pipeline component-name references resolve to defined components."""
    import yaml
    d = yaml.safe_load(text) or {}
    defined = (set((d.get('receivers')  or {}).keys())
               | set((d.get('processors') or {}).keys())
               | set((d.get('exporters')  or {}).keys()))
    pipelines = (d.get('service') or {}).get('pipelines', {}) or {}
    for p in pipelines.values():
        for key in ('receivers', 'processors', 'exporters'):
            for ref in (p.get(key) or []):
                if ref not in defined:
                    return False
    return True

def prometheus_yaml_consistent(text):
    import yaml
    d = yaml.safe_load(text) or {}
    for g in (d.get('groups') or []):
        for r in (g.get('rules') or []):
            if not r.get('alert') or not r.get('expr') or not r.get('for'):
                return False
            if not re.match(r'^[0-9]+(ms|s|m|h|d)$', r['for']):
                return False
    return True


# --------------------------------------------------------------------------
# Per-scenario evaluation
# --------------------------------------------------------------------------
def evaluate_scenario(instance_path, generated_dir, manual_seconds=None):
    # Validation pass on the source model (needed for CED's N_errors)
    _, errors, warnings = validate(instance_path)
    model = ET.parse(instance_path).getroot()
    required = count_required(model)

    # Time the generation step end-to-end (T_generated for MER)
    out = generated_dir
    out.mkdir(parents=True, exist_ok=True)
    for f in out.iterdir(): f.unlink()
    t0 = time.perf_counter()
    gen_span_instrumentation(model, out)
    gen_metric_registration(model, out)
    gen_collector_yaml(model, out)
    gen_prometheus_alerts(model, out)
    gen_log_instrumentation(model, out)
    t_generated = time.perf_counter() - t0

    artefacts = sorted(out.iterdir())

    # Inventory the generated artefacts
    spans_present = 0
    metrics_present = 0
    logs_present = 0
    alerts_present = 0
    instrumentation_files = [f for f in artefacts if f.name.endswith('Instrumentation.java')]
    metric_files          = [f for f in artefacts if f.name.endswith('Metrics.java')]
    log_files             = [f for f in artefacts if f.name.endswith('Logs.java')]
    collector_file        = next((f for f in artefacts if f.name == 'otel-collector.yaml'), None)
    alerts_file           = next((f for f in artefacts if f.name == 'prometheus-alerts.yaml'), None)
    for f in instrumentation_files:
        spans_present += count_java_methods(f.read_text(), prefix='start')
    for f in metric_files:
        metrics_present += count_metric_fields(f.read_text())
    for f in log_files:
        logs_present += count_java_methods(f.read_text(), prefix='log')
    if alerts_file:
        alerts_present = count_alerts(alerts_file.read_text())

    # CCR — count correct artefacts
    artefact_results = []
    for f in artefacts:
        text = f.read_text()
        if f.suffix == '.java':
            ok = java_correct(text)
        elif f.name == 'otel-collector.yaml':
            ok = yaml_correct(text) and collector_yaml_consistent(text)
        elif f.name == 'prometheus-alerts.yaml':
            ok = yaml_correct(text) and prometheus_yaml_consistent(text)
        else:
            ok = True
        artefact_results.append({'file': f.name, 'correct': ok})
    n_correct = sum(1 for r in artefact_results if r['correct'])
    ccr = n_correct / len(artefacts) if artefacts else 0.0

    # TCR — per signal type and overall
    n_required = sum(required.values())
    n_present  = spans_present + metrics_present + logs_present + alerts_present
    tcr_overall = (n_present / n_required) if n_required else 0.0
    tcr_breakdown = {
        'spans':   (spans_present   / required['spans'])   if required['spans']   else None,
        'metrics': (metrics_present / required['metrics']) if required['metrics'] else None,
        'logs':    (logs_present    / required['logs'])    if required['logs']    else None,
        'alerts':  (alerts_present  / required['alerts'])  if required['alerts']  else None,
    }

    # CED — config errors per entity
    incorrect = len(artefacts) - n_correct
    n_errors  = len(errors) + incorrect
    n_entities = (spans_present + metrics_present + alerts_present
                  + len(_kids(model, 'pipelines')) * 1)
    ced = (n_errors / n_entities) if n_entities else 0.0

    # MER — only if a manual baseline is provided
    mer = None
    if manual_seconds is not None:
        mer = (manual_seconds - t_generated) / manual_seconds if manual_seconds > 0 else None

    return {
        'scenario': pathlib.Path(instance_path).stem,
        'instance':  str(instance_path),
        'generated_dir': str(out),
        'source_model': {
            'services':  len(_kids(model, 'services')),
            'pipelines': len(_kids(model, 'pipelines')),
            'required':  required,
        },
        'generated': {
            'artefacts':       len(artefacts),
            'spans_present':   spans_present,
            'metrics_present': metrics_present,
            'logs_present':    logs_present,
            'alerts_present':  alerts_present,
        },
        'artefact_correctness': artefact_results,
        'metrics': {
            'CCR': round(ccr, 4),
            'TCR_overall': round(tcr_overall, 4),
            'TCR_breakdown': {k: (round(v, 4) if v is not None else None) for k, v in tcr_breakdown.items()},
            'CED': round(ced, 6),
            'MER': round(mer, 4) if mer is not None else None,
        },
        'timing_seconds': {
            'T_generated': round(t_generated, 6),
            'T_manual':    manual_seconds,
        },
        'validation': {
            'source_model_errors':   len(errors),
            'source_model_warnings': len(warnings),
        },
    }


# --------------------------------------------------------------------------
# Driver
# --------------------------------------------------------------------------
def _resolve_manual_seconds(entry):
    """Accept either {'T_manual_seconds': N, ...} or a bare number."""
    if entry is None: return None
    if isinstance(entry, (int, float)): return entry
    if isinstance(entry, dict): return entry.get('T_manual_seconds')
    return None


def main():
    default_baselines = HERE / 'baselines.json'
    ap = argparse.ArgumentParser()
    ap.add_argument('--baselines',
                    help=f'JSON file with per-scenario manual timing. '
                         f'Defaults to {default_baselines} if it exists.')
    ap.add_argument('--out', default=str(RESULTS_DIR), help='output directory for results JSON')
    ap.add_argument('--examples', default=str(EXAMPLES_DIR), help='directory of .observability instances')
    args = ap.parse_args()

    baseline_path = pathlib.Path(args.baselines) if args.baselines else default_baselines
    baselines = {}
    if baseline_path.exists():
        baselines = json.loads(baseline_path.read_text())

    out_dir = pathlib.Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    scenarios = sorted(pathlib.Path(args.examples).glob('*.observability'))
    if not scenarios:
        print(f'No scenarios found in {args.examples}')
        sys.exit(1)

    results = []
    for inst in scenarios:
        gen_dir = GENERATED_DIR / inst.stem
        manual = _resolve_manual_seconds(baselines.get(inst.stem))
        r = evaluate_scenario(inst, gen_dir, manual_seconds=manual)
        results.append(r)

    summary = {
        'scenarios': len(results),
        'avg_CCR': round(sum(r['metrics']['CCR'] for r in results) / len(results), 4),
        'avg_TCR_overall': round(sum(r['metrics']['TCR_overall'] for r in results) / len(results), 4),
        'avg_CED': round(sum(r['metrics']['CED'] for r in results) / len(results), 6),
        'avg_MER': (round(sum(r['metrics']['MER'] for r in results if r['metrics']['MER'] is not None)
                          / max(1, sum(1 for r in results if r['metrics']['MER'] is not None)), 4)
                    if any(r['metrics']['MER'] is not None for r in results) else None),
    }
    report = {'generated_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
              'summary': summary, 'scenarios': results}
    out_json = out_dir / 'evaluation.json'
    out_json.write_text(json.dumps(report, indent=2))

    # Pretty table to stdout
    print(f'\nPhase III Evaluation — {len(results)} scenario(s)')
    print(f"{'Scenario':<32} {'Artefacts':>9} {'CCR':>6} {'TCR':>6} {'CED':>8} {'MER':>6}")
    print('-' * 72)
    for r in results:
        m = r['metrics']
        mer = f"{m['MER']:.3f}" if m['MER'] is not None else '   —  '
        print(f"{r['scenario']:<32} {r['generated']['artefacts']:>9} "
              f"{m['CCR']:>6.3f} {m['TCR_overall']:>6.3f} {m['CED']:>8.4f} {mer:>6}")
    print('-' * 72)
    avg_mer = f"{summary['avg_MER']:.3f}" if summary['avg_MER'] is not None else '   —  '
    print(f"{'AVERAGE':<32} {'':>9} "
          f"{summary['avg_CCR']:>6.3f} {summary['avg_TCR_overall']:>6.3f} "
          f"{summary['avg_CED']:>8.4f} {avg_mer:>6}")
    print(f'\nFull report: {out_json}')


if __name__ == '__main__':
    main()
