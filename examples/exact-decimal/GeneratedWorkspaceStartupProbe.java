package com.example.exactdecimalservice;

import io.teaql.provider.jdbc.JdbcSqlExecutor;
import org.springframework.boot.SpringApplication;

/** Closes the real generated Spring workspace after its ensure-schema runner. */
public final class GeneratedWorkspaceStartupProbe {
    private GeneratedWorkspaceStartupProbe() {}

    public static void main(String[] args) {
        try (var context = SpringApplication.run(App.class, args)) {
            if (context.getBeansOfType(JdbcSqlExecutor.class).size() != 1) {
                throw new AssertionError("generated SQLite workspace must own one JDBC executor");
            }
        }
        System.out.println("GENERATED_EXACT_DECIMAL_WORKSPACE_PASS");
    }
}
