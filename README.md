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

A user authors one **M1 instance** describing their services, signals, pipelines, and alerts. The framework validates the instance against the metamodel (including 20 OCL semantic invariants), then runs five model-to-text transformations to produce all M0 artefacts deterministically.

## Repository contents

```
.
├── metamodel/model/observability.ecore      Platform-independent metamodel
│                                            27 EClasses, 13 EEnums, 20 OCL invariants
│
├── examples/                                Sample .observability instance models
│   ├── payment-service.observability        Single-service scenario   ( 66 nodes)
│   ├── ecommerce.observability              Three-service slice       ( 87 nodes)
│   └── banking.observability                Six-service banking slice (151 nodes)
│
├── templates/acceleo/                       OMG-standard model-to-text templates
│   ├── span-instrumentation.mtl             → Java OTel Tracer/Span builders
│   ├── metric-registration.mtl              → Java OTel Meter API
│   ├── log-instrumentation.mtl              → Java OTel logs API
│   ├── collector-yaml.mtl                   → OpenTelemetry Collector YAML
│   └── prometheus-alerts.mtl                → Prometheus alerting rules
│
├── scripts/                                 Python prototype runtime
│   ├── validate_instance.py                 Structural + OCL validator
│   └── generate.py                          Python emitter mirroring the .mtl templates
│
├── jvm/                                     JVM runtime (Maven, EMF, picocli)
│   ├── model/                               EMF bootstrap + Java validator
│   ├── generator/                           Five Emitter implementations
│   └── cli/                                 obs-generate command-line entry point
│
├── generated/                               Reference outputs
│   ├── payment-service/                     5 artefacts
│   ├── ecommerce/                           11 artefacts
│   └── banking/                             20 artefacts
│
├── evaluation/                              Phase III evaluation
│   ├── evaluate.py                          CCR / TCR / CED / ICR harness
│   ├── validators/                          Real-tool correctness checks
│   │   ├── pom.xml                          Pinned opentelemetry-api classpath shim
│   │   └── checks.py                        javac / otelcol / promtool wrappers
│   ├── corpus/negative/                     Minimal instances violating E1–E12
│   └── results/evaluation.json              Latest run
│
└── requirements.txt                         Python deps for the evaluation harness
```

### Metamodel

`observability.ecore` defines the domain language. An `ObservabilityModel` contains `Service`s, `TelemetryPipeline`s, and `AlertRule`s. Each `Service` has an optional `Sampler`, resource attributes, and one or more `InstrumentationScope`s holding `Span`s, `Metric`s, and `Log`s. Pipelines compose abstract `Receiver` / `Processor` / `Exporter` classes; concrete subtypes include `OtlpReceiver`, `JaegerReceiver`, `PrometheusReceiver`, `BatchProcessor`, `MemoryLimiterProcessor`, `AttributesProcessor`, `TailSamplingProcessor`, and `OtlpExporter` / `PrometheusExporter` / `LoggingExporter` / `JaegerExporter`.

Twenty OCL invariants enforce semantic correctness — for example, alert rules must reference a metric that exists somewhere in the same model, ratio-based samplers require a ratio in [0,1], a TRACES pipeline cannot use Prometheus components, and a METRICS pipeline cannot use Jaeger components. Invariants are embedded as `EAnnotation` entries on the relevant EClasses, so an EMF runtime evaluates them automatically during model validation.

### Validator

Two implementations re-implement the same checks against the same metamodel:

- `scripts/validate_instance.py` — zero-dependency Python; validates XML well-formedness, class / feature / enum-literal coverage, cardinality bounds, cross-reference resolution, and every OCL invariant.
- `jvm/model/.../Validator.java` — JVM-side equivalent built on EMF; same diagnostic output (errors must fix, warnings informational), no OCL evaluator on the classpath.

### Generator

The five `.mtl` templates under `templates/acceleo/` are the canonical Phase II artefacts: OMG-standard model-to-text transformations covering Java span instrumentation, Java metric registration, Java log emission, Collector YAML, and Prometheus alerting rules. Two runtimes execute the same logic:

- `scripts/generate.py` — Python prototype, useful for environments without a JDK.
- `jvm/cli/obs-generate.jar` — shaded executable JAR built from the Maven multi-module project. Outputs are byte-identical to the Python prototype modulo trailing newlines.

Both runtimes validate the source model first and refuse to emit if any error-tier OCL invariant fails.

### Evaluation harness

`evaluation/evaluate.py` computes four metrics across two disjoint corpora — three on the positive scenarios under `examples/`, plus a coverage metric on the negative corpus under `evaluation/corpus/negative/`:

| Metric | Corpus | Definition |
|---|---|---|
| **CCR** — Configuration Correctness Rate | positive | `N_correct / N_checkable` |
| **TCR** — Telemetry Coverage Ratio       | positive | `N_present / N_required` (overall + per signal type) |
| **CED** — Configuration Error Density    | positive | `N_errors / N_entities` |
| **ICR** — Invariant Coverage Rate        | negative | `N_caught / N_seeded` |

CCR is computed with real validators rather than syntactic heuristics: generated Java is compiled with `javac` against the pinned `opentelemetry-api` classpath defined in `evaluation/validators/pom.xml`; Collector YAML is checked with `otelcol validate` when the binary is on PATH (with a structural fallback covering pipeline references and component types); Prometheus alert rules are checked with `promtool check rules` when present (with a duration-grammar and rule-structure fallback). The validator used per artefact is recorded in `evaluation/results/evaluation.json`. Artefacts whose validator is unavailable are reported as `unchecked` and excluded from CCR's denominator.

ICR is measured against the negative corpus — one minimal `.observability` instance per OCL error invariant (E1–E12, with E6 split into E6a / E6b for the signal-specific pipeline-component rules). Each subdirectory pairs an `instance.observability` with an `expected.json` declaring the seeded error code; the harness asserts that code appears among the validator's errors. CED and ICR sit on disjoint inputs, so they complement each other rather than overlap.

## Quickstart

### Python prototype

`validate_instance.py` and `generate.py` use the standard library only. The evaluation harness additionally needs PyYAML (for the YAML structural checks) and a JDK + Maven (to resolve the OpenTelemetry API classpath for `javac`).

```bash
# 0. (One-time) create a venv and install harness deps
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt

# 1. Validate an instance against the metamodel
python3 scripts/validate_instance.py examples/payment-service.observability

# 2. Generate Java + YAML artefacts
python3 scripts/generate.py examples/payment-service.observability \
                            generated/payment-service

# 3. Run the full evaluation across all scenarios + the negative corpus
.venv/bin/python evaluation/evaluate.py
```

On the first run, the evaluation harness invokes Maven once under `evaluation/validators/` to materialise `classpath.txt` (an ignored, machine-specific file) containing the resolved `opentelemetry-api` jars. Subsequent runs reuse the cached classpath. Installing `otelcol` (or `otelcol-contrib`) and `promtool` on the PATH switches the YAML checks from the structural fallbacks to the canonical validators.

### JVM runtime (Java 21 + Maven)

```bash
# Build the shaded executable JAR
cd jvm && mvn package -DskipTests

# Validate + generate
java -jar jvm/cli/target/obs-generate.jar \
     --metamodel metamodel/model/observability.ecore \
     examples/banking.observability \
     out/banking
```

CLI flags: `--validate-only`, `--strict` (treat warnings as errors), `--metamodel <path>`, `--help`.
