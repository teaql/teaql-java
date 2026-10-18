package com.example.exactdecimalservice;

import com.example.exactdecimalservice.decimalentry.DecimalEntry;
import com.example.exactdecimalservice.platform.Platform;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.portable.IdSpaceIdGenerator;
import io.teaql.core.sqlite.SqliteDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.TeaQLRuntime;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.sqlite.SQLiteDataSource;

/** Application-owned exact Decimal check against a freshly generated domain library. */
public final class GeneratedExactDecimalProbe {
    private GeneratedExactDecimalProbe() {}

    private static TeaQLRuntime runtime(SQLiteDataSource dataSource, JdbcSqlExecutor sql) {
        return TeaQLRuntime.builder()
                .metadata(new SimpleEntityMetaFactory())
                .dataService("default", new SqliteDataServiceExecutor("default", sql, dataSource))
                .idGenerationService(new IdSpaceIdGenerator(new WorkspaceTeaQLDatabase(sql, dataSource)))
                .executionLogging(false)
                .build()
                .install(GeneratedRuntimeModule.module());
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("teaql-java-generated-exact-decimal-");
        SQLiteDataSource sourceData = new SQLiteDataSource();
        sourceData.setUrl("jdbc:sqlite:" + directory.resolve("source.sqlite"));
        JdbcSqlExecutor sourceSql = new JdbcSqlExecutor(sourceData);
        var source = new CustomUserContext(runtime(sourceData, sourceSql));
        source.ensureSchema();
        Platform platform = Q.platforms().withIdIs(1L)
                .comment("Locate the exact Decimal platform")
                .purpose("Attach ledger entries to their generated root")
                .executeForOne(source);
        if (platform == null) throw new AssertionError("seeded platform is missing");

        save(source, platform, "first", new BigDecimal("0.1"));
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

        System.out.println("GENERATED_EXACT_DECIMAL_PASS q=1 e=1 save=2 sum=0.3");
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
