"""Pytest configuration — wire sys.path so tests can import the Python
prototype as plain modules without packaging it."""
import pathlib, sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
for sub in ('scripts', 'evaluation', 'evaluation/validators'):
    p = str(ROOT / sub)
    if p not in sys.path:
        sys.path.insert(0, p)
