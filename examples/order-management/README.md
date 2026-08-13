# Order Management — Java + SQLite

Use Java 21. No database server, fixture DB, model input, or generator installation is needed.

```bash
cd examples/order-management
mvn -q install -DskipTests
mvn -q exec:java -pl java-app-console
```

The first run creates `.local/order.db`, ensures schema from generated metadata, seeds through generated entities, performs a governed query, and saves an audited preset. The second run demonstrates idempotency.

Read `java-app-console/.../OrderManagementApp.java` first (handwritten), then `java-lib-core/lib/.../Q.java`, `CustomerOrderRequest.java`, and `CustomerOrder.java` (generated). Java is the naming and governance gold standard: `comment(...)` may appear anywhere before `purpose(...)`; only the purposed request exposes execute methods.

## Verify the first result

Expect one `WEB-2026-001` row dated `2026-08-12` with amount `129.95`. The first run says it created the database and preset; the second says both seed data and preset already exist. Inspect the TeaQL SQL log to connect the fluent request to the executed SQLite statement.

## Customize it

Change the `withOrderNumberContaining` filter, ordering, or projection in the app and rerun. Add business behavior only under `java-app-console`; regenerate everything under `java-lib-core`. This library was generated from the shared Order Management model used by the six-language example suite, but that model and the generator are not runtime prerequisites.
### Materialized-list hard limit

`executeForList` protects the service by applying a default hard limit of 10,000 rows. A requested page size above that ceiling fails explicitly. Trusted application code can call `hardLimit(...)` to override the outer-query ceiling. **Caution:** most applications should not override it; do so only for a reviewed, exceptional requirement. This setting does not describe streaming execution.
