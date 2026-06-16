package io.teaql.core.tools;

/**
 * The terminal phase of an Agent tool call.
 * This class finally provides the execute() method.
 */
public interface ExecutableHttpTool {
    
    /**
     * Triggers the underlying HTTP tool, implicitly logging the audit trail first.
     * @return The string response from the HTTP call.
     */
    String execute();
}
