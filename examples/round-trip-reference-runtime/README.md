# Round-Trip Reference runtime example

This example proves that the reusable runtime serialization boundary can be
called by any Web adapter without depending on Spring, JAX-RS, Servlet, or a
specific JSON library.

It covers governed issue/resolve, opaque context binding, tamper rejection,
cross-context rejection, expected-type validation, key rotation, and the
explicit development-only raw diagnostic representation.

The core contract receives the complete `UserContext`; it does not define what
the context binding means. The runtime customization supplies opaque binding
bytes. Successfully resolving a reference only restores `(type, id, version)`:
the application must still re-run its current authorization and policy checks.

Run locally against repository sources:

```bash
mvn -pl examples/round-trip-reference-runtime -am test
```
