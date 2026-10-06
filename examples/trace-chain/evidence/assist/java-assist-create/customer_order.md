<!-- ephemeral -->

# Java Assist — Create `Customer Order`

Use the exact generated `Q.customerOrders()` entry point. The
trusted `UserContext` owns actor, tenant, policy, provider, initialization, and
audit infrastructure; none of those values belong in writable business input.

```java
package com.teaql.tracechainservice;

import io.teaql.core.UserContext;
import com.teaql.tracechainservice.customerorder.CustomerOrder;

public final class CustomerOrderCreateService {
    private CustomerOrderCreateService() {}

    public static CustomerOrder create(
            com.teaql.tracechainservice.platform.Platform platform,
            java.lang.String orderNumber,
            java.lang.String description,
            UserContext context) {
        var entity = Q.customerOrders()
                .comment("what: initialize Customer Order")
                .purpose("why: create Customer Order")
                .newEntity(context);

        entity.updatePlatform(platform);
        entity.updateOrderNumber(orderNumber);
        entity.updateDescription(description);

        entity.auditAs("Create Customer Order for the requested business operation")
                .save(context);
        return entity;
    }
}
```

Only the generated updater methods above are writable. Constant candidates,
when present, are also generated and must be copied exactly:

Compile the source unchanged. Test persistence and query-back, and prove that
blank/missing intent, blank/missing audit reason, unknown fields, and attempted
trusted-context overrides fail. Never instantiate a generated entity directly
for creation and do not edit generated sources.

## Compose an audited object graph

Create children with their own generated Q entry point. The following exact
methods attach them to this object and set the corresponding parent reference:

| Reverse relation | Child creation entry point | Attach to parent |
| --- | --- | --- |
| `order_item_list` | `Q.orderItems()` | `entity.addOrderItem(child)` |
| `payment_list` | `Q.payments()` | `entity.addPayment(child)` |
| `shipment_list` | `Q.shipments()` | `entity.addShipment(child)` |


Use `child.comment("authorize payment")` for a child-specific mutation reason.
Leave the child's mutation comment unset when it should inherit its parent's
reason. Creation query intent and mutation intent are distinct. Use a child's
current Create Assist to populate its required business fields; do not save it
separately when the operation must persist as one graph.

For an already-loaded child, call `child.markForDeletion()` and then
`child.comment("remove unavailable item")`, keeping the child in the graph.
Finally call `entity.auditAs("submit order").save(context)` once. The runtime
assigns missing IDs, checks the complete graph, and persists inserts, updates
and deletion marks with each item's own root-to-leaf responsibility chain.


---

## TeaQL seven-language assist contract

Apply the verified Rust semantic ceiling while using only the exact JAVA generated and
runtime APIs. Discover APIs through the generated application AGENTS.md and progressive
model-aware Assist. Do not inspect generated domain-library source.

- Do not create plurals by appending `s` or `es`; use the centralized generated plural.
- Human and non-human entities use different generated predicate vocabularies. Preserve
  forms such as “who are active” and “whose email is”; never infer them from English.
- Configure filters, projection, paging, and other query options before `purpose(...)`.
  Comment may appear anywhere in the chain. Purpose enters the executable stage; execution
  requires both values, but comment does not have to immediately precede purpose.
- Every execute/list/stream and every save accepts exactly one context argument:
  `UserContext`. Name that argument `context`, never `runtime`; data services and global
  policy are injected when the context is built. Reserve `runtime` for process-level
  runtime ownership, provider/pool setup, and module assembly.
- Tenant, merchant, identity, permissions, request policy, purpose policy, hard limit,
  and continuous-page cursor policy come only from trusted context, never dynamic JSON or TFP.
- If the required operation is absent after current entity/action and required field
  Assist, stop that path and report MISSING_ASSIST. Do not guess an API or search the
  generated library as a fallback.
- Create each application-owned source file once. After its first compile attempt,
  repair only the smallest block identified by the exact compiler or test diagnostic.
  Preserve unrelated code; do not rewrite the complete file as an error-recovery loop.
- Before a repair that would replace more than 25% of an existing application file,
  stop and report LARGE_REWRITE_REQUEST with the file, exact diagnostic, reason, and
  estimated scope. Initial creation and model-driven regeneration are not repairs.

Capability: `create`.

- Validate and allow-list writable business fields; never mass-assign dynamic JSON.
- Create through the generated request/entity API, attach a non-empty audit reason,
  save with the same UserContext, and return the runtime's native save result.
- Add a negative test proving a missing audit reason cannot write.
