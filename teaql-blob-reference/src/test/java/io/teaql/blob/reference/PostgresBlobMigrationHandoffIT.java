package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import java.time.Instant;
import java.util.List;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** Opt-in two-runtime handoff; the conformance runner owns and deletes both databases. */
public class PostgresBlobMigrationHandoffIT {
    private static final BlobScope SCOPE=new BlobScope("test","tenant-a");
    private static final Instant NOW=Instant.ofEpochMilli(1_000_000);

    @Test public void currentCatalogCanHandOffToOrFromRust() {
        String phase=System.getenv("TEAQL_R7_HANDOFF_PHASE");
        Assume.assumeTrue("explicit R7 handoff phase required",phase!=null && phase.startsWith("java-"));
        assertTrue("unknown R7 handoff phase",phase.equals("java-first") || phase.equals("java-second"));
        String url=required("TEAQL_BLOB_TEST_PG_URL"),user=required("TEAQL_BLOB_TEST_PG_USER"),password=required("TEAQL_BLOB_TEST_PG_PASSWORD");
        var sql=new JdbcSqlExecutor(new PostgresBlobCatalogIT.TestDataSource(url,user,password,"public"));
        var catalog=new PostgresBlobCatalog(sql);
        if (phase.equals("java-first")) {
            sql.execute("CREATE TABLE asset_data(id BIGINT PRIMARY KEY,version BIGINT NOT NULL,document VARCHAR(128))");
            sql.execute("INSERT INTO asset_data VALUES (1,1,'legacy-invoice.pdf'),(2,1,'https://legacy.example.test/files/2.pdf')");
            assertEquals(BlobException.Code.BLOB_SCHEMA_MISSING,
                    assertThrows(BlobException.class,catalog::validateSchema).code());
        }
        catalog.migrate(); catalog.validateSchema();
        assertEquals(List.of("legacy-invoice.pdf","https://legacy.example.test/files/2.pdf"),
                sql.queryForList("SELECT document FROM asset_data ORDER BY id",new Object[0]).stream().map(row -> row.get("document")).toList());
        assertEquals(0L,((Number)sql.queryForList("SELECT count(*) AS count FROM teaql_blob_binding",new Object[0]).get(0).get("count")).longValue());
        if (phase.equals("java-second")) {
            BlobCatalogRecord foreign=catalog.find(SCOPE,BlobRef.of("blob_r7_rust")).orElseThrow();
            assertEquals(BlobState.UPLOADED_UNBOUND,foreign.state());
            assertEquals("sample.pdf",foreign.originalName());
            assertEquals(Long.valueOf(1),foreign.sizeBytes());
            assertEquals("application/pdf",foreign.contentType());
        }
        BlobFieldSpec field=BlobFieldSpec.of("Asset","document",List.of("pdf"),true,"default");
        var draft=new PostgresBlobCatalog.UploadDraft(SCOPE,BlobRef.of("blob_r7_java"),"upload_r7_java",
                "request_r7_java","a".repeat(64),"sample.pdf","pdf","owned/java",
                new BlobFieldTarget("Asset",null,"document"),BlobPolicies.defaults().resolve(field));
        assertEquals(BlobState.INITIATED,catalog.begin(draft,NOW).state());
        var complete=catalog.complete(SCOPE,draft.reference(),
                new PostgresBlobCatalog.VerifiedContent("application/pdf",1,"b".repeat(64),"version-java"),NOW.plusMillis(1));
        assertEquals(BlobState.UPLOADED_UNBOUND,complete.state());
        assertEquals(complete,catalog.find(SCOPE,draft.reference()).orElseThrow());
    }

    private static String required(String name) {
        String value=System.getenv(name);
        if (value==null || value.isBlank()) throw new IllegalStateException("Integration environment missing: "+name);
        return value;
    }
}
