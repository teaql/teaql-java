# Business ID focused runtime example

This example verifies the new Java runtime Business ID boundary without running
the full multi-module regression suite. It covers the default eight-digit
daily sequence, controlled Context date, root scoping, Mutation Ledger retry
stability, explicit SQLite schema setup, cross-instance allocation, restart,
strong `OrderNumber` parsing, and immutable established aggregates.

```bash
mvn -pl examples/business-id-runtime -am \
  -Dtest=BusinessIdRuntimeExampleTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```
