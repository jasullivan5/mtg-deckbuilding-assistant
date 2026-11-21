"""Reuse the strict timing comparator for the three source-grounded legacy-helper corrections.

All eligibility, historical assurance, admissions and unrelated mechanics remain exact.
Only revision-bound native structures for the explicitly corrected cohort may differ.
"""
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location("exile_timing_comparator", Path(__file__).with_name("compare-exile-timing.py"))
comparator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(comparator)
comparator.CORRECTED = {"Fiend Hunter", "Savior of Ollenbock", "Werefox Bodyguard"}
comparator.__doc__ = __doc__

if __name__ == "__main__":
    comparator.main()
