package io.teaql.blob.reference;

import io.teaql.core.FrameworkInternal;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobPolicyProfile.Sharing;
import io.teaql.dataservice.sql.SqlExecutionAdapter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import static io.teaql.core.blob.BlobException.Code.*;

/** PostgreSQL catalog. Binding operations borrow the business adapter and never own its commit. */
public final class PostgresBlobCatalog {
    private static final String MIGRATION = "/io/teaql/blob/reference/postgres-catalog-v1.sql";
    private final SqlExecutionAdapter sql;

    /** Passive construction. Schema migration and readiness are explicit host operations. */
    public PostgresBlobCatalog(SqlExecutionAdapter sql) { this.sql = Objects.requireNonNull(sql); }

    public void requireResource(Object resource) {
        if (resource != sql) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
    }
    Object requireActive(Object resource) {
        requireResource(resource);
        Object identity = sql.activeTransactionIdentity();
        if (!sql.hasActiveTransaction() || identity == null) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        return identity;
    }
    void requireActive(Object resource, Object transaction) {
        if (requireActive(resource) != transaction) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
    }

    /** Fixed infrastructure migration, separate from model ensureSchema and from installation. */
    public void migrate() {
        if (sql.hasActiveTransaction()) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        String migration = migration(); String digest = digest(migration);
        transaction(() -> {
            query("SELECT pg_advisory_xact_lock(73402110318407)");
            sql.execute("CREATE TABLE IF NOT EXISTS teaql_blob_schema (id INTEGER PRIMARY KEY CHECK (id = 1), "
                    + "schema_version INTEGER NOT NULL, schema_digest VARCHAR(64) NOT NULL)");
            update("INSERT INTO teaql_blob_schema VALUES (1,0,'pending') ON CONFLICT (id) DO NOTHING");
            Map<String,Object> row = one(query("SELECT schema_version, schema_digest FROM teaql_blob_schema WHERE id=1 FOR UPDATE"));
            long version = number(row,"schema_version");
            if (version == 0) {
                for (String statement : migration.split(";")) if (!statement.isBlank()) sql.execute(statement);
                update("UPDATE teaql_blob_schema SET schema_version=1, schema_digest=? WHERE id=1", digest);
            } else if (version != 1 || !digest.equals(row.get("schema_digest"))) throw new BlobException(BLOB_SCHEMA_MISSING);
            validateSchema();
            return null;
        });
    }

    /** Reads/checks only. Unknown versions, partial schemas and unreadable catalogs fail closed. */
    public void validateSchema() {
        try {
            Map<String,Object> row = one(query("SELECT schema_version, schema_digest FROM teaql_blob_schema WHERE id=1"));
            if (number(row,"schema_version") != 1 || !digest(migration()).equals(row.get("schema_digest"))) {
                throw new BlobException(BLOB_SCHEMA_MISSING);
            }
            query("SELECT blob_key,scope_realm,scope_boundary,upload_id,request_key,request_digest,state,original_name,extension,actual_mime,"
                    + "size_bytes,sha256,content_version,storage_location,target_entity,target_id,target_field,policy_json,created_at,"
                    + "upload_expires_at,completed_at,first_bound_at,unreferenced_at,retention_floor_at,effective_unref_seconds,"
                    + "single_binding,deleted_at FROM teaql_blob_object WHERE FALSE");
            query("SELECT scope_realm,scope_boundary,entity_type,entity_id,field_name,blob_key,retired_key,binding_version,bound_at,retain_until "
                    + "FROM teaql_blob_binding WHERE FALSE");
            Map<String,String> required=new HashMap<>(Map.of(
                    "teaql_blob_object_pk","PRIMARY KEY (blob_key)",
                    "teaql_blob_upload_uk","UNIQUE (upload_id)",
                    "teaql_blob_request_uk","UNIQUE (scope_realm, scope_boundary, request_key)",
                    "teaql_blob_binding_pk","PRIMARY KEY (scope_realm, scope_boundary, entity_type, entity_id, field_name)",
                    "teaql_blob_binding_fk","FOREIGN KEY (blob_key) REFERENCES teaql_blob_object(blob_key)",
                    "teaql_blob_retired_fk","FOREIGN KEY (retired_key) REFERENCES teaql_blob_object(blob_key)"));
            for (var constraint:query("SELECT conname,convalidated,pg_get_constraintdef(oid) AS definition FROM pg_constraint "
                    + "WHERE conrelid IN ('teaql_blob_object'::regclass,'teaql_blob_binding'::regclass)")) {
                String name=(String)constraint.get("conname");
                if (required.containsKey(name)) {
                    if (!Boolean.TRUE.equals(constraint.get("convalidated")) || !required.get(name).equals(constraint.get("definition"))) {
                        throw new BlobException(BLOB_SCHEMA_MISSING);
                    }
                    required.remove(name);
                }
            }
            if (!required.isEmpty()) throw new BlobException(BLOB_SCHEMA_MISSING);
        } catch (RuntimeException unavailable) { throw new BlobException(BLOB_SCHEMA_MISSING); }
    }

