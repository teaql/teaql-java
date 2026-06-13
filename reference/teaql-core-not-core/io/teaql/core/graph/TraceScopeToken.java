package io.teaql.core.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TraceScopeToken {
    private final TraceScopeToken parent;
    private final TraceNode track;
    private final int nodeIndex;

    public TraceScopeToken(TraceScopeToken parent, TraceNode track, int nodeIndex) {
        this.parent = parent;
        this.track = track;
        this.nodeIndex = nodeIndex;
    }

    public TraceScopeToken getParent() {
        return parent;
    }

    public TraceNode getTrack() {
        return track;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public List<TraceNode> recoverTraceChain() {
        List<TraceNode> chain = new ArrayList<>();
        TraceScopeToken current = this;
        while (current != null) {
            chain.add(current.track);
            current = current.parent;
        }
        Collections.reverse(chain);
        return chain;
    }
}
