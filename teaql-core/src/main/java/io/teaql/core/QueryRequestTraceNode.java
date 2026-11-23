package io.teaql.core;

import java.util.*;

/** Detached invocation-local diagnostics, never a writable request ownership API. */
public record QueryRequestTraceNode(int id, Integer parentId, String kind, String name,
        String detail, @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) String method) {
    public static List<QueryRequestTraceNode> fromScopes(List<QueryRequestScope> scopes) {
        if (scopes.size() < 2) return List.of();
        var distinct = Collections.newSetFromMap(new IdentityHashMap<QueryRequestScope, Boolean>());
        distinct.addAll(scopes);
        if (distinct.size() < 2) return List.of();
        var indices = new IdentityHashMap<QueryRequestScope, Integer>();
        var nodes = new ArrayList<QueryRequestTraceNode>();
        for (var leaf : scopes) {
            var ancestry = new ArrayList<QueryRequestScope>();
            for (var current = leaf; current != null; current = current.parentRequest()) ancestry.add(current);
            Collections.reverse(ancestry);
            for (var scope : ancestry) {
                if (indices.containsKey(scope)) continue;
                Integer parent;
                if (scope.parentRequest() == null) {
                    parent = nodes.size();
                    nodes.add(new QueryRequestTraceNode(parent, null, "purpose", scope.requestName(), scope.purpose(), null));
                } else parent = Objects.requireNonNull(indices.get(scope.parentRequest()));
                int id = nodes.size();
                nodes.add(new QueryRequestTraceNode(id, parent, "comment", scope.requestName(), scope.comment(),
                        scope.operation() == null ? null : scope.operation().method()));
                indices.put(scope, id);
            }
        }
        return List.copyOf(nodes);
    }
}
