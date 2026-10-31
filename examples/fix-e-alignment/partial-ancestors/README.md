# Saving a complete descendant through sparse ancestors

Run the regular source-paired example from the runtime repository:

```bash
node examples/fix-e-alignment/verify-local.cjs
```

`PartialAncestorExampleTest` uses the unchanged three-entity model and generated
Q/E APIs. The query explicitly loads Tenant → PurchaseOrder → OrderItem, with
identity-only ancestors and a complete leaf. Only the leaf is changed; saving
the root must persist that leaf without loading or updating either ancestor.

The same test checks that JSON omits unselected fields and that E raises
NotLoaded without provider access. Modifying an identity-only leaf must reject
twice before provider entry, retain the pending mutation, and leave stored values
and versions unchanged. Business time comes from an injected BusinessClock.

Both this test and the original Fix/E suite run twice on their respective
unchanged SQLite databases. The verifier checks generated-fixture hashes, uses a
disposable source-paired Maven reactor, and retains compact logs and databases
while removing owned build output. It does not install or publish runtime jars.

The additional JSON dependencies belong to this test consumer, not the domain
library. No generated implementation or production runtime code is modified.
