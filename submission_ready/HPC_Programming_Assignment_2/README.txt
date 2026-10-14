HPC Programming Assignment 2
Concurrent data structures in Java

Overview
This submission implements and measures four concurrent data structure
problems. The Java sources, launch scripts, raw measurements, averages, plots,
validation records, machine inventory, and report are included in this archive.

Implementations
Q1  Lazy synchronized doubly linked set and CAS-published immutable snapshots
Q2  ReentrantLock stack and Treiber stack
Q3  ReentrantLock queue and Michael-Scott queue
Q4  Treiber stack and elimination-backoff stack

The Q1 snapshot implementation rebuilds the full list after each successful
update, so its update cost is O(N). It is a simple non-blocking design, not the
Sundell-Tsigas linked-node algorithm. The benchmark report calls out this
tradeoff when comparing read-heavy and update-heavy workloads.
Both Q1 lists use long-valued sentinels outside the int key range, so all int
keys, including Integer.MIN_VALUE and Integer.MAX_VALUE, are supported.

Experimental setup
Each question used N=1,000 and N=10,000, a 50% prefill, three workload mixes,
thread counts 1, 2, 4, 6, 8, 10, 12, 14, and 16, and five runs per
configuration. Each run targeted 10 seconds. System.nanoTime() was used for
timing; prefill, trace creation, thread setup, and file output were outside the
workload interval. Throughput is completed operations divided by measured
elapsed time. Speedup compares average throughput with the matching one-thread
run for the same implementation, size, and workload.

Validation and interpretation
All four raw files contain 540 runs each. The validation script checked
configuration coverage, repetition counts, elapsed times, throughput values,
and the verified flags. The stress and invariant suite passed on all three lab
hosts. After the Q1 sentinel revision, the extended full-int-boundary tests
also passed on Device 1. These are not a formal linearizability proof. See
results/correctness_validation.txt for the test scope.

The revised Q1 run records the host name inhydmupc0899-l. The original Q2-Q4
runs have "unknown" in their raw host fields because those Java processes did
not receive a HOSTNAME value. The host allocation is documented in
machine_specs.txt and in the averaged results. A small number of Q3 runs took
longer than 10 seconds to complete; their measured elapsed times were kept in
the raw data and used in the throughput calculations.

Results
results/q1_raw.csv through q4_raw.csv contain individual runs.
results/all_raw_runs.csv combines the raw rows, and results/averages.csv
contains mean throughput and matched speedup calculations. The eight PNG
charts were generated with scripts/plot_results.gp and are in results/plots/.
The PDF report is in results/.

Reproduction
On a lab host, place Structures.java, HPC2.java, CorrectnessTest.java, and the
appropriate run script in ~/Downloads/hpc/hpc2. Then run the question's script:
run_q1.sh for Q1, run_q3.sh for Q3, or run_q2_q4.sh for Q2 followed by Q4.
These scripts refuse to overwrite existing raw files. After copying results
back to the laptop, use scripts/summarize.py and scripts/plot_results.gp to
rebuild the tables and charts. The Python report builder creates report.html;
the PDF is printed from that HTML using a browser.

Compiled .class files and executables are not included.
