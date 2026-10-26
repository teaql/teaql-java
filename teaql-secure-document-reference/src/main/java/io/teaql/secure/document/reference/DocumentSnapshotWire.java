package io.teaql.secure.document.reference;

import io.teaql.core.reference.DocumentSnapshot;
import io.teaql.core.reference.BoundDocumentSnapshot;
import io.teaql.core.reference.DocumentBlobBindingSnapshot;
import io.teaql.core.blob.BlobRef;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Exact tqd1 plaintext layout. Its version and bounds are independent of tqr1. */
final class DocumentSnapshotWire {
    private static final int MAX_TEXT = 16384;
    private DocumentSnapshotWire() {}

    record Decoded(byte[] actorFingerprint, long issuedAt, long expiresAt,
                   DocumentSnapshot snapshot) {}
    record DecodedBound(byte[] actorFingerprint, long issuedAt, long expiresAt,
                        BoundDocumentSnapshot document) {}

    static byte[] aad(String prefix, String keyId, String service,
                      String environment, String purpose) {
        var out = new ByteArrayOutputStream();
        text(out, prefix);
        text(out, keyId);
        text(out, service);
        text(out, environment);
        text(out, purpose);
        return out.toByteArray();
    }

    static byte[] encode(DocumentSnapshot snapshot, byte[] actorFingerprint,
                         long issuedAt, long expiresAt) {
        if (actorFingerprint.length != 32) throw invalid();
        var out = new ByteArrayOutputStream();
        out.write(1);
        out.writeBytes(actorFingerprint);
        longValue(out, issuedAt);
        longValue(out, expiresAt);
        text(out, snapshot.modelFingerprint());
        text(out, snapshot.businessId());
        scope(out, snapshot.scope());
        identity(out, snapshot.aggregate());
        count(out, snapshot.entities().size());
        for (var entity : snapshot.entities()) {
            text(out, entity.rowKey());
            identity(out, entity.identity());
            textSet(out, entity.loadedFields());
            textSet(out, entity.writableFields());
            mutations(out, entity.allowedMutations());
        }
        count(out, snapshot.relations().size());
        for (var relation : snapshot.relations()) {
            text(out, relation.name());
            out.write(relation.completeness() == DocumentSnapshot.Completeness.COMPLETE ? 1 : 2);
            out.write(relation.writable() ? 1 : 0);
            mutations(out, relation.allowedMutations());
            count(out, relation.rowKeys().size());
            relation.rowKeys().forEach(key -> text(out, key));
        }
        return out.toByteArray();
    }

    static byte[] encodeBound(BoundDocumentSnapshot document, byte[] actorFingerprint,
                              long issuedAt, long expiresAt) {
        byte[] base = encode(document.snapshot(), actorFingerprint, issuedAt, expiresAt);
        base[0] = 2;
        var out = new ByteArrayOutputStream();
        out.writeBytes(base);
        count(out, document.bindings().size());
        for (var binding : document.bindings()) {
            text(out, binding.rowKey());
            text(out, binding.canonicalField());
            text(out, binding.entityType());
            out.write(binding.reference() == null ? 0 : 1);
            if (binding.reference() != null) text(out, binding.reference().key());
            longValue(out, binding.bindingVersion());
            out.write(binding.nullable() ? 1 : 0);
            out.write(binding.writable() ? 1 : 0);
        }
        return out.toByteArray();
    }

