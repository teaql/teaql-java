# Matched business-slice experiment

This experiment completes four launcher comparisons for an identical bounded tanker-filling workflow. It does not migrate the original service's 100 custom Java files or exercise PLCs, SMS, authentication, Android or native images.

## Variants

| ID | TeaQL | Domain artifact | Entry point | HTTP server |
| --- | --- | --- | --- | --- |
| legacy-spring | 1.163-RELEASE | liquidfilling-core 296 | LegacyProbeApplication | Spring Boot 3.2.0 / Tomcat 10.1.16 |
| modern-spring | 1.554-RELEASE | liquidfilling-core 297 | ProbeApplication | Spring Boot 3.2.0 / Tomcat 10.1.16 |
| modern-classpath | 1.554-RELEASE | liquidfilling-core 297 | StandaloneProbe, classpath | JDK HTTP Server |
| modern-jpms | 1.554-RELEASE | liquidfilling-core 297 | Same StandaloneProbe, module path | Same JDK HTTP Server |

All use Corretto 21.0.12.1, Xms128m/Xmx512m, HikariCP 5.0.1 (minimum/maximum 10), PostgreSQL JDBC 42.6.0, Jackson 2.22.3 and an identical WARN console logging configuration. Forty-five shared JARs have identical hashes. The old and new distributions have 91 and 57 JARs respectively; packaging and auto-configuration differences are part of the compared stacks. See build-provenance.json. Framework and common dependency versions were pinned rather than letting the old starter transitively select Spring Boot 3.4.

Redis auto-configuration is disabled in the old slice, and a process-local DataStore adapter satisfies its mandatory legacy interface; this workload does not use distributed caching. The generated legacy general-purpose service controllers and GraphQL registry are excluded. Spring Actuator is present in both distributions. The modern standalone modes retain the entire modern bundle to control classpath/module-path inputs. JPMS resolves ALL-MODULE-PATH and the application/domain JARs are automatic modules, so this is not a minimal module set or proof of strong application encapsulation.

## Business contract

Both isolated databases contain one identified Merchant with external ID `matched-merchant`, name `Benchmark merchant`, taskLifeHours 48 and Platform relation 1. The seven Merchant physical columns have matching names, SQL types and lengths. The modern schema adds NOT NULL constraints on name, external_id, platform and version; the fixture satisfies them. The generated Platform labels differ (legacy 液体充装系统, modern 液体业务平台); the timed projection checks only its ID. A separate post-campaign diagnostic verifies actual parent-name E traversal against each direct root query. The schemas differ outside this slice: 56 legacy tables versus 76 modern tables, including modern tables for view descriptors. Nonpositive constant-ID remapping and reserved field-name changes remain as documented in the upgrade README.

- `/version`: exact version JSON; establishes Web readiness only.
- `/read`: query the Merchant by external ID, load its Platform relation, extract its name with E, return four identical fields.
- `/write?hours=49`, then `48`: query, perform a real update, persist, reload the row/relation and validate all fields. Modern persistence uses auditAs(...).save; legacy persistence uses saveGraph. These are the supported version-specific write APIs.
- `/testS`: the same empty-result weight-report Q predicate from the original service.
- `/ensureDB` and `/setup`: initialization and identified fixture setup; excluded from timing.

verify_contract.py verifies repeated schema setup and all business responses for every launcher before measurement. contract-verification.json retains the post-campaign responses, including rejection of hours=47 and a read confirming no mutation. Parent-field diagnostic evidence is under relation-diagnostics/.

## Collection

Run from a dedicated deployment directory containing `legacy/lib`, `modern/lib`, the scripts and `logback.xml`. Supply a mode-600 `.benchmark.env` containing only the test database password; never commit it. start-matched.sh specifies isolated database names and loopback port 18884.

```sh
python3 verify_contract.py
python3 collect_matched.py --runs 30 --settle 10 --load-repeats 5
```

The collector launches one JVM at a time. Each of 30 blocks contains all four variants in a shuffled order using seed 20261009. Web readiness is an externally observed exact `/version` response, polled every 20 ms. It then measures a first nonempty read and two checked writes; settled memory is taken after the writes, empty weight query and a ten-second wait. Procfs supplies RSS, high-water memory, CPU seconds and thread counts. Timings use time.monotonic_ns. The first-read and business-ready definitions are stricter than the earlier empty-query probe, so campaigns must not be combined.

A separate bounded load campaign uses five shuffled blocks with fresh JVMs. Every sample warms up with 200 checked reads and 20 alternating checked writes. It then measures 400 checked reads at concurrency four and 40 alternating checked writes at concurrency one. End-to-end latency includes loopback HTTP and client-side scheduling. This is a fixed workload, not a saturation or sustained production throughput claim. Every measured response is validated; raw per-request latencies are retained.

OS caches are not cleared. The host is shared, CPU frequency is not pinned, PostgreSQL/Redis and earlier idle test services remain running, and no JFR/NMT is enabled. Randomized blocks reduce time-order confounding but do not establish cross-host or production generality. Results summarize this host and workload.

Only a completed final batch is evidence. Earlier incomplete directories `results-20261009-193631` and `results-20261009-193841` were stopped while finalizing the adapter/dependency pins and are excluded. Nothing in those partial batches is pooled into the final results.

## Later logging finding

Framework WARN did not disable TeaQL's own SQL execution traces. Modern timed logs contain trace output; legacy logs do not. This batch remains historical evidence for its actual configuration. The matched-six experiment explicitly disables modern execution logging and repeats all six variants rather than pooling observations.
