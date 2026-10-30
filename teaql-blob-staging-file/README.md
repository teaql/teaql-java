# Private leased Blob staging

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

This optional Java 17/POSIX helper depends only on core in production. Constructors
perform no I/O and start no jobs. `open()` provisions a dedicated private root on
first use and creates a private job directory with a cross-process OS lease. The
lease covers the input file and all request-specific parser/cache files. Ordinary
close removes the entire job; process termination releases its native lease so an
explicit host collector can recover it. Root initialization/claims/final removal
also use a JVM and OS coordination lock. Root markers are forced before use.

`maintenance(permission)` creates a passive host management service. Its required
permission receives the current UserContext on every collection stage. This is
authority to collect **shared service-account scratch**, separate from tenant-scoped
Blob catalog collection. A tenant administrator does not acquire it implicitly.
Collection never traverses published object storage or changes catalog/bindings.

`collect(ctx, minimumAge, limit, cursor)` requires a positive age and 1-1,000 candidate
limit. Age only selects candidates; a held lease always preserves an active job,
including one whose timestamp is old. The returned cursor is bound to the configured
area. The collector enumerates its dedicated root with memory bounded by the
candidate limit, then tries each job lease without waiting for active workers.
Recursive cleanup is bounded to 10,000 entries and depth 16 per job, does not follow
symlinks, and leaves failed jobs recoverable. Fresh or concurrently vanished jobs
are skipped. Advance the cursor until absent, then begin another sweep. Partial
cleanup changes directory timestamps, so age may defer its next retry.

Provisioning rejects unrelated unmarked roots; unknown names are never cleaned.
An old unleased staging layout requires an explicit offline migration/drain with
old writers stopped. The collector cannot infer safety for another implementation's
files from timestamps or names. The protocol is intended for a dedicated local
filesystem with coherent POSIX locks; other filesystems need their own conformance.

The host owns scheduling, context creation and worker shutdown. File Blob storage
and Tika expose their configured collectors through `stagingMaintenance(permission)`;
no path or permission comes from a file upload request. Base/core/runtime have no
production dependency on this concrete implementation.

Tests verify private permissions, ordinary and auxiliary cleanup, age/cursors,
current authority/revocation, symlink/corrupt-root controls, cleanup budgets,
concurrent collectors and a live separate JVM followed by forced termination.
The Blob reference module's opt-in `BlobScratchIT` also kills actual public
upload/verification processes, checks SQL/native fencing and verifies retry through
confirmation, native business binding and retained-object collection.
