CREATE TABLE teaql_blob_command (
    scope_realm VARCHAR(256) NOT NULL,
    scope_boundary VARCHAR(256) NOT NULL,
    operation VARCHAR(128) NOT NULL,
    invocation_key VARCHAR(128) NOT NULL,
    request_digest VARCHAR(64) NOT NULL,
    principal_digest VARCHAR(64) NOT NULL,
    receipt_json TEXT,
    completed_at BIGINT,
    CONSTRAINT teaql_blob_command_pk PRIMARY KEY (scope_realm, scope_boundary, operation, invocation_key),
    CONSTRAINT teaql_blob_command_completion_ck CHECK ((receipt_json IS NULL) = (completed_at IS NULL)),
    CONSTRAINT teaql_blob_command_time_ck CHECK (completed_at IS NULL OR completed_at >= 0),
    CONSTRAINT teaql_blob_command_size_ck CHECK (receipt_json IS NULL OR octet_length(receipt_json) <= 1048576)
);
