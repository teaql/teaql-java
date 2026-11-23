# Trusted Context fields on Catalog entries

## Current business model and workspace

`model.xml` defines Catalog and Entry. Trusted Context supplies collection/edit
times, IP and actor fields; the example shows why saving an unchanged entry
must leave those business values untouched. `java-lib-core/` is the exact, read-only
current generated domain library. The application is in `java-app-console/showcase-tests/io/teaql/examples/contextfix/GeneratedContextFixTest.java`.
The historical `fixture/` remains byte-identical and retains all of its original
fingerprint checks; a separate guard verifies the current library before and
after business execution.

Run the focused source qualifier entry from the SDK repository root:

```bash
node examples/conformance/context-derived/verify-local.cjs
```

The original assertions run twice on retained stores: no-op saves produce no
updates, genuine edits consume the trusted Context, failed validation/provider
work preserves edits, and a later invalid child rolls back staged graph fields.
Current managed creation results are explicitly awaited or checked. No generated
implementation is hand-edited. Compact receipts and result stores are retained;
owned build output is removed. Full runtime inventories remain separate gates.

The current model adds only `_audit_mask_fields="guarded_token"` to satisfy the
current evaluator. Business fields/defaults and the frozen model are unchanged.
