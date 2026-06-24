package io.teaql.runtime.log;

import io.teaql.runtime.config.TeaQLEnv;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LogConfig {
    public enum LogLevel {
        SILENT, SUMMARY, FULL, FULL_WITH_PAYLOAD;

        public static LogLevel parse(String s, LogLevel defaultLevel) {
            if (s == null) return defaultLevel;
            switch (s.toLowerCase()) {
                case "_silent": return SILENT;
                case "_summary": return SUMMARY;
                case "_full": return FULL;
                case "_full_with_payload": return FULL_WITH_PAYLOAD;
                default: return defaultLevel;
            }
        }
    }

    private final LogLevel auditLevel;
    private final LogLevel sqlLevel;
    private final LogLevel toolLevel;

    private final List<String> auditEntities;
    private final List<String> sqlTables;
    private final List<String> toolFocus;

    private static final LogConfig INSTANCE = new LogConfig();

    private LogConfig() {
        this.auditLevel = LogLevel.parse(TeaQLEnv.get("TEAQL_AUDIT_LOG"), LogLevel.FULL);
        this.sqlLevel = LogLevel.parse(TeaQLEnv.get("TEAQL_SQL_LOG"), LogLevel.SUMMARY);
        this.toolLevel = LogLevel.parse(TeaQLEnv.get("TEAQL_TOOL_LOG"), LogLevel.FULL);

        this.auditEntities = parseList(TeaQLEnv.get("TEAQL_AUDIT_LOG_ENTITIES"));
        this.sqlTables = parseList(TeaQLEnv.get("TEAQL_SQL_LOG_TABLES"));
        this.toolFocus = parseList(TeaQLEnv.get("TEAQL_TOOL_LOG_FOCUS"));
    }

    public static LogConfig getInstance() {
        return INSTANCE;
    }

    private List<String> parseList(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return Arrays.stream(val.split(","))
                     .map(String::trim)
                     .map(String::toLowerCase)
                     .collect(Collectors.toList());
    }

    public boolean shouldLogAudit(String entity) {
        if (auditLevel == LogLevel.SILENT) return false;
        if (auditEntities != null && entity != null) {
            return auditEntities.contains(entity.toLowerCase());
        }
        return true;
    }

    public boolean shouldLogSql(String sql) {
        if (sqlLevel == LogLevel.SILENT) return false;
        if (sqlTables != null && sql != null) {
            String sqlLower = sql.toLowerCase();
            return sqlTables.stream().anyMatch(sqlLower::contains);
        }
        return true;
    }

    public boolean shouldLogTool(String module) {
        if (toolLevel == LogLevel.SILENT) return false;
        if (toolFocus != null && module != null) {
            return toolFocus.contains(module.toLowerCase());
        }
        return true;
    }
}
