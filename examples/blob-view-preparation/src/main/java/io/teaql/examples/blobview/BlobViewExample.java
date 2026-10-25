package io.teaql.examples.blobview;

import io.teaql.blob.reference.*;
import io.teaql.blob.store.file.*;
import io.teaql.blob.content.tika.TikaBlobContentVerifier;
import io.teaql.core.*;
import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.*;
import io.teaql.core.view.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** Real library-only CLI. The optional storage provider owns its temporary HTTP listener. */
public final class BlobViewExample {
    private static final BlobScope SCOPE=new BlobScope("example","tenant-a");
    private static final SecureRandom RANDOM=new SecureRandom();
    public static void main(String[] arguments) {
        try {
            Path root=Files.createTempDirectory("teaql-owned-blob-view-");
            try { run(root); }
            finally { try (var paths=Files.walk(root)) { for (var path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
        } catch (Exception failed) {
            String code=failed instanceof ViewPreparationException view?view.code().name():failed.getClass().getSimpleName();
            System.err.println("FAIL standalone Blob view: "+code); System.exit(1);
        }
    }
    private static void run(Path root) throws Exception {
        Clock clock=Clock.systemUTC(); byte[] signingKey=new byte[32]; RANDOM.nextBytes(signingKey);
        var files=new FileBlobStore(root.resolve("objects"));
        try (var database=new OwnedDatabase(); var gateway=new FileBlobHttpGateway(files,new InetSocketAddress("127.0.0.1",0),
                URI.create("http://127.0.0.1:0/blob-read"),"example",Map.of("example",signingKey),clock,Duration.ofMinutes(10))) {
            var sql=database.sql; var catalog=new PostgresBlobCatalog(sql); var journal=new PostgresBlobCommandJournal(sql);
            sql.execute("CREATE TABLE asset_data(id BIGINT PRIMARY KEY,version BIGINT NOT NULL,title VARCHAR(100),document VARCHAR(128))");
            catalog.migrate(); journal.migrate(); files.initialize(); gateway.start();
            var scopes=(BlobBindingAuthorization.ScopeProvider)context -> context.getAttribute("scope",BlobScope.class);
            var module=BlobReferenceModule.builder().catalog(catalog).store(gateway).clock(clock)
                    .verifier(new TikaBlobContentVerifier(root.resolve("verify"),TikaBlobContentVerifier.Limits.defaults()))
                    .bindingAuthorization(new BlobBindingAuthorization(scopes,(context,target,kind) -> true,(context,target,blob) -> true))
                    .uploadAuthorization(new BlobUploadAuthorization(scopes,(context,target) -> true,(context,target,action,info) -> true))
                    .readAuthorization(new BlobReadAuthorization(scopes,(context,target) -> true,(context,target,blob) -> true))
                    .maintenancePermission((context,scope) -> false)
                    .commands(journal,new BlobCommandAuthorization(scopes,context -> context.getAttribute("principal",String.class),(context,request) -> true,(context,receipt) -> true)).build();
            try (var runtime=module.installInto(TeaQLRuntime.builder().metadata(metadata()).dataService("default",new SqlDataServiceExecutor("default",sql))
                    .viewAuthorization(viewPolicy()).viewClock(clock).telemetry(RuntimeTelemetry.NOOP).executionLogging(false)).build()) {
                runtime.install(RuntimeModule.of().withRequiredCapabilities("blob.v1","blob.command.v1","view.v1")); runtime.validateReadiness();
                var actor=new DefaultUserContext(runtime); actor.putAttribute("scope",SCOPE); actor.putAttribute("principal","cli");
                byte[] bytes=Files.readAllBytes(Path.of(required("TEAQL_VIEW_PDF"))); var upload=actor.capability(BlobRuntime.class).upload();
                var session=upload.begin(actor,new BlobFieldTarget("Asset",null,"document"),new BlobRuntime.Uploads.FileInfo("sample.pdf",null,null),"example-upload");
                upload.write(actor,session.uploadId(),new ByteArrayInputStream(bytes)); BlobRef key=upload.complete(actor,session.uploadId());
                var entity=new Asset(); entity.__internalInitializeNewEntityId(42L); entity.updateProperty("title","example"); entity.updateProperty("document",key); entity.setComment("standalone Blob view example");
                AtomicInteger actions=new AtomicInteger(); var commands=actor.capability(BlobCommandService.class);
                var request=new BlobCommandService.Request("Asset.create/v1","example-command",("Asset:42:"+key.key()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                commands.execute(actor,request,() -> { actions.incrementAndGet(); runtime.saveGraph(actor,entity); return List.of(new BlobCommandService.EntityRevision("Asset",42,entity.getVersion())); });
                var options=ViewOptions.afterCommit(); var prepared=runtime.prepareView(actor,Map.of("data",entity),options);
                var body=(Map<?,?>)((Map<?,?>)prepared.value()).get("data"); URI url=URI.create((String)body.get("document"));
                var connection=(HttpURLConnection)url.toURL().openConnection(); connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
                try (var stream=connection.getInputStream()) { require(Arrays.equals(bytes,stream.readAllBytes())); require("application/pdf".equals(connection.getContentType())); }
                finally { connection.disconnect(); }
                require(prepared==runtime.prepareView(actor,prepared,options)); require(key.equals(entity.document));
                require(key.key().equals(database.independent.queryForList("SELECT document FROM asset_data WHERE id=42",new Object[0]).get(0).get("document")));
                gateway.close();
                try { runtime.prepareView(actor,entity,ViewOptions.afterCommit()); throw new IllegalStateException("Expected unavailable storage"); }
                catch (ViewPreparationException failed) { require(failed.businessCommitted()); }
                require(commands.resolve(actor,request).isPresent()); require(actions.get()==1);
                require(((Number)database.independent.queryForList("SELECT version FROM asset_data WHERE id=42",new Object[0]).get(0).get("version")).longValue()==1);
                System.out.println("PASS Blob view: real upload/verification, atomic command, complete HTTP URL, stable SQL Key, presentation reuse and committed-failure recovery");
            }
        }
    }
    private static EntityMetaFactory metadata() {
        var metadata=new SimpleEntityMetaFactory(); var descriptor=new SQLEntityDescriptor(); descriptor.setType("Asset"); descriptor.setTargetType(Asset.class); descriptor.setEntitySupplier(Asset::new);
        ((GenericSQLProperty)descriptor.addSimpleProperty("id",Long.class)).setColumnType("BIGINT");
        ((GenericSQLProperty)descriptor.addSimpleProperty("version",Long.class)).setColumnType("BIGINT");
        ((GenericSQLProperty)descriptor.addSimpleProperty("title",String.class)).setColumnType("VARCHAR(100)");
        var document=(BlobSQLProperty)descriptor.addSimpleProperty("document",BlobRef.class,BlobSQLProperty::new);
        document.setTableName("asset_data"); document.setColumnType("VARCHAR(128)"); document.withBlobFieldSpec(BlobFieldSpec.of("Asset","document",List.of("pdf"),true,"default")); metadata.register(descriptor); return metadata;
    }
    private static ViewAuthorization viewPolicy() {
        return new ViewAuthorization() {
            @Override public Identity identity(UserContext context) { var scope=context.getAttribute("scope",BlobScope.class); return new Identity(scope.realm(),context.getAttribute("principal",String.class),scope.boundary()); }
            @Override public boolean canReadEntity(UserContext context,EntityTarget target,String path) { return true; }
            @Override public FieldDecision field(UserContext context,EntityTarget target,String field,String path,ViewOptions.Mode mode) { return FieldDecision.show(); }
        };
    }
    private static void require(boolean condition) { if (!condition) throw new IllegalStateException("Standalone view invariant failed"); }
    private static String required(String name) { String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing CLI configuration: "+name); return value; }
    public static final class Asset extends BaseEntity {
        private BlobRef document; private String title;
        @Override public String typeName() { return "Asset"; }
        @Override public void __internalSet(String field,Object value) { switch (field) {
            case "document" -> { document=(BlobRef)value; markPropertyLoaded(field); } case "title" -> { title=(String)value; markPropertyLoaded(field); } default -> super.__internalSet(field,value);
        } }
        @Override public Object __internalGet(String field) { return switch (field) { case "document" -> document; case "title" -> title; default -> super.__internalGet(field); }; }
    }
    private static final class OwnedDatabase implements AutoCloseable {
        final String schema="r4_view_cli_"+UUID.randomUUID().toString().replace("-","");
        final String url=required("TEAQL_BLOB_TEST_PG_URL"),user=required("TEAQL_BLOB_TEST_PG_USER"),password=required("TEAQL_BLOB_TEST_PG_PASSWORD");
        final JdbcSqlExecutor sql,independent;
        OwnedDatabase() throws SQLException {
            try (Connection connection=DriverManager.getConnection(url,user,password); Statement statement=connection.createStatement()) { statement.execute("CREATE SCHEMA "+schema); }
            var source=new DataSource() {
                @Override public Connection getConnection() throws SQLException {
                    Connection connection=DriverManager.getConnection(url,user,password);
                    try (Statement statement=connection.createStatement()) { statement.execute("SET search_path TO "+schema); statement.execute("SET lock_timeout='5s'"); statement.execute("SET statement_timeout='10s'"); }
                    catch (SQLException failed) { connection.close(); throw failed; } return connection;
                }
                @Override public Connection getConnection(String user,String password) throws SQLException { throw new SQLFeatureNotSupportedException(); }
                @Override public PrintWriter getLogWriter() { return null; }
                @Override public void setLogWriter(PrintWriter writer) {}
                @Override public void setLoginTimeout(int seconds) {}
                @Override public int getLoginTimeout() { return 0; }
                @Override public Logger getParentLogger() { return Logger.getLogger("teaql.example.blobview"); }
                @Override public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLFeatureNotSupportedException(); }
                @Override public boolean isWrapperFor(Class<?> type) { return false; }
            };
            sql=new JdbcSqlExecutor(source); independent=new JdbcSqlExecutor(source);
        }
        @Override public void close() throws SQLException { try (Connection connection=DriverManager.getConnection(url,user,password); Statement statement=connection.createStatement()) { statement.execute("DROP SCHEMA "+schema+" CASCADE"); } }
    }
}
