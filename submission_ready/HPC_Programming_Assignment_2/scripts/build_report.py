#!/usr/bin/env python3
"""Build a data-led HTML report from validated HPC2 averages and GNUplot images."""
import argparse
import csv
import html
import os
from datetime import datetime
from pathlib import Path

QUESTIONS = {
    "Q1": ("Concurrent doubly linked list", ("lazy", "lockfree")),
    "Q2": ("Concurrent stack", ("lock", "treiber")),
    "Q3": ("Concurrent queue", ("lock", "ms")),
    "Q4": ("Elimination-backoff stack", ("treiber", "elimination")),
}
WORKLOADS = ("0C-50I-50D", "50C-25I-25D", "100C-0I-0D")


def e(value):
    return html.escape(str(value))


def load_averages(path):
    with path.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    if len(rows) != 432:
        raise SystemExit(f"Expected 432 average rows, found {len(rows)}")
    return rows


def row_for(rows, q, impl, n, workload, threads):
    matches = [r for r in rows if r["question"] == q
               and r["implementation"] == impl and int(r["n"]) == n
               and r["workload"] == workload and int(r["threads"]) == threads]
    if len(matches) != 1:
        raise SystemExit(f"Expected one average row for {q}/{impl}/N={n}/{workload}/T={threads}")
    return matches[0]


