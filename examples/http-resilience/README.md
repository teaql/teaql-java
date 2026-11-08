# Context-owned HTTP example

Run `bash examples/http-resilience/verify.sh`. The verifier starts a loopback-only
HTTP fixture, installs one named service profile on `DefaultUserContext`, and
executes the same call through both the provider-neutral Context API and the
Agent HTTP tool. Each native test runs two rounds on the same server, Context
and service: request/trace counters grow from two to four without resetting.
The verifier executes that native example twice. An optional fresh
`TEAQL_HTTP_EXAMPLE_EVIDENCE_DIR` retains compact per-execution JUnit reports;
it never stores build trees or credentials.

Only trusted startup code registers an upstream base URI. Application code uses
its alias and a relative path; it cannot substitute an arbitrary URL. Each call
has a comment and purpose. By default it makes one physical attempt and never
follows redirects. Wider retry/circuit/rate cases live in `teaql-tool-http` tests.

## Profile-scoped private HTTPS trust

Trusted startup may configure `.trustedRootPem(pem)` or
`.trustedRootsPem(List.of(oldRoot, newRoot))` on an HTTPS service profile.
The explicit one-to-four-root set replaces system trust **for that profile**;
certificate and hostname verification remain enabled. Each PEM input contains
one certificate and is at most 64 KiB. Empty, duplicate, bundled, malformed,
oversized or plaintext-profile inputs reject before use. Unconfigured profiles
continue to use ordinary system roots.

Profiles are immutable snapshots. Rotate by constructing a new service with
old+new roots, then new-only roots after upstream rollover. This does not mutate
other clients, Contexts or the process trust store. The verification script also
runs the standalone `HttpProfileTrustTest` loopback HTTPS matrix, including two
rounds over both live peers without resetting their counters. It requires local
OpenSSL and removes generated private keys; no Vault or external service is used.
