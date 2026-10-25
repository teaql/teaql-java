package io.teaql.blob.reference;

import io.teaql.core.UserContext;
import io.teaql.core.blob.*;
import io.teaql.core.blob.BlobCommandService.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import static io.teaql.core.blob.BlobException.Code.*;

/** Trusted current identity plus separate execute/replay policies. No actor is cached by the service. */
public final class BlobCommandAuthorization {
    @FunctionalInterface public interface PrincipalProvider { String principal(UserContext context); }
    @FunctionalInterface public interface ExecutePermission { boolean canExecute(UserContext context,Request request); }
    @FunctionalInterface public interface ReplayPermission { boolean canReplay(UserContext context,Receipt receipt); }
    record Identity(BlobScope scope,String principalDigest) {}
    private final BlobBindingAuthorization.ScopeProvider scopes;
    private final PrincipalProvider principals;
    private final ExecutePermission execute;
    private final ReplayPermission replay;
    public BlobCommandAuthorization(BlobBindingAuthorization.ScopeProvider scopes,PrincipalProvider principals,ExecutePermission execute,ReplayPermission replay) {
        this.scopes=Objects.requireNonNull(scopes); this.principals=Objects.requireNonNull(principals); this.execute=Objects.requireNonNull(execute); this.replay=Objects.requireNonNull(replay);
    }
    Identity identity(UserContext context) {
        try {
            BlobScope scope=scopes.scope(Objects.requireNonNull(context)); String principal=principals.principal(context);
            if (scope==null || principal==null || principal.isBlank() || principal.getBytes(StandardCharsets.UTF_8).length>256
                    || principal.codePoints().anyMatch(value -> Character.isISOControl(value) || value>=0xd800 && value<=0xdfff)) throw denied();
            var digest=MessageDigest.getInstance("SHA-256"); digest.update("teaql.blob.command.principal/v1\0".getBytes(StandardCharsets.UTF_8));
            return new Identity(scope,HexFormat.of().formatHex(digest.digest(principal.getBytes(StandardCharsets.UTF_8))));
        } catch (RuntimeException | NoSuchAlgorithmException failed) { throw denied(); }
    }
    void requireSame(UserContext context,Identity expected) { if (!expected.equals(identity(context))) throw denied(); }
    void requireExecute(UserContext context,Request request) {
        try { if (!execute.canExecute(context,request)) throw denied(); } catch (RuntimeException failed) { throw denied(); }
    }
    void requireReplay(UserContext context,Receipt receipt) {
        try { if (!replay.canReplay(context,receipt)) throw denied(); } catch (RuntimeException failed) { throw denied(); }
    }
    private static BlobException denied() { return new BlobException(BLOB_COMMAND_DENIED); }
}
