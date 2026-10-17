package io.teaql.tools.http;

public interface HttpIntentPhase {

    HttpCommentPhase comment(String comment);

    /** A comment is required before execution; use comment(...).purpose(...). */
    ExecutableHttpTool purpose(String purposeMessage);

    /** A comment is required before execution; use comment(...).auditAs(...). */
    ExecutableHttpTool auditAs(String auditMessage);
}
