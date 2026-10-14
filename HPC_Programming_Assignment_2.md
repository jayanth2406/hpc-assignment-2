## Introduction Concurrent Data Structures in Java: Lazy and Non-Blocking Synchronization

Concurrent data structures are essential for exploiting multicore processors efficiently. When several threads operate on the same linked structure, stack, or queue, the implementation must preserve correctness while minimizing synchronization overhead. This assignment focuses on the practical implementation and performance evaluation of concurrent data structures using Lazy Synchronization, Non-Blocking Synchronization, and, for one problem, Elimination Backoff. The implementation language is Java. The main learning goals are to:

- implement thread-safe data structures using locks and atomic operations;

- identify the linearization point of important concurrent operations;

- measure execution time consistently across different thread counts;

- study the effects of contention, CAS failures, synchronization overhead, and cache-coherence traffic;

- calculate speedup and analyze scalability; and

- visualize results using GNUplot.

## Synchronization Models

## Lazy Synchronization

Lazy synchronization allows traversal to proceed with limited locking. After locating the relevant nodes or state, the thread acquires the required lock(s), validates that the observed state is still valid, performs the update, and then releases the lock(s). A deletion may first mark an element as logically deleted and later unlink it physically. This approach can permit unrelated operations to proceed concurrently while avoiding the serialization imposed by one global lock.

## Non-Blocking Synchronization

Non-blocking algorithms avoid mutual-exclusion locks in their principal update paths. They typically use atomic operations such as Compare-And-Swap (CAS). Conceptually, CAS(address, expected, newValue) updates the shared value only when its current value still equals the expected value. If another thread changes the state first, CAS fails and the operation normally retries. Java classes such as AtomicReference and AtomicMarkableReference may be used where appropriate.

## Linearization Points

A linearizable concurrent operation should appear to take effect at one instantaneous point between its invocation and response. This instant is its linearization point. For example, in a Treiber stack, the successful CAS that changes the top reference is normally the linearization point of a successful push or pop. For each implementation in this assignment, identify the linearization points of the major successful and unsuccessful operations.

## Experimental Setup

Use the following thread counts and problem sizes for every applicable implementation:


| Thread counts | 1, 2, 4, 6, 8, 10, 12, 14, 16 |
| --- | --- |
| Problem sizes | N = 1,000 and N = 10,000 |
| Duration of each run | 10,000 milliseconds (10 seconds) after prefill |
| Runs per configuration | 5 |

Fixed-duration execution: After the 50% prefill is completed, each run must execute the concurrent workload continuously for 10,000 milliseconds (10 seconds), irrespective of the number of threads. Use System.nanoTime() to establish the start time and the 10-second deadline. During this interval, every worker repeatedly performs operations according to the selected workload mix. Count the total number of completed operations in the 10-second interval. The problem size N determines the configured data-structure size and the 50% prefill level; it does not prescribe a fixed number of timed operations.

50% prefill requirement: Before starting the timed workload, prefill the data structure with 50% of the problem-size elements. Thus, for N = 1,000, initialize the structure with 500 elements; for N = 10,000, initialize it with 5,000 elements. The prefill must be completed before the timing starts and must not be included in the measured execution time. Use valid initial contents appropriate to the structure: insert distinct keys into the list, push elements into a stack, and enqueue elements into a queue. Apply the same principle to the Treiber and Elimination Backoff stacks.

## Workloads

Use the following operation mixes. C denotes a read/contains-type operation, I an insertion-type operation, and D a deletion-type operation.

| Workload | C / Read | I / Insert | D / Delete |
| --- | --- | --- | --- |
| 0C-50I-50D | 0% | 50% | 50% |
| 50C-25I-25D | 50% | 25% | 25% |
| 100C-0I-0D | 100% | 0% | 0% |

Interpret the operations for each data structure as follows:

| Data Structure | C | I | D |
| --- | --- | --- | --- |
| Doubly Linked List | contains() | insert() | delete() |
| Stack | peek() | push() | pop() |
| Queue | peek() | enqueue() | dequeue() |
| Elimination Backoff Stack | peek() | push() | pop() |

## Execution-Time Measurement

Use only Java System.nanoTime() to measure execution time. No other timer or timing API should be used for benchmark measurements.


```
long start = System.nanoTime();
long deadline = start + 10_000_000_000L; // 10,000 ms
// worker threads repeatedly execute workload operations
// while (System.nanoTime() < deadline) { ... }
long end = System.nanoTime();
long elapsedNs = end - start;
```

The timed interval must contain only the concurrent workload. The following must remain outside the measured interval:

- 50% data-structure prefill and initialization;

- workload generation;

- thread-object creation where possible;

- JVM startup;

- file input/output;

- printing of results; and

- GNUplot generation.

Coordinate worker threads so that the timed concurrent phase starts approximately together. CountDownLatch or CyclicBarrier may be used for coordination; however, the start time, deadline, and elapsed time must be determined only through System.nanoTime(). Each run must continue until the 10,000-millisecond deadline is reached.

## Repeated Runs, Throughput, and Speedup

Execute each experimental configuration five times. For every 10,000-millisecond run, record the total number of completed operations and the actual elapsed time measured using System.nanoTime(). Compute throughput as:

```
Throughput = Completed Operations / Elapsed Time (seconds)
```

Calculate the throughput in millions of operations per second.

```
Throughput (MOPS) = Completed Operations / (Elapsed Time (seconds) x 1,000,000)
```

