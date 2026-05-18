"""Small helpers for emitter / validator tests.

Builds minimal .observability XML strings programmatically so each test can
seed exactly the structure it needs without dragging in the full example
scenarios.
"""
from __future__ import annotations
import pathlib, textwrap, xml.etree.ElementTree as ET

NS_PROLOGUE = textwrap.dedent('''\
    <?xml version="1.0" encoding="UTF-8"?>
    <obs:ObservabilityModel xmi:version="2.0"
        xmlns:xmi="http://www.omg.org/XMI"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:obs="http://dissertation.um.edu.my/observability/1.0"
        name="{name}" version="1.0.0">
    {body}
    </obs:ObservabilityModel>
    ''')


def build_model(body: str, *, name: str = 'test-model') -> ET.Element:
    """Wrap a body XML fragment in the canonical ObservabilityModel envelope
    and return the parsed root element."""
    return ET.fromstring(NS_PROLOGUE.format(name=name, body=body))


def write_instance(tmp_path: pathlib.Path, body: str, *, name: str = 'test') -> pathlib.Path:
    """Write a built model to disk and return the path (for validator tests
    that take a file path)."""
    f = tmp_path / f'{name}.observability'
    f.write_text(NS_PROLOGUE.format(name=name, body=body))
    return f
