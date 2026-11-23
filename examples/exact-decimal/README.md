# Generated Java/SQLite exact Decimal example

This is a small, generation-dependent runtime example. It uses two entities,
one modeled `number()` field, a local generator checkout and this local Java
runtime checkout. No generated domain source is edited.

```bash
TEAQL_CODEGEN_DIR=/path/to/teaql-code-gen bash examples/exact-decimal/verify.sh
```

The verifier copies both source checkouts into one owned temporary build area,
uses the validated local generator entry point, and builds the runtime, generated
library and plain console together in a Maven reactor. It never installs or
deploys TeaQL artifacts into a shared Maven cache. Both rounds use the same two
SQLite databases (one Q/E probe database and one console startup database):

- Generated Q equality, strong `BigDecimal` hydration and E evaluation.
- Audited saves of `0.1` and `0.2`, followed by an exact modeled Q `SUM=0.3`.
- A `3.50` scale round-trip and audited update to
  `123456789012.3456789` without losing numeric precision.
- Exponent-form `BigDecimal` inputs (`1E-7`, `1E+8`) through generated
  audited mutation, typed Q filtering and E hydration.
- The actual generated plain Java/JDBC console startup, including repeated
  `ensureSchema` against one workspace database.

It also verifies that the generated library hash does not change while the
application code is compiled and executed. Three packaged runtime class hashes
must match the classes built from the paired checkout, excluding accidental
reuse of a cached published version. The Q/E probe retains exactly five rows
across both rounds. Every run retains its logs, source identities, generated
file hashes and small databases under the printed evidence directory; disposable
compilers, source copies and JARs are removed even on failure. Set
`TEAQL_JAVA_EXACT_EVIDENCE_DIR` to a new path to choose that directory.
This does not qualify migration of
pre-existing SQLite `NUMERIC` columns or participation in an application-owned
Spring `@Transactional` boundary; TeaQL's own graph save is transactional.
Committed mutation replication is tested separately on its P1 branch and is
not required for this isolated Decimal fix.
