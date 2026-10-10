# Completed evidence audit

validate_results.py passed against the final retained batch:

- 180 startup observations, 30 per variant, all six variants in each randomized block.
- Every median and nearest-rank p90 recomputed from CSV.
- 30 fixed-warmup samples, five per variant; all rates, medians and p95 values recomputed from raw per-request latencies.
- 12,000 measured reads and 1,200 measured writes retained; workload sizes checked.
- 210 timed logs: no ERROR/Exception and no TeaQL SQL execution trace output.
- Every deployed JAR path/hash exactly matches the local build-provenance inventory.
- Both shared implementation class files, the generated domain JAR and all eight TeaQL JARs have identical bytes across modern adapters.
- The completed chart was visually inspected using all 180 observations; zero-based axes and individual dots are retained.

Post-campaign response checks are retained in contract-verification.json. Input rejection here is an adapter guard, not proof of framework authorization or transaction rollback. The root-field diagnostic linked from the report was performed separately in the preceding validation; it is not a new native/device claim.

result-checksums.json hashes the final result directory. These checks establish descriptive integrity for this campaign, not generality across production workloads or latest framework versions.
