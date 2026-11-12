# Natural Checker messages from a generated SQLite graph

Tracking: [Java #278](https://github.com/teaql/teaql-java/issues/278),
[conformance #244](https://github.com/teaql/teaql-conformance/issues/244).

This two-entity example returns complete human-readable messages while keeping
canonical error identity separate. For the second entry of a loaded catalog:

```text
The name of the 2nd entry is required.
The name of the 2nd entry must not exceed 100 UTF-16 code units; its current length is 102.
```

The corresponding pointer remains `/entryList/1/name`, not index 2. Switching
the trusted Context to `zh-CN` produces `请填写第2项明细的名称。` without frontend
sentence assembly. The model supplies no specific classifier, so it uses `项`.
A private name's length error reports its original 102 UTF-16 code units,
not its 101 Unicode scalars or its masked value's length. Neither the message nor the
structured response exposes that private value.

## Run locally

```bash
TEAQL_NATURAL_MESSAGES_UNIT_TESTS=1 \
TEAQL_NATURAL_MESSAGES_EVIDENCE_DIR=/absolute/new-evidence-directory \
node examples/conformance/natural-messages/verify-local.cjs
```

Run from the runtime repository with JDK 21, Maven and Node available. `MAVEN`
can select a local Maven executable. This is an offline, source-paired gate;
framework dependencies must already be available. It installs or publishes no
TeaQL artifacts and refuses an existing explicit evidence destination.

The verifier validates every generated fixture hash, copies current runtime
sources to an owned scratch reactor, binds only the disposable generated POM to
those sources, and runs the example twice against the same SQLite file. It
removes its build/source copies after owned processes exit. It retains compact
receipts, logs and the database in the evidence directory.

## What the example proves

- `ensureSchema` provisions the root; sample entries use generated mutation
  methods and audited Save, never application-written INSERT statements.
- Bounded generated Q loads the parent and latest two entries. Generated E
  reads complete modeled fields; it is not replaced by reflection or raw maps.
- The unmodified generated Checker reports required and private-length failures
  at the second child; no application-owned replacement Checker is installed.
- The parent remains unchanged, and the first child is also modified. The
  renderer nevertheless resolves the second child's full path from the parent
  graph, not from the first writable entity.
- English and Chinese validations reject before any provider call or committed
  audit. Failed automatic timestamp changes roll back, while caller edits remain
  available for correction.
- Correcting/reverting invalid edits causes no Context-only write; a real
  corrected change subsequently saves and reads back successfully.
- Four backend responses each carry one structured violation, original rule IDs,
  zero-based pointers, projected inputs and complete human messages.
- A customer renderer receives only projected input and its original length,
  runs once for that violation and cannot restore the rejected private value.
- An independent read-only SQLite oracle confirms only the corrected entries
  reach version 2; siblings remain unchanged across both rounds.

`GeneratedRuntimeModule.module()` installs both the generated Checker and
passive model metadata. Application code does not construct presentation
descriptors or call `withCheckerPresentation(...)`. Only the disposable generated
POM is pinned to the repository-local runtime; generated implementation and
metadata hashes remain unchanged.

## Fixture origin

`automatic-fixture/` is the current generated input. Its 24-file manifest,
raw model, evaluation output and generator origin are retained alongside
`generation.json`. `JavaNaturalMessagesGenerationTest` evaluates the model and
generates the library from the local generator source; the paired conformance
receipt binds the emitted file hashes to that source snapshot.

`fixture/` preserves the earlier service-generated library unchanged. Its
original raw model remains at `fixture/model.xml`; its old generation receipt
and top-level `evaluation.md` are historical evidence, not the current input.
The old manually composed acceptance must not be presented as automatic
generator-composition acceptance.

The old Context Fix fixture remains unchanged. Its model is not reused for new
generation because the current evaluator now requires an additional credential
mask declaration there. This new example does not bypass evaluation or weaken
the frozen old fixture's provenance guards.
