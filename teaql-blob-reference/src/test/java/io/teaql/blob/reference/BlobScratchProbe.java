package io.teaql.blob.reference;

import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.blob.store.file.FileBlobStore;
import io.teaql.core.blob.*;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.sql.*;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.List;

/** Parent owns forced termination while an actual public service holds its staging lease. */
public final class BlobScratchProbe {
    public static void main(String[] arguments) throws Exception {
        var sql=new JdbcSqlExecutor(new PostgresBlobCatalogIT.TestDataSource(required("TEAQL_BLOB_TEST_PG_URL"),required("TEAQL_BLOB_TEST_PG_USER"),
                required("TEAQL_BLOB_TEST_PG_PASSWORD"),required("TEAQL_BLOB_CRASH_SCHEMA")));
        var catalog=new PostgresBlobCatalog(sql); Path root=Path.of(arguments[0]); var files=new FileBlobStore(root.resolve("store")); files.initialize();
        var metadata=new SimpleEntityMetaFactory(); var descriptor=new SQLEntityDescriptor(); descriptor.setType("Asset");
        descriptor.setTargetType(PostgresBlobCatalogIT.Asset.class); descriptor.setEntitySupplier(PostgresBlobCatalogIT.Asset::new);
        BlobSQLProperty property=(BlobSQLProperty)descriptor.addSimpleProperty("document",BlobRef.class,BlobSQLProperty::new);
        property.withBlobFieldSpec(BlobFieldSpec.of("Asset","document",List.of("pdf"),true,"default")); metadata.register(descriptor);
        var tika=new TikaBlobContentVerifier(root.resolve("verify"),TikaBlobContentVerifier.Limits.defaults());
        BlobContentVerifier verifier=arguments[1].equals("verify")?(source,extension,maximum,empty) -> tika.verify(new SignalInput(source),extension,maximum,empty):tika;
        try (var runtime=TeaQLRuntime.builder().metadata(metadata).telemetry(RuntimeTelemetry.NOOP).executionLogging(false).build()) {
            var context=new DefaultUserContext(runtime); context.putAttribute("scope",PostgresBlobCatalogIT.SCOPE);
            var service=new BlobUploadService(catalog,files,verifier,BlobPolicies.defaults(),new BlobUploadAuthorization(
                    actor -> actor.getAttribute("scope",BlobScope.class),(actor,target) -> true,(actor,target,action,info) -> true),Clock.fixed(Instant.parse(arguments[3]),ZoneOffset.UTC));
            if (arguments[1].equals("write")) try (var source=Files.newInputStream(Path.of(required("TEAQL_BLOB_TEST_FIXTURES"),"sample.pdf"))) { service.write(context,arguments[2],new SignalInput(source)); }
            else service.complete(context,arguments[2]);
        }
    }
    private static final class SignalInput extends FilterInputStream {
        private boolean signalled;
        SignalInput(InputStream source) { super(source); }
        private void signal() throws IOException { if (!signalled) { signalled=true; System.out.println("ready"); System.out.flush(); System.in.read(); } }
        @Override public int read() throws IOException { signal(); return in.read(); }
        @Override public int read(byte[] bytes,int offset,int length) throws IOException { signal(); return in.read(bytes,offset,length); }
    }
    private static String required(String name) { String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing probe environment"); return value; }
}
