# Optional Blob reference module

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

R3 implementation is in progress. Current components provide trusted immutable
profile overrides, resolved policy snapshots and retention/deadline rules.
`BlobPolicies` resolves field profiles over module defaults and built-in defaults,
intersects formats with model/deployment limits, and caps read credentials without
changing retained object lifetime. Historical retention promises take the maximum.
Private cache settings are explicit; automatic public caching is rejected.

The shared policy fixtures verify these rules. The PostgreSQL catalog and save
participant additionally have a real SQL component gate described below. Upload
orchestration now combines actual verification with private file storage and SQL;
the optional versioned S3 transport now has a real backend/SQL component gate.
Current-context target-authorized batch reads and passive typed assembly are now
available as described below. A known Key or a resolved profile grants no read or
binding authority. Full graph/cascade/idempotency acceptance, authorized response
projection and the remaining R3-R8 gates are still in progress.

Concrete transport remains optional. The low-level object storage SPI adds an
explicit version-conditional-delete capability; unsupported providers reject.
The existing `teaql-object-storage-s3` adapter uses the SDK's `DeleteObject.ifMatch` and preserves precondition
conflicts without an unconditional fallback, following
[Amazon's DeleteObject contract](https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html).
The loopback HTTP/SDK negative control passes; each actual S3-compatible backend
still requires its own precondition conformance check.

The base runtime now offers a lightweight `BlobMutationParticipant` save SPI.
`plan` runs after approval; a pure resource check runs before opening the business
transaction, and `prepare`/`apply` run inside it. Apply receives immutable planned
and persisted identity/revision mappings. Missing participants and incompatible
transactions fail before business writes. The optional provider must additionally
verify the exact SQL adapter and its active transaction; the runtime does not infer
physical atomicity from a matching database address.
Save also checks installed manifest requirements when the host skipped startup
readiness. A participant alone remains insufficient to satisfy full `blob.v1`.
Missing persisted IDs/revisions reject before binding apply/commit. A failure in
post-commit audit delivery never makes the participant repeat business/binding writes.

`BlobBindingIntents` extracts typed assignments from that approved plan and current
context metadata without reading entities or storage. Updates preserve omitted
fields; nullable explicit nulls clear; creates require real keys for required fields.
Delete/recovery intents carry the field surface for authoritative catalog resolution.
In-memory save-boundary tests cover phase ordering, rollback, retry and final IDs.

`PostgresBlobCatalog` uses explicit, transactional, fixed version/checksum migrations
for its own infrastructure tables. It checks columns and named primary/unique/foreign
key constraints. This is separate from model schema reconciliation; no general DDL
diff or global model registry is introduced. Constructors never open connections.
Upload begin/complete catalog transitions are idempotent, with immutable policy
snapshots; `VerifiedContent` remains a trusted internal input. Public callers use
`BlobUploadService` so actual immutable bytes are verified before completion.

`PostgresBlobMutationParticipant` borrows the exact SQL adapter and its opaque active
physical transaction identity. It locks target bindings and sorted old/new object
keys, intersects current host field permission with an additional Blob policy,
checks scope/formats/size/state/deadlines/sharing, and applies changes before commit.
Empty/deleted target rows retain monotonic binding versions; a retired Key supports
authoritative recovery and grants no read authority. Accepted retention maxima survive
unbind. A logical deleting claim rechecks live references under the same object lock;
the explicit maintenance service performs physical version-conditional deletion
after that claim commits.

The explicit opt-in `PostgresBlobCatalogIT` suite runs against a real PostgreSQL:
native business saves plus independent-connection SQL oracles, commit/rollback,
recovery/clearing, scope and both permission layers, expiry/retention, concurrent
single-binding winners and GC lock competition, cross-transaction Prepared rejection,
post-commit audit failure, and actual compiled/generic typed SQL hydration. It requires
`TEAQL_BLOB_TEST_PG_URL`, `TEAQL_BLOB_TEST_PG_USER`, `TEAQL_BLOB_TEST_PG_PASSWORD`, and
`TEAQL_BLOB_TEST_FIXTURES` pointing to the shared real-file fixture directory. It does
not establish public artifact, generated-workspace, caller-owned Java transaction
completion or full R3 acceptance at that earlier component gate. The direct-JDBC
owner now also supports the runtime journal integration described below.

`BlobUploadService` receives the catalog, transport and content verifier explicitly.
Every operation resolves its trusted target against current context metadata and
intersects current host field permission with an additional upload policy. Scope
and permission are rechecked after streaming/verification. No actor is cached.
Begin normalizes a basename, checks format/declared size and snapshots policy.
Identical scoped requests replay the original upload/Key/deadline and snapshot;
changed caller parameters conflict. Later profile edits cannot rewrite that
snapshot, while current deployment hard limits and disabled formats still apply.

Write is bounded, checks the session deadline during stream reads, preserves caller
stream ownership, and records immutable storage version/size/hash without making
the object bindable. Server-stream publication holds the upload's catalog row lock
through immutable storage write and fact recording. This fences a collection claim
against an in-flight upload, including a file published just before a deadline or
failed database acknowledgement. Host transports still own stalled-input timeouts;
deadline checks occur around source reads. Complete opens that exact version, verifies actual MIME,
container, size and digest, then rechecks current authorization/deadline before
promoting to `uploaded_unbound`. Request MIME/size hints never supply actual facts.
Failed verification leaves the upload initiated for bounded later collection.
Successful retries return the same Key without extending any deadline. Changed
storage generations conflict. Unsupported direct upload fails explicitly.

`BlobUploadAuthorization.AccessPolicy` receives available file facts: before
verification the content type can be absent and declared hints are untrusted;
after verification the confirmation check receives the actual type and size.
Permission policies must still authorize the target in both checks. Format and
size enforcement is performed by the service independently of those callbacks.

`BlobUploadIT` is a second explicit PostgreSQL gate using the optional
`teaql-blob-store-file` and `teaql-blob-content-tika` providers as test dependencies.
It runs all ten real formats and all seven shared upload cases through actual
begin/write/complete; unverified binding attempts roll back and verified native
SQL saves are checked through an independent connection. It also covers retries,
concurrent callers, current scope/both permission layers, revocation, deadlines,
I/O failure, empty content, changed generations and policy/deployment changes.
Use the same environment as `PostgresBlobCatalogIT`, selecting
`-Dtest=PostgresBlobCatalogIT,BlobUploadIT` explicitly. These upload gates cover
the underlying services. The passive module below adds typed assembly and reads;
authorized view projection and the remaining R3-R8 gates are unfinished.

`BlobMaintenanceService` is explicit host-scheduled work. Its constructor starts no
timer and performs no I/O; the host owns scheduler/worker lifecycle and supplies the
current context on each run. A required maintenance permission and scope provider
authorize each stage, including after storage I/O. Missing/revoked authorization
fails with `BLOB_MAINTENANCE_DENIED`. Maintenance rejects a caller-owned active SQL
transaction so no file can be deleted on the strength of an uncommitted claim.

Each batch processes at most its configured candidate count (default 100, maximum
1,000). A scoped
continuation cursor lets the host advance past persistent failures and restart a
new sweep when `next` is absent. Candidate discovery uses accepted upload/retention
snapshots; locking and reference/deadline rechecks remain authoritative. The service
commits `deleting`, performs the provider's atomic version-conditional delete, then
commits `deleted` and its timestamp. Store/catalog failures leave a recoverable
deleting row. A missing object is an idempotent success; a changed version remains
pinned and reports a conflict, with no unconditional fallback or automatic retarget.
An expired initiated upload with unrecorded bytes first pins the observed generation
for deletion without claiming its contents were verified. Its row lock fences all
public synchronous server-stream writers before the generation is observed or
absence accepted. Remote providers additionally implement `sealForDeletion` after
the committed claim, before absent content may acknowledge deletion. The S3
provider leaves a persistent ordinary-object fence against delayed remote PUTs.

`BlobMaintenanceIT` combines the same PostgreSQL/file/verifier stack with 13 methods:
separate lifecycle deadlines, live/shared references and historical retention floors,
failure before/after delete, failed catalog acknowledgement, scope/permission/cursor
checks, version conflicts, current revocation, in-flight publication and binding
competition, concurrent confirmation and its independent unbound deadline,
concurrent collectors, and an explicitly started/stopped host timer.
Owned separate JVMs call `Runtime.halt` after a committed claim and after actual
file deletion; a new service instance recovers the durable directory state. Probe
credentials stay in the inherited environment, and all owned children are reaped.
Select `-Dtest=PostgresBlobCatalogIT,BlobUploadIT,BlobMaintenanceIT` explicitly.

This collector supports server-stream uploads. Direct upload needs a provider's
proven request quiescence/fencing before collection and currently rejects explicitly.
File upload and Tika providers use leased private request directories. Their
`stagingMaintenance(permission)` is explicit shared service-account management,
with a separate current-context permission; catalog maintenance authority alone
does not grant it. `BlobScratchIT` kills real upload/verification processes while
leases are held, confirms active stages remain untouched, reaps abandoned scratch,
and verifies retry through actual confirmation, SQL binding and physical collection.
The optional [S3 transport](../teaql-blob-store-s3/README.md) and seven-method
`BlobS3IT` gate combine actual versioned storage, ten-format verification, native
SQL binding, rollback, committed audit failure, shared retention, abandoned uploads,
authorization revocation and physical collection recovery. Source tests require
the S3 test variables listed in that provider's README in addition to PostgreSQL.
The public module also has a native S3 URL/Range gate. Full four-combination
consumer and external-network validation remain pending; logical `deleted` does
not claim destruction of deployment backups or all storage versions.

`BlobOuterTransactionIT` adds caller-owned PostgreSQL transaction coverage. Runtime
uses the physical owner completion API and a reversible submitted-intent journal:
multiple saves consume provisional authoritative versions, later edits remain
unsubmitted, rollback restores intent/version, and only real commit delivers frozen
audits. Actual Blob/dynamic/ordinary fields share that SQL transaction. Commit-time
notification failure preserves committed state; UNKNOWN blocks blind same-object
retry pending durable reconciliation. See
[runtime completion semantics](../teaql-runtime/TRANSACTION_COMPLETION.md).
Blob executors without physical completion support fail before writes. Shared
pending entities can now move between graph roots under the same physical owner
while unrelated working intent stays isolated. All graph state settles
before audit delivery. Full graph/cascade/cross-policy/idempotency acceptance
remains pending.


## Passive module assembly and reads

`BlobReferenceModule` assembles the existing PostgreSQL catalog, upload verifier,
binding participant, target-authorized read service and bounded maintenance service.
It depends on the runtime API for installation; core/runtime still have no concrete
Blob/S3/Tika/secure-document implementation dependency. Store, verifier, current
scope/host permissions/additional policies and maintenance permission are supplied
by the trusted host. No user identity is retained.

```java
BlobReferenceModule module = BlobReferenceModule.builder()
    .catalog(catalog).store(store).verifier(verifier).policies(policies)
    .uploadAuthorization(uploadAuthorization)
    .bindingAuthorization(bindingAuthorization)
    .readAuthorization(readAuthorization)
    .maintenancePermission(maintenancePermission)
    .build();
TeaQLRuntime runtime = module.installInto(TeaQLRuntime.builder()
    .metadata(metadata).dataService("default", businessExecutor)).build();
runtime.install(businessModule);
// The host provisions/migrates infrastructure separately, before this probe.
runtime.validateReadiness();
BlobRuntime blobs = context.capability(BlobRuntime.class);
```

Construction/installation does no catalog/storage I/O, migration, upload, signing,
or worker startup. Business manifests and Blob assembly can be installed in either
order. Duplicate or participant/module replacement is rejected by the builder.
`validateCapabilities()` checks required slots and pure model/policy/route/provider
configuration; saves can repeat it within an existing physical transaction.
`validateReadiness()` additionally detects Blob fields even without a generated
requirement, checks the fixed catalog schema and invokes the store's read-only
probe. It rejects an active catalog transaction. Missing infrastructure is never
created or repaired by either method. Maintenance remains explicitly host-scheduled.

Every effective field must have compatible BlobRef metadata, a known profile,
formats attested by the verifier and a completion-capable business route borrowing
the same physical adapter as the catalog. Range, signed reads, conditional deletion
and the adapter's declared single-write bound are checked. Unsupported providers
fail closed. Direct upload remains unsupported by this module until its remote
publisher fencing/collection contract is implemented and verified.

`BlobRuntime.Reads.readBatch` accepts at most 256 currently authorized, visible,
loaded field targets with their expected BlobRef. It checks current scope, host
field visibility, separate read policy and an authoritative target-to-Key binding;
knowing a Key alone grants nothing. It batches catalog lookups and deduplicates
only equivalent transport requests within the call. Empty input performs no
catalog/signing I/O. Reads reject an unfinished catalog transaction. Signing is
followed by binding and current permission/scope rechecks before any URL is returned.

Results contain the provider's complete URL and actual expiry, original name,
actual MIME/size and private cache policy, with no storage locator. Current policy,
request, store and optional document deadlines cap lifetimes. An overlong/expired
or malformed signer result is rejected, never relabeled with a false expiry.
Context-checked reads require a provider that explicitly supports them; current
file/S3 bearer providers reject that option. Entity fields and ledgers are untouched.
No serializer/getter/SQL codec signs URLs. The R4 authorized response graph and
ViewPreparationService still need to consume this service.

The unreleased upload and maintenance DTOs now live in `BlobRuntime.Uploads` and
`BlobRuntime.Maintenance` in core. Ordinary qualified source references through the
implementing service remain inherited; imports must use the canonical core name.
Previously compiled development consumers must be rebuilt against matching sources.
This does not change persisted BlobRef, catalog migration v1 or tqr1/tqd1 protocols.


## Native graph acceptance

`BlobGraphIT` uses a three-level/cyclic native PostgreSQL graph with two Blob fields.
It covers net single-binding field swaps/transfers, explicit approved cascade
DELETE/RECOVER with unloaded field values, partial title-only updates, per-field
format/current permission rejection, whole-batch rollback and retry, stale-child
optimistic conflicts, concurrent swaps, REFER aliases and cross-route rejection.
Deletion intent still comes from the application: marking a root does not silently
mark every related entity. Every planned deletion releases its authoritative Blob
bindings, including fields absent from a partial object; recovery uses retired
binding history. Unmarked related rows/bindings stay live.

The graph must use one canonical mutable instance for each logical entity key.
Distinct materialized aliases with working/submitted intent now reject before
Checker/Fix and ledger merge; the check repeats after ID allocation. This prevents
one alias's local placeholders from overwriting another alias's requested fields.
Read-only copies and REFER relation aliases retain existing behavior, including
when a different entity in the same cohort has pending writes. Reconcile
edits onto a canonical instance before retrying an ambiguous graph.

The actual file-provider retention gate binds one Key through short and long
field profiles, removes those bindings in stages, and verifies physical collection
only at the greatest historical minimum/unreferenced deadline. Policy relaxation
or removal of the long field cannot shorten an accepted promise. These graph gates
still do not implement durable command idempotency or process-crash reconciliation.

## Optional durable commands

`blob.command.v1` adds an explicit command coordinator through the core
`BlobCommandService` API. It is optional alongside ordinary `blob.v1`; the host
configures a `PostgresBlobCommandJournal` and `BlobCommandAuthorization` with
separate current execute/replay policies and a trusted scope/principal provider:

```java
// Explicit infrastructure provisioning, outside business transactions:
PostgresBlobCommandJournal journal = new PostgresBlobCommandJournal(sql);
journal.migrate();
// Add this to the normal BlobReferenceModule builder before build/install:
// .commands(journal, commandAuthorization)
BlobCommandService commands = context.capability(BlobCommandService.class);
BlobCommandService.Request request = new BlobCommandService.Request(
    "Asset.bind/v1", invocationKey, canonicalValidatedRequestBytes);
BlobCommandService.Execution result = commands.execute(context, request, () -> {
    runtime.saveGraph(context, asset);
    return List.of(new BlobCommandService.EntityRevision(
        asset.typeName(), asset.getId(), asset.getVersion()));
});
```

Canonical bytes come from trusted versioned command code and include every
semantic input, including purpose/intent where applicable. The service computes
the fingerprint itself. Do not accept a client hash as proof of the command
payload. The stable identity is scope + operation + opaque invocation key;
principal and fingerprint are bound separately. Changed canonical input conflicts.
An identical committed replay checks current authority and returns the original
metadata receipt without running the action. The action's native graph writes,
Blob bindings and receipt borrow the same physical SQL owner and commit atomically.
Receipt revisions come from the authoritative provisional results in that owner.

The coordinator owns the outer transaction. Already active caller transactions,
nested commands and graph saves through another physical resource reject. The
synchronous action must not manage commit, dispatch independent threads, write
unrelated SQL or perform irreversible external I/O. Prepare uploads beforehand.
Installation is passive; readiness checks but never creates the separate fixed
command schema. Catalog migration v1 stays unchanged. Keep receipts durable with
the business database: no automatic expiry/deletion can make an old identity run
again. This implementation provides no online receipt removal API.

Only a known commit returns normally. `TransactionCompletionException` preserves
UNKNOWN or COMMITTED when acknowledgement/notification fails. `resolve` uses an
independent committed read and current replay permission; an empty result does
not establish rollback while an earlier owner is in flight. Executing the same
identity again safely serializes against that owner. A durable receipt can resolve
an acknowledgement loss without clearing the original graph's UNKNOWN protection.
Reload currently authorized state into fresh entities before further mutation.
Receipt metadata is historical proof, not a current view or authority to restore
an old Blob binding. No payload, business values or URLs are persisted in receipts.

`BlobCommandIT` exercises these boundaries against real PostgreSQL, including
native business/binding/receipt oracles, current authority, concurrent replay,
rollback, corrupt/incomplete markers and commit fault injection. Its component
evidence does not establish Rust, generated-consumer, registry or R3-R8 acceptance.
