# Generated Java shared load state example

This small SQLite acceptance example uses a newly generated School library and
the Java runtime source from the same Maven reactor. It does not install,
publish or substitute TeaQL jars from a public repository.

## Run

Requires Java 21 or newer, Maven, Bash, `rg`, `sha256sum`, and a local generator
checkout. Third-party dependencies may download on the first run.

```bash
export TEAQL_CODEGEN_DIR=/path/to/teaql-code-gen
bash examples/shared-load-state/verify.sh --generate
```

The producer reads the current runtime version from its parent POM, generates
the library and Query/Create/Expression/Delete Assist into `target/generated`,
and never edits generated Java source. The `shared-load-state-example` Maven
profile builds the generated library and application-owned tests with the
runtime dependencies in one reactor. Omit `--generate` to replay that artifact.

Use `bash examples/shared-load-state/verify.sh --generate --wide` for the overflow
gate. The producer adds 130 nullable School probes, producing 141 fixed slots,
and retains `model-under-test.xml` plus `fixture-mode.txt`. Both rounds assert
generated slots 0/31/32/63/64/65/129, actual NULL payloads, sparse NotLoaded and
overflow copy-on-write. A requested wide run rejects a narrow artifact; an
existing wide artifact automatically keeps the wide assertions enabled.

Add `--inheritance` during generation to append one Academy subtype with a
campus code. The base model has Platform, SchoolType, School and a small
SchoolCapacitySummary reporting target. The producer
retains a separate inheritance marker and the verifier selects an additional
Maven test source only for that generated artifact. A requested inherited run
rejects a flat artifact; replay preserves the inherited gate automatically.
The inherited gate checks shared parent indexes and separate type identity,
Q/E, actual snapshot sharing and governed create/update/delete with a fully
loaded object. Its qualification is separate from the plain School gate.

Each invocation retains a unique temporary directory containing its SQLite file,
round logs and generated-source hashes. Both rounds use that same database
without cleanup. The test checks the actual class-loading locations for core,
runtime, JDBC, SQLite and the generated School: every one must resolve to local
reactor `target/classes`, not an installed jar.

## Acceptance

`GeneratedMaterializationAcceptance` calculates a capacity total over two
bounded School Q results using E. The readonly `_total_capacity` result gets
no fixed slot and must not persist when the source is saved. Persistence is
explicit: create a modeled SchoolCapacitySummary with its generated Mutation
API, attach an audit reason, save, and query it back through Q/E. The expected
total is 37 and contributor count is 2. The source sibling retains its original
shared snapshot and clean mutation state. This is application-side calculation
and modeled storage, not a database aggregate-query performance claim.

`GeneratedNestedGraphAssertions` also verifies a bounded Platform-to-Schools
view and School-to-Platform-to-Schools graph through generated Q/E and JSON.
Loaded empty lists serialize as arrays; unselected lists stay absent. A target
filter cannot erase the source's known FK or invent loaded target detail.
These cases are required in both retained rounds, not optional smoke output.

The mapper uses `new TeaQLModule(context)` for indexed, strongly typed JSON
reads through installed suppliers rather than bean reflection. Both rounds
roundtrip full and minimal School projections, nested forward/reverse graphs,
and loaded empty versus omitted reverse lists. Missing target details stay
NotLoaded, actual NULL stays loaded, and compatible decoded rows share state
without creating mutation intent. This does not certify arbitrary polymorphic
or persistent `#` JSON input: those require their own contract. Decoded data is
not a substitute for loading authoritative state before mutation.

- Repeated schema bootstrap preserves Platform 1 and SchoolType 1001/1002.
- Generated indexes and revision match the installed type layout.
- Compatible full and sparse lists share exact immutable snapshot references
  within each shape, without sharing values or merging their load boundaries.
- Filling an omitted field detaches that row's state; changing an existing value
  keeps the snapshot reference. Zero/false remain loaded values.
- Generated Q/E traverse forward relations, and the full-object Checker rejects
  sparse saves. Audited create, update and soft delete preserve sibling values
  and optimistic versions.
- A JDBC Dynamic Field provider uses the same SQL executor as native mutation.
  Governed save distinguishes stored Value, explicit NULL and Delete.
- A held dynamic view rejects a changed storage profile before DML, keeps its
  pending intent, and retries after restoring the original profile. Rebuilding
  the provider over the same executor is valid; it does not change storage identity.
- Readonly `_name` coexists with native `name`; a missing dynamic property reads
  null, and ordinary save does not persist derived properties.

Partial child projections are valid for E reads, but the whole-graph Checker
does not accept them as complete writable objects. The example reloads the full
School before deletion rather than disabling that validation.

The persistent ID allocator uses its framework database bridge. Application
business data is created through generated mutation APIs, not handwritten SQL.
Dynamic-field definitions are registered explicitly as provider metadata; each
round has its own test definition.

This is a small generated-consumer gate, not an allocation or throughput benchmark.
The optional wide fixture covers compiled overflow entities. Nested extension
graphs, broader provider provenance, serialization and immutable internal-artifact
consumers remain separate gates.

For a new object with no queried extension view, obtain definitions with
`context.dynamicFields().metadata(ownerType)` and install an empty
`DynamicFieldValues` using that snapshot before updating extensions. This reads
definitions, not owner values. Unbound hand-built metadata cannot be silently
assigned to the current storage during save.

## State allocation probe

```bash
bash examples/shared-load-state/verify-allocations.sh
```

The probe lives in the unnamed example module, so the core module gains no JVM
management dependency. It uses `ThreadMXBean` allocated-byte counters after
warmup, with 4/64/130 fixed fields and 1/100/10,000 operations. Fixture setup,
payloads, JDBC and output formatting are outside the counted region. Ordinary
sets and Map-backed code views are both checked. A value-free private raw-code
array is memoized in the shared snapshot; it neither assigns dynamic-field
indexes nor changes the snapshot's availability, equality or hash. Real selection
changes still detach, and caller-owned sets cannot mutate the saved shape.

The script retains separate state and four-field hydration CSV/logs. After
generating a wide fixture, add `--wide` to measure actual generated School and
optional Academy hydration at 3/64/full selected fields and 1/100/10,000 rows.
This checks shared overflow snapshots, COW isolation and lazy ledgers, requires
unchanged generated-source hashes, and rejects a non-wide artifact. Prepared
inputs, JDBC and logging remain outside the measured hydration region. These
are not whole-query latency, retained heap, CPU saturation or ORM comparisons.
