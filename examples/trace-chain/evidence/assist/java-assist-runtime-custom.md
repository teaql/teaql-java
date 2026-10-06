<!-- ephemeral -->
# TeaQL Java Runtime Customization

```java
import io.teaql.core.DataServiceExecutor;
import io.teaql.core.MutationPolicyApprovalProvider;
import io.teaql.core.MutationPolicyRegistry;
import io.teaql.core.QueryPolicy;
import io.teaql.core.SchemaExecutor;
import io.teaql.core.meta.EntityMetaFactory;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeLogSink;
import io.teaql.runtime.TeaQLRuntime;
import java.util.List;
import java.util.Map;

public final class RuntimeCustomization {
    private RuntimeCustomization() {}

    public static TeaQLRuntime buildRuntime(
            EntityMetaFactory metadata,
            DataServiceExecutor provider,
            QueryPolicy queryPolicy,
            MutationPolicyRegistry mutationPolicies,
            MutationPolicyApprovalProvider mutationPolicyApprovals,
            RuntimeLogSink runtimeLogSink) {
        if (metadata == null || provider == null || queryPolicy == null
                || mutationPolicies == null || mutationPolicyApprovals == null) {
            throw new IllegalArgumentException("metadata, provider and policies are required");
        }
        return TeaQLRuntime.builder().metadata(metadata).dataService("default", provider)
                .queryPolicy(queryPolicy)
                .mutationPolicyRegistry(mutationPolicies)
                .mutationPolicyApprovalProvider(mutationPolicyApprovals)
                .logSink(runtimeLogSink).build();
    }

    public static DefaultUserContext requestContext(
            TeaQLRuntime runtime, String trustedTenant, AppAuditEventSink appAuditSink) {
        if (runtime == null || trustedTenant == null || trustedTenant.isBlank() || appAuditSink == null) {
            throw new IllegalArgumentException("trusted runtime, tenant and App Audit Sink are required");
        }
        DefaultUserContext context = new DefaultUserContext(runtime);
        context.putAttribute("trustedTenant", trustedTenant);
        context.putAttribute(AppAuditEventSink.class.getName(), appAuditSink);
        return context;
    }

    public static void readiness(DefaultUserContext context) {
        if (context.getAttribute("trustedTenant", String.class) == null) {
            throw new IllegalStateException("missing trusted tenant");
        }
        context.ensureSchema();
    }

    public static void rejectGovernanceOverride(Object input) {
        if (input instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (List.of("tenant", "provider", "queryPolicy", "mutationPolicy",
                        "mutationPolicyApproval", "auditSink", "hardLimit",
                        "continuousPage").stream().anyMatch(value -> value.equalsIgnoreCase(key))) {
                    throw new IllegalArgumentException("forbidden governance override: " + key);
                }
                rejectGovernanceOverride(entry.getValue());
            }
        } else if (input instanceof Iterable<?> values) {
            values.forEach(RuntimeCustomization::rejectGovernanceOverride);
        }
    }
}
```

The generated workspace owns application-scoped provider construction. `UserContext`
initialization is the trusted boundary for tenant data and the customizable App Audit Sink;
query/save still receive only that context. Raw row audit remains separate. Readiness invokes
the real `SchemaExecutor` and propagates provider failure. Reject governance keys recursively
from JSON/TFP. The integration gate must also run generated query and audited mutation against
SQLite; a route-only `/health` smoke test is insufficient.

## Runtime telemetry

Observability is optional and application-owned. Build
`io.teaql.opentelemetry.OpenTelemetryRuntimeTelemetry` from the application's
OpenTelemetry tracer, meter and logger, then pass it to
`TeaQLRuntime.builder().telemetry(telemetry)`. Keep the no-op default when it is
not configured. The application owns bounded SDK processors, OTLP exporters,
`forceFlush` and shutdown; telemetry failure must never change a query, save,
audit or readiness result. Installing telemetry does not call `ensureSchema`.
TeaQL derives `teaql.error.category` from the native error type. Sampling never
controls or replaces App Audit Sink delivery. Do not generate a Collector,
additional exporters, auto-discovery, or a telemetry configuration DSL.

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

Capability: `runtime-custom`.

- Keep trusted dependencies and global runtime policy in UserContext initialization.
  Custom providers, policy hooks, and audit sinks must not add execute/save arguments.
- Preserve immutable row audit events and a separate customizable App Audit Sink.
  Include health, integration, and negative governance tests for every customization.
