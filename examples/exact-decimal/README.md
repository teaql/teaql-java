# Generated Java/SQLite exact Decimal example

This is a small, generation-dependent runtime example. It uses two entities,
one modeled `number()` field, a local generator checkout and this local Java
runtime checkout. No generated domain source is edited.

```bash
TEAQL_CODEGEN_DIR=/path/to/teaql-code-gen bash examples/exact-decimal/verify.sh
```

The verifier creates a fresh generated Java library and console workspace,
installs only the local runtime into Maven's local cache, then runs twice:

- Generated Q equality, strong `BigDecimal` hydration and E evaluation.
- Audited saves of `0.1` and `0.2`, followed by an exact modeled Q `SUM=0.3`.
- A `3.50` scale round-trip and audited update to
  `123456789012.3456789` without losing numeric precision.
- Exponent-form `BigDecimal` inputs (`1E-7`, `1E+8`) through generated
  audited mutation, typed Q filtering and E hydration.
- The actual generated Spring console startup, including repeated
  `ensureSchema` against one workspace database.

It also verifies that the generated library hash does not change while the
application code is compiled and executed. Every run retains its logs under
the printed temporary evidence directory. This does not qualify migration of
pre-existing SQLite `NUMERIC` columns or participation in an application-owned
Spring `@Transactional` boundary; TeaQL's own graph save is transactional.
Committed mutation replication is tested separately on its P1 branch and is
not required for this isolated Decimal fix.
