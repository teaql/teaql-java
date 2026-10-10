# Six-launcher matched business experiment

This extends the earlier four-way experiment with Quarkus and Micronaut. It is a fresh randomized campaign, not a pooled ranking of later and earlier runs. It uses the same bounded Merchant read/relation/E/update-and-readback and empty weight-report query, not the full service's 100 custom business Java files.

| Variant | Runtime | HTTP implementation |
| --- | --- | --- |
| legacy-spring | TeaQL 1.163, Spring Boot 3.2.0 | Tomcat 10.1.16 |
| modern-spring | TeaQL 1.554, Spring Boot 3.2.0 | Tomcat 10.1.16 |
| modern-classpath | TeaQL 1.554, explicit assembly | JDK HTTP Server |
| modern-jpms | Same standalone main, module path | Same JDK HTTP Server |
| modern-quarkus | TeaQL 1.554, Quarkus 3.8.3 | RESTEasy Reactive / Vert.x |
| modern-micronaut | TeaQL 1.554, Micronaut platform 4.3.8, core 4.3.14 | Netty |

Framework versions follow the existing examples; this is not a comparison of the latest releases. Java is Corretto 21.0.12.1 on the same Debian/i7-4770HQ host. JVM heap limits, PostgreSQL JDBC 42.6.0, HikariCP 5.0.1 with minimum/maximum ten connections, Jackson core/databind 2.22.3 and annotations 2.22 are fixed. Generated domain JAR bytes, all eight TeaQL 1.554 JARs and the shared ProbeOperations class bytes are identical across all modern launchers. Quarkus and Micronaut have their own framework dependency closures; those differences are retained in the hash inventory.

The common implementation is the original ProbeOperations.java, compiled into shared-operations by a source include rule, with no copied business implementation. Quarkus uses a singleton producer and StartupEvent to assemble it before readiness; Micronaut uses an eager @Context factory. Both close it on shutdown. JDBC operations run on blocking workers, not event-loop threads. Micronaut's blocking executor is four threads. Framework worker defaults otherwise differ and are part of the selected stacks. Actuator is included in the Spring bundles but not added as an artificial feature to the other containers.

## Corrections caught before timing

Micronaut's default serialization omitted the empty data array from /testS. jackson.serialization-inclusion=ALWAYS preserves the exact response contract. The verifier continued requiring the full expected JSON; no expected field was waived.

TeaQL execution traces write SQL independently of the application logger level. The original four-way experiment had framework WARN configuration, but modern timed logs still contained SQL traces while legacy logs did not. This campaign explicitly sets executionLogging(false) in the shared modern bootstrap and keeps legacy framework execution logs at WARN. All six preflight logs had zero TeaQL SQL trace occurrences. Quarkus uses its native JBoss logger at WARN; others use the retained WARN Logback configuration. No performance sample mixes these two logging scenarios.

## Data and behavioral boundary

Each isolated database has one identified Merchant, matching name/external ID/hours and Platform ID 1. Timed responses contain the same four Merchant projection fields. Schemas, internal identity/revision history and generated Platform labels differ as documented in the previous experiment's schema-verification.json: 56 legacy versus 76 modern tables, added modern NOT NULL constraints, and root labels 液体充装系统 versus 液体业务平台. This is equivalent accepted workflow behavior, not identical full database snapshots.

The shared modern bootstrap was adapted to published TeaQL 1.554 using locally patched generator commits be6191f4/e1d25552. Those fixes are unreleased; the published runtime is unpatched. View descriptors producing physical tables still need review before production migration. Android, native executables, full original business migration, data protection and forced-transaction rollback are not tested here.

## Reproduce

Build/deploy recipes are in tanker-filling-service/upgrade/web-probes/README.md. App source commit and per-JAR hashes are retained in build-provenance.json. The modern Spring/classpath/JPMS bundle is separate from the historical four-way directory, so its originals remain unchanged. The legacy binary is reused unchanged.

From a dedicated directory with legacy/, modern/, quarkus/, micronaut/, logback.xml and a mode-600 test password environment file:

```sh
python3 verify_contract.py
python3 collect_matched.py --runs 30 --settle 10 --load-repeats 5
```

One JVM runs at a time on loopback port 18884. Thirty randomized blocks include all six launchers, seed 20261009. External monotonic timing and 20 ms polling measure Web availability followed by first nonempty read, two actual checked updates and the weight query; settled procfs memory follows a ten-second wait. A separate five-block campaign warms each JVM with 200 checked reads and 20 alternating checked writes, then measures 400 reads at concurrency four and 40 alternating writes at concurrency one. Per-request latencies are retained; this is bounded HTTP/client throughput, not saturation capacity.

OS caches are not cleared, CPU frequency is not pinned, the host is shared and earlier idle test services remain running. No JFR/NMT is enabled. Protocol equality and checksum verification support reproducibility; measurements remain descriptive for this host and workload.
