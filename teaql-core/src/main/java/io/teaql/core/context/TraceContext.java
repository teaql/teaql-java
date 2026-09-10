package io.teaql.core.context;

import io.teaql.core.ExecutionMetadata;
import io.teaql.core.TraceNode;
import java.util.List;

/**
 * Trace and audit context interface.
 * Provides tracing and execution metadata recording capabilities.
 */
public interface TraceContext {
    
    /**
     * Push a trace comment onto the trace chain.
     */
    void pushTrace(String comment);
    
    /**
     * Get the current trace chain.
     */
    List<TraceNode> getTraceChain();
    
    /**
     * Pop the last trace comment from the trace chain.
     */
    void popTrace();
    
    /**
     * Record execution metadata for auditing and logging.
     */
    void recordExecutionMetadata(ExecutionMetadata metadata);
}
