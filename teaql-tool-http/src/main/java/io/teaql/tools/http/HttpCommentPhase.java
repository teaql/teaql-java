package io.teaql.tools.http;

public interface HttpCommentPhase {
    ExecutableHttpTool purpose(String purposeMessage);
    ExecutableHttpTool auditAs(String auditMessage);
}
