package io.teaql.core.view;

import java.time.Duration;
import java.util.Objects;

/** Trusted endpoint options. One instance identifies one presentation shared by nested renderers. */
public final class ViewOptions {
    public enum Mode { READ_ONLY, GOVERNED }
    public enum BusinessCompletion { QUERY, COMMITTED }
    private final Mode mode;
    private final BusinessCompletion completion;
    private final Duration readLifetime,cacheLifetime;
    private final boolean contextCheckedRead;
    public ViewOptions(Mode mode,BusinessCompletion completion,Duration readLifetime,Duration cacheLifetime,boolean contextCheckedRead) {
        this.mode=Objects.requireNonNull(mode); this.completion=Objects.requireNonNull(completion);
        if (readLifetime!=null && (readLifetime.isNegative() || readLifetime.isZero() || readLifetime.getNano()!=0)
                || cacheLifetime==null || cacheLifetime.isNegative() || cacheLifetime.getNano()!=0) throw new IllegalArgumentException("Invalid view lifetime");
        this.readLifetime=readLifetime; this.cacheLifetime=cacheLifetime; this.contextCheckedRead=contextCheckedRead;
    }
    public static ViewOptions readOnly() { return new ViewOptions(Mode.READ_ONLY,BusinessCompletion.QUERY,null,Duration.ZERO,false); }
    public static ViewOptions afterCommit() { return new ViewOptions(Mode.READ_ONLY,BusinessCompletion.COMMITTED,null,Duration.ZERO,false); }
    public Mode mode() { return mode; }
    public BusinessCompletion businessCompletion() { return completion; }
    public Duration readLifetime() { return readLifetime; }
    public Duration cacheLifetime() { return cacheLifetime; }
    public boolean requireContextCheckedRead() { return contextCheckedRead; }
}
