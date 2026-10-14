#!/usr/bin/env bash
set -euo pipefail
cd "$HOME/Downloads/hpc/hpc2"
mkdir -p results
for out in results/q2_raw.csv results/q4_raw.csv; do if [[ -e "$out" ]]; then echo "Refusing to overwrite $out" >&2; exit 2; fi; done
javac -Xlint:all Structures.java HPC2.java CorrectnessTest.java
java CorrectnessTest
java HPC2 Q2 results/q2_raw.csv 10000 5
java HPC2 Q4 results/q4_raw.csv 10000 5

