#!/usr/bin/env python3
"""
Phase III evaluation harness.

Computes four metrics across all scenarios under examples/ plus the
negative-corpus under corpus/negative/, and emits both a JSON report and a
tabular summary.

  CCR = N_correct / N_total                 correctness rate, [0,1]
  TCR = N_present / N_required              transformation coverage,  [0,1]
  CED = N_errors  / N_entities              error density on positive inputs, >=0
  ICR = N_caught  / N_seeded_violations     invariant coverage rate on the
                                            negative corpus, [0,1]

Notes on definitions:
  * CCR is now computed against real validators
    (javac against opentelemetry-api, otelcol validate, promtool check rules)
    with structural fallbacks recorded explicitly when the binary is absent.
    See validators/checks.py for the per-artefact contract.
  * CED is intentionally measured only on the positive corpus. On models that
    pass the OCL gate, CED >= 0 reflects defects the validator did not catch
    but real tools (javac etc.) do. ICR complements CED by quantifying
    validator coverage on the negative corpus.

Run:
    python3 evaluate.py [--out results/]
"""
import argparse, json, pathlib, re, sys, time
import xml.etree.ElementTree as ET

XSI = '{http://www.w3.org/2001/XMLSchema-instance}'
HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parent
EXAMPLES_DIR  = ROOT / 'examples'
GENERATED_DIR = ROOT / 'generated'
CORPUS_NEG_DIR = HERE / 'corpus' / 'negative'
RESULTS_DIR   = HERE / 'results'

sys.path.insert(0, str(ROOT / 'scripts'))
sys.path.insert(0, str(HERE / 'validators'))
from validate_instance import validate           # noqa: E402
from generate import (                           # noqa: E402
    gen_span_instrumentation, gen_metric_registration,
    gen_collector_yaml, gen_prometheus_alerts,
    gen_log_instrumentation)
from checks import (                             # noqa: E402
    compile_java_batch, check_collector_yaml, check_prometheus_alerts,
    CheckResult)


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
# Per-scenario evaluation (positive corpus)
# --------------------------------------------------------------------------
def evaluate_scenario(instance_path, generated_dir):
    _, errors, warnings = validate(instance_path)
    model = ET.parse(instance_path).getroot()
    required = count_required(model)

    out = generated_dir
    out.mkdir(parents=True, exist_ok=True)
    for f in out.iterdir(): f.unlink()
    gen_span_instrumentation(model, out)
    gen_metric_registration(model, out)
    gen_collector_yaml(model, out)
    gen_prometheus_alerts(model, out)
    gen_log_instrumentation(model, out)

    artefacts = sorted(out.iterdir())
    java_files = [f for f in artefacts if f.suffix == '.java']

    spans_present = 0
    metrics_present = 0
    logs_present = 0
    alerts_present = 0
    for f in artefacts:
        n = f.name
        if n.endswith('Instrumentation.java'): spans_present   += count_java_methods(f.read_text(), prefix='start')
        elif n.endswith('Metrics.java'):       metrics_present += count_metric_fields(f.read_text())
        elif n.endswith('Logs.java'):          logs_present    += count_java_methods(f.read_text(), prefix='log')
        elif n == 'prometheus-alerts.yaml':    alerts_present   = count_alerts(f.read_text())

    java_results = compile_java_batch(java_files)

    # An artefact whose validator could not run (binary or library absent)
    # is reported but excluded from CCR's denominator: CCR is a property of
    # artefacts we can actually verify.
    UNAVAILABLE = {'missing-pyyaml', 'java-syntactic-fallback'}

    artefact_results = []
    for f in artefacts:
        if f.suffix == '.java':
            r = java_results[str(f)]
        elif f.name == 'otel-collector.yaml':
            r = check_collector_yaml(f)
        elif f.name == 'prometheus-alerts.yaml':
            r = check_prometheus_alerts(f)
        else:
            r = CheckResult(ok=True, validator='none')
        artefact_results.append({
            'file': f.name,
            'correct': r.ok,
            'validator': r.validator,
            'unchecked': r.validator in UNAVAILABLE,
            'diagnostics': r.diagnostics,
        })
    checkable = [a for a in artefact_results if not a['unchecked']]
    n_correct = sum(1 for r in checkable if r['correct'])
    ccr = (n_correct / len(checkable)) if checkable else 0.0

    n_required = sum(required.values())
    n_present  = spans_present + metrics_present + logs_present + alerts_present
    tcr_overall = (n_present / n_required) if n_required else 0.0
    tcr_breakdown = {
        'spans':   (spans_present   / required['spans'])   if required['spans']   else None,
        'metrics': (metrics_present / required['metrics']) if required['metrics'] else None,
        'logs':    (logs_present    / required['logs'])    if required['logs']    else None,
        'alerts':  (alerts_present  / required['alerts'])  if required['alerts']  else None,
    }

    incorrect = len(checkable) - n_correct
    n_errors  = len(errors) + incorrect
    n_entities = (spans_present + metrics_present + alerts_present
                  + len(_kids(model, 'pipelines')) * 1)
    ced = (n_errors / n_entities) if n_entities else 0.0

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
            'checkable_artefacts': len(checkable),
            'unchecked_artefacts': len(artefacts) - len(checkable),
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
        },
        'validation': {
            'source_model_errors':   len(errors),
            'source_model_warnings': len(warnings),
        },
    }


