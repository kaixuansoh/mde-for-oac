# Automated Observability Configuration in Cloud Native Systems

A model-driven engineering framework for observability-as-code generation.

## Background

Cloud-native systems rely on logs, metrics, traces, and alerts for operational visibility. In practice, these telemetry artefacts are configured manually across heterogeneous services, environments, and tools, leading to high operational overhead, configuration drift, inconsistent instrumentation, and limited scalability. Standards such as OpenTelemetry reduce vendor lock-in but do not remove the configuration burden — engineers still author Collector pipelines, alert rules, and instrumentation code by hand.

This project applies **Model-Driven Engineering (MDE)** to lift observability configuration one layer of abstraction higher: capture the *intent* once in a platform-independent model, then mechanically derive every concrete Observability-as-Code (OaC) artefact via model-to-text transformation.

## Approach

The framework follows the OMG four-layer modelling architecture:

```
M3  Ecore (provided by EMF)                          ← language for metamodels
M2  observability.ecore                              ← this domain's metamodel
M1  *.observability instances                        ← user-authored models
M0  Java OTel SDK + Collector YAML + Prometheus      ← generated artefacts
        + actual telemetry at runtime
```

A user authors one **M1 instance** describing their services, signals, pipelines, and alerts. The framework validates the instance against the metamodel (including 20 OCL semantic invariants), then runs four model-to-text transformations to produce all M0 artefacts deterministically.

## Repository contents

```
.
├── metamodel/model/observability.ecore      Platform-independent metamodel
│                                            27 EClasses, 13 EEnums, 20 OCL invariants
│
├── examples/                                Sample .observability instance models
│   ├── payment-service.observability        Single-service scenario (66 nodes)
│   └── ecommerce.observability              Three-service slice (87 nodes)
│
├── templates/acceleo/                       OMG-standard model-to-text templates
│   ├── span-instrumentation.mtl             → Java OTel Tracer/Span builders
│   ├── metric-registration.mtl              → Java OTel Meter API
│   ├── collector-yaml.mtl                   → OpenTelemetry Collector YAML
│   └── prometheus-alerts.mtl                → Prometheus alerting rules
│
├── scripts/
│   ├── validate_instance.py                 Structural + OCL validator
│   └── generate.py                          Python prototype mirroring the .mtl templates
│
├── generated/                               Reference outputs from generate.py
│   ├── payment-service/                     4 artefacts, 225 lines
│   └── ecommerce/                           8 artefacts, 354 lines
│
└── evaluation/                              Phase III evaluation
    ├── evaluate.py                          CCR / TCR / CED / MER harness
    └── results/evaluation.json              Latest run
```

### Metamodel

`observability.ecore` defines the domain language. An `ObservabilityModel` contains `Service`s, `TelemetryPipeline`s, and `AlertRule`s. Each `Service` has an optional `Sampler`, resource attributes, and one or more `InstrumentationScope`s holding `Span`s, `Metric`s, and `Log`s. Pipelines compose abstract `Receiver` / `Processor` / `Exporter` classes; concrete subtypes include `OtlpReceiver`, `JaegerReceiver`, `PrometheusReceiver`, `BatchProcessor`, `MemoryLimiterProcessor`, `AttributesProcessor`, `TailSamplingProcessor`, and `OtlpExporter` / `PrometheusExporter` / `LoggingExporter` / `JaegerExporter`.

Twenty OCL invariants enforce semantic correctness — for example, alert rules must reference a metric that exists somewhere in the same model, ratio-based samplers require a ratio in [0,1], a TRACES pipeline cannot use Prometheus components, and a METRICS pipeline cannot use Jaeger components. Invariants are embedded as `EAnnotation` entries on the relevant EClasses, so an EMF runtime evaluates them automatically during model validation.

### Validator

`scripts/validate_instance.py` is a standalone validator that loads the metamodel and any `.observability` instance and checks XML well-formedness, class / feature / enum-literal coverage, cardinality bounds, cross-reference resolution, and every OCL invariant. It returns errors (must fix) and warnings separately, without requiring an Eclipse runtime.

### Generator

The four `.mtl` templates under `templates/acceleo/` are the canonical Phase II artefacts: OMG-standard model-to-text transformations covering Java span instrumentation, Java metric registration, Collector YAML, and Prometheus alerting rules. `scripts/generate.py` is a Python prototype that mirrors the same logic so the pipeline runs end-to-end without an Eclipse + Acceleo runtime. The generator runs the validator first and refuses to emit if the source model has any error-tier violations.

### Evaluation harness

`evaluation/evaluate.py` computes four metrics:

| Metric | Definition |
|---|---|
| **CCR** — Configuration Correctness Rate | `N_correct / N_total` |
| **TCR** — Telemetry Coverage Ratio       | `N_present / N_required` (overall + per signal type) |
| **CED** — Configuration Error Density    | `N_errors / N_entities` |
| **MER** — Manual Effort Reduction        | `(T_manual − T_generated) / T_manual` |

## Quickstart

Requires Python 3.9+ and PyYAML.

```bash
# 1. Validate an instance against the metamodel
python3 scripts/validate_instance.py examples/payment-service.observability

# 2. Generate Java + YAML artefacts
python3 scripts/generate.py examples/payment-service.observability \
                            generated/payment-service

# 3. Run the full evaluation across all scenarios
python3 evaluation/evaluate.py
```
