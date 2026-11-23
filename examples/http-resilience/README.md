# Synchronize supplier order status

The [supplier model](model.xml) defines the supplier and orders awaiting
confirmation. The generated workspace installs a named HTTP profile on
the Context and reconciles orders against a loopback supplier. A direct response
and one retry update their own orders. Retry exhaustion, cancellation and a
response for another order leave the local versions unchanged.

```bash
bash examples/http-resilience/verify-sync.sh
```

## Original HTTP calibration

Run `bash examples/http-resilience/verify.sh`. The verifier starts a loopback-only
HTTP fixture, installs one named service profile on `DefaultUserContext`, and
executes the same call through both the provider-neutral Context API and the
Agent HTTP tool. It runs twice without deleting or resetting any fixture.

Only trusted startup code registers an upstream base URI. Application code uses
its alias and a relative path; it cannot substitute an arbitrary URL. Each call
has a comment and purpose. By default it makes one physical attempt and never
follows redirects. Wider retry/circuit/rate cases live in `teaql-tool-http` tests.
