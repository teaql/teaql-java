package io.teaql.core.tools;

/**
 * Intermediate phase that forces the caller to declare the intent of the operation.
 * The terminal execute method is intentionally hidden until intent is provided.
 */
public interface HttpIntentPhase {
    
    /**
     * State the business purpose of this read operation.
     * @param purposeMessage the intent message
     * @return the executable tool
     */
    ExecutableHttpTool purpose(String purposeMessage);
    
    /**
     * Audit this mutation or outgoing action.
     * @param auditMessage the audit message
     * @return the executable tool
     */
    ExecutableHttpTool auditAs(String auditMessage);
}
