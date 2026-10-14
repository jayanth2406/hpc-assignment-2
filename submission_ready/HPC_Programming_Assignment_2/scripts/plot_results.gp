# Run from the assignment root after summarize.py succeeds:
# gnuplot -e "DATA='results/averages.csv'; OUT='results/plots'" scripts/plot_results.gp
if (!exists("DATA")) DATA = "results/averages.csv"
if (!exists("OUT")) OUT = "results/plots"

set datafile separator comma
set terminal pngcairo size 1900,1150 enhanced font "Liberation Sans,10"
set border linewidth 1
set grid ytics lc rgb "#dddddd"
set key top left font ",8"
set xlabel "Threads"
set xtics (1,2,4,6,8,10,12,14,16)
set xrange [0:17]
set style line 1 lc rgb "#1769aa" lw 2 pt 7
set style line 2 lc rgb "#d1495b" lw 2 pt 5
set style line 3 lc rgb "#555555" lw 1.5 dt 2

do for [qi=1:4] {
    q = sprintf("Q%d", qi)
    if (qi == 1) { implA = "lazy"; implB = "lockfree" }
    if (qi == 2) { implA = "lock"; implB = "treiber" }
    if (qi == 3) { implA = "lock"; implB = "ms" }
    if (qi == 4) { implA = "treiber"; implB = "elimination" }

    set output sprintf("%s/%s_throughput.png", OUT, q)
    set multiplot layout 2,3 rowsfirst title sprintf("%s: Average Throughput (MOPS)", q) font ",15"
    set ylabel "Throughput (MOPS)"
    do for [ni=1:2] {
        n = (ni == 1 ? 1000 : 10000)
        do for [wi=1:3] {
            w = (wi == 1 ? "0C-50I-50D" : (wi == 2 ? "50C-25I-25D" : "100C-0I-0D"))
            set title sprintf("N = %d, %s", n, w)
            plot DATA using ((strcol(1) eq q && strcol(2) eq implA && $3 == n && strcol(4) eq w) ? $5 : 1/0):9 with linespoints ls 1 title implA, \
                 DATA using ((strcol(1) eq q && strcol(2) eq implB && $3 == n && strcol(4) eq w) ? $5 : 1/0):9 with linespoints ls 2 title implB
        }
    }
    unset multiplot
    unset output

    set output sprintf("%s/%s_speedup.png", OUT, q)
    set multiplot layout 2,3 rowsfirst title sprintf("%s: Throughput Speedup", q) font ",15"
    set ylabel "Speedup vs. one thread"
    set yrange [0:*]
    do for [ni=1:2] {
        n = (ni == 1 ? 1000 : 10000)
        do for [wi=1:3] {
            w = (wi == 1 ? "0C-50I-50D" : (wi == 2 ? "50C-25I-25D" : "100C-0I-0D"))
            set title sprintf("N = %d, %s", n, w)
            plot DATA using ((strcol(1) eq q && strcol(2) eq implA && $3 == n && strcol(4) eq w) ? $5 : 1/0):10 with linespoints ls 1 title implA, \
                 DATA using ((strcol(1) eq q && strcol(2) eq implB && $3 == n && strcol(4) eq w) ? $5 : 1/0):10 with linespoints ls 2 title implB, \
                 x with lines ls 3 title "Ideal linear speedup"
        }
    }
    unset multiplot
    unset output
    unset yrange
}

