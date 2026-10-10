#!/usr/bin/env python3
"""Integrate final retained measurements into the English working draft."""
import argparse,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('results',type=Path);a=p.parse_args();s=json.loads((a.results/'startup-summary.json').read_text());m=lambda v,k:s[v][k]['median']
article=Path(__file__).resolve().parents[3]/'java-runtime-android-native-image.md';text=article.read_text();start=text.index('## Separate demonstrated architecture from portability evidence') if '## Separate demonstrated architecture from portability evidence' in text else text.index('## Measure runtime, container and module-path changes separately');end=text.index('## Beyond the JVM:',start)
section='''## Measure runtime, container and module-path changes separately

We tested a bounded workflow from a tanker-filling service: query a persisted Merchant, load its Platform relation, extract its name with E, update a field and reload it. Four launchers executed this same contract. The legacy version uses saveGraph; the modern version uses auditAs(...).save. Both updates were checked against persisted results.

The experiment used Corretto 21, Spring Boot 3.2.0 where applicable, matching common dependencies, ten-connection pools, identical heap limits and matching Merchant projections/root IDs. Thirty randomized blocks each launched all four variants. Schema setup stayed outside timing; OS caches were not cleared. These are medians from one shared host.

| Launcher | Web ready (s) | First business read complete (s) | RSS after read (MiB) |
| --- | ---: | ---: | ---: |
'''
for v,label in [('legacy-spring','TeaQL 1.163 / Spring'),('modern-spring','TeaQL 1.554 / Spring'),('modern-classpath','TeaQL 1.554 / standalone classpath'),('modern-jpms','Same standalone / JPMS')]:section+=f'| {label} | {m(v,"web_ready_s"):.3f} | {m(v,"business_ready_s"):.3f} | {m(v,"business_ready_rss_mib"):.1f} |\n'
reduction=100*(1-m('modern-spring','web_ready_s')/m('legacy-spring','web_ready_s'))
section+=f'''
Keeping Spring/Tomcat constant, the modern stack reduced median Web-ready time by {reduction:.1f}%. That measures combined runtime, packaging and assembly changes. The old library defines 55 Spring repository beans; the modern adapter explicitly installs a generated module and PostgreSQL provider. Separately, replacing Spring/Tomcat with the JDK HTTP server reduced startup further.

The cost was not uniformly lower: first update and readback took {m('modern-spring','first_write_49_ms'):.1f} ms with modern Spring versus {m('legacy-spring','first_write_49_ms'):.1f} ms with legacy Spring. The same standalone main took {m('modern-jpms','web_ready_s')-m('modern-classpath','web_ready_s'):.3f} seconds longer on the module path. JPMS resolved the full bundle, including unused Spring modules; application/domain JARs were automatic modules.

![Business-ready time and RSS observations across four matched launchers](figures/04-matched-startup.png)

*Figure 4. Thirty observations per launcher; points show individual JVMs. Memory is process RSS. The comparison separates runtime-stack changes, HTTP-container replacement and module-path loading.*

Migration required generator fixes and constant-ID remapping. Full schemas differ, and modern view descriptors currently create tables. The [experiment report](benchmark/tanker-filling/matched/results-2026-10-09.md) retains those limits, commands, dependency hashes, raw startup observations and a separate fixed-warmup read/write campaign. This slice does not establish complete service migration, production capacity or native-image performance.

## Keep portability claims tied to their evidence

The historical examples report generation, compilation and some framework startup checks. Their runtime versions differ from the measured 1.554 slice. Android consumes selected artifacts outside JPMS; no new Android device or native-image build was tested here.

A stronger cross-provider claim requires the same workflow, relation loading, policy rejection and forced-failure rollback on each adapter. Local SQLite transactions also leave synchronization, duplicate delivery, version conflicts and server authorization to an application protocol.

The transferable sequence is to inspect dependency closure, extract execution contracts, register construction/access explicitly, and validate equivalent behavior before measuring deployment costs. [Recorded framework results](https://github.com/teaql/teaql-java-app-examples/blob/main/vending-machine-tests.md).

'''
article.write_text(text[:start]+section+text[end:])
print('Manuscript whitespace-delimited words:',len(article.read_text().split()))
