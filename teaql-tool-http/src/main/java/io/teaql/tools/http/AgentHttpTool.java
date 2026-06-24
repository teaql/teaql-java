package io.teaql.tools.http;

public interface AgentHttpTool {

    HttpIntentPhase get(String url);

    HttpIntentPhase post(String url, Object body);
}
