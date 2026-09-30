# TeaQL DuckDB Provider

This module provides DuckDB support for TeaQL.

The executable compatibility gate defaults to DuckDB JDBC `1.5.5.1`:

```bash
mvn -pl teaql-duckdb test
```

An older driver can be checked with the same tests, for example:

```bash
mvn -pl teaql-duckdb clean test -Dduckdb.version=1.0.0
```

Compatibility claims refer to the Java JDBC driver exercised by this gate,
not to a separately installed DuckDB CLI.

## Test Report
The historical Vending Machine report used DuckDB `1.0.0`. The retained Maven
gate verifies current JDBC compatibility; read [TEST_REPORT.md](./TEST_REPORT.md)
for the earlier test scope.
