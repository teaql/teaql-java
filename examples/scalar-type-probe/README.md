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

`--rejected` uses one evaluated model twice to test Core lifecycle rejection,
not JSON profile equivalence. A generated FK-only reference rejects typed/generic
updates, version edits and an explicit legacy-cache fault injection without
changing payload, availability or ledger state. A failed generic cast stays
NotLoaded, an incomplete modified object is rejected before provider access, and
the complete owner still saves without writing the reference.

`--explicit-null` runs the same small model twice: an explicit null edit on a
NotLoaded field becomes loaded Null and retains pending intent. Changing it again
does not turn the unknown original value into a known null. Incomplete modified
objects still fail Checker before provider access. Equal values on already loaded
fields are true no-ops (no lifecycle change, ledger allocation, SQL, audit or
version bump). ID-only references cannot use a null-version edit to claim that
the version was loaded. A complete object's value-to-null update persists and
reads back through Q/E and an independent SQL oracle. These checks run from the
standard example entry point; they are not full graph or artifact acceptance.

`--loaded-revert` verifies one model twice for known-value and known-null
restoration. An intermediate edit followed by the original loaded value removes
that field's pending ledger entry and does not write, audit or bump the version.
Restoring one field retains a different field's actual change; the policy and
SQL readback retain that real edit plus the model's normal managed `update_time`
Fix. A no-op save does not trigger that Fix. Core tests additionally preserve dynamic
intent, deletion, type-qualified sibling keys, loaded snapshots, original versions
and lineage. A missing original value remains unknown and cannot be cancelled
merely because its backing storage was null. Cancelling a field against its
persistent baseline is distinct from popping a change-set scope; it cannot expose
an older intermediate value of that field or erase unrelated staged values.

`--version-lifecycle` runs generated Q/E/JSON and audited Save twice on one small
SQLite database. Ordinary generated creates precede deliberate SQL boundary
injection. The last update reaches `Long.MAX_VALUE` exactly; further updates fail
at `/version` before the generated Checker, policy, provider or audit. Unchanged
exhausted rows remain no-ops. Deleting `MAX` produces exactly `MIN`, recovery of
that exhausted tombstone rejects, and recovery of `MIN+2` reaches `MAX` exactly.
Both loaded-null and non-null notes, independent pending objects and the untouched
parent remain intact. The complete example inventory includes this mode; passing
this probe alone is not private-artifact or full-runtime qualification.

`node examples/scalar-type-probe/verify-local.cjs --restoration-views` tests three
scenarios twice, without resetting their separate SQLite stores: sparse fixed
fields and loaded relations plus readonly result types/presence; a caller-owned
JDBC transaction with no-op, rollback, retry and commit; and persistent extension
Value/Null/NotLoaded, shared geometry, storage provenance and a real `#` edit
surviving native-field restoration. The no-op enters no provider or policy and
emits no mutation audit. A genuine saved object gets authoritative readback;
audit waits for the physical transaction owner to commit. Rollback retains the
optimistic baseline and pending intent for retry. Standard JDBC nested failures
are rollback-only even if application code catches the inner exception. A sink
failure after commit is a delivery failure, not proof the database rolled back.

`--outer-replay` adds seven scenarios, twice on seven separate unreset stores:
two saves changing different fields, repeated changes to the same field, an
unsaved later field, create followed by update, dynamic followed by native,
later unsaved dynamic input, and retained invalid raw input. A physical rollback
must preserve the final requested values against the initial optimistic version.
Replaying a tracked updater retains its first-change provenance; raw keys remain
raw for normal admission, including explicit NULL and invalid keys. No rolled-back
attempt delivers mutation audit. Retrying final intent writes once. This mode
also records whether the original generated factory assigned an ID before the
first save; it does not treat deferred allocation as early-ID conformance.

These paths are part of the complete example script. They retain logs, hashes and
three databases, not build outputs. The current probe qualifies the standard
JDBC adapter, not arbitrary custom transaction implementations, all lifecycle
combinations (such as create/delete/recover chains) or installed packages.

This does not qualify all scalar wire boundaries, cross-runtime DDL parity or a
published artifact. Provenance is in `fixture-manifest.json` and
`json-profiles/provenance.json`; generated sources are never edited by the probes.

`node examples/scalar-type-probe/verify-local.cjs --jdbc-exact` runs sixteen
legacy-corruption cases twice on one unreset SQLite store, using generated Q
single-result and stream queries. It rejects fractional or overflowing integers,
overflowing Float, infinite Double and invalid Decimal text without disclosing
private values. A projection that excludes the corrupt column still returns its
selected fields; E reports the unselected numeric field as NotLoaded. Direct SQL
is confined to explicit fault injection, its repair and independent readback.
Audited generated creates and a real update, exact native integer endpoints,
connection recovery and unchanged query-time audit/policy counts are also checked.
The complete example entry point includes this mode.
