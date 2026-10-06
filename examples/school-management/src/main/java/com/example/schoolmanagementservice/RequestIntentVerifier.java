package com.example.schoolmanagementservice;

import io.teaql.core.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.runtime.TeaQLRuntime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Application-owned acceptance probe over the retained generated School Q/Mutation APIs. */
final class RequestIntentVerifier {
  private RequestIntentVerifier() {}

  static void verify(TeaQLRuntime original) {
    var statements = new ArrayList<ExecutionMetadata>();
    var queryPolicyCalls = new AtomicInteger();
    var mutationPolicyCalls = new AtomicInteger();
    var runtime = TeaQLRuntime.builder()
        .metadata(new SimpleEntityMetaFactory())
        .registry(original.getRegistry())
        .idGenerationService(original.getIdGenerationService())
        .queryPolicy(new QueryPolicy() {
          @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
            queryPolicyCalls.incrementAndGet();
          }
        })
        .mutationPolicyRegistry(key -> Optional.of(new MutationPolicy() {
          @Override public MutationPolicyIdentity identity() {
            return new MutationPolicyIdentity("school.intent.probe", "1", "local-example");
          }
          @Override public MutationDecision review(UserContext context, MutationPlan plan) {
            mutationPolicyCalls.incrementAndGet(); return MutationDecision.allow();
          }
        }))
        .logSink((context, metadata) -> statements.add(metadata))
        .build().install(GeneratedRuntimeModule.module());
    UserContext context = new CustomUserContext(runtime);
    context.pushTrace(TraceKind.COMMENT, "School", "unrelated ambient comment");
    context.pushTrace(TraceKind.PURPOSE, "School", "unrelated ambient purpose");
    try {
      required("REQUEST_COMMENT_REQUIRED", () -> runtime.executeForList(context, Q.schools()));
      required("REQUEST_COMMENT_REQUIRED", () -> runtime.executeForStream(context, Q.schools()));
      required("REQUEST_COMMENT_REQUIRED", () -> runtime.aggregation(context, Q.schools()));
      required("REQUEST_COMMENT_REQUIRED", () -> runtime.executeForPage(context, Q.schools(), 0, 10));
      required("QUERY_PURPOSE_REQUIRED", () -> runtime.executeForList(context, Q.schools().comment("load schools")));
      require(queryPolicyCalls.get() == 0 && statements.isEmpty(), "Invalid query reached policy or SQL");
      for (String blank : new String[]{"", " \t\r\n", "\u2003", "\u00a0"}) {
        required("REQUEST_COMMENT_REQUIRED", () -> Q.schools().comment(blank).purpose("verify root intent"));
      }
      var school = Q.schools().comment("prepare request-intent rejection probe")
          .purpose("verify mutation intent before Checker and provider access").newEntity(context);
      statements.clear();
      school.setComment("\u2003");
      required("REQUEST_COMMENT_REQUIRED", () -> runtime.saveGraph(context, school));
      require(mutationPolicyCalls.get() == 0 && statements.isEmpty(), "Invalid mutation reached policy or SQL");
    } finally {
      context.popTrace(); context.popTrace();
    }

    var rows = Q.schools().top(10)
        .selectPlatformWith(Q.platformsWithMinimalFields().selectName())
        .selectSchoolTypeWith(Q.schoolTypesWithMinimalFields().selectCode())
        .comment("load request-intent relation probe")
        .purpose("verify inherited root intent through generated relation requests")
        .executeForList(context);
    require(!rows.isEmpty(), "School relation probe returned no fixture");
    require(!statements.isEmpty(), "The valid generated query emitted no SQL diagnostics");
    for (var statement : statements) {
      require("load request-intent relation probe".equals(statement.getComment()), "Derived query lost the root comment");
      require("verify inherited root intent through generated relation requests".equals(statement.getPurpose()),
          "Derived query lost the root purpose");
    }
    require(context.getTraceChain().isEmpty(), "Request intent leaked into the reused context");
    System.out.println("PASS Java request-owned comment/purpose gates and generated relation inheritance");
  }

  private static void required(String code, Runnable action) {
    try {
      action.run(); throw new IllegalStateException("Expected " + code);
    } catch (RequestIntentException error) {
      require(code.equals(error.getCode()), "Unexpected intent rejection: " + error.getCode());
      require((code.equals("QUERY_PURPOSE_REQUIRED") ? "purpose" : "comment").equals(error.getField()),
          "Intent diagnostic has the wrong field location");
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }
}