# --------------------------------------------------------------------------
# Negative corpus — Invariant Coverage Rate (ICR)
# --------------------------------------------------------------------------
def evaluate_negative_corpus(corpus_dir):
    """For each subdirectory holding {instance.observability, expected.json},
    run the validator and check that the expected diagnostic code appears.

    Codes starting with 'E' are matched against the validator's error list;
    codes starting with 'W' are matched against the warning list. ICR counts
    a case as caught when the seeded code appears in the matching list.

    Returns (per_case_records, overall_icr).
    """
    if not corpus_dir.exists():
        return [], 0.0

    cases = sorted(p for p in corpus_dir.iterdir() if p.is_dir())
    records = []
    for case_dir in cases:
        inst = case_dir / 'instance.observability'
        expected = json.loads((case_dir / 'expected.json').read_text())
        code = expected['code']
        kind = 'warning' if code.startswith('W') else 'error'
        _, errs, warns = validate(inst)
        # Match the seeded code as a whole token at the start of the message
        # (the validator emits "E1 UniqueServiceNames: ...", "W3 Service ...").
        pat = re.compile(rf'\b{re.escape(code)}\b')
        target = warns if kind == 'warning' else errs
        caught_by = [m for m in target if pat.search(m)]
        incidental = [m for m in target if m not in caught_by]
        records.append({
            'case': case_dir.name,
            'seeded_code': code,
            'seeded_kind': kind,
            'seeded_name': expected.get('name'),
            'rationale':   expected.get('rationale'),
            'caught': bool(caught_by),
            'caught_messages':    caught_by,
            'incidental_errors':  errs if kind == 'warning' else incidental,
            'warnings':           incidental if kind == 'warning' else warns,
        })
    n_caught = sum(1 for r in records if r['caught'])
    icr = (n_caught / len(records)) if records else 0.0
    return records, icr


# --------------------------------------------------------------------------
# Driver
# --------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=str(RESULTS_DIR), help='output directory for results JSON')
    ap.add_argument('--examples', default=str(EXAMPLES_DIR), help='directory of .observability instances')
    ap.add_argument('--negative-corpus', default=str(CORPUS_NEG_DIR), help='directory of negative test cases')
    args = ap.parse_args()

    out_dir = pathlib.Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    scenarios = sorted(pathlib.Path(args.examples).glob('*.observability'))
    if not scenarios:
        print(f'No scenarios found in {args.examples}')
        sys.exit(1)

    pos_results = []
    for inst in scenarios:
        gen_dir = GENERATED_DIR / inst.stem
        r = evaluate_scenario(inst, gen_dir)
        pos_results.append(r)

    neg_records, icr = evaluate_negative_corpus(pathlib.Path(args.negative_corpus))

    summary = {
        'scenarios': len(pos_results),
        'avg_CCR': round(sum(r['metrics']['CCR'] for r in pos_results) / len(pos_results), 4),
        'avg_TCR_overall': round(sum(r['metrics']['TCR_overall'] for r in pos_results) / len(pos_results), 4),
        'avg_CED': round(sum(r['metrics']['CED'] for r in pos_results) / len(pos_results), 6),
        'negative_cases': len(neg_records),
        'ICR': round(icr, 4),
    }
    report = {
        'generated_at': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
        'summary': summary,
        'scenarios': pos_results,
        'negative_corpus': neg_records,
    }
    out_json = out_dir / 'evaluation.json'
    out_json.write_text(json.dumps(report, indent=2))

    print(f'\nPhase III Evaluation — {len(pos_results)} positive scenario(s), '
          f'{len(neg_records)} negative case(s)')
    print(f"{'Scenario':<32} {'Artefacts':>9} {'CCR':>6} {'TCR':>6} {'CED':>8}")
    print('-' * 65)
    for r in pos_results:
        m = r['metrics']
        print(f"{r['scenario']:<32} {r['generated']['artefacts']:>9} "
              f"{m['CCR']:>6.3f} {m['TCR_overall']:>6.3f} {m['CED']:>8.4f}")
    print('-' * 65)
    print(f"{'AVERAGE':<32} {'':>9} "
          f"{summary['avg_CCR']:>6.3f} {summary['avg_TCR_overall']:>6.3f} "
          f"{summary['avg_CED']:>8.4f}")

    if neg_records:
        print(f'\nNegative-corpus ICR — {len(neg_records)} cases')
        print(f"{'Case':<42} {'Seeded':>7} {'Kind':>8} {'Caught':>7}")
        print('-' * 68)
        for r in neg_records:
            mark = 'yes' if r['caught'] else 'NO'
            print(f"{r['case']:<42} {r['seeded_code']:>7} {r['seeded_kind']:>8} {mark:>7}")
        print('-' * 68)
        print(f"ICR overall: {icr:.3f}")

    print(f'\nFull report: {out_json}')


if __name__ == '__main__':
    main()
