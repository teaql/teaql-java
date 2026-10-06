<!-- ephemeral -->

# Java Assist — Create `Shipment`

Use the exact generated `Q.shipments()` entry point. The
trusted `UserContext` owns actor, tenant, policy, provider, initialization, and
audit infrastructure; none of those values belong in writable business input.

```java
package com.teaql.tracechainservice;

import io.teaql.core.UserContext;
import com.teaql.tracechainservice.shipment.Shipment;

public final class ShipmentCreateService {
    private ShipmentCreateService() {}

    public static Shipment create(
            com.teaql.tracechainservice.customerorder.CustomerOrder customerOrder,
            java.lang.String referenceCode,
            UserContext context) {
        var entity = Q.shipments()
                .comment("what: initialize Shipment")
                .purpose("why: create Shipment")
                .newEntity(context);

        entity.updateCustomerOrder(customerOrder);
        entity.updateReferenceCode(referenceCode);

        entity.auditAs("Create Shipment for the requested business operation")
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
