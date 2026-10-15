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
