package io.teaql.spring.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.MutationExecutor;
import io.teaql.core.QueryExecutor;
import io.teaql.core.UserContext;
import io.teaql.runtime.DefaultMutationRequest;
import io.teaql.runtime.DefaultQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teaql")
public class TeaQLController {

    @Autowired(required = false)
    private QueryExecutor queryExecutor;

    @Autowired(required = false)
    private MutationExecutor mutationExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private UserContext userContext;

    @PostMapping
    public ResponseEntity<?> handleRequest(@RequestBody TeaQLRequest req) {
        try {
            if (req.getOperation() == null) {
                return ResponseEntity.badRequest().body("Operation cannot be null");
            }
            switch (req.getOperation().toLowerCase()) {
                case "query":
                    if (queryExecutor == null) {
                        return ResponseEntity.internalServerError().body("QueryExecutor not configured");
                    }
                    DefaultQueryRequest queryRequest = objectMapper.treeToValue(req.getPayload(), DefaultQueryRequest.class);
                    return ResponseEntity.ok(queryExecutor.query(userContext, queryRequest));
                case "insert":
                case "update":
                case "delete":
                    if (mutationExecutor == null) {
                        return ResponseEntity.internalServerError().body("MutationExecutor not configured");
                    }
                    DefaultMutationRequest mutationRequest = objectMapper.treeToValue(req.getPayload(), DefaultMutationRequest.class);
                    return ResponseEntity.ok(mutationExecutor.mutate(userContext, mutationRequest));
                default:
                    return ResponseEntity.badRequest().body("Unknown operation: " + req.getOperation());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
