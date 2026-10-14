#!/usr/bin/env python3
"""Validate a partial checkpoint without treating it as a complete benchmark."""
import csv
import math
import sys
from pathlib import Path

EXPECTED = {
    "Q1": ("lazy", "lockfree"),
    "Q2": ("lock", "treiber"),
    "Q3": ("lock", "ms"),
    "Q4": ("treiber", "elimination"),
}
HEADER = (
    "question", "implementation", "n", "workload", "threads", "repetition",
    "elapsed_ns", "operations", "throughput_mops", "verified", "host",
)


def check(path):
    with path.open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        if tuple(reader.fieldnames or ()) != HEADER:
            raise ValueError(f"{path}: bad CSV header")
        rows = list(reader)
    if not rows:
        raise ValueError(f"{path}: no data rows")
    question = rows[0]["question"]
    if question not in EXPECTED or any(row["question"] != question for row in rows):
        raise ValueError(f"{path}: mixed or unknown question")
    seen = set()
    for index, row in enumerate(rows, 2):
        key = (row["implementation"], int(row["n"]), row["workload"],
               int(row["threads"]), int(row["repetition"]))
        if row["implementation"] not in EXPECTED[question]:
            raise ValueError(f"{path}:{index}: unexpected implementation")
        if key in seen:
            raise ValueError(f"{path}:{index}: duplicate run key")
        seen.add(key)
        elapsed, operations = int(row["elapsed_ns"]), int(row["operations"])
        measured = float(row["throughput_mops"])
        expected = operations / (elapsed / 1e9) / 1e6
        if row["verified"].lower() != "true" or operations <= 0:
            raise ValueError(f"{path}:{index}: invalid status/count")
        if not 9_800_000_000 <= elapsed <= 12_000_000_000:
            raise ValueError(f"{path}:{index}: unexpected elapsed {elapsed}")
        if not math.isclose(measured, expected, rel_tol=1e-6, abs_tol=1e-8):
            raise ValueError(f"{path}:{index}: throughput formula mismatch")
    print(f"PASS {question}: {len(rows)} unique verified rows; checkpoint is partial.")


def main():
    if len(sys.argv) < 2:
        raise SystemExit("Usage: py scripts/check_checkpoint.py <raw-csv> [...]")
    for arg in sys.argv[1:]:
        check(Path(arg))


if __name__ == "__main__":
    main()

