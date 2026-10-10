# Validation — 2026-10-10

Accepted batches:

- `results-20261010-092356`: 12 unchanged deployment/JDK combinations, 360 starts and 60 load samples.
- `current-results-20261010-104637`: six upgraded framework/JDK combinations, 180 starts and 30 load samples.
- `native25/results-20261010-125259`: five native deployments, 150 starts and 25 load samples.

Total: 690 accepted starts and 115 load samples. Every accepted variant has 30 starts and five load samples. Startup medians/p90, request medians/p95, sample counts, latency-array lengths and rates were recalculated from raw observations. Timed logs contain no ERROR/Exception or TeaQL SQL traces. Per-batch checksums are retained. All 12/six/five pre/post business contracts passed, including persisted updates and expected Q/E/JSON responses.

`verify_artifacts.py` confirms all nine modern shared TeaQL/domain artifacts across JVM/native inputs, the retained JVM launcher hashes, and 91 unchanged legacy native JARs. Business-operation class bytes are identical across modern JVM deployments. Native executable sizes/hashes match the identities captured by the collector; every accepted configured build has exit status zero. Compiler versions, flags, AOT class hashes, reachability configuration and successful/failed build diagnostics are retained.

`native25/results-20261010-121226` is an incomplete failed load batch, excluded from performance tables. It preserves the pre-fix binary/build identity and the Micronaut SharedArenaSupport error. The replacement binary passed a one-start/one-load smoke campaign for each deployment with identical warmup/concurrency and clean logs before full sampling. A separate three-start/one-load AOT diagnostic is also excluded; it did not resolve the legacy native startup regression.

Charts were generated from raw CSV values and visually inspected. Main manuscript and report local links resolve. Helper Python/Bash syntax checks passed. The application working tree is clean on `codex/java25-27-benchmark` (`3a29946`). Original services at 18880 and 18882 still return version 20240205. No publication or submission was performed.

Scope remains the persisted Merchant business slice on one shared Linux host. Full-service migration, schema parity, device behavior and protected-reference/entity JSON are not established by these measurements. Java 27 observations are JVM only; no native 27 toolchain/result is claimed. Reproduction requires the project-local domain/operation artifacts.
