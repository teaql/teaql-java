<!-- ephemeral -->

# Java Assist — Expression `Payment Attempt`

Generated expressions preserve three states: a loaded value, a loaded database
Null, and NotLoaded. `eval()` returns the first two as the native Java value or
`null`; it throws `TeaQLNotLoadedException` for NotLoaded. `orIfNull` applies only
to loaded Null and deliberately propagates NotLoaded.

The following is complete model-derived source.

```java
package com.teaql.tracechainservice;

import com.teaql.tracechainservice.paymentattempt.PaymentAttempt;

public final class PaymentAttemptExpressionService {
    private PaymentAttemptExpressionService() {}

    public static java.lang.Long extractId(PaymentAttempt entity) {
        return E.paymentAttempt(entity).getId().eval();
    }

    public static java.lang.Long extractIdOrIfNull(PaymentAttempt entity, java.lang.Long fallback) {
        return E.paymentAttempt(entity).getId().orIfNull(fallback);
    }

    public static com.teaql.tracechainservice.payment.Payment traversePayment(PaymentAttempt entity) {
        return E.paymentAttempt(entity).getPayment().eval();
    }

    public static java.lang.String extractReferenceCode(PaymentAttempt entity) {
        return E.paymentAttempt(entity).getReferenceCode().eval();
    }

    public static java.lang.String extractReferenceCodeOrIfNull(PaymentAttempt entity, java.lang.String fallback) {
        return E.paymentAttempt(entity).getReferenceCode().orIfNull(fallback);
    }

    public static java.lang.Long extractVersion(PaymentAttempt entity) {
        return E.paymentAttempt(entity).getVersion().eval();
    }

    public static java.lang.Long extractVersionOrIfNull(PaymentAttempt entity, java.lang.Long fallback) {
        return E.paymentAttempt(entity).getVersion().orIfNull(fallback);
    }

}
```

Select every field and relation before traversal. Never catch
`TeaQLNotLoadedException` merely to provide a default, and never replace the E
facade with optional chaining or direct getters.


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

Capability: `expression`.

- Distinguish a loaded null from a field or relation that was not projected. A
  NotLoaded/coding error must remain visible; do not turn it into an ordinary null.
- Select every traversed relation first and use the generated E/expression API for
  scalar, object, and list traversal. Do not translate Java accessor names by guess.
