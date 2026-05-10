#!/usr/bin/env python3
"""
Lightweight structural + OCL validator for .observability instances.

Loads observability.ecore and a target instance file, then checks:
  1. XML well-formedness
  2. Class / feature / enum literal coverage against the metamodel
  3. Cardinality bounds (lower/upper) for attributes and containments
  4. Cross-reference path resolution for non-containment EReferences
  5. Hand-coded equivalents of every OCL invariant defined in the metamodel

Errors fail the run (exit 1); warnings are reported but do not fail.

Replaceable by Eclipse OCL Diagnostician once the Java framework is in place.
Usage:
    python3 validate-instance.py <instance.observability> [<metamodel.ecore>]
"""
import re, sys, pathlib
import xml.etree.ElementTree as ET

XSI_NS = 'http://www.w3.org/2001/XMLSchema-instance'
HERE = pathlib.Path(__file__).resolve().parent
DEFAULT_ECORE = HERE.parent / 'metamodel' / 'model' / 'observability.ecore'

UCUM_UNITS = {'1','ms','s','By','KBy','MBy','GBy','{requests}','{errors}','%'}
SEMCONV_PREFIXES = ('http.','db.','rpc.','messaging.','net.','service.','k8s.','cloud.')


def load_metamodel(path):
    root = ET.parse(path).getroot()
    classes, enums = {}, {}
    for c in root.findall('eClassifiers'):
        t = c.get(f'{{{XSI_NS}}}type'); name = c.get('name')
        if t == 'ecore:EClass':
            super_types = [s.replace('#//','') for s in (c.get('eSuperTypes') or '').split() if s]
            feats = {}
            for f in c.findall('eStructuralFeatures'):
                ft = f.get(f'{{{XSI_NS}}}type'); fname = f.get('name')
                etype = f.get('eType','')
                ref_type = etype.split('#//')[-1] if '#//' in etype else etype.split('/')[-1]
                feats[fname] = {
                    'kind': 'attr' if ft == 'ecore:EAttribute' else 'ref',
                    'lower': int(f.get('lowerBound') or 0),
                    'upper': int(f.get('upperBound') or 1),
                    'containment': f.get('containment') == 'true',
                    'refType': ref_type,
                }
            classes[name] = {'features': feats, 'super': super_types,
                             'abstract': c.get('abstract') == 'true'}
        elif t == 'ecore:EEnum':
            enums[name] = {l.get('name') for l in c.findall('eLiterals')}
    return classes, enums


