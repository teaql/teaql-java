# Matched Linux native-image results

Completed batch: `results-20261010-003131`. Thirty fresh native processes and five fixed-warmup load samples per accepted variant.

Table guide: ↓ lower is better; ↑ higher is better. ①/②/③ mark the three best distinct displayed values within each table; displayed ties share a rank. Paired p50/p95 values are ranked separately. Thread counts are descriptive. These marks do not imply statistical significance or an overall framework ranking.

| Native executable | HTTP ready (s) ↓ | Business ready (s) ↓ | Business RSS (MiB) ↓ | Settled RSS (MiB) ↓ | Executable (MiB) ↓ |
| --- | ---: | ---: | ---: | ---: | ---: |
| Modern standalone | 0.041 ① | 0.072 ② | 52.7 ① | 55.9 ① | 66.7 ① |
| Modern Spring | 0.099 ② | 0.130 ③ | 99.4 | 102.6 | 97.9 |
| Modern Quarkus | 0.041 ① | 0.070 ① | 64.4 ② | 68.2 ② | 91.1 ③ |
| Modern Micronaut (Jackson 2.16.1) | 0.041 ① | 0.072 ② | 77.1 ③ | 80.7 ③ | 86.9 ② |
| Legacy Spring | 0.244 ③ | 0.274 | 159.8 | 160.0 | 158.9 |

All table values except executable size are medians. Business ready includes the first successful nonempty Merchant query and E expression evaluation. HTTP readiness is the exact version JSON; it does not establish a connected pool.

| Native executable | First read (ms) ↓ | First write to 49 (ms) ↓ | Next write to 48 (ms) ↓ | Warm read req/s ↑ | Warm write req/s ↑ |
| --- | ---: | ---: | ---: | ---: | ---: |
| Modern standalone | 30.0 ③ | 17.3 ① | 15.9 ① | 238.7 ① | 58.7 ③ |
| Modern Spring | 30.1 | 17.7 | 16.1 ③ | 233.6 ② | 56.6 |
| Modern Quarkus | 28.9 ① | 17.5 ② | 16.0 ② | 232.2 ③ | 59.4 ① |
| Modern Micronaut (Jackson 2.16.1) | 30.4 | 18.0 | 16.5 | 227.1 | 59.0 ② |
| Legacy Spring | 29.7 ② | 17.6 ③ | 16.1 ③ | 221.3 | 56.0 |

The bounded load is 400 reads at concurrency four and 40 alternating writes at concurrency one, after 200 reads and 20 warmup writes. HTTP/client scheduling and database work are included; these rates are not maximum throughput.

## Build observations

| Executable | Configured build elapsed ↓ | Compiler peak RSS (MiB) ↓ | Result |
| --- | ---: | ---: | --- |
| Modern standalone | 3:38.43 ① | 4107.0 ① | Success |
| Modern Spring | 5:07.70 | 5373.4 | Success |
| Legacy Spring | 7:07.17 | 7333.7 | Success |
| Modern Quarkus | 4:36.06 ③ | 5171.9 ③ | Success |
| Modern Micronaut (Jackson 2.16.1) | 4:21.42 ② | 4973.6 ② | Success |

Builds used Oracle GraalVM 21.0.12+7.1, GCC 14.2.0, compatibility instructions, optimization level 2, four compilation threads and an 8 GiB compiler heap. They were serial. Agent training/contract checks overlapped some builds; these are shared-machine observations, not a controlled build-speed ranking.

## JVM comparison and scope

Use the retained six-way JVM batch for legacy Spring, modern Spring, standalone classpath and Quarkus. The native standalone executable is one closed program; it does not preserve JPMS/classpath as two runtime loading modes. Native and JVM both accept -Xms128m/-Xmx512m, but native Serial GC and JVM default GC have different physical-memory behavior. OS caches were not cleared; the CPU governor and other original server services were not changed.

Micronaut's matched Jackson 2.16.1 JVM control is `jvm-control-results-20261010-005914`: HTTP ready 1.307s, business ready 1.596s, business RSS 211.7MiB, warm reads 201.9/s and writes 49.3/s. Its native dependencies exactly match this control. The original six-way JVM Micronaut used Jackson 2.22.3 and remains separate.

## Initial attempts and integration changes

Standalone initially compiled but failed during Hikari initialization; the observed JDBC field-updater/resource metadata fixed this path. Spring initially failed to instantiate the XML-configured ConsoleAppender; tracing-agent metadata fixed that logging path. Quarkus initially rejected absent optional Hikari metrics types under global strict linkage; the configured build limits strict linkage to io.quarkus/io.teaql and enables service loading for the manual JDBC pool. Micronaut 4.3.14 referenced a Jackson naming strategy removed in 2.22; the native-compatible profile aligns Jackson 2.16.1. The unconfigured legacy attempt was superseded after analysis. Its subsequent image compiled but failed the first real SQL query: Hutool package scanning discovered no SQL expression parsers. A native-only adapter bootstrap explicitly registers the exact 17 shipped parser implementations, with separate instances per repository, and restores WARN after legacy AOT logging. The old runtime and controller operations remain unchanged; this required adaptation is part of the reported deployment stack.

The traced standalone contract recorded 61 reflection entries, zero TeaQL package types and zero application package types. The AOT legacy trace recorded 1,092 entries, including 80 TeaQL types and 94 application package types (including configuration/repositories/adapters). These counts describe observed reflection in this workload, not the compiler’s total registrations or arbitrary application coverage. Modern Spring still traces adapter/container reflection.

Modern adapter source commit 075990f and legacy native-bootstrap commit fb1fb9c use the same controller/operation source as JVM commit 176a323. Spring Boot adds parameter metadata to compiled application classes; their bytes differ, while the generated domain and published TeaQL artifacts are unchanged. Local/unreleased generator changes remain required. The hours=47 rejection is the adapter fixture guard, not a framework authorization or forced-failure rollback test. Full original-service migration, schema parity, Android, encrypted reference/ID JSON, field masking and database-failure rollback remain outside this measurement.

## Evidence

- [Startup CSV](results-20261010-003131/startup.csv) and [summary](results-20261010-003131/startup-summary.json).
- [Load CSV](results-20261010-003131/load.csv) and [summary](results-20261010-003131/load-summary.json).
- [Executable identity and environment](results-20261010-003131/environment.json).
- [Build provenance](build-provenance.json), actual compiler logs and observed metadata.
- [Pre/post business contract](contract-verification.json), [collector](collect_native.py), [launcher](start-native.sh), [build commands](build-native.sh).
