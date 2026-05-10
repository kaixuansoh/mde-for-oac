# Manual baselines

Reference artefacts representing what a competent engineer would author *by hand* to obtain the same observability coverage that the framework produces from a `.observability` model. These exist for two reasons:

1. **Qualitative comparison** — show the gap between the model-driven approach and conventional Observability-as-Code authoring.
2. **MER measurement** — provide a basis for capturing `T_manual`, the wall-clock time required to author the OaC artefacts manually, which is one of the two inputs to the **Manual Effort Reduction** metric:

   ```
   MER = (T_manual − T_generated) / T_manual
   ```

Until valid `T_manual` values are recorded in `evaluation/baselines.json`, `evaluate.py` reports MER as `null`.

## Layout

```
baselines/
├── README.md                  this file
└── manual/
    ├── payment-service/       hand-authored OaC for the single-service scenario
    │   ├── PaymentServiceInstrumentation.java
    │   ├── PaymentServiceMetrics.java
    │   ├── PaymentServiceLogs.java
    │   ├── otel-collector.yaml
    │   └── prometheus-alerts.yaml
    └── ecommerce/             hand-authored OaC for the three-service scenario
        ├── WebGatewayInstrumentation.java
        ├── WebGatewayMetrics.java
        ├── WebGatewayLogs.java
        ├── OrderServiceInstrumentation.java
        ├── OrderServiceMetrics.java
        ├── OrderServiceLogs.java
        ├── InventoryServiceInstrumentation.java
        ├── InventoryServiceMetrics.java
        ├── InventoryServiceLogs.java
        ├── otel-collector.yaml
        └── prometheus-alerts.yaml
```

The artefacts are functionally equivalent to the framework's `generated/` outputs (same spans, metrics, logs, alerts, pipelines) but written in a more conventional first-pass style with constants, helper methods, and comments typical of human authoring.

## How to capture `T_manual` defensibly

The dissertation's `MER` metric is meaningful only if `T_manual` reflects realistic human effort. Pick one of the protocols below and document the choice in the dissertation's evaluation section.

### Protocol A — direct authoring (most rigorous)

1. Identify a participant who is competent in Java and OpenTelemetry but has *not* seen the model-driven framework.
2. Provide them with the dissertation's intent description for one scenario (a paragraph naming the services, signals, pipelines, and alerts).
3. Ask them to author the OaC artefacts from scratch using only OpenTelemetry's public documentation.
4. Record wall-clock time from start of authoring to a working, validated set of artefacts.
5. Repeat per scenario; ideally repeat with multiple participants and report the median.

### Protocol B — researcher self-timing

The dissertation author times themselves authoring the manual baseline from the intent description, having forgotten the framework's exact output. Less rigorous than Protocol A but reproducible. Document explicitly that the author's familiarity with the domain may bias `T_manual` downward.

### Protocol C — model-based estimation

If neither A nor B is feasible, use a published estimation model:

- **LOC × authoring rate**: industry data suggests ~5–10 minutes per non-trivial line of configuration code authored from scratch (lookup, type, verify, debug). Multiply against the manual baseline LOC.
- **COCOMO II** parameters tuned for configuration code.
- **Function-point** mapping with a configuration-domain weight.

Whichever protocol is used, record:

- the protocol name (A/B/C)
- the participant's experience level
- the literature source if Protocol C
- the raw time and any breakdown by artefact type

These details belong in the dissertation's threats-to-validity section.

## Filling in `evaluation/baselines.json`

```json
{
  "payment-service": {
    "T_manual_seconds": 5400,
    "protocol": "B",
    "notes": "Self-timed by researcher on YYYY-MM-DD; working artefacts validated against scenario intent."
  },
  "ecommerce": {
    "T_manual_seconds": 12600,
    "protocol": "B",
    "notes": "..."
  }
}
```

`evaluate.py` automatically loads `baselines.json` if it exists alongside `evaluate.py`; pass `--baselines <path>` to override.

## What "functionally equivalent" means here

For the comparison to be fair, the manual artefacts must produce the *same observable telemetry* as the framework outputs:

- the same span names, kinds, attributes, and parent-child relationships
- the same metric names, types, units, and dimensions
- the same log severity numbers, bodies, and attributes
- the same Collector pipeline composition (receivers/processors/exporters per signal)
- the same alert names, expressions, severities, durations, labels, and annotations

The artefacts may differ stylistically (constant extraction, helper methods, comments, naming conventions) but the wire-level output of running them must match the framework's output.
