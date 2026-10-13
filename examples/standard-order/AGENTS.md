# Java Standard Order Example Rules

Implement only application-owned code. The files under `generated/lib` are
read-only. Discover generated APIs using progressive model-aware Assist with
`models/model.xml`; never read generated source to discover methods.

```bash
cargo teaql --input models/model.xml java-assist-query/customer_order
cargo teaql --input models/model.xml java-assist-query/customer_order.order_item_list
```

Use bounded Q queries through one trusted UserContext, with non-empty comment
and purpose. Configure offset/limit on the request and execute through
`executeForList(context)`. Do not introduce a separate `executeForPage` entry.
Requested count, facet and relationship metadata belong to the returned
SmartList. Use generated E for loaded-value traversal and preserve NotLoaded
as an error.

Use generated audited mutation APIs for seed data; do not handwrite INSERTs.
Runtime, providers and policy are assembled once. Standard telemetry is selected
by trusted environment settings; business methods contain no SDK initialization
or manually created spans. Ordinary SQL output and telemetry are independent.

Verify against repository-local Maven reactor modules. Run `bash verify.sh` and
retain startup messages, results and generated-library hashes. Backend delivery
is verified separately by conformance's controlled capture. Do not claim export
success merely because the application exits successfully.
