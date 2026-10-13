# Java Standard Telemetry Order Example

This example uses generated Q/E and the ordinary `executeForList(context)`
entry point to load a filtered order list with bounded pagination, status
details, child previews, child counts and status facets. No business method
initializes an SDK or creates a span.

The five-object model seeds one Platform and three status constants through
generated audited mutations. Repeated ensureSchema does not produce duplicate
writes or audits. Four orders and twenty items are created through the same
mutation API. The filtered result contains three orders; the selected one-row
slice has three item previews and an independent count of five items. Status
facets have counts `1, 2, 0` and amount totals `100, 500, 0`. The deliberately
omitted order amount remains NotLoaded.

The local runtime handles a declared ungrouped count alongside rows under one
logical list invocation. Grouped and mixed aggregate requests keep their
existing aggregate semantics. All generated library files are produced from
the checked model; do not patch them to change query behavior.

Run the local application acceptance twice, with startup guidance and unchanged
generated-library hashes:

```bash
bash examples/standard-order/verify.sh
```

The optional standard module enables all supported signals with these trusted
process settings:

```bash
TEAQL_TELEMETRY_ENABLED=true \
TEAQL_TELEMETRY_SERVICE_NAME=order-example \
TEAQL_TELEMETRY_OTLP_ENDPOINT=http://127.0.0.1:4318 \
  mvn -q -Pruntime-examples -pl examples/standard-order -am \
    -Dtest=StandardOrderTelemetryTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Ordinary SQL logs are disabled in the example. The checked model marks order
numbers and customer names for masking; telemetry must still export safe
expanded SQL. Conformance's `telemetry/capture-java-standard-order.cjs` checks
actual backend delivery and parentage. A successful application run alone does
not establish that all SQL diagnostics reached the collector.
