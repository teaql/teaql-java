package io.teaql.core.blob;

import io.teaql.core.UserContext;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Optional durable command boundary. Identity and permission always come from the current context. */
public interface BlobCommandService {
    String CAPABILITY="blob.command.v1";
    final class Request {
        private final String operation,key,fingerprint;
        private final byte[] input;
        public Request(String operation,String invocationKey,byte[] canonicalInput) {
            if (operation==null || !operation.matches("[A-Za-z][A-Za-z0-9_.:/-]{0,127}") || invocationKey==null
                    || !invocationKey.matches("[A-Za-z0-9_-]{1,128}") || canonicalInput==null || canonicalInput.length==0 || canonicalInput.length>1048576) throw invalid();
            this.operation=operation; key=invocationKey; input=canonicalInput.clone();
            try {
                MessageDigest digest=MessageDigest.getInstance("SHA-256"); digest.update("teaql.blob.command.request/v1\0".getBytes(StandardCharsets.UTF_8));
                byte[] name=operation.getBytes(StandardCharsets.UTF_8); digest.update(ByteBuffer.allocate(4).putInt(name.length).array()); digest.update(name);
                digest.update(ByteBuffer.allocate(4).putInt(input.length).array()); digest.update(input); fingerprint=HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException unavailable) { throw new IllegalStateException("SHA-256 unavailable",unavailable); }
        }
        public String operation() { return operation; }
        public String invocationKey() { return key; }
        public String fingerprint() { return fingerprint; }
        public byte[] canonicalInput() { return input.clone(); }
        @Override public String toString() { return "BlobCommandRequest[redacted]"; }
    }
    record EntityRevision(String entity,long id,long revision) {
        public EntityRevision {
            if (entity==null || !entity.matches("[A-Za-z][A-Za-z0-9_]{0,127}") || id<=0 || revision==0 || revision==Long.MIN_VALUE) throw invalid();
        }
        @Override public String toString() { return "BlobCommandEntityRevision[redacted]"; }
    }
    record Receipt(BlobScope scope,String operation,String invocationKey,String fingerprint,Instant completedAt,List<EntityRevision> entities) {
        public Receipt {
            if (scope==null || completedAt==null) throw invalid();
            if (operation==null || !operation.matches("[A-Za-z][A-Za-z0-9_.:/-]{0,127}") || invocationKey==null || !invocationKey.matches("[A-Za-z0-9_-]{1,128}")
                    || fingerprint==null || !fingerprint.matches("[0-9a-f]{64}") || entities==null || entities.size()>1000 || completedAt.getEpochSecond()<0 || completedAt.getEpochSecond()>Long.MAX_VALUE/1000-1 || completedAt.getNano()%1000000!=0) throw invalid();
            List<EntityRevision> sorted=new ArrayList<>(entities); if (sorted.contains(null)) throw invalid(); sorted.sort(Comparator.comparing(EntityRevision::entity).thenComparingLong(EntityRevision::id));
            Set<String> keys=new HashSet<>(); for (var entity:sorted) if (!keys.add(entity.entity()+":"+entity.id())) throw invalid();
            entities=List.copyOf(sorted);
        }
        @Override public String toString() { return "BlobCommandReceipt[redacted]"; }
    }
    record Execution(Receipt receipt,boolean replayed) {
        public Execution { Objects.requireNonNull(receipt); }
        @Override public String toString() { return "BlobCommandExecution[replayed="+replayed+"]"; }
    }
    @FunctionalInterface interface Action { List<EntityRevision> run(); }
    /** Owns a synchronous outer transaction; canonical input must come from trusted versioned command code. */
    Execution execute(UserContext context,Request request,Action action);
    /** Independent committed read. Empty is not rollback proof while an earlier owner may still be in flight. */
    Optional<Receipt> resolve(UserContext context,Request request);
    private static BlobException invalid() { return new BlobException(BlobException.Code.BLOB_COMMAND_INVALID); }
}