    /** Candidate identifiers/locations are generated by the trusted upload service, never the client. */
    public record UploadDraft(BlobScope scope, BlobRef reference, String uploadId, String idempotencyKey,
            String requestDigest, String originalName, String extension, String storageLocation,
            BlobFieldTarget target, BlobPolicySnapshot policy) {
        public UploadDraft {
            Objects.requireNonNull(scope); Objects.requireNonNull(reference); Objects.requireNonNull(target); Objects.requireNonNull(policy);
            text(uploadId,128); text(idempotencyKey,128); text(originalName,512); text(storageLocation,2048);
            if (requestDigest == null || !requestDigest.matches("[0-9a-f]{64}")
                    || extension == null || !policy.field().allowedExtensions().contains(extension)
                    || !target.field().equals(policy.field().field())) throw new BlobException(BLOB_REFERENCE_INVALID);
        }
        @Override public String toString() { return "UploadDraft[redacted]"; }
    }
    public record VerifiedContent(String contentType, long sizeBytes, String sha256, String contentVersion) {
        public VerifiedContent {
            text(contentType,160); text(contentVersion,1024);
            if (sizeBytes < 0 || sha256 == null || !sha256.matches("[0-9a-f]{64}")) throw new BlobException(BLOB_REFERENCE_INVALID);
        }
        @Override public String toString() { return "VerifiedContent[redacted]"; }
    }

    @FrameworkInternal("Trusted upload orchestration only; request digest must describe canonical caller parameters")
    public BlobCatalogRecord begin(UploadDraft draft, Instant now) {
        return transaction(() -> {
            validateSchema();
            update("INSERT INTO teaql_blob_object (blob_key,scope_realm,scope_boundary,upload_id,request_key,request_digest,state,"
                    + "original_name,extension,storage_location,target_entity,target_id,target_field,policy_json,created_at,upload_expires_at,"
                    + "effective_unref_seconds,single_binding) VALUES (?,?,?,?,?,?,'initiated',?,?,?,?,?,?,?,?,?,?,?) "
                    + "ON CONFLICT (scope_realm,scope_boundary,request_key) DO NOTHING",
                    draft.reference().key(),draft.scope().realm(),draft.scope().boundary(),draft.uploadId(),draft.idempotencyKey(),draft.requestDigest(),
                    draft.originalName(),draft.extension(),draft.storageLocation(),draft.target().entity(),draft.target().entityId(),draft.target().field(),
                    BlobPolicyJson.encode(draft.policy()),millis(now),millis(plus(now,draft.policy().uploadSessionTtl())),
                    draft.policy().unreferencedRetention().getSeconds(),draft.policy().sharing()==Sharing.SINGLE_BINDING?1:0);
            Map<String,Object> row = one(query("SELECT * FROM teaql_blob_object WHERE scope_realm=? AND scope_boundary=? AND request_key=? FOR UPDATE",
                    draft.scope().realm(),draft.scope().boundary(),draft.idempotencyKey()));
            if (!draft.requestDigest().equals(row.get("request_digest"))) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
            return record(row);
        });
    }

