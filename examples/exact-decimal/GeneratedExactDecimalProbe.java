package com.example.exactdecimalservice;

import com.example.exactdecimalservice.decimalentry.DecimalEntry;
import com.example.exactdecimalservice.platform.Platform;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteCommittedMutationRecorder;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.core.sqlite.SqliteModelCheckedCommittedTarget;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sqlite.SQLiteDataSource;

/** Application-owned exact Decimal check against a freshly generated domain library. */
public final class GeneratedExactDecimalProbe {
    private static final String SOURCE = "generated-exact-decimal";

    private GeneratedExactDecimalProbe() {}

    private static TeaQLRuntime runtime(SQLiteDataSource dataSource, JdbcSqlExecutor sql,
            SqliteCommittedMutationRecorder recorder) {
        var builder = TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .dataService("default", new SqliteDataServiceExecutor("default", sql, dataSource))
                .idGenerationService(new IdSpaceIdGenerator(new WorkspaceTeaQLDatabase(sql, dataSource)))
                .executionLogging(false);
        if (recorder != null) builder.committedMutationRecorder(recorder);
        return builder.build().install(GeneratedRuntimeModule.module());
    }

    public static void main(String[] args) throws Exception {
        String modelHash = System.getenv("TEAQL_TEST_MODEL_HASH");
        if (modelHash == null || !modelHash.matches("sha256:[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Exact model hash is required");
        }
        Path directory = Files.createTempDirectory("teaql-java-generated-exact-decimal-");
        SQLiteDataSource sourceData = new SQLiteDataSource();
        sourceData.setUrl("jdbc:sqlite:" + directory.resolve("source.sqlite"));
        JdbcSqlExecutor sourceSql = new JdbcSqlExecutor(sourceData);
        new CustomUserContext(runtime(sourceData, sourceSql, null)).ensureSchema();

        var recorder = new SqliteCommittedMutationRecorder(sourceSql, SOURCE, modelHash);
        recorder.eventStore().ensureSchema();
        var source = new CustomUserContext(runtime(sourceData, sourceSql, recorder));
        source.ensureSchema();
        Platform platform = Q.platforms().withIdIs(1L)
                .comment("Locate the exact Decimal platform")
                .purpose("Attach ledger entries to their generated root")
                .executeForOne(source);
        if (platform == null) throw new AssertionError("seeded platform is missing");

        DecimalEntry first = save(source, platform, "first", new BigDecimal("0.1"));
        save(source, platform, "second", new BigDecimal("0.2"));
        var loaded = Q.decimalEntriesWithMinimalFields()
                .selectLabel().selectAmount()
                .withAmountIs(new BigDecimal("0.1"))
                .orderByIdAscending().limit(10)
                .comment("Read a generated exact Decimal row")
                .purpose("Verify Q filtering and E hydration")
                .executeForList(source);
        if (loaded.size() != 1 || !"first".equals(E.decimalEntry(loaded.get(0)).getLabel().eval())
                || E.decimalEntry(loaded.get(0)).getAmount().eval().compareTo(new BigDecimal("0.1")) != 0) {
            throw new AssertionError("generated Decimal Q/E round-trip failed");
        }
        if (sum(source).compareTo(new BigDecimal("0.3")) != 0) {
            throw new AssertionError("generated Decimal SUM is not exact");
        }

        var outbox = recorder.eventStore().outboxAfter(SOURCE, 0, 10);
        if (outbox.size() != 2) throw new AssertionError("two audited saves must emit two committed sets");
        SQLiteDataSource targetData = new SQLiteDataSource();
        targetData.setUrl("jdbc:sqlite:" + directory.resolve("target.sqlite"));
        JdbcSqlExecutor targetSql = new JdbcSqlExecutor(targetData);
        var target = new CustomUserContext(runtime(targetData, targetSql, null));
        target.ensureSchema();
        var checkedTarget = new SqliteModelCheckedCommittedTarget(targetSql, SOURCE, modelHash);
        checkedTarget.ensureSchema();
        var appliedFirst = checkedTarget.apply(target, outbox.get(0));
        var appliedSecond = checkedTarget.apply(target, outbox.get(1));
        var duplicate = checkedTarget.apply(target, outbox.get(0));
        if (appliedFirst.duplicate() || appliedSecond.duplicate() || !duplicate.duplicate()
                || appliedSecond.checkpoint() != 2 || duplicate.checkpoint() != 1) {
            throw new AssertionError("committed Decimal apply and duplicate receipts are invalid");
        }
        var replica = Q.decimalEntriesWithMinimalFields().selectAmount()
                .withIdIs(first.getId()).limit(1)
                .comment("Read a replicated generated Decimal row")
                .purpose("Verify exact target hydration")
                .executeForOne(target);
        if (replica == null || E.decimalEntry(replica).getAmount().eval()
                .compareTo(new BigDecimal("0.1")) != 0
                || sum(target).compareTo(new BigDecimal("0.3")) != 0) {
            throw new AssertionError("replicated Decimal value or SUM is inexact");
        }
        System.out.println("GENERATED_EXACT_DECIMAL_PASS q=1 e=1 save=2 sum=0.3 cmr=2 duplicate=1");
    }

    private static DecimalEntry save(CustomUserContext context, Platform platform,
            String label, BigDecimal amount) {
        DecimalEntry entry = Q.decimalEntries()
                .comment("Initialize generated Decimal entry")
                .purpose("Record an auditable ledger value")
                .newEntity(context);
        entry.updatePlatform(platform);
        entry.updateLabel(label);
        entry.updateAmount(amount);
        entry.auditAs("Create exact Decimal entry for generated integration gate").save(context);
        return entry;
    }

    private static BigDecimal sum(CustomUserContext context) {
        var rows = Q.decimalEntriesWithMinimalFields().sumAmountAs("total")
                .comment("Sum generated exact Decimal entries")
                .purpose("Verify financial aggregation without binary floating point")
                .aggregation(context).toList();
        if (rows.size() != 1 || !(rows.get(0).get("total") instanceof BigDecimal total)) {
            throw new AssertionError("generated Decimal aggregate has the wrong shape");
        }
        return total;
    }
}
