# Purchase-order Fix and loaded-state showcase

## Model and workspace

This showcase uses `model.xml`: Tenant owns PurchaseOrder and OrderItem.
The generated domain library is read-only in `java-lib-core/`; application code is
in `java-app-console/showcase-tests/io/teaql/examples/fixe`. The workspace hosts the original scenario checks and
trusted Context/runtime setup.

From the SDK repository root, run:

```bash
node examples/fix-e-alignment/verify-local.cjs
```

The verifier checks the model and exact generated-library hashes, builds the
native workspace with the local SDK, then runs the original business cases
twice on retained stores. Assertions cover real generated Q/E and audited
saves, precise loaded state, rejected writes and transaction outcomes. It
keeps compact receipts and result data and removes owned build output.
`showcase.json` binds this model, both generation scopes and the focused result;
the final full SDK example inventory remains a separate gate.