    @FrameworkInternal("Only after the upload service has verified actual immutable bytes and provider version")
    public BlobCatalogRecord complete(BlobScope scope, BlobRef reference, VerifiedContent content, Instant now) {
        return transaction(() -> {
            validateSchema(); BlobCatalogRecord blob = lockBlob(scope,reference);
            if (blob.state() == BlobState.DELETING || blob.state() == BlobState.DELETED) throw new BlobException(BLOB_UPLOAD_EXPIRED);
            if (blob.state() != BlobState.INITIATED) {
                if (!Objects.equals(blob.contentType(),content.contentType()) || !Objects.equals(blob.sizeBytes(),content.sizeBytes())
                        || !Objects.equals(blob.sha256(),content.sha256()) || !Objects.equals(blob.contentVersion(),content.contentVersion())) {
                    throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
                }
                return blob;
            }
            if (!now.isBefore(blob.uploadExpiresAt())) throw new BlobException(BLOB_UPLOAD_EXPIRED);
            String expectedMime = BlobFieldSpec.of("Blob","content",List.of(blob.extension()),true,"default").allowedContentTypes().first();
            if (!expectedMime.equals(content.contentType())) throw new BlobException(BLOB_FORMAT_NOT_ALLOWED);
            if (content.sizeBytes() > blob.policy().maxSizeBytes() || content.sizeBytes()==0 && !blob.policy().allowEmpty()) {
                throw new BlobException(BLOB_SIZE_EXCEEDED);
            }
            update("UPDATE teaql_blob_object SET state='uploaded_unbound',actual_mime=?,size_bytes=?,sha256=?,content_version=?,completed_at=? WHERE blob_key=?",
                    content.contentType(),content.sizeBytes(),content.sha256(),content.contentVersion(),millis(now),reference.key());
            return lockBlob(scope,reference);
        });
    }

    public Optional<BlobCatalogRecord> find(BlobScope scope, BlobRef reference) {
        return guarded(() -> {
            validateSchema();
            var rows=query("SELECT * FROM teaql_blob_object WHERE blob_key=? AND scope_realm=? AND scope_boundary=?",
                    reference.key(),scope.realm(),scope.boundary());
            return rows.isEmpty()?Optional.empty():Optional.of(record(one(rows)));
        });
    }

    public record Binding(BlobScope scope, BlobFieldTarget target, BlobRef reference, BlobRef retiredReference,
                          long version, Instant boundAt, Instant retainUntil) {}
    public Optional<Binding> binding(BlobScope scope, BlobFieldTarget target) {
        return guarded(() -> {
            validateSchema();
            var rows=query("SELECT * FROM teaql_blob_binding WHERE scope_realm=? AND scope_boundary=? AND entity_type=? AND entity_id=? AND field_name=?",
                    targetArgs(scope,target));
            return rows.isEmpty()?Optional.empty():Optional.of(binding(one(rows)));
        });
    }

