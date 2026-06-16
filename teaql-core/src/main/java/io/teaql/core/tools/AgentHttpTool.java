package io.teaql.core.tools;

/**
 * Fluent Builder interface for an Agent to execute HTTP actions securely.
 * This capability sandbox forces intent and audit trails before execution.
 */
public interface AgentHttpTool {
    
    /**
     * Start an HTTP GET request.
     * @param url the target URL
     * @return the intent phase requiring a purpose/audit clarification.
     */
    HttpIntentPhase get(String url);
    
    /**
     * Start an HTTP POST request.
     * @param url the target URL
     * @param body the request body
     * @return the intent phase requiring a purpose/audit clarification.
     */
    HttpIntentPhase post(String url, Object body);
}
