#!/usr/bin/env python3
"""Validate full HPC2 raw runs and produce combined and averaged CSVs."""
import argparse
import csv
import math
from collections import defaultdict
from pathlib import Path

THREADS = (1, 2, 4, 6, 8, 10, 12, 14, 16)
SIZES = (1000, 10000)
WORKLOADS = ("0C-50I-50D", "50C-25I-25D", "100C-0I-0D")
IMPLS = {
    "Q1": ("lazy", "lockfree"),
    "Q2": ("lock", "treiber"),
    "Q3": ("lock", "ms"),
    "Q4": ("treiber", "elimination"),
}
HEADER = (
    "question", "implementation", "n", "workload", "threads", "repetition",
    "elapsed_ns", "operations", "throughput_mops", "verified", "host",
)
HOSTS = {
    "Q1": "Device 1 (Core i7-12700)",
    "Q2": "Device 3 (Core i7-12700)",
    "Q3": "Device 2 (Core i7-12700)",
    "Q4": "Device 3 (Core i7-12700)",
}


def fail(message):
    raise SystemExit("ERROR: " + message)


def load_question(path, question):
    with path.open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream)
        if tuple(reader.fieldnames or ()) != HEADER:
            fail(f"{path}: unexpected CSV header")
        rows = list(reader)

    expected = {
        (impl, n, workload, threads, rep)
        for impl in IMPLS[question]
        for n in SIZES
        for workload in WORKLOADS
        for threads in THREADS
        for rep in range(1, 6)
    }
    seen = set()
    normalized = []
    for line_no, row in enumerate(rows, 2):
        try:
            key = (
                row["implementation"], int(row["n"]), row["workload"],
                int(row["threads"]), int(row["repetition"]),
            )
            elapsed = int(row["elapsed_ns"])
            operations = int(row["operations"])
            mops = float(row["throughput_mops"])
        except (ValueError, KeyError) as exc:
            fail(f"{path}:{line_no}: invalid numeric field: {exc}")
        if row["question"] != question:
            fail(f"{path}:{line_no}: expected {question}, got {row['question']}")
        if key not in expected:
            fail(f"{path}:{line_no}: unexpected configuration {key}")
        if key in seen:
            fail(f"{path}:{line_no}: duplicate configuration {key}")
        seen.add(key)
        if row["verified"].lower() != "true":
            fail(f"{path}:{line_no}: run not verified")
        if not 9_800_000_000 <= elapsed <= 12_000_000_000:
            fail(f"{path}:{line_no}: elapsed time {elapsed} ns outside 9.8-12 s")
        if operations <= 0:
            fail(f"{path}:{line_no}: operation count must be positive")
        calculated = operations / (elapsed / 1_000_000_000) / 1_000_000
        if not math.isclose(mops, calculated, rel_tol=1e-6, abs_tol=1e-8):
            fail(f"{path}:{line_no}: throughput does not match operations/time")
        normalized.append(row)

    missing = expected - seen
    if missing:
        fail(f"{path}: {len(missing)} required runs are missing; first: {sorted(missing)[:3]}")
    if len(rows) != 540:
        fail(f"{path}: expected 540 data rows, found {len(rows)}")
    return normalized


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--q1", type=Path, required=True)
    parser.add_argument("--q2", type=Path, required=True)
    parser.add_argument("--q3", type=Path, required=True)
    parser.add_argument("--q4", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, default=Path("results"))
    args = parser.parse_args()

    raw = []
    per_question = {}
    for question in ("Q1", "Q2", "Q3", "Q4"):
        rows = load_question(getattr(args, question.lower()), question)
        per_question[question] = rows
        raw.extend(rows)

    args.out_dir.mkdir(parents=True, exist_ok=True)
    with (args.out_dir / "all_raw_runs.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=HEADER)
        writer.writeheader()
        writer.writerows(raw)

    groups = defaultdict(list)
    for row in raw:
        key = (row["question"], row["implementation"], int(row["n"]),
               row["workload"], int(row["threads"]))
        groups[key].append(row)

    average_rows = []
    for (q, impl, n, workload, threads), runs in sorted(groups.items()):
        if len(runs) != 5:
            fail(f"{q}/{impl}/N={n}/{workload}/T={threads}: expected 5 runs")
        avg_elapsed = sum(int(r["elapsed_ns"]) for r in runs) / 5
        avg_ops = sum(int(r["operations"]) for r in runs) / 5
        avg_mops = sum(float(r["throughput_mops"]) for r in runs) / 5
        average_rows.append({
            "question": q, "implementation": impl, "n": n,
            "workload": workload, "threads": threads, "runs": 5,
            "avg_elapsed_ns": f"{avg_elapsed:.3f}",
            "avg_operations": f"{avg_ops:.3f}",
            "avg_throughput_mops": f"{avg_mops:.9f}",
            "speedup": "", "host": HOSTS[q],
        })

    baseline = {}
    for row in average_rows:
        if row["threads"] == 1:
            key = (row["question"], row["implementation"], row["n"], row["workload"])
            baseline[key] = float(row["avg_throughput_mops"])
    for row in average_rows:
        key = (row["question"], row["implementation"], row["n"], row["workload"])
        row["speedup"] = f"{float(row['avg_throughput_mops']) / baseline[key]:.9f}"

    avg_header = (
        "question", "implementation", "n", "workload", "threads", "runs",
        "avg_elapsed_ns", "avg_operations", "avg_throughput_mops", "speedup", "host",
    )
    with (args.out_dir / "averages.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=avg_header)
        writer.writeheader()
        writer.writerows(average_rows)

    with (args.out_dir / "validation.txt").open("w", encoding="utf-8") as stream:
        stream.write("HPC Assignment 2 benchmark validation: PASS\n")
        stream.write("Raw data: 4 questions, 540 runs each, 2160 total runs.\n")
        stream.write("Each required implementation/size/workload/thread configuration has repetitions 1-5.\n")
        stream.write("Elapsed time, operation counts, throughput formula, and verified flags passed checks.\n")
        stream.write("Averages are arithmetic means of the five per-run throughput measurements.\n")
        stream.write("Speedup = average throughput at P threads / corresponding one-thread average.\n")
        stream.write("Average rows: 432.\n")
    print("PASS: 2160 raw runs validated; wrote combined data, 432 averages, and validation.txt.")


if __name__ == "__main__":
    main()