    BlobCatalogRecord lockBlob(BlobScope scope, BlobRef key) {
        requireActive(sql);
        var rows=query("SELECT * FROM teaql_blob_object WHERE blob_key=? AND scope_realm=? AND scope_boundary=? FOR UPDATE",
                key.key(),scope.realm(),scope.boundary());
        if (rows.isEmpty()) throw new BlobException(BLOB_BIND_DENIED);
        return record(one(rows));
    }
    Binding lockBinding(BlobScope scope, BlobFieldTarget target) {
        requireActive(sql);
        if (target.entityId()==null) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
        update("INSERT INTO teaql_blob_binding (scope_realm,scope_boundary,entity_type,entity_id,field_name,binding_version) "
                + "VALUES (?,?,?,?,?,0) ON CONFLICT (scope_realm,scope_boundary,entity_type,entity_id,field_name) DO NOTHING",targetArgs(scope,target));
        return binding(one(query("SELECT * FROM teaql_blob_binding WHERE scope_realm=? AND scope_boundary=? AND entity_type=? AND entity_id=? AND field_name=? FOR UPDATE",
                targetArgs(scope,target))));
    }
    long bindingCount(BlobRef key) {
        return number(one(query("SELECT count(*) AS binding_count FROM teaql_blob_binding WHERE blob_key=?",key.key())),"binding_count");
    }
    void writeBinding(Binding previous, BlobRef next, BlobRef retired, long version, Instant boundAt, Instant retainUntil) {
        requireActive(sql);
        BlobScope scope=previous.scope(); BlobFieldTarget target=previous.target();
        int changed=update("UPDATE teaql_blob_binding SET blob_key=?,retired_key=?,binding_version=?,bound_at=?,retain_until=? "
                + "WHERE scope_realm=? AND scope_boundary=? AND entity_type=? AND entity_id=? AND field_name=? AND binding_version=?",
                key(next),key(retired),version,nullableMillis(boundAt),nullableMillis(retainUntil),scope.realm(),scope.boundary(),target.entity(),target.entityId(),target.field(),previous.version());
        if (changed!=1) throw new BlobException(BLOB_IDEMPOTENCY_CONFLICT);
    }
    void acceptPolicy(BlobCatalogRecord blob, BlobPolicySnapshot policy, Instant retainUntil) {
        requireActive(sql);
        update("UPDATE teaql_blob_object SET retention_floor_at=CASE WHEN retention_floor_at IS NULL OR retention_floor_at<? THEN ? ELSE retention_floor_at END,"
                + "effective_unref_seconds=GREATEST(effective_unref_seconds,?),single_binding=GREATEST(single_binding,?) WHERE blob_key=?",
                millis(retainUntil),millis(retainUntil),policy.unreferencedRetention().getSeconds(),policy.sharing()==Sharing.SINGLE_BINDING?1:0,blob.reference().key());
    }
    void refreshReferences(BlobScope scope, BlobRef key, Instant now) {
        BlobCatalogRecord blob=lockBlob(scope,key);
        long count=bindingCount(key);
        if (count>0) update("UPDATE teaql_blob_object SET state='available',first_bound_at=COALESCE(first_bound_at,?),unreferenced_at=NULL WHERE blob_key=?",
                millis(now),key.key());
        else if (blob.state()==BlobState.AVAILABLE && blob.unreferencedAt()==null) {
            update("UPDATE teaql_blob_object SET unreferenced_at=? WHERE blob_key=?",millis(now),key.key());
        }
    }

    /** Atomic logical claim only; the maintenance service must subsequently perform version-conditional storage deletion. */
    @FrameworkInternal("Maintenance only; not a physical deletion acknowledgement")
    public Optional<BlobCatalogRecord> claimDeletion(BlobScope scope, BlobRef key, Instant now) {
        return transaction(() -> {
            validateSchema(); BlobCatalogRecord blob=lockBlob(scope,key);
            Instant eligible=blob.unreferencedAt()==null?null:BlobRetention.unreferencedEligibleAt(blob.unreferencedAt(),
                    Duration.ofSeconds(blob.effectiveUnreferencedSeconds()),blob.retentionFloorUntil());
            if (!BlobRetention.collectable(blob.state(),bindingCount(key),now,blob.uploadExpiresAt(),blob.completedAt(),blob.policy().unboundRetention(),eligible)) {
                return Optional.empty();
            }
            update("UPDATE teaql_blob_object SET state='deleting' WHERE blob_key=?",key.key());
            return Optional.of(lockBlob(scope,key));
        });
    }

