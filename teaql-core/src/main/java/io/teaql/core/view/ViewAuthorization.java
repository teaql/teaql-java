package io.teaql.core.view;

import io.teaql.core.UserContext;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Host-owned current policy. Field decisions run before reading a field value. */
public interface ViewAuthorization {
    record Identity(String realm,String subject,String boundary) {
        public Identity { text(realm); text(subject); text(boundary); }
        @Override public String toString() { return "ViewIdentity[redacted]"; }
    }
    record EntityTarget(String entity,Long id,Long revision) {
        public EntityTarget { text(entity); if (id!=null && id<=0 || revision!=null && revision==Long.MIN_VALUE) throw new IllegalArgumentException("Invalid view identity"); }
        @Override public String toString() { return "ViewEntityTarget[redacted]"; }
    }
    enum Projection { HIDDEN, VALUE, MASK }
    record FieldDecision(Projection projection,String mask,boolean writable,boolean completeRelation) {
        public FieldDecision {
            Objects.requireNonNull(projection);
            if (projection!=Projection.MASK && mask!=null || projection!=Projection.VALUE && (writable || completeRelation)
                    || mask!=null && mask.length()>8192) throw new IllegalArgumentException("Invalid field decision");
        }
        public static FieldDecision hide() { return new FieldDecision(Projection.HIDDEN,null,false,false); }
        public static FieldDecision show() { return new FieldDecision(Projection.VALUE,null,false,false); }
        public static FieldDecision mask(String replacement) { return new FieldDecision(Projection.MASK,replacement,false,false); }
        @Override public String toString() { return "ViewFieldDecision[projection="+projection+"]"; }
    }
    Identity identity(UserContext context);
    boolean canReadEntity(UserContext context,EntityTarget entity,String path);
    FieldDecision field(UserContext context,EntityTarget entity,String canonicalField,String path,ViewOptions.Mode mode);
    private static void text(String value) {
        if (value==null || value.isBlank() || value.getBytes(StandardCharsets.UTF_8).length>4096
                || value.codePoints().anyMatch(c -> Character.isISOControl(c) || c>=0xd800 && c<=0xdfff)) throw new IllegalArgumentException("Invalid view identity");
    }
}
