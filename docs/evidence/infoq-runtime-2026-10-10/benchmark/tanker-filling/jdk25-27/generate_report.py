#!/usr/bin/env python3
import json
from pathlib import Path
b=Path(__file__).resolve().parent
lines=['# Java 25 / Java 27 comprehensive benchmark','', 'This report keeps runtime replacement, framework upgrade and native compilation as separate experiments. All observations use the same isolated persisted Merchant Q/E/read/update contract, lazy ten-connection pools and explicit execution logging disabled. Heap flags are -Xms128m/-Xmx512m; OS caches are not cleared. Thirty fresh starts and five bounded fixed-warmup samples are required for each reported variant.','']
env=json.loads((b/'host-environment.json').read_text())
lines += ['## Operating environment', '', '| Component | Configuration |', '|---|---|',
 f"| OS / architecture | {env['os']}; {env['architecture']} |",
 f"| Linux kernel | {env['kernel']} |",
 f"| CPU | {env['cpu']}; {env['sockets']} socket, {env['physical_cores']} physical cores / {env['logical_cpus']} logical CPUs |",
 f"| Memory | {env['memory_bytes']/1024**3:.2f} GiB usable ({env['memory_bytes']:,} bytes reported by Linux) |",
 f"| Storage | {env['disk_model']}, SSD; {env['disk_bytes']/1024**3:.1f} GiB physical capacity |",
 f"| Benchmark filesystem | {env['benchmark_filesystem']}; {env['filesystem_bytes']/1024**3:.1f} GiB filesystem capacity |", '',
 'Application JVMs/native executables and the Python HTTP load generator run on this same host. PostgreSQL 17.11 runs in Docker and is accessed over loopback; Redis 7.4.11 is installed but not exercised by this workload. Other services share the host. CPU affinity and frequency are not pinned, and OS caches are not cleared. No disk throughput or IOPS measurement is claimed.', '',
 f"[Host inventory](host-environment.json) was collected at {env['collected_at_utc']}. Its {env['available_bytes_at_inventory']/1024**3:.1f} GiB available filesystem space is a post-campaign snapshot, not a measurement throughout the runs. Per-batch environment and toolchain records remain linked below.", '']
for title,pattern in [('Existing framework bundles, new JVMs','results-*'),('Current framework wrappers, new JVMs','current-results-*'),('Current/legacy native deployment stacks, GraalVM 25','native25/results-*')]:
 dirs=[p for p in sorted(b.glob(pattern)) if (p/'startup-summary.json').exists() and (p/'load-summary.json').exists()]
 if not dirs:
  lines += ['## '+title, '', 'This campaign is not complete. No performance result is reported.', '']
  continue
 r=dirs[-1];s=json.loads((r/'startup-summary.json').read_text());load=json.loads((r/'load-summary.json').read_text());rel=r.relative_to(b)
 lines+=['## '+title,'',f'Completed raw batch: `{rel}`.','', '| Variant | HTTP ready (s) | Business ready (s) | Business RSS (MiB) | Settled RSS (MiB) | Warm read req/s | Warm write req/s |','|---|---:|---:|---:|---:|---:|---:|']
 for v,d in s.items():
  val=lambda key:d[key]['median'];l=load[v]
  lines.append(f"| {v} | {val('web_ready_s'):.3f} | {val('business_ready_s'):.3f} | {val('business_ready_rss_mib'):.1f} | {val('settled_rss_mib'):.1f} | {l['read_rps']['median']:.1f} | {l['write_rps']['median']:.1f} |")
 lines += ['', '| Variant | First read (ms) | First update to 49 (ms) | Warm read p95 (ms) | Warm write p95 (ms) | CPU by business readiness (s) | Threads after read |', '|---|---:|---:|---:|---:|---:|---:|']
 for v,d in s.items():
  val=lambda key:d[key]['median'];l=load[v]
  lines.append(f"| {v} | {val('first_read_ms'):.1f} | {val('first_write_49_ms'):.1f} | {l['read_p95_ms']['median']:.1f} | {l['write_p95_ms']['median']:.1f} | {val('business_ready_cpu_s'):.2f} | {val('business_ready_threads'):.0f} |")
 lines+=['', 'Warm p95 columns are medians across the five load samples of each sample’s p95. First-read/update columns are medians across 30 fresh starts. CPU is process CPU accumulated by the first successful business read.', '', f'[Startup CSV]({rel}/startup.csv), [load CSV]({rel}/load.csv), [environment]({rel}/environment.json).','']
 if pattern == 'native25/results-*':
  prov=json.loads((b/'native25/build-provenance.json').read_text())
  lines += ['| Native variant | Executable (MiB) | Build elapsed | Compiler peak RSS (MiB) |', '|---|---:|---:|---:|']
  for v in s:
   name='quarkus-probe-1-runner' if v=='quarkus' else v
   build=prov['builds'][v]['build-configured.log'];size=prov['inputs'][v][name]['bytes']/1024**2
   lines.append(f"| {v} | {size:.1f} | {build['elapsed']} | {int(build['peak_rss_kib'])/1024:.1f} |")
  lines += ['', '[Native build provenance](native25/build-provenance.json). Build cost describes the successful configured attempt; failed attempts are retained separately.', '', '![JVM and native Java 25 deployment observations](../../../figures/08-native25.png)', '', 'Native/JVM comparisons include framework AOT and native-specific integration. The legacy adapter registers its existing SQL parsers explicitly. G1 and Serial GC differ.', '']
 if pattern == 'results-*':
  lines += ['![Historical Java 21 and new Java 25/27 runtimes](../../../figures/06-jdk-runtime.png)', '', 'Bars show medians; whiskers show p10–p90 of 30 observations, not confidence intervals. The historical Java 21 distribution differs from the Oracle 25/27 pair.', '']
 elif pattern == 'current-results-*':
  lines += ['![Upgraded frameworks on Java 25 and 27](../../../figures/07-current-frameworks.png)', '', 'Both JVMs run identical Java 25 wrappers. Micronaut labels identify its core version; the platform version is 5.2.2.', '']