    static Decoded decode(byte[] bytes) {
        try {
            var in = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            var decoded = decodeBody(in, 1);
            if (in.hasRemaining()) throw invalid();
            return decoded;
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (RuntimeException error) {
            throw invalid();
        }
    }

    static DecodedBound decodeBound(byte[] bytes) {
        try {
            var in = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            var decoded = decodeBody(in, 2);
            int bindingCount = count(in, 10000);
            var bindings = new ArrayList<DocumentBlobBindingSnapshot>(bindingCount);
            byte[] priorRow = null, priorField = null;
            for (int index = 0; index < bindingCount; index++) {
                String rowKey = text(in);
                String field = text(in);
                String entityType = text(in);
                byte[] rowBytes = rowKey.getBytes(StandardCharsets.UTF_8);
                byte[] fieldBytes = field.getBytes(StandardCharsets.UTF_8);
                if (priorRow != null) {
                    int comparison = compareUnsigned(priorRow, rowBytes);
                    if (comparison > 0 || comparison == 0
                            && compareUnsigned(priorField, fieldBytes) >= 0) throw invalid();
                }
                priorRow = rowBytes;
                priorField = fieldBytes;
                int hasKey = flag(in);
                BlobRef key = hasKey == 1 ? BlobRef.of(text(in)) : null;
                long version = in.getLong();
                boolean nullable = flag(in) == 1;
                boolean writable = flag(in) == 1;
                bindings.add(new DocumentBlobBindingSnapshot(
                        rowKey, field, entityType, key, version, nullable, writable));
            }
            if (in.hasRemaining()) throw invalid();
            return new DecodedBound(decoded.actorFingerprint(), decoded.issuedAt(),
                    decoded.expiresAt(), new BoundDocumentSnapshot(decoded.snapshot(), bindings));
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (RuntimeException error) {
            throw invalid();
        }
    }

    private static Decoded decodeBody(ByteBuffer in, int expectedVersion) {
        if (unsigned(in) != expectedVersion) throw invalid();
        byte[] actor = take(in, 32);
        long issued = in.getLong();
        long expires = in.getLong();
        String fingerprint = text(in);
        String businessId = text(in);
        var scope = scope(in);
        var aggregate = identity(in);
        int entityCount = count(in, 10000);
        var entities = new ArrayList<DocumentSnapshot.Entity>(entityCount);
        for (int index = 0; index < entityCount; index++) {
            entities.add(new DocumentSnapshot.Entity(
                    text(in), identity(in), textSet(in), textSet(in), mutations(in)));
        }
        int relationCount = count(in, 1024);
        var relations = new ArrayList<DocumentSnapshot.Relation>(relationCount);
        for (int index = 0; index < relationCount; index++) {
            String name = text(in);
            int kind = unsigned(in);
            if (kind != 1 && kind != 2) throw invalid();
            int flag = unsigned(in);
            if (flag != 0 && flag != 1) throw invalid();
            Set<DocumentSnapshot.MutationKind> allowed = mutations(in);
            int rowCount = count(in, 10000);
            var keys = new ArrayList<String>(rowCount);
            for (int row = 0; row < rowCount; row++) keys.add(text(in));
            relations.add(new DocumentSnapshot.Relation(name, keys,
                    kind == 1 ? DocumentSnapshot.Completeness.COMPLETE
                            : DocumentSnapshot.Completeness.PARTIAL,
                    flag == 1, allowed));
        }
        return new Decoded(actor, issued, expires,
                new DocumentSnapshot(fingerprint, businessId, scope, aggregate,
                        entities, relations));
    }

    private static int flag(ByteBuffer in) {
        int value = unsigned(in);
        if (value > 1) throw invalid();
        return value;
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        for (int index = 0; index < Math.min(left.length, right.length); index++) {
            int difference = Byte.toUnsignedInt(left[index]) - Byte.toUnsignedInt(right[index]);
            if (difference != 0) return difference;
        }
        return Integer.compare(left.length, right.length);
    }

    private static void scope(ByteArrayOutputStream out, ReferenceDocumentScope value) {
        text(out, value.documentId());
        text(out, value.purpose());
        text(out, value.aggregateType());
        longValue(out, value.aggregateId());
        longValue(out, value.aggregateRevision());
    }

    private static ReferenceDocumentScope scope(ByteBuffer in) {
        return new ReferenceDocumentScope(text(in), text(in), text(in),
                in.getLong(), in.getLong());
    }

    private static void identity(ByteArrayOutputStream out, InternalEntityIdentity value) {
        text(out, value.entityType());
        longValue(out, value.id());
        longValue(out, value.version());
    }

    private static InternalEntityIdentity identity(ByteBuffer in) {
        return new InternalEntityIdentity(text(in), in.getLong(), in.getLong());
    }

    private static void textSet(ByteArrayOutputStream out, Set<String> values) {
        count(out, values.size());
        new TreeSet<>(values).forEach(value -> text(out, value));
    }

    private static Set<String> textSet(ByteBuffer in) {
        int size = count(in, 1024);
        Set<String> values = new HashSet<>();
        for (int index = 0; index < size; index++) {
            if (!values.add(text(in))) throw invalid();
        }
        return values;
    }

    private static void mutations(ByteArrayOutputStream out,
                                  Set<DocumentSnapshot.MutationKind> values) {
        count(out, values.size());
        values.stream().sorted(Comparator.comparingInt(Enum::ordinal))
                .forEach(value -> out.write(value.ordinal() + 1));
    }

    private static Set<DocumentSnapshot.MutationKind> mutations(ByteBuffer in) {
        int size = count(in, 3);
        Set<DocumentSnapshot.MutationKind> values = new HashSet<>();
        var kinds = DocumentSnapshot.MutationKind.values();
        for (int index = 0; index < size; index++) {
            int wire = unsigned(in);
            if (wire < 1 || wire > kinds.length || !values.add(kinds[wire - 1])) throw invalid();
        }
        return values;
    }

    private static void text(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_TEXT) throw invalid();
        count(out, bytes.length);
        out.writeBytes(bytes);
    }

    private static String text(ByteBuffer in) {
        int size = count(in, MAX_TEXT);
        byte[] bytes = take(in, size);
        try {
            String value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (value.trim().isEmpty()) throw invalid();
            return value;
        } catch (CharacterCodingException error) {
            throw invalid();
        }
    }

    private static byte[] take(ByteBuffer in, int length) {
        if (length < 0 || in.remaining() < length) throw invalid();
        byte[] value = new byte[length];
        in.get(value);
        return value;
    }

    private static void count(ByteArrayOutputStream out, int value) {
        out.writeBytes(ByteBuffer.allocate(4).putInt(value).array());
    }

    private static int count(ByteBuffer in, int maximum) {
        int value = in.getInt();
        if (value < 0 || value > maximum) throw invalid();
        return value;
    }

    private static void longValue(ByteArrayOutputStream out, long value) {
        out.writeBytes(ByteBuffer.allocate(8).putLong(value).array());
    }

    private static int unsigned(ByteBuffer in) { return Byte.toUnsignedInt(in.get()); }

    private static RoundTripReferenceException invalid() {
        return new RoundTripReferenceException(
                RoundTripReferenceErrorCode.INVALID_REFERENCE, "ROUND_TRIP_REFERENCE_INVALID");
    }
}
