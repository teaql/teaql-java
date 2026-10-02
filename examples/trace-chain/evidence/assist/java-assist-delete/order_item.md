<!-- ephemeral -->

# Java Assist — Delete `Order Item`

Load the current row and its original optimistic version. Delete means audited,
version-aware soft deletion; do not issue SQL or invent a physical-delete API.

```java
package com.teaql.tracechainservice;

import io.teaql.core.UserContext;

public final class OrderItemDeleteService {
    private OrderItemDeleteService() {}

    public static boolean delete(Long id, UserContext context) {
        var entity = Q.orderItems()
                .withIdIs(id)

                .comment("what: load current Order Item for deletion")
                .purpose("why: preserve original version for optimistic locking")
                .executeForOne(context);
        if (entity == null) {
            return false;
        }

        entity.markForDeletion()
                .auditAs("Delete Order Item for the requested business operation")
                .save(context);
        return true;
    }

    public static boolean restore(Long id, UserContext context) {
        var entity = Q.orderItems()
                .withIdIs(id)

                .deletedRowsOnly()
                .limit(1)
                .comment("what: load deleted Order Item for recovery")
                .purpose("why: preserve the deleted optimistic version")
                .executeForOne(context);
        if (entity == null) {
            return false;
        }
        entity.markToRecover()
                .auditAs("Restore Order Item for the requested business operation")
                .save(context);
        return true;
    }
}
```

Compile the source unchanged. Prove that the row remains stored with a negative
version, normal requests hide it, `deletedRowsOnly()` can retrieve it, a stale
independently loaded copy conflicts, a missing ID returns false, blank/missing
audit fails, and invented physical-delete methods do not compile.

Recovery needs no scalar-field update. `markToRecover()` records a pending recovery;
audited `save(context)` restores visibility and advances the negative version to
a positive one. A graph can recover several children with independent local comments
using one audited root save. Test committed SQL/readback/audit lineage as well as Q/E
visibility; a status flag alone is not evidence that the row was recovered.


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

Capability: `delete`.

- Load the policy-scoped current entity, mark it for deletion, then use audited
  save with the same UserContext. Do not invent a physical-delete API.
- Require an audit reason and optimistic version. Test missing audit and stale
  version as explicit failures.
