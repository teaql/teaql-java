package io.teaql.secure.document.reference;

import io.teaql.core.blob.BlobRef;
import io.teaql.core.reference.BlobSelectionClaim;
import io.teaql.core.reference.BlobSelectionTarget;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.RoundTripReferenceErrorCode;
import io.teaql.core.reference.RoundTripReferenceException;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Exact tqs1 plaintext; no URL, business row ID or mutable storage locator appears here. */
final class BlobSelectionWire {
    private BlobSelectionWire() {}

    record Decoded(byte[] actorFingerprint, long issuedAt, long expiresAt,
                   BlobSelectionClaim claim) {}

    static byte[] encode(BlobSelectionClaim claim, byte[] actorFingerprint,
                         long issuedAt, long expiresAt) {
        if (actorFingerprint.length != 32) throw invalid();
        var target = claim.target();
        var scope = target.scope();
        var out = new ByteArrayOutputStream();
        out.write(1);
        out.writeBytes(actorFingerprint);
        integer(out, issuedAt);
        integer(out, expiresAt);
        text(out, scope.documentId());
        text(out, scope.purpose());
        text(out, scope.aggregateType());
        integer(out, scope.aggregateId());
        integer(out, scope.aggregateRevision());
        text(out, target.targetEntity());
        text(out, target.canonicalField());
        out.write(target.kind() == BlobSelectionTarget.Kind.EXISTING_ROW ? 1 : 2);
        text(out, target.targetKey());
        text(out, claim.reference().key());
        text(out, target.commandId());
        return out.toByteArray();
    }

    static Decoded decode(byte[] bytes) {
        try {
            var in = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
            if (Byte.toUnsignedInt(in.get()) != 1) throw invalid();
            byte[] actor = new byte[32];
            in.get(actor);
            long issued = in.getLong();
            long expires = in.getLong();
            var scope = new ReferenceDocumentScope(
                    text(in), text(in), text(in), in.getLong(), in.getLong());
            String entity = text(in);
            String field = text(in);
            int kind = Byte.toUnsignedInt(in.get());
            if (kind != 1 && kind != 2) throw invalid();
            String targetKey = text(in);
            BlobRef reference = BlobRef.of(text(in));
            String commandId = text(in);
            if (in.hasRemaining()) throw invalid();
            var target = new BlobSelectionTarget(scope, entity, field,
                    kind == 1 ? BlobSelectionTarget.Kind.EXISTING_ROW
                            : BlobSelectionTarget.Kind.NEW_CLIENT_KEY,
                    targetKey, commandId);
            return new Decoded(actor, issued, expires,
                    new BlobSelectionClaim(target, reference));
        } catch (RoundTripReferenceException error) {
            throw error;
        } catch (RuntimeException error) {
            throw invalid();
        }
    }

    private static void text(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > 16384) throw invalid();
        out.writeBytes(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.writeBytes(bytes);
    }

    private static String text(ByteBuffer in) {
        int count = in.getInt();
        if (count <= 0 || count > 16384 || count > in.remaining()) throw invalid();
        byte[] bytes = new byte[count];
        in.get(bytes);
        try {
            String value = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (value.isBlank() || !value.equals(value.trim())) throw invalid();
            return value;
        } catch (CharacterCodingException error) {
            throw invalid();
        }
    }

    private static void integer(ByteArrayOutputStream out, long value) {
        out.writeBytes(ByteBuffer.allocate(8).putLong(value).array());
    }

    private static RoundTripReferenceException invalid() {
        return new RoundTripReferenceException(
                RoundTripReferenceErrorCode.INVALID_REFERENCE, "ROUND_TRIP_REFERENCE_INVALID");
    }
}
