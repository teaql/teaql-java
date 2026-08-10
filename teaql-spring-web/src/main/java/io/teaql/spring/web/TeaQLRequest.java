package io.teaql.spring.web;

import com.fasterxml.jackson.databind.JsonNode;

public class TeaQLRequest {
    private String operation = "Query";
    private JsonNode payload;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
