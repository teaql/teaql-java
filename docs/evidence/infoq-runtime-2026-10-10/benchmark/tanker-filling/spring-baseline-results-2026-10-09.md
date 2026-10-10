# Spring startup baseline — 2026-10-09

Thirty independent JVM launches completed successfully on `192.168.2.206`. This measures the existing Spring application only; no JPMS or explicit-assembly comparison has yet been run.

| Metric | Median | P90 | Minimum–maximum |
| --- | ---: | ---: | ---: |
| Process launch to Web readiness | 5.821 s | 5.980 s | 5.659–6.071 s |
| Process launch to first successful SQL probe | 6.059 s | 6.221 s | 5.893–6.283 s |
| First SQL probe latency | 226.69 ms | 256.94 ms | 206.26–299.32 ms |
| Spring's own logged startup duration | 5.136 s | 5.281 s | 4.997–5.369 s |
| RSS at Web readiness | 444.04 MiB | 455.62 MiB | 415.89–460.89 MiB |
| RSS after first SQL probe | 448.97 MiB | 460.82 MiB | 425.72–465.26 MiB |
| RSS 10 seconds after first SQL probe | 433.34 MiB | 450.57 MiB | 412.62–453.41 MiB |
| Peak RSS through Web readiness | 444.04 MiB | 455.62 MiB | 415.89–460.89 MiB |
| Peak RSS through the post-query 10-second window | 451.93 MiB | 464.48 MiB | 427.08–468.07 MiB |
| Process CPU time through Web readiness | 24.265 CPU-s | 24.720 CPU-s | 23.470–25.100 CPU-s |
| Threads at Web readiness | 96 | 96 | 96–97 |

## Measurement boundaries

The external timer starts immediately before spawning the launch script and uses Python's monotonic clock. Web readiness requires both the Spring `Started App` log marker and a successful `/version` response matching `{"version":20240205}`. Readiness is polled every 50 ms; these are observed readiness times, not an exact timestamp of the internal ready transition.

The first SQL probe is `/testS`. Each response was validated as `resultCode=0`, `status=YES`, and `recordCount=0`. It exercises the application's existing empty-result query through PostgreSQL. It does not verify populated row hydration or a complete filling workflow. The difference between Web readiness and SQL readiness includes first-use database work.

RSS is taken from Linux `/proc/<pid>/status` (`VmRSS`), and peak RSS from `VmHWM`. MiB means 1,048,576 bytes. These are process-resident memory measurements, not Java heap usage or the heap limit. The 10-second value is an idle-window snapshot, not proof of a long-term steady state. PostgreSQL and Redis memory are not included.

CPU time sums process user and system CPU counters from `/proc/<pid>/stat` using the host's 100 ticks/second. It accumulates work across threads, so CPU seconds can exceed wall-clock elapsed seconds. P90 uses the nearest-rank method; 30 observations provide limited tail precision.

## Environment and controls

- Intel Core i7-4770HQ @ 2.20 GHz; approximately 15.5 GiB host memory.
- Debian x86_64, Linux 6.12.107+deb13-amd64.
- Amazon Corretto 17.0.20.1; `-Xms128m -Xmx512m`; no JFR or NMT.
- Spring Boot 3.2.0; TeaQL 1.163-RELEASE; liquidfilling-core 296.
- Source revision `00a9bb393700b691bd3c13a454b327fdf72e354e`.
- JAR SHA-256 `594e9fd43e74efdfc21e09cbc5bc75dd098501a860e16236e40f5733cb339214`.
- PostgreSQL 17.11 and Redis 7.4.11 remained running across trials.
- Preinitialized schema with 56 tables, no business fixtures; `teaql.ensureTable=false` for every trial.
- Fresh JVM process for each trial. OS page caches were not cleared; this is repeated process startup, not a machine-cold or disk-cold experiment.
- Device/card endpoints and SMS credentials replaced by isolated test configuration.
- No application or host tuning was introduced between trials. The host was not reserved exclusively for this experiment.

## Reproduction and retained evidence

Run [collect_startup.py](collect_startup.py) beside the staged JAR, configuration and launcher:

```sh
python3 collect_startup.py --runs 30 --settle-seconds 10
```

The collector stops only the tracked benchmark process and restores a background instance when finished. It does not initialize schema or clear caches.

- [Per-run CSV](results-20261009-171931/raw.csv)
- [Summary JSON](results-20261009-171931/summary.json)
- [Environment metadata](results-20261009-171931/environment.json)
- Thirty individual startup logs in `results-20261009-171931/`.

The local CSV row count, run sequence, log count, medians and P90 values were checked against the summary. After collection the restored process passed `/version` with HTTP 200. These results establish a baseline for this application and host; they establish no improvement attributable to JPMS or removal of Spring.
