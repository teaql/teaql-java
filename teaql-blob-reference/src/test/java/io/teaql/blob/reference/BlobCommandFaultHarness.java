package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import io.teaql.core.meta.*;
import io.teaql.core.sql.*;
import io.teaql.dataservice.sql.SqlDataServiceExecutor;
import io.teaql.provider.jdbc.JdbcSqlExecutor;
import io.teaql.runtime.*;
import java.io.PrintWriter;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.postgresql.PGProperty;
import org.postgresql.jdbc.PgConnection;
import org.postgresql.util.HostSpec;
import static io.teaql.blob.reference.PostgresBlobCatalogIT.*;

/** Typed driver injection at the actual JDBC commit boundary. No reflective connection wrappers. */
final class BlobCommandFaultHarness implements AutoCloseable {
    enum Fault { NONE, ACK_LOST, CRASH_BEFORE_COMMIT, CRASH_AFTER_COMMIT }
    final AtomicReference<Fault> fault=new AtomicReference<>(Fault.NONE);
    final JdbcSqlExecutor sql;
    final TeaQLRuntime runtime;
    final DefaultUserContext actor;
    final PostgresBlobCommandExecutor commands;
    BlobCommandFaultHarness(String schema) {
        if (!schema.matches("r3_blob_it_[0-9a-f]{32}")) throw new IllegalArgumentException("Invalid owned schema");
        String url=required("TEAQL_BLOB_TEST_PG_URL"),configuredUser=required("TEAQL_BLOB_TEST_PG_USER"),configuredPassword=required("TEAQL_BLOB_TEST_PG_PASSWORD");
        var dataSource=new DataSource() {
            @Override public Connection getConnection() throws SQLException { return getConnection(configuredUser,configuredPassword); }
            @Override public Connection getConnection(String user,String password) throws SQLException {
                Properties credentials=new Properties(); credentials.setProperty("user",user); credentials.setProperty("password",password);
                Properties parsed=org.postgresql.Driver.parseURL(url,credentials);
                if (parsed==null) throw new SQLException("Invalid probe connection configuration");
                var connection=new PgConnection(new HostSpec[]{new HostSpec(PGProperty.PG_HOST.getOrDefault(parsed),PGProperty.PG_PORT.getInt(parsed))},parsed,url) {
                    @Override public void commit() throws SQLException {
                        Fault mode=fault.getAndSet(Fault.NONE);
                        if (mode==Fault.CRASH_BEFORE_COMMIT) Runtime.getRuntime().halt(75);
                        super.commit();
                        if (mode==Fault.CRASH_AFTER_COMMIT) Runtime.getRuntime().halt(76);
                        if (mode==Fault.ACK_LOST) throw new SQLException("Owned test discarded the durable commit acknowledgement","08006");
                    }
                };
                try (var statement=connection.createStatement()) { statement.execute("SET search_path TO "+schema); }
                catch (SQLException failed) { connection.close(); throw failed; }
                return connection;
            }
            @Override public PrintWriter getLogWriter() { return null; }
            @Override public void setLogWriter(PrintWriter writer) {}
            @Override public void setLoginTimeout(int seconds) {}
            @Override public int getLoginTimeout() { return 0; }
            @Override public Logger getParentLogger() { return Logger.getGlobal(); }
            @Override public <T> T unwrap(Class<T> type) throws SQLException { throw new SQLException("No wrapper"); }
            @Override public boolean isWrapperFor(Class<?> type) { return false; }
        };
        sql=new JdbcSqlExecutor(dataSource);
        var metadata=new SimpleEntityMetaFactory(); var descriptor=new SQLEntityDescriptor();
        descriptor.setType("Asset"); descriptor.setEntitySupplier(Asset::new); descriptor.setTargetType(Asset.class);
        ((GenericSQLProperty)descriptor.addSimpleProperty("id",Long.class)).setColumnType("BIGINT");
        ((GenericSQLProperty)descriptor.addSimpleProperty("version",Long.class)).setColumnType("BIGINT");
        ((GenericSQLProperty)descriptor.addSimpleProperty("title",String.class)).setColumnType("VARCHAR(100)");
        var document=(BlobSQLProperty)descriptor.addSimpleProperty("document",BlobRef.class,BlobSQLProperty::new);
        document.setTableName("asset_data"); document.setColumnType("VARCHAR(128)"); document.withBlobFieldSpec(BlobFieldSpec.of("Asset","document",List.of("pdf"),true,"default")); metadata.register(descriptor);
        var binding=new BlobBindingAuthorization(context -> context.getAttribute("scope",BlobScope.class),(context,target,kind) -> true,(context,target,blob) -> true);
        Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);
        runtime=TeaQLRuntime.builder().metadata(metadata).dataService("default",new SqlDataServiceExecutor("default",sql))
                .blobMutationParticipant(new PostgresBlobMutationParticipant(new PostgresBlobCatalog(sql),BlobPolicies.defaults(),binding,clock))
                .telemetry(RuntimeTelemetry.NOOP).executionLogging(false).build();
        actor=new DefaultUserContext(runtime); actor.putAttribute("scope",SCOPE); actor.putAttribute("principal","alice");
        commands=new PostgresBlobCommandExecutor(new PostgresBlobCommandJournal(sql),BlobCommandIT.authorization(),clock);
    }
    @Override public void close() { runtime.close(); }
    private static String required(String name) { String value=System.getenv(name); if (value==null || value.isBlank()) throw new IllegalStateException("Missing probe configuration"); return value; }
}
