# Post-campaign full relation check

These sources were compiled separately against the retained legacy/modern distributions and executed only after all timed samples. LegacyRelationMain starts the same legacy Spring configuration on a separate loopback port. RelationOperations copies the modern probe bootstrap without changing the measured JAR; ModernRelationMain invokes the additional diagnostic. No reflection is used to obtain the private context.

Both query the identified Merchant with selectPlatform, evaluate E.merchant(...).getPlatform().getName(), independently query Platform 1, and assert equal non-null names within the respective database. proof.json records the actual results. Labels differ between generated models (液体充装系统 versus 液体业务平台), so the test asserts correspondence to each persisted value, not cross-version label equality. The timed Merchant projection uses only the Platform ID.

Compile with Java 21 and the respective distribution's lib/* as the classpath. Run with BENCHMARK_DB_PASSWORD, PROBE_DB_URL selecting the corresponding isolated database, and PROBE_PORT=18885 for legacy. Retained logs are ../legacy-full-relation.log and ../modern-full-relation.log. These diagnostics are functional evidence, not timed samples or native/module-path build validation.
