package com.example.schoolmanagementservice;

import io.teaql.core.DataServiceOperation;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.GeneratedSchemaBootstrap;
import io.teaql.core.TraceKind;
import io.teaql.core.TraceNode;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.AppAuditEventSink;
import io.teaql.runtime.SafeAuditEvent;
import io.teaql.runtime.TeaQLRuntime;
import java.util.ArrayList;
import java.util.List;

/** Actual generated bootstrap, without supplying expected trace frames to runtime. */
final class BootstrapTraceVerifier {
  private BootstrapTraceVerifier() {}

  static void verify(TeaQLRuntime installedRuntime) {
    var sql = new ArrayList<ExecutionMetadata>();
    var audits = new ArrayList<SafeAuditEvent>();
    var runtime = TeaQLRuntime.builder()
        .metadata(new SimpleEntityMetaFactory())
        .registry(installedRuntime.getRegistry())
        .idGenerationService(installedRuntime.getIdGenerationService())
        .logSink((caller, entry) -> sql.add(entry))
        .build().install(GeneratedRuntimeModule.module());
    var context = new CustomUserContext(runtime);
    context.putAttribute(AppAuditEventSink.class.getName(), (AppAuditEventSink) (caller, event) -> {
      if (event.traceChain().stream().anyMatch(node -> node.getComment().startsWith("create model")
          || node.getComment().startsWith("create generated")
          || node.getComment().startsWith("reconcile model"))) {
        require(GeneratedSchemaBootstrap.AUDIT_ACTOR.equals(
            caller.getAttribute(GeneratedSchemaBootstrap.AUDIT_ACTOR_ATTRIBUTE)),
            "Bootstrap audit lost its runtime actor");
        require(GeneratedSchemaBootstrap.AUDIT_CATEGORY.equals(
            caller.getAttribute(GeneratedSchemaBootstrap.AUDIT_CATEGORY_ATTRIBUTE)),
            "Bootstrap audit lost its category");
      }
      audits.add(event);
    });

    context.ensureSchema();
    boolean freshlySeeded = audits.size() == 3;
    require(freshlySeeded || audits.isEmpty(), "Expected a complete fresh bootstrap or unchanged existing seeds");
    verifyMutationTraces(sql, audits, freshlySeeded ? 3 : 0);
    require(sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.QUERY).count()
        == (freshlySeeded ? 6 : 3), "Bootstrap must retain lookup and authoritative readback evidence");
    sql.clear(); audits.clear();
    context.ensureSchema();
    require(audits.isEmpty(), "Repeated bootstrap emitted new mutation audit");
    require(sql.stream().noneMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION),
        "Repeated bootstrap executed a data write");
    require(sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.QUERY).count() == 3,
        "Repeated bootstrap must inspect root and both constants");

    var primary = Q.schoolTypes().withIdIs(1001L)
        .comment("load full constant for bootstrap reconciliation probe")
        .purpose("verify typed bootstrap repairs model drift with audited save").executeForOne(context);
    require(primary != null && primary.getVersion() > 0, "Missing active primary constant");
    long originalVersion = primary.getVersion();
    require(!freshlySeeded || originalVersion == 1L, "Fresh constant must start at version one");
    String originalName = primary.getName();
    primary.updateName("Temporary bootstrap probe value");
    primary.auditAs("prepare constant reconciliation probe").save(context);
    sql.clear(); audits.clear();
    context.ensureSchema();
    verifyMutationTraces(sql, audits, 1);
    require(audits.size() == 1 && "SchoolType".equals(audits.get(0).entityType())
        && Long.valueOf(1001).equals(audits.get(0).entityId()),
        "Reconciliation must audit only the changed constant");
    var restored = Q.schoolTypes().withIdIs(1001L)
        .comment("verify constant model value restored")
        .purpose("check bootstrap reconciliation and optimistic version").executeForOne(context);
    require(originalName.equals(restored.getName()) && restored.getVersion() == originalVersion + 2,
        "Typed bootstrap did not reconcile the model value with an optimistic update");
    require(context.getAttribute(GeneratedSchemaBootstrap.AUDIT_ACTOR_ATTRIBUTE) == null
        && context.getAttribute(GeneratedSchemaBootstrap.AUDIT_CATEGORY_ATTRIBUTE) == null,
        "Bootstrap contaminated caller audit identity");
    sql.clear(); audits.clear();
    context.ensureSchema();
    require(audits.isEmpty()
        && sql.stream().noneMatch(entry -> entry.getOperation() == DataServiceOperation.MUTATION),
        "Reconciled bootstrap must be idempotent");
    System.out.println("PASS Java generated bootstrap request intent, committed trace and reconciliation"
        + " fresh=" + freshlySeeded + " originalVersion=" + originalVersion);
  }

  private static void verifyMutationTraces(
      List<ExecutionMetadata> sql, List<SafeAuditEvent> audits, int expectedWrites) {
    var writes = sql.stream().filter(entry -> entry.getOperation() == DataServiceOperation.MUTATION).toList();
    require(writes.size() == expectedWrites, "Unexpected physical bootstrap mutation count");
    for (var entry : sql) {
      if (entry.getOperation() == DataServiceOperation.QUERY) {
        require(entry.getComment() != null && !entry.getComment().isBlank()
            && entry.getPurpose() != null && !entry.getPurpose().isBlank(),
            "Bootstrap lookup/readback lost request intent");
      }
    }
    for (var write : writes) {
      require(write.getAuditReason() != null && !write.getAuditReason().isBlank(),
          "Bootstrap mutation lost request comment");
      require(write.getTraceChain().stream().map(TraceNode::getKind).toList().equals(
          List.of(TraceKind.OPERATION, TraceKind.ENTITY, TraceKind.PROVIDER, TraceKind.SQL)),
          "Bootstrap mutation SQL route is not canonical");
      require(!write.getMutationLineage().isEmpty()
          && audits.stream().anyMatch(event -> event.traceChain().equals(write.getMutationLineage())),
          "Bootstrap SQL lineage has no matching committed audit");
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