    private <T> T transaction(Supplier<T> operation) {
        return guarded(() -> {
            List<T> result=new ArrayList<>(1);
            sql.executeInTransaction(() -> { requireActive(sql); result.add(operation.get()); });
            if (result.size()!=1) throw new BlobException(BLOB_TRANSACTION_REQUIRED);
            return result.get(0);
        });
    }
    <T> T guarded(Supplier<T> operation) {
        try { return operation.get(); }
        catch (BlobException known) { throw known; }
        catch (RuntimeException unavailable) { throw new BlobException(BLOB_CATALOG_FAILED); }
    }
    private List<Map<String,Object>> query(String statement,Object... args) { return sql.queryForList(statement,args); }
    private int update(String statement,Object... args) { return sql.update(statement,args); }
    private static Object[] targetArgs(BlobScope scope,BlobFieldTarget target) {
        return new Object[]{scope.realm(),scope.boundary(),target.entity(),target.entityId(),target.field()};
    }
    private static String key(BlobRef value) { return value==null?null:value.key(); }
    private static BlobRef reference(Object value) { return value==null?null:BlobRef.of((String)value); }
    private static Map<String,Object> one(List<Map<String,Object>> rows) {
        if (rows.size()!=1) throw new BlobException(BLOB_CATALOG_FAILED); return rows.get(0);
    }
    private static long number(Map<String,Object> row,String key) { return ((Number)row.get(key)).longValue(); }
    private static Instant instant(Map<String,Object> row,String key) { return row.get(key)==null?null:Instant.ofEpochMilli(number(row,key)); }
    static Instant plus(Instant base,Duration duration) {
        try { Instant value=base.plus(duration); value.toEpochMilli(); return value; }
        catch (java.time.DateTimeException | ArithmeticException invalid) { throw new BlobException(BLOB_POLICY_INVALID); }
    }
    private static long millis(Instant instant) { return Objects.requireNonNull(instant).toEpochMilli(); }
    private static Long nullableMillis(Instant instant) { return instant==null?null:millis(instant); }
    private static void text(String value,int maxBytes) {
        if (value==null || value.isBlank() || value.indexOf('\0')>=0 || value.getBytes(StandardCharsets.UTF_8).length>maxBytes) {
            throw new BlobException(BLOB_REFERENCE_INVALID);
        }
    }
    private static Binding binding(Map<String,Object> row) {
        return new Binding(new BlobScope((String)row.get("scope_realm"),(String)row.get("scope_boundary")),
                new BlobFieldTarget((String)row.get("entity_type"),number(row,"entity_id"),(String)row.get("field_name")),
                reference(row.get("blob_key")),reference(row.get("retired_key")),number(row,"binding_version"),instant(row,"bound_at"),instant(row,"retain_until"));
    }
    private static BlobCatalogRecord record(Map<String,Object> row) {
        Long targetId=row.get("target_id")==null?null:number(row,"target_id");
        return new BlobCatalogRecord(BlobRef.of((String)row.get("blob_key")),new BlobScope((String)row.get("scope_realm"),(String)row.get("scope_boundary")),
                BlobState.valueOf(((String)row.get("state")).toUpperCase(Locale.ROOT)),(String)row.get("upload_id"),(String)row.get("original_name"),
                (String)row.get("extension"),(String)row.get("actual_mime"),row.get("size_bytes")==null?null:number(row,"size_bytes"),
                (String)row.get("sha256"),(String)row.get("content_version"),(String)row.get("storage_location"),
                new BlobFieldTarget((String)row.get("target_entity"),targetId,(String)row.get("target_field")),BlobPolicyJson.decode((String)row.get("policy_json")),
                instant(row,"created_at"),instant(row,"upload_expires_at"),instant(row,"completed_at"),instant(row,"first_bound_at"),
                instant(row,"unreferenced_at"),instant(row,"retention_floor_at"),number(row,"effective_unref_seconds"),number(row,"single_binding")==1,instant(row,"deleted_at"));
    }
    private static String migration() {
        try (var input=PostgresBlobCatalog.class.getResourceAsStream(MIGRATION)) {
            if (input==null) throw new BlobException(BLOB_SCHEMA_MISSING);
            return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        } catch (java.io.IOException unavailable) { throw new BlobException(BLOB_SCHEMA_MISSING); }
    }
    private static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
