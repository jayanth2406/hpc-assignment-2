Java source and run scripts

Structures.java contains the Q1-Q4 data structures. HPC2.java runs the timed
workloads and writes one raw CSV file per question. CorrectnessTest.java runs
the separate semantic, stress, and structural checks.
The Q1 lists support the full int key range; their internal sentinels use long
values outside that range.

The scripts expect these files in ~/Downloads/hpc/hpc2 and refuse to replace
existing raw result files. Run run_q1.sh, run_q3.sh, or run_q2_q4.sh there.
The last script runs Q2 and Q4 one after the other.

The original stress and invariant suite ran on all three lab hosts. After the
Q1 sentinel revision, the updated full-int-boundary suite ran on Device 1.
Test scope and source checksums are recorded in
../results/correctness_validation.txt. These are stress and invariant checks,
not a formal linearizability proof.

No compiled Java class files are included.
