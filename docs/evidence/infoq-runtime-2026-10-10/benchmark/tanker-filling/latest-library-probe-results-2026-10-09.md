# TeaQL 1.554 runtime probe results — 2026-10-09

TeaQL 1.554-RELEASE successfully runs the regenerated tanker-filling domain with explicit runtime assembly, PostgreSQL mapping, audited persistence, relation hydration and generated Q/E expressions. All three launchers completed 30 fresh-JVM startup runs. This is a bounded integration probe, not a complete migration of the original service.

## Matched probe measurements

Medians across 30 runs per variant. RSS is process resident memory, not Java heap usage. “Settled” means ten seconds after the first database query.

| Launcher | Web ready (s) | Query ready (s) | Ready RSS (MiB) | Settled RSS (MiB) | Peak through settle (MiB) | CPU to Web ready (CPU-s) | Ready threads |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Spring Boot 3.2.0 / Tomcat / classpath | 1.804 | 2.062 | 206.303 | 240.004 | 254.945 | 5.550 | 42.000 |
| JDK HTTP server / classpath | 0.528 | 0.828 | 93.701 | 130.010 | 152.740 | 1.095 | 24.000 |
| JDK HTTP server / JPMS | 0.626 | 0.929 | 110.453 | 156.998 | 179.277 | 1.645 | 25.000 |

The standalone classpath probe has about 71% lower Web-ready time and 55% lower ready RSS than the Spring probe in this campaign. This comparison changes Spring/Tomcat to the JDK HTTP server; it does not isolate dependency injection or any single TeaQL mechanism.

JPMS did **not** improve startup here: the same standalone main took approximately 0.098 seconds longer and used 16.75 MiB more ready RSS than its classpath launch. JPMS resolves all modules in this bundle, including unused Spring modules, through `ALL-MODULE-PATH`. A smaller module set is a useful next experiment, not an established result. These measurements demonstrate runtime portability rather than a speed benefit intrinsic to JPMS.

## Functional verification and adaptation

Before timing, `context.ensureSchema()` completed twice successfully on a fresh isolated database. The final schema has 76 tables, including generated constants. The probe then created one identified Merchant using `auditAs(...).save`, reloaded it with its Platform relation and extracted its name using generated E access. The Q query reproduces the original weight-report predicate and returns an expected empty result. After all runs, both the original service and JPMS probe returned the expected version; the JPMS query and Merchant/E verification passed again.

Observed final responses:

```json
{"version":20240205}
{"recordCount":0,"status":"YES","resultCode":0,"data":[]}
{"platformId":1,"name":"TeaQL 1.554 probe","ok":true,"merchantId":1}
```

The trial exposed generator issues, fixed on local generator branch `codex/tanker-java-1554-generation` (commits `be6191f4`, `e1d25552`): stale JsonMe descriptor namespace, removed reverse-relation request method, unsupported custom descriptor construction, and missing custom SQL descriptor initialization/physical column metadata. The domain was regenerated rather than hand-edited. Five generator regression tests passed, including compilation and actual descriptor registration against published 1.554. These generator fixes are not yet released; the published runtime was not patched.

Five legacy constant IDs were nonpositive and rejected by runtime registration. Only the upgrade model remaps them: RequestType FILL_STATION_SETTING 0→10001; RequestType MERCHANT_CONFIGURATION -1→10002; AnalysisType NONE -1→10001; TaskStatus STOPPED -1→10001; TaskStatus NOT_RUNNING 0→10002. Full migration requires corresponding client/data mapping.

The current PostgreSQL provider also creates tables for view descriptors: 76 tables here versus 56 in the original schema. This behavior requires review before production migration. JSON snapshot descriptor registration/schema were tested; a snapshot JSON round trip, masking/encryption behavior, Android and native-image compilation were not tested in this campaign.

## Reproduction and boundaries

- App branch: `codex/teaql-1.554-upgrade`; build `JAVA_HOME=<Java 21> ./gradlew :modern-probe:installDist` in tanker-filling-service. Domain artifact: liquidfilling-core 297; TeaQL 1.554-RELEASE.
- Same 50-JAR distribution, Corretto 21.0.12.1, `-Xms128m -Xmx512m`, lazy Hikari pool (minimum/maximum 10), database and ProbeOperations in all three variants. Archive SHA-256: `c9a83802e920c9a7968819dbc1b50718e7c263034dd6b9b69e54ed461d282466`.
- Host: Debian x86_64, Intel i7-4770HQ, approximately 15.5 GiB RAM; PostgreSQL 17.11 in Docker. Original service, PostgreSQL and Redis remained idle background processes. The new probe does not integrate Redis.
- Database: dedicated `teaql_benchmark_modern_clean`, initialized outside timed runs. No production database or device endpoints are used.
- External monotonic timing includes process launch. Web ready requires a launcher ready marker plus exact `/version` JSON. Query ready requires a successful `/testS` response. Database pool creation is lazy; Web ready is not database ready.
- Fresh JVM per run; OS caches were not cleared. Runs were sequential by variant rather than randomized. Poll interval is 50 ms, so small timing differences should be interpreted cautiously. No JFR or NMT was enabled. RSS includes heap, code, native memory and mapped pages; no causal memory breakdown was collected.
- Classpath and JPMS use the same StandaloneProbe/JDK HTTP server. JPMS command is shown below. Application and domain JARs are automatic modules; TeaQL dependencies include named module descriptors. This does not establish full strong encapsulation of application code.

```sh
java -Xms128m -Xmx512m \
  --module-path ./lib --add-modules ALL-MODULE-PATH,jdk.httpserver \
  --module modern.probe/com.doublechaintech.liquidfilling.probe.StandaloneProbe
```

The [original full-service baseline](spring-baseline-results-2026-10-09.md) measured 5.821 seconds Web ready and 444.04 MiB ready RSS. It uses Java 17, TeaQL 1.163, Redis and 100 custom service Java files. Differences from the reduced modern probe cannot be attributed to a library upgrade alone. A defensible old/new service comparison requires migration of the same business behavior and lifecycle work.

## Raw evidence

[Collector](collect_startup.py) and [launcher](start-modern-probe.sh). Each directory contains 30 log files, raw.csv, environment.json and summary.json. All 90 CSV rows and logs were checked; medians and nearest-rank p90 values were recomputed. The three environment records agree on archive hash, dependency list, JDK and JVM flags.

- [Spring Boot 3.2.0 / Tomcat / classpath](results-20261009-190037-latest-spring/summary.json): [raw CSV](results-20261009-190037-latest-spring/raw.csv), [environment](results-20261009-190037-latest-spring/environment.json).
- [JDK HTTP server / classpath](results-20261009-190742-latest-classpath/summary.json): [raw CSV](results-20261009-190742-latest-classpath/raw.csv), [environment](results-20261009-190742-latest-classpath/environment.json).
- [JDK HTTP server / JPMS](results-20261009-191311-latest-jpms/summary.json): [raw CSV](results-20261009-191311-latest-jpms/raw.csv), [environment](results-20261009-191311-latest-jpms/environment.json).
