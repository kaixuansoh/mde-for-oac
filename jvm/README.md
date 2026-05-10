# JVM runtime

Java implementation of the model-driven observability framework. Loads `.observability` instances against the EMF metamodel, validates them via the OCL invariants declared in `metamodel/model/observability.ecore`, and runs five model-to-text emitters that mirror the canonical Acceleo `.mtl` templates under `templates/acceleo/`.

## Modules

```
jvm/
├── pom.xml                            parent POM (Java 21, EMF 2.36)
├── model/                             metamodel loading + OCL-equivalent validation
├── generator/                         five model-to-text emitters
└── cli/                               command-line entry point (picocli)
```

| Module | Responsibility |
|---|---|
| `model` | Bootstraps EMF for standalone use, loads the Ecore metamodel as a dynamic package, loads `.observability` instances, runs structural + OCL-equivalent validation, returns errors and warnings. |
| `generator` | Five `Emitter` implementations — `SpanInstrumentation`, `MetricRegistration`, `LogInstrumentation`, `CollectorYaml`, `PrometheusAlerts` — each mapping the metamodel's elements to a target artefact. |
| `cli` | Picocli command (`obs-generate`) that wires the model + generator together and produces a runnable shaded JAR. |

## Build

```bash
cd jvm
mvn package -DskipTests
```

Outputs `cli/target/obs-generate.jar` — a self-contained executable JAR.

## Run

```bash
# from the framework/ directory
java -jar jvm/cli/target/obs-generate.jar \
     --metamodel metamodel/model/observability.ecore \
     examples/banking.observability \
     out/banking
```

Options:

| Flag | Effect |
|---|---|
| `--metamodel <path>` | path to `observability.ecore` (default: `../metamodel/model/observability.ecore`) |
| `--validate-only` | run validation, skip generation |
| `--strict` | treat warnings as errors |
| `-h`, `--help` | print usage |

## Relationship to the Python prototype

`scripts/generate.py` and the JVM CLI emit byte-identical artefacts modulo trailing newlines. Both implementations are kept in sync as the canonical `.mtl` templates evolve. The JVM project is the long-lived runtime; the Python script remains as a zero-dependency reproducer for users who do not have a JDK.

## Validator parity

The OCL invariants from `observability.ecore` are re-implemented in `model/Validator.java` so the JVM runtime needs no OCL evaluator on the classpath. Any change to the OCL invariants must be reflected in three places:

1. the `EAnnotation` entries in `metamodel/model/observability.ecore` (authoritative spec)
2. `scripts/validate_instance.py` (Python prototype validator)
3. `jvm/model/src/main/java/.../Validator.java` (JVM runtime validator)