def build(args):
    rows = load_averages(args.averages)
    validation = args.validation.read_text(encoding="utf-8")
    if "PASS" not in validation or "2160" not in validation:
        raise SystemExit("Validation report does not confirm all 2160 raw runs")

    parts = ["""<!doctype html><html><head><meta charset="utf-8">
<style>
@page { size: A4; margin: 16mm 17mm; }
body { font-family: Georgia, 'Times New Roman', serif; font-size: 12pt; line-height: 1.35; color: #20242a; }
h1 { font-size: 23pt; color: #17324d; margin-bottom: 4pt; }
h2 { font-size: 18pt; color: #17324d; border-bottom: 1px solid #9ab0c3; padding-bottom: 4pt; }
h3 { font-size: 13pt; margin-bottom: 4pt; color: #254d70; }
p, li { margin-top: 4pt; margin-bottom: 5pt; }
table { width: 100%; border-collapse: collapse; font-size: 9.5pt; margin: 7pt 0 12pt; }
th, td { border: 1px solid #aeb8c2; padding: 4pt 5pt; text-align: right; }
th { background: #eaf0f5; color: #17324d; }
th:first-child, td:first-child { text-align: left; }
small { font-size: 9pt; color: #4d5965; }
.cover { min-height: 235mm; display: flex; flex-direction: column; justify-content: center; }
.question { page-break-before: always; }
.figures { display: flex; gap: 7mm; align-items: flex-start; }
.figure { width: 50%; }
.figure img { width: 100%; height: auto; }
.caption { font-size: 9pt; color: #4d5965; }
.note { background: #f1f5f8; padding: 7pt 9pt; border-left: 3px solid #477da8; }
</style></head><body>"""]

    max_records = []
    observations = {
        "Q1": "The lazy list scales across all six size/workload settings at 16 threads. The snapshot list's strongest result is read-only; its copy-on-write updates become expensive as the list grows, and four of the six 16-thread cases fall below their one-thread baselines.",
        "Q2": "The lock-based stack is below its one-thread baseline in all six settings at 16 threads. Treiber performs much better for read-only peek workloads, but its shared top reference remains a contention point for push/pop workloads.",
        "Q3": "The lock-based queue is below its one-thread baseline in all six settings at 16 threads. Michael-Scott scales strongly for read-only peek workloads, while mixed workloads contend on shared queue state. The longer Q3 runs are included rather than discarded.",
        "Q4": "At 16 threads, elimination beats Treiber in four of six matched cases, with ratios from 0.92x to 1.86x. This is a workload-dependent gain, not a consistent win; peek operations still use the central stack."
    }

    for q, (name, impls) in QUESTIONS.items():
        candidates = [r for r in rows if r["question"] == q and int(r["threads"]) > 1]
        best = max(candidates, key=lambda r: float(r["speedup"]))
        max_records.append((q, name, best))

    parts.append(f"""<section class="cover"><h1>Concurrent Data Structures in Java</h1>
<p><b>HPC Programming Assignment 2</b></p>
<p>Performance and scalability report</p>
<p>Experiment dates: 2026-10-08 to 2026-10-13</p>
<p>We compared the two implementations for each structure using concurrent workloads on the IT Lab systems. Each speedup uses the one-thread result for the same implementation, problem size, and workload.</p>
<h2>Results at a glance</h2><table><tr><th>Problem</th><th>Highest observed speedup</th><th>Configuration</th></tr>""")
    for q, name, best in max_records:
        config = f'{best["implementation"]}, N={best["n"]}, {best["workload"]}, {best["threads"]} threads'
        parts.append(f"<tr><td>{e(q)}: {e(name)}</td><td>{float(best['speedup']):.2f}x</td><td>{e(config)}</td></tr>")
    parts.append("""</table><p class="note">Each value is the largest measured speedup for that problem, not a general scaling claim. Workload and problem size matter; the sections below show cases that scale less well.</p>
<p><small>Machines: three identical Intel Core i7-12700 systems, 12 cores / 20 logical CPUs, 15.3 GiB RAM, Ubuntu, OpenJDK 1.8.0_352.</small></p></section>""")

    parts.append("""<section class="question"><h2>Method and implementation notes</h2>
<h3>Experimental protocol</h3>
<ul>
<li>Problem sizes: N = 1,000 and 10,000, with 50% prefilled before each run.</li>
<li>Workloads: 0C-50I-50D, 50C-25I-25D, and 100C-0I-0D.</li>
<li>Thread counts: 1, 2, 4, 6, 8, 10, 12, 14, and 16; five repetitions per configuration.</li>
<li>Workers use a shared 10-second deadline based on System.nanoTime(). Prefill, trace generation, worker creation, and CSV output are outside the workload. Elapsed time is recorded through worker completion; six Q3 runs exceeded 10.05 seconds, with a maximum of 10.668 seconds. Those measured durations were retained in the calculations.</li>
<li>Throughput is completed operation calls divided by the measured elapsed seconds, in millions of operations per second (MOPS). Speedup is the ratio of average throughput at P threads to the matching one-thread average.</li>
</ul>
<h3>Algorithms and linearization points</h3>
<ul>
<li><b>Q1 list:</b> the lazy list inserts at the protected predecessor-link update and deletes at the logical mark; unsuccessful insert/delete and contains linearize at the decision made in the validated, locked window. The non-blocking version publishes immutable doubly linked snapshots with one state CAS; successful updates linearize at that CAS, while unsuccessful updates and contains linearize at the observed snapshot read. It has no transient mark/help/unlink phase: delete omits the key from a new version and atomically publishes that version. In-flight readers may finish on an older immutable version. Copy-on-write updates are O(N), a deliberate correctness-first tradeoff rather than the Sundell-Tsigas linked-node algorithm.</li>
<li><b>Q2 stack:</b> lock-based push/pop linearize at the protected top mutation, with empty-pop and peek at the protected top observation. Treiber push/pop linearize at a successful top CAS; empty-pop and peek linearize at the atomic top observation.</li>
<li><b>Q3 queue:</b> lock-based enqueue/dequeue linearize at the protected link/head update, with empty-dequeue and peek at the protected head observation. Michael-Scott enqueue linearizes at the successful tail.next CAS and successful dequeue at the head CAS; empty-dequeue and peek linearize at the validated head/next observation.</li>
<li><b>Q4 elimination stack:</b> operations that use the central stack linearize at the Treiber top CAS (or the top observation for empty-pop/peek). A matched push/pop pair linearizes at the successful WAITING-to-MATCHED CAS on its shared elimination offer.</li>
</ul>
<p>Every run completed and passed its post-run structure check. The base stress and invariant suite ran on all three hosts. After the sentinel change, the extended Q1 suite passed on Device 1 and covered empty and populated lists, duplicate keys, Integer.MIN_VALUE and Integer.MAX_VALUE insertion/lookup/deletion, concurrent list membership, stack value preservation, per-producer queue order, elimination-stack value exchange, and mixed-operation stress. Both lists use long-valued sentinels outside the int range. These checks are not a formal linearizability proof. CAS retries were not counted, so contention is discussed qualitatively.</p>
<p>The revised Q1 raw file records the actual host name, inhydmupc0899-l. The original Q2-Q4 raw files retain "unknown" because those Java processes did not receive a HOSTNAME value. Question-to-host assignments are recorded in the machine inventory and averaged results.</p></section>""")

    for q, (name, impls) in QUESTIONS.items():
        throughput_image = os.path.relpath(args.plots / (q + "_throughput.png"), args.output.parent).replace("\\", "/")
        speedup_image = os.path.relpath(args.plots / (q + "_speedup.png"), args.output.parent).replace("\\", "/")
        parts.append(f'<section class="question"><h2>{e(q)}: {e(name)}</h2>')
        parts.append("<p>Throughput and speedup are shown for both implementations, all three workload mixes, and both problem sizes. The speedup plots include the ideal linear reference.</p>")
        parts.append(f'<div class="figures"><div class="figure"><img src="{e(throughput_image)}"><p class="caption">Average throughput (MOPS).</p></div>')
        parts.append(f'<div class="figure"><img src="{e(speedup_image)}"><p class="caption">Throughput speedup versus one thread.</p></div></div>')
        parts.append('<h3>Representative mixed-workload results, N = 10,000</h3>')
        parts.append("<table><tr><th>Implementation</th><th>Threads</th><th>Avg throughput (MOPS)</th><th>Speedup</th></tr>")
        for impl in impls:
            for threads in (1, 4, 8, 16):
                r = row_for(rows, q, impl, 10000, "50C-25I-25D", threads)
                parts.append(f'<tr><td>{e(impl)}</td><td>{threads}</td><td>{float(r["avg_throughput_mops"]):.4f}</td><td>{float(r["speedup"]):.3f}x</td></tr>')
        parts.append("</table>")
        for impl in impls:
            all_impl = [r for r in rows if r["question"] == q and r["implementation"] == impl]
            best = max(all_impl, key=lambda r: float(r["avg_throughput_mops"]))
            parts.append(f'<p><b>{e(impl)}:</b> peak average throughput {float(best["avg_throughput_mops"]):.4f} MOPS at N={best["n"]}, {e(best["workload"])}, {best["threads"]} threads.</p>')
            baseline = {(int(r["n"]), r["workload"]): float(r["avg_throughput_mops"])
                        for r in all_impl if int(r["threads"]) == 1}
            at16 = [r for r in all_impl if int(r["threads"]) == 16]
            speedups16 = [float(r["avg_throughput_mops"]) / baseline[(int(r["n"]), r["workload"])]
                          for r in at16]
            mean16 = sum(speedups16) / len(speedups16)
            below = sum(value < 1.0 for value in speedups16)
            parts.append(f'<p>At 16 threads, <b>{e(impl)}</b> averaged {mean16:.2f}x speedup across the six size/workload settings (range {min(speedups16):.2f}x to {max(speedups16):.2f}x); {below} of six settings were below its one-thread throughput.</p>')
        if q == "Q4":
            q4_16 = {(int(r["n"]), r["workload"], r["implementation"]): float(r["avg_throughput_mops"])
                     for r in rows if r["question"] == q and int(r["threads"]) == 16}
            ratios = [q4_16[(n, w, "elimination")] / q4_16[(n, w, "treiber")]
                      for n in (1000, 10000) for w in WORKLOADS]
            parts.append(f'<p>At 16 threads, elimination outperformed Treiber in {sum(ratio > 1.0 for ratio in ratios)} of six matched configurations; the throughput ratio ranged from {min(ratios):.2f}x to {max(ratios):.2f}x, so its benefit was workload-dependent.</p>')
        parts.append(f'<p class="note">{e(observations[q])}</p></section>')

    parts.append("""<section class="question"><h2>Interpretation and limitations</h2>
<p>Compare each curve within its host and matching implementation, size, and workload. Read-heavy structures may scale differently from update-heavy structures: lock-based paths serialize on shared locks, while non-blocking paths can experience failed CAS retries and cache-coherence traffic on hot references. More threads can improve aggregate throughput, saturate, or regress depending on the critical path and scheduling overhead.</p>
<p>The Q1 snapshot design trades update cost for immutable versions and a single CAS publication. The lock-based stack and queue serialize updates, while Treiber and Michael-Scott avoid a global lock but still contend on shared atomic references. The measurements reflect these tradeoffs for the tested workloads; they should not be generalized beyond this setup.</p>
<p>Raw operation counts, elapsed times, and run flags are included in the CSV files. Averages and speedups were calculated from those rows. The three machines reported the same CPU model, but each question's speedup is based only on runs from its assigned host.</p>
<p>The accompanying files include Java sources, correctness tests, launch and analysis scripts, GNUplot charts, raw data, averages, validation records, the machine inventory, and this report. Compiled class files are not included.</p></section></body></html>""")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(parts), encoding="utf-8")
    print(f"Wrote {args.output}")


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--averages", type=Path, default=Path("results/averages.csv"))
    p.add_argument("--validation", type=Path, default=Path("results/validation.txt"))
    p.add_argument("--plots", type=Path, default=Path("results/plots"))
    p.add_argument("--output", type=Path, default=Path("results/report.html"))
    build(p.parse_args())


if __name__ == "__main__":
    main()
