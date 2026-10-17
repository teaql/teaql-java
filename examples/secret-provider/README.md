# Context-owned Secret Provider example

Run `bash examples/secret-provider/verify.sh`. The runtime-owned test binds a
symbolic reference to an owner-only local file at trusted startup and resolves
it through `UserContext` with a comment. Changing the file is visible at the
next resolution.

A second test installs a fixed Vault KV v2 path and separate bootstrap token
provider, then uses the real HTTP adapter against a loopback fixture. It checks
the path, token header, response version, and a stalled-response timeout. Both
tests run twice, and the verifier rejects secret canaries in output. This is a
controlled fixture, not a production Vault deployment.

Private-CA rollover is explicit: `trustedRootsPem(List.of(oldPem, newPem))`
temporarily trusts both roots for this named HTTPS profile, replacing system
roots rather than expanding process-wide trust. A later profile with only
`trustedRootPem(newPem)` retires the old CA for new requests. The TLS example
checks reads through both CA-signed server certificates during overlap and
rejects the old certificate after retirement. Each list element contains one
PEM certificate; the bounded profile accepts at most four roots. In-flight
requests and production certificate rollout require separate coordination.
