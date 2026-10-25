package io.teaql.core.view;

/** Contains no business values, credentials or provider exception messages. */
public final class ViewPreparationException extends RuntimeException {
    public enum Code { VIEW_CAPABILITY_MISSING, VIEW_ACCESS_DENIED, VIEW_TRANSACTION_UNFINISHED, VIEW_RESULT_UNSUPPORTED,
        VIEW_LIMIT_EXCEEDED, VIEW_PREPARED_MISMATCH, VIEW_PREPARATION_FAILED, VIEW_EXPIRED }
    private final Code code;
    private final ViewOptions.BusinessCompletion completion;
    public ViewPreparationException(Code code,ViewOptions.BusinessCompletion completion) {
        super(java.util.Objects.requireNonNull(code).name()); this.code=code; this.completion=java.util.Objects.requireNonNull(completion);
    }
    public Code code() { return code; }
    public ViewOptions.BusinessCompletion businessCompletion() { return completion; }
    /** A rendering failure never authorizes mutation replay. Re-read committed state instead. */
    public boolean businessCommitted() { return completion==ViewOptions.BusinessCompletion.COMMITTED; }
}
