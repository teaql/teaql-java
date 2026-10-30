package io.teaql.blob.reference;

import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobCommandService.*;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import static io.teaql.core.blob.BlobException.Code.*;

/** Fixed, independently versioned infrastructure. Receipt and business rows borrow this exact owner. */
public final class PostgresBlobCommandJournal {
    private static final String MIGRATION="/io/teaql/blob/reference/postgres-command-journal-v1.sql";
    private final SqlExecutionAdapter sql;
    public PostgresBlobCommandJournal(SqlExecutionAdapter sql) { this.sql=Objects.requireNonNull(sql); }
    void requireCatalog(PostgresBlobCatalog catalog) { catalog.requireResource(sql); requireCompletion(); }
    void requireCompletion() { if (!sql.supportsTransactionCompletion()) throw new BlobException(BLOB_TRANSACTION_REQUIRED); }
    void requireIdle() { if (sql.hasActiveTransaction() || sql.activeTransactionIdentity()!=null) throw new BlobException(BLOB_TRANSACTION_REQUIRED); }
    Object owner() {
        Object owner=sql.activeTransactionIdentity(); if (!sql.hasActiveTransaction() || owner==null) throw new BlobException(BLOB_TRANSACTION_REQUIRED); return owner;
    }
    void requireOwner(Object expected) { if (owner()!=expected) throw new BlobException(BLOB_TRANSACTION_REQUIRED); }
    BlobCommandScope openScope() { return BlobCommandScope.open(sql,owner()); }
    <T> T transaction(Supplier<T> action) {
        requireCompletion(); requireIdle(); List<T> result=new ArrayList<>(1);
        sql.executeInTransaction(() -> result.add(action.get()));
        if (result.size()!=1) throw new BlobException(BLOB_CATALOG_FAILED); return result.get(0);
    }
    /** Explicit migration. Does not alter catalog-v1 or business schema. */
    public void migrate() {
        BlobCommandScope.requireAbsent(); requireIdle();
        transaction(() -> {
            query("SELECT pg_advisory_xact_lock(73402110318408)");
            execute("CREATE TABLE IF NOT EXISTS teaql_blob_command_schema (id INTEGER PRIMARY KEY CHECK (id=1),schema_version INTEGER NOT NULL,schema_digest VARCHAR(64) NOT NULL)");
            update("INSERT INTO teaql_blob_command_schema VALUES(1,0,'pending') ON CONFLICT(id) DO NOTHING");
            var row=one(query("SELECT schema_version,schema_digest FROM teaql_blob_command_schema WHERE id=1 FOR UPDATE"));
            if (number(row,"schema_version")==0) {
                for (String statement:migration().split(";")) if (!statement.isBlank()) execute(statement);
                update("UPDATE teaql_blob_command_schema SET schema_version=1,schema_digest=? WHERE id=1",digest());
            }
            validateSchema(); return null;
        });
    }
    public void validateReadiness() { BlobCommandScope.requireAbsent(); requireIdle(); requireCompletion(); validateSchema(); }
    public void validateSchema() {
        try {
            var row=one(query("SELECT schema_version,schema_digest FROM teaql_blob_command_schema WHERE id=1"));
            if (number(row,"schema_version")!=1 || !digest().equals(row.get("schema_digest"))) throw schema();
            Map<String,String> columns=new HashMap<>(Map.of("scope_realm","character varying(256)","scope_boundary","character varying(256)","operation","character varying(128)",
                    "invocation_key","character varying(128)","request_digest","character varying(64)","principal_digest","character varying(64)","receipt_json","text","completed_at","bigint"));
            for (var column:query("SELECT attname,format_type(atttypid,atttypmod) AS type,attnotnull FROM pg_attribute WHERE attrelid='teaql_blob_command'::regclass AND attnum>0 AND NOT attisdropped")) {
                String name=(String)column.get("attname");
                if (!Objects.equals(columns.remove(name),column.get("type")) || !Boolean.valueOf(!Set.of("receipt_json","completed_at").contains(name)).equals(column.get("attnotnull"))) throw schema();
            }
            if (!columns.isEmpty()) throw schema();
            Map<String,String> constraints=new HashMap<>(Map.of(
                    "teaql_blob_command_pk","PRIMARY KEY (scope_realm, scope_boundary, operation, invocation_key)",
                    "teaql_blob_command_completion_ck","CHECK (((receipt_json IS NULL) = (completed_at IS NULL)))",
                    "teaql_blob_command_time_ck","CHECK (((completed_at IS NULL) OR (completed_at >= 0)))",
                    "teaql_blob_command_size_ck","CHECK (((receipt_json IS NULL) OR (octet_length(receipt_json) <= 1048576)))"));
            for (var constraint:query("SELECT conname,convalidated,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE conrelid='teaql_blob_command'::regclass")) {
                String name=(String)constraint.get("conname");
                if (constraints.containsKey(name)) {
                    if (!Boolean.TRUE.equals(constraint.get("convalidated")) || !constraints.remove(name).equals(constraint.get("definition"))) throw schema();
                }
            }
            if (!constraints.isEmpty()) throw schema();
        } catch (RuntimeException invalid) { throw schema(); }
    }
    record Claim(boolean fresh,Receipt receipt) {}
    Claim claim(BlobCommandAuthorization.Identity identity,Request request) {
        owner(); validateSchema();
        int inserted=update("INSERT INTO teaql_blob_command(scope_realm,scope_boundary,operation,invocation_key,request_digest,principal_digest) VALUES(?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                identity.scope().realm(),identity.scope().boundary(),request.operation(),request.invocationKey(),request.fingerprint(),identity.principalDigest());
        var row=one(query("SELECT * FROM teaql_blob_command WHERE scope_realm=? AND scope_boundary=? AND operation=? AND invocation_key=? FOR UPDATE",key(identity.scope(),request)));
        requireIdentity(row,identity,request);
        if (inserted==1 && row.get("completed_at")==null && row.get("receipt_json")==null) return new Claim(true,null);
        if (inserted!=0) throw new BlobException(BLOB_CATALOG_FAILED);
        return new Claim(false,receipt(row,identity.scope(),request));
    }
    void complete(BlobCommandAuthorization.Identity identity,Request request,Receipt receipt,Object owner) {
        requireOwner(owner);
        int changed=update("UPDATE teaql_blob_command SET receipt_json=?,completed_at=? WHERE scope_realm=? AND scope_boundary=? AND operation=? AND invocation_key=? AND request_digest=? AND principal_digest=? AND completed_at IS NULL AND receipt_json IS NULL",
                BlobCommandReceiptJson.encode(receipt.entities()),receipt.completedAt().toEpochMilli(),identity.scope().realm(),identity.scope().boundary(),request.operation(),request.invocationKey(),request.fingerprint(),identity.principalDigest());
        if (changed!=1) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
    }
    Optional<Receipt> find(BlobCommandAuthorization.Identity identity,Request request) {
        requireCompletion(); requireIdle(); validateSchema();
        var rows=query("SELECT * FROM teaql_blob_command WHERE scope_realm=? AND scope_boundary=? AND operation=? AND invocation_key=?",key(identity.scope(),request));
        if (rows.isEmpty()) return Optional.empty(); var row=one(rows); requireIdentity(row,identity,request); return Optional.of(receipt(row,identity.scope(),request));
    }
    private static void requireIdentity(Map<String,Object> row,BlobCommandAuthorization.Identity identity,Request request) {
        if (!identity.principalDigest().equals(row.get("principal_digest"))) throw new BlobException(BLOB_COMMAND_DENIED);
        if (!request.fingerprint().equals(row.get("request_digest"))) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
    }
    private static Receipt receipt(Map<String,Object> row,BlobScope scope,Request request) {
        try {
            return new Receipt(scope,request.operation(),request.invocationKey(),request.fingerprint(),Instant.ofEpochMilli(number(row,"completed_at")),BlobCommandReceiptJson.decode((String)row.get("receipt_json")));
        } catch (RuntimeException invalid) { throw new BlobException(BLOB_CATALOG_FAILED); }
    }
    private static Object[] key(BlobScope scope,Request request) { return new Object[]{scope.realm(),scope.boundary(),request.operation(),request.invocationKey()}; }
    private List<Map<String,Object>> query(String statement,Object... values) { return guarded(() -> sql.queryForList(statement,values)); }
    private int update(String statement,Object... values) { return guarded(() -> sql.update(statement,values)); }
    private void execute(String statement) { guarded(() -> { sql.execute(statement); return null; }); }
    private static <T> T guarded(Supplier<T> action) { try { return action.get(); } catch (BlobException known) { throw known; } catch (RuntimeException failed) { throw new BlobException(BLOB_CATALOG_FAILED); } }
    private static Map<String,Object> one(List<Map<String,Object>> rows) { if (rows.size()!=1) throw new BlobException(BLOB_CATALOG_FAILED); return rows.get(0); }
    private static long number(Map<String,Object> row,String field) { return ((Number)row.get(field)).longValue(); }
    private static BlobException schema() { return new BlobException(BLOB_SCHEMA_MISSING); }
    private static String migration() {
        try (var source=PostgresBlobCommandJournal.class.getResourceAsStream(MIGRATION)) {
            if (source==null) throw schema(); return new String(source.readAllBytes(),StandardCharsets.UTF_8);
        } catch (java.io.IOException failed) { throw schema(); }
    }
    private static String digest() {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(migration().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException failed) { throw schema(); }
    }
}
