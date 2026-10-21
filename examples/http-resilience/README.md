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
