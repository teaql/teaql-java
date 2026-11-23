# Edit an order using an opaque line reference

The [model](model.xml) defines the merchant, sales order and line. Its
Java workspace installs the runtime reference provider and current
merchant editor policy, resolves the stored line and changes its quantity.
The original runtime tests still exercise key rotation, expiry and raw
diagnostics.

```bash
bash examples/round-trip-reference-runtime/verify.sh
```

## Original adapter-neutral calibration

This example proves that the reusable runtime serialization boundary can be
called by any Web adapter without depending on Spring, JAX-RS, Servlet, or a
specific JSON library.

It covers governed issue/resolve through `UserContext`, trusted Actor and
Domain Root binding, purpose/document/Aggregate scope, tamper rejection,
expected-type validation, expiry, key rotation, current authorization at issue
and consume time, and the explicit development/test-only raw diagnostic shape.

The core deliberately does not define tenant or role semantics. Runtime
assembly installs the trusted principal and application-owned authorization
policy. The focused runtime tests retain the same deterministic `tqr1` golden
vector as TeaQL Rust and enumerate RAW-01 through RAW-09.

Run locally against repository sources:

```bash
mvn -pl examples/round-trip-reference-runtime -am test
```
