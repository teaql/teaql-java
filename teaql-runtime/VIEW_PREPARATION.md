# Explicit authorized view preparation

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).
The core view API and runtime coordinator are framework independent. The host
invokes them after business work/required transactions complete, before rendering.

```java
TeaQLRuntime runtime = TeaQLRuntime.builder()
    .metadata(metadata).viewAuthorization(currentViewPolicy).build();
ViewOptions presentation = ViewOptions.readOnly();
PreparedView prepared = runtime.prepareView(context, result, presentation);
renderer.render(prepared.value()); // Maps/lists/scalars; pure rendering
```

The required host policy supplies current realm/subject/boundary, entity permission
and field visibility/masking. It runs before reading a field. Unloaded fields are
omitted; explicit null stays null; masked/hidden Blob fields are never read/signed.
Unknown POJOs require explicit host normalization to a supported value/envelope.
No global metadata, bean reflection, getter signing or automatic save is used.
Identity-only entity cycles retain currently permitted loaded id/version fields.
Container cycles and over-budget graphs reject. BaseEntity load state, fields and
mutation ledgers are unchanged; preparation does not create a ledger.

`BlobReferenceModule.installInto` additionally installs its BlobResponseContributor
through the core SPI. It consumes the frozen visible graph, performs current
target-bound reads, and returns provider URLs with their true expiry. Neither the
entity nor SQL field changes from BlobRef to URL. A graph can exceed one 256-request
signing batch: two full-graph binding snapshots surround all batches. Equivalent
transport requests deduplicate; a later batch invalidating an earlier target rejects
the whole presentation. No partial URL collection is delivered.

One `ViewOptions` instance identifies one presentation. Nested renderers pass the
server-created PreparedView and same options to reuse it without reissuing tokens
or signing again. Reuse still checks current identity/policy and expiry. New
presentations use fresh result data and options. Foreign/stale PreparedViews and
client JSON `prepared` flags cannot establish the private service provenance.
The private Blob validator retains immutable scope/catalog/policy facts rather
than an actor or entity. Reuse checks current bindings and separate Blob read
permission without signing. Revoking only Blob policy therefore also denies reuse.

Use `ViewOptions.afterCommit()` when presenting a known committed command result.
`ViewPreparationException.businessCommitted()` then stays true even if storage,
document preparation or authorization fails. Re-read current authorized state and
retry presentation; the coordinator never replays a mutation. Active known physical
owners, command scopes and pending/UNKNOWN entity submission journals reject.

Default cache is `private, no-store`. Explicit positive cache lifetime is capped
by every Blob cache constraint, real credential expiry and document deadline.
The final value and optional document credentials are immutable and bounded.
When emitting cache headers, use `prepared.cachePolicyAt(clock.instant())` to cap
the maximum cache policy by absolute expiry at actual delivery time.
Render only `value()` plus the endpoint's explicit document-credential envelope;
DocumentContext and the internal authorization graph are server-side context.

Governed mode additionally requires a ViewDocumentContributor and trusted reference
principal. The contributor prepares from the frozen surface, then validates current
document/purpose authority after projection and on reuse without reissuing tokens.
Its default validator fails closed. The core SPI does not implement Java document
snapshot/open/accept; that implementation is R5. Existing tqr1/tqd1 is unchanged.
Rust document delegation, selection/snapshot extensions and four-combination
acceptance remain separate gates.

The plain Java mains in [base example](../examples/view-preparation/README.md) and
[Blob example](../examples/blob-view-preparation/README.md) exercise these public
APIs independently of JUnit/Web frameworks. Source-built JAR replay is distinct
from generated consumers and later registry/release acceptance.
