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
requests already holding the old HTTP service may finish within their existing
budget after the Context profile is replaced; replacement is not retroactive
revocation. The example pauses one accepted TLS request, retires its CA for
new calls, proves a second call sends no HTTP request, then lets the first
finish. Production certificate rollout still requires traffic coordination and
draining; do not discard the old CA before all instances have the overlap set.
The controlled two-instance fixture keeps old- and new-CA servers online
together, advances independent Contexts through mixed and overlap profiles,
rejects an old endpoint after trust retirement, then restores the old
instance by routing it to the new endpoint. It checks exact server-side HTTP
request counts; it is not a separate-host production rollout.
The example also accepts an old-CA Vault read but deliberately never returns
its response. The read ends within its 900 ms whole-call budget as
`UNAVAILABLE`, without a second HTTP request; only then is the Context switched
to new-only trust. This is a bounded-drain test, not proof that changing trust
immediately cancels in-flight requests.
