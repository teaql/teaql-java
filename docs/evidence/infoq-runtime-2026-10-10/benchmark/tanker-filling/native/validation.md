# Evidence validation — 2026-10-10

Validated the completed results-20261010-003131 batch: 150 native startup rows, 25 load rows, 10,000 measured read latencies and 1,000 measured write latencies. Recomputed each median/p90, request-latency median/p95 and request rate. All175 timed process logs contain no ERROR, Exception or TeaQL SQL traces. Every measured executable SHA/size matches the retained build provenance and a successful configured compiler log.

Validated jvm-control-results-20261010-005914: 30 Micronaut starts, five load samples, 2,000 read and200 write latencies. Its complete JAR identity matches the native-compatible Micronaut inputs. These checks retain native and control result checksums separately.

The eight published TeaQL1.554 JAR hashes and generated domain JAR hash match the six-way JVM baseline for Spring, Quarkus and Micronaut. Standalone consumes the unchanged matched-six/modern bundle. Compiler version, GCC, hardware and dynamic linkage are retained. Modern source075990f and legacy-bootstrap sourcefb1fb9c are local commits; controller/operation source is unchanged from the measured JVM slice. Spring's parameter metadata changes compiled application bytes as documented.

All five pre/post native business contracts passed: repeated schema checks, nonempty Q/E response, writes to49/48 and readback, empty weight-query JSON, and fixture-guard rejection without mutation. The guard does not establish framework authorization or forced-failure rollback. Failed/unconfigured builds and preflight failures remain evidence, never timing samples.

Commands: validate_results.py <native result directory>; validate_control.py <control directory>. The final report and figure are generated from those retained observations, not manually entered. Full-service migration, schema parity, Android and protected entity/reference JSON remain separate validation work.
