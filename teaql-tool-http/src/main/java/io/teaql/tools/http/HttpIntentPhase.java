package io.teaql.tools.http;

public interface HttpIntentPhase {

    ExecutableHttpTool purpose(String purposeMessage);

    ExecutableHttpTool auditAs(String auditMessage);
}
