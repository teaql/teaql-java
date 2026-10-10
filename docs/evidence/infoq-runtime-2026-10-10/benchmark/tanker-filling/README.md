# Tanker filling startup benchmark

Primary comparison: [six-launcher experiment](matched-six/results-2026-10-09.md), adding Quarkus and Micronaut with explicitly disabled TeaQL execution traces. It supplies 180 startup runs and 30 fixed-warmup samples. The [historical four-launcher campaign](matched/results-2026-10-09.md) remains separate; its modern variants emitted SQL traces despite framework WARN, so its operation costs include that logging difference.

Earlier, separate campaigns: [original full Spring service](spring-baseline-results-2026-10-09.md) and [TeaQL 1.554 runtime probe comparison](latest-library-probe-results-2026-10-09.md), with raw CSV, environment metadata and individual logs. The latter compares three launchers for one reduced probe; it does not represent a complete business-service migration. Those earlier campaigns are not pooled with the matched experiment.

## Spring baseline staging — 2026-10-09

- Source: `/Users/Philip/githome/tanker-filling-service`, commit `00a9bb393700b691bd3c13a454b327fdf72e354e`; working tree clean when inspected.
- Build command: `JAVA_HOME=/Users/Philip/.sdkman/candidates/java/17.0.9-amzn ./gradlew bootJar --console=plain`.
- Build succeeded; this was not a full test run.
- Spring Boot 3.2.0, Java target 17, liquidfilling-core 296, TeaQL 1.163-RELEASE.
- Target: `philip@192.168.2.206`, Debian x86_64.
- Staged JAR: `/home/philip/teaql-startup-benchmark/spring-baseline/app.jar`.
- Local and remote SHA-256: `594e9fd43e74efdfc21e09cbc5bc75dd098501a860e16236e40f5733cb339214`.
- Existing deployment scripts target other hosts; they were not executed.

The initial server inspection found no Java on PATH, no installed JDK under the inspected standard locations, no Docker socket access for philip, and no passwordless sudo. Amazon Corretto 17 was downloaded from its official distribution endpoint and installed under the dedicated user directory. The user subsequently authorized root SSH and Docker installation of PostgreSQL and Redis. No benchmark result is established by the JAR transfer.

## Host storage preparation — 2026-10-09

Image extraction initially failed because `/var` was full. Inspection found approximately 5.1 GB of APT package cache. At the user's request, `apt-get clean` removed that cache. With no running containers, Docker and containerd were stopped; their data was copied and verified by file hashes, ownership, modes, links and device metadata before migration. The original directories were retained under `/home/teaql-startup-benchmark-storage-backup/`.

- `/var/lib/docker` -> `/home/teaql-docker-data`
- `/var/lib/containerd` -> `/home/teaql-containerd-data`
- Docker and containerd services restarted successfully; the pre-existing stopped container remained present.
- `/var` had approximately 5.3 GB free afterward; active container storage resolves to the `/home` filesystem.
- Docker's existing HTTP/HTTPS proxy was already `http://192.168.1.1:1087` and was preserved.

No global Docker/containerd storage-path configuration was changed. The first, configuration-based migration proposal was rejected by automatic review and did not execute; this symlink migration followed explicit user authorization.

## Isolation and readiness

### Installed and verified — 2026-10-09

| Service | Runtime | Address | Verification |
| --- | --- | --- | --- |
| PostgreSQL | 17.11, official `postgres:17-bookworm` | `127.0.0.1:15432` | Docker health check, pg_isready, password-authenticated SQL |
| Redis | 7.4.11, official `redis:7-bookworm` | `127.0.0.1:16379` | Docker health check and PONG |
| Spring baseline | Spring Boot 3.2.0 on Corretto 17.0.20.1 | `127.0.0.1:18880` | `/version` returned HTTP 200 and `{"version":20240205}` |

Container names: `teaql-benchmark-postgres`, `teaql-benchmark-redis`. Both use dedicated named volumes and `unless-stopped` restart policy. The pre-existing stopped Redis container was not replaced.

Image digests:

- `postgres@sha256:3645570cccdfa447589da9f57dd740faa29b30938e861289a5574b6ca6b03826`
- `redis@sha256:4fa24486b8bcca8eec45ee0eb166edc674795e53a2b53d1a9ef263eecebaac85`

Database and role: `teaql_benchmark`. The randomly generated password is retained in root's `/root/teaql-startup-benchmark/postgres.env` and philip's `.benchmark.env`, both restricted files. It is not recorded here. The application password file was verified as mode 600, owned by philip.

Spring was launched as philip using the supplied script, with PID 226366 at verification time; `app.pid` and `app.log` are in the staged application directory. It is a background test process, not a newly installed systemd service. Containers restart after reboot; the Spring process currently requires manual launch.

The isolated schema was subsequently initialized through `/ensureDB`, producing 56 public tables. Seeded business fixtures and full business readiness remain to be prepared before performance measurement. No performance result from this deployment has been added to the manuscript.

### Schema initialization — 2026-10-09

The HTTP endpoint is `/ensureDB`; it invokes `SQLRepositorySchemaHelper.ensureSchema(context, factory)`, then ensures merchant properties for existing merchants. In TeaQL 1.163, `SQLRepository` gates actual DDL execution on `DataConfigProperties.isEnsureTable()`. With `teaql.ensureTable=false`, the helper logs candidate DDL without executing it; the subsequent merchant query failed because no table existed.

The test application was restarted with `BENCHMARK_ENSURE_TABLE=true`, `/ensureDB` returned `{"ok":true}`, and direct PostgreSQL inspection found 56 public tables. It was then restarted with the default `false`. The script now exposes this explicit initialization-only override. Keep this setup stage outside measured startup runs. The endpoint's HTTP 200 alone does not establish success: its failure path also returns HTTP 200 with a `fail` field.

Use the accompanying external properties file to replace bundled configuration. It selects loopback-only HTTP on 18880, test PostgreSQL on 15432, and test Redis on 16379; PLC/card endpoints and SMS credentials are placeholders. Do not activate production profiles. Database password belongs in the process environment, not committed configuration or logs.

Candidate launch after infrastructure exists:

```sh
java -Xms128m -Xmx512m -jar app.jar \
  --spring.config.location=file:./spring-baseline.properties
```

The JVM limits above are a starting configuration, not a measured optimum; use the same settings for comparable JVM variants.

`/version` returns a version object and can establish Web-layer availability. It is not a database business-readiness check. `/ensureDB` mutates schema and metadata and must only be invoked deliberately against the isolated test database, outside timed runs, with the initialization override enabled. Other controller routes include device actions and SMS; do not use them as probes.

Before publishing timings, define and seed a bounded read/write workflow with expected results, preserve identical database snapshots and lifecycle work across variants, and specify whether schema setup is inside or outside the measured boundary. Keep JFR diagnostics separate from primary timing runs. A fresh JVM process is not evidence of a cold OS page cache.

Planned comparisons: A original Spring/classpath; B explicit assembly/classpath; C the same B implementation/module path. An upgrade from TeaQL 1.163 to a modern generated runtime changes more than JPMS and must be reported as an additional variable.
