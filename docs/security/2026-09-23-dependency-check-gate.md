# Java dependency-check gate — 2026-09-23

Issue: [#131](https://github.com/teaql/teaql-java/issues/131). This record describes the tested dependency set, not a claim that future scans will stay clean.

## Reproducible scan

- Source revision before this change: `206a241` (`origin/main`).
- Scanner: OWASP Dependency-Check Maven `13.0.0`; fail on scanner errors and CVSS >= 7.0. CI retains HTML and JSON reports even if the scan fails.
- Data: trusted CI snapshot from [run 35812023476](https://github.com/teaql/teaql-java/actions/runs/35812023476), artifact `teaql-odc-v13.0.0-Linux`. NVD API last checked `2026-09-23T02:52:46Z`, last modified `2026-09-23T02:16:55Z` (from report `scanInfo`). The existing CI freshness check must pass before scanning.
- Local reactor scan time: `2026-09-23T05:46:26Z`–`05:47:40Z`. All 37 reactor modules produced reports. Exactly **zero CVSS >= 7 findings** in those reports; the command exited 0. The school and order standalone libraries and the school standalone application also passed separate local scans, and CI now scans those three POMs after installing the school library. No vulnerability suppression was added.
- Command (substitute the downloaded snapshot path):

  ```bash
  mvn -B org.owasp:dependency-check-maven:13.0.0:check \
    -DdataDirectory=/path/to/teaql-odc-data -DautoUpdate=false \
    -DfailOnError=true -DfailBuildOnCVSS=7 -Dformats=HTML,JSON
  ```

## Remediation and control test

| Dependency source | Before | After | Verification |
| --- | --- | --- | --- |
| Runtime Spring Boot BOM | 3.2.0 (Spring Core 6.1.1, Logback Core 1.4.11) | 4.1.1 (Spring Core 7.0.9) | Full reactor compile and tests; 37-module scan |
| Checked-in generated Java examples | Boot 4.1.0 (Spring Core 7.0.8, Tomcat Core 11.0.22) | Boot 4.1.1, with Tomcat Core/EL/WebSocket 11.0.26 pinned together | Dependency tree, reactor compile and scan |
| Checked-in generated Java libraries | Hutool 5.8.20 | Hutool 5.8.47 | Reactor compile and scan; agrees with generator [draft PR #213](https://github.com/teaql/teaql-code-gen/pull/213) |
| MySQL test dependency | Testcontainers MySQL 1.21.4 managed by old Boot BOM | Explicit 1.21.4 test-scope version, as Boot 4 no longer manages the legacy coordinate | Full reactor compile and tests |

The gate was tested both ways against the *same* NVD snapshot: the upgraded reactor passed; an isolated Maven project with `cn.hutool:hutool-all:5.8.20` failed with CVE-2023-24163 (9.8), CVE-2023-42276 (9.8), and CVE-2023-51075 (7.5). This proves that the threshold is effective, not merely documented. `mvn -q test` and `mvn -q install -DskipTests` passed on JDK 21 after the final dependency changes. The standalone school and order generated libraries each passed `mvn test` and a CVSS >= 7 scan; the school application also passed its standalone scan with the same snapshot.

## Remaining scanner findings, below the gate

| Artifact and path | Reported finding | Triage |
| --- | --- | --- |
| `teaql-android` → `com.google.android:android:4.1.1.4` (`provided`) → `httpclient:4.0.1` | CVE-2011-1498 (4.3), CVE-2014-3577 (5.8), CVE-2020-13956 (5.3) | Legacy Android compile stub; the TeaQL Android source imports SQLite APIs, not Apache HttpClient. The dependency is `provided`, not packaged by TeaQL. Keep visible for any downstream Android deployment audit. |
| Same `provided` Android stub → `opengl-api:gl1.1-android-2.1_r1` | CVE-2012-6636 (6.8) | NVD description concerns `WebView.addJavascriptInterface` on old Android, not the OpenGL API jar. This is a component/CPE misattribution, not a finding in TeaQL's OpenGL usage. |
| `teaql-opentelemetry` → `io.opentelemetry:opentelemetry-api:1.65.0` (compile) | CVE-2026-54285 (5.3) | The [vendor advisory](https://github.com/open-telemetry/opentelemetry-js/security/advisories/GHSA-8988-4f7v-96qf) assigns this CVE to the **JavaScript** `@opentelemetry/core` package. The separate [Java advisory](https://github.com/open-telemetry/opentelemetry-java/security/advisories/GHSA-rcgg-9c38-7xpx) is CVE-2026-45292 and is fixed in Java API 1.62.0; this project resolves 1.65.0. Do not conflate the two. |

No medium finding was silently suppressed. This review covers reported artifacts and dependency scopes, not a proof that every application use is unreachable.

## Follow-up boundaries

- The checked-in examples now pass, but **future generation remains a separate source of truth**. The generator templates still need the Boot 4.1.1 and aligned Tomcat 11.0.26 changes; Hutool 5.8.47 is in draft PR #213. Until those changes land and are regenerated, a new generated project can reproduce the old dependency set. The new gate will expose that regression for reactor examples.
- The two standalone examples are outside the 37-module reactor. CI now runs their separate dependency scans, but still relies on the checked-in generated projects; update generator templates before treating newly generated projects as covered.
- Review the remaining lower-severity findings when changing Android or telemetry dependencies; do not turn the CVSS threshold into a blanket risk acceptance.
