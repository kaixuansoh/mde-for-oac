"""Focused validator tests covering positive examples and edge cases that the
negative corpus does not exercise (boundary values for ratios, severities, and
forDuration)."""
from __future__ import annotations
import pathlib
import pytest

from validate_instance import validate
from _helpers import write_instance

ROOT = pathlib.Path(__file__).resolve().parent.parent
EXAMPLES = ROOT / 'examples'


@pytest.mark.parametrize(
    'example',
    [pytest.param(p, id=p.stem) for p in sorted(EXAMPLES.glob('*.observability'))],
)
def test_shipped_examples_validate_clean(example: pathlib.Path) -> None:
    _, errors, warnings = validate(example)
    assert errors == [], f'unexpected errors in {example.name}: {errors}'
    assert warnings == [], f'unexpected warnings in {example.name}: {warnings}'


# -----------------------------------------------------------------------
# Sampler ratio boundaries (E7)
# -----------------------------------------------------------------------
def _sampler_model(ratio: str, sampler_type: str = 'TRACE_ID_RATIO_BASED') -> str:
    return f'''
    <services name="S" namespace="com.example" language="JAVA">
      <sampler type="{sampler_type}" ratio="{ratio}"/>
      <instrumentations name="com.example">
        <spans name="op" kind="INTERNAL" statusCode="UNSET"/>
      </instrumentations>
    </services>'''


def test_e7_sampler_ratio_zero_is_valid(tmp_path: pathlib.Path) -> None:
    f = write_instance(tmp_path, _sampler_model('0.0'))
    _, errs, _ = validate(f)
    assert not any('E7' in e for e in errs)


def test_e7_sampler_ratio_one_is_valid(tmp_path: pathlib.Path) -> None:
    f = write_instance(tmp_path, _sampler_model('1.0'))
    _, errs, _ = validate(f)
    assert not any('E7' in e for e in errs)


def test_e7_sampler_ratio_above_one_rejected(tmp_path: pathlib.Path) -> None:
    f = write_instance(tmp_path, _sampler_model('1.001'))
    _, errs, _ = validate(f)
    assert any('E7' in e for e in errs)


def test_e7_sampler_ratio_negative_rejected(tmp_path: pathlib.Path) -> None:
    f = write_instance(tmp_path, _sampler_model('-0.1'))
    _, errs, _ = validate(f)
    assert any('E7' in e for e in errs)


# -----------------------------------------------------------------------
# Log severity range (E8)
# -----------------------------------------------------------------------
def _log_model(sev: int) -> str:
    return f'''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <logs body="hello" severityNumber="{sev}" severityText="INFO"/>
      </instrumentations>
    </services>'''


@pytest.mark.parametrize('sev', [1, 9, 24])
def test_e8_log_severity_in_range_accepted(tmp_path: pathlib.Path, sev: int) -> None:
    f = write_instance(tmp_path, _log_model(sev))
    _, errs, _ = validate(f)
    assert not any('E8' in e for e in errs)


@pytest.mark.parametrize('sev', [0, 25])
def test_e8_log_severity_out_of_range_rejected(tmp_path: pathlib.Path, sev: int) -> None:
    f = write_instance(tmp_path, _log_model(sev))
    _, errs, _ = validate(f)
    assert any('E8' in e for e in errs)


# -----------------------------------------------------------------------
# forDuration pattern (E9)
# -----------------------------------------------------------------------
def _alert_model(for_duration: str, severity: str = 'WARNING') -> str:
    return f'''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <metrics name="m" type="COUNTER" unit="1" valueType="LONG"/>
      </instrumentations>
    </services>
    <alertRules name="A"
                expression="rate(m[1m]) &gt; 0"
                severity="{severity}"
                forDuration="{for_duration}"
                referencedMetric="//@services.0/@instrumentations.0/@metrics.0"/>'''


@pytest.mark.parametrize('dur', ['100ms', '30s', '5m', '2h', '1d'])
def test_e9_for_duration_valid_units(tmp_path: pathlib.Path, dur: str) -> None:
    f = write_instance(tmp_path, _alert_model(dur))
    _, errs, _ = validate(f)
    assert not any('E9' in e for e in errs)


@pytest.mark.parametrize('dur', ['5', '5x', '5min', 'forever', ''])
def test_e9_for_duration_invalid_rejected(tmp_path: pathlib.Path, dur: str) -> None:
    f = write_instance(tmp_path, _alert_model(dur))
    _, errs, _ = validate(f)
    assert any('E9' in e for e in errs)


# -----------------------------------------------------------------------
# Cross-validator interaction tests
# -----------------------------------------------------------------------
def test_w6_only_fires_for_critical(tmp_path: pathlib.Path) -> None:
    """A short forDuration on a WARNING alert should not trip W6."""
    f = write_instance(tmp_path, _alert_model('30s', severity='WARNING'))
    _, _, warns = validate(f)
    assert not any('W6' in w for w in warns)


def test_w1_passes_for_ucum_units(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <metrics name="m" type="COUNTER" unit="ms" valueType="LONG"/>
      </instrumentations>
    </services>'''
    f = write_instance(tmp_path, body)
    _, _, warns = validate(f)
    assert not any('W1' in w for w in warns)


def test_w4_silent_when_semantic_convention_present(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example">
        <spans name="op" kind="INTERNAL" statusCode="UNSET">
          <attributes key="http.request.method"
                      valueType="STRING" value="GET"
                      semanticConvention="http.request.method"/>
        </spans>
      </instrumentations>
    </services>'''
    f = write_instance(tmp_path, body)
    _, _, warns = validate(f)
    assert not any('W4' in w for w in warns)


def test_unknown_xsi_type_is_error(tmp_path: pathlib.Path) -> None:
    body = '''
    <services name="S" namespace="com.example" language="JAVA">
      <instrumentations name="com.example"/>
    </services>
    <pipelines name="p" signal="METRICS">
      <receivers xsi:type="obs:NotARealReceiver" name="r" endpoint="x"/>
      <exporters xsi:type="obs:OtlpExporter" name="e" endpoint="x"/>
    </pipelines>'''
    f = write_instance(tmp_path, body)
    _, errs, _ = validate(f)
    assert any('NotARealReceiver' in e for e in errs)


def test_e12_only_fires_when_services_are_absent(tmp_path: pathlib.Path) -> None:
    """E12 should fire on a services-less model but not on any model with at
    least one service, even if that model otherwise violates W7."""
    has_service = write_instance(tmp_path, '''
      <services name="S" namespace="com.example" language="JAVA">
        <instrumentations name="com.example"/>
      </services>''', name='has')
    _, errs, _ = validate(has_service)
    assert not any('E12' in e for e in errs)