def validate(instance_path, ecore_path=DEFAULT_ECORE):
    classes, enums = load_metamodel(ecore_path)

    def all_features(cn):
        out = {}
        for s in classes[cn]['super']: out.update(all_features(s))
        out.update(classes[cn]['features'])
        return out

    def is_kind_of(child, parent):
        if child == parent: return True
        return any(is_kind_of(s, parent) for s in classes.get(child,{}).get('super',[]))

    inst_root = ET.parse(instance_path).getroot()
    errors, warnings, nodes = [], [], {}

    def walk(elem, path, eclass):
        nodes[path] = (eclass, elem)
        feats = all_features(eclass); counters = {}
        for child in list(elem):
            fname = child.tag.split('}')[-1]
            if fname not in feats:
                errors.append(f'{path}: unknown feature "{fname}" on {eclass}'); continue
            feat = feats[fname]
            if feat['kind'] != 'ref' or not feat['containment']:
                errors.append(f'{path}: child <{fname}> not a containment ref'); continue
            idx = counters.get(fname,0); counters[fname] = idx+1
            xsi_type = child.get(f'{{{XSI_NS}}}type')
            if xsi_type:
                child_class = xsi_type.split(':')[-1]
                if child_class not in classes:
                    errors.append(f'{path}/{fname}.{idx}: xsi:type {child_class} unknown'); continue
                if not is_kind_of(child_class, feat['refType']):
                    errors.append(f'{path}/{fname}.{idx}: xsi:type {child_class} not a {feat["refType"]}')
            else:
                child_class = feat['refType']
                if classes[child_class]['abstract']:
                    errors.append(f'{path}/{fname}.{idx}: abstract {child_class} needs xsi:type'); continue
            new_path = f'{path}/@{fname}.{idx}' if path else f'//@{fname}.{idx}'
            walk(child, new_path, child_class)
        for fname, feat in feats.items():
            if feat['kind'] == 'attr':
                v = elem.get(fname)
                if v is None and feat['lower'] >= 1:
                    errors.append(f'{path}: missing required attribute "{fname}"')
                elif v is not None and feat['refType'] in enums and v not in enums[feat['refType']]:
                    errors.append(f'{path}: {fname}={v!r} not literal of enum {feat["refType"]}')
            else:
                if not feat['containment']:
                    if elem.get(fname) is None and feat['lower'] >= 1:
                        errors.append(f'{path}: missing required reference "{fname}"')
                else:
                    count = counters.get(fname,0)
                    if count < feat['lower']:
                        errors.append(f'{path}: containment "{fname}" needs >={feat["lower"]} (got {count})')
                    if feat['upper'] != -1 and count > feat['upper']:
                        errors.append(f'{path}: containment "{fname}" exceeds {feat["upper"]} (got {count})')

    walk(inst_root, '', inst_root.tag.split('}')[-1])

    for path, (eclass, elem) in list(nodes.items()):
        for fname, feat in all_features(eclass).items():
            if feat['kind'] == 'ref' and not feat['containment']:
                ref_path = elem.get(fname)
                if not ref_path: continue
                t = nodes.get(ref_path)
                if t is None:
                    errors.append(f'{path}.{fname}: unresolved {ref_path}')
                elif not is_kind_of(t[0], feat['refType']):
                    errors.append(f'{path}.{fname}: target {t[0]} not a {feat["refType"]}')

    # OCL invariant equivalents
    def kids(e, n): return [c for c in list(e) if c.tag.split('}')[-1] == n]
    def cont_path(p): return re.sub(r'/@[^/]+\.\d+$', '', p) or ''

    model = inst_root
    service_names = [s.get('name') for s in kids(model,'services')]
    if len(set(service_names)) != len(service_names):
        errors.append('E1 UniqueServiceNames: duplicate service names')

    for s in kids(model,'services'):
        for sc in kids(s,'instrumentations'):
            ns = [m.get('name') for m in kids(sc,'metrics')]
            if len(set(ns)) != len(ns):
                errors.append(f'E2 UniqueMetricNames violated in scope {sc.get("name")}')

    all_metrics = set()
    for si,s in enumerate(kids(model,'services')):
        for sci,sc in enumerate(kids(s,'instrumentations')):
            for mi,_ in enumerate(kids(sc,'metrics')):
                all_metrics.add(f'//@services.{si}/@instrumentations.{sci}/@metrics.{mi}')
    for ar in kids(model,'alertRules'):
        if ar.get('referencedMetric') not in all_metrics:
            errors.append(f'E3 AlertRule {ar.get("name")} references unknown metric')

    for path,(cls,elem) in nodes.items():
        if cls == 'Span' and elem.get('parentSpan'):
            if cont_path(path) != cont_path(elem.get('parentSpan')):
                errors.append(f'E4 Span at {path} parent in different scope')

    for p in kids(model,'pipelines'):
        pn=p.get('name'); sig=p.get('signal')
        rs,es,ps = kids(p,'receivers'),kids(p,'exporters'),kids(p,'processors')
        if not rs or not es: errors.append(f'E5 pipeline {pn}: missing receivers/exporters')
        if sig=='TRACES':
            for r in rs+es:
                if 'Prometheus' in (r.get(f'{{{XSI_NS}}}type') or ''):
                    errors.append(f'E6a pipeline {pn}: Prometheus in TRACES')
        if sig=='METRICS':
            for r in rs+es:
                if 'Jaeger' in (r.get(f'{{{XSI_NS}}}type') or ''):
                    errors.append(f'E6b pipeline {pn}: Jaeger in METRICS')
        if not any('BatchProcessor' in (pr.get(f'{{{XSI_NS}}}type') or '') for pr in ps):
            warnings.append(f'W5 pipeline {pn}: no BatchProcessor')

    for s in kids(model,'services'):
        for sa in kids(s,'sampler'):
            t=sa.get('type'); r=sa.get('ratio')
            if t in ('TRACE_ID_RATIO_BASED','PARENT_BASED'):
                if r is None or not (0.0 <= float(r) <= 1.0):
                    errors.append(f'E7 sampler ratio invalid for {t}')

    for s in kids(model,'services'):
        for sc in kids(s,'instrumentations'):
            for log in kids(sc,'logs'):
                sn=int(log.get('severityNumber'))
                if not (1<=sn<=24): errors.append(f'E8 log severity {sn} out of [1,24]')

    for ar in kids(model,'alertRules'):
        fd=ar.get('forDuration')
        if not re.match(r'^[0-9]+(ms|s|m|h|d)$', fd or ''):
            errors.append(f'E9 AlertRule {ar.get("name")} forDuration {fd!r}')
        if ar.get('severity')=='CRITICAL' and re.match(r'^[0-9]+(ms|s)$', fd or ''):
            warnings.append(f'W6 CRITICAL alert {ar.get("name")} short duration {fd}')

    for path,(cls,elem) in nodes.items():
        if cls=='AttributeAction' and elem.get('action') in ('INSERT','UPDATE','UPSERT') and elem.get('value') is None:
            errors.append(f'E10 AttributeAction at {path} missing value')
        if cls=='Metric' and elem.get('type')=='HISTOGRAM':
            agg=elem.get('aggregation')
            if agg and agg not in ('DEFAULT','EXPLICIT_BUCKET_HISTOGRAM'):
                errors.append(f'E11 Histogram metric at {path}: aggregation={agg}')

    if not kids(model,'services'): errors.append('E12 model has no services')

    for path,(cls,elem) in nodes.items():
        if cls=='Metric' and elem.get('unit') not in UCUM_UNITS:
            warnings.append(f'W1 Metric at {path}: non-UCUM unit {elem.get("unit")!r}')
        if cls=='Metric' and len(kids(elem,'attributes'))>10:
            warnings.append(f'W2 Metric at {path} has >10 attributes')

    for s in kids(model,'services'):
        if s.get('language')!='JAVA':
            warnings.append(f'W3 Service {s.get("name")} language={s.get("language")} not JAVA')

    for path,(cls,elem) in nodes.items():
        if cls=='Attribute':
            k=elem.get('key') or ''
            if any(k.startswith(p) for p in SEMCONV_PREFIXES) and not elem.get('semanticConvention'):
                warnings.append(f'W4 Attribute at {path} key={k} missing semanticConvention')

    for s in kids(model,'services'):
        has=False
        for sc in kids(s,'instrumentations'):
            if kids(sc,'spans') or kids(sc,'metrics') or kids(sc,'logs'): has=True; break
        if not has: warnings.append(f'W7 Service {s.get("name")} emits no telemetry')

    return nodes, errors, warnings


def main():
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(2)
    instance = sys.argv[1]
    ecore = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_ECORE
    nodes, errors, warnings = validate(instance, ecore)
    print(f'Instance: {instance}')
    print(f'Metamodel: {ecore}')
    print(f'Nodes: {len(nodes)}')
    print(f'Errors: {len(errors)}')
    for e in errors: print(f'  X {e}')
    print(f'Warnings: {len(warnings)}')
    for w in warnings: print(f'  ! {w}')
    sys.exit(1 if errors else 0)


if __name__ == '__main__':
    main()
