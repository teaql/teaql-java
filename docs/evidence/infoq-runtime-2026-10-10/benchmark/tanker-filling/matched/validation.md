# Completed evidence checks

- 120 startup CSV rows: 30 per variant and all four variants in every randomized block.
- Every startup median and nearest-rank p90 independently recomputed from retained CSV.
- 20 load rows: five per variant; all rates, sample medians and p95 values checked against raw latencies.
- 8,000 measured read latencies and 800 measured write latencies retained; workload sizes checked.
- 120 startup logs and 20 load logs retained, with no ERROR/Exception in timed logs.
- Deployed legacy and modern JAR names/hashes exactly match the local build provenance; 45 shared JARs have identical versions and hashes.
- All four post-campaign contracts passed repeated schema setup, nonempty Q/E reads, updates to 49 and 48, and the weight query. Invalid hours=47 returned HTTP 500 and did not mutate data. This is adapter input rejection, not a framework authorization or rollback test.
- Separate relation diagnostics confirmed parent-name E extraction matches the direct persisted root value within each version. Root labels differ and are disclosed.
- Final article/report relative links resolve; the figure was visually inspected after rendering all 120 observations.
- Original baseline /version on 18880 and earlier JPMS probe /version on 18882 remained available after the experiment.

result-checksums.json hashes the completed result directory. This is evidence integrity and descriptive verification, not a statistical generality or native-image claim.
