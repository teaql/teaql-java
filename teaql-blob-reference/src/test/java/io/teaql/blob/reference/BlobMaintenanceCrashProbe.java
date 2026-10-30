package io.teaql.blob.reference;

import io.teaql.blob.store.file.FileBlobStore;
import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.nio.file.Path;
import java.time.*;

/** Owned child JVM: deliberately bypasses finally at durable crash boundaries. No credentials in argv. */
public final class BlobMaintenanceCrashProbe {
    public static void main(String[] arguments) throws Exception {
        var sql=new JdbcSqlExecutor(new PostgresBlobCatalogIT.TestDataSource(required("TEAQL_BLOB_TEST_PG_URL"),
                required("TEAQL_BLOB_TEST_PG_USER"),required("TEAQL_BLOB_TEST_PG_PASSWORD"),required("TEAQL_BLOB_CRASH_SCHEMA")));
        var catalog=new PostgresBlobCatalog(sql); var files=new FileBlobStore(Path.of(arguments[0])); files.initialize();
        var store=new BlobStoreDelegate(files) {
            @Override public DeleteResult deleteIfVersion(UserContext context,String location,String version) {
                if (sql.hasActiveTransaction()) throw new IllegalStateException("Deletion claim not committed");
                if (!"deleting".equals(sql.queryForList("SELECT state FROM teaql_blob_object WHERE storage_location=?",new Object[]{location}).get(0).get("state"))) {
                    throw new IllegalStateException("Durable claim absent");
                }
                if (arguments[1].equals("before")) Runtime.getRuntime().halt(73);
                var result=super.deleteIfVersion(context,location,version);
                if (arguments[1].equals("after")) Runtime.getRuntime().halt(74);
                return result;
            }
        };
        try (var runtime=TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory()).telemetry(RuntimeTelemetry.NOOP).executionLogging(false).build()) {
            var context=new DefaultUserContext(runtime); context.putAttribute("scope",PostgresBlobCatalogIT.SCOPE);
            new BlobMaintenanceService(catalog,store,actor -> actor.getAttribute("scope",BlobScope.class),(actor,scope) -> true,
                    Clock.fixed(Instant.parse(arguments[2]),ZoneOffset.UTC),100).runBatch(context);
        }
        throw new IllegalStateException("Crash injection was not reached");
    }
    private static String required(String name) {
        String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing probe environment"); return value;
    }
}
