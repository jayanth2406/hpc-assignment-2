#!/usr/bin/env bash
set -euo pipefail
cd "$HOME/Downloads/hpc/hpc2"
mkdir -p results
out=results/q3_raw.csv
if [[ -e "$out" ]]; then echo "Refusing to overwrite $out" >&2; exit 2; fi
javac -Xlint:all Structures.java HPC2.java CorrectnessTest.java
java CorrectnessTest
java HPC2 Q3 "$out" 10000 5

