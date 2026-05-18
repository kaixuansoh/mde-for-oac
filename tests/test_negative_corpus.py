"""Drive the negative corpus through the validator and assert each seeded
invariant fires. This is the test-shaped twin of the ICR metric reported by
evaluation/evaluate.py: every E* / W* directory under evaluation/corpus/negative
is expected to be caught."""
from __future__ import annotations
import json, pathlib, re
import pytest

from validate_instance import validate

ROOT = pathlib.Path(__file__).resolve().parent.parent
CORPUS = ROOT / 'evaluation' / 'corpus' / 'negative'


def _cases():
    if not CORPUS.exists():
        return []
    out = []
    for d in sorted(CORPUS.iterdir()):
        inst = d / 'instance.observability'
        exp = d / 'expected.json'
        if inst.exists() and exp.exists():
            out.append(pytest.param(d, id=d.name))
    return out


@pytest.mark.parametrize('case_dir', _cases())
def test_seeded_invariant_caught(case_dir: pathlib.Path) -> None:
    expected = json.loads((case_dir / 'expected.json').read_text())
    code = expected['code']
    _, errors, warnings = validate(case_dir / 'instance.observability')
    pat = re.compile(rf'\b{re.escape(code)}\b')
    target = warnings if code.startswith('W') else errors
    caught = [m for m in target if pat.search(m)]
    assert caught, (
        f'expected code {code} to appear in '
        f"{'warnings' if code.startswith('W') else 'errors'}; "
        f'got errors={errors!r} warnings={warnings!r}'
    )


def test_corpus_is_non_empty() -> None:
    """Guard against the corpus being wiped — the regression that motivated
    adding tests in the first place."""
    cases = list(CORPUS.iterdir()) if CORPUS.exists() else []
    assert len(cases) >= 12, f'expected >=12 negative cases, found {len(cases)}'
