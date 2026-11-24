# Exact settlement quotes

The settlement desk quotes wharf and customs fees, recalculates an offshore
settlement and records two rate adjustments using precise decimals. The
`java-lib-core` domain and `java-app-console` workspace are generated from
`model.xml`. Application probes live in the console workspace.

```bash
bash examples/exact-decimal/verify.sh
```

The verifier copies this source checkout into an owned temporary build area
and checks the recorded generated scopes. It binds the two copied Maven
manifests to the local runtime version and retains both manifest hashes. It
builds the local runtime, generated library and plain console in one Maven reactor. Both rounds use the same two
SQLite databases (one Q/E probe database and one console startup database):

- Generated Q equality, strong `BigDecimal` hydration and E evaluation.
- Audited saves of `0.1` and `0.2`, followed by an exact modeled Q `SUM=0.3`.
- A `3.50` scale round-trip and audited update to
  `123456789012.3456789` without losing numeric precision.
- Exponent-form `BigDecimal` inputs (`1E-7`, `1E+8`) through generated
  audited mutation, typed Q filtering and E hydration.
- The actual generated plain Java/JDBC console startup, including repeated
  `ensureSchema` against one workspace database.

It also verifies the generated library hashes before and after execution. Five
packaged runtime class hashes must match classes built from the paired checkout.
The Q/E probe retains exactly five quotes
across both rounds. Every run retains its logs, source identities, generated
file hashes and small databases under the printed evidence directory; disposable
compilers, source copies and JARs are removed even on failure. Set
`TEAQL_JAVA_EXACT_EVIDENCE_DIR` to a new path to choose that directory.
This does not qualify migration of
pre-existing SQLite `NUMERIC` columns or participation in an application-owned
Spring `@Transactional` boundary; TeaQL's own graph save is transactional.
Committed mutation replication is tested separately on its P1 branch and is
not required for this isolated Decimal fix.
