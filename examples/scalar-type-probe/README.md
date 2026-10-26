# Generated canonical naming / SQLite probe

Run `node examples/scalar-type-probe/verify-local.cjs` from this runtime checkout.
The example always compiles repository-local runtime modules. It never installs
TeaQL into a shared Maven repository or edits the frozen generated library.
Set `TEAQL_JAVA_SCALAR_EVIDENCE_DIR` to a new absolute evidence directory to keep
the two logs and SQLite database at a chosen location. Repeated rounds use the
same database without cleanup; disposable builds are removed on exit.

The evaluated two-object model tests native Q/E/updaters, canonical Ledger,
Checker/policy/audit keys, physical columns, Value/Null/NotLoaded, twelve malformed
namespace rejections, two required-field rejections, default typed JSON, and a
real second-UPDATE failure after the first UPDATE executes. The same object graph
is retried after rollback, preserving versions and producing two committed audits.
Raw generic mutations are used only for the explicitly labelled negative tests.

`node examples/scalar-type-probe/verify-json-profiles.cjs` additionally compiles
three evaluated model variants and runs each twice. Only the documented root
`json_field_naming` option differs. Native Q/E and database names stay canonical;
JSON names and diagnostic pointers follow camelCase, snake_case or PascalCase.
The standard example entry point runs both verifiers. Set
`TEAQL_JAVA_JSON_PROFILE_EVIDENCE_DIR` to a new directory to retain these logs and
three databases. The type-local mapping accepts canonical keys and explicitly
declared aliases, not incidental physical columns or other profiles' spellings.

The same verifier with `--lifecycle` runs a separate database-backed scenario for
each profile twice. Persisted JSON identities remain clean until changed; an
update writes only the modified object, preserves an untouched partial reference,
and cannot absorb another decoded object's pending changes. Stale versions fail
with optimistic conflict; modified partial objects fail before provider access.
Decoded deletion and recovery retain the version baseline (1 -> 2 -> -3 -> 4).
The standard example entry point also runs this scenario. It retains compact
logs and three test databases, not disposable reactors or dependency copies.

`--numeric` tests generated typed mutation -> Q/E -> JSON -> decoded audited
update for the three profiles, twice each. Long/ID/version/Decimal are exact
strings; Integer remains a number; native Float/Double remain approximate finite
numbers. Decimal scale, 64-bit extremes, signed Integer endpoints, nonfinite
output, wrong-type/unsafe-number input and precise field paths are checked.
This is a numeric boundary gate, not full temporal, dynamic-property or wire
envelope acceptance. These modes are part of the standard example script.

`--temporal` verifies generated calendar Date and wall Time through typed save,
Q/E, JSON and decoded update for all profiles twice. It preserves leap dates,
nine-digit native Time fractions, loaded Null and exact field diagnostics. Native
codec tests separately qualify Instant as offset-input/UTC-output milliseconds,
including rejection of excess precision. The generated model does not contain
Instant; its LocalDateTime and Business Clock behavior are deliberately unchanged
pending the separate local-date-time precision decision. This does not qualify
LocalDateTime wire semantics, generated Instant persistence or all providers.

This does not qualify all scalar wire boundaries, cross-runtime DDL parity or a
published artifact. Provenance is in `fixture-manifest.json` and
`json-profiles/provenance.json`; generated sources are never edited by the probes.
