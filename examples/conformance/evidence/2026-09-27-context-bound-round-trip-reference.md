# Java context-bound Round-Trip Reference evidence

Date: 2026-09-27

Tracking: [teaql-java#175](https://github.com/teaql/teaql-java/issues/175)

Baseline: `origin/main` at `9ca9e261a7b8c5f39292374ac7eab2c7cab303d6`.

Scope: the bounded `tqr1` independent-reference core. This evidence does not
claim a complete encrypted document manifest or generated `openDocument` /
`acceptDocument` adapters.

## Implemented boundary

- high-level `UserContext::referenceFor` / `resolveReference` boundary;
- trusted authentication realm, subject and Domain Root identity supplied by
  runtime assembly, never accepted from the wire;
- service/environment, purpose, document, Aggregate type/ID/revision and
  entity type/ID/version binding;
- AES-256-GCM, 96-bit CSPRNG nonce, HKDF-SHA-256 and unpadded Base64URL;
- current encryption key plus decode-only previous keys;
- expiry, service/environment isolation, tamper rejection, fresh nonces and
  restart/rotation continuity;
- current authorization at issue and consume time;
- one runtime-selected diagnostic raw mode with an exact long acknowledgement,
  production failure, explicit ERROR notice, telemetry and response metadata;
- removal of the earlier weaker codec/raw-fallback path so there is one public
  Round-Trip Reference contract.

## Cross-language vector

The fixed inputs used by Rust and Java are identical: service `order-service`,
environment `test-a`, current key `k2` filled with `0x22`, timestamp
`2026-09-27T00:00:00Z`, nonce filled with `0x33`, principal
`oidc/alice/Platform#7`, scope `doc-100/edit-order/Order#100@9`, identity
`OrderItem#42@3`, and lifetime 600 seconds.

Both runtimes assert this exact token:

```text
tqr1.k2.MzMzMzMzMzMzMzMzmhCET15vBdSc1BbFJcrvMMnRP1wAyjunShYlFPnDZL7Gk5V1LuptWyrbiQ8LQSDHFUmN3o6vb-MEgXZuh3N-YkeO55CYLeVgiyBBYslzuSu-fvWC2KMZcnZ5D8BRmWpGYA2Ok5sWe1Fg1aIyadTjVoKB-H2vPW_0okj-
```

## RAW-01 through RAW-09

`ContextBoundRoundTripReferenceTest` contains individually named deterministic
tests for all nine cases: default governed mode, exact development/test opt-in,
near-match rejection, production failure, current authorization after actor
change, mode/wire-shape mismatch, identity/version preservation, and safe
downgrade metadata.

## Commands

```text
mvn -pl teaql-runtime -am \
  -Dtest=ContextBoundRoundTripReferenceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl examples/round-trip-reference-runtime -am test
mvn clean verify
```

The focused runtime suite reports 12 passed tests. The full clean reactor
reports 39/39 modules successful, including 197 core tests, 48 runtime tests,
the migrated security-foundations example, the standalone Round-Trip Reference
example, all database dialect modules, TFP, telemetry, and generated
conformance workspaces. One external OTLP smoke test is skipped because its
external endpoint is not configured. No release is produced by this work.
