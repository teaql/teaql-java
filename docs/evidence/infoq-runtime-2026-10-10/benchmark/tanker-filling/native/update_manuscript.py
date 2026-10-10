#!/usr/bin/env python3
import argparse,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('results',type=Path);a=p.parse_args();base=Path(__file__).resolve().parent
article=base.parents[2]/'java-runtime-android-native-image.md';s=article.read_text();data=json.loads((a.results/'startup-summary.json').read_text())
labels={'legacy':'TeaQL 1.163 / Spring + native bootstrap','spring':'TeaQL 1.554 / Spring','quarkus':'TeaQL 1.554 / Quarkus','micronaut':'TeaQL 1.554 / Micronaut*','standalone':'TeaQL 1.554 / standalone'}
block='''## Compile and test the same workflow as native executables

Five Linux executables passed the same schema, Q/E, JSON and persisted-update contract. Thirty randomized startup blocks used GraalVM 21.0.12, compatibility instructions and the same heap flags. Native Serial GC and JVM default GC differ; OS caches were not cleared. Standalone classpath and JPMS become one native program rather than two JVM loading modes.

| Native stack | First business read complete (s) | RSS after read (MiB) |
| --- | ---: | ---: |
'''
for v in ['legacy','spring','quarkus','micronaut','standalone']:
 if v in data:
  d=data[v];block+=f"| {labels[v]} | {d['business_ready_s']['median']:.3f} | {d['business_ready_rss_mib']['median']:.1f} |\n"
block+='''
The legacy image initially served HTTP but failed a real query: no parser for AND. SQLRepository discovered parsers through Hutool package scanning, which found no JAR directory in the image. Its adapter required explicit registration of the original parsers. Current [PortableSQLRepository](https://github.com/teaql/teaql-java/blob/main/teaql-sql-portable/src/main/java/io/teaql/core/sql/portable/PortableSQLRepository.java) already constructs its parser registry directly. Reflection metadata alone did not replace discovery.

Equivalent JVM agent traces recorded 61 reflection entries for standalone, with no TeaQL or application-package types; the pre-adaptation legacy trace recorded 1,092, including 80 TeaQL and 94 application-package types. This observes one tested workflow, not every application path or total compiler registration.

Integration still mattered: JDBC/logging needed metadata, Quarkus needed optional-dependency linkage configuration, and Micronaut needed Netty initialization changes. Micronaut (*) uses a Jackson 2.16.1 profile that has a separately measured JVM control; the original JVM campaign used 2.22.3.

![Matched JVM and Linux native business-read latency and RSS](figures/05-native-startup.png)

*Figure 5. Thirty fresh processes per mode; medians and p10–p90. Micronaut uses matching dependencies. Memory is process RSS.*

The [native report](benchmark/tanker-filling/native/results.md) retains failures, required migration work, build cost, executable size and bounded read/write observations. Native success covers this business slice; full-service migration, entity/protected-reference JSON and Android remain separate validation tasks.

'''
anchor='## Keep portability claims tied to their evidence'
if '## Compile and test the same workflow as native executables' in s:
 first=s.index('## Compile and test the same workflow as native executables');end=s.index(anchor,first);s=s[:first]+s[end:]
s=s.replace(anchor,block+anchor)
article.write_text(s);print('Manuscript words:',len(s.split()))
