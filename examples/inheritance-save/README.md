# Generic and inherited audited saves

This six-object SQLite example saves Academy through a generic audited helper,
then reads and updates the same logical object through generated Academy and
School Q/E APIs. It never inserts a parent row manually. The first lifecycle
checks unchanged save, incomplete-target rejection, inherited/local edits,
independent stale copies, parent-view edits, deletion, retained tombstones and
recovery through ordinary audited save. Recovery with field corrections advances
the root version once and retains corrected fields and intent in its audit.
An unrelated SchoolType at the same numeric ID must retain its own version.

The example also supports the local PostgreSQL verification provider. The
runtime owns Context setup and schema/bootstrap; the generated library supplies
typed queries, expressions and mutation methods. Runtime dependencies compile
from this repository in an offline Maven reactor, not a published artifact.

```bash
JAVA_HOME=/path/to/jdk21 MAVEN=/path/to/mvn \
  node examples/inheritance-save/verify.cjs --output /absolute/new/evidence
```

For an existing, explicitly selected local PostgreSQL fixture:

```bash
JAVA_HOME=/path/to/jdk21 MAVEN=/path/to/mvn \
  node examples/inheritance-save/verify.cjs --output /absolute/new/pg-evidence \
  --provider postgres --postgres-container teaql-postgres --postgres-port 5433
```

Use a runtime checkout without Maven targets; the verifier refuses to delete or
reuse unknown build outputs. It runs two rounds on the same owned database,
verifies local class origins and generated hashes, and retains compact logs,
JUnit reports and the database. Its compiler targets and scratch reactor are
removed after child processes exit. Existing services and databases are not
restarted or emptied.

The fixture's model requests the candidate Java version. The verifier records an
explicit Maven property override to the current local source coordinate. This
is source validation, not certification of a published candidate version.
`generation-origin.json` identifies the fresh generation receipt and targeted
Assist; `generated-files.json` binds the unchanged generated library.

Queries retain their native typed view. `runtimeType()` supplies readonly
next-subtype information: School views of TrainingAcademy report Academy,
whereas Academy views report TrainingAcademy. It never selects a different
checker or authorizes a cast. The example covers six base/middle/leaf/sibling
combinations, minimal selection, streaming and parent update/no-op readback.
The verifier checks root-owned versions and mapped child storage independently
with read-only SQL. It is registered in `scripts/verify-examples.sh`.

Application assembly assigns the deliberately quoted `Legacy Campus Code`
storage column before schema reconciliation. This keeps the custom mapping
covered while allowing the library to remain exact, reproducible generator output.

The tested root-version sequence is `1 → 2 → 3 → -4 → 5 → -6 → 7 → 8`.
Marker-only recovery, recovery with edits, stale/live/partial recovery rejection,
and recovery audit lineage are checked on both providers. Native tests also
cover prepared recovery batches, policy intent and the default family ID space.
The example's deliberate numeric-ID collisions use a controlled fixture
allocator, not a claim about default allocation order.

Test-only, reject-only DDL triggers prove mixed-object atomicity: the trigger
first checks that this round's Platform edit is visible in the transaction,
then rejects a named School value. Read-only Q/E and SQL verify neither edit
commits and no committed audit escapes. Correcting the child and retrying the
same graph succeeds with exactly two committed events. Application mutations
still use generated APIs exclusively; fixture SQL never seeds, repairs or
compensates business rows.