lines += ['## Interpretation', '']
for pattern,label in [('results-*','unchanged bundles'),('current-results-*','upgraded framework wrappers')]:
 batches=[p for p in sorted(b.glob(pattern)) if (p/'startup-summary.json').exists()]
 if batches:
  summary=json.loads((batches[-1]/'startup-summary.json').read_text())
  changes=[100*(summary[v[:-5]+'jdk27']['business_ready_s']['median']/d['business_ready_s']['median']-1) for v,d in summary.items() if v.endswith('jdk25')]
  lines += [f'For {label}, replacing Oracle Java 25 with 27 changed median business readiness by {min(changes):+.1f}% to {max(changes):+.1f}%. The observed matrix does not show a universal startup acceleration. These are deployment observations from one shared host, not a prediction for other services.', '']
lines += ['The recorded defaults use G1 on both new JVMs. `UseCompactObjectHeaders` is false on Java 25 and true on Java 27. Results include these VM-default differences; RSS changes have not been attributed to an individual optimization.', '']
lines += ['The initial native batch completed startup sampling but failed under Micronaut load because Netty’s Java 25 shared Arena closure required `-H:+SharedArenaSupport`. That incomplete batch is excluded. The accepted retry passed the same bounded workload smoke check, all 30 starts/five load samples and post-campaign business checks. Netty/Logback initialization and two legacy Redisson descriptor types also needed explicit settings; dependencies and business code were retained.', '', 'Legacy native startup regressed relative to the retained GraalVM 21 batch despite unchanged 91 JARs. A separate three-start diagnostic with explicit Spring AOT enabled did not remove the regression. Its cause remains unisolated; it is not evidence of an inherent TeaQL reflection cost or a general GraalVM regression.', '']
lines+=['## Scope and reproduction','', 'Legacy deployments use TeaQL 1.163 and domain 296; modern deployments use TeaQL 1.554 and domain 297. The legacy/modern comparison changes the deployment stack and domain revision, not only one framework switch. All modern JVM deployments retain identical TeaQL/domain artifacts and business-operation class bytes.','', 'Track 1 changes the runtime only: original Spring 3.2.0, Quarkus 3.8.3, Micronaut Platform 4.3.8 / Core 4.3.14, standalone classpath and JPMS bundles are unchanged Java 21 bytecode. Both new JDKs are Oracle HotSpot (25.0.4.1 and 27+35). Historical Java 21 used Amazon Corretto, so comparisons with it include a distribution change. The direct 25/27 pair uses identical deployed inputs.','', 'Track 2 uses Java 25 wrappers on Spring Boot 4.1.1, Quarkus 3.40.1 LTS and Micronaut Platform 5.2.2 / Core 5.2.15. Shared domain, operation and published TeaQL 1.554 artifacts are retained. Dependency closure/HTTP/DI integration change, so improvements must not be attributed solely to JDK or reflection. Vendor-declared support and observed 27 compatibility are distinct.','', 'Native 25 uses Oracle GraalVM 25.0.4+7.1, four compilation threads, an 8 GiB compiler heap, compatibility instructions, optimization level 2 and Serial GC. Compilations wait until JVM campaigns finish; native sampling waits for all accepted builds and business checks. The old native adapter explicitly registers its 17 original SQL parsers. JVM 25/27 results are not native 27 results. Oracle’s 27 GraalVM script-friendly archive returns HTTP 404; the official release-train announcement states no JDK 26/27/28 builds are planned.','', 'Read samples contain 400 requests at concurrency 4; writes contain 40 requests at concurrency 1 after 200 read/20 write warmup operations. Rates include HTTP/client scheduling and database work; they do not establish maximum capacity. The invalid-hours check is an adapter fixture guard, not authorization or database-failure rollback. Full-service migration, schema parity, Android and entity/protected-reference JSON remain separate work.','', '[Method](README.md), [runtime collector](collect_jdk.py), [current-framework collector](collect_current.py), [toolchain provenance](toolchain-provenance.json), [dependency provenance](build-provenance.json). Historical [Java 21 JVM](../matched-six/results-2026-10-09.md) and [native 21](../native/results.md) batches are retained independently.']
(b/'results.md').write_text('\n'.join(lines)+'\n');print(b/'results.md')

# Preserve ranking annotations when regenerating the report.
import importlib.util
_rank_path = Path(__file__).resolve().parent.parent / "rank_tables.py"
_rank_spec = importlib.util.spec_from_file_location("rank_tables", _rank_path)
_rank_module = importlib.util.module_from_spec(_rank_spec)
_rank_spec.loader.exec_module(_rank_module)
_rank_module.annotate_file(Path(__file__).resolve().parent / "results.md")