If the five measured throughputs are P1, P2, P3, P4, and P5, compute:

```
P_avg = (P1 + P2 + P3 + P4 + P5) / 5
```

Because every benchmark run has the same fixed duration of 10 seconds, calculate scalability/speedup using throughput:

```
Speedup(p) = Throughput(p) / Throughput(1)
```

The one-thread baseline must correspond to the same data structure, implementation, problem size, workload, 10-second duration, and prefill policy.

## GNUplot and Performance Analysis

Use GNUplot to produce clear plots of:

- Average throughput (millions of operations per second) vs. number of threads;

- Speedup vs. number of threads;

- Lazy vs. Non-Blocking implementations;

- the three workloads;

- N = 1,000 vs. N = 10,000; and

- Treiber Stack vs. Elimination Backoff Stack.


Discuss the observed trends using appropriate HPC terminology, including lock contention, CAS failures and retries, synchronization overhead, cache-coherence traffic, shared-memory contention, thread scheduling, and speedup saturation. Where useful, show the ideal speedup line, Speedup(p) = p.

- 1. Concurrent Doubly Linked List

Implement a concurrent doubly linked list in Java supporting:

insert(key) delete(key)

contains(key)

## Lazy Synchronization

Develop a lazy implementation using fine-grained locking, optimistic traversal, validation, logical deletion, and physical deletion. A node may contain key, next, prev, and marked fields. Ensure that concurrent modifications preserve the consistency of both next and prev links.

## Non-Blocking Implementation

Develop a non-blocking version using atomic operations such as CAS. Do not use mutual-exclusion locks in the principal update path. Address logical deletion, physical deletion, CAS failure and retry, and helping if your design uses it.

For both implementations:

- identify the linearization points of successful and unsuccessful insert(), delete(), and contains() operations;

- prefill the list with 50% of the problem-size elements before timing;

- run all specified thread counts, problem sizes, and workloads;

- execute each configuration five times; after prefill, run each timed workload for 10,000 milliseconds using System.nanoTime();

- report average throughput and calculate throughput-based speedup; and

- generate appropriate GNUplot graphs and explain the observed scalability.

- 2. Concurrent Stack

Implement a concurrent stack in Java supporting:

push(x) pop() peek()

## Lazy / Lock-Based Stack

Implement a lock-based stack following the lazy/validation approach. Ensure that an update is performed only after validating that the state on which it depends is still current.

## Non-Blocking Stack

Implement a Treiber Stack using an atomic top reference and CAS. A push links the new node to the observed top and attempts CAS(top, oldTop, newNode). A pop similarly attempts to replace the observed top with its successor. Retry when CAS fails because another thread modified the stack. For both implementations:

- identify the linearization points of push(), pop(), and peek();


- prefill the stack with 50% of the problem-size elements before timing;

- evaluate all specified thread counts, problem sizes, and workloads;

- execute each configuration five times; after prefill, run each timed workload for 10,000 milliseconds using System.nanoTime();

- report average throughput and calculate throughput-based speedup; and

- generate GNUplot graphs comparing the two implementations.

- 3. Concurrent Queue

Implement a concurrent queue in Java supporting:

enqueue(x) dequeue() peek()

## Lazy / Lock-Based Queue

Implement a lock-based queue using appropriate synchronization and validation for safe

modification of the head and tail.

## Non-Blocking Queue

Implement a CAS-based non-blocking queue. A Michael-Scott style queue may be used. Maintain atomic references to head and tail and correctly handle concurrent advancement of these references. For both implementations:

- identify the linearization points of successful enqueue(), successful dequeue(), empty dequeue(), and peek();

- prefill the queue with 50% of the problem-size elements before timing;

- evaluate all specified thread counts, problem sizes, and workloads;

- execute each configuration five times; after prefill, run each timed workload for 10,000 milliseconds using System.nanoTime();

- report average throughput and calculate throughput-based speedup; and

- generate GNUplot graphs comparing the implementations and workloads.

- 4. Elimination Backoff Stack

Implement an Elimination Backoff Stack in Java. The central stack should be a non-blocking Treiber Stack. Under contention, complementary push and pop operations should be able to exchange a value through an elimination slot without both modifying the central top reference.

The implementation should include:

- a Treiber Stack;

- an elimination array;

- random selection of elimination slots;

- a timeout and/or backoff policy; and

- CAS-based synchronization.

Compare the Treiber Stack and the Elimination Backoff Stack. Pay particular attention to 8, 10, 12, 14, and 16 threads, where contention may become more significant. For the experimental evaluation:

- prefill the stack with 50% of the problem-size elements before timing;


- use all specified thread counts, problem sizes, and workloads;

- execute every configuration five times, with each timed run lasting 10,000 milliseconds after prefill;

- use only System.nanoTime() to establish and measure the 10,000-millisecond concurrent workload interval;

- report average throughput and calculate throughput-based speedup;

- generate GNUplot graphs comparing Treiber and Elimination Backoff stacks; and

- discuss whether elimination reduces contention on the central stack and improves scalability, especially for update-intensive workloads.

## Submission Note

Submit the Java source code, experiment scripts, GNUplot scripts, raw timing/operation-count data for all five runs, averaged throughput results, plots, and a PDF report explaining the implementation, linearization points, experimental methodology, results, speedup, and scalability observations. Do not include compiled .class files or executables.
