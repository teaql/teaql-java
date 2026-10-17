package io.teaql.tools.http;

public interface AgentHttpTool {

    HttpIntentPhase get(String profile, String relativePath);

    HttpIntentPhase post(String profile, String relativePath, Object body);

    /** Legacy shape; accepts only profile/relative-path, never an absolute URL. */
    HttpIntentPhase get(String url);

    /** Legacy shape; accepts only profile/relative-path, never an absolute URL. */
    HttpIntentPhase post(String url, Object body);
}
